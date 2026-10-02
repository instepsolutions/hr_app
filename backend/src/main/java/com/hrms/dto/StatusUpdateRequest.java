package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|ON_LEAVE|PROBATION|NOTICE_PERIOD|RESIGNED|EXITED|INACTIVE)$", message = "Unsupported employee status")
    private String status;
    private String reason;
    private String remarks;
}
