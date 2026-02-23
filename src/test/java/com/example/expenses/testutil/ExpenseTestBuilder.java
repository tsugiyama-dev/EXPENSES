package com.example.expenses.testutil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;

/**
 * テストデータビルダーパターン
 *
 * Expenseオブジェクトをテスト用に簡単に作成するためのビルダー
 *
 * 使い方:
 * <pre>
 * // デフォルト値でビルド
 * Expense expense = ExpenseTestBuilder.builder().build();
 *
 * // 一部だけカスタマイズ
 * Expense expense = ExpenseTestBuilder.builder()
 *     .title("ランチ代")
 *     .amount(new BigDecimal("1000"))
 *     .status(ExpenseStatus.SUBMITTED)
 *     .build();
 *
 * // よく使うパターン
 * Expense draft = ExpenseTestBuilder.createDraft();
 * Expense submitted = ExpenseTestBuilder.createSubmitted();
 * Expense approved = ExpenseTestBuilder.createApproved();
 * </pre>
 */
public class ExpenseTestBuilder {

	private Long id = 1L;
	private Long applicantId = 100L;
	private String title = "テスト経費";
	private BigDecimal amount = new BigDecimal("5000");
	private String currency = "JPY";
	private ExpenseStatus status = ExpenseStatus.DRAFT;
	private LocalDateTime submittedAt = LocalDateTime.now();
	private LocalDateTime createdAt = LocalDateTime.now();
	private LocalDateTime updatedAt = LocalDateTime.now();
	private int version = 1;

	private ExpenseTestBuilder() {
		// privateコンストラクタ（builderメソッド経由でのみ作成可能）
	}

	/**
	 * ビルダーのインスタンスを作成
	 */
	public static ExpenseTestBuilder builder() {
		return new ExpenseTestBuilder();
	}

	/**
	 * 経費IDを設定
	 */
	public ExpenseTestBuilder id(Long id) {
		this.id = id;
		return this;
	}

	/**
	 * 申請者IDを設定
	 */
	public ExpenseTestBuilder applicantId(Long applicantId) {
		this.applicantId = applicantId;
		return this;
	}

	/**
	 * タイトルを設定
	 */
	public ExpenseTestBuilder title(String title) {
		this.title = title;
		return this;
	}

	/**
	 * 金額を設定
	 */
	public ExpenseTestBuilder amount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	/**
	 * 金額を設定（long値から）
	 */
	public ExpenseTestBuilder amount(long amount) {
		this.amount = new BigDecimal(amount);
		return this;
	}

	/**
	 * 通貨を設定
	 */
	public ExpenseTestBuilder currency(String currency) {
		this.currency = currency;
		return this;
	}

	/**
	 * ステータスを設定
	 */
	public ExpenseTestBuilder status(ExpenseStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * 提出日時を設定
	 */
	public ExpenseTestBuilder submittedAt(LocalDateTime submittedAt) {
		this.submittedAt = submittedAt;
		return this;
	}

	/**
	 * 作成日時を設定
	 */
	public ExpenseTestBuilder createdAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	/**
	 * 更新日時を設定
	 */
	public ExpenseTestBuilder updatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}

	/**
	 * バージョンを設定
	 */
	public ExpenseTestBuilder version(int version) {
		this.version = version;
		return this;
	}

	/**
	 * Expenseオブジェクトをビルド
	 */
	public Expense build() {
		return new Expense(
			id,
			applicantId,
			title,
			amount,
			currency,
			status,
			submittedAt,
			createdAt,
			updatedAt,
			version
		);
	}

	// ========================================
	// よく使うパターンのファクトリーメソッド
	// ========================================

	/**
	 * 下書き状態の経費を作成
	 */
	public static Expense createDraft() {
		return builder()
			.status(ExpenseStatus.DRAFT)
			.submittedAt(null)
			.build();
	}

	/**
	 * 下書き状態の経費を作成（申請者IDを指定）
	 */
	public static Expense createDraft(Long applicantId) {
		return builder()
			.applicantId(applicantId)
			.status(ExpenseStatus.DRAFT)
			.submittedAt(null)
			.build();
	}

	/**
	 * 提出済み状態の経費を作成
	 */
	public static Expense createSubmitted() {
		return builder()
			.status(ExpenseStatus.SUBMITTED)
			.submittedAt(LocalDateTime.now())
			.build();
	}

	/**
	 * 提出済み状態の経費を作成（申請者IDを指定）
	 */
	public static Expense createSubmitted(Long applicantId) {
		return builder()
			.applicantId(applicantId)
			.status(ExpenseStatus.SUBMITTED)
			.submittedAt(LocalDateTime.now())
			.build();
	}

	/**
	 * 承認済み状態の経費を作成
	 */
	public static Expense createApproved() {
		return builder()
			.status(ExpenseStatus.APPROVED)
			.submittedAt(LocalDateTime.now())
			.version(2)
			.build();
	}

	/**
	 * 承認済み状態の経費を作成（申請者IDを指定）
	 */
	public static Expense createApproved(Long applicantId) {
		return builder()
			.applicantId(applicantId)
			.status(ExpenseStatus.APPROVED)
			.submittedAt(LocalDateTime.now())
			.version(2)
			.build();
	}

	/**
	 * 却下済み状態の経費を作成
	 */
	public static Expense createRejected() {
		return builder()
			.status(ExpenseStatus.REJECTED)
			.submittedAt(LocalDateTime.now())
			.version(2)
			.build();
	}

	/**
	 * 却下済み状態の経費を作成（申請者IDを指定）
	 */
	public static Expense createRejected(Long applicantId) {
		return builder()
			.applicantId(applicantId)
			.status(ExpenseStatus.REJECTED)
			.submittedAt(LocalDateTime.now())
			.version(2)
			.build();
	}
}
