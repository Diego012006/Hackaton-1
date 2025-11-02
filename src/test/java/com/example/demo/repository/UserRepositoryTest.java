package com.example.demo.repository;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.4-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private UserRepository userRepository;

    // Para limpiar el contexto de persistencia en tests
    @Autowired
    private TestEntityManager em;

    // --------- helpers ---------
    private static User newUser(
            String username,
            String email,
            String password,
            Role role,
            String branch
    ) {
        User u = new User();
        u.setUsername(username);
        u.setNombreCompleto("Nombre de " + username);
        u.setEmail(email);
        u.setPassword(password);
        u.setRole(role);
        u.setBranch(branch);
        // La entidad tiene createdAt nullable=false; si no usas @PrePersist, setéalo aquí.
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }

    private Long centralId;
    private Long branchId;

    @BeforeEach
    void seed() {
        User central = newUser(
                "oreo.admin",
                "admin@oreo.com",
                "Oreo1234",
                Role.CENTRAL,
                null
        );
        central = userRepository.saveAndFlush(central);
        centralId = central.getId();

        User branch = newUser(
                "mira.user",
                "mira@oreo.com",
                "Oreo1234",
                Role.BRANCH,
                "Miraflores"
        );
        branch = userRepository.saveAndFlush(branch);
        branchId = branch.getId();
    }

    @Test
    @DisplayName("findByUsername y findByEmail devuelven el usuario esperado")
    void shouldFindByUsernameAndEmail() {
        Optional<User> byUsername = userRepository.findByUsername("oreo.admin");
        Optional<User> byEmail = userRepository.findByEmail("mira@oreo.com");

        assertThat(byUsername).isPresent();
        assertThat(byUsername.get().getId()).isEqualTo(centralId);
        assertThat(byUsername.get().getRole()).isEqualTo(Role.CENTRAL);
        assertThat(byUsername.get().getBranch()).isNull();

        assertThat(byEmail).isPresent();
        assertThat(byEmail.get().getId()).isEqualTo(branchId);
        assertThat(byEmail.get().getRole()).isEqualTo(Role.BRANCH);
        assertThat(byEmail.get().getBranch()).isEqualTo("Miraflores");

        // authorities refleja el rol
        assertThat(byUsername.get().getAuthorities())
                .extracting("authority")
                .containsExactly("CENTRAL");
        assertThat(byEmail.get().getAuthorities())
                .extracting("authority")
                .containsExactly("BRANCH");
    }

    @Test
    @DisplayName("findByUsername/findByEmail devuelven Optional.empty cuando no existe")
    void shouldReturnEmptyWhenNotFound() {
        assertThat(userRepository.findByUsername("no.existe")).isEmpty();
        assertThat(userRepository.findByEmail("nobody@oreo.com")).isEmpty();
    }

    @Test
    @DisplayName("Unicidad: no se permite username duplicado")
    void shouldEnforceUniqueUsername() {
        User dup = newUser(
                "oreo.admin",             // duplicado
                "otra@oreo.com",
                "otraPass123",
                Role.CENTRAL,
                null
        );

        assertThatThrownBy(() -> userRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Unicidad: no se permite email duplicado")
    void shouldEnforceUniqueEmail() {
        User dup = newUser(
                "otro.usuario",
                "mira@oreo.com",          // duplicado
                "otraPass123",
                Role.BRANCH,
                "Miraflores"
        );

        assertThatThrownBy(() -> userRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Flags por defecto se persisten correctamente (expired/locked/credentialsExpired=false, enable=true)")
    void shouldPersistDefaultFlags() {
        User u = newUser(
                "si.user",
                "si@oreo.com",
                "Oreo1234",
                Role.BRANCH,
                "San Isidro"
        );
        u = userRepository.saveAndFlush(u);

        User reloaded = userRepository.findById(u.getId()).orElseThrow();

        assertThat(reloaded.getExpired()).isFalse();
        assertThat(reloaded.getLocked()).isFalse();
        assertThat(reloaded.getCredentialsExpired()).isFalse();
        assertThat(reloaded.getEnable()).isTrue();
    }

    @Test
    @DisplayName("createdAt es inmutable (updatable=false): un update no debe cambiar el valor en BD")
    void createdAtIsImmutable() {
        User u = newUser(
                "temp.user",
                "temp@oreo.com",
                "Oreo1234",
                Role.CENTRAL,
                null
        );

        // Guardamos con createdAt inicial
        LocalDateTime created0 = u.getCreatedAt();
        u = userRepository.saveAndFlush(u);

        // Intentamos cambiar createdAt y actualizar
        LocalDateTime attemptNew = created0.plusDays(3);
        u.setCreatedAt(attemptNew);
        userRepository.saveAndFlush(u);

        // Forzar lectura real desde la BD
        userRepository.flush();
        em.clear();

        // Leemos desde BD
        User reloaded = userRepository.findById(u.getId()).orElseThrow();

        // Debe seguir el valor original
        assertThat(reloaded.getCreatedAt()).isEqualTo(created0);
        assertThat(reloaded.getCreatedAt()).isNotEqualTo(attemptNew);
    }
}
