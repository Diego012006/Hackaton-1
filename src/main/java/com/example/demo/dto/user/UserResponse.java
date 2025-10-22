package com.example.demo.dto.user;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO usado para listar o mostrar detalles de usuarios (solo ROLE_CENTRAL).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String role;
    private String branch;
    private LocalDateTime createdAt;
}
