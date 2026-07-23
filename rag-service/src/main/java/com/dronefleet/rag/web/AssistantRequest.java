package com.dronefleet.rag.web;

import jakarta.validation.constraints.NotBlank;

public record AssistantRequest(@NotBlank String question) {
}
