package com.example.expenses.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.dto.request.ExpenseSearchCriteriaEntity;

@Mapper
public interface ExpenseMapper {

	/**
	 *  経費の登録
	 * @param expense
	 */
	@Insert("""
			INSERT INTO expenses 
				(applicant_id, title, amount, currency, status)
				VALUES
				(#{applicantId}, #{title}, #{amount} , #{currency}, #{status})
				
			""")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	void insert (Expense expense);
	
	
	/**
	 *  expenseIdから経費を取得
	 * @param expenseId
	 * @return
	 */
	@ConstructorArgs({
		@Arg(column = "id", javaType = Long.class, id = true),
		@Arg(column = "applicant_id", javaType = Long.class),
		@Arg(column = "title", javaType = String.class),
		@Arg(column = "amount", javaType = BigDecimal.class),
		@Arg(column = "currency", javaType = String.class),
		@Arg(column = "status", javaType = ExpenseStatus.class),
		@Arg(column = "submitted_at", javaType = LocalDateTime.class),
		@Arg(column = "created_at", javaType = LocalDateTime.class),
		@Arg(column = "updated_at", javaType = LocalDateTime.class),
		@Arg(column = "version", javaType = Integer.class)
	})
	@Select("""
			SELECT id, applicant_id, title, amount, currency, status,
			submitted_at, created_at, updated_at, version
			FROM expenses
			WHERE id = #{expenseId}
			""")
	Expense findById(Long expenseId);
	
	/**
	 *  経費を提出
	 * @param expenseId
	 * @return
	 */
	@Update("""
			UPDATE expenses
			SET status = 'SUBMITTED',
			    submitted_at = NOW()
			WHERE id = #{expenseId}
			    AND status = 'DRAFT'
			""")
	int submitDraft(@Param("expenseId")Long expenseId);
	
	/**
	 * 経費を全件取得
	 */
	@ConstructorArgs({
		@Arg(column = "id", javaType = Long.class, id = true),
		@Arg(column = "applicant_id", javaType = Long.class),
		@Arg(column = "title", javaType = String.class),
		@Arg(column = "amount", javaType = BigDecimal.class),
		@Arg(column = "currency", javaType = String.class),
		@Arg(column = "status", javaType = ExpenseStatus.class),
		@Arg(column = "submitted_at", javaType = LocalDateTime.class),
		@Arg(column = "created_at", javaType = LocalDateTime.class),
		@Arg(column = "updated_at", javaType = LocalDateTime.class),
		@Arg(column = "version", javaType = Integer.class)
	})
	@Select("""
			<script>
			SELECT id, applicant_id, title, 
			       amount, currency, status,
			       submitted_at, created_at, 
			       updated_at, version, category_id
			FROM expenses
			<where>
				<if test="criteria.applicantId != null">
					AND applicant_id = #{criteria.applicantId}
				</if>
				<if test="criteria.status != null and criteria.status != ''">
					AND status = #{criteria.status}
				</if>
				<if test="criteria.title != null and criteria.title != ''">
					AND title LIKE CONCAT("%", #{criteria.title}, "%")
				</if>
				<if test="criteria.amountMin != null">
					AND amount &gt;= #{criteria.amountMin}
				</if>
				<if test="criteria.amountMax != null">
					AND amount &lt;= #{criteria.amountMax}
				</if>
				<if test="criteria.submittedFrom != null">
					AND submitted_at &gt;= #{criteria.submittedFrom}
				</if>
				<if test="criteria.submittedTo != null">
				    AND submitted_at &lt;= #{criteria.submittedTo}
				</if>
			</where>
			ORDER BY created_at ASC
			</script>
			""")
	List<Expense> findAll(@Param("criteria")ExpenseSearchCriteriaEntity criteria);
	
	/**
	 * 条件で経費を取得
	 * @param criteria
	 * @param orderBy
	 * @param direction
	 * @param size
	 * @param offset
	 * @return
	 */
	public List<Expense> search(
			@Param("criteria") ExpenseSearchCriteriaEntity criteria,
			@Param("orderBy") String orderBy,
			@Param("direction") String direction,
			@Param("size") int size,
			@Param("offset")int offset );
	
	/**
	 * 条件から経費の数を取得
	 * @param criteria
	 * @return
	 */
	long count(@Param("criteria")ExpenseSearchCriteriaEntity criteria);
	
	/**
	 *  経費を承認する
	 * @param id
	 * @param version
	 * @return 更新数
	 */
	int approve(@Param("id")long id, @Param("version") int version);
	
	/**
	 * 経費を拒否する
	 * @param id
	 * @param version
	 * @return 更新数
	 */
	int reject(@Param("id")long id, @Param("version") int version);

	
	@ConstructorArgs({
		@Arg(column = "id", javaType = Long.class, id = true),
		@Arg(column = "applicant_id", javaType = Long.class),
		@Arg(column = "title", javaType = String.class),
		@Arg(column = "amount", javaType = BigDecimal.class),
		@Arg(column = "currency", javaType = String.class),
		@Arg(column = "status", javaType = ExpenseStatus.class),
		@Arg(column = "submitted_at", javaType = LocalDateTime.class),
		@Arg(column = "created_at", javaType = LocalDateTime.class),
		@Arg(column = "updated_at", javaType = LocalDateTime.class),
		@Arg(column = "version", javaType = Integer.class)
	})
	@Select("""
			SELECT id, applicant_id, title, amount, currency, status,
			submitted_at, created_at, updated_at, version
			FROM expenses
			WHERE applicant_id = #{applicantId}
			""")
	List<Expense> findByUserId(@Param("applicantId")Long applicantId);
	
	List<Expense> filter(
			@Param("criteria")ExpenseSearchCriteriaEntity criteria,
			@Param("orderBy")String orderBy,
			@Param("direction")String direction);
	
	@Select("""
			SELECT id, applicant_id, title, amount, currency, status,
			submitted_at, created_at, updated_at, version
			FROM expenses
			WHERE submitted_at >= #{start}
			  AND submitted_at <= #{end}
			ORDER BY created_at ASC
			""")
	List<Expense> findByPeriod(@Param("start")LocalDateTime start,@Param("end") LocalDateTime end);
	
	List<Expense> findAllWithPaging(@Param("maxId") Long maxId);
	
	@Select("""
			SELECT COALESCE(MAX(id), 0) FROM expenses
			""")
	Long findMaxId();
}
