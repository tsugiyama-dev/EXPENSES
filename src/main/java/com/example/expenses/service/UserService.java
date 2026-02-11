package com.example.expenses.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.expenses.dto.request.UserRegisterRequest;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.repository.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	
	@Transactional
	public void userRegister(UserRegisterRequest req) {
	
		String encoded = passwordEncoder.encode(req.getPassword());
		req.setPassword(encoded);
		//userテーブルに登録
		int cnt = userMapper.insert(req);
		
		if(cnt == 0) {
			throw new BusinessException("INVALID_STATUS_TRANSITION","そのEmailは使えません");
		}
		List<String> roles = new ArrayList<>();
		
		roles = req.getRole().contains(",") ? Arrays.asList(req.getRole().split(",")) 
											: List.of(req.getRole());
		
		//roleテーブルに登録
		for(String role : roles) {
			userMapper.registerRole(req.getId(), role);
		}
		
	}
		
}
