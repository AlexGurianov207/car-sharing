package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Response returned after async task start")
public class AsyncTaskStartResponse {

    @Schema(description = "Unique task ID", example = "63fe53eb-8f93-4fb7-bf98-d8a421a1fb7f")
    private String taskId;
}
