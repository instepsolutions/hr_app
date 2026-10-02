package com.hrms.dto;

import lombok.Data;

@Data
public class EmployeeProfileRequest {
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
}
