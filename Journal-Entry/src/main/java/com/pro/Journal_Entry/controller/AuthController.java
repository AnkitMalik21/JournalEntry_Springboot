package com.pro.Journal_Entry.controller;

import com.pro.Journal_Entry.dto.AuthRequest;
import com.pro.Journal_Entry.dto.AuthResponse;
import com.pro.Journal_Entry.dto.RegisterRequest;
import com.pro.Journal_Entry.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register new user
     * POST /api/auth/register
     */

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Register new user
     * POST /api/auth/register
     */

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request){
        AuthResponse response = authService.login(request);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    /**
     * Health check
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health(){
        return ResponseEntity.ok("Auth service is running!");
    }
}
