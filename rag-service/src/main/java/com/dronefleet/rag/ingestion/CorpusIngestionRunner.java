package com.dronefleet.rag.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Runs once at startup: ingests the regulation PDF(s) and SOP text files into
// pgvector, skipping entirely if the table already has rows from a prior run
// - re-ingesting on every restart would just duplicate every chunk.
@Slf4j
@RequiredArgsConstructor
@Component
public class CorpusIngestionRunner implements ApplicationRunner {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder().build();

    @Value("${sarb.rag.corpus.regulations-location}")
    private Resource[] regulationResources;

    @Value("${sarb.rag.corpus.sops-location}")
    private Resource[] sopResources;

    @Override
    public void run(ApplicationArguments args) {
        Integer existingRows = jdbcTemplate.queryForObject("SELECT count(*) FROM rag.vector_store", Integer.class);
        if (existingRows != null && existingRows > 0) {
            log.info("Corpus already ingested ({} chunks) - skipping ingestion", existingRows);
            return;
        }

        List<Document> documents = new ArrayList<>();
        for (Resource resource : regulationResources) {
            documents.addAll(readRegulation(resource));
        }
        for (Resource resource : sopResources) {
            documents.addAll(readSop(resource));
        }

        List<Document> chunks = splitter.apply(documents);
        vectorStore.add(chunks);
        log.info("Ingested {} source document(s) into {} chunks", documents.size(), chunks.size());
    }

    private List<Document> readRegulation(Resource resource) {
        List<Document> pages = new PagePdfDocumentReader(resource).get();
        return pages.stream().map(doc -> doc.mutate().metadata("sourceType", "regulation").build()).toList();
    }

    private List<Document> readSop(Resource resource) {
        TextReader reader = new TextReader(resource);
        reader.getCustomMetadata().put("sourceType", "sop");
        return reader.get();
    }
}
