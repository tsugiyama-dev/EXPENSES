package com.example.expenses.storage;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;


public interface FileStorageService {

	/**
	 * ファイルの保存
	 *@param file アップロードされたファイル
	 *@param directory 保存先ディレクトリ
	 *@param filename 保存するファイル名(例："uuid.jpg")
	 *@param return 保存されたファイルのパス
	 *@throws IOException ファイル保存に失敗した場合
	 */
	String store(MultipartFile file, String directory, String filename) throws IOException;
	
	/**
	 * ファイルをリソースとして読み込む
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	Resource loadAsResource(String filePath) throws IOException;
	
	/**
	 * ファイルを削除
	 * 
	 * @param filePath
	 * @throws IOException
	 */
	void delete(String filePath) throws IOException;
	
	/**
	 * ファイルの存在チェック
	 * 
	 * @param filePath ファイルパス
	 * @return ファイルが存在する場合true
	 */
	boolean exists(String filePath);
	
	/**
	 * ストレージのルートパスを取得
	 * @return ルートパス
	 */
	Path getRootLocation();
}
