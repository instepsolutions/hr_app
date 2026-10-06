package com.hrms.pms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class PmsReviewService {

    private static final String ELIGIBLE = "e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')";
    private static final String REVIEW_SELECT = "SELECT a.appraisal_id, e.employee_id, e.employee_code, e.display_name,"
            + " e.department_id, d.department_name, ds.designation_name, e.location_id, l.location_name,"
            + " e.date_of_joining, e.reporting_manager_id, manager.display_name, c.cycle_id, c.cycle_name,"
            + " c.due_date, a.self_status, a.self_rating, a.manager_status, a.manager_rating, a.hr_status,"
            + " a.hr_rating, a.final_rating, a.assigned_hr_reviewer_id, reviewer.display_name,"
            + " a.self_data, a.manager_data, a.hr_data"
            + " FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
            + " JOIN pms_appraisal_cycle c ON c.cycle_id = a.cycle_id"
            + " LEFT JOIN department d ON d.department_id = e.department_id"
            + " LEFT JOIN designation ds ON ds.designation_id = e.designation_id"
            + " LEFT JOIN location l ON l.location_id = e.location_id"
            + " LEFT JOIN employee manager ON manager.employee_id = e.reporting_manager_id"
            + " LEFT JOIN employee reviewer ON reviewer.employee_id = a.assigned_hr_reviewer_id";
    private static final Set<String> FINAL_RECOMMENDATIONS = Set.of("PROMOTION", "INCREMENT", "PIP", "NO_CHANGE");

    private final JdbcTemplate jdbc;
    private final PmsAccess access;
    private final ObjectMapper mapper;
    private final AuditLogService audit;

    public PmsReviewService(JdbcTemplate jdbc, PmsAccess access, ObjectMapper mapper, AuditLogService audit) {
        this.jdbc = jdbc;
        this.access = access;
        this.mapper = mapper;
        this.audit = audit;
    }

    public Map<String, Object> managerOverview(Long cycleId, Long departmentId) {
        Scope scope = managerScope();
        StringBuilder filter = new StringBuilder(" WHERE ").append(ELIGIBLE);
        List<Object> args = new ArrayList<>();
        if (!scope.global()) addFilter(filter, args, "e.reporting_manager_id = ?", scope.employeeId());
        addFilter(filter, args, "e.department_id = ?", departmentId);
        addFilter(filter, args, "a.cycle_id = ?", cycleId);
        long total = count("SELECT COUNT(DISTINCT e.employee_id) FROM employee e LEFT JOIN pms_appraisal a"
            + " ON a.employee_id = e.employee_id" + (cycleId == null ? "" : " AND a.cycle_id = ?") + filter,
                withCycle(cycleId, args).toArray());
        String appraisalFilter = filter.toString().replace("WHERE " + ELIGIBLE, "WHERE " + ELIGIBLE + " AND a.appraisal_id IS NOT NULL");
        List<Object> appraisalArgs = args;
        long completed = count("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + appraisalFilter + " AND a.manager_status IN ('SUBMITTED','COMPLETED')", appraisalArgs.toArray());
        long inProgress = count("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + appraisalFilter + " AND a.manager_status = 'IN_PROGRESS'", appraisalArgs.toArray());
        long pending = Math.max(0, total - completed - inProgress);
        Double average = average("SELECT AVG(a.manager_rating) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + appraisalFilter + " AND a.manager_rating IS NOT NULL", appraisalArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", Map.of("totalEmployees", total, "completed", completed, "inProgress", inProgress,
                "pending", pending, "completionRate", percent(completed, total), "averageRating", round2(average)));
        result.put("status", List.of(bucket("Completed", completed), bucket("In Progress", inProgress), bucket("Pending", pending)));
        result.put("ratingDistribution", ratingDistribution("manager_rating", filter.toString(), appraisalArgs.toArray()));
        result.put("departments", departmentSummary("manager_rating", filter.toString(), appraisalArgs.toArray()));
        result.put("pendingByManager", pendingByManager(scope, departmentId, cycleId));
        result.put("timeline", timeline(cycleId, scope, departmentId));
        result.put("team", teamSummary(scope, departmentId, cycleId));
        result.put("insights", insights("MANAGER", total, completed, pending, average, departmentId, cycleId));
        result.put("cycles", cycles(scope, departmentId));
        return result;
    }

    public List<Map<String, Object>> managerQueue(String search, String status, Long departmentId,
                                                   Integer rating, Long cycleId) {
        Scope scope = managerScope();
        StringBuilder where = new StringBuilder(" WHERE ").append(ELIGIBLE)
                .append(" AND a.self_status = 'SUBMITTED' AND a.manager_status <> 'NOT_READY'");
        List<Object> args = new ArrayList<>();
        if (!scope.global()) addFilter(where, args, "e.reporting_manager_id = ?", scope.employeeId());
        addFilter(where, args, "e.department_id = ?", departmentId);
        addFilter(where, args, "a.cycle_id = ?", cycleId);
        if (hasText(status) && !"ALL".equalsIgnoreCase(status)) addFilter(where, args, "a.manager_status = ?", status.toUpperCase(Locale.ROOT));
        if (rating != null) addFilter(where, args, "ROUND(a.self_rating) = ?", rating);
        addSearch(where, args, search);
        return reviewRows(REVIEW_SELECT + where + " ORDER BY c.due_date, e.display_name", args.toArray());
    }

    public Map<String, Object> managerReview(long appraisalId) {
        ReviewRow row = managerAccess(appraisalId);
        if (!"SUBMITTED".equals(row.selfStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Self appraisal must be submitted before manager review.");
        }
        return detail(row);
    }

    @Transactional
    public Map<String, Object> saveManagerDraft(long appraisalId, Map<String, Object> payload) {
        ReviewRow row = managerAccess(appraisalId);
        if (!"SUBMITTED".equals(row.selfStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Self appraisal must be submitted before manager review.");
        }
        if (List.of("SUBMITTED", "COMPLETED").contains(row.managerStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A submitted manager review is locked.");
        }
        BigDecimal rating = decimal(payload.get("overallRating"));
        String data = json(payload);
        String nextStatus = rating == null && payload.isEmpty() ? "PENDING" : "IN_PROGRESS";
        jdbc.update("UPDATE pms_appraisal SET manager_status = ?, manager_rating = ?, manager_data = ?,"
                        + " stage = 'MANAGER_REVIEW', updated_at = SYSTIMESTAMP, updated_by = ? WHERE appraisal_id = ?",
                nextStatus, rating, data, access.username(), appraisalId);
        history(appraisalId, "MANAGER", "SAVE_DRAFT", row.managerStatus(), nextStatus, null);
        return managerReview(appraisalId);
    }

    @Transactional
    public Map<String, Object> submitManagerReview(long appraisalId) {
        ReviewRow row = managerAccess(appraisalId);
        if (List.of("SUBMITTED", "COMPLETED").contains(row.managerStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A submitted manager review is locked.");
        }
        Map<String, Object> details = detail(row);
        Map<String, Object> managerData = object(details.get("managerReview"));
        BigDecimal rating = decimal(managerData.get("overallRating"));
        if (rating == null) rating = row.managerRating();
        validateRating(rating, "A manager rating is required before submission.");
        jdbc.update("UPDATE pms_appraisal SET manager_status = 'SUBMITTED', manager_rating = ?,"
                        + " manager_submitted_at = SYSTIMESTAMP, hr_status = 'PENDING', stage = 'HR_REVIEW',"
                        + " updated_at = SYSTIMESTAMP, updated_by = ? WHERE appraisal_id = ?",
                rating, access.username(), appraisalId);
        history(appraisalId, "MANAGER", "SUBMITTED", row.managerStatus(), "SUBMITTED", null);
        audit.record("PMS_APPRAISAL", appraisalId, "MANAGER_SUBMIT", Map.of("status", row.managerStatus()),
                Map.of("status", "SUBMITTED", "rating", rating));
        return managerReview(appraisalId);
    }

    @Transactional
    public Map<String, Object> sendBackToEmployee(long appraisalId, Map<String, Object> payload) {
        return returnToEmployee(appraisalId, payload, "SENT_BACK");
    }

    @Transactional
    public Map<String, Object> requestClarification(long appraisalId, Map<String, Object> payload) {
        return returnToEmployee(appraisalId, payload, "CLARIFICATION_REQUESTED");
    }

    @Transactional
    public Map<String, Object> remindManager(long appraisalId) {
        ReviewRow row = managerAccess(appraisalId);
        history(appraisalId, "MANAGER", "REMINDER_SENT", row.managerStatus(), row.managerStatus(),
                "Reminder recorded for " + row.employeeName());
        return Map.of("appraisalId", appraisalId, "action", "REMINDER_SENT", "employee", row.employeeName());
    }

    public Map<String, Object> hrOverview(Long cycleId, Long departmentId) {
        requireHr();
        StringBuilder filter = new StringBuilder(" WHERE ").append(ELIGIBLE);
        List<Object> args = new ArrayList<>();
        addFilter(filter, args, "e.department_id = ?", departmentId);
        addFilter(filter, args, "a.cycle_id = ?", cycleId);
        String assignedFilter = reviewerFilter(args);
        if (!assignedFilter.isEmpty()) filter.append(assignedFilter);
        List<Object> countArgs = withCycle(cycleId, args);
        long total = count("SELECT COUNT(DISTINCT e.employee_id) FROM employee e LEFT JOIN pms_appraisal a"
                + " ON a.employee_id = e.employee_id" + (cycleId == null ? "" : " AND a.cycle_id = ?") + filter, countArgs.toArray());
        String eligibleRows = filter.toString().replace("WHERE " + ELIGIBLE, "WHERE " + ELIGIBLE + " AND a.appraisal_id IS NOT NULL");
        long completed = count("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + eligibleRows + " AND a.hr_status IN ('COMPLETED','FINALIZED')", args.toArray());
        long inProgress = count("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + eligibleRows + " AND a.hr_status IN ('IN_PROGRESS','UNDER_CALIBRATION')", args.toArray());
        long pending = Math.max(0, total - completed - inProgress);
        Double average = average("SELECT AVG(a.hr_rating) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + eligibleRows + " AND a.hr_rating IS NOT NULL", args.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", Map.of("totalEmployees", total, "completed", completed, "inProgress", inProgress,
                "pending", pending, "completionRate", percent(completed, total), "averageRating", round2(average)));
        result.put("status", List.of(bucket("Completed", completed), bucket("In Progress", inProgress), bucket("Pending", pending)));
        result.put("ratingDistribution", ratingDistribution("hr_rating", eligibleRows, args.toArray()));
        result.put("departments", departmentSummary("hr_rating", eligibleRows, args.toArray()));
        result.put("pendingByReviewer", pendingByReviewer(departmentId, cycleId));
        result.put("timeline", timeline(cycleId, new Scope(true, null), departmentId));
        result.put("workload", hrWorkload(cycleId, departmentId));
        result.put("insights", insights("HR", total, completed, pending, average, departmentId, cycleId));
        result.put("cycles", cycles(new Scope(true, null), departmentId));
        return result;
    }

    public List<Map<String, Object>> hrQueue(String search, String status, Long departmentId,
                                              Long managerId, Long reviewerId, Integer rating, Long cycleId) {
        requireHr();
        StringBuilder where = new StringBuilder(" WHERE ").append(ELIGIBLE)
                .append(" AND a.manager_status IN ('SUBMITTED','COMPLETED') AND a.hr_status <> 'NOT_READY'");
        List<Object> args = new ArrayList<>();
        addFilter(where, args, "e.department_id = ?", departmentId);
        addFilter(where, args, "e.reporting_manager_id = ?", managerId);
        addFilter(where, args, "a.assigned_hr_reviewer_id = ?", reviewerId);
        addFilter(where, args, "a.cycle_id = ?", cycleId);
        if (hasText(status) && !"ALL".equalsIgnoreCase(status)) addFilter(where, args, "a.hr_status = ?", status.toUpperCase(Locale.ROOT));
        if (rating != null) addFilter(where, args, "ROUND(a.manager_rating) = ?", rating);
        String reviewerFilter = reviewerFilter(args);
        if (!reviewerFilter.isEmpty()) where.append(reviewerFilter);
        addSearch(where, args, search);
        return reviewRows(REVIEW_SELECT + where + " ORDER BY c.due_date, e.display_name", args.toArray());
    }

    public List<Map<String, Object>> hrReviewers() {
        requireHr();
        return jdbc.query("SELECT u.employee_id, e.employee_code, e.display_name, d.department_name"
                        + " FROM app_user u JOIN employee e ON e.employee_id = u.employee_id"
                        + " LEFT JOIN department d ON d.department_id = e.department_id"
                        + " WHERE u.is_active = 1 AND u.role IN ('SUPER_ADMIN','HR_ADMIN','HR_MANAGER','HR_EXECUTIVE')"
                        + " ORDER BY e.display_name",
                (rs, row) -> Map.of("employeeId", rs.getLong(1), "employeeCode", rs.getString(2),
                        "name", rs.getString(3), "department", Objects.toString(rs.getString(4), "")));
    }

    public Map<String, Object> hrReview(long appraisalId) {
        requireHr();
        ReviewRow row = findReview(appraisalId);
        requireHrReviewScope(row);
        return detail(row);
    }

    @Transactional
    public Map<String, Object> saveHrDraft(long appraisalId, Map<String, Object> payload) {
        requireHr();
        ReviewRow row = findReview(appraisalId);
        requireHrReviewScope(row);
        if (List.of("COMPLETED", "FINALIZED").contains(row.hrStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A finalized HR review is locked.");
        }
        if (!List.of("SUBMITTED", "COMPLETED").contains(row.managerStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Manager review must be submitted before HR review.");
        }
        BigDecimal rating = decimal(payload.get("hrRating"));
        BigDecimal finalRating = decimal(payload.get("finalRating"));
        String nextStatus = Boolean.TRUE.equals(payload.get("underCalibration")) ? "UNDER_CALIBRATION" : "IN_PROGRESS";
        jdbc.update("UPDATE pms_appraisal SET hr_status = ?, hr_rating = ?, final_rating = ?, hr_data = ?,"
                        + " updated_at = SYSTIMESTAMP, updated_by = ? WHERE appraisal_id = ?",
                nextStatus, rating, finalRating, json(payload), access.username(), appraisalId);
        history(appraisalId, "HR", "SAVE_DRAFT", row.hrStatus(), nextStatus, text(payload.get("calibrationReason")));
        return detail(findReview(appraisalId));
    }

    @Transactional
    public Map<String, Object> submitHrReview(long appraisalId) {
        requireHr();
        ReviewRow row = findReview(appraisalId);
        requireHrReviewScope(row);
        if (List.of("COMPLETED", "FINALIZED").contains(row.hrStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A finalized HR review is locked.");
        }
        if (!List.of("SUBMITTED", "COMPLETED").contains(row.managerStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Manager review must be submitted before HR review.");
        }
        Map<String, Object> data = object(parseJson(row.hrData()));
        BigDecimal hrRating = decimal(data.getOrDefault("hrRating", row.hrRating()));
        BigDecimal finalRating = decimal(data.getOrDefault("finalRating", hrRating));
        validateRating(hrRating, "An HR rating is required before finalization.");
        validateRating(finalRating, "A final calibrated rating is required before finalization.");
        BigDecimal managerRating = row.managerRating();
        String reason = text(data.get("calibrationReason"));
        if (managerRating != null && managerRating.compareTo(finalRating) != 0 && !hasText(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a calibration reason when the final rating differs from the manager rating.");
        }
        String recommendation = text(data.get("finalRecommendation"));
        if (!hasText(recommendation)) recommendation = "NO_CHANGE";
        if (!Set.of("PROMOTION", "INCREMENT", "PIP", "NO_CHANGE").contains(recommendation.toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid final recommendation.");
        }
        jdbc.update("UPDATE pms_appraisal SET hr_status = 'FINALIZED', hr_rating = ?, final_rating = ?,"
                        + " final_recommendation = ?, stage = 'COMPLETED', completed_at = TRUNC(SYSDATE),"
                        + " hr_submitted_at = SYSTIMESTAMP, updated_at = SYSTIMESTAMP, updated_by = ? WHERE appraisal_id = ?",
                hrRating, finalRating, recommendation.toUpperCase(Locale.ROOT), access.username(), appraisalId);
        history(appraisalId, "HR", "FINALIZED", row.hrStatus(), "FINALIZED", reason);
        audit.record("PMS_APPRAISAL", appraisalId, "HR_FINALIZE", Map.of("previousFinalRating", Objects.toString(row.finalRating(), "")),
                Map.of("finalRating", finalRating, "recommendation", recommendation));
        return detail(findReview(appraisalId));
    }

    @Transactional
    public Map<String, Object> reassignHrReview(long appraisalId, Map<String, Object> payload) {
        requireHr();
        ReviewRow row = findReview(appraisalId);
        requireHrReviewScope(row);
        Long reviewerId = longValue(payload.get("reviewerId"));
        if (reviewerId == null || count("SELECT COUNT(*) FROM app_user WHERE employee_id = ? AND is_active = 1"
                + " AND role IN ('SUPER_ADMIN','HR_ADMIN','HR_MANAGER','HR_EXECUTIVE')", reviewerId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an active HR reviewer.");
        }
        jdbc.update("UPDATE pms_appraisal SET assigned_hr_reviewer_id = ?, updated_at = SYSTIMESTAMP, updated_by = ?"
                + " WHERE appraisal_id = ?", reviewerId, access.username(), appraisalId);
        history(appraisalId, "HR", "REASSIGNED", Objects.toString(row.reviewerId(), "UNASSIGNED"), reviewerId.toString(), null);
        return findReview(appraisalId).toMap();
    }

    @Transactional
    public Map<String, Object> remindHrReviewer(long appraisalId) {
        requireHr();
        ReviewRow row = findReview(appraisalId);
        requireHrReviewScope(row);
        history(appraisalId, "HR", "REMINDER_SENT", row.hrStatus(), row.hrStatus(), "HR review reminder recorded");
        return Map.of("appraisalId", appraisalId, "action", "REMINDER_SENT", "employee", row.employeeName());
    }

    public List<Map<String, Object>> reviewHistory(long appraisalId) {
        requireReviewAccess(appraisalId);
        return jdbc.query("SELECT reviewer_name, reviewer_role, action_type, old_value, new_value, reason, changed_at"
                        + " FROM pms_review_history WHERE appraisal_id = ? ORDER BY changed_at DESC, history_id DESC"
                        + " FETCH FIRST 100 ROWS ONLY", (rs, row) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("user", rs.getString(1)); item.put("role", rs.getString(2));
                    item.put("action", rs.getString(3)); item.put("oldValue", rs.getString(4));
                    item.put("newValue", rs.getString(5)); item.put("reason", rs.getString(6));
                    item.put("changedAt", rs.getTimestamp(7).toLocalDateTime().toString()); return item;
                }, appraisalId);
    }

    public String exportCsv(String reviewType, Long cycleId, Long departmentId) {
        String type = reviewType.toUpperCase(Locale.ROOT);
        List<Map<String, Object>> rows;
        if ("MANAGER".equals(type)) {
            rows = managerQueue(null, null, departmentId, null, cycleId);
        } else if ("HR".equals(type)) {
            rows = hrQueue(null, null, departmentId, null, null, null, cycleId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review type must be MANAGER or HR.");
        }
        StringBuilder csv = new StringBuilder("Employee ID,Employee,Department,Manager,Self Rating,Manager Rating,HR Rating,Final Rating,Manager Status,HR Status,Due Date\r\n");
        for (Map<String, Object> row : rows) {
            appendCsv(csv, row.get("employeeCode"), row.get("employeeName"), row.get("department"), row.get("manager"),
                    row.get("selfRating"), row.get("managerRating"), row.get("hrRating"), row.get("finalRating"),
                    row.get("managerStatus"), row.get("hrStatus"), row.get("dueDate"));
        }
        return csv.toString();
    }

    private Map<String, Object> returnToEmployee(long appraisalId, Map<String, Object> payload, String action) {
        ReviewRow row = managerAccess(appraisalId);
        if (!"SUBMITTED".equals(row.selfStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Self appraisal must be submitted before manager review.");
        }
        String reason = text(payload.get("reason"));
        if (!hasText(reason)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a reason for the employee.");
        jdbc.update("UPDATE pms_appraisal SET manager_status = 'SENT_BACK', self_status = 'DRAFT', stage = 'SELF_APPRAISAL',"
                + " updated_at = SYSTIMESTAMP, updated_by = ? WHERE appraisal_id = ?", access.username(), appraisalId);
        history(appraisalId, "MANAGER", action, row.managerStatus(), "SENT_BACK", reason);
        return detail(findReview(appraisalId));
    }

    private Map<String, Object> managerReview(long appraisalId, boolean includeNotSubmitted) {
        return detail(managerAccess(appraisalId));
    }

    private Map<String, Object> detail(ReviewRow row) {
        Map<String, Object> result = row.toMap();
        result.put("selfAppraisal", parseJson(row.selfData()));
        result.put("managerReview", parseJson(row.managerData()));
        result.put("hrReview", parseJson(row.hrData()));
        result.put("goals", jdbc.query("SELECT g.goal_id, g.title, g.description, g.kra_id, k.kra_name, g.kpi_id,"
                        + " kp.kpi_name, g.target_value, g.unit, g.weightage, g.progress_percent FROM pms_goal g"
                        + " LEFT JOIN pms_kra k ON k.kra_id = g.kra_id LEFT JOIN pms_kpi kp ON kp.kpi_id = g.kpi_id"
                        + " WHERE g.employee_id = ? AND g.is_archived = 0 ORDER BY g.due_date, g.goal_id",
                (rs, index) -> {
                    Map<String, Object> goal = new LinkedHashMap<>(); goal.put("goalId", rs.getLong(1));
                    goal.put("title", rs.getString(2)); goal.put("description", rs.getString(3));
                    goal.put("kraId", nullableLong(rs, 4)); goal.put("kra", rs.getString(5));
                    goal.put("kpiId", nullableLong(rs, 6)); goal.put("kpi", rs.getString(7));
                    goal.put("target", rs.getBigDecimal(8)); goal.put("unit", rs.getString(9));
                    goal.put("weightage", rs.getBigDecimal(10)); goal.put("progress", rs.getBigDecimal(11)); return goal;
                }, row.employeeId()));
        result.put("history", reviewHistory(row.appraisalId()));
        result.put("timeline", timeline(row.cycleId(), new Scope(true, null), row.departmentId()));
        return result;
    }

    private List<Map<String, Object>> reviewRows(String sql, Object... args) {
        return jdbc.query(sql, reviewMapper(), args);
    }

    private RowMapper<Map<String, Object>> reviewMapper() {
        return (rs, index) -> reviewMap(rs);
    }

    private ReviewRow toReviewRow(ResultSet rs) throws SQLException {
        return new ReviewRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), nullableLong(rs, 5),
                rs.getString(6), rs.getString(7), nullableLong(rs, 8), rs.getString(9), dateString(rs, 10),
                nullableLong(rs, 11), rs.getString(12), rs.getLong(13), rs.getString(14), dateString(rs, 15),
                rs.getString(16), rs.getBigDecimal(17), rs.getString(18), rs.getBigDecimal(19), rs.getString(20),
                rs.getBigDecimal(21), rs.getBigDecimal(22), nullableLong(rs, 23), rs.getString(24),
                clobString(rs, "SELF_DATA"), clobString(rs, "MANAGER_DATA"), clobString(rs, "HR_DATA"));
    }

    private Map<String, Object> reviewMap(ResultSet rs) throws SQLException {
        return toReviewRow(rs).toMap();
    }

    private ReviewRow findReview(long appraisalId) {
        List<ReviewRow> rows = jdbc.query(REVIEW_SELECT + " WHERE a.appraisal_id = ?",
            (rs, row) -> toReviewRow(rs), appraisalId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Appraisal review not found");
        return rows.get(0);
    }

    private ReviewRow managerAccess(long appraisalId) {
        ReviewRow row = findReview(appraisalId);
        if (access.canManage()) return row;
        long managerId = access.requireEmployeeId();
        if (row.managerId() == null || row.managerId() != managerId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only review employees who report to you.");
        }
        return row;
    }

    private void requireReviewAccess(long appraisalId) {
        if (access.canManage()) return;
        if (access.canReviewHr()) {
            requireHrReviewScope(findReview(appraisalId));
            return;
        }
        managerAccess(appraisalId);
    }

    private void requireHrReviewScope(ReviewRow row) {
        if (access.canManage()) return;
        Long reviewerId = access.employeeIdOrNull();
        if (row.reviewerId() != null && !Objects.equals(row.reviewerId(), reviewerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access HR reviews assigned to you.");
        }
    }

    private void requireHr() {
        if (!access.canReviewHr()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "HR review access is required.");
    }

    private Scope managerScope() {
        if (access.canManage()) return new Scope(true, null);
        return new Scope(false, access.requireEmployeeId());
    }

    private List<Map<String, Object>> pendingByManager(Scope scope, Long departmentId, Long cycleId) {
        StringBuilder where = new StringBuilder(" WHERE ").append(ELIGIBLE)
                .append(" AND a.self_status = 'SUBMITTED' AND a.manager_status IN ('PENDING','IN_PROGRESS','SENT_BACK')");
        List<Object> args = new ArrayList<>();
        if (!scope.global()) addFilter(where, args, "e.reporting_manager_id = ?", scope.employeeId());
        addFilter(where, args, "e.department_id = ?", departmentId);
        addFilter(where, args, "a.cycle_id = ?", cycleId);
        return jdbc.query("SELECT manager.employee_id, manager.display_name, d.department_name, COUNT(*)"
                        + " FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                        + " LEFT JOIN employee manager ON manager.employee_id = e.reporting_manager_id"
                        + " LEFT JOIN department d ON d.department_id = manager.department_id" + where
                        + " GROUP BY manager.employee_id, manager.display_name, d.department_name"
                        + " ORDER BY COUNT(*) DESC FETCH FIRST 10 ROWS ONLY", (rs, row) -> {
                    Map<String, Object> item = new LinkedHashMap<>(); item.put("managerId", nullableLong(rs, 1));
                    item.put("managerName", Objects.toString(rs.getString(2), "Unassigned"));
                    item.put("department", Objects.toString(rs.getString(3), "Unassigned")); item.put("pending", rs.getLong(4)); return item;
                }, args.toArray());
    }

    private List<Map<String, Object>> pendingByReviewer(Long departmentId, Long cycleId) {
        StringBuilder where = new StringBuilder(" WHERE ").append(ELIGIBLE)
                .append(" AND a.manager_status IN ('SUBMITTED','COMPLETED') AND a.hr_status IN ('PENDING','IN_PROGRESS','UNDER_CALIBRATION')");
        List<Object> args = new ArrayList<>();
        addFilter(where, args, "e.department_id = ?", departmentId);
        addFilter(where, args, "a.cycle_id = ?", cycleId);
        String reviewer = reviewerFilter(args);
        if (!reviewer.isEmpty()) where.append(reviewer);
        return jdbc.query("SELECT a.assigned_hr_reviewer_id, reviewer.display_name, d.department_name, COUNT(*)"
                        + " FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                        + " LEFT JOIN employee reviewer ON reviewer.employee_id = a.assigned_hr_reviewer_id"
                        + " LEFT JOIN department d ON d.department_id = reviewer.department_id" + where
                        + " GROUP BY a.assigned_hr_reviewer_id, reviewer.display_name, d.department_name"
                        + " ORDER BY COUNT(*) DESC FETCH FIRST 10 ROWS ONLY", (rs, row) -> {
                    Map<String, Object> item = new LinkedHashMap<>(); item.put("reviewerId", nullableLong(rs, 1));
                    item.put("reviewerName", Objects.toString(rs.getString(2), "Unassigned"));
                    item.put("department", Objects.toString(rs.getString(3), "Unassigned")); item.put("pending", rs.getLong(4)); return item;
                }, args.toArray());
    }

    private Map<String, Object> teamSummary(Scope scope, Long departmentId, Long cycleId) {
        List<Object> args = new ArrayList<>();
        StringBuilder filter = new StringBuilder(" WHERE ").append(ELIGIBLE);
        if (!scope.global()) addFilter(filter, args, "e.reporting_manager_id = ?", scope.employeeId());
        addFilter(filter, args, "e.department_id = ?", departmentId);
        addFilter(filter, args, "a.cycle_id = ?", cycleId);
        long total = count("SELECT COUNT(DISTINCT e.employee_id) FROM employee e LEFT JOIN pms_appraisal a"
            + " ON a.employee_id = e.employee_id" + (cycleId == null ? "" : " AND a.cycle_id = ?") + filter,
                withCycle(cycleId, args).toArray());
        long completed = count("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + filter + " AND a.manager_status IN ('SUBMITTED','COMPLETED')", args.toArray());
        long inProgress = count("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + filter + " AND a.manager_status = 'IN_PROGRESS'", args.toArray());
        double pending = Math.max(0, total - completed - inProgress);
        Double average = average("SELECT AVG(a.manager_rating) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + filter + " AND a.manager_rating IS NOT NULL", args.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total); result.put("completed", completed); result.put("inProgress", inProgress);
        result.put("pending", pending); result.put("completionRate", percent(completed, total));
        result.put("averageRating", round2(average)); return result;
    }

    private Map<String, Object> hrWorkload(Long cycleId, Long departmentId) {
        List<Object> args = new ArrayList<>();
        StringBuilder filter = new StringBuilder(" WHERE ").append(ELIGIBLE)
                .append(" AND a.manager_status IN ('SUBMITTED','COMPLETED') AND a.hr_status <> 'NOT_READY'");
        addFilter(filter, args, "e.department_id = ?", departmentId);
        addFilter(filter, args, "a.cycle_id = ?", cycleId);
        String reviewerFilter = reviewerFilter(args);
        if (!reviewerFilter.isEmpty()) filter.append(reviewerFilter);
        long total = count("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id" + filter, args.toArray());
        long completed = count("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + filter + " AND a.hr_status IN ('COMPLETED','FINALIZED')", args.toArray());
        long inProgress = count("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + filter + " AND a.hr_status IN ('IN_PROGRESS','UNDER_CALIBRATION')", args.toArray());
        Double average = average("SELECT AVG(a.hr_rating) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + filter + " AND a.hr_rating IS NOT NULL", args.toArray());
        return Map.of("total", total, "completed", completed, "inProgress", inProgress,
                "pending", Math.max(0, total - completed - inProgress), "completionRate", percent(completed, total),
                "averageRating", round2(average));
    }

    private List<Map<String, Object>> ratingDistribution(String column, String baseFilter, Object... args) {
        if (!Set.of("manager_rating", "hr_rating").contains(column)) throw new IllegalArgumentException("Invalid rating column");
        Map<Integer, Long> counts = new LinkedHashMap<>();
        jdbc.query("SELECT ROUND(a." + column + "), COUNT(*) FROM pms_appraisal a"
                        + " JOIN employee e ON e.employee_id = a.employee_id" + baseFilter
                        + " AND a." + column + " IS NOT NULL GROUP BY ROUND(a." + column + ")",
            rs -> { counts.put(rs.getInt(1), rs.getLong(2)); }, args);
        List<Map<String, Object>> result = new ArrayList<>();
        for (int rating = 1; rating <= 5; rating++) result.add(Map.of("rating", rating, "count", counts.getOrDefault(rating, 0L)));
        return result;
    }

    private List<Map<String, Object>> departmentSummary(String ratingColumn, String baseFilter, Object... args) {
        if (!Set.of("manager_rating", "hr_rating").contains(ratingColumn)) throw new IllegalArgumentException("Invalid rating column");
        return jdbc.query("SELECT d.department_name, COUNT(DISTINCT e.employee_id),"
                        + " COUNT(DISTINCT CASE WHEN a." + (ratingColumn.equals("manager_rating") ? "manager_status" : "hr_status")
                        + " IN ('SUBMITTED','COMPLETED','FINALIZED') THEN e.employee_id END),"
                        + " COUNT(DISTINCT CASE WHEN a." + (ratingColumn.equals("manager_rating") ? "manager_status" : "hr_status")
                        + " IN ('IN_PROGRESS','UNDER_CALIBRATION') THEN e.employee_id END),"
                        + " NVL(AVG(a." + ratingColumn + "),0) FROM employee e LEFT JOIN department d ON d.department_id = e.department_id"
                        + " LEFT JOIN pms_appraisal a ON a.employee_id = e.employee_id"
                        + baseFilter.replace("WHERE ", "AND ")
                        + " GROUP BY d.department_name ORDER BY d.department_name", (rs, row) -> {
                    long total = rs.getLong(2), completed = rs.getLong(3), inProgress = rs.getLong(4);
                    Map<String, Object> item = new LinkedHashMap<>(); item.put("department", Objects.toString(rs.getString(1), "Others"));
                    item.put("totalEmployees", total); item.put("completed", completed);
                    item.put("inProgress", inProgress); item.put("pending", Math.max(0, total - completed - inProgress));
                    item.put("completionRate", percent(completed, total)); item.put("averageRating", round2(rs.getDouble(5)));
                    return item;
                }, args);
    }

    private List<Map<String, Object>> timeline(Long cycleId, Scope scope, Long departmentId) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT c.cycle_id, c.cycle_name, c.start_date, c.due_date"
                + " FROM pms_appraisal_cycle c JOIN pms_appraisal a ON a.cycle_id = c.cycle_id"
                + " JOIN employee e ON e.employee_id = a.employee_id WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        addFilter(sql, args, "a.cycle_id = ?", cycleId);
        addFilter(sql, args, "e.department_id = ?", departmentId);
        if (!scope.global()) addFilter(sql, args, "e.reporting_manager_id = ?", scope.employeeId());
        sql.append(" ORDER BY c.start_date DESC FETCH FIRST 1 ROWS ONLY");
        List<Map<String, Object>> cycles = jdbc.query(sql.toString(), (rs, row) -> {
            long id = rs.getLong(1);
            Map<String, Object> cycle = new LinkedHashMap<>(); cycle.put("cycleId", id); cycle.put("cycleName", rs.getString(2));
            cycle.put("stages", jdbc.query("SELECT stage_code,start_date,end_date FROM pms_appraisal_timeline"
                            + " WHERE cycle_id = ? ORDER BY CASE stage_code WHEN 'SELF_APPRAISAL' THEN 1"
                            + " WHEN 'MANAGER_REVIEW' THEN 2 WHEN 'HR_REVIEW' THEN 3 ELSE 4 END",
                    (stageRs, stageRow) -> Map.of("stageCode", stageRs.getString(1),
                            "startDate", stageRs.getDate(2).toLocalDate().toString(),
                            "endDate", stageRs.getDate(3).toLocalDate().toString()), id));
            return cycle;
        }, args.toArray());
        return cycles;
    }

    private List<Map<String, Object>> cycles(Scope scope, Long departmentId) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT c.cycle_id,c.cycle_name,c.dept_group,c.start_date,c.due_date"
                + " FROM pms_appraisal_cycle c JOIN pms_appraisal a ON a.cycle_id=c.cycle_id"
                + " JOIN employee e ON e.employee_id=a.employee_id WHERE c.status='ACTIVE'");
        List<Object> args = new ArrayList<>();
        addFilter(sql, args, "e.department_id = ?", departmentId);
        if (!scope.global()) addFilter(sql, args, "e.reporting_manager_id = ?", scope.employeeId());
        sql.append(" ORDER BY c.start_date DESC");
        return jdbc.query(sql.toString(), (rs, row) -> Map.of("cycleId", rs.getLong(1),
                "cycleName", rs.getString(2), "departmentGroup", rs.getString(3),
                "startDate", rs.getDate(4).toLocalDate().toString(), "endDate", rs.getDate(5).toLocalDate().toString()), args.toArray());
    }

    private List<Map<String, Object>> insights(String type, long total, long completed, long pending,
                                               Double average, Long departmentId, Long cycleId) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(Map.of("label", "Completion rate", "value", percent(completed, total) + "%"));
        result.add(Map.of("label", "Pending reviews", "value", pending));
        if (average != null) result.add(Map.of("label", "Average rating", "value", String.format(Locale.ROOT, "%.2f / 5", average)));
        List<Map<String, Object>> departments = departmentSummary(type.equals("HR") ? "hr_rating" : "manager_rating",
                " WHERE " + ELIGIBLE + (departmentId == null ? "" : " AND e.department_id = ?")
                + (cycleId == null ? "" : " AND a.cycle_id = ?"), filters(departmentId, cycleId).toArray());
        Map<String, Object> highest = departments.stream().max(Comparator.comparingDouble(row -> ((Number) row.get("averageRating")).doubleValue())).orElse(null);
        Map<String, Object> lowestCompletion = departments.stream().min(Comparator.comparingDouble(row -> ((Number) row.get("completionRate")).doubleValue())).orElse(null);
        if (highest != null) result.add(Map.of("label", "Highest average department", "value", highest.get("department")));
        if (lowestCompletion != null) result.add(Map.of("label", "Lowest completion department", "value", lowestCompletion.get("department")));
        return result;
    }

    private List<Object> filters(Long departmentId, Long cycleId) {
        List<Object> args = new ArrayList<>(); if (departmentId != null) args.add(departmentId); if (cycleId != null) args.add(cycleId); return args;
    }

    private String reviewerFilter(List<Object> args) {
        Long reviewerId = access.employeeIdOrNull();
        if (!access.canManage() || reviewerId != null) {
            args.add(reviewerId);
            return " AND (a.assigned_hr_reviewer_id = ? OR a.assigned_hr_reviewer_id IS NULL)";
        }
        return "";
    }

    private void history(long appraisalId, String role, String action, String oldValue, String newValue, String reason) {
        Long employeeId = access.employeeIdOrNull();
        jdbc.update("INSERT INTO pms_review_history (history_id,appraisal_id,reviewer_employee_id,reviewer_name,reviewer_role,"
                        + " action_type,old_value,new_value,reason) VALUES (pms_review_history_seq.NEXTVAL,?,?,?,?,?,?,?,?)",
                appraisalId, employeeId, access.username(), role, action, oldValue, newValue, reason);
    }

    private void addSearch(StringBuilder where, List<Object> args, String search) {
        if (!hasText(search)) return;
        where.append(" AND (LOWER(e.display_name) LIKE ? OR LOWER(e.employee_code) LIKE ?"
            + " OR LOWER(NVL(manager.display_name,' ')) LIKE ? OR LOWER(NVL(d.department_name,' ')) LIKE ?"
            + " OR LOWER(NVL(ds.designation_name,' ')) LIKE ? OR LOWER(NVL(l.location_name,' ')) LIKE ?)");
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        for (int index = 0; index < 6; index++) args.add(pattern);
    }

    private void addFilter(StringBuilder where, List<Object> args, String clause, Object value) {
        if (value == null) return;
        where.append(" AND ").append(clause); args.add(value);
    }

    private List<Object> withCycle(Long cycleId, List<Object> args) {
        List<Object> result = new ArrayList<>();
        if (cycleId != null) result.add(cycleId);
        result.addAll(args);
        return result;
    }

    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args); return value == null ? 0 : value;
    }

    private Double average(String sql, Object... args) {
        Double value = jdbc.queryForObject(sql, Double.class, args); return value;
    }

    private Map<String, Object> bucket(String status, long count) {
        return Map.of("status", status, "count", count);
    }

    private double percent(long value, long total) { return total == 0 ? 0 : round1(value * 100.0 / total); }
    private double round1(double value) { return Math.round(value * 10.0) / 10.0; }
    private double round2(Double value) { return value == null ? 0 : Math.round(value * 100.0) / 100.0; }

    private BigDecimal decimal(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try { return new BigDecimal(value.toString()); }
        catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be numeric."); }
    }

    private Long longValue(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try { return Long.valueOf(value.toString()); }
        catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID must be numeric."); }
    }

    private void validateRating(BigDecimal rating, String message) {
        if (rating == null || rating.compareTo(BigDecimal.ONE) < 0 || rating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String json(Object data) {
        try { return mapper.writeValueAsString(data); }
        catch (JsonProcessingException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review data must be valid JSON."); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String data) {
        if (data == null || data.isBlank()) return new LinkedHashMap<>();
        try { return mapper.readValue(data, Map.class); }
        catch (JsonProcessingException ex) { return new LinkedHashMap<>(); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value) { return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<>(); }

    private String text(Object value) { return value == null ? null : value.toString().trim(); }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private Long nullableLong(ResultSet rs, int index) throws SQLException { Object value = rs.getObject(index); return value == null ? null : ((Number) value).longValue(); }
    private String dateString(ResultSet rs, int index) throws SQLException { Date value = rs.getDate(index); return value == null ? null : value.toLocalDate().toString(); }
    private String clobString(ResultSet rs, String column) throws SQLException { return rs.getString(column); }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        if (safe.startsWith("=") || safe.startsWith("+") || safe.startsWith("-") || safe.startsWith("@")) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private void appendCsv(StringBuilder csv, Object... values) {
        for (int i = 0; i < values.length; i++) { if (i > 0) csv.append(','); csv.append(csv(Objects.toString(values[i], ""))); }
        csv.append("\r\n");
    }

    private record Scope(boolean global, Long employeeId) { }

    private record ReviewRow(long appraisalId, long employeeId, String employeeCode, String employeeName,
                             Long departmentId, String department, String designation, Long locationId,
                             String location, String joiningDate, Long managerId, String manager, long cycleId,
                             String cycleName, String dueDate, String selfStatus, BigDecimal selfRating,
                             String managerStatus, BigDecimal managerRating, String hrStatus, BigDecimal hrRating,
                             BigDecimal finalRating, Long reviewerId, String reviewer, String selfData,
                             String managerData, String hrData) {
        Map<String, Object> toMap() {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("appraisalId", appraisalId); item.put("employeeId", employeeId); item.put("employeeCode", employeeCode);
            item.put("employeeName", employeeName); item.put("departmentId", departmentId); item.put("department", department);
            item.put("designation", designation); item.put("locationId", locationId); item.put("location", location);
            item.put("joiningDate", joiningDate); item.put("managerId", managerId); item.put("manager", manager);
            item.put("cycleId", cycleId); item.put("cycleName", cycleName); item.put("dueDate", dueDate);
            item.put("selfStatus", selfStatus); item.put("selfRating", selfRating); item.put("managerStatus", managerStatus);
            item.put("managerRating", managerRating); item.put("hrStatus", hrStatus); item.put("hrRating", hrRating);
            item.put("finalRating", finalRating); item.put("reviewerId", reviewerId); item.put("reviewer", reviewer);
            item.put("selfData", selfData); item.put("managerData", managerData); item.put("hrData", hrData);
            long days = dueDate == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate));
            item.put("daysRemaining", Math.max(0, days)); return item;
        }
    }

    private ReviewRow mapRow(ResultSet rs) throws SQLException {
        return new ReviewRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), nullableLong(rs, 5),
                rs.getString(6), rs.getString(7), nullableLong(rs, 8), rs.getString(9), dateString(rs, 10),
                nullableLong(rs, 11), rs.getString(12), rs.getLong(13), rs.getString(14), dateString(rs, 15),
                rs.getString(16), rs.getBigDecimal(17), rs.getString(18), rs.getBigDecimal(19), rs.getString(20),
                rs.getBigDecimal(21), rs.getBigDecimal(22), nullableLong(rs, 23), rs.getString(24),
                clobString(rs, "SELF_DATA"), clobString(rs, "MANAGER_DATA"), clobString(rs, "HR_DATA"));
    }
}
