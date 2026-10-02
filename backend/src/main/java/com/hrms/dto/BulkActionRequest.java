package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkActionRequest {
    @NotBlank(message = "Action type is required")
    private String actionType;

    @NotNull(message = "Employee IDs are required")
    private List<Long> employeeIds;

    private String newValue;
    private String remarks;
}
