package com.seventhray.contract_backend.dto.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response returned when doing pure keyword searches inside contract text.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private Long contractId;
    private String contractTitle;
    private String clientName;
    private String keyword;
    private int matchCount;
    private List<String> matchingSnippets;
}