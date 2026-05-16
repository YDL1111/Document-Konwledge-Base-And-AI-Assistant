package com.docbase.infrastructure.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Add a request id to MDC and response headers for traceability.
 */
@AllArgsConstructor
@Slf4j
public class TraceIdFilter implements Filter {

    private String requestIdKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        try {
            String uuid = UUID.randomUUID().toString();
            if (StrUtil.isNotEmpty(requestIdKey)) {
                MDC.put(requestIdKey, uuid);
                if (request instanceof HttpServletRequest) {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setHeader(requestIdKey, uuid);
                }
            } else {
                log.error("traceRequestIdKey is null, please check configuration");
            }

            chain.doFilter(request, response);
        } finally {
            removeRequestIdSafely(requestIdKey);
        }
    }

    public void removeRequestIdSafely(String requestIdKey) {
        try {
            MDC.remove(requestIdKey);
        } catch (Exception e) {
            log.error("failed to remove traceRequestIdKey from MDC", e);
        }
    }
}
