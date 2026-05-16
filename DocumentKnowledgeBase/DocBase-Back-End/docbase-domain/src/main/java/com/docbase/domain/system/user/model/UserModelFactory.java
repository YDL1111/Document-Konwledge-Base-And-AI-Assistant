package com.docbase.domain.system.user.model;

import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.domain.system.dept.model.DeptModelFactory;
import com.docbase.domain.system.post.model.PostModelFactory;
import com.docbase.domain.system.role.model.RoleModelFactory;
import com.docbase.domain.system.user.db.SysUserEntity;
import com.docbase.domain.system.user.db.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户模型工厂
 * @author valarchie
 */
@Component
@RequiredArgsConstructor
public class UserModelFactory {

    private final SysUserService userService;

    private final PostModelFactory postModelFactory;

    private final DeptModelFactory deptModelFactory;

    private final RoleModelFactory roleModelFactory;

    public UserModel loadById(Long userId) {
        SysUserEntity byId = userService.getById(userId);
        if (byId == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, userId, "用户");
        }
        return new UserModel(byId, userService, postModelFactory, deptModelFactory, roleModelFactory);
    }

    public UserModel create() {
        return new UserModel(userService, postModelFactory, deptModelFactory, roleModelFactory);
    }

}
