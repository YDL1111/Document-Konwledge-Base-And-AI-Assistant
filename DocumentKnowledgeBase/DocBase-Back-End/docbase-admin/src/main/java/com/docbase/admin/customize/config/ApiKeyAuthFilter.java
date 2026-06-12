package com.docbase.admin.customize.config;

import com.docbase.infrastructure.user.web.RoleInfo;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API Key 认证过滤器 —— 供 Python 后端等内部服务通过 X-API-Key 请求头调用。
 * <p>
 * 校验通过后注入一个管理员级别的系统身份，确保 Agent 工具接口（以及其他需要
 * 内部互信的端点）可以在不携带用户 JWT 的情况下通过 Spring Security 鉴权。
 * <p>
 * 该过滤器在 JWT 过滤器之前执行：如果请求携带合法 API Key 则直接完成认证，
 * 跳过后续的 JWT 解析逻辑。
 */
@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    /**
     * 与 Python 端 JAVA_API_KEY / Java 端 docbase.ai.python.api-key 保持一致。
     * 为空时过滤器自动跳过，不会影响正常用户认证流程。
     */
    @Value("${docbase.ai.python.api-key:}")
    private String apiKey;

    /**
     * 内部服务使用的虚拟用户 ID，避免与真实用户 ID 冲突。
     */
    private static final long INTERNAL_SERVICE_USER_ID = -1L;

    private static final String API_KEY_HEADER = "X-API-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 未配置 API Key 时直接放行，不影响原有认证链路
        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 如果已经有认证信息（JWT 过滤器已处理），不再覆盖
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestApiKey = request.getHeader(API_KEY_HEADER);
        if (!StringUtils.hasText(requestApiKey) || !apiKey.equals(requestApiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        // API Key 校验通过，注入系统级管理员身份
        SystemLoginUser loginUser = buildInternalServiceUser();
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        loginUser, null, loginUser.getAuthorities());
        authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.debug("API Key authentication successful for request: {} {}",
                request.getMethod(), request.getRequestURI());

        filterChain.doFilter(request, response);
    }

    private SystemLoginUser buildInternalServiceUser() {
        RoleInfo roleInfo = new RoleInfo(
                RoleInfo.ADMIN_ROLE_ID,
                RoleInfo.ADMIN_ROLE_KEY,
                null,                           // dataScope — null = 不限制
                Collections.emptySet(),          // deptIdSet
                RoleInfo.ADMIN_PERMISSIONS,      // *:*:* 通配权限
                Collections.emptySet()           // menuIds
        );

        SystemLoginUser user = new SystemLoginUser();
        user.setUserId(INTERNAL_SERVICE_USER_ID);
        user.setAdmin(true);
        user.setUsername("internal-service");
        user.setPassword("");
        user.setRoleInfo(roleInfo);
        user.setDeptId(-1L);
        return user;
    }
}
