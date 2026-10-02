package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStatisticsResponse {
    private Long totalEmployees;
    private Long activeEmployees;
    private Long newJoiners;
    private Long onLeaveToday;
    private Long probationEmployees;
    private Long exitedEmployees;
}
