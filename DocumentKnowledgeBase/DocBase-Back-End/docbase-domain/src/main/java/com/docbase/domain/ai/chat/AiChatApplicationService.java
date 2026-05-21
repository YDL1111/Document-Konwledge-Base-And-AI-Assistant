package com.docbase.domain.ai.chat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.ai.chat.db.AiChatMessageEntity;
import com.docbase.domain.ai.chat.db.AiChatMessageService;
import com.docbase.domain.ai.chat.db.AiChatSessionEntity;
import com.docbase.domain.ai.chat.db.AiChatSessionService;
import com.docbase.domain.ai.chat.dto.AiChatAnswerDTO;
import com.docbase.domain.ai.chat.dto.AiChatMessageDTO;
import com.docbase.domain.ai.chat.dto.AiChatQueryRequest;
import com.docbase.domain.ai.chat.dto.AiChatSessionDTO;
import com.docbase.domain.ai.chat.dto.AiChatStreamEventDTO;
import com.docbase.domain.ai.chat.query.AiChatSessionQuery;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentService;
import com.docbase.infrastructure.client.python.KbMappingProperties;
import com.docbase.infrastructure.client.python.PythonAiClient;
import com.docbase.infrastructure.client.python.dto.PythonChatRequest;
import com.docbase.infrastructure.client.python.dto.PythonChatResponse;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatApplicationService {

    private static final String DEFAULT_STREAM_ERROR_MESSAGE =
            "AI service is temporarily unavailable, please try again later";

    private final AiChatSessionService aiChatSessionService;
    private final PythonAiClient pythonAiClient;
    private final KbMappingProperties kbMappingProperties;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final AiChatMessageService aiChatMessageService;
    private final ObjectMapper objectMapper;

    public PageDTO<AiChatSessionDTO> getSessionList(AiChatSessionQuery query) {
        Page<AiChatSessionEntity> page = aiChatSessionService.page(query.toPage(), query.toQueryWrapper());
        List<AiChatSessionDTO> records = page.getRecords().stream()
                .map(AiChatSessionDTO::new)
                .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public List<AiChatMessageDTO> getMessages(Long sessionId) {
        List<AiChatMessageEntity> entities = aiChatMessageService.lambdaQuery()
                .eq(AiChatMessageEntity::getSessionId, sessionId)
                .orderByAsc(AiChatMessageEntity::getCreateTime)
                .list();

        return entities.stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, SystemLoginUser loginUser) {
        AiChatSessionEntity session = aiChatSessionService.getById(sessionId);
        if (session == null) {
            return;
        }

        if (!loginUser.isAdmin() && !Objects.equals(session.getUserId(), loginUser.getUserId())) {
            throw new IllegalArgumentException("You do not have permission to delete this session");
        }

        Integer pythonConvId = session.getPythonConvId();

        aiChatMessageService.lambdaUpdate()
                .eq(AiChatMessageEntity::getSessionId, sessionId)
                .remove();
        aiChatSessionService.removeById(sessionId);

        if (pythonConvId != null) {
            pythonAiClient.deleteConversation(pythonConvId);
        }
    }

    public AiChatAnswerDTO query(AiChatQueryRequest request, SystemLoginUser loginUser) {
        Integer kbId = resolveKbId(request);
        AiChatSessionEntity session = findOrCreateSession(request, loginUser);
        Integer pythonConvId = session.getPythonConvId();

        saveUserMessage(session, request, kbId, loginUser);

        PythonChatRequest pythonRequest = PythonChatRequest.builder()
                .kbId(kbId)
                .convId(pythonConvId)
                .question(request.getQuestion())
                .stream(false)
                .build();

        log.info("AI query: sessionId={}, kbId={}, pythonConvId={}, question={}",
                session.getSessionId(), kbId, pythonConvId, request.getQuestion());

        try {
            PythonChatResponse pythonResponse = pythonAiClient.sendMessage(pythonRequest);

            if (pythonResponse.getData() != null) {
                session.setPythonConvId(pythonResponse.getData().getConvId());
            }

            updateSessionAfterQuery(session, request.getQuestion());
            saveAssistantSuccessMessage(session, pythonResponse, kbId);

            return buildAnswerDTO(session.getSessionId(), pythonResponse);
        } catch (Exception e) {
            saveAssistantErrorMessage(session, kbId, e);
            throw e;
        }
    }

    public SseEmitter streamQuery(AiChatQueryRequest request, SystemLoginUser loginUser) {
        Integer kbId = resolveKbId(request);
        AiChatSessionEntity session = findOrCreateSession(request, loginUser);
        Integer pythonConvId = session.getPythonConvId();

        saveUserMessage(session, request, kbId, loginUser);

        PythonChatRequest pythonRequest = PythonChatRequest.builder()
                .kbId(kbId)
                .convId(pythonConvId)
                .question(request.getQuestion())
                .stream(true)
                .build();

        SseEmitter emitter = new SseEmitter();

        Thread streamThread = new Thread(() -> {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginUser,
                    null,
                    loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);
            StringBuilder answerBuilder = new StringBuilder();
            List<AiChatAnswerDTO.SourceInfo> finalSources = new ArrayList<>();

            try {
                sendEvent(emitter, "conv_id", session.getSessionId());
                pythonAiClient.streamMessage(pythonRequest, line -> {
                    try {
                        handlePythonStreamLine(line, session, emitter, answerBuilder, finalSources);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                updateSessionAfterQuery(session, request.getQuestion());
                saveAssistantStreamMessage(session, kbId, answerBuilder.toString(), finalSources);
                sendEvent(emitter, "done", Map.of("answer", answerBuilder.toString(), "sources", finalSources));
                emitter.complete();
            } catch (Exception e) {
                log.error("AI stream query failed: sessionId={}", session.getSessionId(), e);
                saveAssistantErrorMessage(session, kbId, e);
                try {
                    sendEvent(emitter, "error", e.getMessage() != null ? e.getMessage() : DEFAULT_STREAM_ERROR_MESSAGE);
                } catch (IOException ignored) {
                    log.warn("Failed to send stream error event");
                }
                emitter.complete();
            } finally {
                SecurityContextHolder.clearContext();
            }
        }, "ai-chat-stream-" + session.getSessionId());
        streamThread.start();

        return emitter;
    }

    private Integer resolveKbId(AiChatQueryRequest request) {
        if (request.getKbId() != null) {
            log.info("kbId resolved: source=explicit, kbId={}", request.getKbId());
            return request.getKbId();
        }

        if (request.getDocumentId() != null) {
            KnowledgeDocumentEntity doc = knowledgeDocumentService.getById(request.getDocumentId());
            if (doc != null && doc.getCategoryId() != null) {
                if (request.getCategoryId() != null && !doc.getCategoryId().equals(request.getCategoryId())) {
                    log.warn(
                            "kbId resolution: documentId({}) categoryId({}) differs from request categoryId({}), using document category for mapping",
                            request.getDocumentId(), doc.getCategoryId(), request.getCategoryId());
                }
                Integer mapped = kbMappingProperties.getCategoryMappings().get(doc.getCategoryId());
                if (mapped != null) {
                    log.info("kbId resolved: source=documentId({}) -> categoryId({}) -> config-map, kbId={}",
                            request.getDocumentId(), doc.getCategoryId(), mapped);
                    return mapped;
                }
            }
        }

        if (request.getCategoryId() != null) {
            Integer mapped = kbMappingProperties.getCategoryMappings().get(request.getCategoryId());
            if (mapped != null) {
                log.info("kbId resolved: source=categoryId({}) -> config-map, kbId={}",
                        request.getCategoryId(), mapped);
                return mapped;
            }
        }

        int defaultKbId = kbMappingProperties.getDefaultKbId();
        log.info("kbId resolved: source=fallback-default, kbId={}", defaultKbId);
        return defaultKbId;
    }

    private AiChatSessionEntity findOrCreateSession(AiChatQueryRequest request, SystemLoginUser loginUser) {
        if (request.getSessionId() != null) {
            AiChatSessionEntity session = aiChatSessionService.getById(request.getSessionId());
            if (session != null) {
                return session;
            }
        }

        String title = request.getQuestion();
        if (title.length() > 20) {
            title = title.substring(0, 20) + "...";
        }

        AiChatSessionEntity session = new AiChatSessionEntity();
        session.setSessionTitle(title);
        session.setUserId(loginUser.getUserId());
        session.setDeptId(loginUser.getDeptId());
        session.setLastMessageTime(new Date());
        session.setStatus(1);
        aiChatSessionService.save(session);
        return session;
    }

    private void updateSessionAfterQuery(AiChatSessionEntity session, String question) {
        session.setLastMessageTime(new Date());
        if (session.getSessionTitle() == null || session.getSessionTitle().isBlank()) {
            String title = question.length() > 20 ? question.substring(0, 20) + "..." : question;
            session.setSessionTitle(title);
        }
        aiChatSessionService.updateById(session);
    }

    private AiChatAnswerDTO buildAnswerDTO(Long sessionId, PythonChatResponse pythonResponse) {
        PythonChatResponse.PythonMessage msg = pythonResponse.getData() != null
                ? pythonResponse.getData().getMessage()
                : null;

        String answer = msg != null ? msg.getContent() : "No answer was returned";
        List<AiChatAnswerDTO.SourceInfo> sources = List.of();

        if (msg != null && msg.getSources() != null) {
            sources = msg.getSources().stream()
                    .map(s -> AiChatAnswerDTO.SourceInfo.builder()
                            .filename(s.getFilename())
                            .page(s.getPage())
                            .score(s.getScore())
                            .content(s.getContent())
                            .build())
                    .collect(Collectors.toList());
        }

        return AiChatAnswerDTO.builder()
                .sessionId(sessionId)
                .answer(answer)
                .sources(sources)
                .build();
    }

    private void saveUserMessage(AiChatSessionEntity session, AiChatQueryRequest request,
            Integer kbId, SystemLoginUser loginUser) {
        AiChatMessageEntity msg = new AiChatMessageEntity();
        msg.setSessionId(session.getSessionId());
        msg.setMessageRole(1);
        msg.setMessageContent(request.getQuestion());
        msg.setKbId(kbId);
        msg.setErrorFlag(0);
        msg.setCreator(loginUser.getUsername());
        msg.setCreateTime(new Date());
        aiChatMessageService.save(msg);
    }

    private void saveAssistantSuccessMessage(AiChatSessionEntity session,
            PythonChatResponse pythonResponse, Integer kbId) {
        AiChatMessageEntity msg = new AiChatMessageEntity();
        msg.setSessionId(session.getSessionId());
        msg.setMessageRole(2);
        msg.setKbId(kbId);
        msg.setModelName("deepseek-v4-flash");
        msg.setErrorFlag(0);

        if (session.getPythonConvId() != null) {
            msg.setPythonConvId(session.getPythonConvId());
        }

        PythonChatResponse.PythonMessage pyMsg = pythonResponse.getData() != null
                ? pythonResponse.getData().getMessage()
                : null;
        msg.setMessageContent(pyMsg != null ? pyMsg.getContent() : "");

        if (pyMsg != null && pyMsg.getSources() != null) {
            try {
                msg.setSourcesJson(objectMapper.writeValueAsString(pyMsg.getSources()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize sources to JSON", e);
            }
        }

        aiChatMessageService.save(msg);
    }

    private void saveAssistantErrorMessage(AiChatSessionEntity session, Integer kbId, Exception e) {
        AiChatMessageEntity msg = new AiChatMessageEntity();
        msg.setSessionId(session.getSessionId());
        msg.setMessageRole(2);
        msg.setMessageContent("");
        msg.setKbId(kbId);
        msg.setErrorFlag(1);
        String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        if (errMsg.length() > 500) {
            errMsg = errMsg.substring(0, 500);
        }
        msg.setErrorMessage(errMsg);
        msg.setCreateTime(new Date());
        aiChatMessageService.save(msg);
    }

    private void saveAssistantStreamMessage(AiChatSessionEntity session, Integer kbId,
            String answer, List<AiChatAnswerDTO.SourceInfo> sources) {
        AiChatMessageEntity msg = new AiChatMessageEntity();
        msg.setSessionId(session.getSessionId());
        msg.setMessageRole(2);
        msg.setKbId(kbId);
        msg.setModelName("deepseek-v4-flash");
        msg.setErrorFlag(0);
        msg.setPythonConvId(session.getPythonConvId());
        msg.setMessageContent(answer);
        msg.setCreateTime(new Date());

        if (sources != null && !sources.isEmpty()) {
            try {
                msg.setSourcesJson(objectMapper.writeValueAsString(sources));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize stream sources to JSON", e);
            }
        }

        aiChatMessageService.save(msg);
    }

    private void handlePythonStreamLine(String line,
            AiChatSessionEntity session,
            SseEmitter emitter,
            StringBuilder answerBuilder,
            List<AiChatAnswerDTO.SourceInfo> finalSources) throws IOException {
        if (line == null || line.isBlank()) {
            return;
        }

        String json = extractSseData(line);
        if (json == null || json.isBlank()) {
            return;
        }

        AiChatStreamEventDTO event = objectMapper.readValue(json, AiChatStreamEventDTO.class);
        if (event == null || event.getType() == null) {
            return;
        }

        switch (event.getType()) {
            case "token" -> {
                String token = event.getData() != null ? String.valueOf(event.getData()) : "";
                answerBuilder.append(token);
                sendEvent(emitter, "token", token);
            }
            case "sources" -> {
                List<AiChatAnswerDTO.SourceInfo> sources = objectMapper.convertValue(
                        event.getData(),
                        new TypeReference<List<AiChatAnswerDTO.SourceInfo>>() {});
                finalSources.clear();
                finalSources.addAll(sources);
                sendEvent(emitter, "sources", sources);
            }
            case "conv_id" -> {
                Integer convId = objectMapper.convertValue(event.getData(), Integer.class);
                if (convId != null) {
                    session.setPythonConvId(convId);
                    aiChatSessionService.updateById(session);
                    sendEvent(emitter, "python_conv_id", convId);
                }
            }
            case "error" -> {
                String error = event.getData() != null
                        ? String.valueOf(event.getData())
                        : DEFAULT_STREAM_ERROR_MESSAGE;
                sendEvent(emitter, "error", error);
                throw new IOException(error);
            }
            default -> {
                // Ignore done/unknown events from Python; Java emits final done after persistence.
            }
        }
    }

    private String extractSseData(String eventBlock) {
        StringBuilder builder = new StringBuilder();
        String[] lines = eventBlock.split("\\R");
        for (String item : lines) {
            if (item == null) {
                continue;
            }

            String trimmed = item.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }

            String value = trimmed.length() > 5 ? trimmed.substring(5).trim() : "";
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(value);
        }

        return builder.isEmpty() ? null : builder.toString();
    }

    private void sendEvent(SseEmitter emitter, String type, Object data) throws IOException {
        String json = objectMapper.writeValueAsString(AiChatStreamEventDTO.builder()
                .type(type)
                .data(data)
                .build());
        emitter.send(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
    }

    private AiChatMessageDTO toMessageDTO(AiChatMessageEntity entity) {
        AiChatMessageDTO dto = new AiChatMessageDTO();
        dto.setMessageId(entity.getMessageId());
        dto.setSessionId(entity.getSessionId());
        dto.setMessageRole(entity.getMessageRole());
        dto.setMessageContent(entity.getMessageContent());
        dto.setCreateTime(entity.getCreateTime());

        if (entity.getSourcesJson() != null && !entity.getSourcesJson().isBlank()) {
            try {
                List<AiChatAnswerDTO.SourceInfo> sources = objectMapper.readValue(
                        entity.getSourcesJson(),
                        new TypeReference<List<AiChatAnswerDTO.SourceInfo>>() {});
                dto.setSources(sources);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize sources_json for messageId={}", entity.getMessageId());
            }
        }

        return dto;
    }
}
