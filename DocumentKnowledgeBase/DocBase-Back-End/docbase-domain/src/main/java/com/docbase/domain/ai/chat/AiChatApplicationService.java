package com.docbase.domain.ai.chat;

import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.ai.chat.db.AiChatSessionEntity;
import com.docbase.domain.ai.chat.db.AiChatSessionService;
import com.docbase.domain.ai.chat.dto.AiChatSessionDTO;
import com.docbase.domain.ai.chat.query.AiChatSessionQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiChatApplicationService {

    private final AiChatSessionService aiChatSessionService;

    public PageDTO<AiChatSessionDTO> getSessionList(AiChatSessionQuery query) {
        Page<AiChatSessionEntity> page = aiChatSessionService.page(query.toPage(), query.toQueryWrapper());
        List<AiChatSessionDTO> records = page.getRecords().stream()
            .map(AiChatSessionDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }
}
