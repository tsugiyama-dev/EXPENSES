package com.example.expenses.batch.tasklet;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

public class ReportTasklet implements Tasklet {

	@Override
	public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		
		Long writeCount = contribution.getStepExecution().getWriteCount();
		
        System.out.println("========================================");
        System.out.println("  📊 インポートレポート");
        System.out.println("========================================");
        System.out.println("  取り込み件数: " + writeCount + " 件");
        System.out.println("  ステータス: 正常");
        System.out.println("========================================");
        
        // todo : 実際のレポート生成処理をここに実装する
		return RepeatStatus.FINISHED;
	}

}
