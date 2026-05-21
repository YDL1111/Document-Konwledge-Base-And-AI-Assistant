package com.docbase.admin.controller.ai;

import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.ai.chat.AiChatApplicationService;
import com.docbase.domain.ai.chat.dto.AiChatAnswerDTO;
import com.docbase.domain.ai.chat.dto.AiChatMessageDTO;
import com.docbase.domain.ai.chat.dto.AiChatQueryRequest;
import com.docbase.domain.ai.chat.dto.AiChatSessionDTO;
import com.docbase.domain.ai.chat.query.AiChatSessionQuery;
import com.docbase.infrastructure.user.AuthenticationUtils;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI问答API", description = "AI问答会话相关接口")
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AiChatController extends BaseController {

    private final AiChatApplicationService aiChatApplicationService;

    @Operation(summary = "AI问答会话列表")
    @PreAuthorize("@permission.has('ai:chat:list')")
    @GetMapping("/sessions")
    public ResponseDTO<PageDTO<AiChatSessionDTO>> list(AiChatSessionQuery query) {
        restrictToCurrentUserIfNeeded(query);
        return ResponseDTO.ok(aiChatApplicationService.getSessionList(query));
    }

    @Operation(summary = "AI问答消息历史")
    @PreAuthorize("@permission.has('ai:chat:list')")
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseDTO<List<AiChatMessageDTO>> getMessages(@PathVariable Long sessionId) {
        return ResponseDTO.ok(aiChatApplicationService.getMessages(sessionId));
    }

    @Operation(summary = "AI问答查询")
    @PreAuthorize("@permission.has('ai:chat:query')")
    @PostMapping("/query")
    public ResponseDTO<AiChatAnswerDTO> query(@RequestBody AiChatQueryRequest request) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        AiChatAnswerDTO answer = aiChatApplicationService.query(request, loginUser);
        return ResponseDTO.ok(answer);
    }

    @Operation(summary = "AI闂瓟娴佸紡鏌ヨ")
    @PreAuthorize("@permission.has('ai:chat:query')")
    @PostMapping("/stream")
    public SseEmitter stream(@RequestBody AiChatQueryRequest request) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        return aiChatApplicationService.streamQuery(request, loginUser);
    }

    private void restrictToCurrentUserIfNeeded(AiChatSessionQuery query) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        if (!loginUser.isAdmin()) {
            query.setUserId(loginUser.getUserId());
        }
    }
}
