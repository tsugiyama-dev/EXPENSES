package com.example.expenses.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.expenses.domain.Receipt;

@Mapper
public interface ReceiptMapper {

	/**
	 * 
	 * @param receipt
	 */
	void insert(Receipt receipt);
	
	/**
	 * 
	 * @param id
	 * @return 1件の領収書
	 */
	Receipt findById(Long id);
	
	/**
	 * 
	 * @param expenseId
	 * @return 領収書のリスト
	 */
	List<Receipt> findByExpenseId(Long expenseId);
	
	/**
	 * 
	 * @param id
	 */
	void deleteById(Long id);
	
	/**
	 * 
	 * @param expenseId
	 * @return 領収書の件数
	 */
	int countByExpenseId(Long expenseId);
}
