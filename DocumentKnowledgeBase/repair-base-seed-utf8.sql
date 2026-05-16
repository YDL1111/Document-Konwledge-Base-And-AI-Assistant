USE `docbase_knowledge`;

-- 修复系统基础字典数据
UPDATE `sys_role` SET `role_name` = '超级管理员' WHERE `role_id` = 1;
UPDATE `sys_role` SET `role_name` = '普通角色' WHERE `role_id` = 2;
UPDATE `sys_role` SET `role_name` = '停用角色' WHERE `role_id` = 3;

UPDATE `sys_dept` SET `dept_name` = 'DocBase集团' WHERE `dept_id` = 1;
UPDATE `sys_dept` SET `dept_name` = '研发中心' WHERE `dept_id` = 2;
UPDATE `sys_dept` SET `dept_name` = '运营中心' WHERE `dept_id` = 3;
UPDATE `sys_dept` SET `dept_name` = '后端部门' WHERE `dept_id` = 4;
UPDATE `sys_dept` SET `dept_name` = '前端部门' WHERE `dept_id` = 5;
UPDATE `sys_dept` SET `dept_name` = '测试部门' WHERE `dept_id` = 6;
UPDATE `sys_dept` SET `dept_name` = '产品部门' WHERE `dept_id` = 7;
UPDATE `sys_dept` SET `dept_name` = '运维部门' WHERE `dept_id` = 8;
UPDATE `sys_dept` SET `dept_name` = '市场部门' WHERE `dept_id` = 9;
UPDATE `sys_dept` SET `dept_name` = '设计部门' WHERE `dept_id` = 10;

UPDATE `sys_config` SET `config_name` = '系统界面-主题色配置' WHERE `config_id` = 1;
UPDATE `sys_config` SET `config_name` = '用户管理-初始密码' WHERE `config_id` = 2;
UPDATE `sys_config` SET `config_name` = '系统界面-侧边栏主题' WHERE `config_id` = 3;
UPDATE `sys_config` SET `config_name` = '账号安全-验证码开关' WHERE `config_id` = 4;
UPDATE `sys_config` SET `config_name` = '账号安全-是否允许注册' WHERE `config_id` = 5;

UPDATE `sys_post` SET `post_name` = '首席执行官' WHERE `post_id` = 1;
UPDATE `sys_post` SET `post_name` = '软件工程师' WHERE `post_id` = 2;
UPDATE `sys_post` SET `post_name` = '人力资源' WHERE `post_id` = 3;
UPDATE `sys_post` SET `post_name` = '普通员工' WHERE `post_id` = 4;

UPDATE `sys_notice`
SET
  `notice_title` = '通知：2026-05-01 DocBase 企业文档知识库系统上线',
  `notice_type` = 2
WHERE `notice_id` = 1;

UPDATE `sys_notice`
SET
  `notice_title` = '公告：欢迎使用 DocBase 企业文档知识库系统',
  `notice_type` = 1
WHERE `notice_id` = 2;
