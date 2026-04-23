package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Race condition demo execution result")
public class RaceDemoResponse {

    @Schema(description = "Execution mode", example = "UNSAFE")
    private RaceMode mode;

    @Schema(description = "Threads used", example = "50")
    private int threads;

    @Schema(description = "Increments per thread", example = "10000")
    private int iterationsPerThread;

    @Schema(description = "Expected counter value", example = "500000")
    private int expectedValue;

    @Schema(description = "Actual counter value", example = "421037")
    private int actualValue;

    @Schema(description = "Lost updates count", example = "78963")
    private int lostUpdates;

    @Schema(description = "Execution duration in milliseconds", example = "245")
    private long durationMs;
}
