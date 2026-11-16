package com.seventhray.contract_backend.dto.qa;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Payload for asking a question about a specific contract or across all portfolio contracts.
 */
@Data
public class QuestionRequest {

    @NotBlank(message = "Question or search prompt cannot be empty")
    private String question;
}
