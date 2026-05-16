package com.docbase.infrastructure.thread;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Ensure async thread pools are closed on application shutdown.
 */
@Component
@Slf4j
public class ShutdownHook {

    @PreDestroy
    public void destroy() {
        shutdownAllThreadPool();
    }

    private void shutdownAllThreadPool() {
        try {
            log.info("close thread pool");
            ThreadPoolManager.shutdown();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
