package com.example.expenses.batch.tasklet;

import com.example.expenses.dto.batch.MonthlyExpenseReport;
import com.example.expenses.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.format.DateTimeFormatter;

/**
 * メール通知タスクレット
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>JavaMailSenderの使い方</li>
 *   <li>ファイル添付メールの送信</li>
 *   <li>HTMLメールの作成</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class EmailNotificationTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationTasklet.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${batch.notification.admin-email}")
    private String adminEmail;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws MessagingException {
        logger.info("メール通知タスクレット開始");

        // レポート情報を取得
        MonthlyExpenseReport report = (MonthlyExpenseReport) chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .get("monthlyReport");

        String reportFilePath = (String) chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .get("reportFilePath");

        if (report == null || reportFilePath == null) {
            throw new IllegalStateException("レポート情報が見つかりません");
        }

        // メール送信
        sendNotificationEmail(report, reportFilePath);

        logger.info("メール通知タスクレット完了");

        return RepeatStatus.FINISHED;
    }

    /**
     * 通知メールを送信します
     */
    private void sendNotificationEmail(MonthlyExpenseReport report, String reportFilePath) throws MessagingException {
        var message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");

        // 宛先設定
        helper.setFrom(fromEmail);
        helper.setTo(adminEmail);
        helper.setSubject(String.format("【経費管理】月次レポート（%s）",
                report.getTargetMonth().format(DateTimeFormatter.ofPattern("yyyy年MM月"))));

        // メール本文（HTML）
        String emailContent = buildEmailContent(report);
        helper.setText(emailContent, true);

        // Excelファイルを添付
        File reportFile = new File(reportFilePath);
        if (reportFile.exists()) {
            FileSystemResource file = new FileSystemResource(reportFile);
            helper.addAttachment(reportFile.getName(), file);
        }

        // メール送信
        mailSender.send(message);
        logger.info("メール送信完了: {}", adminEmail);
    }

    /**
     * メール本文（HTML）を作成します
     */
    private String buildEmailContent(MonthlyExpenseReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body>");
        sb.append("<h2>月次経費レポート</h2>");
        sb.append("<p>集計期間: ").append(report.getTargetMonth().format(DateTimeFormatter.ofPattern("yyyy年MM月"))).append("</p>");
        sb.append("<hr>");

        sb.append("<h3>集計結果</h3>");
        sb.append("<table border='1' cellpadding='5' cellspacing='0'>");
        sb.append("<tr style='background-color: #4CAF50; color: white;'>");
        sb.append("<th>ステータス</th><th>件数</th><th>合計金額</th><th>割合</th>");
        sb.append("</tr>");

        report.getStatusSummaries().forEach((status, summary) -> {
            sb.append("<tr>");
            sb.append("<td>").append(status.toString()).append("</td>");
            sb.append("<td>").append(summary.getCount()).append("件</td>");
            sb.append("<td>¥").append(String.format("%,d", summary.getAmount().longValue())).append("</td>");
            sb.append("<td>").append(String.format("%.1f%%", summary.getPercentage())).append("</td>");
            sb.append("</tr>");
        });

        sb.append("<tr style='background-color: #f0f0f0; font-weight: bold;'>");
        sb.append("<td>合計</td>");
        sb.append("<td>").append(report.getTotalCount()).append("件</td>");
        sb.append("<td>¥").append(String.format("%,d", report.getTotalAmount().longValue())).append("</td>");
        sb.append("<td>100.0%</td>");
        sb.append("</tr>");
        sb.append("</table>");

        sb.append("<hr>");
        sb.append("<p>詳細は添付のExcelファイルをご確認ください。</p>");
        sb.append("<p>※このメールは自動送信されています。</p>");
        sb.append("</body></html>");

        return sb.toString();
    }
}
