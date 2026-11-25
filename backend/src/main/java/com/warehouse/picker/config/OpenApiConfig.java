package com.warehouse.picker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

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
                )
                .addTagsItem(new Tag()
                        .name("Product Catalog")
                        .description(
                                "Retrieve available products and stock information by warehouse."
                        ));
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
                                new ApiResponse().description(
                                        "Authentication is required or credentials are invalid."
                                )
                        );

                        responses.putIfAbsent(
                                "403",
                                new ApiResponse().description(
                                        "The authenticated user does not have permission to perform this operation."
                                )
                        );

                        responses.get("401").setContent(null);
                        responses.get("403").setContent(null);

                        switch (operation.getOperationId()) {

                            case "importMap" -> {
                                addError(responses, "400", "Invalid map");
                                addError(responses, "404", "Warehouse not found");
                                addError(responses, "409", "Warehouse already contains a map");
                            }

                            case "getMap" ->
                                    addError(responses, "404", "Warehouse not found");

                            case "createUser" -> {
                                addError(responses, "400", "Invalid user data");
                                addError(responses, "409", "Username already exists");
                            }

                            case "create" -> {
                                addError(responses, "400", "Invalid task data or duplicate product");
                                addError(responses, "404", "Warehouse, picker or product not found");
                                addError(responses, "409", "Insufficient product stock");
                            }

                            case "start" -> {
                                addError(responses, "404", "Picking task not found");
                                addError(responses, "409", "Only assigned tasks can be started");
                            }

                            case "collect" -> {
                                addError(responses, "400",
                                        "Invalid quantity or item does not belong to the task");
                                addError(responses, "404",
                                        "Picking task or task item not found");
                                addError(responses, "409",
                                        "Invalid task state or collected quantity exceeds required quantity");
                            }

                            case "complete" -> {
                                addError(responses, "404", "Picking task not found");
                                addError(responses, "409",
                                        "Task is not in progress or not all items have been collected");
                            }

                            case "cancel" -> {
                                addError(responses, "404", "Picking task not found");
                                addError(responses, "409",
                                        "Task cannot be cancelled in its current state");
                            }

                            case "assign" -> {
                                addError(responses, "400",
                                        "Invalid picker data or selected user is not an active picker");
                                addError(responses, "404", "Picking task or picker not found");
                                addError(responses, "409",
                                        "Only new or assigned tasks can be assigned");
                            }

                            case "getById" ->
                                    addError(responses, "404", "Picking task not found");

                            case "getProducts" -> {
                                responses.addApiResponse(
                                        "400",
                                        new ApiResponse().description(
                                                "Missing or invalid warehouseId parameter"
                                        )
                                );

                                addError(responses, "404", "Warehouse not found");
                            }

                            case null, default -> {
                            }
                        }
                    })
            );

            Schema<?> apiError = openApi.getComponents()
                    .getSchemas()
                    .get("ApiError");

            if (apiError != null) {

                apiError.addProperty(
                        "timestamp",
                        new StringSchema().format("date-time")
                );

                apiError.addProperty(
                        "fieldErrors",
                        new MapSchema()
                                .additionalProperties(new StringSchema())
                                .description(
                                        "Field-specific validation errors, when applicable"
                                )
                );
            }
        };
    }

    private void addError(
            ApiResponses responses,
            String statusCode,
            String description
    ) {

        responses.addApiResponse(
                statusCode,
                new ApiResponse()
                        .description(description)
                        .content(new Content()
                                .addMediaType(
                                        "application/json",
                                        new MediaType().schema(
                                                new Schema<>()
                                                        .$ref("#/components/schemas/ApiError")
                                        )
                                )
                        )
        );
    }
}