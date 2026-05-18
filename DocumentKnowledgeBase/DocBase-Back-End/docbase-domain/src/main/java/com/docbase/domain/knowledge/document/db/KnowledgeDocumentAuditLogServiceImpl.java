package com.docbase.domain.knowledge.document.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeDocumentAuditLogServiceImpl
    extends ServiceImpl<KnowledgeDocumentAuditLogMapper, KnowledgeDocumentAuditLogEntity>
    implements KnowledgeDocumentAuditLogService {
}
