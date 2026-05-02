package com.example.expenses.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.expenses.config.TraceIdFilter;
import com.example.expenses.controller.NotificationWebSocketController;
import com.example.expenses.domain.Expense;
import com.example.expenses.domain.User;
import com.example.expenses.dto.ExpenseAuditLog;
import com.example.expenses.dto.NotificationMessage;
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
import com.example.expenses.repository.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor//Lombokが全フィールドのコンストラクタを自動生成
@Slf4j
public class ExpenseService {

	private final ExpenseMapper expenseMapper;
	private final ExpenseAuditLogMapper auditLogMapper;
	private final AuthenticationContext authenticationContext;
	private final ApplicationEventPublisher eventPublisher;
	private final UserMapper userMapper;
	private final NotificationWebSocketController notificationController;
	
	private static final Set<String> ALLOWED_SORTS = Set.of("created_at", "updated_at", "submitted_at", "amount", "id");
	
	/**
	 * @param req
	 * @return 作成された経費申請の情報を含むExpenseResponseオブジェクト
	 */
	@Transactional
	public ExpenseResponse create(ExpenseCreateRequest req) {

		//認証サービスから現在のユーザーＩＤを取得
		Long currentUserId = authenticationContext.getCurrentUserId();
		
		//エンティティを作成
		Expense expense = Expense.create(
				currentUserId,
				req.title(),
				req.amount(),
				req.currency());

		
		//データベースに保存
		expenseMapper.insert(expense);

		
        //監査ログを記録
		auditLogMapper.insert(ExpenseAuditLog.create(
				expense.getId(),
				currentUserId,
				traceId()
		));
		
		return ExpenseResponse.toResponse(expense);
	}
	/**
	 * 経費の全件取得
	 * @param criteria
	 * @return 経費の一覧
	 */
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
	
	public Expense getExpense(Long expenseId) {
		return expenseMapper.findById(expenseId);
	}
	
	//検索条件に基づいて経費申請のリストを取得し、ページネーションされたレスポンスを返す
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
		
		//ROLE_APPROVER以外は全て見れない		
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
	
	/**
	 * 経費提出
	 */
	@Transactional
	public ExpenseResponse submit(Long expenseId, Long applicantId) {
		
		
		//経費申請の取得
		Expense current =expenseMapper.findById(expenseId);
		if(current == null) {
			throw new NoSuchElementException("Expense not found: " + expenseId);
		}
		
		if(!current.canBeSubmittedBy(applicantId)) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "ステータスもしくは本人ではないため提出できません");
		}
		
		//提出処理
		int updated = expenseMapper.submitDraft(expenseId);
		
		if(updated == 0) {
			throw new BusinessException("INVALID_STATUS_TRANSITION",
					"下書き以外提出できません");
		}
		
		//監査ログ登録
		auditLogMapper.insert(ExpenseAuditLog.createDraft(expenseId, applicantId, traceId()));
		
		//イベント発行
		eventPublisher.publishEvent(
				new ExpenseSubmittedEvent(
						expenseId,
						applicantId,
						traceId()
						));
		
		User applicantUser = userMapper.findById(applicantId);
		if(applicantUser == null) {
			throw new BusinessException("NOT_FOUND", "ユーザーが見つかりません");
		}
		
		// WebSocket通知
		NotificationMessage notification = NotificationMessage.builder()
				.type(NotificationMessage.NotificationType.EXPENSE_SUBMITTED)
				.expenseId(expenseId)
				.applicantName(applicantUser.getDisplayName())
				.title(current.getTitle())
				.amount(current.getAmount().toString())
				.message("新しい経費が申請されました")
				.timestamp(LocalDateTime.now())
				.build();
		
		// 全承認者にブロードキャスト
		notificationController.sendNotification(notification);
		
		//6.結果を返す
		Expense saved = expenseMapper.findById(expenseId);
		return ExpenseResponse.toResponse(saved);
	}
	
	/**
	 * 経費承認
	 */
	@Transactional
	public ExpenseResponse approve(long expenseId, int version, Long approverId) {
		

		//経費取得
		Expense expense = expenseMapper.findById(expenseId);
		
		//存在確認
		if(expense == null) {
			throw new BusinessException("NOT_FOUND", "経費申請が見つかりません: EXPENSEID ：" + expenseId, traceId());
		}
		
		if(!expense.canBeApproved()) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は承認できません", traceId());	
		}
		
		//二重更新チェック
		if(expense.getVersion()!= version) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId());
		}
		//承認処理
		int updated = expenseMapper.approve(expenseId, version);
		if(updated == 0) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId());
		}
		
		// WebSocket 通知送信
		User approver = userMapper.findById(approverId);
		if(approver == null) {
			throw new BusinessException("NOT_FOUND", "承認者が見つかりません: " + approverId);
		}
		User applicantUser = userMapper.findById(expense.getApplicantId());
		if(applicantUser == null) {
			throw new BusinessException("NOT_FOUND", "申請者が見つかりません: " + expense.getApplicantId());
		}

		// WebSocket通知
		NotificationMessage notification = NotificationMessage.builder()
				.type(NotificationMessage.NotificationType.EXPENSE_APPROVED)
				.expenseId(expenseId)
				.applicantName(applicantUser.getDisplayName())
				.approverName(approver.getDisplayName())
				.title(expense.getTitle())
				.amount(expense.getAmount().toString())
				.message("経費が承認されました")
				.timestamp(LocalDateTime.now())
				.build();
		notificationController.sendNotificationToUser(expense.getApplicantId(), notification);
		
		//監査ログ登録
		auditLogMapper.insert(ExpenseAuditLog.createApprove(expenseId, approverId, traceId()));
		
		//イベントを発行
		eventPublisher.publishEvent(
				new ExpenseApprovedEvent(
						expenseId,
						approverId,
						expense.getApplicantId(),
						traceId()
						));

		

		//更新後の経費を取得して返す
		var saved = expenseMapper.findById(expenseId);
		return ExpenseResponse.toResponse(saved);
		
		
	}
	
	/**
	 * 経費却下
	 */
	@Transactional
	public ExpenseResponse reject(long expenseId, String reason, int version, Long approverId) {
		
		String traceId = traceId();
		//経費取得
		Expense expense = expenseMapper.findById(expenseId);	
		//存在確認
		if(expense == null) {
			throw new BusinessException("NOT_FOUND", "経費申請が見つかりません: EXPENSEID ：" + expenseId, traceId);
		}
		//ビジネスルールチェック
		if(!expense.canBeRejected()) {
			throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は却下できません", traceId);	
		}
		//二重更新チェック
		if(expense.getVersion() != version) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId);
		}
		//却下処理
		int updated = expenseMapper.reject(expenseId, version);
		
		
		if(updated == 0) {
			throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId);
		}
		
		User approver = userMapper.findById(approverId);
		if(approver == null) {
			throw new BusinessException("NOT_FOUND", "承認者が見つかりません: " + approverId);
		}
		User applicantUser = userMapper.findById(expense.getApplicantId());
		if(applicantUser == null) {
			throw new BusinessException("NOT_FOUND", "申請者が見つかりません: " + expense.getApplicantId());
		}
		
		// WebSocket通知
		NotificationMessage notification = NotificationMessage.builder()
				.type(NotificationMessage.NotificationType.EXPENSE_APPROVED)
				.expenseId(expenseId)
				.applicantName(applicantUser.getDisplayName())
				.approverName(approver.getDisplayName())
				.title(expense.getTitle())
				.amount(expense.getAmount().toString())
				.message("経費が承認されました")
				.timestamp(LocalDateTime.now())
				.build();
		notificationController.sendNotificationToUser(expense.getApplicantId(), notification);
		//監査ログ登録
		auditLogMapper.insert(ExpenseAuditLog.createReject(expenseId, approverId, traceId, reason));
		
		//イベント発行
		eventPublisher.publishEvent(
				new ExpenseRejectedEvent(
						expenseId,
						approverId,
						traceId,
						expense.getApplicantId(),
						reason
						));
		//更新後の経費を取得して返す
		var saved = expenseMapper.findById(expenseId);
		return ExpenseResponse.toResponse(saved);
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
