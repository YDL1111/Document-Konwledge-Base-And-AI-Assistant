USE `docbase_knowledge`;

-- 将 role_id = 2 调整为知识库普通用户角色
UPDATE `sys_role`
SET
  `role_key` = 'knowledge_user',
  `role_sort` = 20,
  `status` = 1
WHERE `role_id` = 2;

-- 补齐普通用户个人中心菜单
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES
(107, '个人中心', 1, 'UserProfile', 100, '/system/user/profile', 0, 'system:user:profile', '{"title":"个人中心","icon":"ep:user-filled","showParent":true}', 1, '普通用户个人中心入口', 1, NOW(), 1, NOW(), 0)
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

-- 重新初始化普通用户角色菜单权限
DELETE FROM `sys_role_menu` WHERE `role_id` = 2;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES
(2, 100),
(2, 102),
(2, 103),
(2, 104),
(2, 105),
(2, 107),
(2, 120),
(2, 130),
(2, 131),
(2, 134),
(2, 135),
(2, 140),
(2, 142),
(2, 150),
(2, 151),
(2, 152);

-- 给缺失角色或部门的新用户兜底补默认值
UPDATE `sys_user`
SET
  `role_id` = CASE WHEN `role_id` IS NULL OR `role_id` = 0 THEN 2 ELSE `role_id` END,
  `dept_id` = CASE WHEN `dept_id` IS NULL OR `dept_id` = 0 THEN 2 ELSE `dept_id` END
WHERE `deleted` = 0
  AND ((`role_id` IS NULL OR `role_id` = 0) OR (`dept_id` IS NULL OR `dept_id` = 0));
