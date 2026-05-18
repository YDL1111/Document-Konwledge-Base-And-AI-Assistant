USE `docbase_knowledge`;

-- 移除知识库管理下不再需要展示的动态菜单
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (101, 107);

UPDATE `sys_menu`
SET `status` = 0,
    `deleted` = 1
WHERE `menu_id` IN (101, 107);

-- 将 AI 问答、AI 审计都提升为一级菜单，避免跨目录导致的导航错乱
UPDATE `sys_menu`
SET `parent_id` = 0,
    `status` = 1,
    `deleted` = 0,
    `meta_info` = '{"title":"AI问答","icon":"ep:chat-dot-round","showParent":true,"rank":5}'
WHERE `menu_id` = 105;

UPDATE `sys_menu`
SET `parent_id` = 0,
    `status` = 1,
    `deleted` = 0,
    `meta_info` = '{"title":"AI审计","icon":"ep:histogram","showParent":true,"rank":6}'
WHERE `menu_id` = 106;

-- 补齐新增文档按钮权限
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES
(138, '新增文档', 2, '', 103, '', 1, 'knowledge:document:add', '{}', 1, '文档新增按钮权限', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
`menu_name` = VALUES(`menu_name`),
`menu_type` = VALUES(`menu_type`),
`router_name` = VALUES(`router_name`),
`parent_id` = VALUES(`parent_id`),
`path` = VALUES(`path`),
`is_button` = VALUES(`is_button`),
`permission` = VALUES(`permission`),
`meta_info` = VALUES(`meta_info`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`updater_id` = VALUES(`updater_id`),
`update_time` = VALUES(`update_time`),
`deleted` = VALUES(`deleted`);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 138),
(2, 138);
