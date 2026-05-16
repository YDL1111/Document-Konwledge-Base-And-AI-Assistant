package com.docbase.infrastructure.annotations.unrepeatable;

import cn.hutool.json.JSONUtil;
import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.infrastructure.cache.RedisUtil;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * Prevent duplicate submissions on request bodies.
 */
@ControllerAdvice(basePackages = "com.docbase")
@Slf4j
@RequiredArgsConstructor
public class UnrepeatableInterceptor extends RequestBodyAdviceAdapter {

    private final RedisUtil redisUtil;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasMethodAnnotation(Unrepeatable.class);
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType) {
        String currentRequest = JSONUtil.toJsonStr(body);

        Unrepeatable resubmitAnno = parameter.getMethodAnnotation(Unrepeatable.class);
        if (resubmitAnno != null) {
            String redisKey = resubmitAnno.checkType().generateResubmitRedisKey(parameter.getMethod());
            log.info("repeat submit intercept, current key:{}, current params:{}", redisKey, currentRequest);

            String preRequest = redisUtil.getCacheObject(redisKey);
            if (preRequest != null && Objects.equals(currentRequest, preRequest)) {
                throw new ApiException(ErrorCode.Client.COMMON_REQUEST_RESUBMIT);
            }

            redisUtil.setCacheObject(redisKey, currentRequest, resubmitAnno.interval(), TimeUnit.SECONDS);
        }

        return body;
    }
}
