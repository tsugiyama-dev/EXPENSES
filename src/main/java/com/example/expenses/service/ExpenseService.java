package com.example.expenses.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.MDC;
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
import com.example.expenses.exception.BusinessException;
import com.example.expenses.kafka.ExpenseEventMessage;
import com.example.expenses.kafka.ExpenseEventMessage.EventType;
import com.example.expenses.kafka.ExpenseKafkaProducer;
import com.example.expenses.repository.ExpenseAuditLogMapper;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.repository.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

	private final ExpenseMapper expenseMapper;
	private final ExpenseAuditLogMapper auditLogMapper;
	private final AuthenticationContext authenticationContext;
	private final ExpenseKafkaProducer kafkaProducer;
	private final UserMapper userMapper;

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
				traceId()));

		return ExpenseResponse.toResponse(expense);
	}

	public List<Expense> getAllExpenses(ExpenseSearchCriteria criteria, Long userId) {
		ExpenseSearchCriteriaEntity e = new ExpenseSearchCriteriaEntity();

		if (authenticationContext.isOwnerOrApprover(userId)) {
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

		if (!authenticationContext.isApprover()) {
			e.setApplicantId(userId);
		}

		String orderBy = normalizedOrderBy(criteria.sort());
		String direction = normalizedDirection(criteria.sort());
		int offset = (currentPage - 1) * pageSize;
		long cnt = expenseMapper.count(e);
		int totalPage = (int) Math.ceil((double) cnt / pageSize);

		List<ExpenseResponse> items = ExpenseResponse.toListResponse(
				expenseMapper.search(e, orderBy, direction, pageSize, offset));

		return new PaginationResponse<>(items, currentPage, pageSize, (int) cnt, totalPage,
				pageList(currentPage, totalPage, 5));
	}

	@Transactional
	public ExpenseResponse submit(Long expenseId, Long applicantId) {
		Expense current = expenseMapper.findById(expenseId);
		if (current == null) {
			throw new NoSuchElementException("Expense not found: " + expenseId);
		}
		if (!current.canBeSubmittedBy(applicantId)) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "ステータスもしくは本人ではないため提出できません");
		}

		int updated = expenseMapper.submitDraft(expenseId);
		if (updated == 0) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "下書き以外提出できません");
		}

		auditLogMapper.insert(ExpenseAuditLog.createDraft(expenseId, applicantId, traceId()));

		kafkaProducer.publish(new ExpenseEventMessage(
				EventType.SUBMITTED, expenseId, applicantId, applicantId, null, traceId()));

		return ExpenseResponse.toResponse(expenseMapper.findById(expenseId));
	}

	@Transactional
	public ExpenseResponse approve(long expenseId, int version, Long approverId) {
		Expense expense = expenseMapper.findById(expenseId);
		if (expense == null) {
			throw new BusinessException("NOT_FOUND", "経費申請が見つかりません: " + expenseId, traceId());
		}
		if (!expense.canBeApproved()) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は承認できません", traceId());
		}
		if (expense.getVersion() != version) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId());
		}

		int updated = expenseMapper.approve(expenseId, version);
		if (updated == 0) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId());
		}

		auditLogMapper.insert(ExpenseAuditLog.createApprove(expenseId, approverId, traceId()));

		kafkaProducer.publish(new ExpenseEventMessage(
				EventType.APPROVED, expenseId, approverId, expense.getApplicantId(), null, traceId()));

		return ExpenseResponse.toResponse(expenseMapper.findById(expenseId));
	}

	@Transactional
	public ExpenseResponse reject(long expenseId, String reason, int version, Long approverId) {
		String traceId = traceId();
		Expense expense = expenseMapper.findById(expenseId);
		if (expense == null) {
			throw new BusinessException("NOT_FOUND", "経費申請が見つかりません: " + expenseId, traceId);
		}
		if (!expense.canBeRejected()) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は却下できません", traceId);
		}
		if (expense.getVersion() != version) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId);
		}

		int updated = expenseMapper.reject(expenseId, version);
		if (updated == 0) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId);
		}

		auditLogMapper.insert(ExpenseAuditLog.createReject(expenseId, approverId, traceId, reason));

		kafkaProducer.publish(new ExpenseEventMessage(
				EventType.REJECTED, expenseId, approverId, expense.getApplicantId(), reason, traceId));

		return ExpenseResponse.toResponse(expenseMapper.findById(expenseId));
	}

	private String traceId() {
		String tid = MDC.get(TraceIdFilter.TRACE_ID_KEY);
		return tid == null ? "" : tid;
	}

	private String normalizedOrderBy(String sort) {
		if (sort == null || sort.isBlank()) return "created_at";
		String key = sort.split(",")[0].trim();
		String column = switch (key) {
			case "updated_at" -> "updated_at";
			case "submitted_at" -> "submitted_at";
			case "amount" -> "amount";
			case "id" -> "id";
			default -> "created_at";
		};
		return ALLOWED_SORTS.contains(column) ? column : "created_at";
	}

	private String normalizedDirection(String sort) {
		if (sort == null || sort.isBlank()) return "DESC";
		String[] parts = sort.split(",");
		if (parts.length < 2) return "DESC";
		return "asc".equalsIgnoreCase(parts[1].trim()) ? "ASC" : "DESC";
	}

	private List<Integer> pageList(int currentPage, int totalPage, int displayPage) {
		int start, end;
		if (totalPage < displayPage) {
			start = 1;
			end = totalPage;
		} else {
			start = Math.max(1, currentPage - 2);
			end = Math.min(totalPage, start + displayPage - 1);
			if (end == totalPage) start = end - displayPage + 1;
		}
		return java.util.stream.IntStream.rangeClosed(start, end).boxed().toList();
	}
}
