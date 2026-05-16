package com.docbase.domain.knowledge.document;

import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentService;
import com.docbase.domain.knowledge.document.dto.KnowledgeDocumentDTO;
import com.docbase.domain.knowledge.document.query.KnowledgeDocumentQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentApplicationService {

    private final KnowledgeDocumentService knowledgeDocumentService;

    public PageDTO<KnowledgeDocumentDTO> getDocumentList(KnowledgeDocumentQuery query) {
        Page<KnowledgeDocumentEntity> page = knowledgeDocumentService.page(query.toPage(), query.toQueryWrapper());
        List<KnowledgeDocumentDTO> records = page.getRecords().stream()
            .map(KnowledgeDocumentDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }
}
