package com.example.expenses.util;



import java.util.List;
import java.util.Objects;

import org.springframework.security.core.context.SecurityContextHolder;

import com.example.expenses.config.LoginUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CurrentUser {

	private CurrentUser() {}
	
	public static Long actorId() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		var principal = auth.getPrincipal();
		
		if(principal instanceof LoginUser loginUser) {
			log.info("ユーザー情報取得={}", loginUser.getUserId());
			return loginUser.getUserId(); 
		}
		throw new IllegalStateException("ログインユーザーを取得できません");
	}
	
	public static List<String> actorRole() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		var principal = auth.getPrincipal();
		
		if(principal instanceof LoginUser loginUser) {
			return loginUser.getAuthorities().stream()
					.map(a -> a.getAuthority())
					.toList();
		}
		log.info("ログイン不可 principal={}", principal);
		
		throw new IllegalStateException("ログインユーザを取得できません");
	}
	
	public static String email() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		var principal = auth.getPrincipal();
		
		if(principal instanceof LoginUser loginUser) {
			var temp = loginUser.getUsername();
			var displayName = temp.substring(0,temp.split("@")[0].length());
			return displayName;
		}
		throw new IllegalStateException("ログインユーザーを取得できません");
	}
	public static boolean is403(long actorId) {

		List<String> roles = actorRole();
		boolean isAdmin = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_APPROVER");
		boolean isOwner = Objects.equals(actorId(), actorId);

		return !isAdmin && !isOwner;
	}
}
