package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "DESIGNATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Designation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "designation_seq")
    @SequenceGenerator(name = "designation_seq", sequenceName = "DESIGNATION_SEQ", allocationSize = 1)
    @Column(name = "DESIGNATION_ID")
    private Long designationId;

    @Column(name = "DESIGNATION_NAME", nullable = false, length = 120)
    private String designationName;

    @Column(name = "DESIGNATION_CODE", length = 30)
    private String designationCode;

    @Column(name = "GRADE", length = 50)
    private String grade;

    @Column(name = "LEVEL_NO")
    private Integer levelNo;

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
