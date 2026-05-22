-- ============================================================
-- DocBase Public Bootstrap SQL
-- Purpose:
--   Public/open-source bootstrap script with schema + demo data only.
--   This file is safe to publish and contains no private business data.
--
-- Usage:
--   mysql -u root -p < docbase_knowledge_public_bootstrap.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `docbase_knowledge`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `docbase_knowledge`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `knowledge_document_audit_log`;
DROP TABLE IF EXISTS `ai_audit_log`;
DROP TABLE IF EXISTS `ai_chat_message`;
DROP TABLE IF EXISTS `ai_chat_session`;
DROP TABLE IF EXISTS `knowledge_ingest_task`;
DROP TABLE IF EXISTS `knowledge_document_acl`;
DROP TABLE IF EXISTS `knowledge_document_version`;
DROP TABLE IF EXISTS `knowledge_document`;
DROP TABLE IF EXISTS `knowledge_category`;
DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_post`;
DROP TABLE IF EXISTS `sys_operation_log`;
DROP TABLE IF EXISTS `sys_notice`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_login_info`;
DROP TABLE IF EXISTS `sys_dept`;
DROP TABLE IF EXISTS `sys_config`;

CREATE TABLE `sys_config`
(
    `config_id`       int NOT NULL AUTO_INCREMENT,
    `config_name`     varchar(128)  NOT NULL DEFAULT '',
    `config_key`      varchar(128)  NOT NULL DEFAULT '',
    `config_options`  varchar(1024) NOT NULL DEFAULT '',
    `config_value`    varchar(256)  NOT NULL DEFAULT '',
    `is_allow_change` tinyint(1)    NOT NULL,
    `creator_id`      bigint DEFAULT NULL,
    `updater_id`      bigint DEFAULT NULL,
    `update_time`     datetime DEFAULT NULL,
    `create_time`     datetime DEFAULT NULL,
    `remark`          varchar(128) DEFAULT NULL,
    `deleted`         tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`config_id`),
    UNIQUE KEY `config_key_uniq_idx` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='System config';

CREATE TABLE `sys_dept`
(
    `dept_id`      bigint NOT NULL AUTO_INCREMENT,
    `parent_id`    bigint NOT NULL DEFAULT 0,
    `ancestors`    text NOT NULL,
    `dept_name`    varchar(64) NOT NULL DEFAULT '',
    `order_num`    int NOT NULL DEFAULT 0,
    `leader_id`    bigint DEFAULT NULL,
    `leader_name`  varchar(64) DEFAULT NULL,
    `phone`        varchar(16) DEFAULT NULL,
    `email`        varchar(128) DEFAULT NULL,
    `status`       smallint NOT NULL DEFAULT 1,
    `creator_id`   bigint DEFAULT NULL,
    `create_time`  datetime DEFAULT NULL,
    `updater_id`   bigint DEFAULT NULL,
    `update_time`  datetime DEFAULT NULL,
    `deleted`      tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`dept_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Department';

CREATE TABLE `sys_login_info`
(
    `info_id`          bigint NOT NULL AUTO_INCREMENT,
    `username`         varchar(50) NOT NULL DEFAULT '',
    `ip_address`       varchar(128) NOT NULL DEFAULT '',
    `login_location`   varchar(255) NOT NULL DEFAULT '',
    `browser`          varchar(50) NOT NULL DEFAULT '',
    `operation_system` varchar(50) NOT NULL DEFAULT '',
    `status`           smallint NOT NULL DEFAULT 0,
    `msg`              varchar(255) NOT NULL DEFAULT '',
    `login_time`       datetime DEFAULT NULL,
    `deleted`          tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`info_id`),
    KEY `idx_username_time` (`username`, `login_time`),
    KEY `idx_status_time` (`status`, `login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Login info';

CREATE TABLE `sys_menu`
(
    `menu_id`      bigint NOT NULL AUTO_INCREMENT,
    `menu_name`    varchar(64) NOT NULL,
    `menu_type`    smallint NOT NULL DEFAULT 0,
    `router_name`  varchar(255) NOT NULL DEFAULT '',
    `parent_id`    bigint NOT NULL DEFAULT 0,
    `path`         varchar(255) DEFAULT NULL,
    `is_button`    tinyint(1) NOT NULL DEFAULT 0,
    `permission`   varchar(128) DEFAULT NULL,
    `meta_info`    varchar(1024) NOT NULL DEFAULT '{}',
    `status`       smallint NOT NULL DEFAULT 1,
    `remark`       varchar(256) DEFAULT '',
    `creator_id`   bigint DEFAULT NULL,
    `create_time`  datetime DEFAULT NULL,
    `updater_id`   bigint DEFAULT NULL,
    `update_time`  datetime DEFAULT NULL,
    `deleted`      tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Menu';

CREATE TABLE `sys_notice`
(
    `notice_id`       int NOT NULL AUTO_INCREMENT,
    `notice_title`    varchar(64) NOT NULL,
    `notice_type`     smallint NOT NULL,
    `notice_content`  text,
    `status`          smallint NOT NULL DEFAULT 1,
    `creator_id`      bigint NOT NULL,
    `create_time`     datetime DEFAULT NULL,
    `updater_id`      bigint DEFAULT NULL,
    `update_time`     datetime DEFAULT NULL,
    `remark`          varchar(255) NOT NULL DEFAULT '',
    `deleted`         tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`notice_id`),
    KEY `idx_status_create_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Notice';

CREATE TABLE `sys_operation_log`
(
    `operation_id`      bigint NOT NULL AUTO_INCREMENT,
    `business_type`     smallint NOT NULL DEFAULT 0,
    `request_method`    smallint NOT NULL DEFAULT 0,
    `request_module`    varchar(64) NOT NULL DEFAULT '',
    `request_url`       varchar(256) NOT NULL DEFAULT '',
    `called_method`     varchar(128) NOT NULL DEFAULT '',
    `operator_type`     smallint NOT NULL DEFAULT 0,
    `user_id`           bigint DEFAULT 0,
    `username`          varchar(32) DEFAULT '',
    `operator_ip`       varchar(128) DEFAULT '',
    `operator_location` varchar(256) DEFAULT '',
    `dept_id`           bigint DEFAULT 0,
    `dept_name`         varchar(64) DEFAULT NULL,
    `operation_param`   varchar(2048) DEFAULT '',
    `operation_result`  varchar(2048) DEFAULT '',
    `status`            smallint NOT NULL DEFAULT 1,
    `error_stack`       varchar(2048) DEFAULT '',
    `operation_time`    datetime NOT NULL,
    `deleted`           tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`operation_id`),
    KEY `idx_user_time` (`user_id`, `operation_time`),
    KEY `idx_module_time` (`request_module`, `operation_time`),
    KEY `idx_status_time` (`status`, `operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Operation log';

CREATE TABLE `sys_post`
(
    `post_id`      bigint NOT NULL AUTO_INCREMENT,
    `post_code`    varchar(64) NOT NULL,
    `post_name`    varchar(64) NOT NULL,
    `post_sort`    int NOT NULL,
    `status`       smallint NOT NULL,
    `remark`       varchar(512) DEFAULT NULL,
    `creator_id`   bigint DEFAULT NULL,
    `create_time`  datetime DEFAULT NULL,
    `updater_id`   bigint DEFAULT NULL,
    `update_time`  datetime DEFAULT NULL,
    `deleted`      tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Post';

CREATE TABLE `sys_role`
(
    `role_id`      bigint NOT NULL AUTO_INCREMENT,
    `role_name`    varchar(32) NOT NULL,
    `role_key`     varchar(128) NOT NULL,
    `role_sort`    int NOT NULL,
    `data_scope`   smallint DEFAULT 1,
    `dept_id_set`  varchar(1024) DEFAULT '',
    `status`       smallint NOT NULL,
    `creator_id`   bigint DEFAULT NULL,
    `create_time`  datetime DEFAULT NULL,
    `updater_id`   bigint DEFAULT NULL,
    `update_time`  datetime DEFAULT NULL,
    `remark`       varchar(512) DEFAULT NULL,
    `deleted`      tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`role_id`),
    UNIQUE KEY `uk_role_key_deleted` (`role_key`, `deleted`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Role';

CREATE TABLE `sys_role_menu`
(
    `role_id` bigint NOT NULL,
    `menu_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Role menu mapping';

CREATE TABLE `sys_user`
(
    `user_id`       bigint NOT NULL AUTO_INCREMENT,
    `post_id`       bigint DEFAULT NULL,
    `role_id`       bigint DEFAULT NULL,
    `dept_id`       bigint DEFAULT NULL,
    `username`      varchar(64) NOT NULL,
    `nickname`      varchar(32) NOT NULL,
    `user_type`     smallint DEFAULT 0,
    `email`         varchar(128) DEFAULT '',
    `phone_number`  varchar(18) DEFAULT '',
    `sex`           smallint DEFAULT 0,
    `avatar`        varchar(512) DEFAULT '',
    `password`      varchar(128) NOT NULL DEFAULT '',
    `status`        smallint NOT NULL DEFAULT 1,
    `login_ip`      varchar(128) DEFAULT '',
    `login_date`    datetime DEFAULT NULL,
    `is_admin`      tinyint(1) NOT NULL DEFAULT 0,
    `creator_id`    bigint DEFAULT NULL,
    `create_time`   datetime DEFAULT NULL,
    `updater_id`    bigint DEFAULT NULL,
    `update_time`   datetime DEFAULT NULL,
    `remark`        varchar(512) DEFAULT NULL,
    `deleted`       tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username_deleted` (`username`, `deleted`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='User';

CREATE TABLE `knowledge_category`
(
    `category_id`    bigint NOT NULL AUTO_INCREMENT,
    `parent_id`      bigint NOT NULL DEFAULT 0,
    `ancestors`      varchar(1024) NOT NULL DEFAULT '',
    `category_name`  varchar(128) NOT NULL DEFAULT '',
    `dept_id`        bigint DEFAULT NULL,
    `sort_num`       int NOT NULL DEFAULT 0,
    `status`         smallint NOT NULL DEFAULT 1,
    `remark`         varchar(255) DEFAULT NULL,
    `creator_id`     bigint DEFAULT NULL,
    `create_time`    datetime DEFAULT NULL,
    `updater_id`     bigint DEFAULT NULL,
    `update_time`    datetime DEFAULT NULL,
    `deleted`        tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`category_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_dept_id` (`dept_id`),
    UNIQUE KEY `uk_parent_name_deleted` (`parent_id`, `category_name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Knowledge category';

CREATE TABLE `knowledge_document`
(
    `document_id`         bigint NOT NULL AUTO_INCREMENT,
    `category_id`         bigint DEFAULT NULL,
    `dept_id`             bigint DEFAULT NULL,
    `title`               varchar(256) NOT NULL DEFAULT '',
    `doc_code`            varchar(64) DEFAULT NULL,
    `summary`             varchar(1000) DEFAULT NULL,
    `tags`                varchar(1000) DEFAULT NULL,
    `visibility`          smallint NOT NULL DEFAULT 2,
    `status`              smallint NOT NULL DEFAULT 3,
    `current_version_id`  bigint DEFAULT NULL,
    `current_version_no`  varchar(32) DEFAULT NULL,
    `audit_remark`        varchar(500) DEFAULT NULL,
    `creator_id`          bigint DEFAULT NULL,
    `create_time`         datetime DEFAULT NULL,
    `updater_id`          bigint DEFAULT NULL,
    `update_time`         datetime DEFAULT NULL,
    `deleted`             tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`document_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_dept_status` (`dept_id`, `status`),
    KEY `idx_visibility_status` (`visibility`, `status`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_update_time` (`update_time`),
    KEY `idx_title_prefix` (`title`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Knowledge document';

CREATE TABLE `knowledge_document_version`
(
    `version_id`      bigint NOT NULL AUTO_INCREMENT,
    `document_id`     bigint NOT NULL,
    `version_no`      varchar(32) NOT NULL DEFAULT '',
    `file_name`       varchar(255) NOT NULL DEFAULT '',
    `file_ext`        varchar(32) DEFAULT NULL,
    `file_size`       bigint DEFAULT NULL,
    `storage_type`    varchar(32) NOT NULL DEFAULT 'local',
    `storage_path`    varchar(512) NOT NULL DEFAULT '',
    `storage_url`     varchar(1024) DEFAULT NULL,
    `content_hash`    varchar(128) DEFAULT NULL,
    `version_remark`  varchar(500) DEFAULT NULL,
    `parse_status`    smallint NOT NULL DEFAULT 1,
    `is_current`      tinyint(1) NOT NULL DEFAULT 0,
    `creator_id`      bigint DEFAULT NULL,
    `create_time`     datetime DEFAULT NULL,
    `deleted`         tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`version_id`),
    UNIQUE KEY `uk_document_version_deleted` (`document_id`, `version_no`, `deleted`),
    KEY `idx_document_current` (`document_id`, `is_current`),
    KEY `idx_content_hash` (`content_hash`),
    KEY `idx_parse_status` (`parse_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Knowledge document version';

CREATE TABLE `knowledge_document_acl`
(
    `acl_id`            bigint NOT NULL AUTO_INCREMENT,
    `document_id`       bigint NOT NULL,
    `subject_type`      smallint NOT NULL,
    `subject_id`        bigint NOT NULL,
    `permission_type`   smallint NOT NULL DEFAULT 1,
    `creator_id`        bigint DEFAULT NULL,
    `create_time`       datetime DEFAULT NULL,
    `deleted`           tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`acl_id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_subject` (`subject_type`, `subject_id`),
    UNIQUE KEY `uk_acl_deleted` (`document_id`, `subject_type`, `subject_id`, `permission_type`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Knowledge document ACL';

CREATE TABLE `knowledge_ingest_task`
(
    `task_id`         bigint NOT NULL AUTO_INCREMENT,
    `task_no`         varchar(64) NOT NULL DEFAULT '',
    `document_id`     bigint NOT NULL,
    `version_id`      bigint NOT NULL,
    `task_type`       smallint NOT NULL DEFAULT 1,
    `status`          smallint NOT NULL DEFAULT 1,
    `retry_count`     int NOT NULL DEFAULT 0,
    `chunk_count`     int DEFAULT NULL,
    `error_message`   varchar(2000) DEFAULT NULL,
    `trace_id`        varchar(64) DEFAULT NULL,
    `started_time`    datetime DEFAULT NULL,
    `finished_time`   datetime DEFAULT NULL,
    `python_kb_id`    int DEFAULT NULL,
    `python_doc_id`   int DEFAULT NULL,
    `creator_id`      bigint DEFAULT NULL,
    `create_time`     datetime DEFAULT NULL,
    `update_time`     datetime DEFAULT NULL,
    `deleted`         tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`task_id`),
    UNIQUE KEY `uk_task_no` (`task_no`),
    KEY `idx_document_version` (`document_id`, `version_id`),
    KEY `idx_status_create_time` (`status`, `create_time`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Knowledge ingest task';

CREATE TABLE `ai_chat_session`
(
    `session_id`         bigint NOT NULL AUTO_INCREMENT,
    `session_title`      varchar(255) NOT NULL DEFAULT '',
    `user_id`            bigint NOT NULL,
    `dept_id`            bigint DEFAULT NULL,
    `last_message_time`  datetime DEFAULT NULL,
    `python_conv_id`     int DEFAULT NULL,
    `status`             smallint NOT NULL DEFAULT 1,
    `creator_id`         bigint DEFAULT NULL,
    `create_time`        datetime DEFAULT NULL,
    `update_time`        datetime DEFAULT NULL,
    `deleted`            tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`session_id`),
    KEY `idx_user_last_time` (`user_id`, `last_message_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI chat session';

CREATE TABLE `ai_chat_message`
(
    `message_id`       bigint NOT NULL AUTO_INCREMENT,
    `session_id`       bigint NOT NULL,
    `message_role`     tinyint NOT NULL,
    `message_content`  longtext NOT NULL,
    `sources_json`     longtext DEFAULT NULL,
    `python_conv_id`   int DEFAULT NULL,
    `kb_id`            int DEFAULT NULL,
    `model_name`       varchar(64) DEFAULT NULL,
    `error_flag`       tinyint NOT NULL DEFAULT 0,
    `error_message`    varchar(500) DEFAULT NULL,
    `create_time`      datetime DEFAULT NULL,
    `creator`          varchar(64) DEFAULT NULL,
    `del_flag`         tinyint NOT NULL DEFAULT 0,
    PRIMARY KEY (`message_id`),
    KEY `idx_session_time` (`session_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI chat message';

CREATE TABLE `ai_audit_log`
(
    `audit_id`          bigint NOT NULL AUTO_INCREMENT,
    `session_id`        bigint DEFAULT NULL,
    `message_id`        bigint DEFAULT NULL,
    `user_id`           bigint DEFAULT NULL,
    `dept_id`           bigint DEFAULT NULL,
    `question_text`     varchar(2000) DEFAULT NULL,
    `retrieval_count`   int DEFAULT NULL,
    `top_score`         decimal(6,4) DEFAULT NULL,
    `model_name`        varchar(128) DEFAULT NULL,
    `total_tokens`      int DEFAULT NULL,
    `latency_ms`        int DEFAULT NULL,
    `result_status`     smallint NOT NULL DEFAULT 1,
    `error_message`     varchar(2000) DEFAULT NULL,
    `creator_id`        bigint DEFAULT NULL,
    `create_time`       datetime DEFAULT NULL,
    `update_time`       datetime DEFAULT NULL,
    `deleted`           tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`audit_id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_dept_time` (`dept_id`, `create_time`),
    KEY `idx_status_time` (`result_status`, `create_time`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI audit log';

CREATE TABLE `knowledge_document_audit_log`
(
    `audit_log_id`   bigint NOT NULL AUTO_INCREMENT,
    `document_id`    bigint NOT NULL,
    `audit_result`   tinyint NOT NULL,
    `audit_remark`   varchar(500) DEFAULT NULL,
    `auditor_id`     bigint DEFAULT NULL,
    `auditor_name`   varchar(64) DEFAULT NULL,
    `before_status`  tinyint DEFAULT NULL,
    `after_status`   tinyint DEFAULT NULL,
    `creator_id`     bigint DEFAULT NULL,
    `create_time`    datetime DEFAULT NULL,
    `updater_id`     bigint DEFAULT NULL,
    `update_time`    datetime DEFAULT NULL,
    `deleted`        tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`audit_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Knowledge document audit log';

INSERT INTO `sys_config`
(`config_id`,`config_name`,`config_key`,`config_options`,`config_value`,`is_allow_change`,`creator_id`,`updater_id`,`update_time`,`create_time`,`remark`,`deleted`)
VALUES
(1,'Default skin','sys.index.skinName','[\"skin-blue\",\"skin-green\",\"skin-purple\",\"skin-red\",\"skin-yellow\"]','skin-blue',1,1,1,NOW(),NOW(),'UI skin',0),
(2,'Init password','sys.user.initPassword','','123456',1,1,1,NOW(),NOW(),'Demo init password',0),
(3,'Side theme','sys.index.sideTheme','[\"theme-dark\",\"theme-light\"]','theme-light',1,1,1,NOW(),NOW(),'Side theme',0),
(4,'Captcha enabled','sys.account.captchaOnOff','[\"true\",\"false\"]','false',1,1,1,NOW(),NOW(),'Login captcha switch',0),
(5,'Allow register','sys.account.registerUser','[\"true\",\"false\"]','false',1,1,1,NOW(),NOW(),'Register switch',0);

INSERT INTO `sys_dept`
(`dept_id`,`parent_id`,`ancestors`,`dept_name`,`order_num`,`leader_id`,`leader_name`,`phone`,`email`,`status`,`creator_id`,`create_time`,`updater_id`,`update_time`,`deleted`)
VALUES
(1,0,'0','DocBase Demo Org',0,NULL,'demo-admin','13800000000','demo@example.com',1,1,NOW(),1,NOW(),0),
(2,1,'0,1','R&D Center',1,NULL,'demo-admin','13800000000','demo@example.com',1,1,NOW(),1,NOW(),0),
(3,2,'0,1,2','Knowledge Team',1,NULL,'demo-admin','13800000000','demo@example.com',1,1,NOW(),1,NOW(),0);

INSERT INTO `sys_post`
(`post_id`,`post_code`,`post_name`,`post_sort`,`status`,`remark`,`creator_id`,`create_time`,`updater_id`,`update_time`,`deleted`)
VALUES
(1,'admin','Administrator',1,1,'Demo admin post',1,NOW(),1,NOW(),0),
(2,'user','Common User',2,1,'Demo user post',1,NOW(),1,NOW(),0);

INSERT INTO `sys_role`
(`role_id`,`role_name`,`role_key`,`role_sort`,`data_scope`,`dept_id_set`,`status`,`creator_id`,`create_time`,`updater_id`,`update_time`,`remark`,`deleted`)
VALUES
(1,'Administrator','admin',1,1,'',1,1,NOW(),1,NOW(),'System administrator',0),
(2,'Knowledge User','knowledge_user',20,2,'',1,1,NOW(),1,NOW(),'Knowledge base demo user',0);

INSERT INTO `sys_user`
(`user_id`,`post_id`,`role_id`,`dept_id`,`username`,`nickname`,`user_type`,`email`,`phone_number`,`sex`,`avatar`,`password`,`status`,`login_ip`,`login_date`,`is_admin`,`creator_id`,`create_time`,`updater_id`,`update_time`,`remark`,`deleted`)
VALUES
(1,1,1,3,'admin','DemoAdmin',0,'admin@example.com','13800000000',0,'','$2b$12$41O8nGxUmz9btpa0nmMEMODJludAMO95JfP41QTtWbtdpKzsc9qoi',1,'127.0.0.1',NOW(),1,1,NOW(),1,NOW(),'Public demo administrator',0);

INSERT INTO `sys_notice`
(`notice_id`,`notice_title`,`notice_type`,`notice_content`,`status`,`creator_id`,`create_time`,`updater_id`,`update_time`,`remark`,`deleted`)
VALUES
(1,'Welcome','2','This repository contains a public demo dataset only.',1,1,NOW(),1,NOW(),'Public demo notice',0);

INSERT INTO `sys_menu`
(`menu_id`,`menu_name`,`menu_type`,`router_name`,`parent_id`,`path`,`is_button`,`permission`,`meta_info`,`status`,`remark`,`creator_id`,`create_time`,`updater_id`,`update_time`,`deleted`)
VALUES
(1,'System Management',2,'',0,'/system',0,'','{\"title\":\"System Management\",\"icon\":\"ep:management\",\"showParent\":true,\"rank\":1}',1,'System module',1,NOW(),1,NOW(),0),
(2,'System Monitor',2,'',0,'/monitor',0,'','{\"title\":\"System Monitor\",\"icon\":\"ep:monitor\",\"showParent\":true,\"rank\":3}',1,'Monitor module',1,NOW(),1,NOW(),0),
(3,'System Tools',2,'',0,'/tool',0,'','{\"title\":\"System Tools\",\"icon\":\"ep:tools\",\"showParent\":true,\"rank\":2}',1,'Tool module',1,NOW(),1,NOW(),0),
(5,'User Management',1,'SystemUser',1,'/system/user/index',0,'system:user:list','{\"title\":\"User Management\",\"icon\":\"ep:user-filled\",\"showParent\":true}',1,'User page',1,NOW(),1,NOW(),0),
(6,'Role Management',1,'SystemRole',1,'/system/role/index',0,'system:role:list','{\"title\":\"Role Management\",\"icon\":\"ep:user\",\"showParent\":true}',1,'Role page',1,NOW(),1,NOW(),0),
(7,'Menu Management',1,'MenuManagement',1,'/system/menu/index',0,'system:menu:list','{\"title\":\"Menu Management\",\"icon\":\"ep:menu\",\"showParent\":true}',1,'Menu page',1,NOW(),1,NOW(),0),
(8,'Department Management',1,'Department',1,'/system/dept/index',0,'system:dept:list','{\"title\":\"Department Management\",\"icon\":\"fa-solid:code-branch\",\"showParent\":true}',1,'Dept page',1,NOW(),1,NOW(),0),
(9,'Post Management',1,'Post',1,'/system/post/index',0,'system:post:list','{\"title\":\"Post Management\",\"icon\":\"ep:postcard\",\"showParent\":true}',1,'Post page',1,NOW(),1,NOW(),0),
(10,'Config',1,'Config',1,'/system/config/index',0,'system:config:list','{\"title\":\"Config\",\"icon\":\"ep:setting\",\"showParent\":true}',1,'Config page',1,NOW(),1,NOW(),0),
(11,'Notice',1,'SystemNotice',1,'/system/notice/index',0,'system:notice:list','{\"title\":\"Notice\",\"icon\":\"ep:notification\",\"showParent\":true}',1,'Notice page',1,NOW(),1,NOW(),0),
(12,'Log Management',1,'LogManagement',1,'/system/logd',0,'','{\"title\":\"Log Management\",\"icon\":\"ep:document\",\"showParent\":true}',1,'Log module',1,NOW(),1,NOW(),0),
(13,'Online User',1,'OnlineUser',2,'/system/monitor/onlineUser/index',0,'monitor:online:list','{\"title\":\"Online User\",\"icon\":\"fa-solid:users\",\"showParent\":true}',1,'Online user page',1,NOW(),1,NOW(),0),
(14,'DB Monitor',1,'DataMonitor',2,'/system/monitor/druid/index',0,'monitor:druid:list','{\"title\":\"DB Monitor\",\"icon\":\"fa:database\",\"showParent\":true,\"frameSrc\":\"/druid/login.html\",\"isFrameSrcInternal\":true}',1,'DB monitor',1,NOW(),1,NOW(),0),
(15,'Server Monitor',1,'ServerInfo',2,'/system/monitor/server/index',0,'monitor:server:list','{\"title\":\"Server Monitor\",\"icon\":\"fa:server\",\"showParent\":true}',1,'Server monitor',1,NOW(),1,NOW(),0),
(16,'Cache Monitor',1,'CacheInfo',2,'/system/monitor/cache/index',0,'monitor:cache:list','{\"title\":\"Cache Monitor\",\"icon\":\"ep:reading\",\"showParent\":true}',1,'Cache monitor',1,NOW(),1,NOW(),0),
(17,'API Docs',1,'SystemAPI',3,'/tool/swagger/index',0,'tool:swagger:list','{\"title\":\"API Docs\",\"icon\":\"ep:document-remove\",\"showParent\":true,\"frameSrc\":\"/swagger-ui/index.html\",\"isFrameSrcInternal\":true}',1,'Swagger',1,NOW(),1,NOW(),0),
(18,'Operation Log',1,'OperationLog',12,'/system/log/operationLog/index',0,'monitor:operlog:list','{\"title\":\"Operation Log\"}',1,'Operation log page',1,NOW(),1,NOW(),0),
(19,'Login Log',1,'LoginLog',12,'/system/log/loginLog/index',0,'monitor:logininfor:list','{\"title\":\"Login Log\"}',1,'Login log page',1,NOW(),1,NOW(),0),
(20,'User Query',3,'',5,'',1,'system:user:query','{\"title\":\"User Query\"}',1,'',1,NOW(),1,NOW(),0),
(21,'User Add',3,'',5,'',1,'system:user:add','{\"title\":\"User Add\"}',1,'',1,NOW(),1,NOW(),0),
(22,'User Edit',3,'',5,'',1,'system:user:edit','{\"title\":\"User Edit\"}',1,'',1,NOW(),1,NOW(),0),
(23,'User Remove',3,'',5,'',1,'system:user:remove','{\"title\":\"User Remove\"}',1,'',1,NOW(),1,NOW(),0),
(24,'User Export',3,'',5,'',1,'system:user:export','{\"title\":\"User Export\"}',1,'',1,NOW(),1,NOW(),0),
(25,'User Import',3,'',5,'',1,'system:user:import','{\"title\":\"User Import\"}',1,'',1,NOW(),1,NOW(),0),
(26,'Reset Password',3,'',5,'',1,'system:user:resetPwd','{\"title\":\"Reset Password\"}',1,'',1,NOW(),1,NOW(),0),
(27,'Role Query',3,'',6,'',1,'system:role:query','{\"title\":\"Role Query\"}',1,'',1,NOW(),1,NOW(),0),
(28,'Role Add',3,'',6,'',1,'system:role:add','{\"title\":\"Role Add\"}',1,'',1,NOW(),1,NOW(),0),
(29,'Role Edit',3,'',6,'',1,'system:role:edit','{\"title\":\"Role Edit\"}',1,'',1,NOW(),1,NOW(),0),
(30,'Role Remove',3,'',6,'',1,'system:role:remove','{\"title\":\"Role Remove\"}',1,'',1,NOW(),1,NOW(),0),
(31,'Role Export',3,'',6,'',1,'system:role:export','{\"title\":\"Role Export\"}',1,'',1,NOW(),1,NOW(),0),
(32,'Role Data Scope',3,'',6,'',1,'system:role:dataScope','{\"title\":\"Role Data Scope\"}',1,'',1,NOW(),1,NOW(),0),
(33,'Role Status Edit',3,'',6,'',1,'system:role:changeStatus','{\"title\":\"Role Status Edit\"}',1,'',1,NOW(),1,NOW(),0),
(34,'Menu Query',3,'',7,'',1,'system:menu:query','{\"title\":\"Menu Query\"}',1,'',1,NOW(),1,NOW(),0),
(35,'Menu Add',3,'',7,'',1,'system:menu:add','{\"title\":\"Menu Add\"}',1,'',1,NOW(),1,NOW(),0),
(36,'Menu Edit',3,'',7,'',1,'system:menu:edit','{\"title\":\"Menu Edit\"}',1,'',1,NOW(),1,NOW(),0),
(37,'Menu Remove',3,'',7,'',1,'system:menu:remove','{\"title\":\"Menu Remove\"}',1,'',1,NOW(),1,NOW(),0),
(38,'Dept Query',3,'',8,'',1,'system:dept:query','{\"title\":\"Dept Query\"}',1,'',1,NOW(),1,NOW(),0),
(39,'Dept Add',3,'',8,'',1,'system:dept:add','{\"title\":\"Dept Add\"}',1,'',1,NOW(),1,NOW(),0),
(40,'Dept Edit',3,'',8,'',1,'system:dept:edit','{\"title\":\"Dept Edit\"}',1,'',1,NOW(),1,NOW(),0),
(41,'Dept Remove',3,'',8,'',1,'system:dept:remove','{\"title\":\"Dept Remove\"}',1,'',1,NOW(),1,NOW(),0),
(42,'Post Query',3,'',9,'',1,'system:post:query','{\"title\":\"Post Query\"}',1,'',1,NOW(),1,NOW(),0),
(43,'Post Add',3,'',9,'',1,'system:post:add','{\"title\":\"Post Add\"}',1,'',1,NOW(),1,NOW(),0),
(44,'Post Edit',3,'',9,'',1,'system:post:edit','{\"title\":\"Post Edit\"}',1,'',1,NOW(),1,NOW(),0),
(45,'Post Remove',3,'',9,'',1,'system:post:remove','{\"title\":\"Post Remove\"}',1,'',1,NOW(),1,NOW(),0),
(46,'Post Export',3,'',9,'',1,'system:post:export','{\"title\":\"Post Export\"}',1,'',1,NOW(),1,NOW(),0),
(47,'Config Query',3,'',10,'',1,'system:config:query','{\"title\":\"Config Query\"}',1,'',1,NOW(),1,NOW(),0),
(48,'Config Add',3,'',10,'',1,'system:config:add','{\"title\":\"Config Add\"}',1,'',1,NOW(),1,NOW(),0),
(49,'Config Edit',3,'',10,'',1,'system:config:edit','{\"title\":\"Config Edit\"}',1,'',1,NOW(),1,NOW(),0),
(50,'Config Remove',3,'',10,'',1,'system:config:remove','{\"title\":\"Config Remove\"}',1,'',1,NOW(),1,NOW(),0),
(51,'Notice Query',3,'',11,'',1,'system:notice:query','{\"title\":\"Notice Query\"}',1,'',1,NOW(),1,NOW(),0),
(52,'Notice Add',3,'',11,'',1,'system:notice:add','{\"title\":\"Notice Add\"}',1,'',1,NOW(),1,NOW(),0),
(53,'Notice Edit',3,'',11,'',1,'system:notice:edit','{\"title\":\"Notice Edit\"}',1,'',1,NOW(),1,NOW(),0),
(54,'Notice Remove',3,'',11,'',1,'system:notice:remove','{\"title\":\"Notice Remove\"}',1,'',1,NOW(),1,NOW(),0),
(55,'Oper Query',3,'',18,'',1,'monitor:operlog:query','{\"title\":\"Oper Query\"}',1,'',1,NOW(),1,NOW(),0),
(56,'Oper Remove',3,'',18,'',1,'monitor:operlog:remove','{\"title\":\"Oper Remove\"}',1,'',1,NOW(),1,NOW(),0),
(57,'Oper Export',3,'',18,'',1,'monitor:operlog:export','{\"title\":\"Oper Export\"}',1,'',1,NOW(),1,NOW(),0),
(58,'Login Query',3,'',19,'',1,'monitor:logininfor:query','{\"title\":\"Login Query\"}',1,'',1,NOW(),1,NOW(),0),
(59,'Login Remove',3,'',19,'',1,'monitor:logininfor:remove','{\"title\":\"Login Remove\"}',1,'',1,NOW(),1,NOW(),0),
(60,'Login Export',3,'',19,'',1,'monitor:logininfor:export','{\"title\":\"Login Export\"}',1,'',1,NOW(),1,NOW(),0),
(61,'Online Query',3,'',13,'',1,'monitor:online:query','{\"title\":\"Online Query\"}',1,'',1,NOW(),1,NOW(),0),
(62,'Online Force Logout',3,'',13,'',1,'monitor:online:forceLogout','{\"title\":\"Online Force Logout\"}',1,'',1,NOW(),1,NOW(),0),
(100,'Knowledge Management',2,'',0,'/knowledge',0,'','{\"title\":\"Knowledge Management\",\"icon\":\"ep:collection\",\"showParent\":true,\"rank\":4}',1,'Knowledge module',1,NOW(),1,NOW(),0),
(102,'Category Management',1,'KnowledgeCategory',100,'/knowledge/category/index',0,'knowledge:category:list','{\"title\":\"Category Management\",\"icon\":\"ep:folder\",\"showParent\":true}',1,'Category page',1,NOW(),1,NOW(),0),
(103,'Document Management',1,'KnowledgeDocument',100,'/knowledge/document/index',0,'knowledge:document:list','{\"title\":\"Document Management\",\"icon\":\"ep:document\",\"showParent\":true}',1,'Document page',1,NOW(),1,NOW(),0),
(104,'Ingest Task',1,'KnowledgeIngestTask',100,'/knowledge/ingest/index',0,'knowledge:ingest:list','{\"title\":\"Ingest Task\",\"icon\":\"ep:operation\",\"showParent\":true}',1,'Ingest page',1,NOW(),1,NOW(),0),
(105,'AI Chat',1,'AiChat',0,'/ai/chat/index',0,'ai:chat:list','{\"title\":\"AI Chat\",\"icon\":\"ep:chat-dot-round\",\"showParent\":true,\"rank\":5}',1,'AI chat page',1,NOW(),1,NOW(),0),
(106,'AI Audit',1,'AiAudit',0,'/ai/audit/index',0,'ai:audit:list','{\"title\":\"AI Audit\",\"icon\":\"ep:histogram\",\"showParent\":true,\"rank\":6}',1,'AI audit page',1,NOW(),1,NOW(),0),
(120,'Category Query',3,'',102,'',1,'knowledge:category:query','{}',1,'',1,NOW(),1,NOW(),0),
(121,'Category Add',3,'',102,'',1,'knowledge:category:add','{}',1,'',1,NOW(),1,NOW(),0),
(122,'Category Edit',3,'',102,'',1,'knowledge:category:edit','{}',1,'',1,NOW(),1,NOW(),0),
(123,'Category Remove',3,'',102,'',1,'knowledge:category:remove','{}',1,'',1,NOW(),1,NOW(),0),
(130,'Document Query',3,'',103,'',1,'knowledge:document:query','{}',1,'',1,NOW(),1,NOW(),0),
(131,'Document Upload',3,'',103,'',1,'knowledge:document:upload','{}',1,'',1,NOW(),1,NOW(),0),
(132,'Document Edit',3,'',103,'',1,'knowledge:document:edit','{}',1,'',1,NOW(),1,NOW(),0),
(133,'Document Remove',3,'',103,'',1,'knowledge:document:remove','{}',1,'',1,NOW(),1,NOW(),0),
(134,'Document Preview',3,'',103,'',1,'knowledge:document:preview','{}',1,'',1,NOW(),1,NOW(),0),
(135,'Document Download',3,'',103,'',1,'knowledge:document:download','{}',1,'',1,NOW(),1,NOW(),0),
(136,'Document Ingest',3,'',103,'',1,'knowledge:document:ingest','{}',1,'',1,NOW(),1,NOW(),0),
(137,'Document Rollback',3,'',103,'',1,'knowledge:document:rollback','{}',1,'',1,NOW(),1,NOW(),0),
(138,'Document Add',3,'',103,'',1,'knowledge:document:add','{}',1,'',1,NOW(),1,NOW(),0),
(139,'Document Audit',3,'',103,'',1,'knowledge:document:audit','{}',1,'',1,NOW(),1,NOW(),0),
(140,'Ingest Query',3,'',104,'',1,'knowledge:ingest:query','{}',1,'',1,NOW(),1,NOW(),0),
(141,'Ingest Retry',3,'',104,'',1,'knowledge:ingest:retry','{}',1,'',1,NOW(),1,NOW(),0),
(142,'Ingest Detail',3,'',104,'',1,'knowledge:ingest:detail','{}',1,'',1,NOW(),1,NOW(),0),
(150,'Chat Query',3,'',105,'',1,'ai:chat:query','{}',1,'',1,NOW(),1,NOW(),0),
(151,'Chat Send',3,'',105,'',1,'ai:chat:send','{}',1,'',1,NOW(),1,NOW(),0),
(152,'Chat Remove',3,'',105,'',1,'ai:chat:remove','{}',1,'',1,NOW(),1,NOW(),0),
(160,'Audit Query',3,'',106,'',1,'ai:audit:query','{}',1,'',1,NOW(),1,NOW(),0),
(161,'Audit Export',3,'',106,'',1,'ai:audit:export','{}',1,'',1,NOW(),1,NOW(),0);

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1, `menu_id` FROM `sys_menu`;

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
(2,100),(2,102),(2,103),(2,104),(2,105),
(2,120),(2,130),(2,134),(2,135),(2,140),
(2,150),(2,151),(2,152);

INSERT INTO `knowledge_category`
(`category_id`,`parent_id`,`ancestors`,`category_name`,`dept_id`,`sort_num`,`status`,`remark`,`creator_id`,`create_time`,`updater_id`,`update_time`,`deleted`)
VALUES
(1,0,'0','Public Demo Knowledge Base',3,1,1,'Open-source demo category',1,NOW(),1,NOW(),0),
(2,1,'0,1','Java Demo Docs',3,1,1,'Demo child category',1,NOW(),1,NOW(),0);

INSERT INTO `knowledge_document`
(`document_id`,`category_id`,`dept_id`,`title`,`doc_code`,`summary`,`tags`,`visibility`,`status`,`current_version_id`,`current_version_no`,`audit_remark`,`creator_id`,`create_time`,`updater_id`,`update_time`,`deleted`)
VALUES
(1,2,3,'Spring Boot Annotation Overview','DEMO-DOC-001','A public demo document record used for UI and API demonstration.','springboot,annotation,demo',1,3,1,'v1.0','Approved demo data',1,NOW(),1,NOW(),0);

INSERT INTO `knowledge_document_version`
(`version_id`,`document_id`,`version_no`,`file_name`,`file_ext`,`file_size`,`storage_type`,`storage_path`,`storage_url`,`content_hash`,`version_remark`,`parse_status`,`is_current`,`creator_id`,`create_time`,`deleted`)
VALUES
(1,1,'v1.0','spring-boot-annotation-overview.txt','txt',1024,'local','/demo/public/spring-boot-annotation-overview.txt',NULL,'demo-content-hash-001','Public demo version',3,1,1,NOW(),0);

INSERT INTO `knowledge_document_acl`
(`acl_id`,`document_id`,`subject_type`,`subject_id`,`permission_type`,`creator_id`,`create_time`,`deleted`)
VALUES
(1,1,2,2,1,1,NOW(),0),
(2,1,2,2,2,1,NOW(),0),
(3,1,2,1,3,1,NOW(),0);

SET FOREIGN_KEY_CHECKS = 1;
