package com.dronefleet.rag.assistant;

public record Citation(String source, String sourceType, Integer page, Double score, String excerpt) {
}
