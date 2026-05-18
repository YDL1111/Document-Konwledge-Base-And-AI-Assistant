package com.docbase.admin.controller.ai;

import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.ai.audit.AiAuditApplicationService;
import com.docbase.domain.ai.audit.dto.AiAuditLogDTO;
import com.docbase.domain.ai.audit.query.AiAuditLogQuery;
import com.docbase.infrastructure.user.AuthenticationUtils;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI审计API", description = "AI审计日志相关接口")
@RestController
@RequestMapping("/ai/audit/logs")
@RequiredArgsConstructor
public class AiAuditController extends BaseController {

    private final AiAuditApplicationService aiAuditApplicationService;

    @Operation(summary = "AI审计日志列表")
    @PreAuthorize("@permission.has('ai:audit:list')")
    @GetMapping
    public ResponseDTO<PageDTO<AiAuditLogDTO>> list(AiAuditLogQuery query) {
        restrictToCurrentUserIfNeeded(query);
        return ResponseDTO.ok(aiAuditApplicationService.getAuditLogList(query));
    }

    private void restrictToCurrentUserIfNeeded(AiAuditLogQuery query) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        if (!loginUser.isAdmin()) {
            query.setUserId(loginUser.getUserId());
        }
    }
}
