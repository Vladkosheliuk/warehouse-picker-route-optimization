package com.warehouse.picker.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturn404ForMissingResource() {
        when(request.getRequestURI())
                .thenReturn("/api/routes/calculate/999");

        var response = handler.handleNotFound(
                new NoSuchElementException("Picking task not found: 999"),
                request
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
        assertEquals(
                "Picking task not found: 999",
                response.getBody().get("message")
        );
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void shouldReturn400ForInvalidArgument() {
        when(request.getRequestURI())
                .thenReturn("/api/routes/calculate/1");

        var response = handler.handleBadRequest(
                new IllegalArgumentException("Invalid route"),
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Invalid route", response.getBody().get("message"));
    }

    @Test
    void shouldReturn409ForInvalidState() {
        when(request.getRequestURI())
                .thenReturn("/api/routes/calculate/1");

        var response = handler.handleConflict(
                new IllegalStateException("Insufficient stock"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals(
                "Insufficient stock",
                response.getBody().get("message")
        );
    }

    @Test
    void shouldReturnFieldValidationErrors() {
        when(request.getRequestURI())
                .thenReturn("/api/warehouses");

        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);

        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError(
                        "warehouseDTO",
                        "name",
                        "Warehouse name is required"
                ),
                new FieldError(
                        "warehouseDTO",
                        "description",
                        null
                ),
                new FieldError(
                        "warehouseDTO",
                        "name",
                        "Duplicate error"
                )
        ));

        var response = handler.handleValidation(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));

        @SuppressWarnings("unchecked")
        var fieldErrors = (java.util.Map<String, String>)
                response.getBody().get("fieldErrors");

        assertNotNull(fieldErrors);
        assertEquals(
                "Warehouse name is required",
                fieldErrors.get("name")
        );
        assertEquals(
                "Invalid value",
                fieldErrors.get("description")
        );
        assertEquals(2, fieldErrors.size());
    }
}
