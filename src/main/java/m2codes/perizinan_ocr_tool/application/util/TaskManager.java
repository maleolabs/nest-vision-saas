package m2codes.perizinan_ocr_tool.application.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskManager {

    private int activeCount;

    private int poolSize;

    private int maxPoolSize;

    private final ThreadPoolTaskExecutor taskExecutor;

    public TaskManager(ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        setAll();
    }

    public boolean isPoolAvailable() {
        return (poolSize - activeCount) > 0;
    }

//    @Scheduled(fixedRate = 5000)
    public void poolMonitor() {
        setAll();
        log.info("ACTIVE COUNT : {}, POOL SIZE : {}, MAX POOL SIZE : {}", activeCount, poolSize, maxPoolSize);
    }

    private void setAll() {
        activeCount = taskExecutor.getActiveCount();
        poolSize = taskExecutor.getPoolSize();
        maxPoolSize = taskExecutor.getMaxPoolSize();
    }

}
