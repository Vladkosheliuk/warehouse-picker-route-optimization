package com.warehouse.picker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9._-]{3,100}")
        String username,

        @NotBlank
        @Size(max = 150)
        String fullName,

        @NotBlank
        @Size(min = 12, max = 72)
        String password,

        @NotNull
        Role role
) {
    public enum Role {
        MANAGER,
        PICKER
    }
}
