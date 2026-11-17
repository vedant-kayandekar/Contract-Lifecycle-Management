package com.seventhray.contract_backend.dto.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Standard OpenAI-compatible Chat Completion response structure.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmChatResponse {

    private String id;
    private List<Choice> choices;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private int index;
        private Message message;
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
    }

    /**
     * Helper to safely extract the assistant's reply text from the first choice.
     */
    public String getFirstChoiceText() {
        if (choices != null && !choices.isEmpty()) {
            Choice first = choices.get(0);
            if (first.getMessage() != null && first.getMessage().getContent() != null) {
                return first.getMessage().getContent().trim();
            }
        }
        return null;
    }
}
