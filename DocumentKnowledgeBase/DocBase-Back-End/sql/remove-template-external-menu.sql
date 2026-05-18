-- 清理模板遗留的外链菜单
-- 执行后请退出重新登录系统，让菜单重新拉取

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (4, 63);
DELETE FROM `sys_menu` WHERE `menu_id` IN (4, 63);
