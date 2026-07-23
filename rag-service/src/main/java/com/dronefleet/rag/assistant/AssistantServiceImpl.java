package com.dronefleet.rag.assistant;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AssistantServiceImpl implements AssistantService {

    private static final int EXCERPT_LENGTH = 220;

    private final ChatClient chatClient;

    @Override
    public AssistantAnswer ask(String question) {
        ChatClientResponse response = chatClient.prompt().user(question).call().chatClientResponse();

        String answer = response.chatResponse().getResult().getOutput().getText();
        List<Citation> citations = extractCitations(response.context());
        return new AssistantAnswer(answer, citations);
    }

    @SuppressWarnings("unchecked")
    private List<Citation> extractCitations(Map<String, Object> context) {
        Object retrieved = context.get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
        if (!(retrieved instanceof List<?> documents)) {
            return List.of();
        }
        return ((List<Document>) documents).stream().map(this::toCitation).toList();
    }

    private Citation toCitation(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        String source = (String) metadata.getOrDefault("file_name", metadata.get("source"));
        String sourceType = (String) metadata.get("sourceType");
        Integer page = (Integer) metadata.get("page_number");
        String text = document.getText();
        String excerpt = text == null || text.length() <= EXCERPT_LENGTH ? text : text.substring(0, EXCERPT_LENGTH) + "…";
        return new Citation(source, sourceType, page, document.getScore(), excerpt);
    }
}
