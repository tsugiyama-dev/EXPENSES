package com.example.expenses.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.expenses.config.LoginUser;
import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.domain.Receipt;
import com.example.expenses.domain.Role;
import com.example.expenses.domain.User;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.repository.ReceiptMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReceiptControllerTest {

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private ExpenseMapper expenseMapper;
	@Autowired
	private ReceiptMapper receiptMapper;
	
	private Long testExpenseId;
	private LoginUser loginUser;
	
	@BeforeEach
	void setUp() {
		
		//CustomUserDetailを実装している場合WithMockUserは使えない。
		User user = new User();
		user.setEmail("test@example.com");
		user.setPassword("1234");
		user.setId(123L);
		Role role = new Role();
		role.setRole("ROLE_USER");
		user.setRoles(List.of(role));
		
	    loginUser = new LoginUser(user);
		
		
		//Test用の経費を作成
		Expense expense = new Expense(
				null, 
				1L,
				"テスト経費",
				BigDecimal.valueOf(10000),
				"JPY",
				ExpenseStatus.REJECTED,
				null,
				LocalDateTime.now(),
				LocalDateTime.now(),
				0);
		
		expenseMapper.insert(expense);
		testExpenseId = expense.getId();
		
	}
	
	@AfterEach
	void tearDown() {
		// テストで作成したファイルをクリーンアップ
		// (@Transactonalによりデータベースはロールバックされる)
	}
	
	@Test
//	@WithMockUser(username = "test@example.com", password = "1234")
	void testUploadReceipt_Success() throws Exception {
		//テスト用のファイルを作成
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"receipt.jpg",
				"image/jpeg",
				"test image content".getBytes());
		
		//アップロードAPIを呼び出し
		mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", testExpenseId)
				.file(file).with(csrf()).with(user(loginUser)))
		.andExpect(status().isCreated())
		.andExpect(jsonPath("$.id", notNullValue()))
		.andExpect(jsonPath("$.expenseId", is(testExpenseId.intValue())))
		.andExpect(jsonPath("$.originalFilename", is("receipt.jpg")))
		.andExpect(jsonPath("$.contentType", is("image/jpeg")))
		.andExpect(jsonPath("$.fileSize", greaterThan(0)))
		.andExpect(jsonPath("$.uploadedAt", notNullValue()));
		
		var receipts = receiptMapper.findByExpenseId(testExpenseId);
		assertEquals(1, receipts.size());
		assertThat(receipts.get(0).getOriginalFilename()).isEqualTo("receipt.jpg");
	}
	
	/**
	 * 異常系：ファイルが空
	 */
	@Test
	void testUploadReceipt_EmptyFile() throws Exception {
		MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
		
		mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", testExpenseId)
				.file(emptyFile).with(csrf()).with(user(loginUser)))
		.andExpect(status().isConflict());
				
	}
	
	/**
	 * 異常系：ファイルサイズが許容する値を超える
	 */
	@Test
	void testUploadReceipt_MoreThanMaxSize() throws Exception {
		
		int size = 1024 * 1024 * 11;
		MockMultipartFile moreThanMaxSize = new MockMultipartFile(
				"file",
				"moreThanMaxSize.jpeg",
				"image/jpeg",
				new byte[size]);
		
		mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", testExpenseId).with(csrf()).with(user(loginUser)).file(moreThanMaxSize))
		.andExpect(status().isConflict())
		.andExpect(jsonPath("$.message", startsWith("ファイルサイズが大きすぎます")));
	}
	
	/**
	 * supportされていないファイル形式
	 */
	@Test
	void testUploadReceipt_UnsupportedFileType() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.exe",
				"text/csv",
				"malicious content".getBytes());
		
		mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", testExpenseId)
				.file(file).with(csrf()).with(user(loginUser)))
		.andExpect(status().isConflict())
		.andExpect(jsonPath("$.message", startsWith("サポートされていないファイル形式です")));
		
	}
	
	/**
	 * 存在しない経費ＩＤ
	 */
	@Test
	void testUploadReceipt_expenseNotFound() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"receipt.jpg",
				"image/jpeg",
				"test content".getBytes());
		
		mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", 99999).
				file(file).with(user(loginUser)).with(csrf()))
		.andExpect(status().isConflict());
	}
	
	/**
	 * 正常系：領収書一覧の取得
	 */
	@Test
	void testGetReceipts_Success() throws Exception {
		//test用領収書の作成（挿入）
		createTestReceipt(testExpenseId, "receipt1.jpg");
		createTestReceipt(testExpenseId, "receipt2.pdf");
		
		mockMvc.perform(get("/api/expenses/{expenseId}/receipts", testExpenseId).with(csrf()).with(user(loginUser)))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.count", is(2)))
		.andExpect(jsonPath("$.receipts", hasSize(2)))
		.andExpect(jsonPath("$.receipts[0].originalFilename", notNullValue()))
		.andExpect(jsonPath("$.receipts[1].originalFilename", notNullValue()));
	}
	
	/**
	 * 正常系：領収書のダウンロード
	 * @throws Exception
	 */
	@Test
	void testDownloadReceipt_Success() throws Exception {
		//
		Long receiptId = createTestReceipt(testExpenseId, "receipt.jpg");
		
		mockMvc.perform(get("/api/receipts/{receiptId}/download", receiptId)
				.with(user(loginUser)).with(csrf()))
		.andExpect(status().isOk())
		.andExpect(header().string("Content-Disposition", containsString("receipt.jpg")))
		.andExpect(content().contentType("image/jpeg"));
	}
	
	/**
	 * 正常系：領収書の削除
	 */
	@Test
	void testDeleteReceipt_Success() throws Exception {
		// Test用の領収書を作成
		Long receiptId = createTestReceipt(testExpenseId, "receipt.jpg");
		
		//削除APIの呼び出し
		mockMvc.perform(delete("/api/receipts/{receiptId}", receiptId).with(user(loginUser)).with(csrf()))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.message", is("領収書を削除しました")));
		
		//データベースから削除されたことを確認
		Receipt deleteReceipt = receiptMapper.findById(receiptId);
		assertNull(deleteReceipt);
	}
	
	/**
	 * 異常系：存在しない領収書の削除
	 */
	@Test
	void testDeleteReceipt_NotFound() throws Exception {
		mockMvc.perform(delete("/api/receipts/{receiptId}", 999999L).with(csrf()).with(user(loginUser)))
		.andExpect(status().isConflict());
	}
	
	/**
	 * セキュリティテスト：認証なしでアクセス
	 */
	@Test
	void testuploadRedceipt_Unauthorized() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"receipt.jpg",
				"image/jpeg",
				"test content".getBytes());
		
		mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", testExpenseId).file(file).with(csrf()))
		.andExpect(status().isUnauthorized());
	}
	/**
	 * 
	 * @param expenseId
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	
	private Long createTestReceipt(Long expenseId, String filename) throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				filename,
				filename.endsWith(".pdf") ? "application/pdf" : "image/jpeg",
			    "test content".getBytes()
			    );
		
//		var result = 
				mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", expenseId)
				.file(file)
				.with(user(loginUser)).with(csrf())
				)
				.andExpect(status().isCreated());
//				.andReturn();
		
		var receipts = receiptMapper.findByExpenseId(expenseId);
		return receipts.get(receipts.size() - 1).getId();
	
	
	
	}

	

}
