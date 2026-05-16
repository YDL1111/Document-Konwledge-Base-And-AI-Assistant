USE `docbase_knowledge`;

UPDATE `sys_role` SET `remark` = '系统内置超级管理员角色' WHERE `role_id` = 1;
UPDATE `sys_role` SET `remark` = '标准业务角色示例' WHERE `role_id` = 2;
UPDATE `sys_role` SET `remark` = '已停用角色示例' WHERE `role_id` = 3;

UPDATE `sys_config` SET `remark` = '可选值 skin-blue、skin-green、skin-purple、skin-red、skin-yellow' WHERE `config_id` = 1;
UPDATE `sys_config` SET `remark` = '初始化密码默认值：123456' WHERE `config_id` = 2;
UPDATE `sys_config` SET `remark` = '可选值 theme-dark、theme-light' WHERE `config_id` = 3;
UPDATE `sys_config` SET `remark` = '是否开启登录验证码：true 表示开启，false 表示关闭' WHERE `config_id` = 4;
UPDATE `sys_config` SET `remark` = '是否允许用户注册：true 表示允许，false 表示关闭' WHERE `config_id` = 5;

UPDATE `sys_notice`
SET
  `notice_content` = 'DocBase 企业文档知识库系统已完成首轮改造上线，当前已具备基础权限、菜单、公告和知识库菜单接入能力。',
  `remark` = '系统上线通知'
WHERE `notice_id` = 1;

UPDATE `sys_notice`
SET
  `notice_content` = '欢迎使用 DocBase 企业文档知识库系统，后续将逐步接入企业文档管理、知识入库与智能问答能力。',
  `remark` = '欢迎公告'
WHERE `notice_id` = 2;

UPDATE `sys_operation_log`
SET `request_module` = '认证登录'
WHERE `request_module` LIKE '%?%' OR `request_module` = '????';

UPDATE `sys_operation_log`
SET `operator_location` = '内网IP'
WHERE `operator_location` LIKE '%?%' OR `operator_location` = '??IP';

UPDATE `sys_login_info`
SET `login_location` = '内网IP'
WHERE `login_location` LIKE '%?%' OR `login_location` = '??IP';

UPDATE `sys_login_info`
SET `msg` = '登录成功'
WHERE `msg` = '?????';

UPDATE `sys_login_info`
SET `msg` = '退出成功'
WHERE `msg` = '????';

UPDATE `sys_login_info`
SET `msg` = '验证码错误'
WHERE `msg` = '????';
