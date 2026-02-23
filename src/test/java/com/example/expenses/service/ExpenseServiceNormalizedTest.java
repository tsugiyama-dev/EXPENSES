package com.example.expenses.service;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * ExpenseService の normalizedOrderBy と normalizedDirection メソッドの単体テスト
 *
 * privateメソッドをリフレクションでテストする例
 */
@DisplayName("ExpenseService 単体テスト（正規化メソッド）")
class ExpenseServiceNormalizedTest {

	private ExpenseService expenseService;
	private Method normalizedOrderByMethod;
	private Method normalizedDirectionMethod;

	@BeforeEach
	void setUp() throws Exception {
		// ExpenseServiceのインスタンスを作成（依存オブジェクトはnull）
		expenseService = new ExpenseService(null, null, null, null, null);

		// privateメソッドを取得
		normalizedOrderByMethod = ExpenseService.class.getDeclaredMethod("normalizedOrderBy", String.class);
		normalizedOrderByMethod.setAccessible(true);

		normalizedDirectionMethod = ExpenseService.class.getDeclaredMethod("normalizedDirection", String.class);
		normalizedDirectionMethod.setAccessible(true);
	}

	@Nested
	@DisplayName("normalizedOrderBy のテスト")
	class NormalizedOrderByTest {

		@Test
		@DisplayName("nullの場合はcreated_atを返す")
		void nullを渡すとcreated_atを返す() throws Exception {
			// Given
			String sort = null;

			// When
			String result = invokeNormalizedOrderBy(sort);

			// Then
			assertThat(result).isEqualTo("created_at");
		}

		@ParameterizedTest
		@NullAndEmptySource
		@DisplayName("nullまたは空文字の場合はcreated_atを返す")
		void nullまたは空文字の場合はデフォルト値(String sort) throws Exception {
			// When
			String result = invokeNormalizedOrderBy(sort);

			// Then
			assertThat(result).isEqualTo("created_at");
		}

		@ParameterizedTest
		@CsvSource({
			"created_at,    created_at",
			"updated_at,    updated_at",
			"submitted_at,  submitted_at",
			"amount,        amount",
			"id,            id"
		})
		@DisplayName("許可されたソート条件は正しく変換される")
		void 許可されたソート条件(String input, String expected) throws Exception {
			// When
			String result = invokeNormalizedOrderBy(input);

			// Then
			assertThat(result).isEqualTo(expected);
		}

		@ParameterizedTest
		@ValueSource(strings = {
			"invalid",
			"DROP TABLE expenses",
			"'; DROP TABLE expenses; --",
			"title",
			"name"
		})
		@DisplayName("不正なソート条件はcreated_atにフォールバック")
		void 不正なソート条件はデフォルト値(String input) throws Exception {
			// When
			String result = invokeNormalizedOrderBy(input);

			// Then
			assertThat(result).isEqualTo("created_at");
		}

		@Test
		@DisplayName("カンマ区切りの場合は最初の値を使う")
		void カンマ区切りの場合() throws Exception {
			// Given
			String sort = "amount,DESC";

			// When
			String result = invokeNormalizedOrderBy(sort);

			// Then
			assertThat(result).isEqualTo("amount");
		}

		@Test
		@DisplayName("前後の空白は削除される")
		void 前後の空白は削除される() throws Exception {
			// Given
			String sort = "  amount  ,  DESC  ";

			// When
			String result = invokeNormalizedOrderBy(sort);

			// Then
			assertThat(result).isEqualTo("amount");
		}

		@Test
		@DisplayName("大文字小文字は区別される（小文字のみ許可）")
		void 大文字小文字は区別される() throws Exception {
			// Given
			String sort = "CREATED_AT";  // 大文字

			// When
			String result = invokeNormalizedOrderBy(sort);

			// Then
			assertThat(result).isEqualTo("created_at");  // デフォルト値
		}
	}

	@Nested
	@DisplayName("normalizedDirection のテスト")
	class NormalizedDirectionTest {

		@Test
		@DisplayName("nullの場合はDESCを返す")
		void nullの場合はDESC() throws Exception {
			// Given
			String sort = null;

			// When
			String result = invokeNormalizedDirection(sort);

			// Then
			assertThat(result).isEqualTo("DESC");
		}

		@ParameterizedTest
		@NullAndEmptySource
		@DisplayName("nullまたは空文字の場合はDESCを返す")
		void nullまたは空文字の場合はDESC(String sort) throws Exception {
			// When
			String result = invokeNormalizedDirection(sort);

			// Then
			assertThat(result).isEqualTo("DESC");
		}

		@ParameterizedTest
		@CsvSource({
			"created_at,asc,  ASC",
			"created_at,ASC,  ASC",
			"created_at,Asc,  ASC",
			"created_at,desc, DESC",
			"created_at,DESC, DESC",
			"created_at,Desc, DESC"
		})
		@DisplayName("ソート方向を正しく正規化する")
		void ソート方向を正しく正規化(String input, String expected) throws Exception {
			// When
			String result = invokeNormalizedDirection(input);

			// Then
			assertThat(result).isEqualTo(expected);
		}

		@ParameterizedTest
		@ValueSource(strings = {
			"created_at,invalid",
			"created_at,up",
			"created_at,down",
			"created_at,123"
		})
		@DisplayName("不正なソート方向はDESCにフォールバック")
		void 不正なソート方向はDESC(String input) throws Exception {
			// When
			String result = invokeNormalizedDirection(input);

			// Then
			assertThat(result).isEqualTo("DESC");
		}

		@Test
		@DisplayName("カンマがない場合はDESCを返す")
		void カンマがない場合はDESC() throws Exception {
			// Given
			String sort = "created_at";

			// When
			String result = invokeNormalizedDirection(sort);

			// Then
			assertThat(result).isEqualTo("DESC");
		}

		@Test
		@DisplayName("前後の空白は削除される")
		void 前後の空白は削除される() throws Exception {
			// Given
			String sort = "created_at,  asc  ";

			// When
			String result = invokeNormalizedDirection(sort);

			// Then
			assertThat(result).isEqualTo("ASC");
		}

		@Test
		@DisplayName("3つ以上のカンマ区切りでも2番目の値を使う")
		void 複数カンマの場合() throws Exception {
			// Given
			String sort = "created_at,asc,extra,value";

			// When
			String result = invokeNormalizedDirection(sort);

			// Then
			assertThat(result).isEqualTo("ASC");
		}
	}

	// ヘルパーメソッド
	private String invokeNormalizedOrderBy(String arg) throws Exception {
		return (String) normalizedOrderByMethod.invoke(expenseService, arg);
	}

	private String invokeNormalizedDirection(String arg) throws Exception {
		return (String) normalizedDirectionMethod.invoke(expenseService, arg);
	}
}
