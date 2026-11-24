package com.warehouse.picker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )



                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/favicon.svg",
                                "/actuator/health",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()

                        // Only managers can manage warehouses
                        .requestMatchers("/api/warehouses/**")
                        .hasRole("MANAGER")

                        // Managers and pickers can calculate routes
                        .requestMatchers("/api/routes/**")
                        .hasAnyRole("MANAGER", "PICKER")

                        // Only managers can create and view user accounts
                        .requestMatchers("/api/users", "/api/users/**")
                        .hasRole("MANAGER")


                        .requestMatchers("/api/tasks", "/api/tasks/**")
                        .hasAnyRole("MANAGER", "PICKER")

                        .requestMatchers("/api/catalog/**")
                        .hasRole("MANAGER")


                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )



                .httpBasic(basic -> {});

        return http.build();
    }
}
