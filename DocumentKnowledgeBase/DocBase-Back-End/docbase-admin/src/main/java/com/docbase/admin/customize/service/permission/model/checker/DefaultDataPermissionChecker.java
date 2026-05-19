package com.docbase.admin.customize.service.permission.model.checker;

import com.docbase.infrastructure.user.web.SystemLoginUser;
import com.docbase.admin.customize.service.permission.model.AbstractDataPermissionChecker;
import com.docbase.admin.customize.service.permission.model.DataCondition;
import lombok.EqualsAndHashCode;

/**
 * 默认拒绝数据权限：始终返回 false，作为未匹配角色的安全兜底。
 */
@EqualsAndHashCode(callSuper = true)
public class DefaultDataPermissionChecker extends AbstractDataPermissionChecker {

    @Override
    public boolean check(SystemLoginUser loginUser, DataCondition condition) {
        return false;
    }

}
