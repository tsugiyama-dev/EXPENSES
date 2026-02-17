package com.example.expenses.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class ExpenseVeiwControllerTest {

	@Test
	void approve_withOldVersion_returnsConcurrentModificationError() {
		
		
	}
	
	@Test
	void approve_whenNotSubmitted_returnsInvalidStatusTransitionError() {
		
	}

}
