package com.example.demo.dto.auth;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthResponse {
    private String token;
    private long   expiresIn; // en segundos
    private String role;      // "CENTRAL" | "BRANCH"
    private String branch;    // null si CENTRAL
}
