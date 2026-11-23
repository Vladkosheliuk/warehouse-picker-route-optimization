
package com.warehouse.picker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record WarehouseMapDTO(

        @NotNull
        @Size(min = 3, max = 1000)
        List<@NotNull @Valid NodeDTO> nodes,

        @NotNull
        @Size(min = 1, max = 5000)
        List<@NotNull @Valid EdgeDTO> edges

) {

    public enum NodeType {
        ENTRANCE,
        EXIT,
        INTERSECTION,
        STORAGE
    }

    public record NodeDTO(

            @NotBlank
            @Pattern(regexp = "[A-Za-z0-9_-]{1,50}")
            String code,

            @NotNull
            NodeType type,

            @NotNull
            @Digits(integer = 8, fraction = 2)
            BigDecimal x,

            @NotNull
            @Digits(integer = 8, fraction = 2)
            BigDecimal y

    ) {
    }

    public record EdgeDTO(

            @NotBlank
            String from,

            @NotBlank
            String to,

            @NotNull
            @DecimalMin("0.01")
            @Digits(integer = 8, fraction = 2)
            BigDecimal distanceM,

            @NotNull
            Boolean bidirectional

    ) {
    }
}
