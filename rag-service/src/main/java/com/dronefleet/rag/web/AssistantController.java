package com.dronefleet.rag.web;

import com.dronefleet.rag.assistant.AssistantAnswer;
import com.dronefleet.rag.assistant.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/ask")
    public AssistantAnswer ask(@Valid @RequestBody AssistantRequest request) {
        return assistantService.ask(request.question());
    }
}
