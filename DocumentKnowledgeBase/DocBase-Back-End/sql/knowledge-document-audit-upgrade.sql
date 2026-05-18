USE `docbase_knowledge`;

ALTER TABLE `knowledge_document`
    ADD COLUMN `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注' AFTER `current_version_no`;

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES
(138, '文档审核', 3, '', 103, '', 1, 'knowledge:document:audit', '{}', 1, '文档审核按钮', 1, NOW(), 1, NOW(), 0)
ON DUPLICATE KEY UPDATE
`menu_name` = VALUES(`menu_name`),
`menu_type` = VALUES(`menu_type`),
`parent_id` = VALUES(`parent_id`),
`is_button` = VALUES(`is_button`),
`permission` = VALUES(`permission`),
`meta_info` = VALUES(`meta_info`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`updater_id` = VALUES(`updater_id`),
`update_time` = VALUES(`update_time`),
`deleted` = VALUES(`deleted`);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 138);
