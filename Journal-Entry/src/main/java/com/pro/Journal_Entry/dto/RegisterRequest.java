package com.pro.Journal_Entry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min=3,max=50,message = "Username is required")
    private String Username;

    @NotBlank(message = "Email is required")
    @Email(message="Invalid email format")
    private String email;

    @NotBlank(message="Password is required")
    @Size(min=6,message = "Password must be aleast 6 characters")
    private String password;
}
