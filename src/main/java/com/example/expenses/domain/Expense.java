package com.example.expenses.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class Expense {

	private Long id;
	private Long applicantId;
	private String title;
	private BigDecimal amount;
	private String currency;
	private ExpenseStatus status;
	private LocalDateTime submittedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private int version;

	/**
	 * 経費を新規作成（ファクトリーメソッド）
	 * 必ず下書き状態で作成される
	 */
	public static Expense create(Long applicantId, String title, BigDecimal amount, String currency) {
		if(applicantId == null) {
			throw new IllegalArgumentException("申請者ＩＤは必須です");
		}
		
		if(title == null  || title.isBlank()) {
			throw new IllegalArgumentException("タイトルは必須です");
		}
		
		if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("金額は正の数でなければなりません");
		}
		
		Expense expense = new Expense();
		expense.applicantId = applicantId;
		expense.title = title;
		expense.amount = amount;
		expense.currency = currency != null ? currency : "JPY";
		expense.status = ExpenseStatus.DRAFT;
		expense.version = 0;
		expense.createdAt = LocalDateTime.now();
		
		return expense;
	}
	
	/**
	 * 経費が下書き状態のときのみ更新可能
	 */
	public void submit() {
		if(this.status != ExpenseStatus.DRAFT) {
			throw new IllegalStateException("下書き状態の経費のみ提出可能です");
		}
		
		this.status = ExpenseStatus.SUBMITTED;
		this.submittedAt = LocalDateTime.now();
		this.version++;
	}
	/**
	 * 提出された経費のみ承認可能
	 */
	public void approve() {
		if(this.status != ExpenseStatus.SUBMITTED) {
			throw new IllegalStateException("提出された経費の未承認できます");
		}
		
		this.status = ExpenseStatus.APPROVED;
		this.updatedAt = LocalDateTime.now();
		this.version++;
	}
	/**
	 *  提出された経費のみ却下可能
	 */
	public void reject(String reason) {
		if(this.status != ExpenseStatus.SUBMITTED) {
			throw new IllegalStateException("提出された経費のみ却下可能です");
		}
		
		this.status = ExpenseStatus.REJECTED;
		this.updatedAt = LocalDateTime.now();
		this.version++;
	}
	/**
	 * 本人で下書き状態の経費のみ提出可能
	 */
	public boolean canBeSubmittedBy(Long userId) {
		return this.applicantId.equals(userId) &&
				this.status == ExpenseStatus.DRAFT;
			
	}
	/**
	 * 経費が承認可能かどうか
	 */
	public boolean canBeApproved() {
		return this.status == ExpenseStatus.SUBMITTED;
	}
	
	/**
	 * 経費が却下可能かどうか
	 * @return
	 */
	public boolean canBeRejected() {
		return this.status == ExpenseStatus.SUBMITTED;
	}
	
	
}
