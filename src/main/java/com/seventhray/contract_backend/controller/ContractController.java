package com.seventhray.contract_backend.controller;

import com.seventhray.contract_backend.dto.contract.ContractResponse;
import com.seventhray.contract_backend.dto.contract.StatusUpdateRequest;
import com.seventhray.contract_backend.model.ContractStatus;
import com.seventhray.contract_backend.model.ContractType;
// ✅ new added field
import com.seventhray.contract_backend.model.RiskLevel;
import com.seventhray.contract_backend.model.User;
import com.seventhray.contract_backend.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    /**
     * 1. UPLOAD CONTRACT
     * Accepts PDF/Word document + metadata. Parses and stores text in PostgreSQL.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractResponse> uploadContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("clientName") String clientName,
            //  new added param
            @RequestParam(value = "counterpartySignatory", required = false) String counterpartySignatory,
            //  new added param
            @RequestParam(value = "riskLevel", required = false) RiskLevel riskLevel,
            @RequestParam(value = "contractType", required = false) ContractType contractType,
            @RequestParam(value = "value", required = false) BigDecimal value,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User currentUser
    ) {
        ContractResponse response = contractService.uploadContract(
                file, title, clientName, counterpartySignatory, riskLevel, contractType, value, startDate, endDate, currentUser
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 2. LIST CONTRACTS
     * Returns all contracts for the logged-in user. Supports optional search and status filter.
     */
    @GetMapping
    public ResponseEntity<List<ContractResponse>> getContracts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) ContractStatus status,
            @AuthenticationPrincipal User currentUser
    ) {
        List<ContractResponse> contracts = contractService.getUserContracts(currentUser, search, status);
        return ResponseEntity.ok(contracts);
    }

    /**
     * 3. GET CONTRACT BY ID
     * Returns single contract with full parsed text.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponse> getContractById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(contractService.getContractById(id, currentUser));
    }

    /**
     * 4. UPDATE STATUS (DRAFT -> UNDER_REVIEW -> APPROVED)
     * Enforces the legal lifecycle state machine.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ContractResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        ContractResponse updated = contractService.updateStatus(id, request.getStatus(), currentUser);
        return ResponseEntity.ok(updated);
    }

    /**
     * 5. DELETE CONTRACT
     * Deletes DB record and removes file from disk.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteContract(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        contractService.deleteContract(id, currentUser);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Contract successfully deleted with id: " + id);
        return ResponseEntity.ok(response);
    }

    /**
     * 6. DOWNLOAD CONTRACT FILE
     * Streams binary file to client with Content-Disposition attachment header.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        Resource resource = contractService.getContractFileAsResource(id, currentUser);
        String filename = contractService.getContractFilename(id, currentUser);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}