package com.warehouse.picker.config;

import com.warehouse.picker.entity.User;
import com.warehouse.picker.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialManagerSetup implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialManagerSetup(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        String username = System.getenv("INITIAL_MANAGER_USERNAME");
        String password = System.getenv("INITIAL_MANAGER_PASSWORD");
        String fullName = System.getenv("INITIAL_MANAGER_FULL_NAME");

        if (username == null || username.isBlank()
                || password == null || password.isBlank()
                || fullName == null || fullName.isBlank()) {
            return;
        }

        if (password.length() < 12) {
            throw new IllegalStateException(
                    "Initial manager password must contain at least 12 characters"
            );
        }

        if (!userRepository.findByRole("MANAGER").isEmpty()) {
            return;
        }

        User manager = new User();
        manager.setUsername(username);
        manager.setPasswordHash(passwordEncoder.encode(password));
        manager.setFullName(fullName);
        manager.setRole("MANAGER");
        manager.setActive(true);

        userRepository.save(manager);
    }
}
