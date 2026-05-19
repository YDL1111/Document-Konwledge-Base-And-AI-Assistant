USE `docbase_knowledge`;

CREATE TABLE `knowledge_category`
(
    `category_id`   bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id`     bigint NOT NULL DEFAULT '0' COMMENT '父分类ID',
    `ancestors`     varchar(1024) NOT NULL DEFAULT '' COMMENT '祖级列表',
    `category_name` varchar(128) NOT NULL DEFAULT '' COMMENT '分类名称',
    `dept_id`       bigint DEFAULT NULL COMMENT '所属部门ID',
    `sort_num`      int NOT NULL DEFAULT '0' COMMENT '同级排序值',
    `status`        smallint NOT NULL DEFAULT '1' COMMENT '分类状态 1启用 0停用',
    `remark`        varchar(255) DEFAULT NULL COMMENT '备注',
    `creator_id`    bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time`   datetime DEFAULT NULL COMMENT '创建时间',
    `updater_id`    bigint DEFAULT NULL COMMENT '更新人ID',
    `update_time`   datetime DEFAULT NULL COMMENT '更新时间',
    `deleted`       tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`category_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_dept_id` (`dept_id`),
    UNIQUE KEY `uk_parent_name_deleted` (`parent_id`, `category_name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库分类表';

CREATE TABLE `knowledge_document`
(
    `document_id`         bigint NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `category_id`         bigint DEFAULT NULL COMMENT '分类ID',
    `dept_id`             bigint DEFAULT NULL COMMENT '所属部门ID',
    `title`               varchar(256) NOT NULL DEFAULT '' COMMENT '文档标题',
    `doc_code`            varchar(64) DEFAULT NULL COMMENT '文档编号',
    `summary`             varchar(1000) DEFAULT NULL COMMENT '文档摘要',
    `tags`                varchar(1000) DEFAULT NULL COMMENT '文档标签，逗号分隔',
    `visibility`          smallint NOT NULL DEFAULT '2' COMMENT '可见范围 1全员可见 2本部门可见 3仅本人可见',
    `status`              smallint NOT NULL DEFAULT '2' COMMENT '文档状态 1草稿 2审核中 3已发布 4已驳回 5已归档',
    `current_version_id`  bigint DEFAULT NULL COMMENT '当前版本ID',
    `current_version_no`  varchar(32) DEFAULT NULL COMMENT '当前版本号',
    `creator_id`          bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time`         datetime DEFAULT NULL COMMENT '创建时间',
    `updater_id`          bigint DEFAULT NULL COMMENT '更新人ID',
    `update_time`         datetime DEFAULT NULL COMMENT '更新时间',
    `deleted`             tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`document_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_dept_status` (`dept_id`, `status`),
    KEY `idx_visibility_status` (`visibility`, `status`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_update_time` (`update_time`),
    KEY `idx_title_prefix` (`title`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档主表';

CREATE TABLE `knowledge_document_version`
(
    `version_id`      bigint NOT NULL AUTO_INCREMENT COMMENT '版本ID',
    `document_id`     bigint NOT NULL COMMENT '文档ID',
    `version_no`      varchar(32) NOT NULL DEFAULT '' COMMENT '版本号',
    `file_name`       varchar(255) NOT NULL DEFAULT '' COMMENT '原始文件名',
    `file_ext`        varchar(32) DEFAULT NULL COMMENT '文件扩展名',
    `file_size`       bigint DEFAULT NULL COMMENT '文件大小，单位字节',
    `storage_type`    varchar(32) NOT NULL DEFAULT 'local' COMMENT '存储类型',
    `storage_path`    varchar(512) NOT NULL DEFAULT '' COMMENT '存储相对路径',
    `storage_url`     varchar(1024) DEFAULT NULL COMMENT '访问地址',
    `content_hash`    varchar(128) DEFAULT NULL COMMENT '文件内容哈希',
    `version_remark`  varchar(500) DEFAULT NULL COMMENT '版本说明',
    `parse_status`    smallint NOT NULL DEFAULT '1' COMMENT '解析状态 1待处理 2解析中 3解析成功 4解析失败',
    `is_current`      tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否当前版本',
    `creator_id`      bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time`     datetime DEFAULT NULL COMMENT '创建时间',
    `deleted`         tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`version_id`),
    UNIQUE KEY `uk_document_version_deleted` (`document_id`, `version_no`, `deleted`),
    KEY `idx_document_current` (`document_id`, `is_current`),
    KEY `idx_content_hash` (`content_hash`),
    KEY `idx_parse_status` (`parse_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档版本表';

CREATE TABLE `knowledge_document_acl`
(
    `acl_id`           bigint NOT NULL AUTO_INCREMENT COMMENT 'ACL记录ID',
    `document_id`      bigint NOT NULL COMMENT '文档ID',
    `subject_type`     smallint NOT NULL COMMENT '主体类型 1用户 2角色 3部门',
    `subject_id`       bigint NOT NULL COMMENT '主体ID',
    `permission_type`  smallint NOT NULL DEFAULT '1' COMMENT '权限类型 1查看 2下载 3管理',
    `creator_id`       bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time`      datetime DEFAULT NULL COMMENT '创建时间',
    `deleted`          tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`acl_id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_subject` (`subject_type`, `subject_id`),
    UNIQUE KEY `uk_acl_deleted` (`document_id`, `subject_type`, `subject_id`, `permission_type`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档访问控制表';

CREATE TABLE `knowledge_ingest_task`
(
    `task_id`         bigint NOT NULL AUTO_INCREMENT COMMENT '入库任务ID',
    `task_no`         varchar(64) NOT NULL DEFAULT '' COMMENT '任务编号',
    `document_id`     bigint NOT NULL COMMENT '文档ID',
    `version_id`      bigint NOT NULL COMMENT '版本ID',
    `task_type`       smallint NOT NULL DEFAULT '1' COMMENT '任务类型 1新增解析 2版本更新 3重试 4删除同步',
    `status`          smallint NOT NULL DEFAULT '1' COMMENT '任务状态 1待处理 2处理中 3成功 4失败',
    `retry_count`     int NOT NULL DEFAULT '0' COMMENT '重试次数',
    `chunk_count`     int DEFAULT NULL COMMENT '切片数量',
    `error_message`   varchar(2000) DEFAULT NULL COMMENT '错误信息',
    `trace_id`        varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
    `started_time`    datetime DEFAULT NULL COMMENT '开始时间',
    `finished_time`   datetime DEFAULT NULL COMMENT '结束时间',
    `creator_id`      bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time`     datetime DEFAULT NULL COMMENT '创建时间',
    `update_time`     datetime DEFAULT NULL COMMENT '更新时间',
    `deleted`         tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`task_id`),
    UNIQUE KEY `uk_task_no` (`task_no`),
    KEY `idx_document_version` (`document_id`, `version_id`),
    KEY `idx_status_create_time` (`status`, `create_time`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库入库任务表';

CREATE TABLE `ai_chat_session`
(
    `session_id`          bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `session_title`       varchar(255) NOT NULL DEFAULT '' COMMENT '会话标题',
    `user_id`             bigint NOT NULL COMMENT '用户ID',
    `dept_id`             bigint DEFAULT NULL COMMENT '部门ID',
    `last_message_time`   datetime DEFAULT NULL COMMENT '最后消息时间',
    `status`              smallint NOT NULL DEFAULT '1' COMMENT '状态 1启用 0停用',
    `creator_id`          bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time`         datetime DEFAULT NULL COMMENT '创建时间',
    `update_time`         datetime DEFAULT NULL COMMENT '更新时间',
    `deleted`             tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`session_id`),
    KEY `idx_user_last_time` (`user_id`, `last_message_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI问答会话表';

CREATE TABLE `ai_chat_message`
(
    `message_id`         bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id`         bigint NOT NULL COMMENT '会话ID',
    `message_role`       smallint NOT NULL COMMENT '消息角色 1用户 2助手',
    `message_content`    longtext NOT NULL COMMENT '消息内容',
    `source_snapshot`    json DEFAULT NULL COMMENT '命中来源快照',
    `model_name`         varchar(128) DEFAULT NULL COMMENT '模型名称',
    `confidence_level`   varchar(32) DEFAULT NULL COMMENT '置信等级',
    `creator_id`         bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time`        datetime DEFAULT NULL COMMENT '创建时间',
    `deleted`            tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`message_id`),
    KEY `idx_session_time` (`session_id`, `create_time`),
    KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI问答消息表';

CREATE TABLE `ai_audit_log`
(
    `audit_id`            bigint NOT NULL AUTO_INCREMENT COMMENT '审计ID',
    `session_id`          bigint DEFAULT NULL COMMENT '会话ID',
    `message_id`          bigint DEFAULT NULL COMMENT '消息ID',
    `user_id`             bigint DEFAULT NULL COMMENT '用户ID',
    `dept_id`             bigint DEFAULT NULL COMMENT '部门ID',
    `question_text`       varchar(2000) DEFAULT NULL COMMENT '问题文本',
    `retrieval_count`     int DEFAULT NULL COMMENT '召回条数',
    `top_score`           decimal(6,4) DEFAULT NULL COMMENT '最高得分',
    `model_name`          varchar(128) DEFAULT NULL COMMENT '模型名称',
    `prompt_tokens`       int DEFAULT NULL COMMENT '提示词Token数',
    `completion_tokens`   int DEFAULT NULL COMMENT '补全Token数',
    `total_tokens`        int DEFAULT NULL COMMENT '总Token数',
    `latency_ms`          int DEFAULT NULL COMMENT '耗时毫秒',
    `result_status`       smallint NOT NULL DEFAULT '1' COMMENT '结果状态 1成功 0失败 2拦截',
    `error_message`       varchar(2000) DEFAULT NULL COMMENT '错误信息',
    `create_time`         datetime DEFAULT NULL COMMENT '创建时间',
    `deleted`             tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`audit_id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_dept_time` (`dept_id`, `create_time`),
    KEY `idx_status_time` (`result_status`, `create_time`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI问答审计日志表';

ALTER TABLE `sys_user`
    ADD UNIQUE KEY `uk_username_deleted` (`username`, `deleted`),
    ADD KEY `idx_dept_id` (`dept_id`),
    ADD KEY `idx_role_id` (`role_id`),
    ADD KEY `idx_status` (`status`);

ALTER TABLE `sys_dept`
    ADD KEY `idx_parent_id` (`parent_id`),
    ADD KEY `idx_status` (`status`);

ALTER TABLE `sys_role`
    ADD UNIQUE KEY `uk_role_key_deleted` (`role_key`, `deleted`),
    ADD KEY `idx_status` (`status`);

ALTER TABLE `sys_notice`
    ADD KEY `idx_status_create_time` (`status`, `create_time`);

ALTER TABLE `sys_login_info`
    ADD KEY `idx_username_time` (`username`, `login_time`),
    ADD KEY `idx_status_time` (`status`, `login_time`);

ALTER TABLE `sys_operation_log`
    ADD KEY `idx_user_time` (`user_id`, `operation_time`),
    ADD KEY `idx_module_time` (`request_module`, `operation_time`),
    ADD KEY `idx_status_time` (`status`, `operation_time`);
