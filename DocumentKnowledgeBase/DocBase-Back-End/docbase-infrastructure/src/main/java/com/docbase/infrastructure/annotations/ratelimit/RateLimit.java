package com.docbase.infrastructure.annotations.ratelimit;

import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.common.utils.ServletHolderUtil;
import com.docbase.infrastructure.user.AuthenticationUtils;
import com.docbase.infrastructure.user.app.AppLoginUser;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate limit annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    String key() default "None";

    int time() default 60;

    int maxCount() default 100;

    LimitType limitType() default LimitType.GLOBAL;

    CacheType cacheType() default CacheType.REDIS;

    enum LimitType {
        GLOBAL {
            @Override
            public String generateCombinedKey(RateLimit rateLimiter) {
                return rateLimiter.key() + this.name();
            }
        },
        IP {
            @Override
            public String generateCombinedKey(RateLimit rateLimiter) {
                return rateLimiter.key() + resolveClientIp();
            }
        },
        SYSTEM_USER {
            @Override
            public String generateCombinedKey(RateLimit rateLimiter) {
                SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
                if (loginUser == null) {
                    throw new ApiException(ErrorCode.Client.COMMON_NO_AUTHORIZATION);
                }
                return rateLimiter.key() + loginUser.getUsername();
            }
        },
        APP_USER {
            @Override
            public String generateCombinedKey(RateLimit rateLimiter) {
                AppLoginUser loginUser = AuthenticationUtils.getAppLoginUser();
                if (loginUser == null) {
                    throw new ApiException(ErrorCode.Client.COMMON_NO_AUTHORIZATION);
                }
                return rateLimiter.key() + loginUser.getUsername();
            }
        };

        public abstract String generateCombinedKey(RateLimit rateLimiter);

        private static String resolveClientIp() {
            String xForwardedFor = ServletHolderUtil.getRequest().getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = ServletHolderUtil.getRequest().getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return xRealIp.trim();
            }
            return ServletHolderUtil.getRequest().getRemoteAddr();
        }
    }

    enum CacheType {
        REDIS,
        Map
    }
}
