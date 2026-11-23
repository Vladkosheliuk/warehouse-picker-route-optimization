
package com.warehouse.picker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
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
