USE `docbase_knowledge`;

ALTER TABLE `sys_user`
COMMENT = '用户信息表';

ALTER TABLE `sys_user`
MODIFY COLUMN `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
MODIFY COLUMN `post_id` bigint DEFAULT NULL COMMENT '岗位ID',
MODIFY COLUMN `role_id` bigint DEFAULT NULL COMMENT '角色ID',
MODIFY COLUMN `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
MODIFY COLUMN `username` varchar(64) NOT NULL COMMENT '用户账号',
MODIFY COLUMN `nickname` varchar(32) NOT NULL COMMENT '用户昵称',
MODIFY COLUMN `user_type` smallint DEFAULT '0' COMMENT '用户类型，00 系统用户',
MODIFY COLUMN `email` varchar(128) DEFAULT '' COMMENT '邮箱',
MODIFY COLUMN `phone_number` varchar(18) DEFAULT '' COMMENT '手机号',
MODIFY COLUMN `sex` smallint DEFAULT '0' COMMENT '性别，0 男 1 女 2 未知',
MODIFY COLUMN `avatar` varchar(512) DEFAULT '' COMMENT '头像地址',
MODIFY COLUMN `password` varchar(128) NOT NULL DEFAULT '' COMMENT '密码',
MODIFY COLUMN `status` smallint NOT NULL DEFAULT '0' COMMENT '状态，1 启用 0 停用 2 锁定',
MODIFY COLUMN `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
MODIFY COLUMN `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
MODIFY COLUMN `is_admin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否管理员，1 是 0 否',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `updater_id` bigint DEFAULT NULL COMMENT '更新人ID',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `remark` varchar(512) DEFAULT NULL COMMENT '备注',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除，0 未删除 1 已删除';

ALTER TABLE `sys_role`
COMMENT = '角色信息表';

ALTER TABLE `sys_role`
MODIFY COLUMN `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
MODIFY COLUMN `role_name` varchar(32) NOT NULL COMMENT '角色名称',
MODIFY COLUMN `role_key` varchar(128) NOT NULL COMMENT '角色权限标识',
MODIFY COLUMN `role_sort` int NOT NULL COMMENT '显示顺序',
MODIFY COLUMN `data_scope` smallint DEFAULT '1' COMMENT '数据范围，1 全部 2 本部门 3 本部门及以下 4 自定义 5 仅本人',
MODIFY COLUMN `dept_id_set` varchar(1024) DEFAULT '' COMMENT '自定义数据权限部门集合',
MODIFY COLUMN `status` smallint NOT NULL COMMENT '状态，1 启用 0 停用',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `updater_id` bigint DEFAULT NULL COMMENT '更新人ID',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `remark` varchar(512) DEFAULT NULL COMMENT '备注',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除，0 未删除 1 已删除';

ALTER TABLE `sys_menu`
COMMENT = '菜单权限表';

ALTER TABLE `sys_menu`
MODIFY COLUMN `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
MODIFY COLUMN `menu_name` varchar(64) NOT NULL COMMENT '菜单名称',
MODIFY COLUMN `menu_type` smallint NOT NULL DEFAULT '0' COMMENT '菜单类型，1 菜单 2 目录 3 按钮 4 内嵌页',
MODIFY COLUMN `router_name` varchar(255) NOT NULL DEFAULT '' COMMENT '路由名称',
MODIFY COLUMN `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父菜单ID',
MODIFY COLUMN `path` varchar(255) DEFAULT NULL COMMENT '路由路径',
MODIFY COLUMN `is_button` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否按钮',
MODIFY COLUMN `permission` varchar(128) DEFAULT NULL COMMENT '权限标识',
MODIFY COLUMN `meta_info` varchar(1024) NOT NULL DEFAULT '{}' COMMENT '前端路由元信息',
MODIFY COLUMN `status` smallint NOT NULL DEFAULT '0' COMMENT '状态，1 启用 0 停用',
MODIFY COLUMN `remark` varchar(256) DEFAULT '' COMMENT '备注',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `updater_id` bigint DEFAULT NULL COMMENT '更新人ID',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `sys_notice`
COMMENT = '通知公告表';

ALTER TABLE `sys_notice`
MODIFY COLUMN `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
MODIFY COLUMN `notice_title` varchar(64) NOT NULL COMMENT '公告标题',
MODIFY COLUMN `notice_type` smallint NOT NULL COMMENT '公告类型，1 公告 2 通知',
MODIFY COLUMN `notice_content` text COMMENT '公告内容',
MODIFY COLUMN `status` smallint NOT NULL DEFAULT '0' COMMENT '状态，1 正常 0 关闭',
MODIFY COLUMN `creator_id` bigint NOT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `updater_id` bigint DEFAULT NULL COMMENT '更新人ID',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `sys_config`
COMMENT = '参数配置表';

ALTER TABLE `sys_config`
MODIFY COLUMN `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数ID',
MODIFY COLUMN `config_name` varchar(128) NOT NULL DEFAULT '' COMMENT '参数名称',
MODIFY COLUMN `config_key` varchar(128) NOT NULL DEFAULT '' COMMENT '参数键名',
MODIFY COLUMN `config_options` varchar(1024) NOT NULL DEFAULT '' COMMENT '可选项配置',
MODIFY COLUMN `config_value` varchar(256) NOT NULL DEFAULT '' COMMENT '参数键值',
MODIFY COLUMN `is_allow_change` tinyint(1) NOT NULL COMMENT '是否允许修改',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `updater_id` bigint DEFAULT NULL COMMENT '更新人ID',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `remark` varchar(128) DEFAULT NULL COMMENT '备注',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `sys_post`
COMMENT = '岗位信息表';

ALTER TABLE `sys_post`
MODIFY COLUMN `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
MODIFY COLUMN `post_code` varchar(64) NOT NULL COMMENT '岗位编码',
MODIFY COLUMN `post_name` varchar(64) NOT NULL COMMENT '岗位名称',
MODIFY COLUMN `post_sort` int NOT NULL COMMENT '显示顺序',
MODIFY COLUMN `status` smallint NOT NULL COMMENT '状态，1 启用 0 停用',
MODIFY COLUMN `remark` varchar(512) DEFAULT NULL COMMENT '备注',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `updater_id` bigint DEFAULT NULL COMMENT '更新人ID',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `sys_dept`
COMMENT = '部门信息表';

ALTER TABLE `sys_dept`
MODIFY COLUMN `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
MODIFY COLUMN `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父部门ID',
MODIFY COLUMN `ancestors` text NOT NULL COMMENT '祖级列表',
MODIFY COLUMN `dept_name` varchar(64) NOT NULL DEFAULT '' COMMENT '部门名称',
MODIFY COLUMN `order_num` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
MODIFY COLUMN `leader_name` varchar(64) DEFAULT NULL COMMENT '负责人',
MODIFY COLUMN `phone` varchar(16) DEFAULT NULL COMMENT '联系电话',
MODIFY COLUMN `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
MODIFY COLUMN `status` smallint NOT NULL DEFAULT '0' COMMENT '状态，0 正常 1 停用',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `updater_id` bigint DEFAULT NULL COMMENT '更新人ID',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `sys_operation_log`
COMMENT = '操作日志表';

ALTER TABLE `sys_operation_log`
MODIFY COLUMN `operation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
MODIFY COLUMN `business_type` smallint NOT NULL DEFAULT '0' COMMENT '业务类型，0 其它 1 新增 2 修改 3 删除',
MODIFY COLUMN `request_method` smallint NOT NULL DEFAULT '0' COMMENT '请求方式',
MODIFY COLUMN `request_module` varchar(64) NOT NULL DEFAULT '' COMMENT '业务模块',
MODIFY COLUMN `request_url` varchar(256) NOT NULL DEFAULT '' COMMENT '请求URL',
MODIFY COLUMN `called_method` varchar(128) NOT NULL DEFAULT '' COMMENT '调用方法',
MODIFY COLUMN `operator_type` smallint NOT NULL DEFAULT '0' COMMENT '操作类别，0 其它 1 后台用户 2 手机端用户',
MODIFY COLUMN `user_id` bigint DEFAULT '0' COMMENT '操作用户ID',
MODIFY COLUMN `username` varchar(32) DEFAULT '' COMMENT '操作用户',
MODIFY COLUMN `operator_ip` varchar(128) DEFAULT '' COMMENT '操作IP',
MODIFY COLUMN `operator_location` varchar(256) DEFAULT '' COMMENT '操作地点',
MODIFY COLUMN `dept_id` bigint DEFAULT '0' COMMENT '部门ID',
MODIFY COLUMN `dept_name` varchar(64) DEFAULT NULL COMMENT '部门名称',
MODIFY COLUMN `operation_param` varchar(2048) DEFAULT '' COMMENT '请求参数',
MODIFY COLUMN `operation_result` varchar(2048) DEFAULT '' COMMENT '返回结果',
MODIFY COLUMN `status` smallint NOT NULL DEFAULT '1' COMMENT '操作状态，1 成功 0 失败',
MODIFY COLUMN `error_stack` varchar(2048) DEFAULT '' COMMENT '异常信息',
MODIFY COLUMN `operation_time` datetime NOT NULL COMMENT '操作时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `sys_login_info`
COMMENT = '登录日志表';

ALTER TABLE `sys_login_info`
MODIFY COLUMN `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
MODIFY COLUMN `username` varchar(50) NOT NULL DEFAULT '' COMMENT '用户账号',
MODIFY COLUMN `ip_address` varchar(128) NOT NULL DEFAULT '' COMMENT '登录IP地址',
MODIFY COLUMN `login_location` varchar(255) NOT NULL DEFAULT '' COMMENT '登录地点',
MODIFY COLUMN `browser` varchar(50) NOT NULL DEFAULT '' COMMENT '浏览器类型',
MODIFY COLUMN `operation_system` varchar(50) NOT NULL DEFAULT '' COMMENT '操作系统',
MODIFY COLUMN `status` smallint NOT NULL DEFAULT '0' COMMENT '登录状态，1 成功 0 失败',
MODIFY COLUMN `msg` varchar(255) NOT NULL DEFAULT '' COMMENT '提示消息',
MODIFY COLUMN `login_time` datetime DEFAULT NULL COMMENT '登录时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';
