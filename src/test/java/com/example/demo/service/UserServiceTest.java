package com.example.demo.service;

import com.example.demo.dto.user.UserResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // 🔧 Helper para crear usuarios de prueba
    private User newUser(Long id, String username, String email, Role role, String branch) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword("Oreo1234");
        u.setRole(role);
        u.setBranch(branch);
        u.setCreatedAt(LocalDateTime.now());
        u.setExpired(false);
        u.setLocked(false);
        u.setCredentialsExpired(false);
        u.setEnable(true);
        return u;
    }

    // 1️⃣ listUsers
    @Test
    @DisplayName("Debe listar todos los usuarios mapeados correctamente a UserResponse")
    void shouldListAllUsersMapped() {
        User u1 = newUser(1L, "oreo.admin", "admin@oreo.com", Role.CENTRAL, null);
        User u2 = newUser(2L, "mira.user", "mira@oreo.com", Role.BRANCH, "Miraflores");
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserResponse> result = userService.listUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("oreo.admin");
        assertThat(result.get(0).getRole()).isEqualTo(Role.CENTRAL);
        assertThat(result.get(1).getUsername()).isEqualTo("mira.user");
        assertThat(result.get(1).getBranch()).isEqualTo("Miraflores");

        verify(userRepository, times(1)).findAll();
        verifyNoMoreInteractions(userRepository);
    }

    // 2️⃣ findById (exitoso)
    @Test
    @DisplayName("Debe devolver un UserResponse cuando el usuario existe")
    void shouldFindUserById() {
        User u = newUser(1L, "oreo.admin", "admin@oreo.com", Role.CENTRAL, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UserResponse result = userService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("oreo.admin");
        assertThat(result.getEmail()).isEqualTo("admin@oreo.com");
        assertThat(result.getRole()).isEqualTo(Role.CENTRAL);

        verify(userRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(userRepository);
    }

    // 3️⃣ findById (no encontrado)
    @Test
    @DisplayName("Debe lanzar BusinessException 404 cuando el usuario no existe")
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getMessage()).contains("Usuario no encontrado");
                });

        verify(userRepository, times(1)).findById(99L);
        verifyNoMoreInteractions(userRepository);
    }

    // 4️⃣ delete (intenta eliminarse a sí mismo)
    @Test
    @DisplayName("Debe lanzar BAD_REQUEST si el usuario intenta eliminarse a sí mismo")
    void shouldNotAllowSelfDelete() {
        User requester = newUser(1L, "oreo.admin", "admin@oreo.com", Role.CENTRAL, null);

        assertThatThrownBy(() -> userService.delete(1L, requester))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("No puede eliminar su propio usuario");
                });

        verifyNoInteractions(userRepository);
    }

    // 5️⃣ delete (usuario no existe)
    @Test
    @DisplayName("Debe lanzar NOT_FOUND si el usuario a eliminar no existe")
    void shouldThrowNotFoundWhenDeletingMissingUser() {
        User requester = newUser(1L, "oreo.admin", "admin@oreo.com", Role.CENTRAL, null);
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L, requester))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getMessage()).contains("Usuario no encontrado");
                });

        verify(userRepository, times(1)).existsById(99L);
        verifyNoMoreInteractions(userRepository);
    }

    // 6️⃣ delete (correcto)
    @Test
    @DisplayName("Debe eliminar correctamente cuando el usuario existe y no es el mismo")
    void shouldDeleteSuccessfully() {
        User requester = newUser(1L, "oreo.admin", "admin@oreo.com", Role.CENTRAL, null);
        when(userRepository.existsById(2L)).thenReturn(true);

        userService.delete(2L, requester);

        verify(userRepository, times(1)).existsById(2L);
        verify(userRepository, times(1)).deleteById(2L);
        verifyNoMoreInteractions(userRepository);
    }
}
