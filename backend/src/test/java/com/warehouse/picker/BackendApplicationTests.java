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

    @Test
    void shouldExposeDocumentedApiErrorResponses() throws Exception {

        var result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        String[][] expectedResponses = {
                {"/api/users", "post", "400", "409"},
                {"/api/tasks", "post", "400", "404", "409"},
                {"/api/tasks/{id}/start", "post", "404", "409"},
                {"/api/tasks/{id}/items/{itemId}/collect", "post", "400", "404", "409"},
                {"/api/tasks/{id}/complete", "post", "404", "409"},
                {"/api/tasks/{id}/cancel", "post", "404", "409"},
                {"/api/tasks/{id}/assign", "post", "400", "404", "409"},
                {"/api/tasks/{id}", "get", "404"},
                {"/api/catalog/products", "get", "400", "404"},
                {"/api/warehouses/{warehouseId}/map/import", "post", "400", "404", "409"},
                {"/api/warehouses/{warehouseId}/map", "get", "404"}
        };

        for (String[] operation : expectedResponses) {

            String responsePath = "$.paths['" + operation[0]
                    + "']." + operation[1] + ".responses";

            for (int i = 2; i < operation.length; i++) {

                result.andExpect(
                        jsonPath(responsePath + "['" + operation[i] + "']")
                                .exists()
                );
            }

            result.andExpect(
                    jsonPath(responsePath + "['401'].content")
                            .doesNotExist()
            );

            result.andExpect(
                    jsonPath(responsePath + "['403'].content")
                            .doesNotExist()
            );
        }

        result.andExpect(
                jsonPath("$.components.schemas.ApiError.properties.fieldErrors.type")
                        .value("object")
        );

        result.andExpect(
                jsonPath("$.components.schemas.ApiError.properties.timestamp.format")
                        .value("date-time")
        );
    }

}
