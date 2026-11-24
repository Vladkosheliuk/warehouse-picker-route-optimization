package com.warehouse.picker.service;

import com.warehouse.picker.dto.CreateUserRequest;
import com.warehouse.picker.dto.UserResponse;
import com.warehouse.picker.entity.User;
import com.warehouse.picker.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        String username = request.username().trim();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException(
                    "Username already exists"
            );
        }

        User user = new User();

        user.setUsername(username);
        user.setFullName(request.fullName().trim());

        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setRole(request.role().name());
        user.setActive(true);

        User saved = userRepository.save(user);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll(
                        Sort.by(Sort.Direction.ASC, "id")
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.isActive()
        );
    }
}
