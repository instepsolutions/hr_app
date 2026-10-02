package com.hrms.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeeProfileResponse {
    private Long profileId;
    private Long employeeId;
    private String bloodGroup;
    private String maritalStatus;
    private String nationality;
    private String aadhaarLastFour;
    private String panLastFour;
    private String emergencyContactName;
    private String emergencyContactNumber;
    private String emergencyContactRelation;
    private String currentAddress;
    private String permanentAddress;
    private String city;
    private String state;
    private String postalCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
