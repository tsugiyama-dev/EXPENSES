package com.example.expenses.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.expenses.config.TraceIdFilter;
import com.example.expenses.domain.Expense;
import com.example.expenses.dto.ExpenseAuditLog;
import com.example.expenses.dto.request.ExpenseCreateRequest;
import com.example.expenses.dto.request.ExpenseSearchCriteria;
import com.example.expenses.dto.request.ExpenseSearchCriteriaEntity;
import com.example.expenses.dto.response.ExpenseResponse;
import com.example.expenses.dto.response.PaginationResponse;
import com.example.expenses.event.ExpenseApprovedEvent;
import com.example.expenses.event.ExpenseRejectedEvent;
import com.example.expenses.event.ExpenseSubmittedEvent;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.repository.ExpenseAuditLogMapper;
import com.example.expenses.repository.ExpenseMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

	private final ExpenseMapper expenseMapper;
	private final ExpenseAuditLogMapper auditLogMapper;
	private final AuthenticationContext authenticationContext;
	
	private final ApplicationEventPublisher eventPublisher;
	private static final Set<String> ALLOWED_SORTS = Set.of("created_at", "updated_at", "submitted_at", "amount", "id");
	

	@Transactional
	public ExpenseResponse create(ExpenseCreateRequest req) {

		Long currentUserId = authenticationContext.getCurrentUserId();
		
		Expense expense = Expense.create(
				currentUserId,
				req.title(),
				req.amount(),
				req.currency());

		expenseMapper.insert(expense);

		auditLogMapper.insert(ExpenseAuditLog.create(
				expense.getId(),
				currentUserId,
				traceId()
		));

		
		return ExpenseResponse.toResponse(expense);
	}

	public List<Expense> getAllExpenses(ExpenseSearchCriteria criteria, Long userId) {

		ExpenseSearchCriteriaEntity e = new ExpenseSearchCriteriaEntity();
		
		if(authenticationContext.isOwnerOrApprover(userId)) {
			e.setTitle(criteria.title());
			e.setAmountMax(criteria.amountMax());
			e.setAmountMin(criteria.amountMin());
			e.setSubmittedFrom(criteria.submittedFrom());
			e.setSubmittedTo(criteria.submittedTo());
			e.setStatus(criteria.status());
			e.setApplicantId(userId);
		}
		return expenseMapper.findAll(e);
	}
	
	/*
	 * キャッシュ対象：経費の1件取得
	 * 
	 * @Cacheable("expenses")の動作
	 *  1．Redisに "expenses::1"（キャッシュ名::キー）が存在する → DB を叩かずにキャッシュを返す
	 *  2．キャッシュが存在しない（またはTTL切れ） → DB から取得してRedisに保存してから返す
	 *  
	 *  key= "#expenseId"はSpEL（Spring Expression Language）。
	 *  メソッド引数の名前をそのままキャッシュキーに使う。
	 *   → Redis のキーは"expenses::1", "expenses::2" ...のようになる。
	 */
	@Cacheable(cacheNames= "expenses", key="#expenseId")
	public Expense getExpense(Long expenseId) {
		return expenseMapper.findById(expenseId);
	}
	
	public PaginationResponse<ExpenseResponse> search(
			ExpenseSearchCriteria criteria,
			int currentPage,
			int pageSize) {
		Long userId = authenticationContext.getCurrentUserId();
		
		ExpenseSearchCriteriaEntity e = new ExpenseSearchCriteriaEntity();
		e.setTitle(criteria.title());
		e.setApplicantId(criteria.applicantId());
		e.setStatus(criteria.status());
		e.setAmountMax(criteria.amountMax());
		e.setAmountMin(criteria.amountMin());
		e.setSubmittedFrom(criteria.submittedFrom());
		e.setSubmittedTo(criteria.submittedTo());
				
		if(!authenticationContext.isApprover()) {
			e.setApplicantId(userId);
		}

		String orderBy  = normalizedOrderBy(criteria.sort());
		String direction =  normalizedDirection(criteria.sort());

		int offset = (currentPage - 1) * pageSize;
		
		long cnt = expenseMapper.count(e);
		
		int  totalPage = (int)Math.ceil((double) cnt / pageSize);
		
		List<Integer> pageList = pageList(currentPage, totalPage, 5);
		
		List<ExpenseResponse> items = ExpenseResponse.toListResponse(
				expenseMapper.search(e, orderBy, direction, pageSize, offset));
		
		return new PaginationResponse<>(items, currentPage, pageSize, (int)cnt, totalPage, pageList);
	}

	@Transactional
	/*
	 * @CacheEvict メソッド実行後（DB 更新後）にRedisから "expenses::{expenseId}" Noエントリを削除
	 * 次にgetExpense()が が呼ばれると @Cacheable が DB から再取得してキャッシュを作り直す
	 * 
	 * beforeInvocation = false (デフォルト)：
	 * 　メソッド実行「後」に削除 → 例外でロールバックした場合はキャッシュを残す。
	 *   true Nisuruto実行「前」に削除 → ロールバックしてもキャッシュは消える（古いデータが消えた状態になる）
	 */
	@CacheEvict(cacheNames = "expenses", key = "#expenseId")
	public ExpenseResponse submit(Long expenseId, Long applicantId) {
		
	
		Expense current =expenseMapper.findById(expenseId);
		if(Objects.isNull(current)) {
			throw new NoSuchElementException("Expense not found: " + expenseId);
		}
		
		if(!current.canBeSubmittedBy(applicantId)) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "ステータスもしくは本人ではないため提出できません");
		}
		
		int updated = expenseMapper.submitDraft(expenseId);
		
		if(updated == 0) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "下書き以外提出できません");
		}
		
		auditLogMapper.insert(ExpenseAuditLog.createDraft(expenseId, applicantId, traceId()));
		
		eventPublisher.publishEvent(new ExpenseSubmittedEvent(expenseId, applicantId, traceId()));

		return ExpenseResponse.toResponse(expenseMapper.findById(expenseId));
	}
	
	/**
	 * 経費承認
	 */
	@CacheEvict(cacheNames = "expenses", key="#expenseId")
	@Transactional
	public ExpenseResponse approve(long expenseId, int version, Long approverId) {
		
		Expense expense = expenseMapper.findById(expenseId);
		
		if(Objects.isNull(expense)) {
			throw new BusinessException("INVALID_ARGUMENT_NOT_FOUND", "経費申請が見つかりません: EXPENSEID ：" + expenseId, traceId());
		}
		
		if(!expense.canBeApproved()) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は承認できません", traceId());	
		}
		
		if(expense.getVersion()!= version) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId());
		}

		int updated = expenseMapper.approve(expenseId, version);
		if(updated == 0) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId());
		}
		
		auditLogMapper.insert(ExpenseAuditLog.createApprove(expenseId, approverId, traceId()));
		
		eventPublisher.publishEvent(
				new ExpenseApprovedEvent(expenseId, approverId, expense.getApplicantId(),traceId()));

		return ExpenseResponse.toResponse(expenseMapper.findById(expenseId));
		
		
	}
	
	/**
	 * 経費却下
	 */
	@CacheEvict(cacheNames="expenses", key="#expenseId")
	@Transactional
	public ExpenseResponse reject(long expenseId, String reason, int version, Long rejectorId) {
		
		String traceId = traceId();

		Expense expense = expenseMapper.findById(expenseId);	

		if(expense == null) {
			throw new BusinessException("INVALID_ARGUMENT_NOT_FOUND", "経費申請が見つかりません: EXPENSEID ：" + expenseId, traceId);
		}

		if(!expense.canBeRejected()) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は却下できません", traceId);	
		}

		if(expense.getVersion() != version) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId);
		}
		
		int updated = expenseMapper.reject(expenseId, version);
		
		
		if(updated == 0) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId);
		}

		auditLogMapper.insert(ExpenseAuditLog.createReject(expenseId, rejectorId, traceId, reason));
		
		eventPublisher.publishEvent(
				new ExpenseRejectedEvent(expenseId, rejectorId, traceId, expense.getApplicantId(), reason));
		
		return ExpenseResponse.toResponse(expenseMapper.findById(expenseId));
	}
	
	
	private String traceId() {
		String tid = MDC.get(TraceIdFilter.TRACE_ID_KEY);
		return tid == null ? "" : tid;
	}
	
	private String normalizedOrderBy(String sort) {
		
		if(sort == null || sort.isBlank()) {
			return "created_at";
		}
		
		String[] parts = sort.split(",");
		String key = parts[0].trim();
		String column = switch(key) {
		case "created_at" -> "created_at";
		case "updated_at" ->  "updated_at";
		case "submitted_at" -> "submitted_at";
		case "amount" -> "amount";
		case "id" -> "id";
		default -> "created_at";
		};
		
		if(!ALLOWED_SORTS.contains(column)) {
			return "created_at";
		}
		return column;
		
	}	
	
	private String normalizedDirection(String sort) {
		
		if(sort == null || sort.isBlank()) return "DESC";
		String[] parts = sort.split(",");
		
		if(parts.length < 2) return "DESC";
		String dir = parts[1].trim();
		return "asc".equalsIgnoreCase(dir) ? "ASC" : "DESC";
	}
	
	private List<Integer> pageList(int currentPage, int totalPage, int displayPage) {
		
		int start = 0;
		int end = 0;
		
		if(totalPage < displayPage) {
			start = 1;
			end = totalPage;
		}else {
			start = Math.max(1, currentPage - 2);
			end = Math.min(totalPage, start + displayPage - 1);
			
			if(end == totalPage) {
				start = end - displayPage + 1;
			}
		}
		return  IntStream.rangeClosed(start, end)
				.boxed()
				.toList();
	}
}
