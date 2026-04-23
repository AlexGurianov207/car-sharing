package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Thread-safe counter current value")
public class CounterValueResponse {

    @Schema(description = "Counter value", example = "42")
    private long value;
}
