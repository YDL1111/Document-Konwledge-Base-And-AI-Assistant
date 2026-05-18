package com.docbase.admin.controller.knowledge;

import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.domain.knowledge.qa.KnowledgeQaApplicationService;
import com.docbase.domain.knowledge.qa.command.KnowledgeQaAskCommand;
import com.docbase.domain.knowledge.qa.dto.KnowledgeQaAnswerDTO;
import com.docbase.domain.knowledge.qa.dto.KnowledgeQaHealthDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库问答 API", description = "知识库问答助手占位接口")
@RestController
@RequestMapping("/knowledge/qa")
@RequiredArgsConstructor
public class KnowledgeQaController extends BaseController {

    private final KnowledgeQaApplicationService knowledgeQaApplicationService;

    @Operation(summary = "知识库问答健康检查")
    @PreAuthorize("@permission.has('ai:chat:list')")
    @GetMapping("/health")
    public ResponseDTO<KnowledgeQaHealthDTO> health() {
        return ResponseDTO.ok(knowledgeQaApplicationService.health());
    }

    @Operation(summary = "知识库问答占位提问")
    @PreAuthorize("@permission.has('ai:chat:list')")
    @PostMapping("/ask")
    public ResponseDTO<KnowledgeQaAnswerDTO> ask(@Valid @RequestBody KnowledgeQaAskCommand command) {
        return ResponseDTO.ok(knowledgeQaApplicationService.ask(command));
    }
}
