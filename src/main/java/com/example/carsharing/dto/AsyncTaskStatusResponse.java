package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Async task current status")
public class AsyncTaskStatusResponse {

    @Schema(description = "Task ID", example = "63fe53eb-8f93-4fb7-bf98-d8a421a1fb7f")
    private String taskId;

    @Schema(description = "Task state", example = "PROCESSING")
    private AsyncTaskState status;

    @Schema(description = "Payment ID associated with successful task execution", example = "15")
    private Long paymentId;

    @Schema(description = "Error message if task failed", example = "Rental not found with id: 100")
    private String errorMessage;

    @Schema(description = "Task creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Task start time")
    private LocalDateTime startedAt;

    @Schema(description = "Task finish time")
    private LocalDateTime finishedAt;
}
