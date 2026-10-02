package com.hrms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hrmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HRMS Employee Management API")
                        .description("Employee management APIs for HRMS backend")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("HRMS Team")
                                .email("support@hrms.local")));
    }
}
