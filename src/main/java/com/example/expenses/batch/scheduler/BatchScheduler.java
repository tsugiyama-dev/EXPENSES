package com.example.expenses.batch.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BatchScheduler {

	private static final Logger logger = LoggerFactory.getLogger(BatchScheduler.class);
	
	private final MonthlyReportScheduler monthlyReportScheduler;
	
	@Scheduled(cron = "${batch.schedule.monthly-report}") // 毎月1日の午前0時に実行
	public void  executeMonthlyExpenseReportJob() {
		logger.info("月次経費レポートジョブのスケジュール実行開始");
		
		monthlyReportScheduler.startMonthlyReportJob();
	}
	
	
}
