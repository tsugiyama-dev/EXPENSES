package com.example.expenses.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.expenses.exception.BusinessException;


@Service
public class LocalStorageService implements FileStorageService{

	private final Path rootLocation;
	private static final Logger logger = LoggerFactory.getLogger(LocalStorageService.class);
	
	public LocalStorageService(@Value("${upload-dir:./uploads}")String uploadDir) {
		this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
		
		logger.info("ファイルストレージのルートディレクトリ：{}", this.rootLocation);
	}
	
	/**
	 * 初期化処理
	 * アプリケーション起動時にルートディレクトリを作成
	 */
	@PostConstruct
	public void init() {
		try {
			Files.createDirectories(this.rootLocation);
			logger.info("アップロードディレクトリを作成しました：{}", this.rootLocation);
		}catch (IOException e) {
			throw new RuntimeException("ファイルストレージの作成に失敗しました");
		}
	}
	/**
	 * @param file
	 * @param directory /保存先ディレクトリ例："2026/03/27"
	 * @param filename
	 * @param 保存されたファイルの相対パス
	 * @throws IOException
	 */
	@Override
	public String store(MultipartFile file, String directory, String filename) throws IOException {

		if(file.isEmpty()) {
			throw new BusinessException("","アップロードされたファイルが空です");
		}
		
		//将来の変更も考慮してnormalize()を付与
		Path directoryPath = this.rootLocation.resolve(directory).normalize();
		
		Files.createDirectories(directoryPath);
		
		Path destinationFile = directoryPath.resolve(filename).normalize();
		
		// セキュリティチェック: パストラバーサル攻撃防止
		if(!destinationFile.startsWith(this.rootLocation)) {
			throw new BusinessException("不正なファイルパスです：" + filename,"");
		}
		
		try 
			(InputStream inputStream = file.getInputStream()) {
			
			Files.copy(inputStream,  destinationFile, StandardCopyOption.REPLACE_EXISTING);
				
		}
		
		String relativePath = this.rootLocation.relativize(destinationFile).toString();
		logger.info("ファイルを保存しました：{}", relativePath);
		
		return relativePath;
	}

	/**
	 * ファイルをリソースとして読み込む
	 * 
	 * @param filePath
	 * @return ファイルリソース
	 * @throws IOException
	 */
	@Override
	public Resource loadAsResource(String filePath) throws IOException {

		try {
			Path file = this.rootLocation.resolve(filePath).normalize();
			
			// セキュリティチェック: パストラバーサル攻撃防止
			if(!file.startsWith(this.rootLocation)) {
				throw new BusinessException("不正なファイルパス：{}", filePath);
			}
			
			Resource resource = new UrlResource(file.toUri());
			
			if(resource.exists() && resource.isReadable()) {
				return resource;
			}else {
				throw new BusinessException("ファイルが見つかりません" + filePath, "");
			}
			
		}catch(MalformedURLException e) {
				throw new BusinessException("ファイルの読み込みに失敗しました" + filePath, e.getMessage());
		}
	}

	@Override
	public void delete(String filePath) throws IOException {
		
		
		Path file = this.rootLocation.resolve(filePath).normalize();
		
		// セキュリティチェック: パストラバーサル攻撃防止
		if(!file.startsWith(this.rootLocation)) {
			throw new BusinessException("不正なファイルパスです","");
		}
		
		if(Files.exists(file)) {
			Files.delete(file);
			logger.info("ファイルを削除しました： {}", filePath);
		}else {
			logger.warn("ファイルが存在しません：{}",filePath);
		}
		
		
	}
	
	/**
	 * ファイルの存在をチェック
	 * 
	 * @param filePath
	 * @return ファイルが存在する場合true
	 */
	@Override
	public boolean exists(String filePath) {
		
		Path file = this.rootLocation.resolve(filePath).normalize();
		
		// セキュリティチェック: パストラバーサル攻撃防止
		if(!file.startsWith(this.rootLocation)) {
			return false;
		}

		return Files.exists(file);
	}

	@Override
	public Path getRootLocation() {

		return this.rootLocation;
	}

}
