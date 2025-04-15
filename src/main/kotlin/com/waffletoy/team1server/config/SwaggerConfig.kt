package com.waffletoy.team1server.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local", "dev", "prod")
class SwaggerConfig(
    @Value("\${custom.domain-name}") private val domain: String,
) {
    @Bean
    @Primary
    fun customOpenAPI(): OpenAPI {
        val servers =
            listOf(
                Server().url("https://api.survey-josha.site").description("Prod Server"),
                Server().url("https://www.api.survey-josha.site").description("Alias Prod Server"),
                Server().url("https://api.dev.survey-josha.site").description("Dev Server"),
                Server().url("http://localhost:8080").description("Local Test Server"),
            )

        return OpenAPI()
            .components(
                Components().addSecuritySchemes(
                    "BearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )
            .addSecurityItem(
                SecurityRequirement().addList("BearerAuth"),
            )
            .servers(servers)
    }
}
