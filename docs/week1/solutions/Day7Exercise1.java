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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Day 7 演習1の解答例
 *
 * 経費承認APIの完全なテスト
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("経費承認API")
class Day7Exercise1_ExpenseApproveApiTest {

	@Autowired
	MockMvc mockMvc;

	@Nested
	@DisplayName("正常系")
	class SuccessCase {

		private long submittedExpenseId;
		private int version;

		@BeforeEach
		void setUp() {
			submittedExpenseId = 29L;  // 提出済みの経費
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
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.id").value(expenseId))
				.andExpect(jsonPath("$.version").value(version + 1));
		}
	}

	@Nested
	@DisplayName("異常系 - 認証")
	class AuthenticationError {

		private long submittedExpenseId;

		@BeforeEach
		void setUp() {
			submittedExpenseId = 29L;
		}

		@Test
		@DisplayName("認証なしで承認すると401エラー")
		void 認証なしで承認すると401エラー() throws Exception {
			// Given: 提出済みの経費
			long expenseId = submittedExpenseId;

			// When: 認証なしで承認
			var result = mockMvc.perform(
				post("/expenses/{id}/approve", expenseId)
			);

			// Then: 401エラー
			result.andExpect(status().isUnauthorized());
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
			result.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("アクセスが拒否されました"));
		}
	}

	@Nested
	@DisplayName("異常系 - データ")
	class DataError {

		@Test
		@DisplayName("存在しない経費を承認すると404エラー")
		void 存在しない経費を承認すると404エラー() throws Exception {
			// Given: 存在しないID
			long notExistExpenseId = 9999L;

			// When: 承認
			var result = mockMvc.perform(
				post("/expenses/{id}/approve", notExistExpenseId)
					.with(httpBasic("approver@example.com", "1234"))
			);

			// Then: 404エラー
			result.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("対象データが見つかりません"));
		}
	}

	@Nested
	@DisplayName("異常系 - ビジネスロジック")
	class BusinessLogicError {

		private long draftExpenseId;
		private long approvedExpenseId;
		private long rejectedExpenseId;

		@BeforeEach
		void setUp() {
			draftExpenseId = 32L;     // 下書き
			approvedExpenseId = 25L;  // 承認済み
			rejectedExpenseId = 27L;  // 却下済み（存在する場合）
		}

		@Test
		@DisplayName("下書きの経費を承認すると409エラー")
		void 下書きの経費を承認すると409エラー() throws Exception {
			// Given: 下書きの経費
			long expenseId = draftExpenseId;

			// When: 承認
			var result = mockMvc.perform(
				post("/expenses/{id}/approve", expenseId)
					.with(httpBasic("approver@example.com", "1234"))
			);

			// Then: 409エラー
			result.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("提出済み以外は承認できません"));
		}

		@Test
		@DisplayName("既に承認済みの経費を承認すると409エラー")
		void 既に承認済みの経費を承認すると409エラー() throws Exception {
			// Given: 承認済みの経費
			long expenseId = approvedExpenseId;

			// When: 承認
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
	@DisplayName("異常系 - 楽観的ロック")
	class OptimisticLockError {

		private long submittedExpenseId;
		private int wrongVersion;

		@BeforeEach
		void setUp() {
			submittedExpenseId = 29L;
			wrongVersion = 999;  // 不正なバージョン
		}

		@Test
		@DisplayName("バージョンが一致しない場合は409エラー")
		void バージョンが一致しない場合は409エラー() throws Exception {
			// Given: 提出済みの経費、不正なバージョン
			long expenseId = submittedExpenseId;
			int version = wrongVersion;

			// When: 承認
			var result = mockMvc.perform(
				post("/expenses/{id}/approve", expenseId)
					.param("version", String.valueOf(version))
					.with(httpBasic("approver@example.com", "1234"))
			);

			// Then: 409エラー
			result.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("他のユーザに更新されています"));
		}
	}
}
