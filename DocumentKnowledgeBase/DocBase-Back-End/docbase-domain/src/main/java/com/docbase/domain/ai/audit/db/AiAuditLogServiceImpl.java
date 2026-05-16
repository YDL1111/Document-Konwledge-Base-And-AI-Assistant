package com.docbase.domain.ai.audit.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AiAuditLogServiceImpl
    extends ServiceImpl<AiAuditLogMapper, AiAuditLogEntity>
    implements AiAuditLogService {
}
