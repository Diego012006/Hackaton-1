package com.example.demo.security;

import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return repository.findByUsername(usernameOrEmail)
                .map(u -> (UserDetails) u)
                .orElseGet(() -> repository.findByEmail(usernameOrEmail)
                        .map(u -> (UserDetails) u)
                        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + usernameOrEmail)));
    }

    public UserDetailsService userDetailsService() { return this; }
}