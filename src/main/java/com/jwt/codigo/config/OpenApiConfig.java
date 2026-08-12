package com.jwt.codigo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI bankingApi() {
        String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("Virtual Banking API").version("v1")
                        .description("API de banca virtual protegida con JWT Bearer, roles, permisos y validación de propiedad. No mueve dinero real.")
                        .contact(new Contact().name("Banking API team"))
                        .license(new License().name("Internal simulator")))
                .components(new Components().addSecuritySchemes(scheme, new SecurityScheme()
                        .name(scheme).type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .security(List.of(new SecurityRequirement().addList(scheme)))
                .servers(List.of(new Server().url("/").description("Current server")));
    }
}
