package com.dronefleet.rag.assistant;

import java.util.List;

public record AssistantAnswer(String answer, List<Citation> citations) {
}
