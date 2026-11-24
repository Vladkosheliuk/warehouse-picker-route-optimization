package com.warehouse.picker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Authentication",
        description = "Authentication and current user information."
)
public class AuthController {

    @GetMapping("/me")
    @Operation(
            summary = "Get current authenticated user",
            description = "Returns the username and application role of the authenticated user. Requires HTTP Basic authentication."
    )
    public CurrentUser getCurrentUser(Authentication authentication) {

        boolean isManager = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_MANAGER".equals(authority.getAuthority())
                );

        boolean isPicker = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_PICKER".equals(authority.getAuthority())
                );

        if (!isManager && !isPicker) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unsupported user role"
            );
        }

        String role = isManager ? "MANAGER" : "PICKER";

        return new CurrentUser(
                authentication.getName(),
                role
        );
    }

    public record CurrentUser(
            String username,
            String role
    ) {
    }
}
