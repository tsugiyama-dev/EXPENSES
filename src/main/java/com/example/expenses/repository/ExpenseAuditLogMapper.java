package com.example.expenses.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.example.expenses.dto.ExpenseAuditLog;

@Mapper
public interface ExpenseAuditLogMapper {

	@Insert("""
			INSERT INTO expense_audit_logs
				(expense_id, actor_id, action, before_status, after_status, note, trace_id)
				VALUES 
				(#{expenseId}, #{actorId}, #{action}, #{beforeStatus}, #{afterStatus},#{note}, #{traceId})
			""")
	@Options(useGeneratedKeys = true, keyProperty="id")
	void insert(ExpenseAuditLog log);
	
	@Select("""
			SELECT  id, expense_id, actor_id, action, 
				    before_status, after_status,
			        note, trace_id, created_at
			FROM expense_audit_logs
			WHERE expense_id = #{expenseId}
			ORDER BY created_at ASC, id ASC
			""")
	List<ExpenseAuditLog> findByExpenseId(long expenseId);
	
	@Select("""
		SELECT id, expense_id, actor_id, action,
			   before_status, after_status,
			   note, trace_id, created_at
	    FROM expense_audit_logs
	    WHERE actor_id = #{actorId}
	    ORDER BY expense_id ASC, created_at DESC
	    LIMIT 5 OFFSET 0
			""")
	List<ExpenseAuditLog> findByActorId(long actorId);
//	@Insert("""
//			INSERT INTO expense_audit_logs (
//				id, expenses_id, actor_id, action,
//				before_status, after_status, note, trace_id
//			) VALUES(
//			#{id}, #{expenseId}, #{actorId}, #{action},
//			#{beforeStatus}, #{afterStatus}, #{note}, #{traceId}
//			)
//			""")
//	void insert(ExpenseAuditLog log)
//	@Insert("""
//			INSERT INTO expense_audit_logs
//			  (expense_id, actor_id, action, before_status, after_status, note, trace_id)
//			VALUES 
//			  (#{expenseId}, #{actorId}, #{action}, #{beforeStatus}, #{afterStatus}, #{note}, #{traceId})
//			""")
//	void insert(ExpenseAuditLog log);
}
