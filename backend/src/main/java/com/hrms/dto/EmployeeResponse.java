package com.hrms.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EmployeeResponse {
    private Long employeeId;
    private String employeeCode;
    private String firstName;
    private String middleName;
    private String lastName;
    private String displayName;
    private String gender;
    private LocalDate dateOfBirth;
    private String personalEmail;
    private String officialEmail;
    private String mobileNumber;
    private String alternateMobile;
    private Long departmentId;
    private Long designationId;
    private Long locationId;
    private Long employmentTypeId;
    private Long reportingManagerId;
    private LocalDate dateOfJoining;
    private LocalDate confirmationDate;
    private LocalDate dateOfExit;
    private String employeeStatus;
    private String profilePhotoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
