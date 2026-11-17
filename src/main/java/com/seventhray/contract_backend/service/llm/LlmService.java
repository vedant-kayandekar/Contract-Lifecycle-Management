package com.seventhray.contract_backend.service.llm;

import com.seventhray.contract_backend.model.ContractChat;

import java.util.List;

/**
 * Service interface for communicating with external LLM gateways (FreeLLMAPI, OpenAI, Gemini).
 */
public interface LlmService {

    /**
     * Checks if the LLM provider is currently enabled in application.properties.
     */
    boolean isEnabled();

    /**
     * Generate an AI legal analysis for a specific contract.
     * Combines system instructions, contract metadata, cited clauses, and rolling history.
     */
    String generateContractAnswer(
            String contractTitle,
            String clientName,
            List<String> relevantClauses,
            List<ContractChat> chatHistory,
            String question
    );

    /**
     * Generate an enterprise AI summary across multiple contracts in the portfolio.
     */
    String generateGlobalAnswer(
            List<String> portfolioFindings,
            List<ContractChat> chatHistory,
            String question,
            int contractCount
    );
}
