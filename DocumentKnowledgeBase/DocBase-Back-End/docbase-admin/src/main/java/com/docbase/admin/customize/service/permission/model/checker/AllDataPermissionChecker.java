package com.docbase.admin.customize.service.permission.model.checker;

import com.docbase.infrastructure.user.web.SystemLoginUser;
import com.docbase.admin.customize.service.permission.model.AbstractDataPermissionChecker;
import com.docbase.admin.customize.service.permission.model.DataCondition;
import lombok.EqualsAndHashCode;

/**
 * 全部数据权限：始终返回 true，供管理员角色使用。
 */
@EqualsAndHashCode(callSuper = true)
public class AllDataPermissionChecker extends AbstractDataPermissionChecker {

    @Override
    public boolean check(SystemLoginUser loginUser, DataCondition condition) {
        return true;
    }
}
