package com.seventhray.contract_backend.dto.contract;

import com.seventhray.contract_backend.model.Contract;
import com.seventhray.contract_backend.model.ContractStatus;
import com.seventhray.contract_backend.model.ContractType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Clean data transfer object returned to client when fetching contract(s).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponse {

    private Long id;
    private String title;
    private String clientName;
    private ContractType contractType;
    private ContractStatus status;
    private String fileName;
    private Long fileSize;
    private String parsedTextPreview; // Short excerpt for table views
    private String fullParsedText;    // Full text (only included on detail requests)
    private BigDecimal value;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Helper method to map JPA Entity into clean DTO.
     */
    public static ContractResponse fromEntity(Contract contract, boolean includeFullText) {
        String preview = "";
        if (contract.getParsedText() != null && !contract.getParsedText().isEmpty()) {
            preview = contract.getParsedText().length() > 300
                    ? contract.getParsedText().substring(0, 300) + "..."
                    : contract.getParsedText();
        }

        return ContractResponse.builder()
                .id(contract.getId())
                .title(contract.getTitle())
                .clientName(contract.getClientName())
                .contractType(contract.getContractType())
                .status(contract.getStatus())
                .fileName(contract.getFileName())
                .fileSize(contract.getFileSize())
                .parsedTextPreview(preview)
                .fullParsedText(includeFullText ? contract.getParsedText() : null)
                .value(contract.getValue())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }
}