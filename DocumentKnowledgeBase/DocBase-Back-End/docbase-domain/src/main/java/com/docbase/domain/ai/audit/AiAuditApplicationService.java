package com.docbase.domain.ai.audit;

import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.ai.audit.db.AiAuditLogEntity;
import com.docbase.domain.ai.audit.db.AiAuditLogService;
import com.docbase.domain.ai.audit.dto.AiAuditLogDTO;
import com.docbase.domain.ai.audit.query.AiAuditLogQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAuditApplicationService {

    private final AiAuditLogService aiAuditLogService;

    public PageDTO<AiAuditLogDTO> getAuditLogList(AiAuditLogQuery query) {
        Page<AiAuditLogEntity> page = aiAuditLogService.page(query.toPage(), query.toQueryWrapper());
        List<AiAuditLogDTO> records = page.getRecords().stream()
            .map(AiAuditLogDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }
}
