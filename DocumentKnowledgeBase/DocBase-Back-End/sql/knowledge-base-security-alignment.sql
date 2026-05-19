USE `docbase_knowledge`;

ALTER TABLE `knowledge_document`
    MODIFY COLUMN `visibility` smallint NOT NULL DEFAULT '2' COMMENT '可见范围（1全员可见 2本部门可见 3仅本人可见）',
    MODIFY COLUMN `status` smallint NOT NULL DEFAULT '1' COMMENT '文档状态（1草稿 2审核中 3已发布 4已驳回 5已归档）';

ALTER TABLE `knowledge_document_version`
    MODIFY COLUMN `storage_type` varchar(32) NOT NULL DEFAULT 'local' COMMENT '存储类型';
