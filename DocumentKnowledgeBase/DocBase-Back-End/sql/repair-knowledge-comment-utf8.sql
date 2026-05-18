USE `docbase_knowledge`;

ALTER TABLE `ai_audit_log`
COMMENT = 'AI问答审计日志表';

ALTER TABLE `ai_audit_log`
MODIFY COLUMN `audit_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审计ID',
MODIFY COLUMN `session_id` bigint DEFAULT NULL COMMENT '会话ID',
MODIFY COLUMN `message_id` bigint DEFAULT NULL COMMENT '消息ID',
MODIFY COLUMN `user_id` bigint DEFAULT NULL COMMENT '用户ID',
MODIFY COLUMN `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
MODIFY COLUMN `question_text` varchar(2000) DEFAULT NULL COMMENT '问题文本',
MODIFY COLUMN `retrieval_count` int DEFAULT NULL COMMENT '检索文档数量',
MODIFY COLUMN `top_score` decimal(6,4) DEFAULT NULL COMMENT '最高匹配分数',
MODIFY COLUMN `model_name` varchar(128) DEFAULT NULL COMMENT '模型名称',
MODIFY COLUMN `prompt_tokens` int DEFAULT NULL COMMENT '输入Token数',
MODIFY COLUMN `completion_tokens` int DEFAULT NULL COMMENT '输出Token数',
MODIFY COLUMN `total_tokens` int DEFAULT NULL COMMENT '总Token数',
MODIFY COLUMN `latency_ms` int DEFAULT NULL COMMENT '耗时毫秒数',
MODIFY COLUMN `result_status` smallint NOT NULL DEFAULT '1' COMMENT '结果状态，1 成功 0 失败 2 拒答',
MODIFY COLUMN `error_message` varchar(2000) DEFAULT NULL COMMENT '错误信息',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `ai_chat_message`
COMMENT = 'AI问答消息表';

ALTER TABLE `ai_chat_message`
MODIFY COLUMN `message_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
MODIFY COLUMN `session_id` bigint NOT NULL COMMENT '会话ID',
MODIFY COLUMN `message_role` smallint NOT NULL COMMENT '消息角色，1 用户 2 助手',
MODIFY COLUMN `message_content` longtext NOT NULL COMMENT '消息内容',
MODIFY COLUMN `source_snapshot` json DEFAULT NULL COMMENT '引用来源快照',
MODIFY COLUMN `model_name` varchar(128) DEFAULT NULL COMMENT '模型名称',
MODIFY COLUMN `confidence_level` varchar(32) DEFAULT NULL COMMENT '置信等级',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `ai_chat_session`
COMMENT = 'AI问答会话表';

ALTER TABLE `ai_chat_session`
MODIFY COLUMN `session_id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
MODIFY COLUMN `session_title` varchar(255) NOT NULL DEFAULT '' COMMENT '会话标题',
MODIFY COLUMN `user_id` bigint NOT NULL COMMENT '用户ID',
MODIFY COLUMN `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
MODIFY COLUMN `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
MODIFY COLUMN `status` smallint NOT NULL DEFAULT '1' COMMENT '会话状态，1 正常 0 关闭',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `knowledge_category`
COMMENT = '知识分类表';

ALTER TABLE `knowledge_category`
MODIFY COLUMN `category_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
MODIFY COLUMN `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类ID',
MODIFY COLUMN `ancestors` varchar(1024) NOT NULL DEFAULT '' COMMENT '祖级路径',
MODIFY COLUMN `category_name` varchar(128) NOT NULL DEFAULT '' COMMENT '分类名称',
MODIFY COLUMN `dept_id` bigint DEFAULT NULL COMMENT '归属部门ID',
MODIFY COLUMN `sort_num` int NOT NULL DEFAULT '0' COMMENT '排序值',
MODIFY COLUMN `status` smallint NOT NULL DEFAULT '1' COMMENT '状态，1 启用 0 停用',
MODIFY COLUMN `remark` varchar(255) DEFAULT NULL COMMENT '备注',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `updater_id` bigint DEFAULT NULL COMMENT '更新人ID',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `knowledge_document`
COMMENT = '知识文档主表';

ALTER TABLE `knowledge_document`
MODIFY COLUMN `document_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档ID',
MODIFY COLUMN `category_id` bigint DEFAULT NULL COMMENT '分类ID',
MODIFY COLUMN `dept_id` bigint DEFAULT NULL COMMENT '归属部门ID',
MODIFY COLUMN `title` varchar(256) NOT NULL DEFAULT '' COMMENT '文档标题',
MODIFY COLUMN `doc_code` varchar(64) DEFAULT NULL COMMENT '文档编号',
MODIFY COLUMN `summary` varchar(1000) DEFAULT NULL COMMENT '文档摘要',
MODIFY COLUMN `tags` varchar(1000) DEFAULT NULL COMMENT '标签列表',
MODIFY COLUMN `visibility` smallint NOT NULL DEFAULT '2' COMMENT '可见范围，1 私有 2 部门 3 全员 4 指定权限',
MODIFY COLUMN `status` smallint NOT NULL DEFAULT '1' COMMENT '文档状态，1 草稿 2 审核中 3 已发布 4 已下线 5 已归档 6 已删除',
MODIFY COLUMN `current_version_id` bigint DEFAULT NULL COMMENT '当前版本ID',
MODIFY COLUMN `current_version_no` varchar(32) DEFAULT NULL COMMENT '当前版本号',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `updater_id` bigint DEFAULT NULL COMMENT '更新人ID',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `knowledge_document_acl`
COMMENT = '文档访问控制表';

ALTER TABLE `knowledge_document_acl`
MODIFY COLUMN `acl_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
MODIFY COLUMN `document_id` bigint NOT NULL COMMENT '文档ID',
MODIFY COLUMN `subject_type` smallint NOT NULL COMMENT '主体类型，1 用户 2 角色 3 部门',
MODIFY COLUMN `subject_id` bigint NOT NULL COMMENT '主体对象ID',
MODIFY COLUMN `permission_type` smallint NOT NULL DEFAULT '1' COMMENT '权限类型，1 查看 2 编辑 3 管理',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `knowledge_document_version`
COMMENT = '文档版本表';

ALTER TABLE `knowledge_document_version`
MODIFY COLUMN `version_id` bigint NOT NULL AUTO_INCREMENT COMMENT '版本ID',
MODIFY COLUMN `document_id` bigint NOT NULL COMMENT '文档ID',
MODIFY COLUMN `version_no` varchar(32) NOT NULL DEFAULT '' COMMENT '版本号',
MODIFY COLUMN `file_name` varchar(255) NOT NULL DEFAULT '' COMMENT '文件名称',
MODIFY COLUMN `file_ext` varchar(32) DEFAULT NULL COMMENT '文件扩展名',
MODIFY COLUMN `file_size` bigint DEFAULT NULL COMMENT '文件大小字节数',
MODIFY COLUMN `storage_type` varchar(32) NOT NULL DEFAULT 'minio' COMMENT '存储类型',
MODIFY COLUMN `storage_path` varchar(512) NOT NULL DEFAULT '' COMMENT '存储路径',
MODIFY COLUMN `storage_url` varchar(1024) DEFAULT NULL COMMENT '访问地址',
MODIFY COLUMN `content_hash` varchar(128) DEFAULT NULL COMMENT '内容哈希值',
MODIFY COLUMN `version_remark` varchar(500) DEFAULT NULL COMMENT '版本说明',
MODIFY COLUMN `parse_status` smallint NOT NULL DEFAULT '1' COMMENT '解析状态，1 待处理 2 处理中 3 成功 4 失败',
MODIFY COLUMN `is_current` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否当前版本',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `knowledge_ingest_task`
COMMENT = '知识入库任务表';

ALTER TABLE `knowledge_ingest_task`
MODIFY COLUMN `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
MODIFY COLUMN `task_no` varchar(64) NOT NULL DEFAULT '' COMMENT '任务编号',
MODIFY COLUMN `document_id` bigint NOT NULL COMMENT '文档ID',
MODIFY COLUMN `version_id` bigint NOT NULL COMMENT '版本ID',
MODIFY COLUMN `task_type` smallint NOT NULL DEFAULT '1' COMMENT '任务类型，1 全量导入 2 重新解析 3 重试 4 手动触发',
MODIFY COLUMN `status` smallint NOT NULL DEFAULT '1' COMMENT '任务状态，1 待处理 2 处理中 3 成功 4 失败',
MODIFY COLUMN `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
MODIFY COLUMN `chunk_count` int DEFAULT NULL COMMENT '切块数量',
MODIFY COLUMN `error_message` varchar(2000) DEFAULT NULL COMMENT '错误信息',
MODIFY COLUMN `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
MODIFY COLUMN `started_time` datetime DEFAULT NULL COMMENT '开始时间',
MODIFY COLUMN `finished_time` datetime DEFAULT NULL COMMENT '结束时间',
MODIFY COLUMN `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
MODIFY COLUMN `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除';

ALTER TABLE `sys_role_menu`
COMMENT = '角色菜单关联表';

ALTER TABLE `sys_role_menu`
MODIFY COLUMN `role_id` bigint NOT NULL COMMENT '角色ID',
MODIFY COLUMN `menu_id` bigint NOT NULL COMMENT '菜单ID';
