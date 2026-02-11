package com.example.expenses.service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

//@ExtendWith(MockitoExtension.class)
//@WebMvcTest(ExpenseController.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseServiceTest {

	@Autowired
	MockMvc mockMvc;
//	@Mock
//	ExpenseMapper expenseMapper;
//	@InjectMocks
//	ExpenseService expenseService;
//	@Test
//	void testSubmit() throws Exception {
//		long expenseId = 24L;	
//		mockMvc.perform(post("/expenses/{id}/approve", expenseId)
//				.with(httpBasic("hikaru@example.com", "pass1234")))
//				.andExpect(status().isForbidden());
//	}
//	@Test
//	void testApprove() throws Exception{
//		long expenseId = 19L;
//		mockMvc.perform(
//				post("/expenses/{id}/approve", expenseId)
//				.with(httpBasic("approver@example.com", "1234"))
//				).andExpect(status().isOk());
//	}	
//	@Test
//	void approve_user_Is_Forbidden_withErrorCode() throws Exception {
//		long expenseId = 36L;
//		
//		mockMvc.perform(post("/expenses/{id}/approve", expenseId)
//				.with(httpBasic("hikaru@example.com", "pass1234")))
//		
//		.andExpect(status().isForbidden());
////		.andExpect(jsonPath("$.error").value("Forbidden"));
//	}
//	@Test
//	void isUnauthorized() throws Exception {
//		
//		long expenseId = 22L;
//		
//		mockMvc.perform(post("/expense/{id}/approve", expenseId))
//		.andExpect(status().isUnauthorized());
//	}
//	@Test
//	void is_Ok() throws Exception {
//		
//		long expenseId = 29L;
//		
//		mockMvc.perform(post("/expenses/{id}/approve", expenseId)
//				.with(httpBasic("approver@example.com", "1234")))
//		.andExpect(jsonPath("$.status").value("APPROVED"));
//	}
//	@Test
//	void not_submitted_invalid_approved() throws Exception {
//		
//		long expenseId = 37L;
//		
//		mockMvc.perform(post("/expenses/{id}/approve", expenseId)
//				.with(httpBasic("approver@example.com", "1234")))
//		.andExpect(jsonPath("$.details[0].field").value(""));
//	}
//	
//	@Test
//	void success_approve() throws Exception {
//		
//		long expenseId = 38L;
//		mockMvc.perform(post("/expenses/{id}/approve", expenseId).with(httpBasic("approver@example.com","1234")))
//		.andExpect(status().isOk());
//	}
	@Test
	void unAuthentcted_status_401()throws Exception {
		
		long expenseId = 99L;
		
		mockMvc.perform(post("/expenses/{id}/submit", expenseId)).
		andExpect(status().isUnauthorized());
	}
	@Test
	void check_403() throws Exception {
		
		long expenseId = 32L;
		
		mockMvc.perform(post("/expenses/{id}/submit", expenseId)
				.with(httpBasic("hikaru@example.com", "pass1234")))
		.andExpect(status().isConflict())
		.andExpect(jsonPath("$.message").value("本人以外は提出できません"));
		
	
	}
	
	@Test
	void check_409() throws Exception {
		long expenseId = 32L;
		
		mockMvc.perform(post("/expenses/{id}/approve", expenseId)
				.with(httpBasic("approver@example.com", "1234")))
		.andExpect(status().isConflict())
		.andExpect(jsonPath("$.message").value("提出済み以外は承認できません"));
	}
	
	@Test
	void check_400() throws Exception {
		String json = """
				{
				"reason":""
				}
				""";
		
		long expenseId  = 999L;
		
		mockMvc.perform(post("/expenses/{id}/reject", expenseId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json)
				.with(httpBasic("approver@example.com", "1234")))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"));
	}
	
	@Test
	void check_404() throws Exception {
		mockMvc.perform(post("/expenses/{id}/submit", 9999)
				.with(httpBasic("hikaru@example.com","pass1234")))
		.andExpect(status().isNotFound())
		.andExpect(jsonPath("$.message").value("対象データが見つかりません"));

	}
	
	@Test
	void check_200_status() throws Exception {
		long id = 29L;
		
		String json = """
				{
					"reason":"申請期限の締め切り日が過ぎているため承認/申請できません"
				}
				""";
		mockMvc.perform(post("/expenses/{id}/reject", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json)
				.with(httpBasic("approver@example.com", "1234")))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.status").value("REJECTED"));
	}

}
