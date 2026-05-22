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
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentService;
import com.docbase.domain.knowledge.document.dto.KnowledgeDocumentDTO;
import com.docbase.domain.knowledge.document.query.KnowledgeDocumentQuery;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskEntity;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskService;
import com.docbase.domain.knowledge.ingest.dto.KnowledgeIngestTaskDTO;
import com.docbase.domain.knowledge.ingest.query.KnowledgeIngestTaskQuery;
import com.docbase.infrastructure.client.python.KbMappingProperties;
import com.docbase.infrastructure.user.AuthenticationUtils;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI Chat API", description = "AI chat session endpoints")
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AiChatController extends BaseController {

    private final AiChatApplicationService aiChatApplicationService;
    private final KnowledgeIngestTaskService knowledgeIngestTaskService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final KbMappingProperties kbMappingProperties;

    @Operation(summary = "List AI chat sessions")
    @PreAuthorize("@permission.has('ai:chat:list')")
    @GetMapping("/sessions")
    public ResponseDTO<PageDTO<AiChatSessionDTO>> list(AiChatSessionQuery query) {
        restrictToCurrentUserIfNeeded(query);
        return ResponseDTO.ok(aiChatApplicationService.getSessionList(query));
    }

    @Operation(summary = "Get AI chat history")
    @PreAuthorize("@permission.has('ai:chat:list')")
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseDTO<List<AiChatMessageDTO>> getMessages(@PathVariable Long sessionId) {
        return ResponseDTO.ok(aiChatApplicationService.getMessages(sessionId));
    }

    @Operation(summary = "Delete AI chat session")
    @PreAuthorize("@permission.has('ai:chat:list')")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseDTO<String> deleteSession(@PathVariable Long sessionId) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        aiChatApplicationService.deleteSession(sessionId, loginUser);
        return ResponseDTO.ok("删除成功");
    }

    @Operation(summary = "Query AI chat")
    @PreAuthorize("@permission.has('ai:chat:query')")
    @PostMapping("/query")
    public ResponseDTO<AiChatAnswerDTO> query(@RequestBody AiChatQueryRequest request) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        AiChatAnswerDTO answer = aiChatApplicationService.query(request, loginUser);
        return ResponseDTO.ok(answer);
    }

    @Operation(summary = "Stream AI chat response")
    @PreAuthorize("@permission.has('ai:chat:query')")
    @PostMapping("/stream")
    public SseEmitter stream(@RequestBody AiChatQueryRequest request,
                             HttpServletResponse response) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        return aiChatApplicationService.streamQuery(request, loginUser, response);
    }

    @Operation(summary = "Stream AI Agent chat response (with tool calling)")
    @PreAuthorize("@permission.has('ai:chat:query')")
    @PostMapping("/agent/stream")
    public SseEmitter agentStream(@RequestBody AiChatQueryRequest request,
                                   HttpServletResponse response) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        return aiChatApplicationService.streamAgentQuery(request, loginUser, response);
    }

    // ---- Agent 只读工具接口（供 Python Agent HTTP 调用，无需 @PreAuthorize） ----

    @Operation(summary = "Agent: 查询导入任务列表")
    @GetMapping("/agent/tools/ingest-tasks")
    public ResponseDTO<PageDTO<KnowledgeIngestTaskDTO>> agentListIngestTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {

        KnowledgeIngestTaskQuery query = new KnowledgeIngestTaskQuery();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setStatus(status);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<KnowledgeIngestTaskEntity> page =
                knowledgeIngestTaskService.page(query.toPage(), query.toQueryWrapper());

        List<KnowledgeIngestTaskDTO> records = page.getRecords().stream()
                .map(KnowledgeIngestTaskDTO::new)
                .collect(Collectors.toList());

        return ResponseDTO.ok(new PageDTO<>(records, page.getTotal()));
    }

    @Operation(summary = "Agent: 查询导入任务详情")
    @GetMapping("/agent/tools/ingest-task/{taskId}")
    public ResponseDTO<KnowledgeIngestTaskDTO> agentGetIngestTaskDetail(@PathVariable Long taskId) {
        KnowledgeIngestTaskEntity entity = knowledgeIngestTaskService.getById(taskId);
        if (entity == null) {
            return ResponseDTO.fail();
        }
        return ResponseDTO.ok(new KnowledgeIngestTaskDTO(entity));
    }

    @Operation(summary = "Agent: 查询文档详情")
    @GetMapping("/agent/tools/document/{documentId}")
    public ResponseDTO<KnowledgeDocumentDTO> agentGetDocumentDetail(@PathVariable Long documentId) {
        KnowledgeDocumentEntity entity = knowledgeDocumentService.getById(documentId);
        if (entity == null) {
            return ResponseDTO.fail();
        }
        return ResponseDTO.ok(new KnowledgeDocumentDTO(entity));
    }

    @Operation(summary = "Agent: 按分类查询文档列表")
    @GetMapping("/agent/tools/documents")
    public ResponseDTO<PageDTO<KnowledgeDocumentDTO>> agentListDocuments(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {

        KnowledgeDocumentQuery query = new KnowledgeDocumentQuery();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        if (categoryId != null) {
            query.setCategoryId(categoryId);
        }
        if (status != null) {
            query.setStatus(status);
        }

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<KnowledgeDocumentEntity> page =
                knowledgeDocumentService.page(query.toPage(), query.toQueryWrapper());

        List<KnowledgeDocumentDTO> records = page.getRecords().stream()
                .map(KnowledgeDocumentDTO::new)
                .collect(Collectors.toList());

        return ResponseDTO.ok(new PageDTO<>(records, page.getTotal()));
    }

    @Operation(summary = "Agent: 查询分类与知识库的映射关系")
    @GetMapping("/agent/tools/kb-mapping")
    public ResponseDTO<Map<String, Object>> agentGetKbMapping() {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("defaultKbId", kbMappingProperties.getDefaultKbId());
        result.put("categoryMappings", kbMappingProperties.getCategoryMappings());
        return ResponseDTO.ok(result);
    }

    @Operation(summary = "Agent: 查询聊天会话列表")
    @GetMapping("/agent/tools/chat-sessions")
    public ResponseDTO<PageDTO<AiChatSessionDTO>> agentListChatSessions(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        AiChatSessionQuery query = new AiChatSessionQuery();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);

        return ResponseDTO.ok(aiChatApplicationService.getSessionList(query));
    }

    @Operation(summary = "Agent: 查询聊天会话历史消息")
    @GetMapping("/agent/tools/chat-sessions/{sessionId}/messages")
    public ResponseDTO<List<AiChatMessageDTO>> agentGetChatMessages(@PathVariable Long sessionId) {
        List<AiChatMessageDTO> messages = aiChatApplicationService.getMessages(sessionId);
        return ResponseDTO.ok(messages);
    }

    // ---- 原有方法 ----

    private void restrictToCurrentUserIfNeeded(AiChatSessionQuery query) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        if (!loginUser.isAdmin()) {
            query.setUserId(loginUser.getUserId());
        }
    }
}
