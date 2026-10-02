package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMPLOYEE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
    @SequenceGenerator(name = "employee_seq", sequenceName = "EMPLOYEE_SEQ", allocationSize = 1)
    @Column(name = "EMPLOYEE_ID")
    private Long employeeId;

    @Column(name = "EMPLOYEE_CODE", nullable = false, unique = true, length = 30)
    private String employeeCode;

    @Column(name = "FIRST_NAME", nullable = false, length = 100)
    private String firstName;

    @Column(name = "MIDDLE_NAME", length = 100)
    private String middleName;

    @Column(name = "LAST_NAME", nullable = false, length = 100)
    private String lastName;

    @Column(name = "DISPLAY_NAME", nullable = false, length = 150)
    private String displayName;

    @Column(name = "GENDER", length = 20)
    private String gender;

    @Column(name = "DATE_OF_BIRTH")
    private LocalDate dateOfBirth;

    @Column(name = "PERSONAL_EMAIL", length = 150)
    private String personalEmail;

    @Column(name = "OFFICIAL_EMAIL", nullable = false, unique = true, length = 150)
    private String officialEmail;

    @Column(name = "MOBILE_NUMBER", length = 30)
    private String mobileNumber;

    @Column(name = "ALTERNATE_MOBILE", length = 30)
    private String alternateMobile;

    @Column(name = "DEPARTMENT_ID")
    private Long departmentId;

    @Column(name = "DESIGNATION_ID")
    private Long designationId;

    @Column(name = "LOCATION_ID")
    private Long locationId;

    @Column(name = "EMPLOYMENT_TYPE_ID")
    private Long employmentTypeId;

    @Column(name = "REPORTING_MANAGER_ID")
    private Long reportingManagerId;

    @Column(name = "DATE_OF_JOINING")
    private LocalDate dateOfJoining;

    @Column(name = "CONFIRMATION_DATE")
    private LocalDate confirmationDate;

    @Column(name = "DATE_OF_EXIT")
    private LocalDate dateOfExit;

    @Column(name = "EMPLOYEE_STATUS", nullable = false, length = 30)
    private String employeeStatus = "ACTIVE";

    @Column(name = "PROFILE_PHOTO_URL", length = 500)
    private String profilePhotoUrl;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
}
