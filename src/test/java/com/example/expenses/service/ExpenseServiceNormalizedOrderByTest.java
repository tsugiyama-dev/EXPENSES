package com.example.expenses.service;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ExpenseServiceNormalizedOrderByTest {

	private Method method;
	private Method normalizedDirectionMethod;
	private ExpenseService service;
	
	@BeforeEach
	void setUp() throws Exception{
		service = new ExpenseService(null, null, null, null);
		method = ExpenseService.class.getDeclaredMethod("normalizedOrderBy", String.class);
		normalizedDirectionMethod = ExpenseService.class.getDeclaredMethod("normalizedDirection", String.class);
		method.setAccessible(true);
		normalizedDirectionMethod.setAccessible(true);
		
	}
	
	@Test
	void 基本の検証() {
		String result = "created_at";
		
		//値が等しい
		assertThat(result).isEqualTo("created_at");
		
		//Not Equals
		assertThat(result).isNotEqualTo("invalid");
		
		//is not null
		assertThat(result).isNotNull();
		
		//is Null
		String nullValue = null;
		assertThat(nullValue).isNull();
		
	}
	
	@Test
	void 文字列の検証() {
		String result = "created_at";
		
		//特定の文字列を含む
		assertThat(result).contains("created");
		
		//特定の文字列で始まる
		assertThat(result).startsWith("created");
		
		//特定の文字列で終わる
		assertThat(result).endsWith("at");
		
		//空文字列ではない
		assertThat(result).isNotEmpty();
		
		//ブランクではない
		assertThat(result).isNotBlank();

	}
	
	@Test
	void 数値の検証() {
		
		int result = 5;
		
		//equal
		assertThat(result).isEqualTo(5);
		
		//isGreaterThan
		assertThat(result).isGreaterThan(3);
		
		//isLessThan
		assertThat(result).isLessThan(7);
		
		//以下
		assertThat(result).isLessThanOrEqualTo(5);
		
		//以上
		assertThat(result).isGreaterThanOrEqualTo(5);
		
		//範囲内
		assertThat(result).isBetween(1, 10);
	}
	
	@Test
	void コレクションの検証() {
		List<Integer> result = List.of(1, 2, 3, 4, 5);
		
		//size
		assertThat(result).hasSize(5);
		
		//要素を含む
		assertThat(result).contains(3);
		
		//複数の要素を含む
		assertThat(result).contains(1, 2, 3);
		
		//要素を含まない
		assertThat(result).doesNotContain(6);
		
		//空ではない
		assertThat(result).isNotEmpty();
		
		//最初の要素
		assertThat(result).first().isEqualTo(1);
		
		//最後の要素
		assertThat(result).last().isEqualTo(5);
		
	}
	
	@Test
	void booleanの検証() {
		boolean result = true;
		
		//is true
		assertThat(result).isTrue();
		
		// is false
		assertThat(!result).isFalse();
	}
	
	@Test
	@DisplayName("Nullの場合はcreated_atを返す")
	void Nullの場合はcreated_atを返す() throws Exception {
		String sortValue = null;
		
		var result = (String)method.invoke(service, sortValue);
		
		assertThat(result).isEqualTo("created_at");
	}
	
	@Test
	@DisplayName("空文字の場合はcreated_atを返す")
	void 空文字の場合はcreated_atを返す() throws Exception{
		//Given
		String sortValue = "";
		
		//when
		var result = (String)method.invoke(service, sortValue);
		
		//then
		assertThat(result).isEqualTo("created_at");
	}
	
	@Test
	@DisplayName("不正な値の場合はcreated_atを返す")
	void 不正な値の場合はcreated_atを返す() throws Exception {
		
		String sortValue = "invalid";
		
		var result = (String)method.invoke(service, sortValue);
		
		assertThat(result).isEqualTo("created_at");
		assertThat(result).isNotNull();
	}
	
	@Test
	@DisplayName("有効な値の場合は値をそのまま返す")
	void 有効な値の場合は値をそのまま返す() throws Exception {
		String sortValue = "submitted_at";
		
		var result = (String)method.invoke(service, sortValue);
		
		assertThat(result).isEqualTo("submitted_at");
		assertThat(result).isNotNull();
		assertThat(result).isNotEqualTo("created_at");
		
	}
	
	@Test
	@DisplayName("Nullの場合はDescを返す")
	void Nullの場合Descを返す() throws Exception{
		//given
		String direction = null;
		
		//when
		var result = (String)normalizedDirectionMethod.invoke(service, direction);
		
		//then
		assertThat(result).isEqualTo("DESC");
	}
	
	@Test
	@DisplayName("カンマ,がない場合DESCを返す")
	void カンマがない場合DESCを返す() throws Exception {
		
		String directionValue = "DESC";
		
		var result = (String)normalizedDirectionMethod.invoke(service, directionValue);
		
		assertThat(result).isEqualTo("DESC");
		
	}
	
	@Test
	@DisplayName("created_at,ascの場合はASCを返す")
	void created_at_and_ascの場合はASCを返す () throws Exception {
		
		String directionValue = "created_at,asc";
		
		var result = (String)normalizedDirectionMethod.invoke(service, directionValue);
		
		assertThat(result).isEqualTo("ASC");
	
	}
	
	@Test
	@DisplayName("created_at,descの場合はDESCを返す")
	void created_at_and_descの場合はDESCを返す() throws Exception {
		String direction = "created_at,desc";
		
		var result = (String)normalizedDirectionMethod.invoke(service, direction);
		
		assertThat(result).isEqualTo("DESC");
		
	}
	
	@Test
	@DisplayName("created_at,invalidの場合はDESCを返す")
	void created_at_and_invalidの場合はDESCを返す() throws Exception {
		String direction = "created_at, invalid";
		
		var result = (String)normalizedDirectionMethod.invoke(service, direction);
		
		assertThat(result).isEqualTo("DESC");
	}
	
	
	
	
	
	
	
}
