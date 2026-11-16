package com.seventhray.contract_backend.repository;

import com.seventhray.contract_backend.model.ContractChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractChatRepository extends JpaRepository<ContractChat, Long> {

    // 1. Fetch exactly the last 10 messages for a single contract (context window)
    List<ContractChat> findTop10ByContractIdAndUserIdOrderByCreatedAtDesc(Long contractId, Long userId);

    // 2. Fetch entire chat history for a contract (ordered chronologically)
    List<ContractChat> findByContractIdAndUserIdOrderByCreatedAtAsc(Long contractId, Long userId);

    // 3. Fetch the last 10 messages for global portfolio chat (where contract_id IS NULL)
    List<ContractChat> findTop10ByContractIsNullAndUserIdOrderByCreatedAtDesc(Long userId);

    // 4. Fetch entire global chat history
    List<ContractChat> findByContractIsNullAndUserIdOrderByCreatedAtAsc(Long userId);
}