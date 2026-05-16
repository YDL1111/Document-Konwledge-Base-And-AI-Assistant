package com.docbase.infrastructure.cache.aop;

import cn.hutool.core.util.StrUtil;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

/**
 * Guava cache adapter.
 */
// @Component
public class GuavaCacheBean implements Cache {

    private com.google.common.cache.Cache<Object, Object> storage;

    @PostConstruct
    private void init() {
        storage = CacheBuilder.newBuilder()
            .maximumSize(100)
            .initialCapacity(16)
            .refreshAfterWrite(10, TimeUnit.MINUTES)
            .build();
    }

    @Override
    public String getName() {
        return CacheNameConstants.GUAVA;
    }

    @Override
    public ValueWrapper get(Object key) {
        if (Objects.isNull(key)) {
            return null;
        }
        Object ifPresent = storage.getIfPresent(key.toString());
        return Objects.isNull(ifPresent) ? null : new SimpleValueWrapper(ifPresent);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return null;
    }

    @Override
    public void put(Object key, Object value) {
        if (StrUtil.isEmpty((CharSequence) key)) {
            return;
        }
        storage.put(key, value);
    }

    @Override
    public void evict(Object key) {
        if (key == null) {
            return;
        }
        storage.invalidate(key);
    }

    @Override
    public void clear() {
    }

    @Override
    public Object getNativeCache() {
        return null;
    }
}
