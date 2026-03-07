package com.example.expenses.repository;

import com.example.expenses.domain.Receipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 領収書テーブルのMapper
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>MyBatisのMapper インターフェース</li>
 *   <li>XML マッピングファイルとの連携</li>
 *   <li>複数パラメータの @Param アノテーション</li>
 * </ul>
 */
@Mapper
public interface ReceiptMapper {

    /**
     * 領収書を保存します
     *
     * @param receipt 領収書エンティティ
     * @return 挿入された行数
     */
    int insert(Receipt receipt);

    /**
     * 領収書IDで検索します
     *
     * @param id 領収書ID
     * @return 領収書エンティティ（存在しない場合null）
     */
    Receipt findById(@Param("id") Long id);

    /**
     * 経費IDに紐づく領収書一覧を取得します
     *
     * @param expenseId 経費ID
     * @return 領収書リスト
     */
    List<Receipt> findByExpenseId(@Param("expenseId") Long expenseId);

    /**
     * 領収書を削除します
     *
     * @param id 領収書ID
     * @return 削除された行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 経費IDに紐づく領収書の件数を取得します
     *
     * @param expenseId 経費ID
     * @return 領収書の件数
     */
    int countByExpenseId(@Param("expenseId") Long expenseId);
}
