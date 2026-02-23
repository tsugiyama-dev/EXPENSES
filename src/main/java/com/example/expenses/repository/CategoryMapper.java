package com.example.expenses.repository;

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

import com.example.expenses.domain.Category;

@Mapper
public interface CategoryMapper {

	/**
	 * カテゴリを新規登録
	 * @param category 登録するカテゴリ
	 */
	@Insert("""
			INSERT INTO categories (name, description, color, icon, active)
			VALUES (#{name}, #{description}, #{color}, #{icon}, #{active})
			""")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	void insert(Category category);
	
	
	/**
	 * IDでカテゴリを取得
	 * @Param id カテゴリID
	 * @return カテゴリ、存在しない場合null
	 */
	@Select("""
			SELECT id, name, description, color, icon, active,
			created_at, updated_at
			FROM categories
			WHERE id = #{id}
			""")
	@ConstructorArgs({
		@Arg(column = "id", javaType = Long.class , id = true),
		@Arg(column = "name", javaType = String.class),
		@Arg(column = "description", javaType= String.class),
		@Arg(column = "color", javaType= String.class),
		@Arg(column = "icon", javaType=String.class),
		@Arg(column = "active", javaType=boolean.class),
		@Arg(column = "created_at", javaType= LocalDateTime.class),
		@Arg(column = "updated_at", javaType= LocalDateTime.class)
	})
	Category findById(Long id);
	
	@Select("""
			SELECT id, name, description, color, icon, active,
			created_at, updated_at
			FROM categories
			WHERE active = true
			ORDER BY  name
			""")
	@ConstructorArgs({
		@Arg(column = "id" , javaType= Long.class, id = true),
		@Arg(column = "name", javaType = String.class),
		@Arg(column = "description", javaType= String.class),
		@Arg(column = "color", javaType= String.class),
		@Arg(column = "icon", javaType= String.class), 
		@Arg(column = "active", javaType= boolean.class),
		@Arg(column = "created_at", javaType = LocalDateTime.class),
		@Arg(column = "updated_at", javaType = LocalDateTime.class)
	})
	List<Category> findAllActive();
	
	
	/**
	 * 全カテゴリ一覧を取得（有効・無効含む）
	 * @return 全カテゴリのリスト
	 */
	@Select("""
			SELECT id, name, descripiton, color, icon, active,
			created_at, updated_at
			FROM categories
			ORDER BY name
			""")
	@ConstructorArgs({
		@Arg(column = "id", javaType = Long.class , id = true),
		@Arg(column = "name", javaType = String.class),
		@Arg(column = "description", javaType= String.class),
		@Arg(column = "color", javaType= String.class),
		@Arg(column = "icon", javaType=String.class),
		@Arg(column = "active", javaType=boolean.class),
		@Arg(column = "created_at", javaType= LocalDateTime.class),
		@Arg(column = "updated_at", javaType= LocalDateTime.class)
	})
	List<Category> findAll();
	
	/**
	 * カテゴリ情報を更新
	 * @param category 更新するカテゴリ
	 *@return 更新件数
	 */
	@Update("""
			UPDATE categories
			SET name = #{name},
			    description = #{description},
			    color = #{color},
			    icon = #{icon},
			    updated_at = NOW()
			WHERE id = #{id}
			""")
	int update(Category category);
	
	/**
	 * カテゴリの有効無効を更新
	 * @param id カテゴリID
	 * @param active 有効フラグ
	 * @return 更新件数
	 */
	@Update("""
			UPDATE categories
			SET active = #{active}, updated_at = NOW()
			WHERE id = #{id}
			""")
	int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
	
	/**
	 * カテゴリ名で検索（重複チェック用）
	 * @param name カテゴリ名
	 * @return カテゴリ、存在しない場合null
	 */
	@Select("""
			SELECT id, name, description, color, icon, active,
		           created_at, updated_at
		    FROM categories
		    WHERE name = #{name}
			""")
	@ConstructorArgs({
		@Arg(column = "id", javaType = Long.class , id = true),
		@Arg(column = "name", javaType = String.class),
		@Arg(column = "description", javaType= String.class),
		@Arg(column = "color", javaType= String.class),
		@Arg(column = "icon", javaType=String.class),
		@Arg(column = "active", javaType=boolean.class),
		@Arg(column = "created_at", javaType= LocalDateTime.class),
		@Arg(column = "updated_at", javaType= LocalDateTime.class)
	})
	Category findByName(String name);
	
	/**
	 * 指定カテゴリを使用している経費の数を取得
	 * @param categoryId カテゴリID
	 * @return 経費数
	 */
	@Select("""
			SELECT COUNT(*)
			FROM expenses
			WHERE category_id = #{categoryId}
			""")
	long countExpensesByCategory(Long categoryId);

}
