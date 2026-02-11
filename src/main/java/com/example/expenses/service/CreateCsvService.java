package com.example.expenses.service;

import java.nio.charset.Charset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.expenses.domain.Expense;

@Service
public class CreateCsvService {

	public byte[] createCsv(List<Expense> list) {
		
		
		StringBuilder sb = new StringBuilder();
		sb.append("ID,タイトル,金額,通貨,ステータス,提出日,作成日,更新日\n");
		
		for(Expense e : list) {
			sb.append(e.getId() + ",");
			sb.append(e.getTitle() + ",");
			sb.append(e.getAmount() + ",");
			sb.append(e.getCurrency() + ",");
			sb.append(e.getStatus() + ",");
			sb.append(e.getSubmittedAt() + ",");
			sb.append(e.getCreatedAt() + ",");
			sb.append(e.getUpdatedAt() + "\n");
		}
		
		return sb.toString().getBytes(Charset.forName("MS932"));
	}
}
