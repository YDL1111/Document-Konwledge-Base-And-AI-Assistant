package com.docbase.domain.knowledge.document.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeDocumentServiceImpl
    extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocumentEntity>
    implements KnowledgeDocumentService {
}
