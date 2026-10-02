package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUDIT_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(name = "audit_log_seq", sequenceName = "AUDIT_LOG_SEQ", allocationSize = 1)
    @Column(name = "AUDIT_ID")
    private Long auditId;

    @Column(name = "ENTITY_NAME", nullable = false, length = 100)
    private String entityName;

    @Column(name = "ENTITY_ID")
    private Long entityId;

    @Column(name = "ACTION_TYPE", nullable = false, length = 50)
    private String actionType;

    @Lob
    @Column(name = "OLD_DATA")
    private String oldData;

    @Lob
    @Column(name = "NEW_DATA")
    private String newData;

    @Column(name = "PERFORMED_BY", length = 100)
    private String performedBy;

    @Column(name = "PERFORMED_AT", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;
}
