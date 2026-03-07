package com.example.expenses.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * ファイルストレージの抽象インターフェース
 *
 * <p>このインターフェースは、ファイルの保存・取得・削除を抽象化します。
 * 実装クラス（LocalFileStorageService、S3FileStorageServiceなど）を
 * 切り替えることで、ストレージバックエンドを変更できます。</p>
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>インターフェースによる抽象化（Dependency Inversion Principle）</li>
 *   <li>戦略パターン（Strategy Pattern）の実装</li>
 *   <li>将来の拡張性を見据えた設計</li>
 * </ul>
 */
public interface FileStorageService {

    /**
     * ファイルを保存します
     *
     * @param file アップロードされたファイル
     * @param directory 保存先ディレクトリ（例: "2026/03/07"）
     * @param filename 保存するファイル名（例: "uuid.jpg"）
     * @return 保存されたファイルのパス
     * @throws IOException ファイル保存に失敗した場合
     */
    String store(MultipartFile file, String directory, String filename) throws IOException;

    /**
     * ファイルをリソースとして読み込みます
     *
     * @param filePath ファイルパス
     * @return ファイルリソース
     * @throws IOException ファイル読み込みに失敗した場合
     */
    Resource loadAsResource(String filePath) throws IOException;

    /**
     * ファイルを削除します
     *
     * @param filePath 削除するファイルのパス
     * @throws IOException ファイル削除に失敗した場合
     */
    void delete(String filePath) throws IOException;

    /**
     * ファイルの存在をチェックします
     *
     * @param filePath ファイルパス
     * @return ファイルが存在する場合true
     */
    boolean exists(String filePath);

    /**
     * ストレージのルートパスを取得します
     *
     * @return ルートパス
     */
    Path getRootLocation();
}
