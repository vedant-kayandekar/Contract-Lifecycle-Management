package com.seventhray.contract_backend.service.impl;

import com.seventhray.contract_backend.dto.contract.SearchResponse;
import com.seventhray.contract_backend.dto.qa.ChatMessageResponse;
import com.seventhray.contract_backend.dto.qa.QuestionResponse;
import com.seventhray.contract_backend.exception.BadRequestException;
import com.seventhray.contract_backend.exception.ResourceNotFoundException;
import com.seventhray.contract_backend.model.Contract;
import com.seventhray.contract_backend.model.ContractChat;
import com.seventhray.contract_backend.model.User;
import com.seventhray.contract_backend.repository.ContractChatRepository;
import com.seventhray.contract_backend.repository.ContractRepository;
import com.seventhray.contract_backend.service.QAService;
import com.seventhray.contract_backend.service.llm.LlmService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of QAService with smart legal clause extraction,
 * multi-contract search, and rolling 10-message conversational memory.
 */
@Service
@Transactional
public class QAServiceImpl implements QAService {

    private final ContractRepository contractRepository;
    private final ContractChatRepository chatRepository;
    private final LlmService llmService;

    // Filter out common English filler words to isolate meaningful legal concepts
    private static final Set<String> STOP_WORDS = Set.of(
            "what", "when", "where", "which", "who", "whom", "whose", "why", "how",
            "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "can", "could", "will", "would", "shall", "should",
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "at", "by",
            "for", "with", "about", "against", "between", "into", "through", "during",
            "before", "after", "above", "below", "to", "from", "up", "down", "in", "out",
            "on", "off", "over", "under", "again", "further", "contract", "agreement", "clause"
    );

    public QAServiceImpl(
            ContractRepository contractRepository,
            ContractChatRepository chatRepository,
            LlmService llmService
    ) {
        this.contractRepository = contractRepository;
        this.chatRepository = chatRepository;
        this.llmService = llmService;
    }

    @Override
    public QuestionResponse askContract(Long contractId, String question, User user) {
        // 1. Verify contract exists and belongs to the authenticated user
        Contract contract = contractRepository.findByIdAndUserId(contractId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with id: " + contractId));

        if (contract.getParsedText() == null || contract.getParsedText().trim().isEmpty()) {
            throw new BadRequestException("This contract does not contain any parsed text to analyze.");
        }

        // 2. Fetch rolling context (last 10 messages)
        List<ContractChat> last10Chats = chatRepository.findTop10ByContractIdAndUserIdOrderByCreatedAtDesc(contractId, user.getId());
        int contextSize = last10Chats.size();

        // 3. Perform intelligent clause extraction
        List<String> keywords = extractKeywords(question);
        List<String> paragraphs = splitIntoParagraphs(contract.getParsedText());
        List<ScoredParagraph> scored = scoreParagraphs(paragraphs, keywords);

        List<String> matchingClauses = scored.stream()
                .filter(sp -> sp.score > 0)
                .limit(3)
                .map(sp -> sp.paragraph)
                .collect(Collectors.toList());

        // 4. Generate structured answer via LLM (FreeLLMAPI) with graceful heuristic fallback
        String answer = null;
        boolean aiGenerated = false;

        if (llmService.isEnabled()) {
            answer = llmService.generateContractAnswer(
                    contract.getTitle(),
                    contract.getClientName(),
                    matchingClauses,
                    last10Chats,
                    question
            );
            if (answer != null && !answer.trim().isEmpty()) {
                aiGenerated = true;
            }
        }

        // Heuristic fallback if LLM is disabled or fails
        if (answer == null) {
            if (matchingClauses.isEmpty()) {
                answer = "I searched through the text of \"" + contract.getTitle() + 
                         "\", but could not find explicit clauses discussing: \"" + question.trim() + 
                         "\". Please verify the term or check if this document contains an appendix/addendum.";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Based on the contract \"").append(contract.getTitle()).append("\", here are the relevant terms found:\n\n");
                for (int i = 0; i < matchingClauses.size(); i++) {
                    sb.append("• [Clause Reference ").append(i + 1).append("]:\n\"")
                      .append(matchingClauses.get(i)).append("\"\n\n");
                }
                sb.append("Summary: The contract addresses this matter in the sections highlighted above.");
                answer = sb.toString();
            }
        }

        // 5. Save to chat history (both user question and assistant answer)
        saveChatMessage(contract, user, "USER", question.trim());
        saveChatMessage(contract, user, "ASSISTANT", answer);

        return QuestionResponse.builder()
                .question(question.trim())
                .answer(answer)
                .matchingClauses(matchingClauses)
                .aiGenerated(aiGenerated)
                .contractId(contract.getId())
                .contractTitle(contract.getTitle())
                .contextHistorySize(contextSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse searchContract(Long contractId, String keyword, User user) {
        Contract contract = contractRepository.findByIdAndUserId(contractId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with id: " + contractId));

        if (contract.getParsedText() == null || contract.getParsedText().trim().isEmpty()) {
            return SearchResponse.builder()
                    .contractId(contract.getId())
                    .contractTitle(contract.getTitle())
                    .clientName(contract.getClientName())
                    .keyword(keyword)
                    .matchCount(0)
                    .matchingSnippets(Collections.emptyList())
                    .build();
        }

        List<String> paragraphs = splitIntoParagraphs(contract.getParsedText());
        String lowerKeyword = keyword.toLowerCase().trim();

        List<String> matchingSnippets = paragraphs.stream()
                .filter(p -> p.toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .contractId(contract.getId())
                .contractTitle(contract.getTitle())
                .clientName(contract.getClientName())
                .keyword(keyword)
                .matchCount(matchingSnippets.size())
                .matchingSnippets(matchingSnippets)
                .build();
    }

    @Override
    public QuestionResponse askGlobal(String question, User user) {
        List<Contract> userContracts = contractRepository.findAllWithParsedTextByUserId(user.getId());

        if (userContracts.isEmpty()) {
            throw new BadRequestException("You do not have any contracts with parsed text uploaded yet.");
        }

        // 1. Fetch rolling global chat history (last 10 messages)
        List<ContractChat> last10Global = chatRepository.findTop10ByContractIsNullAndUserIdOrderByCreatedAtDesc(user.getId());
        int contextSize = last10Global.size();

        // 2. Search across all contracts in the user's portfolio
        List<String> keywords = extractKeywords(question);
        List<String> portfolioFindings = new ArrayList<>();

        for (Contract c : userContracts) {
            List<String> paragraphs = splitIntoParagraphs(c.getParsedText());
            List<ScoredParagraph> scored = scoreParagraphs(paragraphs, keywords);

            List<String> topMatches = scored.stream()
                    .filter(sp -> sp.score > 0)
                    .limit(2)
                    .map(sp -> "[" + c.getTitle() + " - " + c.getClientName() + "]:\n" + sp.paragraph)
                    .toList();

            portfolioFindings.addAll(topMatches);
        }

        // 3. Synthesize answer across portfolio via LLM (with heuristic fallback)
        String answer = null;
        boolean aiGenerated = false;

        if (llmService.isEnabled()) {
            answer = llmService.generateGlobalAnswer(
                    portfolioFindings,
                    last10Global,
                    question,
                    userContracts.size()
            );
            if (answer != null && !answer.trim().isEmpty()) {
                aiGenerated = true;
            }
        }

        if (answer == null) {
            if (portfolioFindings.isEmpty()) {
                answer = "I searched across all " + userContracts.size() + 
                         " contracts in your portfolio, but found no matching provisions for: \"" + question.trim() + "\".";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Searched across ").append(userContracts.size()).append(" contracts in your portfolio. ");
                sb.append("Here are the provisions matching your inquiry:\n\n");
                for (String finding : portfolioFindings.stream().limit(5).toList()) {
                    sb.append("• ").append(finding).append("\n\n");
                }
                answer = sb.toString();
            }
        }

        // 4. Save to global chat history (contract is null)
        saveChatMessage(null, user, "USER", question.trim());
        saveChatMessage(null, user, "ASSISTANT", answer);

        return QuestionResponse.builder()
                .question(question.trim())
                .answer(answer)
                .matchingClauses(portfolioFindings)
                .aiGenerated(aiGenerated)
                .contractId(null)
                .contractTitle("All Portfolio Contracts (" + userContracts.size() + ")")
                .contextHistorySize(contextSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getContractChatHistory(Long contractId, User user) {
        contractRepository.findByIdAndUserId(contractId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with id: " + contractId));

        return chatRepository.findByContractIdAndUserIdOrderByCreatedAtAsc(contractId, user.getId())
                .stream()
                .map(ChatMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getGlobalChatHistory(User user) {
        return chatRepository.findByContractIsNullAndUserIdOrderByCreatedAtAsc(user.getId())
                .stream()
                .map(ChatMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void saveChatMessage(Contract contract, User user, String role, String message) {
        ContractChat chat = ContractChat.builder()
                .contract(contract)
                .user(user)
                .role(role)
                .message(message)
                .build();
        chatRepository.save(chat);
    }

    private List<String> extractKeywords(String question) {
        String cleaned = question.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = cleaned.split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() > 2 && !STOP_WORDS.contains(token)) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private List<String> splitIntoParagraphs(String text) {
        String[] raw = text.split("\\n\\s*\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String p : raw) {
            String trimmed = p.trim();
            if (trimmed.length() > 25) { // Filter out header noise / line numbers
                paragraphs.add(trimmed);
            }
        }
        return paragraphs;
    }

    private List<ScoredParagraph> scoreParagraphs(List<String> paragraphs, List<String> keywords) {
        List<ScoredParagraph> scored = new ArrayList<>();
        for (String p : paragraphs) {
            String lower = p.toLowerCase();
            int score = 0;
            for (String kw : keywords) {
                if (lower.contains(kw)) {
                    score += 10;
                }
            }
            scored.add(new ScoredParagraph(p, score));
        }
        scored.sort((a, b) -> Integer.compare(b.score, a.score));
        return scored;
    }

    private static class ScoredParagraph {
        String paragraph;
        int score;

        ScoredParagraph(String paragraph, int score) {
            this.paragraph = paragraph;
            this.score = score;
        }
    }
}
