package com.hrms.dto;

import lombok.Data;

import java.util.List;

@Data
public class BulkActionResponse {
    private Long bulkActionId;
    private String actionType;
    private String status;
    private Integer totalEmployees;
    private Integer successfulCount;
    private Integer failedCount;
    private String remarks;
    private List<Long> employeeIds;
}
