package com.seventhray.contract_backend.model;

import jakarta.persistence.*;
import lombok.*;
import tools.jackson.databind.ser.jdk.JDKKeySerializers.Default;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity 
@Table(name = "contracts")
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Contract {
     
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many Contracts belong to One user (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(name= "client_name" ,nullable = false)
    private String clientName;

    // Stores the enum as a human-readable string in PostgreSQL (e.g. "EPC", "NDA")
    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 30)
    @Builder.Default
    private ContractType contractType = ContractType.OTHER;

    // Stores status as a string (e.g. "DRAFT", "UNDER_REVIEW")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ContractStatus status = ContractStatus.DRAFT;

    // Original filename uploaded by the user (e.g., "Vendor_SLA_2026.pdf")
    @Column(name = "file_name", nullable = false)
    private String fileName;

    // Absolute disk path where the physical file is stored on the server
    @Column(name = "file_path", nullable = false)
    private String filePath;

    // MIME type (e.g., "application/pdf" or "application/vnd.openxmlformats-...")
    @Column(name = "file_type")
    private String fileType;

    // Size in bytes
    @Column(name = "file_size")
    private Long fileSize;

    // Extracted plain text from the document (Used for Q&A and Search)
    @Column(name = "parsed_text", columnDefinition = "TEXT")
    private String parsedText;

    // Monetary contract value (precision 12, scale 2 means up to 999,999,999.99)
    @Column(precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Auto excecuts before the row is INSERTED into PostgreSQL
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ContractStatus.DRAFT;
        }
        if (this.contractType == null) {
            this.contractType = ContractType.OTHER;
        }
    }

    //Automatic executes before the row is UPDATE in PostgreSQL
    @PreUpdate 
    protected void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

}
