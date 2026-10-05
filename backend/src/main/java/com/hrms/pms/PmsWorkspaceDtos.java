package com.hrms.pms;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PmsWorkspaceDtos {

    private PmsWorkspaceDtos() {
    }

    public record KraRequest(
            @NotBlank @Size(max = 160) String kraName,
            Long categoryId,
            @Size(max = 500) String description,
            Long departmentId,
            @NotBlank String ownerType,
            Long ownerEmployeeId,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal weightage,
            @NotBlank String status,
            @NotNull LocalDate effectiveDate) {
    }

    public record KpiRequest(
            @NotNull Long kraId,
            @NotBlank @Size(max = 160) String kpiName,
            @NotBlank String measurementType,
            @NotNull @DecimalMin("0.01") BigDecimal targetValue,
            @Size(max = 30) String unit,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal weightage,
            Long departmentId,
            @NotBlank String ownerType,
            Long ownerEmployeeId,
            @NotBlank String frequency,
            @NotBlank String status) {
    }

    public record KraRow(long kraId, String kraName, Long categoryId, String categoryName, String description,
                         Long departmentId, String department, String ownerType, Long ownerEmployeeId, String owner,
                         BigDecimal weightage, String status, LocalDate effectiveDate, long kpisLinked,
                         long mappedEmployees, LocalDate createdDate) {
    }

    public record KpiRow(long kpiId, String kpiName, long kraId, String kraName, String measurementType,
                         BigDecimal targetValue, String unit, BigDecimal weightage, Long departmentId,
                         String department, String ownerType, Long ownerEmployeeId, String owner, String frequency,
                         String status, LocalDate createdDate) {
    }

    public record IdMappingRequest(List<Long> kraIds, List<Long> kpiIds) {
    }

    public record EmployeeKraMapping(long kraId, BigDecimal weightage) {
    }

    public record EmployeeKpiMapping(long kpiId, BigDecimal weightage) {
    }

    public record EmployeeMappingRequest(List<EmployeeKraMapping> kras, List<EmployeeKpiMapping> kpis) {
    }

    public record DepartmentMapping(long departmentId, String department, List<KraRow> kras, List<KpiRow> kpis) {
    }

    public record EmployeeMapping(long employeeId, String employeeCode, String employeeName, String department,
                                  String designation, String manager, List<KraRow> kras, List<KpiRow> kpis) {
    }

    public record SetupHistoryRow(long historyId, String actionType, String moduleName, Long itemId, String itemTitle,
                                  String oldValue, String newValue, String changedBy, LocalDateTime changedAt) {
    }

    public record TimelineStage(String stageCode, LocalDate startDate, LocalDate endDate) {
    }

    public record CommentRequest(@NotBlank @Size(max = 2000) String commentText) {
    }

    public record CommentRow(long commentId, long appraisalId, String authorName, String authorRole,
                            String commentText, LocalDateTime createdAt) {
    }
}