package com.docbase.admin.customize.service.login;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.docbase.common.constant.Constants.Token;
import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.domain.common.cache.RedisCacheService;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Token processing service.
 */
@Component
@Slf4j
@Data
@RequiredArgsConstructor
public class TokenService {

    @Value("${token.header}")
    private String header;

    @Value("${token.secret}")
    private String secret;

    @Value("${token.autoRefreshTime}")
    private long autoRefreshTime;

    private final RedisCacheService redisCache;

    public SystemLoginUser getLoginUser(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (StrUtil.isNotEmpty(token)) {
            try {
                Claims claims = parseToken(token);
                String uuid = (String) claims.get(Token.LOGIN_USER_KEY);
                return redisCache.loginUserCache.getObjectOnlyInCacheById(uuid);
            } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
                log.error("parse token failed.", ex);
                throw new ApiException(ex, ErrorCode.Client.INVALID_TOKEN);
            } catch (Exception e) {
                log.error("fail to get cached user from redis", e);
                throw new ApiException(e, ErrorCode.Client.TOKEN_PROCESS_FAILED, e.getMessage());
            }
        }
        return null;
    }

    public String createTokenAndPutUserInCache(SystemLoginUser loginUser) {
        loginUser.setCachedKey(IdUtil.fastUUID());
        redisCache.loginUserCache.set(loginUser.getCachedKey(), loginUser);
        return generateToken(MapUtil.of(Token.LOGIN_USER_KEY, loginUser.getCachedKey()));
    }

    public void refreshToken(SystemLoginUser loginUser) {
        long currentTime = System.currentTimeMillis();
        if (currentTime > loginUser.getAutoRefreshCacheTime()) {
            loginUser.setAutoRefreshCacheTime(currentTime + TimeUnit.MINUTES.toMillis(autoRefreshTime));
            redisCache.loginUserCache.set(loginUser.getCachedKey(), loginUser);
        }
    }

    private String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
            .setClaims(claims)
            .signWith(getSigningKey())
            .compact();
    }

    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader(header);
        if (StrUtil.isNotEmpty(token) && token.startsWith(Token.PREFIX)) {
            token = StrUtil.stripIgnoreCase(token, Token.PREFIX, null);
        }
        return token;
    }
}
