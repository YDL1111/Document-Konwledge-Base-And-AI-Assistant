USE `docbase_knowledge`;

DELETE FROM `sys_role_menu`
WHERE `role_id` = 2
  AND `menu_id` = 138;
