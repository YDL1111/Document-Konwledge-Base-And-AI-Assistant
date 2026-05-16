package com.docbase.domain.knowledge.ingest.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeIngestTaskServiceImpl
    extends ServiceImpl<KnowledgeIngestTaskMapper, KnowledgeIngestTaskEntity>
    implements KnowledgeIngestTaskService {
}
