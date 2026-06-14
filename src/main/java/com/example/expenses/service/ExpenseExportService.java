package com.example.expenses.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.ibatis.cursor.Cursor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.expenses.domain.Expense;
import com.example.expenses.dto.request.ExpenseSearchCriteria;
import com.example.expenses.export.ExportStrategy;
import com.example.expenses.repository.ExpenseMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseExportService {

	private final ExpenseMapper expenseMapper;

	private final List<ExportStrategy> strategies;
	private final Set<String> types = new HashSet<String>(Set.of("csv", "pdf"));
	
	public byte[] export(ExpenseSearchCriteria criteria, String exportType) {
		
		if(!types.contains(exportType)) {
			exportType = "csv";
		}
		log.info("strategies={}",strategies);
		
		ExportStrategy strategy = getToMap().get(exportType);
		if(strategy == null) {
			throw new IllegalArgumentException("exportTypeが不正です：" + exportType);
		}
		
		List<Expense> expenses = expenseMapper.filter(ExpenseSearchCriteria.toEntity(criteria), "created_at", "DESC");
		return strategy.export(expenses);
		
	}
	
	public Map<String, ExportStrategy> getToMap() {
		
		return this.strategies.stream().collect(Collectors.toMap(ExportStrategy::getFileExtension, Function.identity()));
	}
	
    /**
     * Cursor を使って全件をストリーミングで CSV に書き出す。
     * 
     * 【既存のexport()との違い】
     *  service.export() ：List<Expense> で全件をメモリにロード → byte[]を一括生成
     *  exportAllAsCsvStream(): Cursor で1行ずつ読み込み → OutputStream に逐次書き出し
     *                        → 件数がいくら増えてもメモリ消費はほぼ一定
     * 
     * 
     * 【@Transactional(readOnly = true)が必須な理由】
     *  Cursor はSqlSession (= DB コネクション)が開いている間しか読みだせない。
     * トランザクションがないと、メソッド呼び出し直後にセッションが閉じて
     * 「A Cursor is already closed」エラーになる。
     *  readOnly = true は更新を伴わないことを明示し、DB 側の最適化も効く。
     * 
     * 【try - with - resources が必須な理由】
     * Cursor をクローズしないと DB コネクションが返却されずに、
     * コネクションプールが枯渇する。
     */
	@Transactional(readOnly = true)
	public void exportAllAsCsvStream(OutputStream out) {
		
		try(Cursor<Expense> cursor = expenseMapper.findAllAsStream();
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
			
			writer.write('\uFEFF');
			writer.write("id,applicantId,title,amount,currency,status,submittedAt");
			writer.newLine();
			
			// Cursor は Iterable<T>を実装しているので、拡張for で1行ずつ取り出せる
			for(Expense expense : cursor) {
				writer.write(toCsvLine(expense));
				writer.newLine();
			}
			
			writer.flush();
			log.info("CSV ストリーミングエクスポート完了: {}件", cursor.getCurrentIndex() + 1);
		} catch (IOException e) {
			throw new UncheckedIOException("CSV ストリーミング書き出しに失敗しました", e);
			
		}
		
	}
	
	private String toCsvLine(Expense e) {
		
		return String.join(",",
				String.valueOf(e.getId()),
				String.valueOf(e.getApplicantId()),
				escapeCsv(e.getTitle()),
				e.getAmount() == null ? "" : e.getAmount().toPlainString(),
			    e.getCurrency() == null ? "" : e.getCurrency(),
			    e.getStatus() == null ? "" : e.getStatus().name(),
			    e.getSubmittedAt() == null ? "" : e.getSubmittedAt().toString());
	}
	
	
	/** カンマ・改行・ダブルクォートを含む値は"..."で囲み 内部の"は""に変換する*/
	private String escapeCsv(String value) {
		if(value == null) {
			return "";
		}
		if(value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		
		return value;
	}
}
