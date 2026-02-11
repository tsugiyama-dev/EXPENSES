package com.example.expenses.repository;

import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.expenses.domain.User;
import com.example.expenses.dto.request.UserRegisterRequest;

@Mapper
public interface UserMapper {

	Optional<User> findByEmail(@Param("email") String email);
	
	@Insert("""
			INSERT INTO users (email, password, role)
			VALUES (#{email}, #{password}, #{role})
			""")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insert(UserRegisterRequest req);
	
	
	@Insert("""
			INSERT INTO roles (user_id, role)
			VALUES (#{userId}, #{role})
			""")
	int registerRole(
			@Param("userId")Long userId,
			@Param("role")String role);
	
	@Select("""
			SELECT email
			FROM users
			WHERE id = #{userId}
			""")
	String findEmailById(Long userId);
	
	@Select("""
			SELECT email
			FROM users
			WHERE role LIKE CONCAT('%','ROLE_APPROVER','%')
			LIMIT 1
			""")
	String findAnyApproverEmail();
}
