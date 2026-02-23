package com.example.expenses.domain;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class Category {

	private Long id;
	private String name;
	private String description;
	private String color;
	private String icon;
	private boolean active;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	/**
	 * カテゴリを新規作成
	 * デフォルト値を設定して作成
	 */
	
	public static Category create(String name, String description,
	                              String color, String icon) {
		if(name == null || name.isBlank()) {
			throw new IllegalArgumentException("カテゴリ名は必須です");
		}
		if(name.length() > 100) {
			throw new IllegalArgumentException("カテゴリ名は100文字いないです");
		}
		
		Category category = new Category();
		category.name = name;
		category.description = description;
		category.color = color;
		category.icon = (icon != null && !icon.isBlank()) ? icon : "tag";
		category.active = true;
		category.createdAt = LocalDateTime.now();
		category.updatedAt = LocalDateTime.now();
		
		return category;
	}
	
	
	/**
	 * カテゴリ情報を更新
	 * ビジネスルール：名前は必須
	 */
	
	public void update(String name, String description, String color, String icon) {
		if(name == null || name.isBlank()) {
			throw new IllegalArgumentException("カテゴリ名は必須です");
		}
		if(name.length() > 100) {
			throw new IllegalArgumentException("カテゴリ名は100文字以内です");
		}
		
		this.name = name;
		this.description = description;
		this.color = color;
		this.icon = icon;
		this.updatedAt = LocalDateTime.now();
		
	}
	
	public void deactivate() {
		if(!this.active) {
			throw new IllegalArgumentException("カテゴリはすでに無効化されています：" + this.name);
		}
		this.active = false;
		this.updatedAt = LocalDateTime.now();
	}
	
	public void activate() {
		if(this.active) {
			throw new IllegalArgumentException("カテゴリはすでに有効です：" + this.name);
		}
		
		this.active = true;
		this.updatedAt = LocalDateTime.now();
	}
	public boolean isActive() {
		return this.active;
	}
}
