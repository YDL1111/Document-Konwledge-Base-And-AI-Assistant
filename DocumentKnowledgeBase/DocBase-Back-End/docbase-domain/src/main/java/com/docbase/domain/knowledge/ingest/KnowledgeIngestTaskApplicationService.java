package com.docbase.domain.knowledge.ingest;

import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskEntity;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskService;
import com.docbase.domain.knowledge.ingest.dto.KnowledgeIngestTaskDTO;
import com.docbase.domain.knowledge.ingest.query.KnowledgeIngestTaskQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeIngestTaskApplicationService {

    private final KnowledgeIngestTaskService knowledgeIngestTaskService;

    public PageDTO<KnowledgeIngestTaskDTO> getTaskList(KnowledgeIngestTaskQuery query) {
        Page<KnowledgeIngestTaskEntity> page = knowledgeIngestTaskService.page(query.toPage(), query.toQueryWrapper());
        List<KnowledgeIngestTaskDTO> records = page.getRecords().stream()
            .map(KnowledgeIngestTaskDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }
}
