package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "BULK_ACTION_EMPLOYEE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkActionEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bulk_action_employee_seq")
    @SequenceGenerator(name = "bulk_action_employee_seq", sequenceName = "BULK_ACTION_EMPLOYEE_SEQ", allocationSize = 1)
    @Column(name = "BULK_ACTION_EMPLOYEE_ID")
    private Long bulkActionEmployeeId;

    @Column(name = "BULK_ACTION_ID", nullable = false)
    private Long bulkActionId;

    @Column(name = "EMPLOYEE_ID", nullable = false)
    private Long employeeId;

    @Column(name = "OLD_VALUE", length = 200)
    private String oldValue;

    @Column(name = "NEW_VALUE", length = 200)
    private String newValue;

    @Column(name = "STATUS", nullable = false, length = 30)
    private String status;

    @Column(name = "ERROR_MESSAGE", length = 500)
    private String errorMessage;

    @Column(name = "PROCESSED_AT")
    private LocalDateTime processedAt;
}
