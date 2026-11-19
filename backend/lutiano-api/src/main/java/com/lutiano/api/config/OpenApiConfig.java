package com.lutiano.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The API description served at /api/v1/v3/api-docs (staff sign-in required) and committed as packages/api-contracts/openapi.yaml. */
@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI apiDescription() {
    return new OpenAPI()
        .info(new Info().title("LUTIANO API").version("2.0.0")
            .description("Every JSON response is {success, data, timestamp} or {success, error:{status, code, message}, timestamp}. Lists are a page: {items, page, size, total, pages, summary}."))
        .addServersItem(new Server().url("/api/v1"))
        .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("bearer", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
        .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearer"));
  }
}
