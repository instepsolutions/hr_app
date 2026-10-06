package com.hrms.pms;

import com.hrms.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class AppraisalSummaryService {

    private static final String ELIGIBLE = "e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')";
    private static final String BASE = " FROM pms_appraisal a JOIN employee e ON e.employee_id=a.employee_id"
            + " JOIN pms_appraisal_cycle c ON c.cycle_id=a.cycle_id LEFT JOIN department d ON d.department_id=e.department_id"
            + " LEFT JOIN designation ds ON ds.designation_id=e.designation_id LEFT JOIN employee manager ON manager.employee_id=e.reporting_manager_id";

    private final JdbcTemplate jdbc;
    private final PmsAccess access;
    private final PmsReviewService reviews;

    public AppraisalSummaryService(JdbcTemplate jdbc, PmsAccess access, PmsReviewService reviews) {
        this.jdbc = jdbc;
        this.access = access;
        this.reviews = reviews;
    }

    public Map<String, Object> overview(Map<String, String> filters) {
        requireHr();
        List<Object> args = new ArrayList<>();
        String where = where(filters, args, true);
        long total = count("SELECT COUNT(DISTINCT e.employee_id)" + BASE + where, args.toArray());
        long completed = count("SELECT COUNT(DISTINCT e.employee_id)" + BASE + where
                + " AND (a.stage='COMPLETED' OR a.final_rating IS NOT NULL)", args.toArray());
        long inProgress = count("SELECT COUNT(DISTINCT e.employee_id)" + BASE + where
                + " AND a.stage IN ('SELF_APPRAISAL','MANAGER_REVIEW','HR_REVIEW') AND a.final_rating IS NULL", args.toArray());
        long yetToStart = count("SELECT COUNT(DISTINCT e.employee_id)" + BASE + where
                + " AND a.stage='YET_TO_START' AND a.final_rating IS NULL", args.toArray());
        Double average = jdbc.queryForObject("SELECT AVG(a.final_rating)" + BASE + where + " AND a.final_rating IS NOT NULL", Double.class, args.toArray());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEmployees", total); summary.put("completed", completed); summary.put("inProgress", inProgress);
        summary.put("yetToStart", yetToStart); summary.put("completionRate", percent(completed, total));
        summary.put("averageRating", round2(average));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("status", List.of(Map.of("status", "Completed", "count", completed),
                Map.of("status", "In Progress", "count", inProgress), Map.of("status", "Yet to Start", "count", yetToStart)));
        result.put("ratingDistribution", ratingDistribution(filters));
        result.put("departments", departmentSummary(filters));
        result.put("topRated", ratingRows(filters, true));
        result.put("lowRated", ratingRows(filters, false));
        result.put("trend", trend(filters));
        result.put("categories", categorySummary(filters));
        result.put("reviewers", reviewerSummary(filters));
        result.put("timeline", timeline(filters));
        result.put("cycles", cycles());
        result.put("insights", insights(summary, result));
        result.put("lowRatingThreshold", number(filters.get("lowRatingThreshold"), 2.5));
        return result;
    }

    public List<Map<String, Object>> list(Map<String, String> filters) {
        requireHr();
        List<Object> args = new ArrayList<>();
        String where = where(filters, args, true);
        String rating = filters.get("rating");
        if (hasText(rating)) {
            where += " AND ROUND(NVL(a.final_rating,NVL(a.hr_rating,NVL(a.manager_rating,a.self_rating)))) = ?";
            args.add(Integer.valueOf(rating));
        }
        return jdbc.query("SELECT a.appraisal_id,e.employee_id,e.employee_code,e.display_name,d.department_name,ds.designation_name,"
                        + "manager.display_name,c.cycle_id,c.cycle_name,a.stage,a.self_status,a.self_rating,a.manager_status,a.manager_rating,"
                        + "a.hr_status,a.hr_rating,a.final_rating,a.final_recommendation,c.due_date" + BASE + where
                        + " ORDER BY CASE WHEN a.final_rating IS NULL THEN 1 ELSE 0 END,a.final_rating DESC,e.display_name",
                (rs, index) -> summaryRow(rs), args.toArray());
    }

    public Map<String, Object> detail(long appraisalId) {
        requireHr();
        Map<String, Object> detail = reviews.hrReview(appraisalId);
        Object hrValue = detail.get("hrReview");
        if (hrValue instanceof Map<?, ?> hrData) {
            detail.put("finalRecommendation", hrData.get("finalRecommendation"));
        }
        List<Map<String, Object>> pips = jdbc.query("SELECT pip_id,status FROM pms_pip WHERE related_appraisal_id=? ORDER BY created_at DESC FETCH FIRST 1 ROWS ONLY",
                (rs, index) -> Map.of("pipId", rs.getLong(1), "status", rs.getString(2)), appraisalId);
        if (!pips.isEmpty()) {
            detail.put("pipId", pips.get(0).get("pipId"));
            detail.put("pipStatus", pips.get(0).get("status"));
        }
        detail.put("performanceDecision", decision(detail));
        return detail;
    }

    public String export(Map<String, String> filters) {
        List<Map<String, Object>> rows = list(filters);
        StringBuilder csv = new StringBuilder("Employee ID,Employee,Department,Designation,Manager,Cycle,Self Rating,Manager Rating,HR Rating,Final Rating,Recommendation,Stage\r\n");
        for (Map<String, Object> row : rows) {
            appendCsv(csv, row.get("employeeCode"), row.get("employeeName"), row.get("department"), row.get("designation"),
                    row.get("manager"), row.get("cycleName"), row.get("selfRating"), row.get("managerRating"),
                    row.get("hrRating"), row.get("finalRating"), row.get("recommendation"), row.get("stage"));
        }
        return csv.toString();
    }

    private List<Map<String, Object>> ratingDistribution(Map<String, String> filters) {
        List<Object> args = new ArrayList<>(); String where = where(filters, args, true);
        Map<Integer, Long> counts = new LinkedHashMap<>();
        jdbc.query("SELECT ROUND(a.final_rating),COUNT(*)" + BASE + where + " AND a.final_rating IS NOT NULL GROUP BY ROUND(a.final_rating)",
                rs -> { counts.put(rs.getInt(1), rs.getLong(2)); }, args.toArray());
        List<Map<String, Object>> result = new ArrayList<>();
        for (int rating = 1; rating <= 5; rating++) result.add(Map.of("rating", rating, "count", counts.getOrDefault(rating, 0L)));
        return result;
    }

    private List<Map<String, Object>> departmentSummary(Map<String, String> filters) {
        List<Object> args = new ArrayList<>(); String where = where(filters, args, true);
        return jdbc.query("SELECT NVL(d.department_name,'Others'),COUNT(DISTINCT e.employee_id),"
                        + "COUNT(DISTINCT CASE WHEN a.stage='COMPLETED' OR a.final_rating IS NOT NULL THEN e.employee_id END),"
                        + "COUNT(DISTINCT CASE WHEN a.stage IN ('SELF_APPRAISAL','MANAGER_REVIEW','HR_REVIEW') AND a.final_rating IS NULL THEN e.employee_id END),"
                        + "COUNT(DISTINCT CASE WHEN a.stage='YET_TO_START' AND a.final_rating IS NULL THEN e.employee_id END),"
                        + "NVL(AVG(a.final_rating),0)" + BASE + where + " GROUP BY NVL(d.department_name,'Others') ORDER BY 1",
                (rs, index) -> {
                    Map<String, Object> row = new LinkedHashMap<>(); long total = rs.getLong(2), complete = rs.getLong(3);
                    row.put("department", rs.getString(1)); row.put("totalEmployees", total); row.put("completed", complete);
                    row.put("inProgress", rs.getLong(4)); row.put("yetToStart", rs.getLong(5));
                    row.put("completionRate", percent(complete, total)); row.put("averageRating", round2(rs.getDouble(6))); return row;
                }, args.toArray());
    }

    private List<Map<String, Object>> ratingRows(Map<String, String> filters, boolean top) {
        List<Object> args = new ArrayList<>(); String where = where(filters, args, true);
        if (!top) { where += " AND a.final_rating < ?"; args.add(number(filters.get("lowRatingThreshold"), 2.5)); }
        else where += " AND a.final_rating IS NOT NULL";
        String ordering = top ? "DESC" : "ASC";
        return jdbc.query("SELECT a.appraisal_id,e.employee_id,e.employee_code,e.display_name,d.department_name,ds.designation_name,"
                        + "manager.display_name,c.cycle_id,c.cycle_name,a.stage,a.self_status,a.self_rating,a.manager_status,a.manager_rating,"
                        + "a.hr_status,a.hr_rating,a.final_rating,a.final_recommendation,c.due_date" + BASE + where
                        + " ORDER BY a.final_rating " + ordering + ",e.display_name FETCH FIRST 10 ROWS ONLY",
                (rs, index) -> summaryRow(rs), args.toArray());
    }

    private List<Map<String, Object>> trend(Map<String, String> filters) {
        List<Object> args = new ArrayList<>(); String where = where(filters, args, true);
        return jdbc.query("SELECT TO_CHAR(TRUNC(a.completed_at,'MM'),'Mon YYYY'),TRUNC(a.completed_at,'MM'),AVG(a.final_rating),COUNT(*)"
                        + BASE + where + " AND a.final_rating IS NOT NULL AND a.completed_at IS NOT NULL"
                        + " GROUP BY TRUNC(a.completed_at,'MM') ORDER BY TRUNC(a.completed_at,'MM') FETCH FIRST 12 ROWS ONLY",
                (rs, index) -> Map.of("label", rs.getString(1), "date", rs.getDate(2).toLocalDate().toString(),
                        "averageRating", round2(rs.getDouble(3)), "count", rs.getLong(4)), args.toArray());
    }

    private List<Map<String, Object>> categorySummary(Map<String, String> filters) {
        List<Object> args = new ArrayList<>(); String where = where(filters, args, true);
        double excellent = number(filters.get("excellentMin"), 4.5), good = number(filters.get("goodMin"), 3.5);
        double average = number(filters.get("averageMin"), 2.5), below = number(filters.get("belowAverageMin"), 1.5);
        Map<String, Long> values = new LinkedHashMap<>();
        String category = "CASE WHEN a.final_rating>=? THEN 'Excellent' WHEN a.final_rating>=? THEN 'Good' "
            + "WHEN a.final_rating>=? THEN 'Average' WHEN a.final_rating>=? THEN 'Below Average' ELSE 'Poor' END";
        jdbc.query("SELECT rating_category,COUNT(*) FROM (SELECT " + category + " AS rating_category"
                + BASE + where + " AND a.final_rating IS NOT NULL) GROUP BY rating_category",
                rs -> { values.put(rs.getString(1), rs.getLong(2)); },
            concat(new Object[]{excellent, good, average, below}, args.toArray()).toArray());
        long total = values.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : List.of("Excellent", "Good", "Average", "Below Average", "Poor")) {
            long count = values.getOrDefault(name, 0L); double from = switch (name) { case "Excellent" -> excellent; case "Good" -> good; case "Average" -> average; case "Below Average" -> below; default -> 1; };
            double to = switch (name) { case "Excellent" -> 5; case "Good" -> excellent; case "Average" -> good; case "Below Average" -> average; default -> below; };
            result.add(Map.of("name", name, "count", count, "percent", total == 0 ? 0 : round1(count * 100.0 / total), "minimum", from, "maximum", to));
        }
        return result;
    }

    private List<Map<String, Object>> reviewerSummary(Map<String, String> filters) {
        List<Object> args = new ArrayList<>(); String where = where(filters, args, true);
        return jdbc.query("SELECT NVL(reviewer.display_name,'Unassigned'),COUNT(*),"
                        + "SUM(CASE WHEN a.hr_status IN ('FINALIZED','COMPLETED') THEN 1 ELSE 0 END),"
                        + "SUM(CASE WHEN a.hr_status IN ('PENDING','IN_PROGRESS','UNDER_CALIBRATION') THEN 1 ELSE 0 END)" + BASE
                        + " LEFT JOIN employee reviewer ON reviewer.employee_id=a.assigned_hr_reviewer_id" + where
                        + " GROUP BY reviewer.display_name ORDER BY COUNT(*) DESC",
                (rs, index) -> Map.of("reviewer", rs.getString(1), "assigned", rs.getLong(2), "completed", rs.getLong(3), "pending", rs.getLong(4)), args.toArray());
    }

    private List<Map<String, Object>> timeline(Map<String, String> filters) {
        List<Object> args = new ArrayList<>(); String where = where(filters, args, true);
        List<Long> cycleIds = jdbc.query("SELECT DISTINCT c.cycle_id" + BASE + where + " FETCH FIRST 1 ROWS ONLY", (rs, index) -> rs.getLong(1), args.toArray());
        if (cycleIds.isEmpty()) return List.of();
        return jdbc.query("SELECT stage_code,start_date,end_date FROM pms_appraisal_timeline WHERE cycle_id=? ORDER BY CASE stage_code WHEN 'SELF_APPRAISAL' THEN 1 WHEN 'MANAGER_REVIEW' THEN 2 WHEN 'HR_REVIEW' THEN 3 ELSE 4 END",
                (rs, index) -> Map.of("stageCode", rs.getString(1), "startDate", rs.getDate(2).toLocalDate().toString(), "endDate", rs.getDate(3).toLocalDate().toString()), cycleIds.get(0));
    }

    private List<Map<String, Object>> cycles() {
        return jdbc.query("SELECT cycle_id,cycle_name,start_date,due_date FROM pms_appraisal_cycle WHERE status='ACTIVE' ORDER BY start_date DESC",
                (rs, index) -> Map.of("cycleId", rs.getLong(1), "cycleName", rs.getString(2),
                        "startDate", rs.getDate(3).toLocalDate().toString(), "endDate", rs.getDate(4).toLocalDate().toString()));
    }

    private List<Map<String, Object>> insights(Map<String, Object> summary, Map<String, Object> report) {
        List<Map<String, Object>> insights = new ArrayList<>();
        long lowCount = ((List<Map<String, Object>>) report.get("lowRated")).size();
        insights.add(Map.of("label", "Completion rate", "value", summary.get("completionRate") + "%"));
        insights.add(Map.of("label", "Low-rated employees", "value", lowCount));
        @SuppressWarnings("unchecked") List<Map<String, Object>> departments = (List<Map<String, Object>>) report.get("departments");
        Map<String, Object> topDepartment = departments.stream().max((left, right) -> Double.compare(number(left.get("averageRating"), 0), number(right.get("averageRating"), 0))).orElse(null);
        if (topDepartment != null) insights.add(Map.of("label", "Highest average department", "value", topDepartment.get("department")));
        return insights;
    }

    private Map<String, Object> summaryRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("appraisalId", rs.getLong(1)); row.put("employeeId", rs.getLong(2)); row.put("employeeCode", rs.getString(3));
        row.put("employeeName", rs.getString(4)); row.put("department", rs.getString(5)); row.put("designation", rs.getString(6));
        row.put("manager", rs.getString(7)); row.put("cycleId", rs.getLong(8)); row.put("cycleName", rs.getString(9));
        row.put("stage", rs.getString(10)); row.put("selfStatus", rs.getString(11)); row.put("selfRating", rs.getBigDecimal(12));
        row.put("managerStatus", rs.getString(13)); row.put("managerRating", rs.getBigDecimal(14)); row.put("hrStatus", rs.getString(15));
        row.put("hrRating", rs.getBigDecimal(16)); row.put("finalRating", rs.getBigDecimal(17)); row.put("recommendation", rs.getString(18));
        row.put("dueDate", rs.getDate(19) == null ? null : rs.getDate(19).toLocalDate().toString());
        return row;
    }

    private Map<String, Object> decision(Map<String, Object> detail) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("performanceDecision", detail.get("finalRating") == null ? "PENDING" : number(detail.get("finalRating"), 0) >= 4.5 ? "EXCEEDS_EXPECTATIONS" : number(detail.get("finalRating"), 0) >= 3.5 ? "MEETS_EXPECTATIONS" : number(detail.get("finalRating"), 0) >= 2.5 ? "PARTIALLY_MEETS_EXPECTATIONS" : "DOES_NOT_MEET_EXPECTATIONS");
        result.put("recommendation", detail.get("finalRecommendation"));
        return result;
    }

    private String where(Map<String, String> filters, List<Object> args, boolean includeRating) {
        StringBuilder where = new StringBuilder(" WHERE ").append(ELIGIBLE);
        if (hasText(filters.get("cycleId"))) add(where, args, "a.cycle_id=?", Long.valueOf(filters.get("cycleId")));
        if (hasText(filters.get("departmentId"))) add(where, args, "e.department_id=?", Long.valueOf(filters.get("departmentId")));
        if (hasText(filters.get("search"))) {
            where.append(" AND (LOWER(e.display_name) LIKE ? OR LOWER(e.employee_code) LIKE ? OR LOWER(NVL(d.department_name,' ')) LIKE ? OR LOWER(NVL(ds.designation_name,' ')) LIKE ? OR LOWER(NVL(manager.display_name,' ')) LIKE ?)");
            String pattern = "%" + filters.get("search").trim().toLowerCase(Locale.ROOT) + "%";
            for (int index = 0; index < 5; index++) args.add(pattern);
        }
        if (!access.canManage()) {
            where.append(" AND (a.assigned_hr_reviewer_id = ? OR a.assigned_hr_reviewer_id IS NULL)");
            args.add(access.employeeIdOrNull());
        }
        return where.toString();
    }

    private void requireHr() { if (!access.canReviewHr()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "HR review access is required."); }
    private int count(String sql, Object... args) { Integer value = jdbc.queryForObject(sql, Integer.class, args); return value == null ? 0 : value; }
    private void add(StringBuilder where, List<Object> args, String condition, Object value) { where.append(" AND ").append(condition); args.add(value); }
    private double percent(long value, long total) { return total == 0 ? 0 : round1(value * 100.0 / total); }
    private double round1(double value) { return Math.round(value * 10.0) / 10.0; }
    private double round2(Double value) { return value == null ? 0 : Math.round(value * 100.0) / 100.0; }
    private double number(String value, double fallback) { return value == null || value.isBlank() ? fallback : Double.parseDouble(value); }
    private double number(Object value, double fallback) { return value == null || value.toString().isBlank() ? fallback : Double.parseDouble(value.toString()); }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private List<Object> concat(Object[]... arrays) { List<Object> result = new ArrayList<>(); for (Object[] array : arrays) result.addAll(List.of(array)); return result; }
    private void appendCsv(StringBuilder csv, Object... values) { for (int index = 0; index < values.length; index++) { if (index > 0) csv.append(','); String value = Objects.toString(values[index], ""); if (value.startsWith("=") || value.startsWith("+") || value.startsWith("-") || value.startsWith("@")) value = "'" + value; csv.append('"').append(value.replace("\"", "\"\"")).append('"'); } csv.append("\r\n"); }
}