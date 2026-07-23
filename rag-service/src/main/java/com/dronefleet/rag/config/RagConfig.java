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
            أنت المساعد الذكي لمنصة "سرب" لعمليات أسطول الطائرات المسيّرة. مهمتك
            الإجابة عن أسئلة المشغّلين حول لائحة الطيران المسيّر GACAR الجزء 107
            الصادرة عن الهيئة العامة للطيران المدني، وإجراءات التشغيل الداخلية
            الموحدة (SOP) لمنصة سرب.

            التزم دائمًا بما يلي:
            - أجب باللغة العربية الفصحى فقط، بصرف النظر عن لغة المصدر المسترجع.
            - استند حصريًا إلى المقاطع المسترجعة ضمن السياق المرفق؛ لا تختلق أي
              معلومة غير واردة فيه.
            - اذكر في نهاية إجابتك مصدر كل معلومة (اسم الوثيقة، ورقم القسم أو
              الصفحة إن توفر في بيانات المصدر).
            - إذا تعارضت وثيقة SOP الداخلية مع لائحة GACAR الرسمية، نبّه المستخدم
              إلى أن الأسبقية دائمًا للائحة الرسمية.
            """;

    private static final PromptTemplate CONTEXT_PROMPT_TEMPLATE = new PromptTemplate("""
            معلومات السياق التالية مسترجعة من وثائق المصدر:
            ---------------------
            {context}
            ---------------------
            بالاعتماد على معلومات السياق أعلاه فقط، أجب عن السؤال التالي باللغة
            العربية مع ذكر المصدر.
            السؤال: {query}
            الإجابة:
            """);

    private static final PromptTemplate EMPTY_CONTEXT_PROMPT_TEMPLATE = new PromptTemplate("""
            لم يتم العثور على معلومات ذات صلة بالسؤال التالي ضمن وثائق اللائحة أو
            إجراءات التشغيل الموحدة. أخبر المستخدم بوضوح، باللغة العربية، أنه لا
            تتوفر إجابة موثوقة في المصادر المتاحة، دون تخمين إجابة.
            السؤال: {query}
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
