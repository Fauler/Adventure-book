package com.adventurebook.boundary.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * OpenAPI metadata for the Swagger UI served at {@code /swagger-ui/index.html}.
 * Every error response uses the {@link com.adventurebook.common.ApiError} shape.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI adventureBookOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Adventure Book API")
                .version("v1")
                .description("Browse, play, save and add interactive adventure books. "
                        + "Errors share one body: {timestamp, status, code, error, messages[]}; "
                        + "clients should branch on `code`."));
    }
}
