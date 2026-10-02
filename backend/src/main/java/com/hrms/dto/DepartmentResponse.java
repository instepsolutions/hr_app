package com.hrms.dto;

import lombok.Data;

@Data
public class DepartmentResponse {
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private String description;
    private Long headEmployeeId;
    private Long parentDepartmentId;
    private String status;
}
