package com.example.expenses.validation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.expenses.exception.BusinessException;

@Component
public class FileValidator {

	/**
	 * 許可するMIMEタイプのリスト
	 */
	
	private static final List<String> ALLOWED_MIME_TYPES = List.of(
			"image/jpeg",
			"image/ipg",
			"image/png",
			"application/pdf"
	);
	
	/**
	 * 許可する拡張子リスト
	 */
	private static final List<String> ALLOWED_EXTENSIONS = List.of(
			"jpg",
			"jpeg",
			"png",
			"pdf"
	);
	
	/**
	 * 最大ファイルサイズ
	 */
	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    /**
     * 
     * @param file
     * @throws バリデーションエラーの場合
     */
    public void validate(MultipartFile file) {
    	validateNotEmpty(file);
    	validateFileSize(file);
    	validateContentType(file);
    	validateExtension(file);
    }
	
	/**
	 * ファイルが空でないことをチェック
	 * 
	 * @param file アップロードされたファイル
	 * @throws BusinessException バリデーションエラー
	 */
	private void validateNotEmpty(MultipartFile file) {
		if(file == null || file.isEmpty()) {
			throw new BusinessException("ファイルが選択されていません");
		}
		
	}
	
	/**
	 * ファイルサイズをチェック
	 * 
	 * <p>Springの設定でもチェックされるがアプリケーションレベルでも二重チェック</p>
	 */
	private void validateFileSize(MultipartFile file) {
		if(file.getSize() > MAX_FILE_SIZE) {
			throw new BusinessException(
					String.format("ファイルサイズが大きすぎます（最大:%dMB)", MAX_FILE_SIZE/1024/1024));
		}
	}
	
	/**
	 * MIMEタイプをチェック
	 * @param file
	 */
	private void validateContentType(MultipartFile file) {
		String contentType = file.getContentType();
		
		if(!ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
			throw new BusinessException(String.format("サポートされていないファイル形式です（許可：JPG, PNG, PDF）。アップロードされた形式：%s", contentType));
			
		}
	}
	
	/**
	 * ファイル拡張子をチェック
	 * 
	 * <p>MIMEタイプと拡張子の両方をチェックすることで
	 * より堅牢なバリデーションを実現</p>
	 */
	private void validateExtension(MultipartFile file) {
		
		String originalFilename = file.getOriginalFilename();
		
		if(originalFilename == null || originalFilename.isBlank()) {
			throw new BusinessException("ファイル名が不正です");
		}
		
		String extension = getExtension(originalFilename);
		
		if(!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new BusinessException("サポートされていない拡張子です（許可：%s)",
					String.join(",", ALLOWED_EXTENSIONS));
		}
	
	}
	
	/**
	 * ファイル名から拡張子を取得
	 * @param filename
	 * @return 拡張子（ドットなし、小文字）
	 */
	private String getExtension(String filename) {
		
		int lastDotIndex = filename.lastIndexOf(".");
		
		if(lastDotIndex == -1 || lastDotIndex == filename.length() -1) {
			return "";
			
		}
		
		return filename.substring(lastDotIndex + 1);
	}
	
	/**
	 * 安全なファイル名を生成(サニタイズ)
	 *<ul>
	 * 	<li>パストラバーサル攻撃を防ぐため、危険な文字を削除<li>
	 * 	<li>例："../../etc/passwd" -> "etcpasswd"</li>
	 *</ul>
	 *@param originalFilename 元のファイル名
	 *@return サニタイズされたファイル名
	 */
	public String sanitizeFilename(String originalFilename) {
		if(originalFilename == null || originalFilename.isBlank()) {
			return "";
		}
		
		//危険な文字を削除
		return originalFilename
				.replaceAll("[^a-zA-Z0-9.\\-_]", "_")
				.replaceAll("\\.{2,}", ".")
				.replaceAll("_{2,}", "_");
	}
	
}
