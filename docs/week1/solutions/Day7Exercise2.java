package com.example.expenses.service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Day 7 演習2の解答例
 *
 * 既存のテストコードをリファクタリングした例
 *
 * 改善点:
 * - @Nested でグループ化
 * - @DisplayName で日本語のテスト名
 * - @BeforeEach でテストデータを準備
 * - Given-When-Thenパターン
 * - マジックナンバーを変数に変更
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("経費申請API（リファクタリング後）")
class Day7Exercise2_ExpenseApiRefactoredTest {

	@Autowired
	MockMvc mockMvc;

	@Nested
	@DisplayName("経費提出API")
	class SubmitTest {

		private long draftExpenseId;
		private long notExistExpenseId;
		private long anyExpenseId;

		@BeforeEach
		void setUp() {
			draftExpenseId = 32L;       // hikaru さんの下書き
			notExistExpenseId = 9999L;  // 存在しないID
			anyExpenseId = 99L;         // 任意のID
		}

		@Test
		@DisplayName("認証なしで提出すると401エラー")
		void 認証なしで提出すると401エラー() throws Exception {
			// Given: 任意の経費ID
			long expenseId = anyExpenseId;

			// When: 認証なしで提出
			var result = mockMvc.perform(
				post("/expenses/{id}/submit", expenseId)
			);

			// Then: 401エラー
			result.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("本人以外が提出すると409エラー")
		void 本人以外が提出すると409エラー() throws Exception {
			// Given: hikaru さんの経費
			long expenseId = draftExpenseId;

			// When: hikaru さん本人が提出しようとする（テストデータの都合で409になる）
			var result = mockMvc.perform(
				post("/expenses/{id}/submit", expenseId)
					.with(httpBasic("hikaru@example.com", "pass1234"))
			);

			// Then: 409エラー
			result.andExpect(status().isConflict())
				  .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
		}

		@Test
		@DisplayName("存在しない経費を提出すると404エラー")
		void 存在しない経費を提出すると404エラー() throws Exception {
			// Given: 存在しないID
			long expenseId = notExistExpenseId;

			// When: 提出
			var result = mockMvc.perform(
				post("/expenses/{id}/submit", expenseId)
					.with(httpBasic("hikaru@example.com", "pass1234"))
			);

			// Then: 404エラー
			result.andExpect(status().isNotFound())
				  .andExpect(jsonPath("$.message").value("対象データが見つかりません"));
		}
	}

	@Nested
	@DisplayName("経費承認API")
	class ApproveTest {

		private long draftExpenseId;

		@BeforeEach
		void setUp() {
			draftExpenseId = 32L;  // 下書き
		}

		@Test
		@DisplayName("提出済み以外を承認すると409エラー")
		void 提出済み以外を承認すると409エラー() throws Exception {
			// Given: 下書きの経費
			long expenseId = draftExpenseId;

			// When: 承認者が承認
			var result = mockMvc.perform(
				post("/expenses/{id}/approve", expenseId)
					.with(httpBasic("approver@example.com", "1234"))
			);

			// Then: 409エラー
			result.andExpect(status().isConflict())
				  .andExpect(jsonPath("$.message").value("提出済み以外は承認できません"));
		}
	}

	@Nested
	@DisplayName("経費却下API")
	class RejectTest {

		private long submittedExpenseId;
		private long invalidExpenseId;

		@BeforeEach
		void setUp() {
			submittedExpenseId = 29L;  // 提出済み
			invalidExpenseId = 999L;   // 存在しないor不正なID
		}

		@Test
		@DisplayName("却下理由が空の場合は400エラー")
		void 却下理由が空の場合は400エラー() throws Exception {
			// Given: 空の却下理由
			String json = """
				{
					"reason": ""
				}
				""";
			long expenseId = invalidExpenseId;

			// When: 却下
			var result = mockMvc.perform(
				post("/expenses/{id}/reject", expenseId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(json)
					.with(httpBasic("approver@example.com", "1234"))
			);

			// Then: 400エラー
			result.andExpect(status().isBadRequest())
				  .andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"));
		}

		@Test
		@DisplayName("提出済みの経費を却下できる")
		void 提出済みの経費を却下できる() throws Exception {
			// Given: 提出済みの経費と却下理由
			long expenseId = submittedExpenseId;
			String json = """
				{
					"reason": "申請期限の締め切り日が過ぎているため承認/申請できません"
				}
				""";

			// When: 承認者が却下
			var result = mockMvc.perform(
				post("/expenses/{id}/reject", expenseId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(json)
					.with(httpBasic("approver@example.com", "1234"))
			);

			// Then: 成功
			result.andExpect(status().isOk())
				  .andExpect(jsonPath("$.status").value("REJECTED"));
		}
	}
}
