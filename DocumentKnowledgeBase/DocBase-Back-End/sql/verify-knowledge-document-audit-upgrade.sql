USE `docbase_knowledge`;

SHOW COLUMNS FROM `knowledge_document` LIKE 'audit_remark';

SELECT menu_id, menu_name, parent_id, permission, status, deleted
FROM `sys_menu`
WHERE `menu_id` = 138;

SELECT role_id, menu_id
FROM `sys_role_menu`
WHERE `menu_id` = 138
ORDER BY role_id;
