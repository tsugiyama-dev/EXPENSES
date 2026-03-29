package com.example.expenses.batch.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * バッチスケジューラー
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>@Scheduledアノテーションの使い方</li>
 *   <li>Cron式の設定</li>
 *   <li>JobLauncherによるジョブ起動</li>
 *   <li>JobParametersによるパラメータ渡し</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BatchScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job monthlyExpenseReportJob;

    /**
     * 月次経費レポートジョブを自動実行します
     *
     * <p><b>実行タイミング:</b></p>
     * <ul>
     *   <li>毎月1日 AM 0:00に実行</li>
     *   <li>Cron式: 0 0 0 1 * ?</li>
     * </ul>
     *
     * <p><b>Cron式の説明:</b></p>
     * <pre>
     * 秒 分 時 日 月 曜日
     * 0  0  0  1  *  ?
     * │  │  │  │  │  └─ 曜日（? = 指定なし）
     * │  │  │  │  └──── 月（* = 毎月）
     * │  │  │  └─────── 日（1 = 1日）
     * │  │  └────────── 時（0 = 0時）
     * │  └───────────── 分（0 = 0分）
     * └──────────────── 秒（0 = 0秒）
     * </pre>
     */
    @Scheduled(cron = "${batch.schedule.monthly-report}")
    public void executeMonthlyExpenseReportJob() {
        logger.info("月次経費レポートジョブのスケジュール実行開始");

        try {
            // JobParametersを作成（同じジョブを複数回実行できるようにタイムスタンプを追加）
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("executionTime", LocalDateTime.now())
                    .toJobParameters();

            // ジョブ実行
            var jobExecution = jobLauncher.run(monthlyExpenseReportJob, jobParameters);

            logger.info("月次経費レポートジョブのスケジュール実行完了: status={}, exitCode={}",
                    jobExecution.getStatus(),
                    jobExecution.getExitStatus().getExitCode());

        } catch (Exception e) {
            logger.error("月次経費レポートジョブのスケジュール実行エラー", e);
        }
    }
}
