package com.seventhray.contract_backend.service;

import com.seventhray.contract_backend.dto.contract.SearchResponse;
import com.seventhray.contract_backend.dto.qa.ChatMessageResponse;
import com.seventhray.contract_backend.dto.qa.QuestionResponse;
import com.seventhray.contract_backend.model.User;

import java.util.List;

/**
 * Service providing contract-specific and global Q&A, keyword search,
 * and conversational context tracking.
 */
public interface QAService {

    /**
     * Ask a question about a single contract by ID.
     */
    QuestionResponse askContract(Long contractId, String question, User user);

    /**
     * Keyword search within a single contract's parsed text.
     */
    SearchResponse searchContract(Long contractId, String keyword, User user);

    /**
     * Get the full chat message history for a specific contract.
     */
    List<ChatMessageResponse> getContractChatHistory(Long contractId, User user);

    /**
     * Ask a global question across ALL contracts belonging to the user.
     */
    QuestionResponse askGlobal(String question, User user);

    /**
     * Get the global portfolio chat history.
     */
    List<ChatMessageResponse> getGlobalChatHistory(User user);
}
