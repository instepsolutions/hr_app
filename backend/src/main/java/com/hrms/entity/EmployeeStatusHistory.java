package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMPLOYEE_STATUS_HISTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_status_history_seq")
    @SequenceGenerator(name = "employee_status_history_seq", sequenceName = "EMPLOYEE_STATUS_HISTORY_SEQ", allocationSize = 1)
    @Column(name = "HISTORY_ID")
    private Long historyId;

    @Column(name = "EMPLOYEE_ID", nullable = false)
    private Long employeeId;

    @Column(name = "OLD_STATUS", length = 30)
    private String oldStatus;

    @Column(name = "NEW_STATUS", nullable = false, length = 30)
    private String newStatus;

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "REASON", length = 200)
    private String reason;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "CHANGED_BY", length = 100)
    private String changedBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}
