package com.example.expenses.batch.tasklet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.dto.batch.MonthlyExpenseReport;
import com.example.expenses.repository.ExpenseMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class DataAggregationTasklet implements Tasklet {

	private static final  Logger logger = LoggerFactory.getLogger(DataAggregationTasklet.class);
	private final ExpenseMapper expenseMapper;
	
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		logger.info("データ集計タスクレット開始");
		
		// 前月の年月を計算
//		YearMonth lastMonth = YearMonth.now();
		YearMonth lastMonth = YearMonth.now().minusMonths(1);
		LocalDateTime startDate = lastMonth.atDay(1).atStartOfDay();
		LocalDateTime endDate = lastMonth.atEndOfMonth().atTime(23, 59, 59);
		
		logger.info("集計期間:{} ～ {}", startDate, endDate);
		
		// 前月のExpenseデータ取得
		
		List<Expense> expenses = expenseMapper.findByPeriod(startDate, endDate);
		logger.info("取得件数:{}", expenses.size());
		
		// ステータス別集計
		Map<ExpenseStatus, MonthlyExpenseReport.StatusSummary> statusSummaries = new HashMap<>();
		BigDecimal totalAmount = BigDecimal.ZERO;
		
		for(ExpenseStatus status : ExpenseStatus.values()) {
			List<Expense> statusExpenses = expenses.stream()
					.filter(e -> e.getStatus() == status)
					.toList();
			
			BigDecimal statusAmount = statusExpenses.stream()
					.map(Expense::getAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			
			int count = statusExpenses.size();
			totalAmount = totalAmount.add(statusAmount);
			
			statusSummaries.put(status, MonthlyExpenseReport.StatusSummary.builder()
					.count(count)
					.amount(statusAmount)
					.build());
			logger.info("ステータス: {}, 件数: {}, 金額:{}", status, count, statusAmount);
		}
		
		for(MonthlyExpenseReport.StatusSummary summary : statusSummaries.values()) {
			if(totalAmount.compareTo(BigDecimal.ZERO)> 0 ) {
				double percentage = summary.getAmount()
//						.divide(BigDecimal.valueOf(summary.getCount()), 4, RoundingMode.HALF_UP)// (件数、小数点桁数、丸め処理)
						.divide(totalAmount, 6, RoundingMode.HALF_UP)// (合計、小数点桁数、丸め処理)
						.multiply(BigDecimal.valueOf(100))
						.doubleValue();
				summary.setPercentage(percentage);
			}
		}
		// レポート生成
		MonthlyExpenseReport report = MonthlyExpenseReport.builder()
				.targetMonth(lastMonth)
				.totalCount(expenses.size())
				.totalAmount(totalAmount)
				.statusSummaries(statusSummaries)
				.build();
		
		// ChunkContextにレポートを保存
		chunkContext.getStepContext()
		          .getStepExecution()
		          .getJobExecution()
		          .getExecutionContext()
		          .put("monthlyReport", report);
		logger.info("データ集計タスクレット完了: 合計{}件、合計金額{}", expenses.size(), totalAmount);
		
		
		
		
		return RepeatStatus.FINISHED;
	}

}
