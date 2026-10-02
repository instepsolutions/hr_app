package com.hrms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeRequest {

    @NotBlank(message = "Employee code is required")
    private String employeeCode;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY)?$", message = "Unsupported gender value")
    private String gender;

    private LocalDate dateOfBirth;

    @Email(message = "Invalid personal email")
    private String personalEmail;

    @NotBlank(message = "Official email is required")
    @Email(message = "Invalid official email")
    private String officialEmail;

    @Pattern(regexp = "^[0-9+() -]{8,20}$", message = "Invalid mobile number")
    private String mobileNumber;

    private String alternateMobile;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotNull(message = "Designation is required")
    private Long designationId;

    @NotNull(message = "Location is required")
    private Long locationId;

    @NotNull(message = "Employment type is required")
    private Long employmentTypeId;

    private Long reportingManagerId;

    private LocalDate dateOfJoining;
    private LocalDate confirmationDate;
    private LocalDate dateOfExit;

    @Pattern(regexp = "^(ACTIVE|ON_LEAVE|PROBATION|NOTICE_PERIOD|RESIGNED|EXITED|INACTIVE)?$", message = "Unsupported employee status")
    private String employeeStatus;
    private String profilePhotoUrl;
}
