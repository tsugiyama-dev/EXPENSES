package com.example.expenses.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.expenses.config.TraceIdFilter;
import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.dto.ExpenseAuditLog;
import com.example.expenses.dto.request.ExpenseCreateRequest;
import com.example.expenses.dto.request.ExpenseSearchCriteria;
import com.example.expenses.dto.request.ExpenseSearchCriteriaEntity;
import com.example.expenses.dto.response.ExpenseResponse;
import com.example.expenses.dto.response.PaginationResponse;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.notification.NotificationService;
import com.example.expenses.repository.ExpenseAuditLogMapper;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.repository.UserMapper;
import com.example.expenses.util.CurrentUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

	private final ExpenseMapper expenseMapper;
	private final UserMapper userMapper;
	private final ExpenseAuditLogMapper auditLogMapper;
	private final NotificationService notificationService;
	private final CreateCsvService createCsvService;
	
	private static final Set<String> ALLOWED_SORTS = Set.of("created_at", "updated_at", "submitted_at", "amount", "id");
	
	public ExpenseResponse create(ExpenseCreateRequest req) {
		
		Expense expense = new Expense();
		expense.setApplicantId(CurrentUser.actorId());
		expense.setTitle(req.title());
		expense.setAmount(req.amount());
		expense.setCurrency(req.currency());
		expense.setStatus(ExpenseStatus.DRAFT);
		
		expenseMapper.insert(expense);
		log.info("Expense{}", expense);
		
		auditLogMapper.insert(ExpenseAuditLog.create(
				expense.getId(),
				CurrentUser.actorId(),
				traceId()));
		Expense saved = expenseMapper.findById(expense.getId());
		return toResponse(saved);
	}
	
	
	public PaginationResponse<ExpenseResponse> search(
			ExpenseSearchCriteria criteria,
			int currentPage,
			int pageSize,
			Long actorId,
			List<String> roles) {
		
		
		ExpenseSearchCriteriaEntity e = new ExpenseSearchCriteriaEntity();
		e.setTitle(criteria.title());
		e.setApplicantId(criteria.applicantId());
		e.setStatus(criteria.status());
		e.setAmountMax(criteria.amountMax());
		e.setAmountMin(criteria.amountMin());
		e.setSubmittedFrom(criteria.submittedFrom());
		e.setSubmittedTo(criteria.submittedTo());
		
		//本人以外とROLE_APPROVER以外は全て見れない		
		if(!roles.contains("ROLE_APPROVER")) {
			e.setApplicantId(actorId);
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
	
	//draft → submit
	@Transactional
	public ExpenseResponse submit(Long expenseId) {
		
		Long userId = CurrentUser.actorId();
		//存在確認（404判定のため）
		Expense current =expenseMapper.findById(expenseId);
		if(current == null) {
			throw new NoSuchElementException("Expense not found: " + expenseId);
		}
		
		if(!current.getApplicantId().equals(userId)) {
			throw new BusinessException("NOT_OWNER", "本人以外は提出できません");
		}
		
		int updated = expenseMapper.submitDraft(expenseId);
		
		//Draftのみ更新できる（事故防止）
		if(updated == 0) {
			throw new BusinessException("INVALID_STATUS_TRANSITION",
					"下書き以外提出できません");
		}
		auditLogMapper.insert(ExpenseAuditLog.createDraft(expenseId, CurrentUser.actorId(), traceId()));
		
		//申請者へメール送信処理
		notificationService.notifySubmitted(getApproverAddress(), expenseId, traceId());
		
		
		Expense saved = expenseMapper.findById(expenseId);
		return toResponse(saved);
	}
	
	//submitted → approve
	@Transactional
	public ExpenseResponse approve(long expenseId, int version, Long actorId) {
		
		Expense expense = expenseMapper.findById(expenseId);
		
		//存在確認
		if(expense == null) {
			throw new BusinessException("NOT_FOUND", "経費申請が見つかりません: EXPENSEID ：" + expenseId, traceId());
		}
		
		//ステータス確認
		if(expense.getStatus() != ExpenseStatus.SUBMITTED) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は承認できません", traceId());
		}
		//楽観的ロック確認
		if(expense.getVersion()!= version) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId());
		}
		//承認処理
		int updated = expenseMapper.approve(expenseId, version);
		if(updated == 0) {
//			throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は承認できません", traceId());
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId());
		}
		//監査ログ登録
		auditLogMapper.insert(ExpenseAuditLog.createApprove(expenseId, actorId, traceId()));
		
		//申請者へメール通知処理
		try {
			notificationService.notifyApproved(getApplicantAddress(expense.getApplicantId()), expenseId, traceId());
			
		}catch(Exception e) {
			log.warn("mail failed traceId={} expenseId={}", traceId(), expenseId, e);
		}
		
		var saved = expenseMapper.findById(expenseId);
		return ExpenseResponse.toResponse(saved);
		
		
	}
	
	//submitted → reject
	@Transactional
	public ExpenseResponse reject(long expenseId, String reason, int version, Long actorId) {
		
		String traceId = traceId();
		Expense expense = expenseMapper.findById(expenseId);
		
		//存在確認
		if(expense == null) {
			throw new BusinessException("NOT_FOUND", "経費申請が見つかりません: EXPENSEID ：" + expenseId, traceId);
		}
		//ステータス確認
		if(expense.getStatus() != ExpenseStatus.SUBMITTED) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は却下できません", traceId);	
		}
		//楽観的ロック確認
		if(expense.getVersion() != version) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId);
		}
		//却下処理
		int updated = expenseMapper.reject(expenseId, version);
		
		if(updated == 0) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId);
		}
		//監査ログ登録
		auditLogMapper.insert(ExpenseAuditLog.createReject(expenseId, actorId, traceId, reason));
		
		//申請者へメール通知処理
		try{
			notificationService.notifyRejected(getApplicantAddress(expense.getApplicantId()), expenseId, reason, traceId);
		}catch(Exception e) {
			log.warn("mail failed traceId={} expenseId={}", traceId(), expenseId, e);
		}
		
		var saved = expenseMapper.findById(expenseId);
		return toResponse(saved);
	}
	
	public byte[] getCsv(ExpenseSearchCriteria criteria) {
		
		List<Expense> list = expenseMapper.filter(ExpenseSearchCriteria.toEntity(criteria), "created_at", "DESC");
		return createCsvService.createCsv(list);
	}
	
	
	private ExpenseResponse toResponse(Expense expense) {
		return ExpenseResponse.toResponse(expense);
	}
	
	private String traceId() {
		String tid = MDC.get(TraceIdFilter.TRACE_ID_KEY);
		return tid == null ? "" : tid;
	}
	
	private String getApplicantAddress(Long applicantId) {
		return userMapper.findEmailById(applicantId);
	}
	private String getApproverAddress() {
		return userMapper.findAnyApproverEmail();
	}
	
	
	private String normalizedOrderBy(String sort) {
		
		if(sort == null || sort.isBlank()) {
			return "created_at";
		}
		
		String[] parts = sort.split(",");
		
		String key = parts[0].trim();
		
		String column = switch(key) {
		case "created_at":
			yield "created_at";
		case "updated_at":
			yield "updated_at";
		case "submitted_at":
			yield "submitted_at";
		case "amount":
			yield "amount";
		case "id":
			yield "id";
		default :
			yield "created_at";
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
			
			start = Math.max(1, currentPage -2);
			end = Math.min(totalPage, start + displayPage - 1);
			
			if(end == totalPage) {
				start = end - displayPage + 1;
			}
		}

		return  java.util.stream.IntStream.rangeClosed(start, end)
				.boxed()
				.toList();
	}
	
}
