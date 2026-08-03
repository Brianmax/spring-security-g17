package com.jwt.codigo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bankingApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Virtual Banking API")
                        .version("v1")
                        .description("Phase-one API for users, virtual accounts, ledger entries, same-currency transfers, "
                                + "and cross-currency transfers using Decolecta SBS average rates. "
                                + "This API does not move real money and intentionally has no authentication or authorization.")
                        .contact(new Contact().name("Banking API team"))
                        .license(new License().name("Internal simulator")))
                .servers(List.of(new Server().url("/").description("Current server")));
    }
}
