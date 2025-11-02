package com.example.demo.controller;

import com.example.demo.dto.user.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('CENTRAL')")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CENTRAL')")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CENTRAL')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User requester = (User) authentication.getPrincipal();
        userService.delete(id, requester);
        return ResponseEntity.noContent().build();
    }
}