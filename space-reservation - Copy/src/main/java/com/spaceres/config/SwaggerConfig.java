package com.spaceres.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Space Reservation API")
                        .description("공간 예약 시스템 REST API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Team SpaceRes")
                                .email("contact@spaceres.com")))
                // JWT Bearer 인증 스키마 등록
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Access Token을 입력하세요 (Bearer 없이)")))
                // 전역 보안 요구사항 적용
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
