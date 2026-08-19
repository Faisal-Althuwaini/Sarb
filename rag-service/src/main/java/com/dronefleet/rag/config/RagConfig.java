package com.dronefleet.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    private static final String SYSTEM_PROMPT = """
            You are the AI assistant for the "Sarb" drone fleet operations
            platform. Your job is to answer operators' questions about the
            GACAR Part 107 unmanned aircraft regulation issued by the General
            Authority of Civil Aviation, and Sarb's internal Standard
            Operating Procedures (SOPs).

            Always:
            - Answer in English only, regardless of the retrieved source's language.
            - Rely exclusively on the retrieved passages in the attached context;
              never fabricate information that isn't present in it.
            - Cite the source of each piece of information at the end of your
              answer (document name, and section or page number if available
              in the source metadata).
            - If an internal SOP document conflicts with the official GACAR
              regulation, alert the user that the official regulation always
              takes precedence.
            """;

    private static final PromptTemplate CONTEXT_PROMPT_TEMPLATE = new PromptTemplate("""
            The following context information is retrieved from source documents:
            ---------------------
            {context}
            ---------------------
            Based solely on the context information above, answer the following
            question in English, citing the source.
            Question: {query}
            Answer:
            """);

    private static final PromptTemplate EMPTY_CONTEXT_PROMPT_TEMPLATE = new PromptTemplate("""
            No information relevant to the following question was found in the
            regulation documents or standard operating procedures. Clearly tell
            the user, in English, that no reliable answer is available in the
            available sources, without guessing an answer.
            Question: {query}
            """);

    @Bean
    public ChatClient chatClient(ChatModel chatModel, VectorStore vectorStore) {
        VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5)
                .topK(6)
                .build();

        ContextualQueryAugmenter augmenter = ContextualQueryAugmenter.builder()
                .promptTemplate(CONTEXT_PROMPT_TEMPLATE)
                .emptyContextPromptTemplate(EMPTY_CONTEXT_PROMPT_TEMPLATE)
                .allowEmptyContext(true)
                .build();

        RetrievalAugmentationAdvisor retrievalAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(augmenter)
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(retrievalAdvisor)
                .build();
    }
}
