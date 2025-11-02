package com.example.demo.service.auth;

import com.example.demo.config.JwtUtils;
import com.example.demo.dto.auth.*;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    public UserResponse register(RegisterRequest req) {
        if (!Role.contiene(req.getRole()))
            throw new IllegalArgumentException("Role debe ser CENTRAL o BRANCH");

        userRepository.findByUsername(req.getUsername()).ifPresent(u -> {
            throw new BusinessException(HttpStatus.CONFLICT, "El username ya está registrado");
        });
        userRepository.findByEmail(req.getEmail()).ifPresent(u -> {
            throw new BusinessException(HttpStatus.CONFLICT, "El email ya está registrado");
        });

        Role role = Role.valueOf(req.getRole().toUpperCase());
        if (role == Role.BRANCH && (req.getBranch() == null || req.getBranch().isBlank()))
            throw new IllegalArgumentException("Branch es obligatorio para usuarios BRANCH");
        if (role == Role.CENTRAL) req.setBranch(null);

        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRole(role);
        u.setBranch(req.getBranch());
        User saved = userRepository.save(u);

        return UserResponse.builder()
                .id(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .role(saved.getRole())
                .branch(saved.getBranch())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public JwtAuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsernameOrEmail(), req.getPassword())
        );

        // Cargar user por username o email
        UserDetails principal = userRepository.findByUsername(req.getUsernameOrEmail())
                .map(u -> (UserDetails) u)
                .orElseGet(() -> userRepository.findByEmail(req.getUsernameOrEmail())
                        .map(u -> (UserDetails) u)
                        .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas")));

        User u = (User) principal;

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", u.getRole().name());
        claims.put("branch", u.getBranch());

        String token = jwtUtils.generateToken(u, claims);
        return new JwtAuthResponse(token, jwtUtils.getExpiresSeconds(), u.getRole().name(), u.getBranch());
    }
}