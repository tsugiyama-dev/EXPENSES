package com.example.expenses.storage;

import com.example.expenses.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * ローカルファイルシステムを使用したファイルストレージサービス
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>Java NIO（New I/O）の使い方</li>
 *   <li>ファイルシステム操作のベストプラクティス</li>
 *   <li>例外処理とロギング</li>
 *   <li>Spring の @Value によるプロパティ注入</li>
 * </ul>
 *
 * <p><b>セキュリティ対策:</b></p>
 * <ul>
 *   <li>パストラバーサル攻撃防止（Path.normalize()）</li>
 *   <li>ルートディレクトリ外へのアクセス防止</li>
 *   <li>ファイル名のサニタイズ（呼び出し側で実施）</li>
 * </ul>
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path rootLocation;

    /**
     * コンストラクタ
     *
     * @param uploadDir アップロードディレクトリのパス（application.propertiesから注入）
     */
    public LocalFileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        logger.info("ファイルストレージのルートディレクトリ: {}", this.rootLocation);
    }

    /**
     * 初期化処理
     * アプリケーション起動時にルートディレクトリを作成
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.rootLocation);
            logger.info("アップロードディレクトリを作成しました: {}", this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("ファイルストレージの初期化に失敗しました", e);
        }
    }

    /**
     * ファイルを保存します
     *
     * <p><b>実装の詳細:</b></p>
     * <ol>
     *   <li>保存先ディレクトリを作成</li>
     *   <li>パストラバーサル攻撃をチェック</li>
     *   <li>ファイルをストリーミングでコピー（メモリ効率的）</li>
     * </ol>
     *
     * @param file アップロードされたファイル
     * @param directory 保存先ディレクトリ（例: "2026/03/07"）
     * @param filename 保存するファイル名（例: "uuid.jpg"）
     * @return 保存されたファイルの相対パス（例: "2026/03/07/uuid.jpg"）
     * @throws IOException ファイル保存に失敗した場合
     */
    @Override
    public String store(MultipartFile file, String directory, String filename) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("アップロードされたファイルが空です");
        }

        // 保存先ディレクトリのパス
        Path directoryPath = this.rootLocation.resolve(directory).normalize();

        // ディレクトリを作成（存在しない場合）
        Files.createDirectories(directoryPath);

        // 保存先ファイルのパス
        Path destinationFile = directoryPath.resolve(filename).normalize();

        // セキュリティチェック: パストラバーサル攻撃防止
        // ファイルパスがルートディレクトリ配下にあることを確認
        if (!destinationFile.startsWith(this.rootLocation)) {
            throw new BusinessException("不正なファイルパスです: " + filename);
        }

        // ファイルをストリーミングでコピー（大きなファイルでもメモリを圧迫しない）
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // 相対パスを返す（例: "2026/03/07/uuid.jpg"）
        String relativePath = this.rootLocation.relativize(destinationFile).toString();
        logger.info("ファイルを保存しました: {}", relativePath);

        return relativePath;
    }

    /**
     * ファイルをリソースとして読み込みます
     *
     * @param filePath ファイルパス（相対パス）
     * @return ファイルリソース
     * @throws IOException ファイル読み込みに失敗した場合
     */
    @Override
    public Resource loadAsResource(String filePath) throws IOException {
        try {
            Path file = this.rootLocation.resolve(filePath).normalize();

            // セキュリティチェック: パストラバーサル攻撃防止
            if (!file.startsWith(this.rootLocation)) {
                throw new BusinessException("不正なファイルパスです: " + filePath);
            }

            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException("ファイルが見つかりません: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new BusinessException("ファイルの読み込みに失敗しました: " + filePath, e);
        }
    }

    /**
     * ファイルを削除します
     *
     * @param filePath 削除するファイルのパス（相対パス）
     * @throws IOException ファイル削除に失敗した場合
     */
    @Override
    public void delete(String filePath) throws IOException {
        Path file = this.rootLocation.resolve(filePath).normalize();

        // セキュリティチェック: パストラバーサル攻撃防止
        if (!file.startsWith(this.rootLocation)) {
            throw new BusinessException("不正なファイルパスです: " + filePath);
        }

        if (Files.exists(file)) {
            Files.delete(file);
            logger.info("ファイルを削除しました: {}", filePath);
        } else {
            logger.warn("削除対象のファイルが存在しません: {}", filePath);
        }
    }

    /**
     * ファイルの存在をチェックします
     *
     * @param filePath ファイルパス（相対パス）
     * @return ファイルが存在する場合true
     */
    @Override
    public boolean exists(String filePath) {
        Path file = this.rootLocation.resolve(filePath).normalize();

        // セキュリティチェック: パストラバーサル攻撃防止
        if (!file.startsWith(this.rootLocation)) {
            return false;
        }

        return Files.exists(file);
    }

    /**
     * ストレージのルートパスを取得します
     *
     * @return ルートパス
     */
    @Override
    public Path getRootLocation() {
        return this.rootLocation;
    }
}
