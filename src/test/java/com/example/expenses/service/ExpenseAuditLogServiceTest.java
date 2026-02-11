package com.example.expenses.service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseAuditLogServiceTest {

	@Autowired
	MockMvc mockMvc;
	
	@Test
	void isOwnerGetLogs_isSuccess() throws Exception{
		long expenseId = 33L;
		
		mockMvc.perform(get("/expenses/{id}/audit-logs", expenseId).
				with(httpBasic("hikaru@example.com", "pass1234")))
		.andExpect(status().isOk());
	}
	
	@Test
	void unAuthorized_getLogs() throws Exception {
		mockMvc.perform(get("/expenses/{id}/audit-logs", 33L))
		.andExpect(status().isUnauthorized());
	}
	
	@Test
	void isApprover_getLogs_success() throws Exception {
		long expenseId = 39L;
		
		mockMvc.perform(get("/expenses/{id}/audit-logs", expenseId)
				.with(httpBasic("approver@example.com", "1234")))
		.andExpect(status().isOk());
	}
	
	@Test
	void notIsOwner_getLog_forbidden() throws Exception {
		mockMvc.perform(get("/expenses/{id}/audit-logs", 39L)
				.with(httpBasic("yasuko@example.com", "pass1234")))
		.andExpect(status().isConflict());//
	}

}
