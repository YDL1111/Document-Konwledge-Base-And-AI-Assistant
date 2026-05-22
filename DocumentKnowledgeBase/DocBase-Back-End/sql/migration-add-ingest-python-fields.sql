-- Migration: Add Python RAG tracking fields to knowledge_ingest_task
-- Purpose: Track which Python KB/Doc each import task syncs to
-- Run against: docbase_knowledge database

ALTER TABLE `knowledge_ingest_task`
    ADD COLUMN `python_kb_id`  int DEFAULT NULL COMMENT '目标Python知识库ID' AFTER `finished_time`,
    ADD COLUMN `python_doc_id` int DEFAULT NULL COMMENT 'Python侧文档ID' AFTER `python_kb_id`;
