package com.example.expenses.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("経費提出API")
public class ExpenseControllerTest {

	@Autowired
	MockMvc mockMvc;
	
	@Test
	@DisplayName("経費一覧を取得")
	void 経費一覧を取得() throws Exception {
		//when & then
		mockMvc.perform(get("/expenses")
				.with(httpBasic("hikaru@example.com", "pass1234")))//認証
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.items").isArray())
		.andExpect(jsonPath("$.items[0].id").value(116))
		.andExpect(jsonPath("$.currentPage").value(1))
		.andExpect(jsonPath("$.pageNumbers.size()").value(5));
	}
	
	@Test
	@DisplayName("経費を作成できる")
	void 経費を作成できる() throws Exception {
		//Given リクエストボディ
		
		String json = """
				{
					"title": "タクシー代",
					"amount": 5000,
					"currency": "JPY"
				}
				""";
		
		//when & then
		mockMvc.perform(post("/expenses")
				.with(httpBasic("hikaru@example.com", "pass1234"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
		.andDo(print())
		.andExpect(status().isCreated())
		.andExpect(jsonPath("$.id").exists())
		.andExpect(jsonPath("$.title").value("タクシー代"))
		.andExpect(jsonPath("$.amount").value(5000));
	}
	
	@Test
	@DisplayName("認証なしで経費取得をすると500エラー")
	void 認証なしで経費取得するとサーバーエラー() throws Exception {
		
		mockMvc.perform(get("/expenses")).andExpect(status().isInternalServerError());
	}
	
	@Test
	@DisplayName("一般ユーザーが認証すると４０３エラー")
	void 一般ユーザーが認証すると４０３エラー() throws Exception {
		
		mockMvc.perform(post("/expenses/{expenseId}/approve", 29L)
				.with(httpBasic("hikaru@example.com", "pass1234")))
		.andExpect(status().isForbidden());
	}
	
	@Test
	@DisplayName("存在しない経費を提出すると４０４エラー")
	void 存在しない経費を提出すると４０４エラー() throws Exception {
		
		mockMvc.perform(post("/expenses/{expenseId}/submit", 999L)
				.with(httpBasic("hikaru@example.com", "pass1234"))
				.with(csrf()))
		.andExpect(status().isNotFound())
		.andExpect(jsonPath("$.details[0].message").value("Expense not found: 999"));
	}
	
	@Test
	@DisplayName("下書き以外を提出すると４０９エラー")
	void 下書き以外を提出すると４０９エラー() throws Exception {
		
		Long submittedExpenseId = 62L;
		
		mockMvc.perform(post("/expenses/{submittedExpenseId}/submit", submittedExpenseId)
				.with(httpBasic("hikaru@example.com", "pass1234"))
				.with(csrf()))
		.andExpect(status().isConflict())
		.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
		.andExpect(jsonPath("$.message").value("下書き以外提出できません"));
	}
	
	@Nested
	@DisplayName("経費却下API")
	class RejectExpenseTest {
		
		@Test
		@DisplayName("承認者が却下処理をする")
		void 承認者が却下処理をする() throws Exception {
			
			String reason = """
				{"reason": "却下理由"}
				""";
			
			mockMvc.perform(post("/expenses/{expenseId}/reject", 136L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(reason)
					.param("version", "0")
					.with(httpBasic("approver@example.com", "1234"))
					.with(csrf()))
			.andExpect(status().isOk());
		}
		
		@Test
		@DisplayName("却下理由が空で４００エラー")
		void 却下理由が空で４００エラー() throws Exception {
			
			String reason = """
				{"reason": ""}
				""";
			
			mockMvc.perform(post("/expenses/{expenseId}/reject", 136L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(reason)
					.param("version", "0")
					.with(httpBasic("approver@example.com", "1234"))
					.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"));
		}
		
		@Test
		@DisplayName("認証なしで４０１エラー")
		void 認証なしで４０１エラー() throws Exception {
			
			String reason = """
				{"reason": "却下理由"}
				""";
			
			mockMvc.perform(post("/expenses/{expenseId}/reject", 136L)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(reason))
			.andExpect(status().isUnauthorized());
		}
		
		@Test
		@DisplayName("一般ユーザーで却下しようとした場合４０３エラー")
		void 一般ユーザーで却下しようとした場合４０３エラー() throws Exception {
			String reason = """
				{"reason": "却下理由"
				""";
			
			mockMvc.perform(post("/expenses/{expenseId}/reject", 136L)
					.with(httpBasic("hikaru@example.com", "pass1234"))
					.with(csrf())
					.param("version", "0")
					.contentType(MediaType.APPLICATION_JSON)
					.content(reason))
//		.andExpect(status().isOk());
			.andExpect(status().isForbidden()); //SecurityFilterで制御
			
		}
		
		@Test
		@DisplayName("提出済み以外は４０９エラー")
		void 提出済み以外は４０９エラー() throws Exception {
			
			String reason = """
				{"reason": "却下理由"}
				""";
			
			mockMvc.perform(post("/expenses/{expenseId}/reject", 116L)
					.with(httpBasic("approver@example.com", "1234"))
					.with(csrf())
					.param("version", "0")
					.contentType(MediaType.APPLICATION_JSON)
					.content(reason))
			.andExpect(status().isConflict());
			
		}
	}
}
