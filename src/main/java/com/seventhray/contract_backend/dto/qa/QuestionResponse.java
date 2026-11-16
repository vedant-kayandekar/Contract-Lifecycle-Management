package com.seventhray.contract_backend.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured response returned from contract-specific and global Q&A endpoints.
 * Includes the answer, cited clauses, contract metadata, and context window size.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private String question;
    private String answer;
    private List<String> matchingClauses;
    private boolean aiGenerated;
    private Long contractId;
    private String contractTitle;
    private int contextHistorySize; // Count of rolling chat context messages utilized
}
