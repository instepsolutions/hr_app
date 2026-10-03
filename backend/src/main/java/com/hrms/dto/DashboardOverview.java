package com.hrms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardOverview(
        LocalDate reportDate,
        Summary summary,
        Attendance attendance,
        List<DepartmentDistribution> employeeDistribution,
        List<MonthlyMovement> monthlyMovements,
        List<ApprovalCategory> pendingApprovals,
        List<EmployeeCard> recentJoiners,
        List<EmployeeCard> birthdays,
        List<Announcement> announcements,
        List<DashboardEvent> events
) {
    public record Summary(
            long totalEmployees,
            long activeEmployees,
            long newJoiners,
            long previousMonthJoiners,
            Double joinerChangePercent,
            long pendingApprovals,
            long onLeaveToday
    ) {}

    public record Attendance(long present, long late, long absent, double attendancePercent) {}
    public record DepartmentDistribution(String department, long employeeCount, double percentage) {}
    public record MonthlyMovement(String month, long joiners, long exits) {}
    public record ApprovalCategory(String type, String label, long count) {}
    public record EmployeeCard(Long employeeId, String employeeCode, String displayName, String subtitle, String photoUrl, LocalDate date) {}
    public record Announcement(Long id, String title, String summary, LocalDateTime publishedAt, boolean isNew) {}
    public record DashboardEvent(Long id, String title, LocalDate startDate, LocalDate endDate, String location) {}
}
