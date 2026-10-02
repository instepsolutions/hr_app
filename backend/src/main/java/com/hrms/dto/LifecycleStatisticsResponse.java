package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleStatisticsResponse {
    private Long newHires;
    private Long onboarding;
    private Long activeEmployees;
    private Long inDevelopment;
    private Long dueForConfirmation;
    private Long exits;
    private Map<String, Long> stageCounts;
}