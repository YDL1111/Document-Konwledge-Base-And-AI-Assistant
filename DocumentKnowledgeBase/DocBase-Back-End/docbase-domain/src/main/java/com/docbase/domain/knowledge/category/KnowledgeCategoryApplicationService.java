package com.docbase.domain.knowledge.category;

import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.knowledge.category.db.KnowledgeCategoryEntity;
import com.docbase.domain.knowledge.category.db.KnowledgeCategoryService;
import com.docbase.domain.knowledge.category.dto.KnowledgeCategoryDTO;
import com.docbase.domain.knowledge.category.query.KnowledgeCategoryQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeCategoryApplicationService {

    private final KnowledgeCategoryService knowledgeCategoryService;

    public PageDTO<KnowledgeCategoryDTO> getCategoryList(KnowledgeCategoryQuery query) {
        Page<KnowledgeCategoryEntity> page = knowledgeCategoryService.page(query.toPage(), query.toQueryWrapper());
        List<KnowledgeCategoryDTO> records = page.getRecords().stream()
            .map(KnowledgeCategoryDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }
}
