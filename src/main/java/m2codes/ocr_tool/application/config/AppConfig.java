package m2codes.ocr_tool.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionException;

@Configuration
@EnableAsync
@EnableScheduling
@EnableCaching
public class AppConfig {

    @Value("${physical-device.core-pool}")
    private int corePool;

    @Value("${physical-device.max-pool}")
    private int maxPool;

    @Value("${physical-device.queue-capacity}")
    private int queueCapacity;

    @Bean(name = "ocrTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePool);
        executor.setMaxPoolSize(maxPool);
        executor.setQueueCapacity(queueCapacity);
        executor.initialize();
        executor.setRejectedExecutionHandler((runnable, poolExecutor) -> {
            throw new RejectedExecutionException("Server overload! Task rejected");
        });
        return executor;
    }

}