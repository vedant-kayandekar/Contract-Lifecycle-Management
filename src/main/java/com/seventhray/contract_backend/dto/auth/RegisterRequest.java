package com.seventhray.contract_backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "Name cannot be empty")
    @Size(min = 2, max = 80, message = "Name must be between 2 and 80 characters")
    private String name;

    @NotBlank(message = "Emial cannot be empty")
    @Email(message = "Email must be a valid format")
    private String email;

    @NotBlank(message = "Password Cannot be empty")
    @Size(min = 6, max = 25 , message = "Password must be at least 6 characters" )
    private String password;
    private String companyName;

}

