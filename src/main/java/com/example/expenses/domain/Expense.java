package com.example.expenses.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
	 * 下書き状態の経費申請を作成するファクトリメソッド
	 */
	public static Expense createDraft(Long applicantId, String title, BigDecimal amount, String currency) {
		LocalDateTime now = LocalDateTime.now();
		return new Expense(
			null,  // idはDBが自動採番
			applicantId,
			title,
			amount,
			currency,
			ExpenseStatus.DRAFT,
			null,  // submittedAtは提出時に設定
			now,   // createdAt
			now,   // updatedAt
			0      // version初期値
		);
	}

	/**
	 * 提出可能かチェック
	 */
	public boolean canBeSubmitted() {
		return this.status == ExpenseStatus.DRAFT;
	}

	/**
	 * 承認可能かチェック
	 */
	public boolean canBeApproved() {
		return this.status == ExpenseStatus.SUBMITTED;
	}

	/**
	 * 却下可能かチェック
	 */
	public boolean canBeRejected() {
		return this.status == ExpenseStatus.SUBMITTED;
	}

}
