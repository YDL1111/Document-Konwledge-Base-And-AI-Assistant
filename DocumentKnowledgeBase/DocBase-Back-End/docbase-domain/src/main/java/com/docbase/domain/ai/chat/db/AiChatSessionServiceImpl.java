package com.docbase.domain.ai.chat.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AiChatSessionServiceImpl
    extends ServiceImpl<AiChatSessionMapper, AiChatSessionEntity>
    implements AiChatSessionService {
}
