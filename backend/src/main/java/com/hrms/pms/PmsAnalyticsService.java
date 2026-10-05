package com.hrms.pms;

import com.hrms.pms.PmsDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Oracle-backed PMS analytics. Goal figures all come from the latest daily snapshot at or before a date,
 * so KPI cards, trend charts and tables always reconcile with each other.
 */
@Service
public class PmsAnalyticsService {

    static final List<String> GROUPS = List.of("IT", "HR", "Sales & Marketing", "Operations", "Finance", "Others");
    static final int MAX_RANGE_DAYS = 90;
    private static final String ELIGIBLE = "e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')";
    private static final String EMP_GROUP = "COALESCE(dg.group_name, 'Others')";
    private static final String GOAL_GROUP = "COALESCE(dg.group_name, 'Others')";
    private static final String LATEST_SNAPSHOT = " FROM pms_goal g JOIN pms_goal_progress p ON p.goal_id = g.goal_id"
            + " AND p.recorded_on = (SELECT MAX(p2.recorded_on) FROM pms_goal_progress p2"
            + " WHERE p2.goal_id = g.goal_id AND p2.recorded_on <= ?)"
            + " LEFT JOIN pms_department_group_v dg ON dg.department_id = g.department_id WHERE g.is_archived = 0";
    private static final String STATUS_SUMS = "SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END),"
            + " SUM(CASE WHEN p.status = 'IN_PROGRESS' THEN 1 ELSE 0 END),"
            + " SUM(CASE WHEN p.status = 'BEHIND' THEN 1 ELSE 0 END),"
            + " SUM(CASE WHEN p.status = 'NOT_STARTED' THEN 1 ELSE 0 END),"
            + " NVL(AVG(p.progress_percent), 0)";
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    private final JdbcTemplate jdbc;

    public PmsAnalyticsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ period / filters

    public Period period(LocalDate start, LocalDate end, String department) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = end == null ? today : end;
        if (endDate.isAfter(today)) {
            endDate = today;
        }
        LocalDate startDate = start == null ? endDate.minusDays(25) : start;
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be on or before the end date");
        }
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
        return new Period(startDate, endDate, startDate.minusDays(1), days, normalizeDepartment(department));
    }

    public String normalizeDepartment(String department) {
        if (department == null || department.isBlank() || "ALL".equalsIgnoreCase(department)
                || "All Departments".equalsIgnoreCase(department)) {
            return null;
        }
        if (!GROUPS.contains(department)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown department group: " + department);
        }
        return department;
    }

    private static String deptClause(String department, List<Object> args, String expression) {
        if (department == null) {
            return "";
        }
        args.add(department);
        return " AND " + expression + " = ?";
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static Metric metric(double current, double previous) {
        double change = round2(current - previous);
        Double percent = previous == 0 ? null : round1((current - previous) / previous * 100.0);
        return new Metric(current, previous, change, percent);
    }

    // ------------------------------------------------------------------ goal figures

    public GoalSummary goalSummary(LocalDate date, String department) {
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(date));
        String sql = "SELECT COUNT(*), " + STATUS_SUMS + LATEST_SNAPSHOT + deptClause(department, args, GOAL_GROUP);
        return jdbc.queryForObject(sql, (rs, n) -> new GoalSummary(rs.getLong(1), rs.getLong(2), rs.getLong(3),
                rs.getLong(4), rs.getLong(5), round1(rs.getDouble(6))), args.toArray());
    }

    public List<DepartmentGoalRow> departmentGoals(LocalDate date, String department) {
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(date));
        String sql = "SELECT " + GOAL_GROUP + ", COUNT(*), " + STATUS_SUMS + LATEST_SNAPSHOT
                + deptClause(department, args, GOAL_GROUP) + " GROUP BY " + GOAL_GROUP;
        Map<String, DepartmentGoalRow> byGroup = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            byGroup.put(rs.getString(1), new DepartmentGoalRow(rs.getString(1), rs.getLong(2), rs.getLong(3),
                    rs.getLong(4), rs.getLong(5), rs.getLong(6), round1(rs.getDouble(7))));
        }, args.toArray());
        List<DepartmentGoalRow> rows = new ArrayList<>();
        for (String group : GROUPS) {
            if (department != null && !department.equals(group)) {
                continue;
            }
            rows.add(byGroup.getOrDefault(group, new DepartmentGoalRow(group, 0, 0, 0, 0, 0, 0)));
        }
        return rows;
    }

    public List<TrendPoint> trend(LocalDate start, LocalDate end, String granularity, String department) {
        String mode = granularity == null ? "DAILY" : granularity.toUpperCase(Locale.ROOT);
        List<LocalDate> dates = new ArrayList<>();
        switch (mode) {
            case "DAILY" -> {
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    dates.add(d);
                }
            }
            case "WEEKLY" -> {
                for (int i = 7; i >= 0; i--) {
                    dates.add(end.minusDays(7L * i));
                }
            }
            case "MONTHLY" -> {
                for (int i = 3; i >= 0; i--) {
                    dates.add(end.minusDays(30L * i));
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Granularity must be DAILY, WEEKLY or MONTHLY");
        }
        List<TrendPoint> points = new ArrayList<>();
        for (LocalDate date : dates) {
            GoalSummary s = goalSummary(date, department);
            points.add(new TrendPoint(DAY_LABEL.format(date), date, s.total(), s.completed(), s.inProgress(),
                    s.behind(), s.notStarted(), s.averageProgress()));
        }
        return points;
    }

    private List<AlignmentCount> alignment(LocalDate date, String department) {
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(date));
        String sql = "SELECT g.goal_type, COUNT(*)" + LATEST_SNAPSHOT + deptClause(department, args, GOAL_GROUP)
                + " GROUP BY g.goal_type";
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            counts.put(rs.getString(1), rs.getLong(2));
        }, args.toArray());
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        List<AlignmentCount> result = new ArrayList<>();
        for (String type : List.of("COMPANY", "DEPARTMENT", "TEAM", "INDIVIDUAL")) {
            long count = counts.getOrDefault(type, 0L);
            result.add(new AlignmentCount(type, count, total == 0 ? 0 : round1(count * 100.0 / total)));
        }
        return result;
    }

    public List<CategoryCount> categories(LocalDate date, String department) {
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(date));
        String sql = "SELECT NVL(c.category_name, 'Uncategorised'), COUNT(*)" + LATEST_SNAPSHOT.replace(
                "LEFT JOIN pms_department_group_v dg",
                "LEFT JOIN pms_goal_category c ON c.category_id = g.category_id LEFT JOIN pms_department_group_v dg")
                + deptClause(department, args, GOAL_GROUP) + " GROUP BY NVL(c.category_name, 'Uncategorised')"
                + " ORDER BY COUNT(*) DESC, 1";
        List<Object[]> rows = new ArrayList<>();
        jdbc.query(sql, rs -> {
            rows.add(new Object[]{rs.getString(1), rs.getLong(2)});
        }, args.toArray());
        long total = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        List<CategoryCount> result = new ArrayList<>();
        for (Object[] row : rows) {
            long count = (Long) row[1];
            result.add(new CategoryCount((String) row[0], count, total == 0 ? 0 : round1(count * 100.0 / total)));
        }
        return result;
    }

    // ------------------------------------------------------------------ employees / appraisals / PIP

    private long headcount(LocalDate date, String department) {
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(date));
        String sql = "SELECT COUNT(*) FROM employee e LEFT JOIN pms_department_group_v dg ON dg.department_id = e.department_id"
                + " WHERE " + ELIGIBLE + " AND (e.date_of_joining IS NULL OR e.date_of_joining <= ?)"
                + deptClause(department, args, EMP_GROUP);
        Long count = jdbc.queryForObject(sql, Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    private double[] completedAppraisals(LocalDate date, String department) {
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(date));
        String sql = "SELECT COUNT(*), NVL(AVG(a.final_rating), 0) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + " LEFT JOIN pms_department_group_v dg ON dg.department_id = e.department_id"
                + " WHERE a.stage = 'COMPLETED' AND a.completed_at <= ? AND " + ELIGIBLE
                + deptClause(department, args, EMP_GROUP);
        return jdbc.queryForObject(sql, (rs, n) -> new double[]{rs.getLong(1), round2(rs.getDouble(2))}, args.toArray());
    }

    private AppraisalStatus appraisalStatus(String department) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT a.stage, COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + " LEFT JOIN pms_department_group_v dg ON dg.department_id = e.department_id WHERE " + ELIGIBLE
                + deptClause(department, args, EMP_GROUP) + " GROUP BY a.stage";
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            counts.put(rs.getString(1), rs.getLong(2));
        }, args.toArray());
        long total = headcount(LocalDate.now(), department);
        long completed = counts.getOrDefault("COMPLETED", 0L);
        long hr = counts.getOrDefault("HR_REVIEW", 0L);
        long manager = counts.getOrDefault("MANAGER_REVIEW", 0L);
        long self = counts.getOrDefault("SELF_APPRAISAL", 0L);
        long started = completed + hr + manager + self;
        long yetToStart = Math.max(0, total - started);
        return new AppraisalStatus(total, completed, hr, manager, self, yetToStart);
    }

    private List<RatingBucket> ratingDistribution(LocalDate date, String department) {
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(date));
        String sql = "SELECT LEAST(5, GREATEST(1, ROUND(a.final_rating))), COUNT(*) FROM pms_appraisal a"
                + " JOIN employee e ON e.employee_id = a.employee_id"
                + " LEFT JOIN pms_department_group_v dg ON dg.department_id = e.department_id"
                + " WHERE a.stage = 'COMPLETED' AND a.completed_at <= ? AND " + ELIGIBLE
                + deptClause(department, args, EMP_GROUP) + " GROUP BY LEAST(5, GREATEST(1, ROUND(a.final_rating)))";
        Map<Integer, Long> counts = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            counts.put(rs.getInt(1), rs.getLong(2));
        }, args.toArray());
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        List<RatingBucket> buckets = new ArrayList<>();
        for (int rating = 5; rating >= 1; rating--) {
            long count = counts.getOrDefault(rating, 0L);
            buckets.add(new RatingBucket(rating, count, total == 0 ? 0 : round1(count * 100.0 / total)));
        }
        return buckets;
    }

    private List<DepartmentRating> departmentPerformance(LocalDate date, String department) {
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(date));
        String sql = "SELECT " + EMP_GROUP + ", AVG(a.final_rating), COUNT(*) FROM pms_appraisal a"
                + " JOIN employee e ON e.employee_id = a.employee_id"
                + " LEFT JOIN pms_department_group_v dg ON dg.department_id = e.department_id"
                + " WHERE a.stage = 'COMPLETED' AND a.completed_at <= ? AND " + ELIGIBLE
                + deptClause(department, args, EMP_GROUP) + " GROUP BY " + EMP_GROUP;
        List<DepartmentRating> rows = new ArrayList<>();
        jdbc.query(sql, rs -> {
            rows.add(new DepartmentRating(rs.getString(1), round2(rs.getDouble(2)), rs.getLong(3)));
        }, args.toArray());
        rows.sort((a, b) -> Double.compare(b.averageRating(), a.averageRating()));
        return rows;
    }

    private List<TopPerformer> topPerformers(LocalDate date, String department) {
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(date));
        String sql = "SELECT e.employee_id, e.display_name, d.department_name, ds.designation_name, a.final_rating"
                + " FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + " LEFT JOIN department d ON d.department_id = e.department_id"
                + " LEFT JOIN designation ds ON ds.designation_id = e.designation_id"
                + " LEFT JOIN pms_department_group_v dg ON dg.department_id = e.department_id"
                + " WHERE a.stage = 'COMPLETED' AND a.completed_at <= ? AND " + ELIGIBLE
                + deptClause(department, args, EMP_GROUP)
                + " ORDER BY a.final_rating DESC, e.employee_id FETCH FIRST 5 ROWS ONLY";
        return jdbc.query(sql, (rs, n) -> new TopPerformer(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), round2(rs.getDouble(5))), args.toArray());
    }

    private PipOverview pip(String department) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT COUNT(*), SUM(CASE WHEN p.status = 'ACTIVE' THEN 1 ELSE 0 END),"
                + " SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END),"
                + " SUM(CASE WHEN p.outcome = 'IMPROVED' THEN 1 ELSE 0 END),"
                + " SUM(CASE WHEN p.outcome = 'NOT_IMPROVED' THEN 1 ELSE 0 END)"
                + " FROM pms_pip p JOIN employee e ON e.employee_id = p.employee_id"
                + " LEFT JOIN pms_department_group_v dg ON dg.department_id = e.department_id WHERE 1 = 1"
                + deptClause(department, args, EMP_GROUP);
        return jdbc.queryForObject(sql, (rs, n) -> {
            long completed = rs.getLong(3);
            long improved = rs.getLong(4);
            return new PipOverview(rs.getLong(1), rs.getLong(2), completed, improved, rs.getLong(5),
                    completed == 0 ? 0 : round1(improved * 100.0 / completed));
        }, args.toArray());
    }

    private List<Deadline> appraisalDeadlines(String department) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT c.cycle_name, c.dept_group, c.due_date, COUNT(a.appraisal_id),"
                + " SUM(CASE WHEN a.stage <> 'COMPLETED' THEN 1 ELSE 0 END)"
                + " FROM pms_appraisal_cycle c JOIN pms_appraisal a ON a.cycle_id = c.cycle_id"
                + " JOIN employee e ON e.employee_id = a.employee_id"
                + " WHERE c.status = 'ACTIVE' AND c.due_date >= TRUNC(SYSDATE) AND " + ELIGIBLE;
        if (department != null) {
            sql += " AND c.dept_group = ?";
            args.add(department);
        }
        sql += " GROUP BY c.cycle_name, c.dept_group, c.due_date ORDER BY c.due_date, c.cycle_name FETCH FIRST 3 ROWS ONLY";
        return jdbc.query(sql, (rs, n) -> {
            LocalDate due = rs.getDate(3).toLocalDate();
            return new Deadline(rs.getString(1), rs.getString(2), due, ChronoUnit.DAYS.between(LocalDate.now(), due),
                    rs.getLong(5), rs.getLong(4), "APPRAISAL");
        }, args.toArray());
    }

    public List<Deadline> goalDeadlines(String department) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT " + GOAL_GROUP + ", g.due_date, COUNT(*) FROM pms_goal g"
                + " LEFT JOIN pms_department_group_v dg ON dg.department_id = g.department_id"
                + " WHERE g.is_archived = 0 AND g.status <> 'COMPLETED' AND g.due_date >= TRUNC(SYSDATE)"
                + deptClause(department, args, GOAL_GROUP)
                + " GROUP BY " + GOAL_GROUP + ", g.due_date ORDER BY g.due_date, 1 FETCH FIRST 3 ROWS ONLY";
        return jdbc.query(sql, (rs, n) -> {
            LocalDate due = rs.getDate(2).toLocalDate();
            return new Deadline(rs.getString(1) + " goals due", rs.getString(1), due,
                    ChronoUnit.DAYS.between(LocalDate.now(), due), rs.getLong(3), rs.getLong(3), "GOAL");
        }, args.toArray());
    }

    // ------------------------------------------------------------------ overviews

    public DashboardOverview dashboard(LocalDate start, LocalDate end, String department) {
        Period period = period(start, end, department);
        String dept = period.department();
        LocalDate to = period.endDate();
        LocalDate base = period.baselineDate();

        GoalSummary now = goalSummary(to, dept);
        GoalSummary before = goalSummary(base, dept);
        double[] apprNow = completedAppraisals(to, dept);
        double[] apprBefore = completedAppraisals(base, dept);
        DashboardKpis kpis = new DashboardKpis(
                metric(headcount(to, dept), headcount(base, dept)),
                metric(now.total(), before.total()),
                metric(now.completed(), before.completed()),
                metric(now.averageProgress(), before.averageProgress()),
                metric(apprNow[0], apprBefore[0]),
                metric(apprNow[1], apprBefore[1]));

        return new DashboardOverview(period, kpis, now, trend(period.startDate(), to, "DAILY", dept),
                appraisalStatus(dept), ratingDistribution(to, dept), departmentPerformance(to, dept),
                topPerformers(to, dept), pip(dept), appraisalDeadlines(dept), departmentGoals(to, dept));
    }

    public GoalOverview goalOverview(LocalDate start, LocalDate end, String department, List<GoalRow> myGoals,
                                     boolean linked) {
        Period period = period(start, end, department);
        String dept = period.department();
        LocalDate to = period.endDate();
        GoalSummary now = goalSummary(to, dept);
        GoalSummary before = goalSummary(period.baselineDate(), dept);
        GoalKpis kpis = new GoalKpis(
                metric(now.total(), before.total()),
                metric(now.completed(), before.completed()),
                metric(now.inProgress(), before.inProgress()),
                metric(now.behind(), before.behind()),
                metric(now.notStarted(), before.notStarted()),
                metric(now.averageProgress(), before.averageProgress()));
        double completionRate = now.total() == 0 ? 0 : round1(now.completed() * 100.0 / now.total());
        return new GoalOverview(period, kpis, now, trend(period.startDate(), to, "DAILY", dept),
                alignment(to, dept), categories(to, dept), departmentGoals(to, dept), myGoals,
                goalDeadlines(dept), completionRate, linked);
    }

    // ------------------------------------------------------------------ export

    public String dashboardCsv(DashboardOverview o) {
        StringBuilder csv = new StringBuilder();
        csv.append("PMS Dashboard,").append(o.period().startDate()).append(" to ").append(o.period().endDate())
                .append(",Department,").append(o.period().department() == null ? "All" : csv(o.period().department()))
                .append('\n').append('\n');
        csv.append("Metric,Value,Previous,Change,Change %\n");
        appendMetric(csv, "Total Employees", o.kpis().totalEmployees());
        appendMetric(csv, "Goals Assigned", o.kpis().goalsAssigned());
        appendMetric(csv, "Goals Completed", o.kpis().goalsCompleted());
        appendMetric(csv, "Average Goal Progress %", o.kpis().averageProgress());
        appendMetric(csv, "Appraisals Completed", o.kpis().appraisalsCompleted());
        appendMetric(csv, "Average Rating", o.kpis().averageRating());
        csv.append('\n').append("Department,Total Goals,Completed,In Progress,Behind,Not Started,Avg Progress %\n");
        for (DepartmentGoalRow row : o.departmentGoals()) {
            csv.append(csv(row.department())).append(',').append(row.total()).append(',').append(row.completed())
                    .append(',').append(row.inProgress()).append(',').append(row.behind()).append(',')
                    .append(row.notStarted()).append(',').append(row.averageProgress()).append('\n');
        }
        csv.append('\n').append("Rating,Employees,Percent\n");
        for (RatingBucket bucket : o.ratingDistribution()) {
            csv.append(bucket.rating()).append(',').append(bucket.count()).append(',').append(bucket.percent()).append('\n');
        }
        return csv.toString();
    }

    private static void appendMetric(StringBuilder csv, String name, Metric m) {
        csv.append(csv(name)).append(',').append(m.value()).append(',').append(m.previous()).append(',')
                .append(m.change()).append(',').append(m.changePercent() == null ? "" : m.changePercent()).append('\n');
    }

    /** Escapes a CSV cell and neutralises spreadsheet formula injection. */
    public static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
