USE `docbase_knowledge`;

-- 修复系统管理与知识库菜单名称
UPDATE `sys_menu`
SET
  `menu_name` = '系统管理',
  `meta_info` = '{"title":"系统管理","icon":"ep:management","showParent":true,"rank":1}'
WHERE `menu_id` = 1;

UPDATE `sys_menu`
SET
  `menu_name` = '系统监控',
  `meta_info` = '{"title":"系统监控","icon":"ep:monitor","showParent":true,"rank":3}'
WHERE `menu_id` = 2;

UPDATE `sys_menu`
SET
  `menu_name` = '系统工具',
  `meta_info` = '{"title":"系统工具","icon":"ep:tools","showParent":true,"rank":2}'
WHERE `menu_id` = 3;

UPDATE `sys_menu` SET `menu_name` = '用户管理', `meta_info` = '{"title":"用户管理","icon":"ep:user-filled","showParent":true}' WHERE `menu_id` = 5;
UPDATE `sys_menu` SET `menu_name` = '角色管理', `meta_info` = '{"title":"角色管理","icon":"ep:user","showParent":true}' WHERE `menu_id` = 6;
UPDATE `sys_menu` SET `menu_name` = '菜单管理', `meta_info` = '{"title":"菜单管理","icon":"ep:menu","showParent":true}' WHERE `menu_id` = 7;
UPDATE `sys_menu` SET `menu_name` = '部门管理', `meta_info` = '{"title":"部门管理","icon":"fa-solid:code-branch","showParent":true}' WHERE `menu_id` = 8;
UPDATE `sys_menu` SET `menu_name` = '岗位管理', `meta_info` = '{"title":"岗位管理","icon":"ep:postcard","showParent":true}' WHERE `menu_id` = 9;
UPDATE `sys_menu` SET `menu_name` = '参数配置', `meta_info` = '{"title":"参数配置","icon":"ep:setting","showParent":true}' WHERE `menu_id` = 10;
UPDATE `sys_menu` SET `menu_name` = '通知公告', `meta_info` = '{"title":"通知公告","icon":"ep:notification","showParent":true}' WHERE `menu_id` = 11;
UPDATE `sys_menu` SET `menu_name` = '日志管理', `meta_info` = '{"title":"日志管理","icon":"ep:document","showParent":true}' WHERE `menu_id` = 12;
UPDATE `sys_menu` SET `menu_name` = '在线用户', `meta_info` = '{"title":"在线用户","icon":"fa-solid:users","showParent":true}' WHERE `menu_id` = 13;
UPDATE `sys_menu` SET `menu_name` = '数据监控', `meta_info` = '{"title":"数据监控","icon":"fa:database","showParent":true,"frameSrc":"/druid/login.html","isFrameSrcInternal":true}' WHERE `menu_id` = 14;
UPDATE `sys_menu` SET `menu_name` = '服务监控', `meta_info` = '{"title":"服务监控","icon":"fa:server","showParent":true}' WHERE `menu_id` = 15;
UPDATE `sys_menu` SET `menu_name` = '缓存监控', `meta_info` = '{"title":"缓存监控","icon":"ep:reading","showParent":true}' WHERE `menu_id` = 16;
UPDATE `sys_menu` SET `menu_name` = '接口文档', `meta_info` = '{"title":"接口文档","icon":"ep:document-remove","showParent":true,"frameSrc":"/swagger-ui/index.html","isFrameSrcInternal":true}' WHERE `menu_id` = 17;
UPDATE `sys_menu` SET `menu_name` = '操作日志', `meta_info` = '{"title":"操作日志"}' WHERE `menu_id` = 18;
UPDATE `sys_menu` SET `menu_name` = '登录日志', `meta_info` = '{"title":"登录日志"}' WHERE `menu_id` = 19;

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES
(100, '知识库管理', 2, '', 0, '/knowledge', 0, '', '{"title":"知识库管理","icon":"ep:collection","showParent":true,"rank":4}', 1, '知识库管理目录', 1, NOW(), 1, NOW(), 0),
(101, '知识库工作台', 1, 'KnowledgeWorkspace', 100, '/knowledge/workspace/index', 0, 'knowledge:workspace:view', '{"title":"知识库工作台","icon":"ep:home-filled","showParent":true}', 1, '知识库工作台页面', 1, NOW(), 1, NOW(), 0),
(102, '分类管理', 1, 'KnowledgeCategory', 100, '/knowledge/category/index', 0, 'knowledge:category:list', '{"title":"分类管理","icon":"ep:folder","showParent":true}', 1, '知识分类管理页面', 1, NOW(), 1, NOW(), 0),
(103, '文档管理', 1, 'KnowledgeDocument', 100, '/knowledge/document/index', 0, 'knowledge:document:list', '{"title":"文档管理","icon":"ep:document","showParent":true}', 1, '知识文档管理页面', 1, NOW(), 1, NOW(), 0),
(104, '导入任务', 1, 'KnowledgeIngestTask', 100, '/knowledge/ingest/index', 0, 'knowledge:ingest:list', '{"title":"导入任务","icon":"ep:operation","showParent":true}', 1, '知识导入任务页面', 1, NOW(), 1, NOW(), 0),
(105, 'AI问答', 1, 'AiChat', 100, '/ai/chat/index', 0, 'ai:chat:list', '{"title":"AI问答","icon":"ep:chat-dot-round","showParent":true}', 1, 'AI问答页面', 1, NOW(), 1, NOW(), 0),
(106, 'AI审计', 1, 'AiAudit', 100, '/ai/audit/index', 0, 'ai:audit:list', '{"title":"AI审计","icon":"ep:histogram","showParent":true}', 1, 'AI审计页面', 1, NOW(), 1, NOW(), 0)
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

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES
(120, '分类查询', 3, '', 102, '', 1, 'knowledge:category:query', '{}', 1, '分类查询按钮', 1, NOW(), 1, NOW(), 0),
(121, '分类新增', 3, '', 102, '', 1, 'knowledge:category:add', '{}', 1, '分类新增按钮', 1, NOW(), 1, NOW(), 0),
(122, '分类编辑', 3, '', 102, '', 1, 'knowledge:category:edit', '{}', 1, '分类编辑按钮', 1, NOW(), 1, NOW(), 0),
(123, '分类删除', 3, '', 102, '', 1, 'knowledge:category:remove', '{}', 1, '分类删除按钮', 1, NOW(), 1, NOW(), 0),
(130, '文档查询', 3, '', 103, '', 1, 'knowledge:document:query', '{}', 1, '文档查询按钮', 1, NOW(), 1, NOW(), 0),
(131, '文档上传', 3, '', 103, '', 1, 'knowledge:document:upload', '{}', 1, '文档上传按钮', 1, NOW(), 1, NOW(), 0),
(132, '文档编辑', 3, '', 103, '', 1, 'knowledge:document:edit', '{}', 1, '文档编辑按钮', 1, NOW(), 1, NOW(), 0),
(133, '文档删除', 3, '', 103, '', 1, 'knowledge:document:remove', '{}', 1, '文档删除按钮', 1, NOW(), 1, NOW(), 0),
(134, '文档预览', 3, '', 103, '', 1, 'knowledge:document:preview', '{}', 1, '文档预览按钮', 1, NOW(), 1, NOW(), 0),
(135, '文档下载', 3, '', 103, '', 1, 'knowledge:document:download', '{}', 1, '文档下载按钮', 1, NOW(), 1, NOW(), 0),
(136, '提交导入', 3, '', 103, '', 1, 'knowledge:document:ingest', '{}', 1, '文档提交导入按钮', 1, NOW(), 1, NOW(), 0),
(137, '版本回滚', 3, '', 103, '', 1, 'knowledge:document:rollback', '{}', 1, '文档版本回滚按钮', 1, NOW(), 1, NOW(), 0),
(140, '任务查询', 3, '', 104, '', 1, 'knowledge:ingest:query', '{}', 1, '导入任务查询按钮', 1, NOW(), 1, NOW(), 0),
(141, '任务重试', 3, '', 104, '', 1, 'knowledge:ingest:retry', '{}', 1, '导入任务重试按钮', 1, NOW(), 1, NOW(), 0),
(142, '任务详情', 3, '', 104, '', 1, 'knowledge:ingest:detail', '{}', 1, '导入任务详情按钮', 1, NOW(), 1, NOW(), 0),
(150, '会话查询', 3, '', 105, '', 1, 'ai:chat:query', '{}', 1, 'AI问答会话查询按钮', 1, NOW(), 1, NOW(), 0),
(151, '发送消息', 3, '', 105, '', 1, 'ai:chat:send', '{}', 1, 'AI问答发送消息按钮', 1, NOW(), 1, NOW(), 0),
(152, '删除会话', 3, '', 105, '', 1, 'ai:chat:remove', '{}', 1, 'AI问答删除会话按钮', 1, NOW(), 1, NOW(), 0),
(160, '审计查询', 3, '', 106, '', 1, 'ai:audit:query', '{}', 1, 'AI审计查询按钮', 1, NOW(), 1, NOW(), 0),
(161, '审计导出', 3, '', 106, '', 1, 'ai:audit:export', '{}', 1, 'AI审计导出按钮', 1, NOW(), 1, NOW(), 0)
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

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES
(1, 100), (1, 101), (1, 102), (1, 103), (1, 104), (1, 105), (1, 106),
(1, 120), (1, 121), (1, 122), (1, 123),
(1, 130), (1, 131), (1, 132), (1, 133), (1, 134), (1, 135), (1, 136), (1, 137),
(1, 140), (1, 141), (1, 142),
(1, 150), (1, 151), (1, 152),
(1, 160), (1, 161);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES
(2, 100), (2, 101), (2, 103), (2, 105),
(2, 130), (2, 134), (2, 135),
(2, 150), (2, 151), (2, 152);
