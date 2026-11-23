
package com.warehouse.picker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI warehouseOpenAPI() {

        String securitySchemeName = "basicAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Warehouse Picker Route Optimization API")
                        .version("1.0.0")
                        .description(
                                "REST API for warehouse management, " +
                                        "user accounts, warehouse graph layouts, " +
                                        "picking tasks, product availability and " +
                                        "weight-aware route optimization."
                        )
                        .license(new License()
                                .name("All rights reserved")
                                .identifier("LicenseRef-Proprietary")
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Current application server")
                ))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(securitySchemeName)
                )
                .components(new Components()
                        .addSecuritySchemes(
                                securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                        )
                );
    }

    @Bean
    public OpenApiCustomizer commonErrorResponses() {

        return openApi -> {

            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem ->

                    pathItem.readOperations().forEach(operation -> {

                        if (operation.getResponses() == null) {
                            operation.setResponses(new ApiResponses());
                        }

                        ApiResponses responses = operation.getResponses();

                        responses.putIfAbsent(
                                "401",
                                new ApiResponse()
                                        .description(
                                                "Authentication is required or credentials are invalid."
                                        )
                        );

                        responses.putIfAbsent(
                                "403",
                                new ApiResponse()
                                        .description(
                                                "The authenticated user does not have permission to perform this operation."
                                        )
                        );
                    })
            );
        };
    }
}
