package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMPLOYEE_LIFECYCLE_EVENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_lifecycle_event_seq")
    @SequenceGenerator(name = "employee_lifecycle_event_seq", sequenceName = "EMPLOYEE_LIFECYCLE_EVENT_SEQ", allocationSize = 1)
    @Column(name = "EVENT_ID")
    private Long eventId;

    @Column(name = "EMPLOYEE_ID", nullable = false)
    private Long employeeId;

    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private String eventType;

    @Column(name = "EVENT_DATE", nullable = false)
    private LocalDate eventDate;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "REFERENCE_ID", length = 100)
    private String referenceId;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}
