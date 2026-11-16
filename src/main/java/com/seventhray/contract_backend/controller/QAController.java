package com.seventhray.contract_backend.controller;

import com.seventhray.contract_backend.dto.contract.SearchResponse;
import com.seventhray.contract_backend.dto.qa.ChatMessageResponse;
import com.seventhray.contract_backend.dto.qa.QuestionRequest;
import com.seventhray.contract_backend.dto.qa.QuestionResponse;
import com.seventhray.contract_backend.model.User;
import com.seventhray.contract_backend.service.QAService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Contract Q&A, Keyword Clause Search,
 * and Conversational Context across single or multiple contracts.
 */
@RestController
@RequestMapping("/api/contracts")
public class QAController {

    private final QAService qaService;

    public QAController(QAService qaService) {
        this.qaService = qaService;
    }

    /**
     * Ask a question about a specific contract.
     * Analyzes parsed text, scores relevant clauses, logs interaction into chat history.
     */
    @PostMapping("/{id}/ask")
    public ResponseEntity<QuestionResponse> askContract(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        QuestionResponse response = qaService.askContract(id, request.getQuestion(), currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Search specific keywords inside a contract's parsed text,
     * returning all matching paragraph chunks with snippet highlights.
     */
    @PostMapping("/{id}/search")
    public ResponseEntity<SearchResponse> searchContract(
            @PathVariable Long id,
            @RequestParam("keyword") String keyword,
            @AuthenticationPrincipal User currentUser
    ) {
        SearchResponse response = qaService.searchContract(id, keyword, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the full conversational Q&A history for a specific contract.
     */
    @GetMapping("/{id}/chat-history")
    public ResponseEntity<List<ChatMessageResponse>> getContractChatHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        List<ChatMessageResponse> history = qaService.getContractChatHistory(id, currentUser);
        return ResponseEntity.ok(history);
    }

    /**
     * Ask a global question across ALL user's parsed contracts combined.
     */
    @PostMapping("/global/ask")
    public ResponseEntity<QuestionResponse> askGlobal(
            @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        QuestionResponse response = qaService.askGlobal(request.getQuestion(), currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the global portfolio chat history.
     */
    @GetMapping("/global/chat-history")
    public ResponseEntity<List<ChatMessageResponse>> getGlobalChatHistory(
            @AuthenticationPrincipal User currentUser
    ) {
        List<ChatMessageResponse> history = qaService.getGlobalChatHistory(currentUser);
        return ResponseEntity.ok(history);
    }
}
