package com.seventhray.contract_backend.service.impl;

import com.seventhray.contract_backend.dto.contract.ContractResponse;
import com.seventhray.contract_backend.exception.BadRequestException;
import com.seventhray.contract_backend.exception.FileStorageException;
import com.seventhray.contract_backend.exception.ResourceNotFoundException;
import com.seventhray.contract_backend.model.Contract;
import com.seventhray.contract_backend.model.ContractStatus;
import com.seventhray.contract_backend.model.ContractType;
import com.seventhray.contract_backend.model.User;
import com.seventhray.contract_backend.repository.ContractRepository;
import com.seventhray.contract_backend.service.ContractService;
import com.seventhray.contract_backend.service.parser.DocumentParserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    // Reads "app.upload.dir" from application.properties (defaults to ./uploads)
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private final ContractRepository contractRepository;
    private final DocumentParserService parserService;

    public ContractServiceImpl(ContractRepository contractRepository, DocumentParserService parserService) {
        this.contractRepository = contractRepository;
        this.parserService = parserService;
    }

    @Override
    public ContractResponse uploadContract(
            MultipartFile file,
            String title,
            String clientName,
            ContractType contractType,
            BigDecimal value,
            LocalDate startDate,
            LocalDate endDate,
            User user
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select a PDF or Word (.docx) file to upload");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "contract.pdf";

        // 1. Create a dedicated folder for this user: ./uploads/{userId}/
        Path userUploadPath = Paths.get(uploadDir, user.getId().toString()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(userUploadPath);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory: " + userUploadPath, e);
        }

        // 2. Generate unique filename using UUID to prevent collisions
        String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        Path targetLocation = userUploadPath.resolve(storedFilename);

        // 3. Save physical file to disk
        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file on disk: " + storedFilename, e);
        }

        // 4. Extract readable plain text using our DocumentParserService (Apache PDFBox / POI)
        String extractedText = parserService.extractText(file);

        // 5. Construct and persist Contract entity in PostgreSQL
        Contract contract = Contract.builder()
                .user(user) // Binds contract to the currently logged-in user
                .title(title.trim())
                .clientName(clientName.trim())
                .contractType(contractType != null ? contractType : ContractType.OTHER)
                .status(ContractStatus.DRAFT) // All new contracts start as DRAFT
                .fileName(originalFilename)
                .filePath(targetLocation.toString())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .parsedText(extractedText)
                .value(value)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Contract saved = contractRepository.save(contract);
        return ContractResponse.fromEntity(saved, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> getUserContracts(User user, String search, ContractStatus status) {
        List<Contract> contracts;

        // Apply filters if provided
        if (status != null) {
            contracts = contractRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status);
        } else if (search != null && !search.trim().isEmpty()) {
            contracts = contractRepository.searchContracts(user.getId(), search.trim());
        } else {
            contracts = contractRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }

        // Map list of entities to list of DTOs (without full text to save bandwidth)
        return contracts.stream()
                .map(c -> ContractResponse.fromEntity(c, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContractById(Long id, User user) {
        Contract contract = findContractOrThrow(id, user);
        return ContractResponse.fromEntity(contract, true);
    }

    @Override
    public ContractResponse updateStatus(Long id, ContractStatus newStatus, User user) {
        Contract contract = findContractOrThrow(id, user);
        ContractStatus current = contract.getStatus();

        // 🛑 ENFORCE STATE MACHINE:
        if (!current.canTransitionTo(newStatus)) {
            throw new BadRequestException(
                    "Invalid status transition: Cannot move contract from '" + current + "' to '" + newStatus +
                    "'. Standard flow: DRAFT -> UNDER_REVIEW -> APPROVED (or REJECTED)."
            );
        }

        contract.setStatus(newStatus);
        Contract updated = contractRepository.save(contract);
        return ContractResponse.fromEntity(updated, false);
    }

    @Override
    public void deleteContract(Long id, User user) {
        Contract contract = findContractOrThrow(id, user);

        // Delete physical file from server disk
        try {
            Path filePath = Paths.get(contract.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {}

        // Delete row from PostgreSQL
        contractRepository.delete(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getContractFileAsResource(Long id, User user) {
        Contract contract = findContractOrThrow(id, user);
        try {
            Path filePath = Paths.get(contract.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Contract file not found on server");
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Invalid file path: " + contract.getFilePath());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getContractFilename(Long id, User user) {
        Contract contract = findContractOrThrow(id, user);
        return contract.getFileName();
    }

    /**
     * Tenant Isolation Guard: Ensures the contract exists AND belongs to the requesting user.
     */
    private Contract findContractOrThrow(Long id, User user) {
        return contractRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with id: " + id));
    }
}