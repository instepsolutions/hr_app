package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "DEPARTMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "department_seq")
    @SequenceGenerator(name = "department_seq", sequenceName = "DEPARTMENT_SEQ", allocationSize = 1)
    @Column(name = "DEPARTMENT_ID")
    private Long departmentId;

    @Column(name = "DEPARTMENT_NAME", nullable = false, length = 120)
    private String departmentName;

    @Column(name = "DEPARTMENT_CODE", nullable = false, unique = true, length = 30)
    private String departmentCode;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "HEAD_EMPLOYEE_ID")
    private Long headEmployeeId;

    @Column(name = "PARENT_DEPARTMENT_ID")
    private Long parentDepartmentId;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
}
