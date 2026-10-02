package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "EMPLOYEE_PROFILE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_profile_seq")
    @SequenceGenerator(name = "employee_profile_seq", sequenceName = "EMPLOYEE_PROFILE_SEQ", allocationSize = 1)
    @Column(name = "PROFILE_ID")
    private Long profileId;

    @Column(name = "EMPLOYEE_ID", nullable = false, unique = true)
    private Long employeeId;

    @Column(name = "BLOOD_GROUP", length = 10)
    private String bloodGroup;

    @Column(name = "MARITAL_STATUS", length = 30)
    private String maritalStatus;

    @Column(name = "NATIONALITY", length = 80)
    private String nationality;

    @Column(name = "AADHAAR_LAST_FOUR", length = 10)
    private String aadhaarLastFour;

    @Column(name = "PAN_LAST_FOUR", length = 10)
    private String panLastFour;

    @Column(name = "EMERGENCY_CONTACT_NAME", length = 120)
    private String emergencyContactName;

    @Column(name = "EMERGENCY_CONTACT_NUMBER", length = 30)
    private String emergencyContactNumber;

    @Column(name = "EMERGENCY_CONTACT_RELATION", length = 50)
    private String emergencyContactRelation;

    @Column(name = "CURRENT_ADDRESS", length = 500)
    private String currentAddress;

    @Column(name = "PERMANENT_ADDRESS", length = 500)
    private String permanentAddress;

    @Column(name = "CITY", length = 100)
    private String city;

    @Column(name = "STATE", length = 100)
    private String state;

    @Column(name = "POSTAL_CODE", length = 20)
    private String postalCode;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}
