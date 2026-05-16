package com.docbase.domain.common.cache;

import com.docbase.infrastructure.cache.RedisUtil;
import com.docbase.infrastructure.cache.redis.CacheKeyEnum;
import com.docbase.infrastructure.cache.redis.RedisCacheTemplate;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import com.docbase.domain.system.post.db.SysPostEntity;
import com.docbase.domain.system.role.db.SysRoleEntity;
import com.docbase.domain.system.user.db.SysUserEntity;
import com.docbase.domain.system.post.db.SysPostService;
import com.docbase.domain.system.role.db.SysRoleService;
import com.docbase.domain.system.user.db.SysUserService;
import java.io.Serializable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author valarchie
 */
@Component
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisUtil redisUtil;
    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysPostService postService;

    public RedisCacheTemplate<String> captchaCache;
    public RedisCacheTemplate<SystemLoginUser> loginUserCache;
    public RedisCacheTemplate<SysUserEntity> userCache;
    public RedisCacheTemplate<SysRoleEntity> roleCache;

    public RedisCacheTemplate<SysPostEntity> postCache;

//    public RedisCacheTemplate<RoleInfo> roleModelInfoCache;

    @PostConstruct
    public void init() {

        captchaCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.CAPTCHAT);

        loginUserCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.LOGIN_USER_KEY);

        userCache = new RedisCacheTemplate<SysUserEntity>(redisUtil, CacheKeyEnum.USER_ENTITY_KEY) {
            @Override
            public SysUserEntity getObjectFromDb(Object id) {
                return userService.getById((Serializable) id);
            }
        };

        roleCache = new RedisCacheTemplate<SysRoleEntity>(redisUtil, CacheKeyEnum.ROLE_ENTITY_KEY) {
            @Override
            public SysRoleEntity getObjectFromDb(Object id) {
                return roleService.getById((Serializable) id);
            }
        };

//        roleModelInfoCache = new RedisCacheTemplate<RoleInfo>(redisUtil, CacheKeyEnum.ROLE_MODEL_INFO_KEY) {
//            @Override
//            public RoleInfo getObjectFromDb(Object id) {
//                UserDetailsService userDetailsService = SpringUtil.getBean(UserDetailsService.class);
//                return userDetailsService.getRoleInfo((Long) id);
//            }
//
//        };

        postCache = new RedisCacheTemplate<SysPostEntity>(redisUtil, CacheKeyEnum.POST_ENTITY_KEY) {
            @Override
            public SysPostEntity getObjectFromDb(Object id) {
                return postService.getById((Serializable) id);
            }

        };


    }


}
