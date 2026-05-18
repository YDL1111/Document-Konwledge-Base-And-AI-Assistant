USE `docbase_knowledge`;

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (106, 108);

UPDATE `sys_menu`
SET `status` = 0,
    `deleted` = 1
WHERE `menu_id` = 108;

UPDATE `sys_menu`
SET `menu_name` = 'AI审计',
    `menu_type` = 2,
    `router_name` = '',
    `parent_id` = 0,
    `path` = '/ai/audit',
    `is_button` = 0,
    `permission` = '',
    `meta_info` = '{"title":"AI审计","icon":"ep:histogram","showParent":true,"rank":6}',
    `status` = 1,
    `remark` = 'AI审计一级菜单',
    `updater_id` = 1,
    `update_time` = NOW(),
    `deleted` = 0
WHERE `menu_id` = 106;

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES
(109, '审计日志', 1, 'AiAudit', 106, '/ai/audit/index', 0, 'ai:audit:list', '{"title":"AI审计","icon":"ep:histogram","showParent":true}', 1, 'AI审计页面', 1, NOW(), 1, NOW(), 0)
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

DELETE FROM `sys_role_menu` WHERE `menu_id` = 109;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 106),
(1, 109);
