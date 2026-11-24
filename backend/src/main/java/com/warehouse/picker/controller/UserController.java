package com.warehouse.picker.controller;

import com.warehouse.picker.dto.CreateUserRequest;
import com.warehouse.picker.dto.UserResponse;
import com.warehouse.picker.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(
        name = "User Management",
        description = "Manage application users and picker accounts. Requires the MANAGER role."
)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(
            summary = "List all users",
            description = "Returns registered application users without exposing password hashes. Requires the MANAGER role."
    )
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a user account",
            description = "Creates a MANAGER or PICKER account. Validates the username, full name, password and role. Requires the MANAGER role."
    )
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return userService.createUser(request);
    }
}
