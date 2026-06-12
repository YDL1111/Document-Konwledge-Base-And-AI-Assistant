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
import com.docbase.domain.knowledge.document.KnowledgeDocumentConstant;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentService;
import com.docbase.domain.knowledge.ingest.KnowledgeIngestTaskApplicationService;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskEntity;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskService;
import com.docbase.domain.system.dept.db.SysDeptService;
import com.docbase.infrastructure.client.python.KbMappingProperties;
import com.docbase.infrastructure.client.python.PythonAiClient;
import com.docbase.infrastructure.client.python.dto.PythonChatRequest;
import com.docbase.infrastructure.client.python.dto.PythonChatResponse;
import com.docbase.infrastructure.user.web.DataScopeEnum;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
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
    private final KnowledgeIngestTaskService knowledgeIngestTaskService;
    private final SysDeptService sysDeptService;
    private final AiChatMessageService aiChatMessageService;
    private final ObjectMapper objectMapper;

    public PageDTO<AiChatSessionDTO> getSessionList(AiChatSessionQuery query) {
        Page<AiChatSessionEntity> page = aiChatSessionService.page(query.toPage(), query.toQueryWrapper());
        List<AiChatSessionDTO> records = page.getRecords().stream()
                .map(AiChatSessionDTO::new)
                .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public List<AiChatMessageDTO> getMessages(Long sessionId, SystemLoginUser loginUser) {
        // 🔒 校验会话归属：非管理员只能读取自己的会话消息
        AiChatSessionEntity session = aiChatSessionService.getById(sessionId);
        if (session == null) {
            return Collections.emptyList();
        }
        if (!loginUser.isAdmin() && !Objects.equals(session.getUserId(), loginUser.getUserId())) {
            throw new IllegalArgumentException("You do not have permission to view this session");
        }

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
        List<Integer> visibleDocIds = resolveVisiblePythonDocIds(request, kbId, loginUser);

        saveUserMessage(session, request, kbId, loginUser);

        PythonChatRequest pythonRequest = PythonChatRequest.builder()
                .kbId(kbId)
                .convId(pythonConvId)
                .question(request.getQuestion())
                .stream(false)
                .visibleDocIds(visibleDocIds)
                .build();

        log.info("AI query: sessionId={}, kbId={}, pythonConvId={}, visibleDocs={}, question={}",
                session.getSessionId(), kbId, pythonConvId, visibleDocIds.size(), request.getQuestion());

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

    public SseEmitter streamQuery(AiChatQueryRequest request, SystemLoginUser loginUser,
                                   HttpServletResponse response) {
        Integer kbId = resolveKbId(request);
        AiChatSessionEntity session = findOrCreateSession(request, loginUser);
        Integer pythonConvId = session.getPythonConvId();
        List<Integer> visibleDocIds = resolveVisiblePythonDocIds(request, kbId, loginUser);

        saveUserMessage(session, request, kbId, loginUser);

        PythonChatRequest pythonRequest = PythonChatRequest.builder()
                .kbId(kbId)
                .convId(pythonConvId)
                .question(request.getQuestion())
                .stream(true)
                .visibleDocIds(visibleDocIds)
                .build();

        SseEmitter emitter = new SseEmitter(-1L);

        Thread streamThread = new Thread(() -> {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginUser,
                    null,
                    loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);
            StringBuilder answerBuilder = new StringBuilder();
            List<AiChatAnswerDTO.SourceInfo> finalSources = new ArrayList<>();

            try {
                sendEvent(emitter, "conv_id", session.getSessionId(), response);
                pythonAiClient.streamMessage(pythonRequest, line -> {
                    try {
                        handlePythonStreamLine(line, session, emitter, answerBuilder,
                                finalSources, response);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                updateSessionAfterQuery(session, request.getQuestion());
                saveAssistantStreamMessage(session, kbId, answerBuilder.toString(), finalSources);
                sendEvent(emitter, "done",
                        Map.of("answer", answerBuilder.toString(), "sources", finalSources), response);
                emitter.complete();
            } catch (Exception e) {
                log.error("AI stream query failed: sessionId={}", session.getSessionId(), e);
                saveAssistantErrorMessage(session, kbId, e);
                try {
                    sendEvent(emitter, "error",
                            e.getMessage() != null ? e.getMessage() : DEFAULT_STREAM_ERROR_MESSAGE, response);
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

    private List<Integer> resolveVisiblePythonDocIds(AiChatQueryRequest request, Integer kbId,
                                                     SystemLoginUser loginUser) {
        List<KnowledgeDocumentEntity> candidateDocs = loadCandidateDocuments(request, kbId, loginUser);
        if (candidateDocs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> visibleDocumentIds = candidateDocs.stream()
                .filter(doc -> canCurrentUserView(doc, loginUser))
                .map(KnowledgeDocumentEntity::getDocumentId)
                .collect(Collectors.toList());
        if (visibleDocumentIds.isEmpty()) {
            return Collections.emptyList();
        }

        if (request.getDocumentId() == null) {
            Long inferredDocumentId = inferDocumentIdFromQuestion(candidateDocs, loginUser, request.getQuestion());
            if (inferredDocumentId != null) {
                visibleDocumentIds = visibleDocumentIds.stream()
                        .filter(documentId -> Objects.equals(documentId, inferredDocumentId))
                        .collect(Collectors.toList());
                log.info("AI query auto scoped to documentId={} by question match: question={}",
                        inferredDocumentId, request.getQuestion());
            }
        }

        return knowledgeIngestTaskService.list(
                        new LambdaQueryWrapper<KnowledgeIngestTaskEntity>()
                                .eq(KnowledgeIngestTaskEntity::getStatus,
                                        KnowledgeIngestTaskApplicationService.STATUS_SUCCESS)
                                .ne(KnowledgeIngestTaskEntity::getTaskType,
                                        KnowledgeIngestTaskApplicationService.TASK_TYPE_DELETE)
                                .eq(KnowledgeIngestTaskEntity::getPythonKbId, kbId)
                                .isNotNull(KnowledgeIngestTaskEntity::getPythonDocId)
                                .in(KnowledgeIngestTaskEntity::getDocumentId, visibleDocumentIds)
                                .orderByDesc(KnowledgeIngestTaskEntity::getFinishedTime)
                                .orderByDesc(KnowledgeIngestTaskEntity::getTaskId))
                .stream()
                .filter(task -> task.getPythonDocId() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                KnowledgeIngestTaskEntity::getDocumentId,
                                KnowledgeIngestTaskEntity::getPythonDocId,
                                (first, ignored) -> first),
                        map -> new ArrayList<>(new HashSet<>(map.values()))));
    }

    private Long inferDocumentIdFromQuestion(List<KnowledgeDocumentEntity> candidateDocs,
                                             SystemLoginUser loginUser,
                                             String question) {
        if (question == null || question.isBlank()) {
            return null;
        }

        String normalizedQuestion = normalizeForMatch(question);
        if (normalizedQuestion.isBlank()) {
            return null;
        }

        List<KnowledgeDocumentEntity> visibleDocs = candidateDocs.stream()
                .filter(doc -> canCurrentUserView(doc, loginUser))
                .collect(Collectors.toList());

        List<KnowledgeDocumentEntity> strongMatches = visibleDocs.stream()
                .filter(doc -> {
                    String title = normalizeForMatch(doc.getTitle());
                    return !title.isBlank() && normalizedQuestion.contains(title);
                })
                .collect(Collectors.toList());
        if (strongMatches.size() == 1) {
            return strongMatches.get(0).getDocumentId();
        }

        KnowledgeDocumentEntity bestDoc = null;
        int bestScore = 0;
        boolean tie = false;
        for (KnowledgeDocumentEntity doc : visibleDocs) {
            int score = matchScore(normalizedQuestion, doc);
            if (score <= 0) {
                continue;
            }
            if (score > bestScore) {
                bestScore = score;
                bestDoc = doc;
                tie = false;
            } else if (score == bestScore) {
                tie = true;
            }
        }

        if (!tie && bestDoc != null && bestScore >= 2) {
            return bestDoc.getDocumentId();
        }
        return null;
    }

    private int matchScore(String normalizedQuestion, KnowledgeDocumentEntity doc) {
        String title = normalizeForMatch(doc.getTitle());
        if (title.isBlank()) {
            return 0;
        }

        int score = 0;
        if (normalizedQuestion.contains(title)) {
            score += 10;
        }

        for (String token : splitMatchTokens(title)) {
            if (token.length() >= 2 && normalizedQuestion.contains(token)) {
                score += 1;
            }
        }

        String summary = normalizeForMatch(doc.getSummary());
        if (!summary.isBlank()) {
            for (String token : splitMatchTokens(summary)) {
                if (token.length() >= 2 && normalizedQuestion.contains(token)) {
                    score += 1;
                }
            }
        }
        return score;
    }

    private List<String> splitMatchTokens(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(text.split("\\s+")).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toList());
    }

    private String normalizeForMatch(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("\\.(pdf|doc|docx|txt)$", " ")
                .replaceAll("[^\\p{IsHan}a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<KnowledgeDocumentEntity> loadCandidateDocuments(AiChatQueryRequest request,
                                                                 Integer kbId,
                                                                 SystemLoginUser loginUser) {
        if (request.getDocumentId() != null) {
            KnowledgeDocumentEntity doc = knowledgeDocumentService.getById(request.getDocumentId());
            return doc == null ? Collections.emptyList() : List.of(doc);
        }

        List<Long> importedDocumentIds = knowledgeIngestTaskService.list(
                        new LambdaQueryWrapper<KnowledgeIngestTaskEntity>()
                                .eq(KnowledgeIngestTaskEntity::getStatus,
                                        KnowledgeIngestTaskApplicationService.STATUS_SUCCESS)
                                .ne(KnowledgeIngestTaskEntity::getTaskType,
                                        KnowledgeIngestTaskApplicationService.TASK_TYPE_DELETE)
                                .eq(KnowledgeIngestTaskEntity::getPythonKbId, kbId)
                                .isNotNull(KnowledgeIngestTaskEntity::getPythonDocId)
                                .orderByDesc(KnowledgeIngestTaskEntity::getFinishedTime)
                                .orderByDesc(KnowledgeIngestTaskEntity::getTaskId))
                .stream()
                .map(KnowledgeIngestTaskEntity::getDocumentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (importedDocumentIds.isEmpty()) {
            log.warn("No successfully imported documents found for kbId={}", kbId);
            return Collections.emptyList();
        }

        LambdaQueryWrapper<KnowledgeDocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocumentEntity::getStatus, KnowledgeDocumentConstant.Status.PUBLISHED)
                .in(KnowledgeDocumentEntity::getDocumentId, importedDocumentIds);
        if (request.getCategoryId() != null) {
            wrapper.eq(KnowledgeDocumentEntity::getCategoryId, request.getCategoryId());
        }
        if (!loginUser.isAdmin()) {
            List<Long> accessibleDeptIds = accessibleDeptIds(loginUser);
            wrapper.and(visibleWrapper -> visibleWrapper
                    .eq(KnowledgeDocumentEntity::getCreatorId, loginUser.getUserId())
                    .or(sharedWrapper -> sharedWrapper
                            .eq(KnowledgeDocumentEntity::getStatus, KnowledgeDocumentConstant.Status.PUBLISHED)
                            .eq(KnowledgeDocumentEntity::getVisibility, KnowledgeDocumentConstant.Visibility.PUBLIC))
                    .or(sharedWrapper -> {
                        sharedWrapper.eq(KnowledgeDocumentEntity::getStatus, KnowledgeDocumentConstant.Status.PUBLISHED)
                                .eq(KnowledgeDocumentEntity::getVisibility, KnowledgeDocumentConstant.Visibility.DEPT);
                        if (accessibleDeptIds.isEmpty()) {
                            sharedWrapper.isNull(KnowledgeDocumentEntity::getDeptId);
                        } else {
                            sharedWrapper.in(KnowledgeDocumentEntity::getDeptId, accessibleDeptIds);
                        }
                    }));
        }
        return knowledgeDocumentService.list(wrapper);
    }

    private List<Long> accessibleDeptIds(SystemLoginUser loginUser) {
        if (loginUser == null || loginUser.getRoleInfo() == null) {
            return loginUser != null && loginUser.getDeptId() != null
                    ? List.of(loginUser.getDeptId())
                    : Collections.emptyList();
        }

        DataScopeEnum scope = loginUser.getRoleInfo().getDataScope();
        return switch (scope) {
            case ALL -> Collections.emptyList();
            case CUSTOM_DEFINE -> {
                Set<Long> deptIdSet = loginUser.getRoleInfo().getDeptIdSet();
                yield deptIdSet != null ? new ArrayList<>(deptIdSet) : Collections.emptyList();
            }
            case SINGLE_DEPT, ONLY_SELF -> loginUser.getDeptId() != null
                    ? List.of(loginUser.getDeptId())
                    : Collections.emptyList();
            case DEPT_TREE -> loginUser.getDeptId() != null
                    ? sysDeptService.getDeptAndChildrenIds(loginUser.getDeptId())
                    : Collections.emptyList();
        };
    }

    private boolean canCurrentUserView(KnowledgeDocumentEntity doc, SystemLoginUser loginUser) {
        if (loginUser.isAdmin()) {
            return true;
        }
        if (Objects.equals(doc.getCreatorId(), loginUser.getUserId())) {
            return true;
        }
        if (!Objects.equals(doc.getStatus(), KnowledgeDocumentConstant.Status.PUBLISHED)) {
            return false;
        }
        if (Objects.equals(doc.getVisibility(), KnowledgeDocumentConstant.Visibility.PUBLIC)) {
            return true;
        }
        if (Objects.equals(doc.getVisibility(), KnowledgeDocumentConstant.Visibility.DEPT)) {
            return canAccessDocumentDept(doc.getDeptId(), loginUser);
        }
        return false;
    }

    private boolean canAccessDocumentDept(Long documentDeptId, SystemLoginUser loginUser) {
        if (documentDeptId == null) {
            return false;
        }
        DataScopeEnum scope = loginUser.getRoleInfo().getDataScope();
        return switch (scope) {
            case ALL -> true;
            case CUSTOM_DEFINE -> {
                Set<Long> deptIdSet = loginUser.getRoleInfo().getDeptIdSet();
                yield deptIdSet != null && deptIdSet.contains(documentDeptId);
            }
            case SINGLE_DEPT -> Objects.equals(documentDeptId, loginUser.getDeptId());
            case DEPT_TREE -> Objects.equals(documentDeptId, loginUser.getDeptId())
                    || sysDeptService.isChildOfTheDept(loginUser.getDeptId(), documentDeptId);
            case ONLY_SELF -> false;
        };
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
                // 🔒 校验会话归属：非管理员不能继续他人的会话
                if (!loginUser.isAdmin() && !Objects.equals(session.getUserId(), loginUser.getUserId())) {
                    throw new IllegalArgumentException("You do not have permission to continue this session");
                }
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
            List<AiChatAnswerDTO.SourceInfo> finalSources,
            HttpServletResponse response) throws IOException {
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
                sendEvent(emitter, "token", token, response);
            }
            case "sources" -> {
                List<AiChatAnswerDTO.SourceInfo> sources = objectMapper.convertValue(
                        event.getData(),
                        new TypeReference<List<AiChatAnswerDTO.SourceInfo>>() {});
                finalSources.clear();
                finalSources.addAll(sources);
                sendEvent(emitter, "sources", sources, response);
            }
            case "conv_id" -> {
                Integer convId = objectMapper.convertValue(event.getData(), Integer.class);
                if (convId != null) {
                    session.setPythonConvId(convId);
                    aiChatSessionService.updateById(session);
                    sendEvent(emitter, "python_conv_id", convId, response);
                }
            }
            case "error" -> {
                String error = event.getData() != null
                        ? String.valueOf(event.getData())
                        : DEFAULT_STREAM_ERROR_MESSAGE;
                sendEvent(emitter, "error", error, response);
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

    private void sendEvent(SseEmitter emitter, String type, Object data,
                           HttpServletResponse response) throws IOException {
        String json = objectMapper.writeValueAsString(AiChatStreamEventDTO.builder()
                .type(type)
                .data(data)
                .build());
        emitter.send(SseEmitter.event().data(json));
        try {
            response.flushBuffer();
        } catch (IllegalStateException ignored) {
            // response already committed
        }
    }

    public SseEmitter streamAgentQuery(AiChatQueryRequest request, SystemLoginUser loginUser,
                                       HttpServletResponse response) {
        Integer kbId = resolveKbId(request);
        AiChatSessionEntity session = findOrCreateSession(request, loginUser);
        Integer pythonConvId = session.getPythonConvId();
        List<Integer> visibleDocIds = resolveVisiblePythonDocIds(request, kbId, loginUser);

        saveUserMessage(session, request, kbId, loginUser);

        PythonChatRequest pythonRequest = PythonChatRequest.builder()
                .kbId(kbId)
                .convId(pythonConvId)
                .question(request.getQuestion())
                .stream(true)
                .visibleDocIds(visibleDocIds)
                .build();

        SseEmitter emitter = new SseEmitter(-1L);

        Thread streamThread = new Thread(() -> {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginUser,
                    null,
                    loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);
            StringBuilder answerBuilder = new StringBuilder();
            List<AiChatAnswerDTO.SourceInfo> finalSources = new ArrayList<>();

            try {
                sendEvent(emitter, "conv_id", session.getSessionId(), response);
                pythonAiClient.streamAgentMessage(pythonRequest, line -> {
                    try {
                        handleAgentStreamLine(line, session, emitter, answerBuilder,
                                finalSources, response);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                updateSessionAfterQuery(session, request.getQuestion());
                saveAssistantStreamMessage(session, kbId, answerBuilder.toString(), finalSources);
                sendEvent(emitter, "done",
                        Map.of("answer", answerBuilder.toString(), "sources", finalSources), response);
                emitter.complete();
            } catch (Exception e) {
                log.error("AI Agent stream query failed: sessionId={}", session.getSessionId(), e);
                saveAssistantErrorMessage(session, kbId, e);
                try {
                    sendEvent(emitter, "error",
                            e.getMessage() != null ? e.getMessage() : DEFAULT_STREAM_ERROR_MESSAGE, response);
                } catch (IOException ignored) {
                    log.warn("Failed to send Agent stream error event");
                }
                emitter.complete();
            } finally {
                SecurityContextHolder.clearContext();
            }
        }, "ai-chat-agent-stream-" + session.getSessionId());
        streamThread.start();

        return emitter;
    }

    private void handleAgentStreamLine(String line,
            AiChatSessionEntity session,
            SseEmitter emitter,
            StringBuilder answerBuilder,
            List<AiChatAnswerDTO.SourceInfo> finalSources,
            HttpServletResponse response) throws IOException {
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
                sendEvent(emitter, "token", token, response);
            }
            case "sources" -> {
                List<AiChatAnswerDTO.SourceInfo> sources = objectMapper.convertValue(
                        event.getData(),
                        new TypeReference<List<AiChatAnswerDTO.SourceInfo>>() {});
                finalSources.clear();
                finalSources.addAll(sources);
                sendEvent(emitter, "sources", sources, response);
            }
            case "conv_id" -> {
                Integer convId = objectMapper.convertValue(event.getData(), Integer.class);
                if (convId != null) {
                    session.setPythonConvId(convId);
                    aiChatSessionService.updateById(session);
                    sendEvent(emitter, "python_conv_id", convId, response);
                }
            }
            case "error" -> {
                String error = event.getData() != null
                        ? String.valueOf(event.getData())
                        : DEFAULT_STREAM_ERROR_MESSAGE;
                sendEvent(emitter, "error", error, response);
            }
            // Agent-specific events: forward directly to frontend
            // done 也透传，因为 Python Agent 的 done 携带 steps 信息，
            // Java 自身的 done 不含 steps，不会覆盖前端的 agentTrace
            case "start", "step", "tool_call", "tool_result", "done" -> {
                sendEvent(emitter, event.getType(), event.getData(), response);
            }
            default -> {
                // Ignore other unknown events
            }
        }
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
