package com.seventhray.contract_backend.dto.contract;

import com.seventhray.contract_backend.model.ContractStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload sent in PATCH /api/contracts/{id}/status
 */
@Data
public class StatusUpdateRequest {

    @NotNull(message = "Status is required (DRAFT, UNDER_REVIEW, APPROVED, REJECTED)")
    private ContractStatus status;
}