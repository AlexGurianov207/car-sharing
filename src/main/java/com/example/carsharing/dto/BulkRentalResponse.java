package com.example.carsharing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Bulk rental creation response")
public class BulkRentalResponse {

    @Schema(description = "Number of requested rentals", example = "3")
    private int requestedCount;

    @Schema(description = "Number of successfully created rentals", example = "3")
    private int createdCount;

    @Schema(description = "Created rentals")
    private List<RentalResponse> rentals;
}
