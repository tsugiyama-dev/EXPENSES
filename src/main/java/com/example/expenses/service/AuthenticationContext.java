package com.example.expenses.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.expenses.config.LoginUser;

@Component
public class AuthenticationContext {
	/**
	 * 	現在ログイン中のユーザーIDを取得
	 *  @return ユーザーＩＤ
	 * 	@throws IllegalStateException 未認証の場合   
	 */
	
	public Long getCurrentUserId() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		var principal = auth.getPrincipal();
		
		if(principal instanceof LoginUser loginUser) {
			return loginUser.getUserId();
		}
		
		throw new IllegalStateException("未認証のユーザーです");
		
	}
	
	/**
	 * 現在ログイン中のロール一覧を取得
	 * @Return ロールのリスト(例: [ROLE_USER, ROLE_ADMIN])
	 */
	public List<String> getCurrentUserRoles() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		
		return auth.getAuthorities().stream()
				.map(authority -> authority.getAuthority())
				.toList();
	}
	
	public boolean isApprover() {
		return getCurrentUserRoles().contains("ROLE_APPROVER");
	}
	
	/**
	 * 現在のユーザーが指定されたユーザーＩＤの所有者の所有者、または承認者かを確認
	 * @param ownerId 対象のユーザーＩＤ
	 * @return 所有者または承認者の場合true
	 */
	
	public boolean isOwnerOrApprover(Long ownerId) {
		return getCurrentUserId().equals(ownerId) || isApprover();
	}

}
