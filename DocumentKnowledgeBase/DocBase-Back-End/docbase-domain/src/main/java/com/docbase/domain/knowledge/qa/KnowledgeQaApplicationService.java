package com.docbase.domain.knowledge.qa;

import com.docbase.domain.knowledge.qa.command.KnowledgeQaAskCommand;
import com.docbase.domain.knowledge.qa.dto.KnowledgeQaAnswerDTO;
import com.docbase.domain.knowledge.qa.dto.KnowledgeQaHealthDTO;
import java.util.Collections;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeQaApplicationService {

    public KnowledgeQaHealthDTO health() {
        KnowledgeQaHealthDTO dto = new KnowledgeQaHealthDTO();
        dto.setService("knowledge-qa");
        dto.setStatus("READY_FOR_INTEGRATION");
        dto.setProvider("python-rag-placeholder");
        dto.setMessage("后续可在这里对接 Python 知识库问答服务。");
        return dto;
    }

    public KnowledgeQaAnswerDTO ask(KnowledgeQaAskCommand command) {
        KnowledgeQaAnswerDTO dto = new KnowledgeQaAnswerDTO();
        dto.setQuestion(command.getQuestion());
        dto.setProvider("python-rag-placeholder");
        dto.setStatus("NOT_IMPLEMENTED");
        dto.setAnswer("当前为知识库问答接口骨架，后续可接入 Python 检索增强问答服务。");
        dto.setCitations(Collections.emptyList());
        return dto;
    }
}
