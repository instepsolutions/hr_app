package com.hrms.pms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class PipManagementService {

    private static final String ELIGIBLE = "e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')";
    private static final String SELECT = "SELECT p.pip_id,p.employee_id,e.employee_code,e.display_name,d.department_name,"
            + "ds.designation_name,p.manager_id,manager.display_name,p.assigned_hr_reviewer_id,reviewer.display_name,"
            + "p.status,p.outcome,p.reason_code,p.reason,p.start_date,p.end_date,p.overall_progress,p.next_review_date,"
            + "p.related_appraisal_id,c.cycle_name,p.review_frequency,p.created_at,(p.end_date-p.start_date),"
            + "p.current_performance,p.expected_performance,p.performance_gap,p.evidence,p.support_required,p.workflow_data,p.extension_reason,p.outcome_reason"
            + " FROM pms_pip p JOIN employee e ON e.employee_id=p.employee_id"
            + " LEFT JOIN department d ON d.department_id=e.department_id"
            + " LEFT JOIN designation ds ON ds.designation_id=e.designation_id"
            + " LEFT JOIN employee manager ON manager.employee_id=p.manager_id"
            + " LEFT JOIN employee reviewer ON reviewer.employee_id=p.assigned_hr_reviewer_id"
            + " LEFT JOIN pms_appraisal a ON a.appraisal_id=p.related_appraisal_id"
            + " LEFT JOIN pms_appraisal_cycle c ON c.cycle_id=a.cycle_id";

    private final JdbcTemplate jdbc;
    private final PmsAccess access;
    private final ObjectMapper mapper;

    public PipManagementService(JdbcTemplate jdbc, PmsAccess access, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.access = access;
        this.mapper = mapper;
    }

    public Map<String, Object> overview(Long departmentId) {
        if (!access.canManage()) return scopedOverview(list(Map.of(), departmentId));
        String filter = " FROM pms_pip p JOIN employee e ON e.employee_id=p.employee_id WHERE " + ELIGIBLE
                + (departmentId == null ? "" : " AND e.department_id=?");
        Object[] args = departmentId == null ? new Object[0] : new Object[]{departmentId};
        Map<String, Object> summary = jdbc.queryForObject("SELECT COUNT(*),"
                        + " SUM(CASE WHEN p.status IN ('ACTIVE','PENDING_REVIEW') THEN 1 ELSE 0 END),"
                        + " SUM(CASE WHEN p.status='COMPLETED' THEN 1 ELSE 0 END),"
                        + " SUM(CASE WHEN p.status='DRAFT' THEN 1 ELSE 0 END),"
                        + " SUM(CASE WHEN p.outcome IN ('SUCCESSFUL','IMPROVED') THEN 1 ELSE 0 END),"
                        + " SUM(CASE WHEN p.outcome IN ('UNSUCCESSFUL','NOT_IMPROVED') THEN 1 ELSE 0 END),"
                        + " NVL(AVG(CASE WHEN p.status IN ('ACTIVE','PENDING_REVIEW') THEN p.overall_progress END),0)"
                        + filter, (rs, row) -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("total", rs.getLong(1)); result.put("active", rs.getLong(2));
                    result.put("completed", rs.getLong(3)); result.put("draft", rs.getLong(4));
                    result.put("successful", rs.getLong(5)); result.put("unsuccessful", rs.getLong(6));
                    result.put("averageProgress", round1(rs.getDouble(7)));
                    long total = rs.getLong(1), completed = rs.getLong(3);
                    result.put("successRate", completed == 0 ? 0 : round1(rs.getLong(5) * 100.0 / completed));
                    result.put("atRisk", count("SELECT COUNT(*)" + filter + " AND p.status IN ('ACTIVE','PENDING_REVIEW')"
                            + " AND (p.end_date < TRUNC(SYSDATE) OR p.next_review_date < TRUNC(SYSDATE))", args));
                    return result;
                }, args);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("status", statusCounts(departmentId));
        result.put("byDepartment", departmentCounts(departmentId));
        result.put("outcomes", outcomeCounts(departmentId));
        result.put("duration", durationCounts(departmentId));
        result.put("progress", progressCounts(departmentId));
        result.put("trend", trend(departmentId));
        result.put("attention", list(Map.of("status", "ACTIVE", "dueBefore", LocalDate.now().plusDays(14).toString()), departmentId));
        result.put("cycles", cycles());
        return result;
    }

    private Map<String, Object> scopedOverview(List<Map<String, Object>> rows) {
        Map<String, Object> summary = new LinkedHashMap<>();
        long active = rows.stream().filter(row -> List.of("ACTIVE", "PENDING_REVIEW").contains(row.get("status"))).count();
        long completed = rows.stream().filter(row -> "COMPLETED".equals(row.get("status"))).count();
        long drafts = rows.stream().filter(row -> "DRAFT".equals(row.get("status"))).count();
        long successful = rows.stream().filter(row -> List.of("SUCCESSFUL", "IMPROVED").contains(row.get("outcome"))).count();
        long unsuccessful = rows.stream().filter(row -> List.of("UNSUCCESSFUL", "NOT_IMPROVED").contains(row.get("outcome"))).count();
        long atRisk = rows.stream().filter(row -> List.of("ACTIVE", "PENDING_REVIEW").contains(row.get("status"))
                && (hasText((String) row.get("endDate")) && LocalDate.parse((String) row.get("endDate")).isBefore(LocalDate.now())
                || hasText((String) row.get("nextReviewDate")) && LocalDate.parse((String) row.get("nextReviewDate")).isBefore(LocalDate.now()))).count();
        double avg = rows.stream().filter(row -> row.get("overallProgress") != null)
                .mapToDouble(row -> number(row.get("overallProgress"), 0)).average().orElse(0);
        summary.put("total", rows.size()); summary.put("active", active); summary.put("completed", completed);
        summary.put("draft", drafts); summary.put("successful", successful); summary.put("unsuccessful", unsuccessful);
        summary.put("averageProgress", round1(avg)); summary.put("atRisk", atRisk);
        summary.put("successRate", completed == 0 ? 0 : round1(successful * 100.0 / completed));
        Map<String, Long> statuses = new LinkedHashMap<>(); Map<String, Long> outcomes = new LinkedHashMap<>();
        Map<String, Long> departments = new LinkedHashMap<>(); Map<String, Long> durations = new LinkedHashMap<>();
        Map<String, Long> progress = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            statuses.merge(Objects.toString(row.get("status"), "UNKNOWN"), 1L, Long::sum);
            outcomes.merge(Objects.toString(row.get("outcome"), "OPEN"), 1L, Long::sum);
            departments.merge(Objects.toString(row.get("department"), "Others"), 1L, Long::sum);
            long duration = (long) number(row.get("durationDays"), 0);
            durations.merge(duration <= 30 ? "30 DAYS" : duration <= 60 ? "60 DAYS" : duration <= 90 ? "90 DAYS" : "CUSTOM", 1L, Long::sum);
            double value = number(row.get("overallProgress"), 0);
            progress.merge(value <= 0 ? "NOT STARTED" : value < 25 ? "EARLY STAGE" : value < 75 ? "IN PROGRESS" : value < 100 ? "NEAR COMPLETION" : "COMPLETED", 1L, Long::sum);
        }
        List<Map<String, Object>> byDepartment = departments.entrySet().stream().map(entry -> Map.<String, Object>of("department", entry.getKey(), "total", entry.getValue())).toList();
        Map<String, Object> result = new LinkedHashMap<>(); result.put("summary", summary);
        result.put("status", statusRows(statuses)); result.put("outcomes", countRows(outcomes, "outcome"));
        result.put("duration", countRows(durations, "duration")); result.put("progress", countRows(progress, "range"));
        result.put("byDepartment", byDepartment); result.put("trend", List.of()); result.put("attention", rows.stream().filter(row -> atRiskRow(row)).toList());
        result.put("cycles", cycles()); return result;
    }

    private List<Map<String, Object>> statusRows(Map<String, Long> values) { return countRows(values, "status"); }
    private List<Map<String, Object>> countRows(Map<String, Long> values, String key) {
        return values.entrySet().stream().map(entry -> Map.<String, Object>of(key, entry.getKey(), "count", entry.getValue())).toList();
    }

    private boolean atRiskRow(Map<String, Object> row) {
        return List.of("ACTIVE", "PENDING_REVIEW").contains(row.get("status"))
                && (hasText((String) row.get("endDate")) && LocalDate.parse((String) row.get("endDate")).isBefore(LocalDate.now())
                || hasText((String) row.get("nextReviewDate")) && LocalDate.parse((String) row.get("nextReviewDate")).isBefore(LocalDate.now()));
    }

    public List<Map<String, Object>> list(Map<String, String> filters, Long departmentId) {
        StringBuilder where = new StringBuilder(" WHERE ").append(ELIGIBLE);
        List<Object> args = new ArrayList<>();
        if (departmentId != null) add(where, args, "e.department_id = ?", departmentId);
        addFilter(where, args, "p.status = ?", filters.get("status"));
        addFilter(where, args, "p.outcome = ?", filters.get("outcome"));
        addFilter(where, args, "p.reason_code = ?", filters.get("reasonCode"));
        addFilter(where, args, "p.manager_id = ?", numberOrNull(filters.get("managerId")));
        addFilter(where, args, "p.assigned_hr_reviewer_id = ?", numberOrNull(filters.get("reviewerId")));
        addFilter(where, args, "e.department_id = ?", numberOrNull(filters.get("departmentId")));
        addFilter(where, args, "a.cycle_id = ?", numberOrNull(filters.get("cycleId")));
        if (hasText(filters.get("startDate"))) add(where, args, "p.start_date >= ?", Date.valueOf(LocalDate.parse(filters.get("startDate"))));
        if (hasText(filters.get("endDate"))) add(where, args, "p.end_date <= ?", Date.valueOf(LocalDate.parse(filters.get("endDate"))));
        if (hasText(filters.get("dueBefore"))) add(where, args, "p.next_review_date <= ?", Date.valueOf(LocalDate.parse(filters.get("dueBefore"))));
        if (hasText(filters.get("search"))) {
            where.append(" AND (LOWER(e.display_name) LIKE ? OR LOWER(e.employee_code) LIKE ? OR LOWER(NVL(d.department_name,' ')) LIKE ?"
                    + " OR LOWER(NVL(manager.display_name,' ')) LIKE ? OR LOWER(NVL(ds.designation_name,' ')) LIKE ?)");
            String pattern = "%" + filters.get("search").trim().toLowerCase(Locale.ROOT) + "%";
            for (int index = 0; index < 5; index++) args.add(pattern);
        }
        applyScope(where, args);
        where.append(" ORDER BY CASE p.status WHEN 'ACTIVE' THEN 1 WHEN 'PENDING_REVIEW' THEN 2 WHEN 'DRAFT' THEN 3 ELSE 4 END, p.end_date, e.display_name");
        return jdbc.query(SELECT + where, (rs, row) -> pipMap(rs), args.toArray());
    }

    @Transactional
    public Map<String, Object> createDraft(Map<String, Object> payload) {
        requireHr();
        long employeeId = requiredLong(payload.get("employeeId"), "Select an employee.");
        Long managerId = longValue(payload.get("managerId"));
        if (managerId == null) managerId = nullableLong("SELECT reporting_manager_id FROM employee WHERE employee_id=?", employeeId);
        LocalDate start = date(payload.get("startDate"), LocalDate.now());
        int duration = (int) Math.max(1, number(payload.get("durationDays"), 30));
        LocalDate end = date(payload.get("endDate"), start.plusDays(duration));
        validateDates(start, end);
        long id = nextId("pms_pip_seq");
        jdbc.update("INSERT INTO pms_pip (pip_id,employee_id,manager_id,status,start_date,end_date,reason,reason_code,"
                        + "related_appraisal_id,assigned_hr_reviewer_id,overall_progress,review_frequency,next_review_date,"
                        + "current_performance,expected_performance,performance_gap,evidence,support_required,workflow_data,created_by,updated_by)"
                        + " VALUES (?,?,?,'DRAFT',?,?,?,?,?, ?,0,?,?,?,?,?,?,?,?,?,?)",
                id, employeeId, typed(managerId, Types.NUMERIC), Date.valueOf(start), Date.valueOf(end), typed(text(payload.get("reason")), Types.VARCHAR),
                typed(text(payload.get("reasonCode")), Types.VARCHAR), typed(longValue(payload.get("relatedAppraisalId")), Types.NUMERIC),
                typed(longValue(payload.get("reviewerId")), Types.NUMERIC), text(payload.get("reviewFrequency")) == null ? "MONTHLY" : text(payload.get("reviewFrequency")),
                typed(sqlDate(payload.get("nextReviewDate")), Types.DATE), typed(text(payload.get("currentPerformance")), Types.CLOB),
                typed(text(payload.get("expectedPerformance")), Types.CLOB), typed(text(payload.get("performanceGap")), Types.CLOB),
                typed(text(payload.get("evidence")), Types.CLOB), typed(text(payload.get("supportRequired")), Types.CLOB),
                typed(json(payload), Types.CLOB), access.username(), access.username());
        persistChildren(id, payload);
        audit(id, "PIP_CREATED", null, "DRAFT");
        return detail(id);
    }

    @Transactional
    public Map<String, Object> update(long pipId, Map<String, Object> payload) {
        PipRow row = requireAccess(pipId, true);
        if (List.of("COMPLETED", "CANCELLED").contains(row.status())) conflict("Closed PIPs cannot be edited.");
        LocalDate start = date(payload.get("startDate"), LocalDate.parse(row.startDate()));
        LocalDate end = date(payload.get("endDate"), LocalDate.parse(row.endDate()));
        validateDates(start, end);
        jdbc.update("UPDATE pms_pip SET manager_id=?,assigned_hr_reviewer_id=?,start_date=?,end_date=?,reason=?,reason_code=?,"
                        + "related_appraisal_id=?,review_frequency=?,next_review_date=?,current_performance=?,expected_performance=?,"
                        + "performance_gap=?,evidence=?,support_required=?,workflow_data=?,updated_at=SYSTIMESTAMP,updated_by=? WHERE pip_id=?",
                typed(longValue(payload.get("managerId")) == null ? row.managerId() : longValue(payload.get("managerId")), Types.NUMERIC),
                typed(longValue(payload.get("reviewerId")), Types.NUMERIC), Date.valueOf(start), Date.valueOf(end), typed(text(payload.get("reason")), Types.VARCHAR),
                typed(text(payload.get("reasonCode")), Types.VARCHAR), typed(longValue(payload.get("relatedAppraisalId")), Types.NUMERIC),
                text(payload.get("reviewFrequency")) == null ? "MONTHLY" : text(payload.get("reviewFrequency")),
                typed(sqlDate(payload.get("nextReviewDate")), Types.DATE), typed(text(payload.get("currentPerformance")), Types.CLOB),
                typed(text(payload.get("expectedPerformance")), Types.CLOB), typed(text(payload.get("performanceGap")), Types.CLOB),
                typed(text(payload.get("evidence")), Types.CLOB), typed(text(payload.get("supportRequired")), Types.CLOB),
                typed(json(payload), Types.CLOB), access.username(), pipId);
        persistChildren(pipId, payload);
        audit(pipId, "PIP_UPDATED", row.status(), row.status());
        return detail(pipId);
    }

    @Transactional
    public Map<String, Object> start(long pipId) {
        requireHr();
        PipRow row = requireAccess(pipId, true);
        if (!"DRAFT".equals(row.status())) conflict("Only a draft PIP can be started.");
        jdbc.update("UPDATE pms_pip SET status='ACTIVE',updated_at=SYSTIMESTAMP,updated_by=? WHERE pip_id=?", access.username(), pipId);
        audit(pipId, "PIP_STARTED", "DRAFT", "ACTIVE");
        return detail(pipId);
    }

    @Transactional
    public Map<String, Object> addReview(long pipId, Map<String, Object> payload) {
        PipRow row = requireReviewerAccess(pipId);
        if (!List.of("ACTIVE", "PENDING_REVIEW").contains(row.status())) conflict("Only active PIPs can be reviewed.");
        for (Map<String, Object> objective : maps(payload.get("objectives"))) updateObjective(pipId, objective);
        BigDecimal progress = count("SELECT COUNT(*) FROM pms_pip_objective WHERE pip_id=?", pipId) > 0
            ? weightedProgress(pipId) : decimal(payload.get("overallProgress"));
        if (progress == null) progress = BigDecimal.ZERO;
        if (progress.compareTo(BigDecimal.ZERO) < 0 || progress.compareTo(BigDecimal.valueOf(100)) > 0) conflict("Progress must be between 0 and 100.");
        String role = actorRole();
        jdbc.update("INSERT INTO pms_pip_review (review_id,pip_id,reviewer_employee_id,reviewer_name,reviewer_role,review_date,"
                        + "overall_progress,manager_assessment,employee_comments,hr_comments) VALUES (pms_pip_review_seq.NEXTVAL,?,?,?,?,TRUNC(SYSDATE),?,?,?,?)",
                pipId, access.employeeIdOrNull(), access.username(), role, progress,
                text(payload.get("managerAssessment")), text(payload.get("employeeComments")), text(payload.get("hrComments")));
        jdbc.update("UPDATE pms_pip SET overall_progress=?,status='ACTIVE',next_review_date=?,updated_at=SYSTIMESTAMP,updated_by=? WHERE pip_id=?",
            progress, typed(sqlDate(payload.get("nextReviewDate")), Types.DATE), access.username(), pipId);
        audit(pipId, "REVIEW_ADDED", row.status(), "ACTIVE");
        return detail(pipId);
    }

    @Transactional
    public Map<String, Object> extend(long pipId, Map<String, Object> payload) {
        requireHr();
        PipRow row = load(pipId);
        LocalDate newEnd = date(payload.get("newEndDate"), null);
        String reason = text(payload.get("extensionReason"));
        if (newEnd == null || newEnd.isBefore(LocalDate.parse(row.endDate()))) conflict("Choose a new end date after the current end date.");
        if (!hasText(reason)) conflict("An extension reason is required.");
        jdbc.update("UPDATE pms_pip SET end_date=?,outcome='EXTENDED',status='ACTIVE',extension_reason=?,outcome_reason=NULL,"
                        + "updated_at=SYSTIMESTAMP,updated_by=? WHERE pip_id=?",
                Date.valueOf(newEnd), reason, access.username(), pipId);
        audit(pipId, "PIP_EXTENDED", row.endDate(), newEnd.toString());
        return detail(pipId);
    }

    @Transactional
    public Map<String, Object> close(long pipId, Map<String, Object> payload) {
        requireHr();
        PipRow row = load(pipId);
        String outcome = text(payload.get("outcome"));
        if (!Set.of("SUCCESSFUL", "UNSUCCESSFUL", "CLOSED").contains(Objects.toString(outcome, ""))) conflict("Choose a valid PIP outcome.");
        String reason = text(payload.get("reason"));
        if ("UNSUCCESSFUL".equals(outcome) && !hasText(reason)) conflict("Provide a reason for an unsuccessful PIP.");
        String old = row.status() + "/" + Objects.toString(row.outcome(), "OPEN");
        jdbc.update("UPDATE pms_pip SET status='COMPLETED',outcome=?,outcome_reason=?,closed_at=SYSTIMESTAMP,"
                        + "overall_progress=CASE WHEN ?='SUCCESSFUL' THEN 100 ELSE overall_progress END,updated_at=SYSTIMESTAMP,updated_by=? WHERE pip_id=?",
                outcome, reason, outcome, access.username(), pipId);
        audit(pipId, "PIP_CLOSED", old, "COMPLETED/" + outcome);
        return detail(pipId);
    }

    @Transactional
    public Map<String, Object> cancel(long pipId, String reason) {
        requireHr();
        PipRow row = load(pipId);
        if (List.of("COMPLETED", "CANCELLED").contains(row.status())) conflict("This PIP is already closed.");
        jdbc.update("UPDATE pms_pip SET status='CANCELLED',outcome='CLOSED',outcome_reason=?,closed_at=SYSTIMESTAMP,"
                        + "updated_at=SYSTIMESTAMP,updated_by=? WHERE pip_id=?", reason, access.username(), pipId);
        audit(pipId, "PIP_CANCELLED", row.status(), "CANCELLED");
        return detail(pipId);
    }

    @Transactional
    public Map<String, Object> remind(long pipId) {
        PipRow row = requireAccess(pipId, true);
        audit(pipId, "REMINDER_SENT", row.status(), row.status());
        return Map.of("pipId", pipId, "action", "REMINDER_SENT");
    }

    public Map<String, Object> detail(long pipId) {
        PipRow row = requireAccess(pipId, false);
        Map<String, Object> result = row.toMap();
        List<Map<String, Object>> objectives = jdbc.query("SELECT o.objective_id,o.goal_id,g.title,o.kra_id,k.kra_name,o.kpi_id,kp.kpi_name,"
                + "o.title,o.current_state,o.expected_state,o.measurement,o.target_value,o.actual_value,o.weightage,o.progress_percent,"
                + "o.deadline,o.status,o.manager_assessment,o.employee_comments,o.hr_comments FROM pms_pip_objective o "
                + "LEFT JOIN pms_goal g ON g.goal_id=o.goal_id LEFT JOIN pms_kra k ON k.kra_id=o.kra_id "
                + "LEFT JOIN pms_kpi kp ON kp.kpi_id=o.kpi_id WHERE o.pip_id=? ORDER BY o.objective_id",
                (rs, index) -> objectiveMap(rs), pipId);
        List<Map<String, Object>> actions = jdbc.query("SELECT action_id,objective_id,action_text,owner_id,owner_name,due_date,support_required,"
                        + "training_required,success_criteria,status FROM pms_pip_action_plan WHERE pip_id=? ORDER BY due_date,action_id",
                (rs, index) -> actionMap(rs), pipId);
        List<Map<String, Object>> reviews = jdbc.query("SELECT review_id,reviewer_name,reviewer_role,review_date,overall_progress,"
                        + "manager_assessment,employee_comments,hr_comments FROM pms_pip_review WHERE pip_id=? ORDER BY review_date DESC,review_id DESC",
                (rs, index) -> reviewMap(rs), pipId);
        List<Map<String, Object>> history = jdbc.query("SELECT actor_name,actor_role,action_type,old_value,new_value,changed_at FROM pms_pip_audit "
                        + "WHERE pip_id=? ORDER BY changed_at DESC,audit_id DESC",
                (rs, index) -> auditMap(rs), pipId);
        result.put("objectives", objectives); result.put("actionPlans", actions); result.put("reviews", reviews); result.put("history", history);
        result.put("workflowData", parse(row.workflowData()));
        if (row.relatedAppraisalId() != null) result.put("appraisal", appraisalLink(row.relatedAppraisalId()));
        return result;
    }

    public List<Map<String, Object>> templates() {
        requireHr();
        return jdbc.query("SELECT template_id,template_name,duration_days,review_frequency,description FROM pms_pip_template WHERE is_active=1 ORDER BY duration_days",
                (rs, index) -> Map.of("templateId", rs.getLong(1), "name", rs.getString(2), "durationDays", rs.getInt(3),
                        "reviewFrequency", rs.getString(4), "description", Objects.toString(rs.getString(5), "")));
    }

    public String export(Map<String, String> filters, Long departmentId) {
        List<Map<String, Object>> rows = list(filters, departmentId);
        StringBuilder csv = new StringBuilder("PIP ID,Employee ID,Employee,Department,Manager,HR Reviewer,Status,Outcome,Start Date,End Date,Progress,Reason\r\n");
        for (Map<String, Object> row : rows) appendCsv(csv, row.get("pipId"), row.get("employeeCode"), row.get("employeeName"),
                row.get("department"), row.get("manager"), row.get("reviewer"), row.get("status"), row.get("outcome"),
                row.get("startDate"), row.get("endDate"), row.get("overallProgress"), row.get("reason"));
        return csv.toString();
    }

    private List<Map<String, Object>> statusCounts(Long departmentId) {
        return jdbc.query("SELECT p.status,COUNT(*) FROM pms_pip p JOIN employee e ON e.employee_id=p.employee_id WHERE " + ELIGIBLE
                        + (departmentId == null ? "" : " AND e.department_id=?") + " GROUP BY p.status ORDER BY p.status",
                (rs, index) -> Map.of("status", rs.getString(1), "count", rs.getLong(2)), optionalId(departmentId));
    }

    private List<Map<String, Object>> departmentCounts(Long departmentId) {
        return jdbc.query("SELECT d.department_name,COUNT(*),SUM(CASE WHEN p.status IN ('ACTIVE','PENDING_REVIEW') THEN 1 ELSE 0 END),"
                        + "SUM(CASE WHEN p.status='COMPLETED' THEN 1 ELSE 0 END),SUM(CASE WHEN p.end_date<TRUNC(SYSDATE) AND p.status IN ('ACTIVE','PENDING_REVIEW') THEN 1 ELSE 0 END),"
                        + "NVL(AVG(CASE WHEN p.status='COMPLETED' AND p.outcome IN ('SUCCESSFUL','IMPROVED') THEN 100 WHEN p.status='COMPLETED' THEN 0 END),0) "
                        + "FROM pms_pip p JOIN employee e ON e.employee_id=p.employee_id LEFT JOIN department d ON d.department_id=e.department_id "
                        + "WHERE " + ELIGIBLE + (departmentId == null ? "" : " AND e.department_id=?")
                        + " GROUP BY d.department_name ORDER BY COUNT(*) DESC,d.department_name",
                (rs, index) -> Map.of("department", Objects.toString(rs.getString(1), "Others"), "total", rs.getLong(2),
                        "active", rs.getLong(3), "completed", rs.getLong(4), "overdue", rs.getLong(5), "successRate", round1(rs.getDouble(6))), optionalId(departmentId));
    }

    private List<Map<String, Object>> outcomeCounts(Long departmentId) {
        return jdbc.query("SELECT NVL(p.outcome,'OPEN'),COUNT(*) FROM pms_pip p JOIN employee e ON e.employee_id=p.employee_id WHERE " + ELIGIBLE
                        + (departmentId == null ? "" : " AND e.department_id=?") + " GROUP BY p.outcome",
                (rs, index) -> Map.of("outcome", rs.getString(1), "count", rs.getLong(2)), optionalId(departmentId));
    }

    private List<Map<String, Object>> durationCounts(Long departmentId) {
        return jdbc.query("SELECT CASE WHEN p.end_date-p.start_date<=30 THEN '30 DAYS' WHEN p.end_date-p.start_date<=60 THEN '60 DAYS' "
                        + "WHEN p.end_date-p.start_date<=90 THEN '90 DAYS' ELSE 'CUSTOM' END,COUNT(*) FROM pms_pip p JOIN employee e ON e.employee_id=p.employee_id WHERE " + ELIGIBLE
                        + (departmentId == null ? "" : " AND e.department_id=?") + " GROUP BY CASE WHEN p.end_date-p.start_date<=30 THEN '30 DAYS' WHEN p.end_date-p.start_date<=60 THEN '60 DAYS' WHEN p.end_date-p.start_date<=90 THEN '90 DAYS' ELSE 'CUSTOM' END",
                (rs, index) -> Map.of("duration", rs.getString(1), "count", rs.getLong(2)), optionalId(departmentId));
    }

    private List<Map<String, Object>> progressCounts(Long departmentId) {
        return jdbc.query("SELECT CASE WHEN p.overall_progress=0 THEN 'NOT STARTED' WHEN p.overall_progress<25 THEN 'EARLY STAGE' "
                        + "WHEN p.overall_progress<75 THEN 'IN PROGRESS' WHEN p.overall_progress<100 THEN 'NEAR COMPLETION' ELSE 'COMPLETED' END,COUNT(*) "
                        + "FROM pms_pip p JOIN employee e ON e.employee_id=p.employee_id WHERE " + ELIGIBLE
                        + (departmentId == null ? "" : " AND e.department_id=?") + " GROUP BY CASE WHEN p.overall_progress=0 THEN 'NOT STARTED' WHEN p.overall_progress<25 THEN 'EARLY STAGE' WHEN p.overall_progress<75 THEN 'IN PROGRESS' WHEN p.overall_progress<100 THEN 'NEAR COMPLETION' ELSE 'COMPLETED' END",
                (rs, index) -> Map.of("range", rs.getString(1), "count", rs.getLong(2)), optionalId(departmentId));
    }

    private List<Map<String, Object>> trend(Long departmentId) {
        return jdbc.query("SELECT TO_CHAR(TRUNC(p.created_at,'MM'),'Mon YYYY'),COUNT(*),SUM(CASE WHEN p.status='COMPLETED' THEN 1 ELSE 0 END),"
                        + "SUM(CASE WHEN p.end_date<TRUNC(SYSDATE) AND p.status IN ('ACTIVE','PENDING_REVIEW') THEN 1 ELSE 0 END) FROM pms_pip p "
                        + "JOIN employee e ON e.employee_id=p.employee_id WHERE p.created_at>=ADD_MONTHS(TRUNC(SYSDATE,'MM'),-5) AND " + ELIGIBLE
                        + (departmentId == null ? "" : " AND e.department_id=?") + " GROUP BY TRUNC(p.created_at,'MM') ORDER BY TRUNC(p.created_at,'MM')",
                (rs, index) -> Map.of("month", rs.getString(1), "active", rs.getLong(2)-rs.getLong(3), "completed", rs.getLong(3), "overdue", rs.getLong(4)), optionalId(departmentId));
    }

    private List<Map<String, Object>> cycles() {
        return jdbc.query("SELECT cycle_id,cycle_name,start_date,due_date FROM pms_appraisal_cycle WHERE status='ACTIVE' ORDER BY start_date DESC",
                (rs, index) -> Map.of("cycleId", rs.getLong(1), "cycleName", rs.getString(2),
                        "startDate", date(rs, 3), "endDate", date(rs, 4)));
    }

    private Map<String, Object> appraisalLink(Long appraisalId) {
        List<Map<String, Object>> rows = jdbc.query("SELECT a.appraisal_id,c.cycle_name,a.final_rating,a.final_recommendation,a.manager_rating,a.hr_rating "
                        + "FROM pms_appraisal a JOIN pms_appraisal_cycle c ON c.cycle_id=a.cycle_id WHERE a.appraisal_id=?",
                (rs, index) -> Map.of("appraisalId", rs.getLong(1), "cycleName", rs.getString(2),
                        "finalRating", rs.getBigDecimal(3), "recommendation", Objects.toString(rs.getString(4), ""),
                        "managerRating", rs.getBigDecimal(5), "hrRating", rs.getBigDecimal(6)), appraisalId);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private void persistChildren(long pipId, Map<String, Object> payload) {
        jdbc.update("DELETE FROM pms_pip_action_plan WHERE pip_id=?", pipId);
        jdbc.update("DELETE FROM pms_pip_objective WHERE pip_id=?", pipId);
        Map<Integer, Long> objectiveIds = new LinkedHashMap<>();
        List<Map<String, Object>> objectives = maps(payload.get("objectives"));
        for (int index = 0; index < objectives.size(); index++) {
            Map<String, Object> objective = objectives.get(index); long id = nextId("pms_pip_objective_seq"); objectiveIds.put(index, id);
                jdbc.update("INSERT INTO pms_pip_objective (objective_id,pip_id,goal_id,kra_id,kpi_id,title,current_state,expected_state,measurement,target_value,"
                            + "actual_value,weightage,progress_percent,deadline,status,manager_assessment,employee_comments,hr_comments) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, pipId, typed(longValue(objective.get("goalId")), Types.NUMERIC), typed(longValue(objective.get("kraId")), Types.NUMERIC),
                    typed(longValue(objective.get("kpiId")), Types.NUMERIC), hasText(text(objective.get("title"))) ? text(objective.get("title")) : "Improvement objective",
                    typed(text(objective.get("currentState")), Types.CLOB), typed(text(objective.get("expectedState")), Types.CLOB), typed(text(objective.get("measurement")), Types.VARCHAR),
                    typed(decimal(objective.get("targetValue")), Types.NUMERIC), typed(decimal(objective.get("actualValue")), Types.NUMERIC), number(objective.get("weightage"), 0),
                    number(objective.get("progress"), 0), typed(sqlDate(objective.get("deadline")), Types.DATE),
                    text(objective.get("status")) == null ? "OPEN" : text(objective.get("status")),
                    typed(text(objective.get("managerAssessment")), Types.CLOB), typed(text(objective.get("employeeComments")), Types.CLOB), typed(text(objective.get("hrComments")), Types.CLOB));
        }
        for (Map<String, Object> action : maps(payload.get("actionPlans"))) {
            Integer objectiveIndex = intOrNull(action.get("objectiveIndex")); long actionId = nextId("pms_pip_action_seq");
            jdbc.update("INSERT INTO pms_pip_action_plan (action_id,pip_id,objective_id,action_text,owner_id,owner_name,due_date,"
                            + "support_required,training_required,success_criteria,status) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    actionId, pipId, typed(objectiveIndex == null ? null : objectiveIds.get(objectiveIndex), Types.NUMERIC),
                    hasText(text(action.get("action"))) ? text(action.get("action")) : "Follow-up", typed(longValue(action.get("ownerId")), Types.NUMERIC),
                    typed(text(action.get("owner")), Types.VARCHAR), typed(sqlDate(action.get("dueDate")), Types.DATE), typed(text(action.get("supportRequired")), Types.VARCHAR),
                    typed(text(action.get("trainingRequired")), Types.VARCHAR), typed(text(action.get("successCriteria")), Types.VARCHAR),
                    text(action.get("status")) == null ? "OPEN" : text(action.get("status")));
        }
        if (!objectives.isEmpty()) {
            jdbc.update("UPDATE pms_pip SET overall_progress=?,updated_at=SYSTIMESTAMP,updated_by=? WHERE pip_id=?",
                    weightedProgress(pipId), access.username(), pipId);
        }
    }

    private void updateObjective(long pipId, Map<String, Object> payload) {
        Long objectiveId = longValue(payload.get("objectiveId"));
        if (objectiveId == null || count("SELECT COUNT(*) FROM pms_pip_objective WHERE pip_id=? AND objective_id=?", pipId, objectiveId) == 0) {
            conflict("Select an objective that belongs to this PIP.");
        }
        List<String> setters = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (payload.containsKey("actualValue")) { setters.add("actual_value=?"); args.add(typed(decimal(payload.get("actualValue")), Types.NUMERIC)); }
        if (payload.containsKey("progress")) { setters.add("progress_percent=?"); args.add(number(payload.get("progress"), 0)); }
        if (hasText(text(payload.get("status")))) { setters.add("status=?"); args.add(typed(text(payload.get("status")), Types.VARCHAR)); }
        for (String field : List.of("managerAssessment", "employeeComments", "hrComments")) {
            if (payload.containsKey(field)) { setters.add(field.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT) + "=?"); args.add(typed(text(payload.get(field)), Types.CLOB)); }
        }
        if (setters.isEmpty()) return;
        setters.add("updated_at=SYSTIMESTAMP");
        args.add(objectiveId); args.add(pipId);
        jdbc.update("UPDATE pms_pip_objective SET " + String.join(",", setters) + " WHERE objective_id=? AND pip_id=?", args.toArray());
    }

    private BigDecimal weightedProgress(long pipId) {
        BigDecimal result = jdbc.queryForObject("SELECT NVL(SUM(progress_percent*weightage)/NULLIF(SUM(weightage),0),0) FROM pms_pip_objective WHERE pip_id=?", BigDecimal.class, pipId);
        return result == null ? BigDecimal.ZERO : result;
    }

    private void audit(long pipId, String action, String oldValue, String newValue) {
        jdbc.update("INSERT INTO pms_pip_audit (audit_id,pip_id,actor_employee_id,actor_name,actor_role,action_type,old_value,new_value) "
                        + "VALUES (pms_pip_audit_seq.NEXTVAL,?,?,?,?,?,?,?)",
                pipId, typed(access.employeeIdOrNull(), Types.NUMERIC), access.username(), actorRole(), action,
                typed(oldValue, Types.CLOB), typed(newValue, Types.CLOB));
    }

    private void applyScope(StringBuilder where, List<Object> args) {
        if (access.canManage()) return;
        Long employeeId = access.employeeIdOrNull();
        if (access.canReviewHr()) {
            where.append(" AND (p.assigned_hr_reviewer_id = ? OR p.assigned_hr_reviewer_id IS NULL)"); args.add(employeeId);
        } else if (employeeId != null) {
            where.append(" AND (e.employee_id = ? OR p.manager_id = ?)"); args.add(employeeId); args.add(employeeId);
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Employee access is required.");
        }
    }

    private PipRow requireAccess(long pipId, boolean write) {
        PipRow row = load(pipId);
        if (access.canManage()) return row;
        Long employeeId = access.employeeIdOrNull();
        if (access.canReviewHr() && (row.reviewerId() == null || Objects.equals(row.reviewerId(), employeeId))) return row;
        if (employeeId != null && row.managerId() != null && row.managerId().equals(employeeId)) return row;
        if (!write && employeeId != null && row.employeeId() == employeeId) return row;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, write ? "You cannot update this PIP." : "You cannot view this PIP.");
    }

    private PipRow requireReviewerAccess(long pipId) {
        if (access.canManage() || access.canReviewHr()) return requireAccess(pipId, true);
        PipRow row = load(pipId);
        if (Objects.equals(row.managerId(), access.employeeIdOrNull())) return row;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the manager or HR reviewer can submit a PIP review.");
    }

    private PipRow load(long pipId) {
        List<PipRow> rows = jdbc.query(SELECT + " WHERE p.pip_id=?", (rs, index) -> mapPip(rs), pipId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PIP not found.");
        return rows.get(0);
    }

    private Map<String,Object> pipMap(ResultSet rs)throws SQLException { return row(rs).toMap(); }
    private PipRow mapPip(ResultSet rs) throws SQLException { return row(rs); }
    private PipRow loadRow(ResultSet rs)throws SQLException { return row(rs); }
    private PipRow row(ResultSet rs)throws SQLException { return new PipRow(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),nullableLong(rs,7),rs.getString(8),nullableLong(rs,9),rs.getString(10),rs.getString(11),rs.getString(12),rs.getString(13),rs.getString(14),date(rs,15),date(rs,16),rs.getBigDecimal(17),date(rs,18),nullableLong(rs,19),rs.getString(20),rs.getString(21),rs.getTimestamp(22).toLocalDateTime().toString(),rs.getInt(23),rs.getString("CURRENT_PERFORMANCE"),rs.getString("EXPECTED_PERFORMANCE"),rs.getString("PERFORMANCE_GAP"),rs.getString("EVIDENCE"),rs.getString("SUPPORT_REQUIRED"),rs.getString("WORKFLOW_DATA"),rs.getString("EXTENSION_REASON"),rs.getString("OUTCOME_REASON")); }
    private Map<String, Object> objectiveMap(ResultSet rs) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("objectiveId", rs.getLong(1)); result.put("goalId", nullableLong(rs, 2)); result.put("goal", rs.getString(3));
        result.put("kraId", nullableLong(rs, 4)); result.put("kra", rs.getString(5));
        result.put("kpiId", nullableLong(rs, 6)); result.put("kpi", rs.getString(7)); result.put("title", rs.getString(8));
        result.put("currentState", rs.getString(9)); result.put("expectedState", rs.getString(10)); result.put("measurement", rs.getString(11));
        result.put("targetValue", rs.getBigDecimal(12)); result.put("actualValue", rs.getBigDecimal(13));
        result.put("weightage", rs.getBigDecimal(14)); result.put("progress", rs.getBigDecimal(15));
        result.put("deadline", date(rs, 16)); result.put("status", rs.getString(17));
        result.put("managerAssessment", rs.getString(18)); result.put("employeeComments", rs.getString(19));
        result.put("hrComments", rs.getString(20));
        return result;
    }
    private Map<String,Object> actionMap(ResultSet rs)throws SQLException { Map<String,Object> r=new LinkedHashMap<>();r.put("actionId",rs.getLong(1));r.put("objectiveId",nullableLong(rs,2));r.put("action",rs.getString(3));r.put("ownerId",nullableLong(rs,4));r.put("owner",rs.getString(5));r.put("dueDate",date(rs,6));r.put("supportRequired",rs.getString(7));r.put("trainingRequired",rs.getString(8));r.put("successCriteria",rs.getString(9));r.put("status",rs.getString(10));return r; }
    private Map<String,Object> reviewMap(ResultSet rs)throws SQLException { Map<String,Object> r=new LinkedHashMap<>();r.put("reviewId",rs.getLong(1));r.put("reviewer",rs.getString(2));r.put("role",rs.getString(3));r.put("reviewDate",date(rs,4));r.put("progress",rs.getBigDecimal(5));r.put("managerAssessment",rs.getString(6));r.put("employeeComments",rs.getString(7));r.put("hrComments",rs.getString(8));return r; }
    private Map<String,Object> auditMap(ResultSet rs)throws SQLException { Map<String,Object> r=new LinkedHashMap<>();r.put("user",rs.getString(1));r.put("role",rs.getString(2));r.put("action",rs.getString(3));r.put("oldValue",rs.getString(4));r.put("newValue",rs.getString(5));r.put("changedAt",rs.getTimestamp(6).toLocalDateTime().toString());return r; }

    private void requireHr() { if(!access.canReviewHr())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"HR review access is required."); }
    private void addFilter(StringBuilder where,List<Object> args,String clause,Object value){if(value!=null){where.append(" AND ").append(clause);args.add(value);}}
    private void add(StringBuilder where,List<Object> args,String clause,Object value){where.append(" AND ").append(clause);args.add(value);}
    private int count(String sql,Object...args){Integer value=jdbc.queryForObject(sql,Integer.class,args);return value==null?0:value;}
    private Object[] optionalId(Long id){return id==null?new Object[0]:new Object[]{id};}
    private long nextId(String sequence){Long id=jdbc.queryForObject("SELECT "+sequence+".NEXTVAL FROM dual",Long.class);return id==null?0:id;}
    private Long nullableLong(String sql,long id){List<Long> values=jdbc.query(sql,(rs,row)->{Object v=rs.getObject(1);return v==null?null:((Number)v).longValue();},id);return values.isEmpty()?null:values.get(0);}
    private Long nullableLong(ResultSet rs,int index)throws SQLException{Object v=rs.getObject(index);return v==null?null:((Number)v).longValue();}
    private Long longValue(Object v){if(v==null||v.toString().isBlank())return null;return Long.valueOf(v.toString());}
    private Long numberOrNull(String v){return v==null||v.isBlank()?null:Long.valueOf(v);}
    private long requiredLong(Object v,String message){Long n=longValue(v);if(n==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);return n;}
    private Integer intOrNull(Object v){return v==null||v.toString().isBlank()?null:Integer.valueOf(v.toString());}
    private BigDecimal decimal(Object v){return v==null||v.toString().isBlank()?null:new BigDecimal(v.toString());}
    private double number(Object v,double fallback){return v==null||v.toString().isBlank()?fallback:Double.parseDouble(v.toString());}
    private LocalDate date(Object v,LocalDate fallback){return v==null||v.toString().isBlank()?fallback:LocalDate.parse(v.toString().substring(0,10));}
    private Date sqlDate(Object v){LocalDate d=date(v,null);return d==null?null:Date.valueOf(d);}
    private String date(ResultSet rs,int index)throws SQLException{Date d=rs.getDate(index);return d==null?null:d.toLocalDate().toString();}
    private void validateDates(LocalDate start,LocalDate end){if(start==null||end==null||end.isBefore(start))conflict("PIP end date must be on or after the start date.");}
    private void conflict(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private String text(Object v){return v==null?null:v.toString().trim();}
    private SqlParameterValue typed(Object value,int sqlType){return new SqlParameterValue(sqlType,value);}
    private boolean hasText(String v){return v!=null&&!v.isBlank();}
    private String actorRole(){return access.canReviewHr()?"HR":access.employeeIdOrNull()==null?"ADMIN":"MANAGER";}
    private double round1(double v){return Math.round(v*10.0)/10.0;}
    private String json(Object v){try{return mapper.writeValueAsString(v);}catch(JsonProcessingException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"PIP data must be valid JSON.");}}
    private Map<String,Object> parse(String v){if(v==null||v.isBlank())return new LinkedHashMap<>();try{return mapper.readValue(v,Map.class);}catch(JsonProcessingException ex){return new LinkedHashMap<>();}}
    @SuppressWarnings("unchecked") private List<Map<String,Object>> maps(Object v){if(!(v instanceof List<?> list))return List.of();List<Map<String,Object>> result=new ArrayList<>();for(Object row:list)if(row instanceof Map<?,?> map)result.add((Map<String,Object>)map);return result;}
    private String csv(String v){String s=Objects.toString(v,"");if(s.startsWith("=")||s.startsWith("+")||s.startsWith("-")||s.startsWith("@"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
    private void appendCsv(StringBuilder out,Object...values){for(int i=0;i<values.length;i++){if(i>0)out.append(',');out.append(csv(Objects.toString(values[i],"")));}out.append("\r\n");}

    private record PipRow(long pipId,long employeeId,String employeeCode,String employeeName,String department,String designation,Long managerId,String manager,Long reviewerId,String reviewer,String status,String outcome,String reasonCode,String reason,String startDate,String endDate,BigDecimal progress,String nextReviewDate,Long relatedAppraisalId,String cycleName,String reviewFrequency,String createdAt,int durationDays,String currentPerformance,String expectedPerformance,String performanceGap,String evidence,String supportRequired,String workflowData,String extensionReason,String outcomeReason){
        Map<String,Object> toMap(){Map<String,Object> m=new LinkedHashMap<>();m.put("pipId",pipId);m.put("employeeId",employeeId);m.put("employeeCode",employeeCode);m.put("employeeName",employeeName);m.put("department",department);m.put("designation",designation);m.put("managerId",managerId);m.put("manager",manager);m.put("reviewerId",reviewerId);m.put("reviewer",reviewer);m.put("status",status);m.put("outcome",outcome);m.put("reasonCode",reasonCode);m.put("reason",reason);m.put("startDate",startDate);m.put("endDate",endDate);m.put("overallProgress",progress);m.put("nextReviewDate",nextReviewDate);m.put("relatedAppraisalId",relatedAppraisalId);m.put("cycleName",cycleName);m.put("reviewFrequency",reviewFrequency);m.put("createdAt",createdAt);m.put("durationDays",durationDays);m.put("currentPerformance",currentPerformance);m.put("expectedPerformance",expectedPerformance);m.put("performanceGap",performanceGap);m.put("evidence",evidence);m.put("supportRequired",supportRequired);m.put("extensionReason",extensionReason);m.put("outcomeReason",outcomeReason);return m;}
    }
}