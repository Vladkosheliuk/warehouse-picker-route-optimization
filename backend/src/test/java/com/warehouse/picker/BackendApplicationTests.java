package com.warehouse.picker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "spring.flyway.user=test_user",
        "spring.flyway.password=test_password"
})
@AutoConfigureMockMvc
class BackendApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldConnectToIsolatedTestDatabase() {
        String databaseName = jdbcTemplate.queryForObject(
                "SELECT current_database()",
                String.class
        );

        assertEquals(
                "warehouse_picker_test",
                databaseName
        );
    }

    @Test
    void shouldApplyFlywayMigrations() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE success = TRUE
                """,
                Integer.class
        );

        assertEquals(5, migrationCount);
    }

    @Test
    void shouldAllowHealthCheckWithoutAuthentication()
            throws Exception {

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnauthorizedRouteCalculation()
            throws Exception {

        mockMvc.perform(
                        post("/api/routes/calculate/1")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthorizedWarehouseAccess()
            throws Exception {

        mockMvc.perform(get("/api/warehouses"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @Transactional
    void shouldAllowManagerWithBasicAuthentication()
            throws Exception {

        String username = "integration-manager";
        String password = "TestManagerPassword123!";

        jdbcTemplate.update("""
            INSERT INTO public.users
                (username, password_hash, full_name, role, is_active)
            VALUES (?, ?, ?, ?, true)
            """,
                username,
                passwordEncoder.encode(password),
                "Integration Manager",
                "MANAGER"
        );

        mockMvc.perform(
                        get("/api/warehouses")
                                .with(httpBasic(username, password))
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void shouldEnforcePickerPermissions()
            throws Exception {

        String username = "integration-picker";
        String password = "TestPickerPassword123!";

        jdbcTemplate.update("""
            INSERT INTO public.users
                (username, password_hash, full_name, role, is_active)
            VALUES (?, ?, ?, ?, true)
            """,
                username,
                passwordEncoder.encode(password),
                "Integration Picker",
                "PICKER"
        );

        // A picker cannot manage warehouses.
        mockMvc.perform(
                        get("/api/warehouses")
                                .with(httpBasic(username, password))
                )
                .andExpect(status().isForbidden());

        // A picker can access route calculation,
        // but this picking task does not exist.
        mockMvc.perform(
                        post("/api/routes/calculate/999999999")
                                .with(httpBasic(username, password))
                )
                .andExpect(status().isNotFound());
    }

}
