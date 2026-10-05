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

public final class PmsDtos {

    private PmsDtos() {
    }

    public record Metric(double value, double previous, double change, Double changePercent) {
    }

    public record Period(LocalDate startDate, LocalDate endDate, LocalDate baselineDate, int days, String department) {
    }

    public record GoalSummary(long total, long completed, long inProgress, long behind, long notStarted, double averageProgress) {
    }

    public record TrendPoint(String label, LocalDate date, long total, long completed, long inProgress, long behind,
                             long notStarted, double averageProgress) {
    }

    public record AppraisalStatus(long totalEmployees, long completed, long hrReview, long managerReview,
                                  long selfAppraisal, long yetToStart) {
    }

    public record RatingBucket(int rating, long count, double percent) {
    }

    public record DepartmentRating(String department, double averageRating, long appraisals) {
    }

    public record TopPerformer(long employeeId, String name, String department, String designation, double rating) {
    }

    public record PipOverview(long total, long active, long completed, long improved, long notImproved, double successRate) {
    }

    public record Deadline(String name, String department, LocalDate dueDate, long daysLeft, long pending, long employees,
                           String kind) {
    }

    public record DepartmentGoalRow(String department, long total, long completed, long inProgress, long behind,
                                    long notStarted, double averageProgress) {
    }

    public record CategoryCount(String category, long count, double percent) {
    }

    public record AlignmentCount(String type, long count, double percent) {
    }

    public record DashboardKpis(Metric totalEmployees, Metric goalsAssigned, Metric goalsCompleted, Metric averageProgress,
                                Metric appraisalsCompleted, Metric averageRating) {
    }

    public record DashboardOverview(Period period, DashboardKpis kpis, GoalSummary goalSummary, List<TrendPoint> trend,
                                    AppraisalStatus appraisalStatus, List<RatingBucket> ratingDistribution,
                                    List<DepartmentRating> departmentPerformance, List<TopPerformer> topPerformers,
                                    PipOverview pip, List<Deadline> deadlines, List<DepartmentGoalRow> departmentGoals) {
    }

    public record GoalKpis(Metric total, Metric completed, Metric inProgress, Metric behind, Metric notStarted,
                           Metric averageProgress) {
    }

    public record GoalOverview(Period period, GoalKpis kpis, GoalSummary summary, List<TrendPoint> trend,
                               List<AlignmentCount> alignment, List<CategoryCount> categories,
                               List<DepartmentGoalRow> departmentGoals, List<GoalRow> myGoals, List<Deadline> deadlines,
                               double completionRate, boolean linkedToEmployee) {
    }

    public record GoalRow(Long goalId, String title, String description, Long employeeId, String employeeName,
                          String employeeCode, Long departmentId, String department, String departmentGroup,
                          Long categoryId, String category, String goalType, Long kraId, String kra, Long kpiId, String kpi,
                          BigDecimal weightage, BigDecimal targetValue, String unit, BigDecimal progressPercent,
                          String priority, Long alignmentGoalId, String alignmentGoalTitle, Long managerId, String managerName,
                          String status, LocalDate startDate, LocalDate dueDate, LocalDateTime completedAt,
                          boolean archived, LocalDateTime archivedAt, String archivedBy, String archiveReason,
                          LocalDateTime updatedAt, boolean overdue) {
    }

    public record GoalHistoryRow(String actionType, String oldValue, String newValue, String changedBy,
                                 LocalDateTime changedAt) {
    }

    public record GoalPage(List<GoalRow> content, long totalElements, int totalPages, int page, int size) {
    }

    public record GoalRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotNull Long employeeId,
            Long departmentId,
            Long categoryId,
            @NotBlank String goalType,
            Long kraId,
            Long kpiId,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal weightage,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal targetValue,
            @NotBlank @Size(max = 30) String unit,
            @DecimalMin("0") @DecimalMax("100") BigDecimal progressPercent,
            @NotBlank String priority,
            Long alignmentGoalId,
            Long managerId,
            @NotBlank String status,
            @NotNull LocalDate startDate,
            @NotNull LocalDate dueDate
    ) {
    }

    public record ProgressRequest(
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal progressPercent,
            String status,
            @Size(max = 300) String comment
    ) {
    }

    public record ArchiveRequest(@Size(max = 500) String reason) {
    }

    public record AlignmentNode(Long goalId, String title, String goalType, String owner, String status,
                                BigDecimal progressPercent, long childCount, List<AlignmentNode> children) {
    }

    public record CalendarEvent(Long goalId, String title, String kind, LocalDate date, String status, String owner,
                                String priority) {
    }

    public record LookupItem(Long id, String name, Long parentId, String unit, BigDecimal targetValue) {
    }
}
