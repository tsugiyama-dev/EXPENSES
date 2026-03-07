package com.example.expenses.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 非同期処理の設定クラス
 *
 * <p>Spring の @Async アノテーションを有効化し、
 * 非同期処理用のスレッドプールを設定します。
 *
 * <p>設定内容:
 * <ul>
 *   <li>コアプールサイズ: 2スレッド（常時起動）</li>
 *   <li>最大プールサイズ: 5スレッド（負荷が高いときの上限）</li>
 *   <li>キューサイズ: 100タスク（待機キューの最大サイズ）</li>
 *   <li>スレッド名プレフィックス: "async-" （ログで識別しやすくする）</li>
 * </ul>
 *
 * <p>使用例:
 * <pre>
 * {@code
 * @Async
 * public void sendEmailAsync() {
 *     // この処理は非同期で実行される
 *     emailService.send(...);
 * }
 * }
 * </pre>
 *
 * @see org.springframework.scheduling.annotation.EnableAsync
 * @see org.springframework.scheduling.annotation.Async
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 非同期処理用のスレッドプールを設定
     *
     * <p>Bean名を "taskExecutor" にすることで、
     * Spring が自動的にこの Executor を @Async で使用します。
     *
     * @return 設定済みの ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // コアプールサイズ（常時起動しているスレッド数）
        executor.setCorePoolSize(2);

        // 最大プールサイズ（負荷が高いときの最大スレッド数）
        executor.setMaxPoolSize(5);

        // キューサイズ（待機タスクの最大数）
        executor.setQueueCapacity(100);

        // スレッド名のプレフィックス（ログで識別しやすくする）
        executor.setThreadNamePrefix("async-");

        // 初期化
        executor.initialize();

        return executor;
    }
}
