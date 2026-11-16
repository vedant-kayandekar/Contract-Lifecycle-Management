package com.seventhray.contract_backend.service;

import com.seventhray.contract_backend.dto.auth.AuthResponse;
import com.seventhray.contract_backend.dto.auth.LoginRequest;
import com.seventhray.contract_backend.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse resgister(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
