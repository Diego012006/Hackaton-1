package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI oreoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Oreo Insight Factory API")
                        .version("v1")
                        .description("Documentación de la API de Oreo Insight Factory")
                        .contact(new Contact().name("Oreo Insight Factory").email("soporte@oreofactory.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
                );
    }
}