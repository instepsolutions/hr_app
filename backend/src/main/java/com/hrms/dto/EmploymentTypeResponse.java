package com.hrms.dto;

import lombok.Data;

@Data
public class EmploymentTypeResponse {
    private Long employmentTypeId;
    private String employmentTypeName;
    private String description;
    private String status;
}
