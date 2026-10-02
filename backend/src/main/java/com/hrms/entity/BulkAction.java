package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "BULK_ACTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkAction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bulk_action_seq")
    @SequenceGenerator(name = "bulk_action_seq", sequenceName = "BULK_ACTION_SEQ", allocationSize = 1)
    @Column(name = "BULK_ACTION_ID")
    private Long bulkActionId;

    @Column(name = "ACTION_TYPE", nullable = false, length = 80)
    private String actionType;

    @Column(name = "REQUESTED_BY", length = 100)
    private String requestedBy;

    @Column(name = "REQUESTED_AT", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    @Column(name = "TOTAL_EMPLOYEES")
    private Integer totalEmployees;

    @Column(name = "SUCCESSFUL_COUNT")
    private Integer successfulCount;

    @Column(name = "FAILED_COUNT")
    private Integer failedCount;

    @Column(name = "STATUS", nullable = false, length = 30)
    private String status;

    @Column(name = "REMARKS", length = 500)
    private String remarks;
}
