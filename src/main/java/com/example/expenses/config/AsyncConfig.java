package com.example.expenses.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 非同期処理の設定クラス
 * -@EnableAsyncを使用して非同期処理を有効化
 * -AsyncConfigurerを実装して非同期処理の詳細な設定を行うことができる
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	/**
	 * 非同期処理のスレッドプール設定
	 */
	@Override
	public Executor getAsyncExecutor() {
		
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("ExpenseAsync-");
		executor.initialize();
		return executor;
		
		//カスタムスレッドプールを使用する場合は以下のように実装
		/*
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5); //コアスレッド数
		executor.setMaxPoolSize(10); //最大スレッド数
		executor.setQueueCapacity(25); //キューの容量
		executor.initialize();
		return executor;
		 */
	}
	
}
