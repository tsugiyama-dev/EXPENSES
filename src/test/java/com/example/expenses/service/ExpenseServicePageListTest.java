package com.example.expenses.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.expenses.domain.Expense;
import com.example.expenses.exception.BusinessException;

public class ExpenseServicePageListTest {

	private ExpenseService service;
	private Method pageListMethod;
	
	@BeforeEach
	void setUp() throws  Exception {

		service = new ExpenseService(null, null, null, null);
	
		pageListMethod = ExpenseService.class.getDeclaredMethod("pageList", int.class, int.class, int.class);
		pageListMethod.setAccessible(true);
	}
	
	@Test
	@DisplayName("最初のページ")
	void 最初のページ()  throws Exception{
		List<Integer> pages = invokePageList(service, 1, 10, 5);
		
		assertEquals(5, pages.size());
		assertEquals(1, pages.get(0));
		assertEquals(5, pages.get(pages.size() -1));
	}
	@Test
	@DisplayName("中間ページ")
	void 中間ページ() throws Exception {
		
		List<Integer> pages = invokePageList(service, 5, 10, 5);
		
		assertEquals(5, pages.size());
		assertEquals(3, pages.get(0));
		assertEquals(7, pages.get(pages.size() - 1));
	}
	
	@Test
	@DisplayName("最後のページ")
	void 最後のページ() throws Exception {
		List<Integer> pages = invokePageList(service, 10, 10, 5);
		
		assertEquals(5, pages.size());
		assertEquals(6, pages.get(0));
		assertEquals(10, pages.get(pages.size() -1));
	}
	
	@Test
	@DisplayName("二重更新によるエラー")
	void 二重更新によるエラー() {
		
		Expense expense = new Expense(
				null, null, null, null, null, null, null,null, null,4);
		int updatedVersion = 999;
		

		
		assertThrows(BusinessException.class, () -> {
			if(!expense.getVersion().equals(updatedVersion)) {
				throw new BusinessException("CONCURRENT_MODIFICATION","ほかのユーザに更新されています");
			}
		});
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	private List<Integer> invokePageList(ExpenseService service, int currentPage, int totalPage, int displayPage) throws Exception {
		
		return (List<Integer>)pageListMethod.invoke(service, currentPage, totalPage, displayPage);
	}
	
	@Test
	void 足し算のテスト() {
		
		int result = 1 + 1;
		
		assertEquals(2, result);
	}
	
	@Test
	void 空文字チェック() {
		String str = "";

		
		assertTrue(str.isEmpty());
	}
	
	@Test
	@DisplayName("非空文字チェック")
	void  非空文字チェック() {
		String str = "hello";
		assertFalse(str.isEmpty());
	}
	
	@Test
	void nullチェック() {
		String str = null;
		
		assertNull(str);
	}
	
	@Test
	void 非nullチェック() {
		String str = "null";
		
		assertNotNull(str);
	}
	
	@Test
	void ゼロ除算でエラー() {
		assertThrows(ArithmeticException.class, () -> {
			int result = 10/ 0; //exceptrion!!
		});
	}
	


}
