package com.warehouse.picker.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API error response")
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
