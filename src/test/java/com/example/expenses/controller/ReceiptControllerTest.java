package com.example.expenses.controller;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.domain.Receipt;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.repository.ReceiptMapper;
import com.example.expenses.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ReceiptController の統合テスト
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>MockMvcを使ったマルチパートファイルアップロードのテスト</li>
 *   <li>ファイルダウンロードのテスト</li>
 *   <li>セキュリティのテスト（@WithMockUser）</li>
 *   <li>統合テストのベストプラクティス</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private ExpenseMapper expenseMapper;

    @Autowired
    private FileStorageService fileStorageService;

    private Long testExpenseId;

    @BeforeEach
    void setUp() {
        // テスト用の経費を作成
        Expense expense = new Expense(
                null,
                1L,
                "テスト経費",
                BigDecimal.valueOf(10000),
                "JPY",
                ExpenseStatus.PENDING,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                0
        );
        expenseMapper.insert(expense);
        testExpenseId = expense.getId();
    }

    @AfterEach
    void tearDown() {
        // テストで作成したファイルをクリーンアップ
        // （@Transactionalによりデータベースはロールバックされる）
    }

    /**
     * 正常系: 領収書のアップロード
     */
    @Test
    @WithMockUser(username = "test@example.com")
    void testUploadReceipt_Success() throws Exception {
        // テスト用のファイルを作成
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // アップロードAPIを呼び出し
        mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", testExpenseId)
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.expenseId", is(testExpenseId.intValue())))
                .andExpect(jsonPath("$.originalFilename", is("receipt.jpg")))
                .andExpect(jsonPath("$.contentType", is("image/jpeg")))
                .andExpect(jsonPath("$.fileSize", greaterThan(0)))
                .andExpect(jsonPath("$.uploadedAt", notNullValue()));

        // データベースに保存されたことを確認
        var receipts = receiptMapper.findByExpenseId(testExpenseId);
        assertEquals(1, receipts.size());
        assertEquals("receipt.jpg", receipts.get(0).getOriginalFilename());
    }

    /**
     * 異常系: ファイルが空
     */
    @Test
    @WithMockUser(username = "test@example.com")
    void testUploadReceipt_EmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", testExpenseId)
                        .file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    /**
     * 異常系: サポートされていないファイル形式
     */
    @Test
    @WithMockUser(username = "test@example.com")
    void testUploadReceipt_UnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/x-msdownload",
                "malicious content".getBytes()
        );

        mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", testExpenseId)
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    /**
     * 異常系: 存在しない経費ID
     */
    @Test
    @WithMockUser(username = "test@example.com")
    void testUploadReceipt_ExpenseNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", 999999L)
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    /**
     * 正常系: 領収書一覧の取得
     */
    @Test
    @WithMockUser(username = "test@example.com")
    void testGetReceipts_Success() throws Exception {
        // テスト用の領収書を2件作成
        createTestReceipt(testExpenseId, "receipt1.jpg");
        createTestReceipt(testExpenseId, "receipt2.pdf");

        // 一覧取得APIを呼び出し
        mockMvc.perform(get("/api/expenses/{expenseId}/receipts", testExpenseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(2)))
                .andExpect(jsonPath("$.receipts", hasSize(2)))
                .andExpect(jsonPath("$.receipts[0].originalFilename", notNullValue()))
                .andExpect(jsonPath("$.receipts[1].originalFilename", notNullValue()));
    }

    /**
     * 正常系: 領収書のダウンロード
     */
    @Test
    @WithMockUser(username = "test@example.com")
    void testDownloadReceipt_Success() throws Exception {
        // テスト用の領収書を作成
        Long receiptId = createTestReceipt(testExpenseId, "receipt.jpg");

        // ダウンロードAPIを呼び出し
        mockMvc.perform(get("/api/receipts/{receiptId}/download", receiptId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("receipt.jpg")))
                .andExpect(content().contentType("image/jpeg"));
    }

    /**
     * 正常系: 領収書の削除
     */
    @Test
    @WithMockUser(username = "test@example.com")
    void testDeleteReceipt_Success() throws Exception {
        // テスト用の領収書を作成
        Long receiptId = createTestReceipt(testExpenseId, "receipt.jpg");

        // 削除APIを呼び出し
        mockMvc.perform(delete("/api/receipts/{receiptId}", receiptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("領収書を削除しました")));

        // データベースから削除されたことを確認
        Receipt deletedReceipt = receiptMapper.findById(receiptId);
        assertNull(deletedReceipt);
    }

    /**
     * 異常系: 存在しない領収書の削除
     */
    @Test
    @WithMockUser(username = "test@example.com")
    void testDeleteReceipt_NotFound() throws Exception {
        mockMvc.perform(delete("/api/receipts/{receiptId}", 999999L))
                .andExpect(status().isBadRequest());
    }

    /**
     * セキュリティテスト: 認証なしでアクセス
     */
    @Test
    void testUploadReceipt_Unauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", testExpenseId)
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    /**
     * テスト用の領収書を作成するヘルパーメソッド
     */
    private Long createTestReceipt(Long expenseId, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                filename.endsWith(".pdf") ? "application/pdf" : "image/jpeg",
                "test content".getBytes()
        );

        var result = mockMvc.perform(multipart("/api/expenses/{expenseId}/receipts", expenseId)
                        .file(file)
                        .with(request -> {
                            request.setRemoteUser("test@example.com");
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        // 簡易的にIDを抽出（本来はJSONパーサーを使うべき）
        var receipts = receiptMapper.findByExpenseId(expenseId);
        return receipts.get(receipts.size() - 1).getId();
    }
}
