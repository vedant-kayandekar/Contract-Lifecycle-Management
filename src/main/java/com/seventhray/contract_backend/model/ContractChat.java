package com.seventhray.contract_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores Q&A and chat history for the 10-message rolling context window.
 */
@Entity
@Table(name = "contract_chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign key to Contract. 
    // NULLABLE! If null -> this is a global chat across all contracts.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    // Foreign key to User (ensures chats are isolated to the logged-in user)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Who sent this message? "USER" or "ASSISTANT"
    @Column(nullable = false, length = 20)
    private String role;

    // The text of the question or the extracted answer
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}