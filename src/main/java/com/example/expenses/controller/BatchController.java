package com.example.expenses.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * バッチコントローラー
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>REST APIによるバッチジョブの手動実行</li>
 *   <li>JobLauncherの使い方</li>
 *   <li>ジョブ実行結果の取得</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    private final JobLauncher jobLauncher;
    private final Job monthlyExpenseReportJob;

    /**
     * 月次経費レポートジョブを手動実行します
     *
     * <p><b>リクエスト例:</b></p>
     * <pre>
     * POST /api/batch/jobs/monthly-report
     * </pre>
     *
     * @return ジョブ実行結果
     */
    @PostMapping("/jobs/monthly-report")
    public ResponseEntity<Map<String, Object>> executeMonthlyReportJob() {
        logger.info("月次経費レポートジョブの手動実行開始");

        try {
            // JobParametersを作成
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("executionTime", LocalDateTime.now())
                    .addString("trigger", "manual")
                    .toJobParameters();

            // ジョブ実行
            JobExecution jobExecution = jobLauncher.run(monthlyExpenseReportJob, jobParameters);

            // レスポンス作成
            Map<String, Object> response = new HashMap<>();
            response.put("jobExecutionId", jobExecution.getId());
            response.put("status", jobExecution.getStatus().name());
            response.put("startTime", jobExecution.getStartTime());
            response.put("endTime", jobExecution.getEndTime());
            response.put("exitCode", jobExecution.getExitStatus().getExitCode());

            if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                response.put("message", "月次レポートジョブが正常に完了しました");
                logger.info("月次経費レポートジョブの手動実行完了: jobExecutionId={}", jobExecution.getId());
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "月次レポートジョブが失敗しました");
                logger.warn("月次経費レポートジョブの手動実行失敗: jobExecutionId={}, status={}",
                        jobExecution.getId(), jobExecution.getStatus());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            logger.error("月次経費レポートジョブの手動実行エラー", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "ジョブ実行エラー: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
