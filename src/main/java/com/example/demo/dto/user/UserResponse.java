package com.example.demo.dto.user;

import com.example.demo.entity.Role;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class UserResponse {
    Long id;
    String username;
    String email;
    Role role;
    String branch;
    LocalDateTime createdAt;
}