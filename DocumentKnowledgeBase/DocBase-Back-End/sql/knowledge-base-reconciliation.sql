-- ============================================================
-- DocBase 知识库菜单与权限 整合脚本 (v2)
-- ============================================================
-- 说明：
--   本脚本定义了知识库模块所需的全部菜单、按钮权限及角色授权
--   的最终状态。执行本脚本后即可得到一致的知识库菜单体系。
--
-- 取代关系（旧脚本可保留但不再单独执行）：
--   - knowledge-base-menu-init.sql      → 本脚本涵盖并统一了其内容
--   - knowledge-base-member-init.sql    → 本脚本涵盖并统一了其内容
--   - repair-menu-structure.sql         → 菜单结构调整已合并
--   - knowledge-document-audit-upgrade.sql → menu_id 冲突已解决(139)
--   - repair-ai-audit-menu.sql          → AI 菜单结构已合并
--   - repair-ai-chat-menu-meta.sql      → AI 菜单元数据已合并
--   - remove-role2-document-audit-permission.sql → 角色授权已统一
-- ============================================================

USE `docbase_knowledge`;

-- ============================================================
-- 一、知识库一级目录 (menu_id=100)
-- ============================================================
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES
(100, '知识库管理', 2, '', 0, '/knowledge', 0, '', '{"title":"知识库管理","icon":"ep:collection","showParent":true,"rank":4}', 1, '知识库管理目录', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_type` = VALUES(`menu_type`),
  `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`),
  `permission` = VALUES(`permission`), `meta_info` = VALUES(`meta_info`),
  `status` = VALUES(`status`), `remark` = VALUES(`remark`),
  `updater_id` = VALUES(`updater_id`), `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);

-- ============================================================
-- 二、知识库页面菜单 (menu_id=101~107)
-- ============================================================
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES
(101, '知识库工作台', 1, 'KnowledgeWorkspace', 100, '/knowledge/workspace/index', 0, 'knowledge:workspace:view', '{"title":"知识库工作台","icon":"ep:home-filled","showParent":true}', 1, '知识库工作台页面', 1, NOW(), 1, NOW(), 0),
(102, '分类管理',     1, 'KnowledgeCategory',  100, '/knowledge/category/index',  0, 'knowledge:category:list',  '{"title":"分类管理","icon":"ep:folder","showParent":true}',         1, '知识分类管理页面',   1, NOW(), 1, NOW(), 0),
(103, '文档管理',     1, 'KnowledgeDocument',  100, '/knowledge/document/index',  0, 'knowledge:document:list',  '{"title":"文档管理","icon":"ep:document","showParent":true}',       1, '知识文档管理页面',   1, NOW(), 1, NOW(), 0),
(104, '导入任务',     1, 'KnowledgeIngestTask', 100, '/knowledge/ingest/index',    0, 'knowledge:ingest:list',   '{"title":"导入任务","icon":"ep:operation","showParent":true}',      1, '知识导入任务页面',   1, NOW(), 1, NOW(), 0),
(105, 'AI问答',       1, 'AiChat',              0,   '/ai/chat/index',            0, 'ai:chat:list',            '{"title":"AI问答","icon":"ep:chat-dot-round","showParent":true,"rank":5}',   1, 'AI问答页面',         1, NOW(), 1, NOW(), 0),
(106, 'AI审计',       1, 'AiAudit',             0,   '/ai/audit/index',           0, 'ai:audit:list',           '{"title":"AI审计","icon":"ep:histogram","showParent":true,"rank":6}',       1, 'AI审计页面',         1, NOW(), 1, NOW(), 0),
(107, '个人中心',     1, 'UserProfile',         100, '/system/user/profile',       0, 'system:user:profile',     '{"title":"个人中心","icon":"ep:user-filled","showParent":true}',     1, '普通用户个人中心',   1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_type` = VALUES(`menu_type`),
  `router_name` = VALUES(`router_name`), `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`), `is_button` = VALUES(`is_button`),
  `permission` = VALUES(`permission`), `meta_info` = VALUES(`meta_info`),
  `status` = VALUES(`status`), `remark` = VALUES(`remark`),
  `updater_id` = VALUES(`updater_id`), `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);

-- ============================================================
-- 三、按钮权限 (menu_id=120~139)
-- ============================================================
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES
-- 分类管理按钮 (parent_id=102)
(120, '分类查询', 3, '', 102, '', 1, 'knowledge:category:query',  '{}', 1, '分类查询按钮', 1, NOW(), 1, NOW(), 0),
(121, '分类新增', 3, '', 102, '', 1, 'knowledge:category:add',    '{}', 1, '分类新增按钮', 1, NOW(), 1, NOW(), 0),
(122, '分类编辑', 3, '', 102, '', 1, 'knowledge:category:edit',   '{}', 1, '分类编辑按钮', 1, NOW(), 1, NOW(), 0),
(123, '分类删除', 3, '', 102, '', 1, 'knowledge:category:remove', '{}', 1, '分类删除按钮', 1, NOW(), 1, NOW(), 0),

-- 文档管理按钮 (parent_id=103)
(130, '文档查询', 3, '', 103, '', 1, 'knowledge:document:query',    '{}', 1, '文档查询按钮',     1, NOW(), 1, NOW(), 0),
(131, '文档上传', 3, '', 103, '', 1, 'knowledge:document:upload',   '{}', 1, '文档上传按钮',     1, NOW(), 1, NOW(), 0),
(132, '文档编辑', 3, '', 103, '', 1, 'knowledge:document:edit',     '{}', 1, '文档编辑按钮',     1, NOW(), 1, NOW(), 0),
(133, '文档删除', 3, '', 103, '', 1, 'knowledge:document:remove',   '{}', 1, '文档删除按钮',     1, NOW(), 1, NOW(), 0),
(134, '文档预览', 3, '', 103, '', 1, 'knowledge:document:preview',  '{}', 1, '文档预览按钮',     1, NOW(), 1, NOW(), 0),
(135, '文档下载', 3, '', 103, '', 1, 'knowledge:document:download', '{}', 1, '文档下载按钮',     1, NOW(), 1, NOW(), 0),
(136, '提交导入', 3, '', 103, '', 1, 'knowledge:document:ingest',   '{}', 1, '文档提交导入按钮', 1, NOW(), 1, NOW(), 0),
(137, '版本回滚', 3, '', 103, '', 1, 'knowledge:document:rollback', '{}', 1, '文档版本回滚按钮', 1, NOW(), 1, NOW(), 0),
(138, '新增文档', 3, '', 103, '', 1, 'knowledge:document:add',      '{}', 1, '文档新增按钮',     1, NOW(), 1, NOW(), 0),
(139, '文档审核', 3, '', 103, '', 1, 'knowledge:document:audit',    '{}', 1, '文档审核按钮',     1, NOW(), 1, NOW(), 0),

-- 导入任务按钮 (parent_id=104)
(140, '任务查询', 3, '', 104, '', 1, 'knowledge:ingest:query',  '{}', 1, '导入任务查询按钮', 1, NOW(), 1, NOW(), 0),
(141, '任务重试', 3, '', 104, '', 1, 'knowledge:ingest:retry',  '{}', 1, '导入任务重试按钮', 1, NOW(), 1, NOW(), 0),
(142, '任务详情', 3, '', 104, '', 1, 'knowledge:ingest:detail', '{}', 1, '导入任务详情按钮', 1, NOW(), 1, NOW(), 0),

-- AI问答按钮 (parent_id=105)
(150, '会话查询', 3, '', 105, '', 1, 'ai:chat:query',  '{}', 1, 'AI问答会话查询按钮', 1, NOW(), 1, NOW(), 0),
(151, '发送消息', 3, '', 105, '', 1, 'ai:chat:send',   '{}', 1, 'AI问答发送消息按钮', 1, NOW(), 1, NOW(), 0),
(152, '删除会话', 3, '', 105, '', 1, 'ai:chat:remove', '{}', 1, 'AI问答删除会话按钮', 1, NOW(), 1, NOW(), 0),

-- AI审计按钮 (parent_id=106)
(160, '审计查询', 3, '', 106, '', 1, 'ai:audit:query',  '{}', 1, 'AI审计查询按钮', 1, NOW(), 1, NOW(), 0),
(161, '审计导出', 3, '', 106, '', 1, 'ai:audit:export', '{}', 1, 'AI审计导出按钮', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_type` = VALUES(`menu_type`),
  `parent_id` = VALUES(`parent_id`), `is_button` = VALUES(`is_button`),
  `permission` = VALUES(`permission`), `meta_info` = VALUES(`meta_info`),
  `status` = VALUES(`status`), `remark` = VALUES(`remark`),
  `updater_id` = VALUES(`updater_id`), `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);

-- ============================================================
-- 四、角色授权
-- ============================================================

-- 4.1 管理员 (role_id=1) — 拥有全部知识库菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 100), (1, 101), (1, 102), (1, 103), (1, 104), (1, 105), (1, 106), (1, 107),
(1, 120), (1, 121), (1, 122), (1, 123),
(1, 130), (1, 131), (1, 132), (1, 133), (1, 134), (1, 135), (1, 136), (1, 137), (1, 138), (1, 139),
(1, 140), (1, 141), (1, 142),
(1, 150), (1, 151), (1, 152),
(1, 160), (1, 161);

-- 4.2 知识库普通用户 (role_id=2) — 受限权限
-- 先清除旧授权，再重新分配
DELETE FROM `sys_role_menu` WHERE `role_id` = 2;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
-- 页面入口
(2, 100), (2, 101), (2, 102), (2, 103), (2, 104), (2, 105), (2, 107),
-- 分类：只能查看
(2, 120),
-- 文档：查询 + 预览 + 下载
(2, 130), (2, 134), (2, 135),
-- 导入任务：查询 + 详情
(2, 140), (2, 142),
-- AI问答：完整使用权限
(2, 150), (2, 151), (2, 152);

-- ============================================================
-- 五、知识库系统参数配置
-- ============================================================
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `is_allow_change`, `creator_id`, `create_time`, `remark`, `deleted`)
VALUES
('文档最大上传大小', 'knowledge.document.maxUploadSizeMb', '50', 1, 1, NOW(), '单位MB，默认50MB', 0),
('AI问答功能开关',    'knowledge.ai.chatEnabled',       'true',  1, 1, NOW(), 'true=启用 false=停用', 0),
('文档是否需要审核',  'knowledge.document.auditRequired', 'true', 1, 1, NOW(), 'true=上传后需审核 false=直接发布', 0),
('文档默认可见范围',  'knowledge.document.defaultVisibility', '2', 1, 1, NOW(), '1全员可见 2本部门可见 3仅本人可见', 0)
ON DUPLICATE KEY UPDATE
  `config_name` = VALUES(`config_name`),
  `config_value` = VALUES(`config_value`),
  `remark` = VALUES(`remark`),
  `updater_id` = 1,
  `update_time` = NOW();

-- ============================================================
-- 六、同步历史用户的角色与部门
-- ============================================================
UPDATE `sys_user`
SET `role_id` = CASE WHEN `role_id` IS NULL OR `role_id` = 0 THEN 2 ELSE `role_id` END,
    `dept_id` = CASE WHEN `dept_id` IS NULL OR `dept_id` = 0 THEN 2 ELSE `dept_id` END
WHERE `deleted` = 0
  AND ((`role_id` IS NULL OR `role_id` = 0) OR (`dept_id` IS NULL OR `dept_id` = 0));
