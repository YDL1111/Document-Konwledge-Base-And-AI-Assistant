package com.docbase.admin.customize.service.permission;

import com.docbase.admin.customize.service.permission.model.AbstractDataPermissionChecker;
import com.docbase.admin.customize.service.permission.model.checker.AllDataPermissionChecker;
import com.docbase.admin.customize.service.permission.model.checker.CustomDataPermissionChecker;
import com.docbase.admin.customize.service.permission.model.checker.DefaultDataPermissionChecker;
import com.docbase.admin.customize.service.permission.model.checker.DeptTreeDataPermissionChecker;
import com.docbase.admin.customize.service.permission.model.checker.OnlySelfDataPermissionChecker;
import com.docbase.admin.customize.service.permission.model.checker.SingleDeptDataPermissionChecker;
import com.docbase.domain.system.dept.db.SysDeptService;
import com.docbase.infrastructure.user.web.DataScopeEnum;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 数据权限检测器工厂
 * @author valarchie
 */
@Component
public class DataPermissionCheckerFactory {
    private final SysDeptService deptService;

    private static AbstractDataPermissionChecker allChecker;
    private static AbstractDataPermissionChecker customChecker;
    private static AbstractDataPermissionChecker singleDeptChecker;
    private static AbstractDataPermissionChecker deptTreeChecker;
    private static AbstractDataPermissionChecker onlySelfChecker;
    private static AbstractDataPermissionChecker defaultSelfChecker;

    public DataPermissionCheckerFactory(SysDeptService deptService) {
        this.deptService = deptService;
    }

    @PostConstruct
    public void initAllChecker() {
        allChecker = new AllDataPermissionChecker();
        customChecker = new CustomDataPermissionChecker(deptService);
        singleDeptChecker = new SingleDeptDataPermissionChecker(deptService);
        deptTreeChecker = new DeptTreeDataPermissionChecker(deptService);
        onlySelfChecker = new OnlySelfDataPermissionChecker(deptService);
        defaultSelfChecker = new DefaultDataPermissionChecker();
    }


    public static AbstractDataPermissionChecker getChecker(SystemLoginUser loginUser) {
        if (loginUser == null) {
            return deptTreeChecker;
        }

        DataScopeEnum dataScope = loginUser.getRoleInfo().getDataScope();
        switch (dataScope) {
            case ALL:
                return allChecker;
            case CUSTOM_DEFINE:
                return customChecker;
            case SINGLE_DEPT:
                return singleDeptChecker;
            case DEPT_TREE:
                return deptTreeChecker;
            case ONLY_SELF:
                return onlySelfChecker;
            default:
                return defaultSelfChecker;
        }
    }

}
