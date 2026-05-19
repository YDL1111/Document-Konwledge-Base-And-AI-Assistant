package com.docbase.admin.customize.service.permission;

import cn.hutool.core.collection.CollUtil;
import com.docbase.domain.knowledge.category.db.KnowledgeCategoryEntity;
import com.docbase.domain.knowledge.document.KnowledgeDocumentConstant;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import com.docbase.domain.knowledge.document.query.KnowledgeDocumentQuery;
import com.docbase.domain.knowledge.category.query.KnowledgeCategoryQuery;
import com.docbase.domain.knowledge.ingest.query.KnowledgeIngestTaskQuery;
import com.docbase.domain.system.dept.db.SysDeptService;
import com.docbase.infrastructure.user.AuthenticationUtils;
import com.docbase.infrastructure.user.web.DataScopeEnum;
import com.docbase.infrastructure.user.web.RoleInfo;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 知识库模块统一数据权限辅助类。
 * 将 Controller 和 ApplicationService 中散落的 isAdmin + deptId + creatorId
 * 判断收敛到一处，并复用系统已有的 DataScopeEnum 五级数据权限模型。
 */
@Component
@RequiredArgsConstructor
public class KnowledgePermissionHelper {

    private final SysDeptService sysDeptService;

    // ──────────────── 用户上下文 ────────────────

    private SystemLoginUser currentUser() {
        return AuthenticationUtils.getSystemLoginUser();
    }

    private DataScopeEnum currentDataScope() {
        RoleInfo roleInfo = currentUser().getRoleInfo();
        return roleInfo != null ? roleInfo.getDataScope() : DataScopeEnum.ONLY_SELF;
    }

    private boolean isAdmin() {
        return currentUser().isAdmin();
    }

    /**
     * 当前用户可访问的部门 ID 集合（含 DEPT_TREE 子部门展开）。
     */
    private List<Long> accessibleDeptIds() {
        SystemLoginUser user = currentUser();
        RoleInfo roleInfo = user.getRoleInfo();
        if (roleInfo == null) {
            return Collections.singletonList(user.getDeptId());
        }
        DataScopeEnum scope = roleInfo.getDataScope();
        switch (scope) {
            case ALL:
                return Collections.emptyList(); // 空列表 = 不过滤
            case CUSTOM_DEFINE:
                Set<Long> deptIdSet = roleInfo.getDeptIdSet();
                return deptIdSet != null ? new ArrayList<>(deptIdSet) : Collections.emptyList();
            case SINGLE_DEPT:
                return Collections.singletonList(user.getDeptId());
            case DEPT_TREE:
                return sysDeptService.getDeptAndChildrenIds(user.getDeptId());
            case ONLY_SELF:
            default:
                return Collections.singletonList(user.getDeptId());
        }
    }

    /**
     * 当前用户是否有权访问指定部门的数据。
     */
    private boolean canAccessDept(Long targetDeptId) {
        if (isAdmin()) {
            return true;
        }
        if (targetDeptId == null) {
            return false;
        }
        DataScopeEnum scope = currentDataScope();
        if (scope == DataScopeEnum.ALL) {
            return true;
        }
        if (scope == DataScopeEnum.CUSTOM_DEFINE) {
            Set<Long> deptIdSet = currentUser().getRoleInfo().getDeptIdSet();
            return deptIdSet != null && deptIdSet.contains(targetDeptId);
        }
        if (scope == DataScopeEnum.DEPT_TREE) {
            if (Objects.equals(targetDeptId, currentUser().getDeptId())) {
                return true;
            }
            return sysDeptService.isChildOfTheDept(currentUser().getDeptId(), targetDeptId);
        }
        // SINGLE_DEPT / ONLY_SELF
        return Objects.equals(targetDeptId, currentUser().getDeptId());
    }

    // ──────────────── Query 范围注入 ────────────────

    /**
     * 根据当前用户的 DataScopeEnum 向文档查询对象注入过滤条件。
     */
    public void applyDocumentQueryScope(KnowledgeDocumentQuery query) {
        if (isAdmin()) {
            return;
        }
        // 非管理员：始终能看到自己的文档 + 已发布共享文档
        query.setCreatorId(currentUser().getUserId());
        query.setDeptId(currentUser().getDeptId());
        query.setOnlyVisibleToCurrentUser(Boolean.TRUE);
        query.setIncludePublishedShared(Boolean.TRUE);
        DataScopeEnum scope = currentDataScope();
        if (scope != DataScopeEnum.ALL) {
            query.setDeptIdList(accessibleDeptIds());
        }
    }

    /**
     * 根据当前用户的 DataScopeEnum 向分类查询对象注入过滤条件。
     */
    public void applyCategoryQueryScope(KnowledgeCategoryQuery query) {
        if (isAdmin()) {
            return;
        }
        DataScopeEnum scope = currentDataScope();
        if (scope == DataScopeEnum.ALL) {
            return;
        }
        if (scope == DataScopeEnum.ONLY_SELF) {
            query.setCreatorId(currentUser().getUserId());
            return;
        }
        query.setDeptIdList(accessibleDeptIds());
    }

    /**
     * 根据当前用户的 DataScopeEnum 向入库任务查询对象注入过滤条件。
     */
    public void applyIngestTaskQueryScope(KnowledgeIngestTaskQuery query) {
        if (isAdmin()) {
            return;
        }
        DataScopeEnum scope = currentDataScope();
        if (scope == DataScopeEnum.ALL) {
            return;
        }
        if (scope == DataScopeEnum.ONLY_SELF) {
            query.setCreatorId(currentUser().getUserId());
            return;
        }
        // 入库任务目前只有 creatorId 过滤能力，部门级暂不做
        query.setDeptIdList(accessibleDeptIds());
    }

    // ──────────────── 实体访问权判断 ────────────────

    /**
     * 判断当前用户是否有权查看某篇文档。
     * 规则：创建人永远可见 + 已发布文档按 visibility 判定。
     */
    public boolean canViewDocument(KnowledgeDocumentEntity entity) {
        if (isAdmin()) {
            return true;
        }
        Long currentUserId = currentUser().getUserId();
        // 1. 创建人永远可见
        if (Objects.equals(entity.getCreatorId(), currentUserId)) {
            return true;
        }
        // 2. 非已发布文档，非创建人不可见
        if (!Objects.equals(entity.getStatus(), KnowledgeDocumentConstant.Status.PUBLISHED)) {
            return false;
        }
        // 3. 全员可见
        if (Objects.equals(entity.getVisibility(), KnowledgeDocumentConstant.Visibility.PUBLIC)) {
            return true;
        }
        // 4. 本部门可见
        if (Objects.equals(entity.getVisibility(), KnowledgeDocumentConstant.Visibility.DEPT)) {
            return canAccessDept(entity.getDeptId());
        }
        // 5. 仅本人可见 → 非创建人不可见
        return false;
    }

    /**
     * 判断当前用户是否有权访问某分类。
     */
    public void checkCategoryAccessible(KnowledgeCategoryEntity entity) {
        if (isAdmin()) {
            return;
        }
        if (!canAccessDept(entity.getDeptId())) {
            throw new com.docbase.common.exception.ApiException(
                com.docbase.common.exception.error.ErrorCode.Business.PERMISSION_NOT_ALLOWED_TO_OPERATE);
        }
    }

    /**
     * 判断当前用户是否有权审核某文档。
     */
    public void checkDocumentAuditable(KnowledgeDocumentEntity entity) {
        if (isAdmin()) {
            return;
        }
        if (!canAccessDept(entity.getDeptId())) {
            throw new com.docbase.common.exception.ApiException(
                com.docbase.common.exception.error.ErrorCode.Business.PERMISSION_NOT_ALLOWED_TO_OPERATE);
        }
    }

    /**
     * 解析目标部门 ID：管理员可指定，非管理员强制使用本人部门。
     */
    public Long resolveTargetDeptId(Long commandDeptId) {
        if (isAdmin()) {
            return commandDeptId;
        }
        return currentUser().getDeptId();
    }
}
