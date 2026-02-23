package com.example.expenses.api.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.expenses.dto.request.CategoryCreateRequest;
import com.example.expenses.dto.request.CategoryUpdateRequest;
import com.example.expenses.dto.response.CategoryResponse;
import com.example.expenses.service.CategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;
	
	/**
	 * カテゴリの作成
	 * POST /api/v1/categories
	 * 
	 * @param request 作成リクエスト(JSONボディ）
	 * @return 201 Created + 作成されたカテゴリ + Location ヘッダー
	 */
	
	@PostMapping
	public ResponseEntity<CategoryResponse>create(
			@Valid @RequestBody CategoryCreateRequest request) {
		//サービス層で作成
		CategoryResponse response = categoryService.create(request);
		
		URI location = URI.create("/api/v1/categories" + response.id());
		
		//201 Created で返す
		return ResponseEntity.created(location).body(response);
	}
	
	/**
	 * カテゴリ一覧取得
	 * GET /api/v1/categories?activeOnly=true
	 * 
	 * @param activeOnly 有効なカテゴリの未取得する（デフォルト：true)
	 * @return 200 OK + カテゴリリスト
	 */
	@GetMapping
	public ResponseEntity<List<CategoryResponse>> findAll(
			@RequestParam(defaultValue = "true") boolean activeOnly) {
		
		//activeOnlパラメータに応じて取得
		List<CategoryResponse> categories = activeOnly
				? categoryService.findAllActive()
			    : categoryService.findAll();
		
		return ResponseEntity.ok().body(categories);
	}
	
	
	/**
	 * カテゴリ詳細情報取得
	 * GET /api/v1/categories/{id}
	 * 
	 * @param id カテゴリID（パスパラメータ）
	 * @return 200 OK + カテゴリ詳細
	 */
	@GetMapping("/{id}")
	public ResponseEntity<CategoryResponse> findById(
			@PathVariable Long id) {
		CategoryResponse category = categoryService.findById(id);
		
		return ResponseEntity.ok().body(category);
	}
	
	/**
	 * カテゴリ更新
	 * PUT /api/v1/categories/{id}
	 * 
	 * @param id カテゴリＩＤ
	 * @param request 更新リクエスト
	 * @return 200 OK + 更新後のカテゴリ
	 */
	
	@PutMapping("/{id}")
	public ResponseEntity<CategoryResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody CategoryUpdateRequest request) {
		
		CategoryResponse response = categoryService.update(id,  request);
		return ResponseEntity.ok(response);
		
	}
	
	/**
	 * カテゴリの無効化（論理削除）
	 * DELETE /api/v1/categories/{id}
	 * 
	 * @param id カテゴリID
	 * @return 204 No Content
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable Long id) {
		
		categoryService.deactivate(id);
		//204 No Content - ボディなし
	}
	
	
	/**
	 * カテゴリの有効化
	 * POST /api/v1/categories/{id}/activate
	 * 
	 * @param id カテゴリID
	 * @return 204 No Content
	 */
	@PostMapping("/{id}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void activate(@PathVariable Long id) {
		
		categoryService.activate(id);
	}
}
