package com.example.expenses.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.expenses.domain.Category;
import com.example.expenses.dto.request.CategoryCreateRequest;
import com.example.expenses.dto.request.CategoryUpdateRequest;
import com.example.expenses.dto.response.CategoryResponse;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.repository.CategoryMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

	private final CategoryMapper categoryMapper;
	
	/**
	 * カテゴリの新規作成
	 * ビジネスルール：同じ名前のカテゴリは作成不可
	 * 
	 * @param request
	 * @return 作成されたカテゴリ
	 * @throws BusinessException 同盟カテゴリが作成された場合
	 */
	@Transactional
	public CategoryResponse create(CategoryCreateRequest request) {
		log.info("カテゴリ作成開始：name={}", request.name());
		
		//重複チェック
		Category existing = categoryMapper.findByName(request.name());
		if(existing != null) {
			throw new BusinessException("CATEGORY_DUPULICATE",
					"同名のカテゴリが存在します");
		}
		Category category = Category.create(
				request.name(),
				request.description(),
				request.color(),
				request.icon());
		//データベースに保存
		categoryMapper.insert(category);
		
		log.info("カテゴリ作成完了：id={}, name={}", category);
		
		return CategoryResponse.fromDomain(category);
	}
	
	@Transactional(readOnly = true)
	public List<CategoryResponse> findAllActive() {
		log.debug("有効カテゴリ一覧取得");
		
		return CategoryResponse.fromDomainList(categoryMapper.findAllActive());
	}
	
	/**
	 * 全カテゴリ一覧を取得（有効・無効含む）
	 * 
	 * @return 全カテゴリのリスト
	 */
	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		log.debug("全カテゴリ一覧を取得");
		
		return CategoryResponse.fromDomainList(categoryMapper.findAll());
	}
	
	/**
	 * IDでカテゴリを取得
	 * 
	 * @param id カテゴリID
	 * @return カテゴリ
	 * @throws NoSuchElementException カテゴリが存在しない場合
	 */
	
	@Transactional(readOnly = true)
	public CategoryResponse findById(Long id) {
		log.debug("カテゴリ取得：id={}", id);
		
		Category category = categoryMapper.findById(id);
		if(category == null) {
			throw new NoSuchElementException("カテゴリが見つかりません：" + id);
		}
		
		return CategoryResponse.fromDomain(category);
	}
	
	/**
	 * カテゴリを更新
	 * 
	 * @param id カテゴリID
	 * @param request 更新リクエスト
	 * @return 更新後のカテゴリ
	 * @throws NoSuchElementExceptioon カテゴリが存在しない場合
	 * @throws BusinessException 更新後のカテゴリが既存と重複する場合
	 */
	@Transactional
	public CategoryResponse update(Long id, CategoryUpdateRequest request) {
		log.info("カテゴリ更新開始: id={}, name={}", id, request.name());
		
		Category category = categoryMapper.findById(id);
		if(category == null) {
			throw new NoSuchElementException("カテゴリが見つかりません：" + id);
		}
		
		if(!category.getName().equals(request.name()) ){
			Category existing = categoryMapper.findByName(request.name());
			if(existing != null && existing.getName().equals(request.name())) {
				throw new BusinessException("CATEGORY_DUPLICATE",
						"同名のカテゴリが存在します：" + request.name());
			}
			
		}
		category.update(
				request.name(),
				request.description(),
				request.color(),
				request.icon());
		categoryMapper.update(category);
		
		log.info("カテゴリの更新完了： id={}", id);
		
		return CategoryResponse.fromDomain(category);
	}
	
	/**
	 * カテゴリを無効化（論理削除）
	 * ビジネスルール：使用中のカテゴリは無効化不可
	 * 
	 * @param id カテゴリId
	 * @throws NoSuchElementException カテゴリが存在しない場合
	 * @throws BusinessException 使用中のカテゴリを無効化しようとした場合
	 */
	
	@Transactional
	public void deactivate(Long id) {
		log.info("カテゴリ無効化開始: id={}", id);
		
		Category category = categoryMapper.findById(id);
		if(category == null) {
			throw new NoSuchElementException("カテゴリが存在しません" + id);
		}
		
		long usageCount = categoryMapper.countExpensesByCategory(id);
		if(usageCount > 0) {
			throw new BusinessException("CATEGORY_IN_USE",
					String.format("このカテゴリは%d件の経費で使用されているため削除できません", usageCount));
		}
		
		//ドメインメソッドで無効化
		category.deactivate();
		
		//データベース更新
		categoryMapper.updateActiveStatus(id, false);
		
		log.info("カテゴリ無効化完了：id={}", id);
	}
	
	/**
	 * カテゴリを有効か
	 * @param id カテゴリID
	 * @throws NoSuchElementException カテゴリが存在しない場合
	 */
	
	@Transactional
	public void activate(Long id) {
		log.info("カテゴリ有効化開始 id={}", id);
		
		//対象カテゴリを取得
		Category category = categoryMapper.findById(id);
		if(category == null) {
			throw new NoSuchElementException("カテゴリが存在しません：" + id);
		}
		
		//ドメインメソッドで有効化
		
		category.activate();
		
		categoryMapper.updateActiveStatus(id, true);
		
		log.info("カテゴリ有効化完了：id={}", id);
		
		
	}
	
	
}
