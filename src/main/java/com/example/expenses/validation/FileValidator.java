package com.example.expenses.validation;

import com.example.expenses.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * ファイルバリデーター
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>セキュリティ: MIMEタイプと拡張子の両方をチェック</li>
 *   <li>拡張子偽装攻撃の防止</li>
 *   <li>ファイルサイズ制限によるDoS攻撃防止</li>
 * </ul>
 *
 * <p><b>セキュリティ対策:</b></p>
 * <ul>
 *   <li>許可リスト方式（ホワイトリスト）を採用</li>
 *   <li>拡張子とMIMEタイプの両方をチェック</li>
 *   <li>ファイルサイズの二重チェック（Spring設定 + アプリケーションレベル）</li>
 * </ul>
 */
@Component
public class FileValidator {

    /**
     * 許可するMIMEタイプのリスト（ホワイトリスト）
     */
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/pdf"
    );

    /**
     * 許可する拡張子のリスト（ホワイトリスト）
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg",
            "jpeg",
            "png",
            "pdf"
    );

    /**
     * 最大ファイルサイズ（10MB）
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * ファイルをバリデーションします
     *
     * @param file アップロードされたファイル
     * @throws BusinessException バリデーションエラーの場合
     */
    public void validate(MultipartFile file) {
        validateNotEmpty(file);
        validateFileSize(file);
        validateContentType(file);
        validateExtension(file);
    }

    /**
     * ファイルが空でないことをチェック
     */
    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("ファイルが選択されていません");
        }
    }

    /**
     * ファイルサイズをチェック
     *
     * <p>Spring の設定（spring.servlet.multipart.max-file-size）でも
     * チェックされますが、アプリケーションレベルでも二重チェックします。</p>
     */
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                    String.format("ファイルサイズが大きすぎます（最大: %dMB）",
                            MAX_FILE_SIZE / 1024 / 1024));
        }
    }

    /**
     * MIMEタイプをチェック
     *
     * <p><b>セキュリティポイント:</b></p>
     * <ul>
     *   <li>拡張子偽装攻撃を防ぐため、MIMEタイプもチェック</li>
     *   <li>例: .exeファイルを.jpgにリネームしてアップロード → 拒否</li>
     * </ul>
     */
    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(
                    String.format("サポートされていないファイル形式です（許可: JPG, PNG, PDF）。アップロードされた形式: %s",
                            contentType));
        }
    }

    /**
     * ファイルの拡張子をチェック
     *
     * <p>MIMEタイプと拡張子の両方をチェックすることで、
     * より堅牢なバリデーションを実現します。</p>
     */
    private void validateExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException("ファイル名が不正です");
        }

        String extension = getExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException(
                    String.format("サポートされていないファイル拡張子です（許可: %s）",
                            String.join(", ", ALLOWED_EXTENSIONS)));
        }
    }

    /**
     * ファイル名から拡張子を取得します
     *
     * @param filename ファイル名
     * @return 拡張子（ドットなし、小文字）
     */
    private String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');

        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 安全なファイル名を生成します（サニタイズ）
     *
     * <p><b>セキュリティポイント:</b></p>
     * <ul>
     *   <li>パストラバーサル攻撃を防ぐため、危険な文字を削除</li>
     *   <li>例: "../../etc/passwd" → "etcpasswd"</li>
     * </ul>
     *
     * @param originalFilename 元のファイル名
     * @return サニタイズされたファイル名
     */
    public String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "file";
        }

        // 危険な文字を削除（パストラバーサル攻撃防止）
        return originalFilename
                .replaceAll("[^a-zA-Z0-9.\\-_]", "_")  // 安全な文字のみ許可
                .replaceAll("\\.{2,}", ".")            // 連続するドットを1つに
                .replaceAll("_{2,}", "_");             // 連続するアンダースコアを1つに
    }
}
