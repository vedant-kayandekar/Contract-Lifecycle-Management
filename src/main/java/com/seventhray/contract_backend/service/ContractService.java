package com.seventhray.contract_backend.service;

import com.seventhray.contract_backend.dto.contract.ContractResponse;
import com.seventhray.contract_backend.model.ContractStatus;
import com.seventhray.contract_backend.model.ContractType;
import com.seventhray.contract_backend.model.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ContractService {

    ContractResponse uploadContract(
            MultipartFile file,
            String title,
            String clientName,
            ContractType contractType,
            BigDecimal value,
            LocalDate startDate,
            LocalDate endDate,
            User user
    );

    List<ContractResponse> getUserContracts(User user, String search, ContractStatus status);

    ContractResponse getContractById(Long id, User user);

    ContractResponse updateStatus(Long id, ContractStatus newStatus, User user);

    void deleteContract(Long id, User user);

    Resource getContractFileAsResource(Long id, User user);

    String getContractFilename(Long id, User user);
}