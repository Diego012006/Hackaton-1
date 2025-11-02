package com.example.demo.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Arranca un contenedor de PostgreSQL y expone las properties de Spring.
 * Extiende esta clase en tus tests (@DataJpaTest, @SpringBootTest, etc.).
 */
@Testcontainers
public abstract class TestContainersConfig {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.4-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);

        // Crear y destruir esquema automáticamente durante el test
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Opcional
        r.add("spring.jpa.show-sql", () -> "false");
        r.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
    }
}
