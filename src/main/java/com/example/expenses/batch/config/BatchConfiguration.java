package com.example.expenses.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.expenses.batch.tasklet.DataAggregationTasklet;
import com.example.expenses.batch.tasklet.EmailNotificationTasklet;
import com.example.expenses.batch.tasklet.ReportGenerationTasklet;

import lombok.RequiredArgsConstructor;

/**
 * Spring Batchバッチ設定
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>Spring Batchの基本構成</li>
 *   <li>ジョブ・ステップの定義</li>
 *   <li>タスクレット方式の実装</li>
 *   <li>トランザクション管理</li>
 * </ul>
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class BatchConfiguration {

    private final DataAggregationTasklet dataAggregationTasklet;
    private final ReportGenerationTasklet reportGenerationTasklet;
    private final EmailNotificationTasklet emailNotificationTasklet;

    /**
     * 月次経費レポートジョブ
     *
     * <p>ジョブは複数のステップで構成されます：</p>
     * <ol>
     *   <li>データ集計ステップ</li>
     *   <li>レポート生成ステップ</li>
     *   <li>メール通知ステップ</li>
     * </ol>
     */
    @Bean
    public Job monthlyExpenseReportJob(JobRepository jobRepository,
                                       Step dataAggregationStep,
                                       Step reportGenerationStep,
                                       Step emailNotificationStep) {
        return new JobBuilder("monthlyExpenseReportJob", jobRepository)
                .start(dataAggregationStep)      // Step 1: データ集計
                .next(reportGenerationStep)      // Step 2: レポート生成
                .next(emailNotificationStep)     // Step 3: メール通知
                .build();
    }

    /**
     * Step 1: データ集計ステップ
     *
     * <p>前月のExpenseデータを集計します</p>
     */
    @Bean
    public Step dataAggregationStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager) {
        return new StepBuilder("dataAggregationStep", jobRepository)
                .tasklet(dataAggregationTasklet, transactionManager)
                .build();
    }

    /**
     * Step 2: レポート生成ステップ
     *
     * <p>集計結果をExcelレポートとして生成します</p>
     */
    @Bean
    public Step reportGenerationStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
        return new StepBuilder("reportGenerationStep", jobRepository)
                .tasklet(reportGenerationTasklet, transactionManager)
                .build();
    }

    /**
     * Step 3: メール通知ステップ
     *
     * <p>管理者にレポートをメール送信します</p>
     */
    @Bean
    public Step emailNotificationStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        return new StepBuilder("emailNotificationStep", jobRepository)
                .tasklet(emailNotificationTasklet, transactionManager)
                .build();
    }
}
