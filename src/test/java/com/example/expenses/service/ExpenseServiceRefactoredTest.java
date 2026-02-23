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
 * 経費申請API統合テスト（リファクタリング版）
 *
 * 既存のExpenseServiceTestを改善：
 * - @Nestedでグループ化
 * - @DisplayNameで日本語のテスト名
 * - @BeforeEachでテストデータを準備
 * - Given-When-Thenパターン
 * - マジックナンバーを変数に
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("経費申請API")
class ExpenseServiceRefactoredTest {

	@Autowired
	MockMvc mockMvc;

	@Nested
	@DisplayName("経費提出API")
	class SubmitExpenseTest {

		private long draftExpenseId;
		private long submittedExpenseId;
		private long notExistExpenseId;

		@BeforeEach
		void setUp() {
			draftExpenseId = 32L;       // hikaru さんの下書き
			submittedExpenseId = 29L;   // hikaru さんの提出済み
			notExistExpenseId = 9999L;  // 存在しないID
		}

		@Test
		@DisplayName("下書きの経費を提出できる")
		void 下書きの経費を提出できる() throws Exception {
			// Given: 下書きの経費
			long expenseId = draftExpenseId;

			// When: 本人が提出
			var result = mockMvc.perform(
				post("/expenses/{id}/submit", expenseId)
					.with(httpBasic("hikaru@example.com", "pass1234"))
			);

			// Then: 成功
			result.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUBMITTED"))
				.andExpect(jsonPath("$.id").value(expenseId));
		}

		@Test
		@DisplayName("認証なしで提出すると401エラー")
		void 認証なしで提出すると401エラー() throws Exception {
			// Given: 下書きの経費
			long expenseId = draftExpenseId;

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
			// Given: hikaru さんの下書き
			long expenseId = draftExpenseId;

			// When: yasuko さんが提出
			var result = mockMvc.perform(
				post("/expenses/{id}/submit", expenseId)
					.with(httpBasic("yasuko@example.com", "pass1234"))
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

		@Test
		@DisplayName("下書き以外を提出すると409エラー")
		void 下書き以外を提出すると409エラー() throws Exception {
			// Given: 既に提出済みの経費
			long expenseId = submittedExpenseId;

			// When: 再度提出
			var result = mockMvc.perform(
				post("/expenses/{id}/submit", expenseId)
					.with(httpBasic("hikaru@example.com", "pass1234"))
			);

			// Then: 409エラー
			result.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("下書き以外提出できません"));
		}
	}

	@Nested
	@DisplayName("経費承認API")
	class ApproveExpenseTest {

		private long draftExpenseId;
		private long submittedExpenseId;
		private int version;

		@BeforeEach
		void setUp() {
			draftExpenseId = 32L;     // 下書き
			submittedExpenseId = 29L; // 提出済み
			version = 1;
		}

		@Test
		@DisplayName("提出済みの経費を承認できる")
		void 提出済みの経費を承認できる() throws Exception {
			// Given: 提出済みの経費
			long expenseId = submittedExpenseId;

			// When: 承認者が承認
			var result = mockMvc.perform(
				post("/expenses/{id}/approve", expenseId)
					.param("version", String.valueOf(version))
					.with(httpBasic("approver@example.com", "1234"))
			);

			// Then: 成功
			result.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));
		}

		@Test
		@DisplayName("一般ユーザーが承認すると403エラー")
		void 一般ユーザーが承認すると403エラー() throws Exception {
			// Given: 提出済みの経費
			long expenseId = submittedExpenseId;

			// When: 一般ユーザーが承認
			var result = mockMvc.perform(
				post("/expenses/{id}/approve", expenseId)
					.with(httpBasic("hikaru@example.com", "pass1234"))
			);

			// Then: 403エラー
			result.andExpect(status().isForbidden());
		}

		@Test
		@DisplayName("下書きの経費を承認すると409エラー")
		void 下書きの経費を承認すると409エラー() throws Exception {
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
	class RejectExpenseTest {

		private long submittedExpenseId;
		private int version;

		@BeforeEach
		void setUp() {
			submittedExpenseId = 29L;
			version = 1;
		}

		@Test
		@DisplayName("提出済みの経費を却下できる")
		void 提出済みの経費を却下できる() throws Exception {
			// Given: 提出済みの経費、却下理由
			long expenseId = submittedExpenseId;
			String json = """
				{
					"reason": "申請期限切れ"
				}
				""";

			// When: 承認者が却下
			var result = mockMvc.perform(
				post("/expenses/{id}/reject", expenseId)
					.param("version", String.valueOf(version))
					.contentType(MediaType.APPLICATION_JSON)
					.content(json)
					.with(httpBasic("approver@example.com", "1234"))
			);

			// Then: 成功
			result.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"));
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

			// When: 却下
			var result = mockMvc.perform(
				post("/expenses/{id}/reject", submittedExpenseId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(json)
					.with(httpBasic("approver@example.com", "1234"))
			);

			// Then: 400エラー
			result.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"));
		}

		@Test
		@DisplayName("一般ユーザーが却下すると403エラー")
		void 一般ユーザーが却下すると403エラー() throws Exception {
			// Given: 提出済みの経費、却下理由
			String json = """
				{
					"reason": "申請期限切れ"
				}
				""";

			// When: 一般ユーザーが却下
			var result = mockMvc.perform(
				post("/expenses/{id}/reject", submittedExpenseId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(json)
					.with(httpBasic("hikaru@example.com", "pass1234"))
			);

			// Then: 403エラー
			result.andExpect(status().isForbidden());
		}
	}
}
