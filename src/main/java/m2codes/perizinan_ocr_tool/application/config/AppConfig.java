package m2codes.perizinan_ocr_tool.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AppConfig {

    @Value("${physical-device.core-pool}")
    private int corePool;

    @Value("${physical-device.max-pool}")
    private int maxPool;

    @Value("${physical-device.queue-capacity}")
    private int queueCapacity;

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePool);
        executor.setMaxPoolSize(maxPool);
        executor.setQueueCapacity(queueCapacity);
        executor.initialize();
        executor.setRejectedExecutionHandler((runnable, poolExecutor) ->
                System.err.println("Task " + runnable.toString() + " rejected from " + poolExecutor.toString()));
        return executor;
    }

}