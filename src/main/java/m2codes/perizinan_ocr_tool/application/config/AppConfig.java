package m2codes.perizinan_ocr_tool.application.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.TimeUnit;

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

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache entriDataCache = new CaffeineCache("entriDataCache",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build());
        CaffeineCache tokenCache = new CaffeineCache("tokenCache",
                Caffeine.newBuilder().expireAfterWrite(2, TimeUnit.HOURS).build());
        CaffeineCache clientTokenCache = new CaffeineCache("clientTokenCache",
                Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build());
        return new ConcurrentMapCacheManager(entriDataCache.getName(), tokenCache.getName(), clientTokenCache.getName());
    }

}