package com.example.expenses.batch.tasklet;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

public class ErrorNotificationTasklet implements Tasklet {

	@Override
	public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		
        System.out.println("❌ エラー: インポート件数が0件です。");
        System.out.println("❌ CSVファイルの内容を確認してください。");
        
        // todo: ここでメール送信やSlack通知を行う
		return RepeatStatus.FINISHED;
	}

}
