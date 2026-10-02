package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "EMPLOYMENT_TYPE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employment_type_seq")
    @SequenceGenerator(name = "employment_type_seq", sequenceName = "EMPLOYMENT_TYPE_SEQ", allocationSize = 1)
    @Column(name = "EMPLOYMENT_TYPE_ID")
    private Long employmentTypeId;

    @Column(name = "EMPLOYMENT_TYPE_NAME", nullable = false, length = 80)
    private String employmentTypeName;

    @Column(name = "DESCRIPTION", length = 250)
    private String description;

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
