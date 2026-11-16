package com.seventhray.contract_backend.dto.qa;

import com.seventhray.contract_backend.model.ContractChat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing an individual chat message in the 10-message conversational history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private String role; // "USER" or "ASSISTANT"
    private String message;
    private LocalDateTime createdAt;

    /**
     * Converts a database JPA entity (ContractChat) into a lightweight client-facing DTO.
     */
    public static ChatMessageResponse fromEntity(ContractChat chat) {
        return ChatMessageResponse.builder()
                .id(chat.getId())
                .role(chat.getRole())
                .message(chat.getMessage())
                .createdAt(chat.getCreatedAt())
                .build();
    }
}
