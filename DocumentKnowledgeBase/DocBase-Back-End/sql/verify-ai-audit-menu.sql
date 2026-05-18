USE `docbase_knowledge`;

SELECT menu_id, menu_name, parent_id, menu_type, path, router_name, status, deleted
FROM sys_menu
WHERE menu_id IN (106, 108, 109)
ORDER BY menu_id;

SELECT role_id, menu_id
FROM sys_role_menu
WHERE menu_id IN (106, 108, 109)
ORDER BY role_id, menu_id;
