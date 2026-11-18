package com.seventhray.contract_backend.repository;

import com.seventhray.contract_backend.model.Contract;
import com.seventhray.contract_backend.model.ContractStatus;
// new added field
import com.seventhray.contract_backend.model.RiskLevel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    // 1. Get all contracts for a specific user, newest first
    List<Contract> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 2. Security check: fetch contract ONLY if it belongs to this user ID!
    // If user 1 tries to access contract of user 2, this returns Optional.empty()
    Optional<Contract> findByIdAndUserId(Long id, Long userId);

    // 3. Filter contracts by user and status (e.g. show all my DRAFT contracts)
    List<Contract> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ContractStatus status);

    // 4. Custom search: searches keyword across title, client name, OR parsed text
    @Query("SELECT c FROM Contract c WHERE c.user.id = :userId AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(c.clientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(c.parsedText) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Contract> searchContracts(@Param("userId") Long userId, @Param("keyword") String keyword);

    // 5. Global Portfolio query: fetches all contracts of this user that have parsed text
    @Query("SELECT c FROM Contract c WHERE c.user.id = :userId AND c.parsedText IS NOT NULL")
    List<Contract> findAllWithParsedTextByUserId(@Param("userId") Long userId);

    // 6. new Filter contracts by counterpartySignatory name
    @Query("SELECT c FROM Contract c WHERE c.user.id = :userId AND " +
           "LOWER(c.counterpartySignatory) LIKE LOWER(CONCAT('%', :signatory, '%'))")
    List<Contract> findByUserIdAndSignatoryContaining(
            @Param("userId") Long userId,
            @Param("signatory") String signatory
    );

    // 7. new Filter contracts by risk level
    List<Contract> findByUserIdAndRiskLevelOrderByCreatedAtDesc(Long userId, RiskLevel riskLevel);
}