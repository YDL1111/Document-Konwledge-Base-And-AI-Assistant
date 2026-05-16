package com.docbase.domain.common.cache;

import com.docbase.infrastructure.cache.guava.AbstractGuavaCacheTemplate;
import com.docbase.infrastructure.cache.redis.RedisCacheTemplate;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import com.docbase.domain.system.dept.db.SysDeptEntity;
import com.docbase.domain.system.post.db.SysPostEntity;
import com.docbase.domain.system.role.db.SysRoleEntity;
import com.docbase.domain.system.user.db.SysUserEntity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 缓存中心  提供全局访问点
 * 如果是领域类的缓存  可以自己新建一个直接放在CacheCenter   不用放在infrastructure包里的GuavaCacheService
 * 或者RedisCacheService
 * @author valarchie
 */
@Component
@RequiredArgsConstructor
public class CacheCenter {

    private final GuavaCacheService guavaCache;
    private final RedisCacheService redisCache;

    public static AbstractGuavaCacheTemplate<String> configCache;

    public static AbstractGuavaCacheTemplate<SysDeptEntity> deptCache;

    public static RedisCacheTemplate<String> captchaCache;

    public static RedisCacheTemplate<SystemLoginUser> loginUserCache;

    public static RedisCacheTemplate<SysUserEntity> userCache;

    public static RedisCacheTemplate<SysRoleEntity> roleCache;

    public static RedisCacheTemplate<SysPostEntity> postCache;

    @PostConstruct
    public void init() {
        configCache = guavaCache.configCache;
        deptCache = guavaCache.deptCache;

        captchaCache = redisCache.captchaCache;
        loginUserCache = redisCache.loginUserCache;
        userCache = redisCache.userCache;
        roleCache = redisCache.roleCache;
        postCache = redisCache.postCache;
    }

}
