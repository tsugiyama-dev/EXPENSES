package com.example.expenses.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.expenses.domain.Expense;
import com.example.expenses.dto.request.ExpenseSearchCriteriaEntity;

@Mapper
public interface ExpenseMapper {

	@Insert("""
			INSERT INTO expenses 
				(applicant_id, title, amount, currency, status)
				VALUES
				(#{applicantId}, #{title}, #{amount} , #{currency}, #{status})
				
			""")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	void insert (Expense expense);
	
	@Select("""
			SELECT id, applicant_id, title, amount, currency, status,
			submitted_at, created_at, updated_at, version
			FROM expenses
			WHERE id = #{expenseId}
			""")
	Expense findById(Long expenseId);
	
	@Update("""
			UPDATE expenses
			SET status = 'SUBMITTED',
			    submitted_at = NOW()
			WHERE id = #{expenseId}
			    AND status = 'DRAFT'
			""")
	int submitDraft(@Param("expenseId")Long expenseId);
	
	public List<Expense> search(
			@Param("criteria") ExpenseSearchCriteriaEntity criteria,
			@Param("orderBy") String orderBy,
			@Param("direction") String direction,
			@Param("size") int size,
			@Param("offset")int offset );
	
	long count(@Param("criteria")ExpenseSearchCriteriaEntity criteria);
	
	int approve(@Param("id")long id, @Param("version") int version);
	int reject(@Param("id")long id, @Param("version") int version);

	
	@Select("""
			SELECT id, applicant_id, title, amount, currency, status,
			submitted_at, created_at, updated_at, version
			FROM expenses
			WHERE applicant_id = #{applicantId}
			LIMIT 5 OFFSET 0
			""")
	List<Expense> findByUserId(@Param("applicantId")Long applicantId);
	
	List<Expense> filter(
			@Param("criteria")ExpenseSearchCriteriaEntity criteria,
			@Param("orderBy")String orderBy,
			@Param("direction")String direction);
}
