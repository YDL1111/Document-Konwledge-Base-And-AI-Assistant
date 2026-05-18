package com.docbase.admin.controller.ai;

import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.ai.chat.AiChatApplicationService;
import com.docbase.domain.ai.chat.dto.AiChatSessionDTO;
import com.docbase.domain.ai.chat.query.AiChatSessionQuery;
import com.docbase.infrastructure.user.AuthenticationUtils;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI问答API", description = "AI问答会话相关接口")
@RestController
@RequestMapping("/ai/chat/sessions")
@RequiredArgsConstructor
public class AiChatController extends BaseController {

    private final AiChatApplicationService aiChatApplicationService;

    @Operation(summary = "AI问答会话列表")
    @PreAuthorize("@permission.has('ai:chat:list')")
    @GetMapping
    public ResponseDTO<PageDTO<AiChatSessionDTO>> list(AiChatSessionQuery query) {
        restrictToCurrentUserIfNeeded(query);
        return ResponseDTO.ok(aiChatApplicationService.getSessionList(query));
    }

    private void restrictToCurrentUserIfNeeded(AiChatSessionQuery query) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        if (!loginUser.isAdmin()) {
            query.setUserId(loginUser.getUserId());
        }
    }
}
