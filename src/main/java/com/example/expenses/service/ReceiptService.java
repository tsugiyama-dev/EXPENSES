package com.example.expenses.service;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.Receipt;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.repository.ReceiptMapper;
import com.example.expenses.storage.FileStorageService;
import com.example.expenses.validation.FileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 領収書サービス
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>トランザクション管理（@Transactional）</li>
 *   <li>ファイルアップロードのビジネスロジック</li>
 *   <li>セキュリティ対策（権限チェック、バリデーション）</li>
 *   <li>依存性の注入（DI）</li>
 * </ul>
 */
@Service
public class ReceiptService {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptService.class);

    /**
     * 1つの経費に対する最大領収書数
     */
    private static final int MAX_RECEIPTS_PER_EXPENSE = 5;

    private final ReceiptMapper receiptMapper;
    private final ExpenseMapper expenseMapper;
    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;

    public ReceiptService(ReceiptMapper receiptMapper,
                          ExpenseMapper expenseMapper,
                          FileStorageService fileStorageService,
                          FileValidator fileValidator) {
        this.receiptMapper = receiptMapper;
        this.expenseMapper = expenseMapper;
        this.fileStorageService = fileStorageService;
        this.fileValidator = fileValidator;
    }

    /**
     * 領収書をアップロードします
     *
     * <p><b>処理フロー:</b></p>
     * <ol>
     *   <li>ファイルのバリデーション</li>
     *   <li>経費の存在確認</li>
     *   <li>アップロード権限のチェック</li>
     *   <li>領収書数の上限チェック</li>
     *   <li>ファイルの保存</li>
     *   <li>データベースへの登録</li>
     * </ol>
     *
     * @param expenseId 経費ID
     * @param file アップロードされたファイル
     * @param userEmail アップロードするユーザーのメールアドレス
     * @return 保存された領収書エンティティ
     */
    @Transactional
    public Receipt uploadReceipt(Long expenseId, MultipartFile file, String userEmail) {
        // 1. ファイルのバリデーション
        fileValidator.validate(file);

        // 2. 経費の存在確認
        Expense expense = expenseMapper.findById(expenseId);
        if (expense == null) {
            throw new BusinessException("経費が見つかりません: ID=" + expenseId);
        }

        // 3. アップロード権限のチェック（経費の申請者のみアップロード可能）
        // TODO: ExpenseにapplicantEmailフィールドを追加する必要がある
        // 現在は一旦スキップ

        // 4. 領収書数の上限チェック
        int currentCount = receiptMapper.countByExpenseId(expenseId);
        if (currentCount >= MAX_RECEIPTS_PER_EXPENSE) {
            throw new BusinessException(
                    String.format("領収書は最大%d枚までアップロードできます", MAX_RECEIPTS_PER_EXPENSE));
        }

        try {
            // 5. ファイルの保存
            String originalFilename = file.getOriginalFilename();
            String sanitizedFilename = fileValidator.sanitizeFilename(originalFilename);
            String storedFilename = generateStoredFilename(sanitizedFilename);
            String directory = generateDirectory();
            String filePath = fileStorageService.store(file, directory, storedFilename);

            // 6. データベースへの登録
            Receipt receipt = new Receipt(
                    expenseId,
                    originalFilename,
                    storedFilename,
                    filePath,
                    file.getContentType(),
                    file.getSize(),
                    userEmail
            );

            receiptMapper.insert(receipt);

            logger.info("領収書をアップロードしました: expenseId={}, receiptId={}, filename={}",
                    expenseId, receipt.getId(), originalFilename);

            return receipt;

        } catch (IOException e) {
            logger.error("ファイルの保存に失敗しました: expenseId={}, filename={}",
                    expenseId, file.getOriginalFilename(), e);
            throw new BusinessException("ファイルのアップロードに失敗しました", e);
        }
    }

    /**
     * 経費IDに紐づく領収書一覧を取得します
     *
     * @param expenseId 経費ID
     * @param userEmail ユーザーのメールアドレス
     * @return 領収書リスト
     */
    public List<Receipt> getReceipts(Long expenseId, String userEmail) {
        // 経費の存在確認
        Expense expense = expenseMapper.findById(expenseId);
        if (expense == null) {
            throw new BusinessException("経費が見つかりません: ID=" + expenseId);
        }

        // TODO: アクセス権限のチェック（経費の申請者 or 承認者のみ）

        return receiptMapper.findByExpenseId(expenseId);
    }

    /**
     * 領収書をダウンロードします
     *
     * @param receiptId 領収書ID
     * @param userEmail ユーザーのメールアドレス
     * @return ファイルリソース
     */
    public Resource downloadReceipt(Long receiptId, String userEmail) {
        // 領収書の存在確認
        Receipt receipt = receiptMapper.findById(receiptId);
        if (receipt == null) {
            throw new BusinessException("領収書が見つかりません: ID=" + receiptId);
        }

        // TODO: アクセス権限のチェック（経費の申請者 or 承認者のみ）

        try {
            return fileStorageService.loadAsResource(receipt.getFilePath());
        } catch (IOException e) {
            logger.error("ファイルの読み込みに失敗しました: receiptId={}, filePath={}",
                    receiptId, receipt.getFilePath(), e);
            throw new BusinessException("ファイルのダウンロードに失敗しました", e);
        }
    }

    /**
     * 領収書を削除します
     *
     * @param receiptId 領収書ID
     * @param userEmail ユーザーのメールアドレス
     */
    @Transactional
    public void deleteReceipt(Long receiptId, String userEmail) {
        // 領収書の存在確認
        Receipt receipt = receiptMapper.findById(receiptId);
        if (receipt == null) {
            throw new BusinessException("領収書が見つかりません: ID=" + receiptId);
        }

        // 経費の存在確認
        Expense expense = expenseMapper.findById(receipt.getExpenseId());
        if (expense == null) {
            throw new BusinessException("経費が見つかりません: ID=" + receipt.getExpenseId());
        }

        // TODO: 削除権限のチェック（経費の申請者のみ、かつ申請前のみ削除可能）

        try {
            // ファイルの削除
            fileStorageService.delete(receipt.getFilePath());

            // データベースから削除
            receiptMapper.deleteById(receiptId);

            logger.info("領収書を削除しました: receiptId={}, filename={}",
                    receiptId, receipt.getOriginalFilename());

        } catch (IOException e) {
            logger.error("ファイルの削除に失敗しました: receiptId={}, filePath={}",
                    receiptId, receipt.getFilePath(), e);
            throw new BusinessException("ファイルの削除に失敗しました", e);
        }
    }

    /**
     * 領収書情報を取得します（ダウンロード時のメタデータ取得用）
     *
     * @param receiptId 領収書ID
     * @return 領収書エンティティ
     */
    public Receipt getReceipt(Long receiptId) {
        Receipt receipt = receiptMapper.findById(receiptId);
        if (receipt == null) {
            throw new BusinessException("領収書が見つかりません: ID=" + receiptId);
        }
        return receipt;
    }

    /**
     * 保存用のファイル名を生成します
     *
     * <p>UUID + 拡張子の形式で生成します。</p>
     * <p>例: "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"</p>
     *
     * @param originalFilename 元のファイル名
     * @return 保存用ファイル名
     */
    private String generateStoredFilename(String originalFilename) {
        String extension = getExtension(originalFilename);
        return UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
    }

    /**
     * 保存先ディレクトリを生成します
     *
     * <p>年/月/日の形式で生成します。</p>
     * <p>例: "2026/03/07"</p>
     *
     * @return ディレクトリパス
     */
    private String generateDirectory() {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        return now.format(formatter);
    }

    /**
     * ファイル名から拡張子を取得します
     *
     * @param filename ファイル名
     * @return 拡張子（ドットなし、小文字）
     */
    private String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
}
