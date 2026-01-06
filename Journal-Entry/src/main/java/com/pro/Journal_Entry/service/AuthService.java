package com.pro.Journal_Entry.service;

import com.pro.Journal_Entry.dto.AuthRequest;
import com.pro.Journal_Entry.dto.AuthResponse;
import com.pro.Journal_Entry.dto.RegisterRequest;
import com.pro.Journal_Entry.entity.Role;
import com.pro.Journal_Entry.entity.User;
import com.pro.Journal_Entry.repository.RoleRepository;
import com.pro.Journal_Entry.repository.UserRepository;
import com.pro.Journal_Entry.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Register new user
     * @Transactional - If anything fails, rollback everything
     */

    @Transactional
    public AuthResponse register(RegisterRequest request){
        //check if username or email already exists
        if(userRepository.existsByUsername(request.getUsername())){
            throw new RuntimeException("Username already exists");
        }

        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email already exists");
        }

        //Get Role_user from database
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        //Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(userRole))
                .build();

        user = userRepository.save(user);

        //Generate Jwt token
        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getId(),
                "ROLE_USER"
        );

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                        .build();
    }

    //Login user
    public AuthResponse login(AuthRequest request){
        //Authenticate user(Spring Security handles password verification)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        //If authentication successful,get user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(()-> new RuntimeException("User not found"));

        //Get primary role
        String primaryRole = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_USER");

        //Generate JWT token
        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getId(),
                primaryRole
        );

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();

    }

}
