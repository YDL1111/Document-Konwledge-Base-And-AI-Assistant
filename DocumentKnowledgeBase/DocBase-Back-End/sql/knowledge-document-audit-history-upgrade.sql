USE `docbase_knowledge`;

CREATE TABLE IF NOT EXISTS `knowledge_document_audit_log` (
    `audit_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审核记录ID',
    `document_id` bigint NOT NULL COMMENT '文档ID',
    `audit_result` tinyint NOT NULL COMMENT '审核结果，1通过 0驳回',
    `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
    `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID',
    `auditor_name` varchar(64) DEFAULT NULL COMMENT '审核人名称',
    `before_status` tinyint DEFAULT NULL COMMENT '审核前状态',
    `after_status` tinyint DEFAULT NULL COMMENT '审核后状态',
    `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `updater_id` bigint DEFAULT NULL COMMENT '更新人ID',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`audit_log_id`),
    KEY `idx_kd_audit_document_id` (`document_id`),
    KEY `idx_kd_audit_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档审核记录表';
