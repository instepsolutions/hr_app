package com.hrms.service;

import com.hrms.dto.DashboardOverview;
import com.hrms.entity.Department;
import com.hrms.entity.Designation;
import com.hrms.entity.Employee;
import com.hrms.repository.DepartmentRepository;
import com.hrms.repository.DesignationRepository;
import com.hrms.repository.EmployeeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final List<String> DEPARTMENT_GROUPS = List.of(
            "Operations", "Sales & Marketing", "Technology", "Finance", "HR", "Others"
    );
    private static final Map<String, String> APPROVAL_LABELS = Map.of(
            "LEAVE_REQUEST", "Leave Requests",
            "EXPENSE_CLAIM", "Expense Claims",
            "TIMESHEET", "Timesheet Approvals",
            "DOCUMENT", "Document Approvals",
            "EXIT_CLEARANCE", "Exit Clearances"
    );

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final JdbcTemplate jdbcTemplate;

    public DashboardService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

        public DashboardOverview getOverview(LocalDate requestedDate, int months) {
        LocalDate reportDate = requestedDate == null ? LocalDate.now() : requestedDate;
        List<Employee> employees = employeeRepository.findAll();
        Map<Long, String> departmentNames = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getDepartmentId, Department::getDepartmentName));
        Map<Long, String> designationNames = designationRepository.findAll().stream()
                .collect(Collectors.toMap(Designation::getDesignationId, Designation::getDesignationName));

        long activeEmployees = employees.stream().filter(employee -> "ACTIVE".equals(employee.getEmployeeStatus())).count();
        long onLeaveToday = employees.stream().filter(employee -> "ON_LEAVE".equals(employee.getEmployeeStatus())).count();
        YearMonth currentMonth = YearMonth.from(reportDate);
        long newJoiners = countJoiners(employees, currentMonth);
        long previousMonthJoiners = countJoiners(employees, currentMonth.minusMonths(1));
        Double joinerChangePercent = previousMonthJoiners == 0 ? null
                : ((double) (newJoiners - previousMonthJoiners) / previousMonthJoiners) * 100.0;

        long pendingApprovals = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dashboard_approval WHERE status = 'PENDING'", Long.class);
        DashboardOverview.Summary summary = new DashboardOverview.Summary(
                employees.size(), activeEmployees, newJoiners, previousMonthJoiners,
                joinerChangePercent, pendingApprovals, onLeaveToday
        );

        return new DashboardOverview(
                reportDate,
                summary,
                getAttendance(reportDate),
                getDepartmentDistribution(employees, departmentNames),
                getMonthlyMovements(employees, currentMonth, months),
                getPendingApprovals(),
                getRecentJoiners(employees, reportDate, departmentNames, designationNames),
                getBirthdays(employees, reportDate, departmentNames, designationNames),
                getAnnouncements(reportDate),
                getEvents(reportDate)
        );
    }

    private long countJoiners(List<Employee> employees, YearMonth month) {
        return employees.stream()
                .map(Employee::getDateOfJoining)
                .filter(date -> date != null && YearMonth.from(date).equals(month))
                .count();
    }

    private DashboardOverview.Attendance getAttendance(LocalDate date) {
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query(
                "SELECT attendance_status, COUNT(*) AS total FROM dashboard_attendance WHERE attendance_date = ? GROUP BY attendance_status",
                statement -> statement.setDate(1, Date.valueOf(date)),
                                resultSet -> {
                                        counts.put(resultSet.getString("attendance_status"), resultSet.getLong("total"));
                                }
        );
        long present = counts.getOrDefault("PRESENT", 0L);
        long late = counts.getOrDefault("LATE", 0L);
        long absent = counts.getOrDefault("ABSENT", 0L);
        long total = present + late + absent;
        double percent = total == 0 ? 0.0 : ((double) (present + late) / total) * 100.0;
        return new DashboardOverview.Attendance(present, late, absent, percent);
    }

    private List<DashboardOverview.DepartmentDistribution> getDepartmentDistribution(
            List<Employee> employees,
            Map<Long, String> departmentNames
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        DEPARTMENT_GROUPS.forEach(group -> counts.put(group, 0L));
        for (Employee employee : employees) {
            String departmentName = departmentNames.get(employee.getDepartmentId());
            String group = DEPARTMENT_GROUPS.contains(departmentName) ? departmentName : "Others";
            counts.compute(group, (key, value) -> value + 1);
        }
        long total = employees.size();
        return counts.entrySet().stream()
                .map(entry -> new DashboardOverview.DepartmentDistribution(
                        entry.getKey(), entry.getValue(), total == 0 ? 0.0 : (double) entry.getValue() * 100.0 / total
                ))
                .toList();
    }

        private List<DashboardOverview.MonthlyMovement> getMonthlyMovements(List<Employee> employees, YearMonth month, int months) {
        List<DashboardOverview.MonthlyMovement> results = new ArrayList<>();
                for (int offset = months - 1; offset >= 0; offset--) {
            YearMonth itemMonth = month.minusMonths(offset);
            long joiners = employees.stream().map(Employee::getDateOfJoining)
                    .filter(date -> date != null && YearMonth.from(date).equals(itemMonth)).count();
            long exits = employees.stream().map(Employee::getDateOfExit)
                    .filter(date -> date != null && YearMonth.from(date).equals(itemMonth)).count();
            results.add(new DashboardOverview.MonthlyMovement(
                    itemMonth.atDay(1).format(java.time.format.DateTimeFormatter.ofPattern("MMM ''yy", Locale.US)),
                    joiners,
                    exits
            ));
        }
        return results;
    }

    private List<DashboardOverview.ApprovalCategory> getPendingApprovals() {
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query(
                "SELECT approval_type, COUNT(*) AS total FROM dashboard_approval WHERE status = 'PENDING' GROUP BY approval_type",
                                resultSet -> {
                                        counts.put(resultSet.getString("approval_type"), resultSet.getLong("total"));
                                }
        );
        return APPROVAL_LABELS.entrySet().stream()
                .map(entry -> new DashboardOverview.ApprovalCategory(
                        entry.getKey(), entry.getValue(), counts.getOrDefault(entry.getKey(), 0L)
                ))
                .toList();
    }

    private List<DashboardOverview.EmployeeCard> getRecentJoiners(
            List<Employee> employees,
            LocalDate date,
            Map<Long, String> departments,
            Map<Long, String> designations
    ) {
        return employees.stream()
                .filter(employee -> employee.getDateOfJoining() != null && !employee.getDateOfJoining().isAfter(date))
                .sorted(Comparator.comparing(Employee::getDateOfJoining).reversed())
                .limit(5)
                .map(employee -> employeeCard(employee, employee.getDateOfJoining(), departments, designations))
                .toList();
    }

    private List<DashboardOverview.EmployeeCard> getBirthdays(
            List<Employee> employees,
            LocalDate date,
            Map<Long, String> departments,
            Map<Long, String> designations
    ) {
        return employees.stream()
                .filter(employee -> employee.getDateOfBirth() != null)
                .map(employee -> Map.entry(employee, nextBirthday(employee.getDateOfBirth(), date)))
                .filter(entry -> !entry.getValue().isBefore(date))
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(entry -> employeeCard(entry.getKey(), entry.getValue(), departments, designations))
                .toList();
    }

    private LocalDate nextBirthday(LocalDate birthDate, LocalDate date) {
        LocalDate next;
        try {
            next = birthDate.withYear(date.getYear());
        } catch (DateTimeException ignored) {
            next = LocalDate.of(date.getYear(), 2, 28);
        }
        if (next.isBefore(date)) {
            try {
                next = next.withYear(date.getYear() + 1);
            } catch (DateTimeException ignored) {
                next = LocalDate.of(date.getYear() + 1, 2, 28);
            }
        }
        return next;
    }

    private DashboardOverview.EmployeeCard employeeCard(
            Employee employee,
            LocalDate date,
            Map<Long, String> departments,
            Map<Long, String> designations
    ) {
        String designation = designations.getOrDefault(employee.getDesignationId(), "Employee");
        String department = departments.getOrDefault(employee.getDepartmentId(), "Unassigned");
        return new DashboardOverview.EmployeeCard(
                employee.getEmployeeId(), employee.getEmployeeCode(), employee.getDisplayName(),
                designation + " / " + department, employee.getProfilePhotoUrl(), date
        );
    }

    private List<DashboardOverview.Announcement> getAnnouncements(LocalDate date) {
        return jdbcTemplate.query(
                "SELECT announcement_id, title, summary, published_at, is_new FROM dashboard_announcement WHERE is_active = 1 AND published_at < ? ORDER BY published_at DESC FETCH FIRST 3 ROWS ONLY",
                statement -> statement.setTimestamp(1, Timestamp.valueOf(date.plusDays(1).atStartOfDay())),
                (resultSet, rowNumber) -> new DashboardOverview.Announcement(
                        resultSet.getLong("announcement_id"), resultSet.getString("title"),
                        resultSet.getString("summary"), resultSet.getTimestamp("published_at").toLocalDateTime(),
                        resultSet.getInt("is_new") == 1
                )
        );
    }

    private List<DashboardOverview.DashboardEvent> getEvents(LocalDate date) {
        return jdbcTemplate.query(
                "SELECT event_id, event_name, start_date, end_date, location FROM dashboard_event WHERE is_active = 1 AND end_date >= ? ORDER BY start_date FETCH FIRST 3 ROWS ONLY",
                statement -> statement.setDate(1, Date.valueOf(date)),
                (resultSet, rowNumber) -> new DashboardOverview.DashboardEvent(
                        resultSet.getLong("event_id"), resultSet.getString("event_name"),
                        resultSet.getDate("start_date").toLocalDate(), resultSet.getDate("end_date").toLocalDate(),
                        resultSet.getString("location")
                )
        );
    }
}
