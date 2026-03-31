package com.example.carsharing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI carsharingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Carsharing API")
                        .version("v1")
                        .description("API for managing users, cars, rentals, payments and extra services")
                        .contact(new Contact().name("Carsharing Team")));
    }
}
