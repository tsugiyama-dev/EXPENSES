package com.example.expenses.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.Receipt;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.repository.ReceiptMapper;
import com.example.expenses.storage.FileStorageService;
import com.example.expenses.validation.FileValidator;

@Service
public class ReceiptService {

	private static Logger logger = LoggerFactory.getLogger(ReceiptService.class);
	
	private static final int MAX_RECEIPTS_PER_EXPENSE = 5;
	
	private final ReceiptMapper receiptMapper;
	private final ExpenseMapper expenseMapper;
	private final FileStorageService fileStorageService;
	private final FileValidator fileValidator;
	private final AuthenticationContext authenticationContext;
	
	public ReceiptService(ReceiptMapper receiptMapper,
						ExpenseMapper expenseMapper,
						FileStorageService fileStorageService,
						FileValidator fileValidator,
						AuthenticationContext authenticationContext) {
		this.receiptMapper = receiptMapper;
		this.expenseMapper = expenseMapper;
		this.fileStorageService = fileStorageService;
		this.fileValidator = fileValidator;
		this.authenticationContext = authenticationContext;
		
	}
	
	/**
	 * 領収書をアップロード
	 */
	
	
	/**
	 * 
	 * @param expenseId
	 * @param file
	 * @param userEmail
	 * @return
	 */
	@Transactional
	public Receipt uploadReceipt(Long expenseId, MultipartFile file, String userEmail) {
		// ファイルバリデーション
		fileValidator.validate(file);
		
		// 経費の存在確認
		Expense expense = expenseMapper.findById(expenseId);
		if(expense == null) {
			throw new BusinessException("経費が見つかりません：ＩＤ=" + expenseId, "");
		}
		
		//領収書の上限チェック
		int currentCount = receiptMapper.countByExpenseId(expenseId);
		if(currentCount >= MAX_RECEIPTS_PER_EXPENSE) {
			throw new BusinessException(String.format("領収書は最大%d枚までアップロードできます", MAX_RECEIPTS_PER_EXPENSE),"");
		}
		
		try {
			// ファイルの保存
			String originalFilename = file.getOriginalFilename();
			String sanitizedFilename = fileValidator.sanitizeFilename(originalFilename);
			String storedFilename = generateStoredFilename(sanitizedFilename);
			String directory = generateDirectory();
			String filePath = fileStorageService.store(file, directory, storedFilename);
			
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
			
			logger.info("領収書をアップロードしました：expenseId={}, receiptId={}, filename={}",
					expenseId, receipt.getId(), originalFilename);
			
			return receipt;
			
		}catch(IOException e) {
			logger.error("ファイルの保存に失敗しました：expenseId={}, filename={}",
					expenseId, file.getOriginalFilename());
			
			throw new BusinessException("CODE_999" ,"ファイルのアップロードに失敗しました", e);
		}
	}
	
	/**
	 * 経費ＩＤに紐づく領収書一覧の取得
	 * @param expenseId
	 * @param userEmail
	 * @return 領収書のリスト
	 */
	public List<Receipt> getReceipts(Long expenseId, String userEmail) {
		
		
		// 経費の存在確認
		Expense expense = expenseMapper.findById(expenseId);
		if(expense == null) {
			throw new BusinessException("NOT_FOUND", "経費が見つかりません");
		}
		
		//TODO: アクセス権限のチェック（経費の申請者 or 承認者のみ）
		if(!authenticationContext.isOwnerOrApprover(expense.getApplicantId())) {
			
			throw new BusinessException(/**"ERROR_999",*/ "本人または承認者以外は閲覧できません");
		
		}
		
		return receiptMapper.findByExpenseId(expenseId);
	}
	
	/**
	 * 
	 * @param receiptId
	 * @param userEmail
	 * @return 領収書のデータ
	 */
    public Resource downloadReceipt(Long receiptId, String userEmail) {
    	//領収書の存在確認
    	Receipt receipt = receiptMapper.findById(receiptId);
    	if(receipt == null) {
    		throw new BusinessException("NOT_FOUND", "領収書が見つかりません");
    	}
    	
    	Expense expense = expenseMapper.findById(receipt.getExpenseId());
    	

    	if(!authenticationContext.isOwnerOrApprover(expense.getApplicantId())) {
    		throw new BusinessException("ERROR_999", "本人または承認者以外はダウンロードできません");
    	}
    	
    	try {
    		 return fileStorageService.loadAsResource(receipt.getFilePath());
    	
    	}catch(IOException e) {
    		throw new BusinessException("DOWNLOAD_ERROR","領収書のダウンロードに失敗しました", e);
    	}

    }
    
    /**
     * 
     * @param receiptId
     */
    @Transactional
    public void deleteReceipt(Long receiptId, String userEmail) {
    	
    	//領収書の存在確認
    	Receipt receipt = receiptMapper.findById(receiptId);
    	if(receipt == null) {
    		throw new BusinessException("NOT_FOUND", "領収書が存在しません");
    	}
    	
    	// TODO: 削除権限のチェック（経費の申請者のみ、かつ申請前のみ削除可能）
    	Expense expense = expenseMapper.findById(receipt.getExpenseId());
    	if(authenticationContext.isOwnerOrApprover(expense.getApplicantId())) {
    		throw new BusinessException("ERROR_999", "本人または承認者以外はダウンロードできません");
    	}
    	try {
    		// ファイルの削除
    		fileStorageService.delete(receipt.getFilePath());
    		// データベースから削除
    		receiptMapper.deleteById(receiptId);
    		
    	}catch (IOException e) {
    		logger.error("領収書の削除に失敗しました.receiptId={}", receiptId);
    		
    		throw new BusinessException("","ファイルの削除に失敗しました",e);
    	}
    	
    }
    
    /**
     * 
     * @param receiptId
     * @return receiptIdに紐づいた領収書
     */
    public Receipt getReceipt(Long receiptId) {
    	Receipt receipt = receiptMapper.findById(receiptId);
    	if(receipt == null) {
    		throw new BusinessException("NOT_FOUND", "領収書が見つかりません；receiptId=" + receiptId );
    	}
    	
    	return receipt;
    }
    
    /**
     * 
     * @param originalFilename
     * @return 保存用のファイル名(UUID + 拡張子)
     */
    private String generateStoredFilename(String originalFilename) {
    	String extension = getExtension(originalFilename);
    	return UUID.randomUUID().toString() + "." +  extension;
    }
    
    /**
     * <p>年月日の形式で生成</p>
     * <p>例: "yyyy/MM/dd"</p>
     * 
     * @return ディレクトリパス
     */
    private String generateDirectory() {
    	LocalDate now = LocalDate.now();
    	DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    	
    	return now.format(format);
    }
    
    /**
     * 
     * @param filename
     * @return 拡張子
     */
    private String getExtension(String filename) {
    	
    	if(filename == null || filename.isBlank()) {
    		return"";
    	}
    	
    	return filename.substring(filename.indexOf(".") + 1).toLowerCase();
    }
    
	

}
