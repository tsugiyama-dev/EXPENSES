package com.example.expenses.batch.tasklet;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

public class WarningTasklet implements Tasklet {

	@Override
	public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		
		Long writeCount = contribution.getStepExecution().getWriteCount();
		
        System.out.println("⚠️  警告: インポート件数が少ない (" + writeCount + " 件)");
        System.out.println("⚠️  100件以上のデータが推奨されます。");
        
		return RepeatStatus.FINISHED;
	}

}
