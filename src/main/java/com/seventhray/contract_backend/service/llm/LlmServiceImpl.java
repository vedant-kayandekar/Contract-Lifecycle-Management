package com.seventhray.contract_backend.service.llm;

import com.seventhray.contract_backend.dto.llm.LlmChatRequest;
import com.seventhray.contract_backend.dto.llm.LlmChatResponse;
import com.seventhray.contract_backend.model.ContractChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Universal OpenAI-compatible LLM Service.
 * Works seamlessly across:
 *  - Local Dev: FreeLLMAPI (http://127.0.0.1:31415/v1)
 *  - Production Cloud: Google Gemini (https://generativelanguage.googleapis.com/v1beta/openai/)
 *  - Production Cloud: NVIDIA NIM (https://integrate.api.nvidia.com/v1)
 *  - Production Cloud: Groq (https://api.groq.com/openai/v1)
 */
@Service
public class LlmServiceImpl implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmServiceImpl.class);

    private final boolean enabled;
    private final String model;
    private final Double temperature;
    private final RestClient restClient;

    public LlmServiceImpl(
            @Value("${app.llm.enabled:true}") boolean enabled,
            @Value("${app.llm.base-url:http://127.0.0.1:31415/v1}") String baseUrl,
            @Value("${app.llm.api-key:freellmapi-18d37e1c9c0b0bfa7b5f370e248f57ba60401e827cdbbc6e}") String apiKey,
            @Value("${app.llm.model:auto:fast}") String model,
            @Value("${app.llm.temperature:0.2}") Double temperature
    ) {
        this.enabled = enabled;
        this.model = model;
        this.temperature = temperature;

        // Spring 6 RestClient: Pre-configured with Base URL & Bearer Auth
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        log.info("LlmService initialized -> BaseURL: [{}], Model: [{}], Enabled: [{}]", baseUrl, model, enabled);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String generateContractAnswer(
            String title,
            String clientName,
            List<String> clauses,
            List<ContractChat> chatHistory,
            String question
    ) {
        if (!enabled) return null;

        try {
            List<LlmChatRequest.Message> messages = new ArrayList<>();

            // 1. System Prompt (Senior Legal Counsel persona + zero hallucination rule)
            messages.add(new LlmChatRequest.Message("system",
                    "You are the Senior Legal AI for 'Contract Story' (Seventh Ray LegalTech). " +
                    "Answer questions concisely and factually based strictly on the provided contract excerpts. " +
                    "Always cite the clause names or numbers. If not mentioned in the excerpts, say so directly."));

            // 2. Rolling Context (chronological order)
            appendHistory(messages, chatHistory);

            // 3. User Prompt with injected RAG context
            String prompt = String.format("""
                    CONTRACT: %s (Counterparty: %s)
                    EXCERPTS:
                    %s
                    
                    QUESTION: %s
                    Answer with clause citations:""",
                    title,
                    clientName,
                    (clauses == null || clauses.isEmpty()) ? "None found." : String.join("\n\n", clauses),
                    question.trim()
            );
            messages.add(new LlmChatRequest.Message("user", prompt));

            return callLlm(messages);

        } catch (Exception e) {
            log.warn("LLM call failed for '{}': {}. Falling back to heuristic answer.", title, e.getMessage());
            return null; // Triggers safe, instant heuristic fallback
        }
    }

    @Override
    public String generateGlobalAnswer(
            List<String> portfolioFindings,
            List<ContractChat> chatHistory,
            String question,
            int contractCount
    ) {
        if (!enabled) return null;

        try {
            List<LlmChatRequest.Message> messages = new ArrayList<>();

            messages.add(new LlmChatRequest.Message("system",
                    "You are the Senior Legal AI for 'Contract Story'. " +
                    "Synthesize contract provisions across the portfolio and clearly cite which agreement each term belongs to."));

            appendHistory(messages, chatHistory);

            String prompt = String.format("""
                    PORTFOLIO: %d contracts analyzed.
                    PROVISIONS FOUND:
                    %s
                    
                    QUESTION: %s
                    Synthesize an executive summary across agreements:""",
                    contractCount,
                    (portfolioFindings == null || portfolioFindings.isEmpty()) ? "None found." : String.join("\n\n", portfolioFindings),
                    question.trim()
            );
            messages.add(new LlmChatRequest.Message("user", prompt));

            return callLlm(messages);

        } catch (Exception e) {
            log.warn("Global LLM call failed: {}. Falling back to heuristic answer.", e.getMessage());
            return null;
        }
    }

    private String callLlm(List<LlmChatRequest.Message> messages) {
        LlmChatRequest request = LlmChatRequest.builder()
                .model(model)
                .temperature(temperature)
                .messages(messages)
                .build();

        LlmChatResponse response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(LlmChatResponse.class);

        return (response != null) ? response.getFirstChoiceText() : null;
    }

    private void appendHistory(List<LlmChatRequest.Message> messages, List<ContractChat> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) return;

        // Reverse newest-first SQL list to restore chronological conversational order
        List<ContractChat> chronological = new ArrayList<>(chatHistory);
        Collections.reverse(chronological);

        for (ContractChat chat : chronological) {
            String role = "USER".equalsIgnoreCase(chat.getRole()) ? "user" : "assistant";
            messages.add(new LlmChatRequest.Message(role, chat.getMessage()));
        }
    }
}
