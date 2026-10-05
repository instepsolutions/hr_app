package com.hrms.pms;

import com.hrms.exception.ResourceNotFoundException;
import com.hrms.pms.PmsDtos.*;
import com.hrms.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PmsGoalService {

    private static final Set<String> TYPES = Set.of("COMPANY", "DEPARTMENT", "TEAM", "INDIVIDUAL");
    private static final Set<String> STATUSES = Set.of("NOT_STARTED", "IN_PROGRESS", "COMPLETED", "BEHIND");
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Map<String, String> SORTS = Map.ofEntries(
            Map.entry("title", "g.title"),
            Map.entry("employee", "e.display_name"),
            Map.entry("department", "d.department_name"),
            Map.entry("category", "c.category_name"),
            Map.entry("goalType", "g.goal_type"),
            Map.entry("priority", "g.priority"),
            Map.entry("progress", "g.progress_percent"),
            Map.entry("status", "g.status"),
            Map.entry("startDate", "g.start_date"),
            Map.entry("dueDate", "g.due_date"),
            Map.entry("updatedAt", "g.updated_at"));

    private static final String SELECT = "SELECT g.goal_id, g.title, g.description, g.employee_id, e.display_name, e.employee_code,"
            + " g.department_id, d.department_name, COALESCE(dg.group_name, 'Others'), g.category_id, c.category_name,"
            + " g.goal_type, g.kra_id, kr.kra_name, g.kpi_id, kp.kpi_name, g.weightage, g.target_value, g.unit,"
            + " g.progress_percent, g.priority, g.alignment_goal_id, pg.title, g.manager_id, m.display_name, g.status,"
            + " g.start_date, g.due_date, g.completed_at, g.is_archived, g.archived_at, g.archived_by, g.archive_reason,"
            + " g.updated_at";
    private static final String FROM = " FROM pms_goal g JOIN employee e ON e.employee_id = g.employee_id"
            + " LEFT JOIN department d ON d.department_id = g.department_id"
            + " LEFT JOIN pms_department_group_v dg ON dg.department_id = g.department_id"
            + " LEFT JOIN pms_goal_category c ON c.category_id = g.category_id"
            + " LEFT JOIN pms_kra kr ON kr.kra_id = g.kra_id"
            + " LEFT JOIN pms_kpi kp ON kp.kpi_id = g.kpi_id"
            + " LEFT JOIN pms_goal pg ON pg.goal_id = g.alignment_goal_id"
            + " LEFT JOIN employee m ON m.employee_id = g.manager_id";

    private final JdbcTemplate jdbc;
    private final PmsAccess access;
    private final AuditLogService auditLogService;

    public PmsGoalService(JdbcTemplate jdbc, PmsAccess access, AuditLogService auditLogService) {
        this.jdbc = jdbc;
        this.access = access;
        this.auditLogService = auditLogService;
    }

    public record GoalFilter(String search, String status, String department, String goalType, String priority,
                             Long categoryId, LocalDate dueFrom, LocalDate dueTo) {
    }

    private final RowMapper<GoalRow> goalMapper = (rs, n) -> {
        Date start = rs.getDate(27);
        Date due = rs.getDate(28);
        Timestamp completed = rs.getTimestamp(29);
        Timestamp archivedAt = rs.getTimestamp(31);
        Timestamp updated = rs.getTimestamp(34);
        boolean archived = rs.getInt(30) == 1;
        String status = rs.getString(26);
        LocalDate dueDate = due.toLocalDate();
        boolean overdue = !archived && !"COMPLETED".equals(status) && dueDate.isBefore(LocalDate.now());
        return new GoalRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getString(5),
                rs.getString(6), nullableLong(rs.getObject(7)), rs.getString(8), rs.getString(9),
                nullableLong(rs.getObject(10)), rs.getString(11), rs.getString(12), nullableLong(rs.getObject(13)),
                rs.getString(14), nullableLong(rs.getObject(15)), rs.getString(16), rs.getBigDecimal(17),
                rs.getBigDecimal(18), rs.getString(19), rs.getBigDecimal(20), rs.getString(21),
                nullableLong(rs.getObject(22)), rs.getString(23), nullableLong(rs.getObject(24)), rs.getString(25),
                status, start.toLocalDate(), dueDate, completed == null ? null : completed.toLocalDateTime(),
                archived, archivedAt == null ? null : archivedAt.toLocalDateTime(), rs.getString(32), rs.getString(33),
                updated == null ? null : updated.toLocalDateTime(), overdue);
    };

    private static Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    // ------------------------------------------------------------------ queries

    private record Where(String sql, List<Object> args) {
    }

    private Where where(String requestedScope, GoalFilter f) {
        String scope = requestedScope == null ? "ALL" : requestedScope.toUpperCase(Locale.ROOT);
        if (access.employeeOnly()) {
            scope = "MY";
        }
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        switch (scope) {
            case "ARCHIVED" -> sql.append(" WHERE g.is_archived = 1");
            case "MY" -> {
                Long me = access.employeeIdOrNull();
                sql.append(" WHERE g.is_archived = 0 AND g.employee_id = ?");
                args.add(me == null ? -1L : me);
            }
            case "TEAM" -> {
                Long me = access.employeeIdOrNull();
                sql.append(" WHERE g.is_archived = 0 AND g.employee_id <> ? AND (g.manager_id = ? OR e.reporting_manager_id = ?)");
                long id = me == null ? -1L : me;
                args.add(id);
                args.add(id);
                args.add(id);
            }
            case "ALL" -> sql.append(" WHERE g.is_archived = 0");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown goal scope: " + requestedScope);
        }
        if (f != null) {
            if (hasText(f.search())) {
                String like = "%" + f.search().trim().toLowerCase(Locale.ROOT) + "%";
                sql.append(" AND (LOWER(g.title) LIKE ? OR LOWER(e.display_name) LIKE ? OR LOWER(e.employee_code) LIKE ?)");
                args.add(like);
                args.add(like);
                args.add(like);
            }
            if (hasText(f.status())) {
                requireOneOf(f.status(), STATUSES, "status");
                sql.append(" AND g.status = ?");
                args.add(f.status());
            }
            if (hasText(f.department())) {
                sql.append(" AND COALESCE(dg.group_name, 'Others') = ?");
                args.add(f.department());
            }
            if (hasText(f.goalType())) {
                requireOneOf(f.goalType(), TYPES, "goal type");
                sql.append(" AND g.goal_type = ?");
                args.add(f.goalType());
            }
            if (hasText(f.priority())) {
                requireOneOf(f.priority(), PRIORITIES, "priority");
                sql.append(" AND g.priority = ?");
                args.add(f.priority());
            }
            if (f.categoryId() != null) {
                sql.append(" AND g.category_id = ?");
                args.add(f.categoryId());
            }
            if (f.dueFrom() != null) {
                sql.append(" AND g.due_date >= ?");
                args.add(Date.valueOf(f.dueFrom()));
            }
            if (f.dueTo() != null) {
                sql.append(" AND g.due_date <= ?");
                args.add(Date.valueOf(f.dueTo()));
            }
        }
        return new Where(sql.toString(), args);
    }

    public GoalPage list(String scope, GoalFilter filter, int page, int size, String sort, String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be >= 0 and size between 1 and 100");
        }
        Where w = where(scope, filter);
        String sortColumn = SORTS.getOrDefault(sort == null ? "" : sort, "g.updated_at");
        String dir = "asc".equalsIgnoreCase(direction) ? "ASC" : "DESC";
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + FROM + w.sql(), Long.class, w.args().toArray());
        long totalElements = total == null ? 0 : total;
        List<Object> args = new ArrayList<>(w.args());
        args.add((long) page * size);
        args.add(size);
        List<GoalRow> rows = jdbc.query(SELECT + FROM + w.sql() + " ORDER BY " + sortColumn + " " + dir
                + ", g.goal_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", goalMapper, args.toArray());
        int totalPages = (int) Math.ceil(totalElements / (double) size);
        return new GoalPage(rows, totalElements, totalPages, page, size);
    }

    public GoalRow get(long goalId) {
        List<GoalRow> rows = jdbc.query(SELECT + FROM + " WHERE g.goal_id = ?", goalMapper, goalId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Goal " + goalId + " was not found");
        }
        GoalRow row = rows.get(0);
        if (access.employeeOnly()) {
            Long me = access.employeeIdOrNull();
            if (me == null || !me.equals(row.employeeId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own goals");
            }
        }
        return row;
    }

    public List<GoalHistoryRow> history(long goalId) {
        get(goalId);
        return jdbc.query("SELECT action_type, old_value, new_value, changed_by, changed_at FROM pms_goal_history"
                        + " WHERE goal_id = ? ORDER BY changed_at DESC, history_id DESC FETCH FIRST 50 ROWS ONLY",
                (rs, n) -> new GoalHistoryRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getTimestamp(5).toLocalDateTime()), goalId);
    }

    public List<GoalRow> myGoals(Long employeeId, int limit) {
        if (employeeId == null) {
            return List.of();
        }
        return jdbc.query(SELECT + FROM + " WHERE g.is_archived = 0 AND g.employee_id = ?"
                + " ORDER BY g.updated_at DESC, g.goal_id FETCH FIRST " + Math.max(1, Math.min(limit, 20)) + " ROWS ONLY",
                goalMapper, employeeId);
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public GoalRow create(GoalRequest request) {
        requireManage();
        validate(request, null);
        Long employeeDepartment = jdbc.queryForObject("SELECT department_id FROM employee WHERE employee_id = ?",
                Long.class, request.employeeId());
        Long employeeManager = jdbc.queryForObject("SELECT reporting_manager_id FROM employee WHERE employee_id = ?",
                Long.class, request.employeeId());
        Long departmentId = request.departmentId() != null ? request.departmentId() : employeeDepartment;
        Long managerId = request.managerId() != null ? request.managerId() : employeeManager;
        String[] resolved = resolve(request.status(), request.progressPercent());
        Long goalId = jdbc.queryForObject("SELECT pms_goal_seq.NEXTVAL FROM dual", Long.class);
        String actor = access.username();
        jdbc.update("INSERT INTO pms_goal (goal_id, title, description, employee_id, department_id, category_id, goal_type,"
                        + " kra_id, kpi_id, weightage, target_value, unit, progress_percent, priority, alignment_goal_id,"
                        + " manager_id, status, start_date, due_date, completed_at, is_archived, created_by, updated_by)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)",
                goalId, request.title().trim(), blankToNull(request.description()), request.employeeId(), departmentId,
                request.categoryId(), request.goalType(), request.kraId(), request.kpiId(), request.weightage(),
                request.targetValue(), request.unit().trim(), new BigDecimal(resolved[1]), request.priority(),
                request.alignmentGoalId(), managerId, resolved[0], Date.valueOf(request.startDate()),
                Date.valueOf(request.dueDate()), "COMPLETED".equals(resolved[0]) ? Timestamp.valueOf(LocalDateTime.now()) : null,
                actor, actor);
        record(goalId, "CREATED", null, resolved[0] + " " + resolved[1] + "%");
        snapshot(goalId);
        auditLogService.record("PMS_GOAL", goalId, "CREATE", null, summary(request.title(), resolved));
        return get(goalId);
    }

    @Transactional
    public GoalRow update(long goalId, GoalRequest request) {
        requireManage();
        GoalRow current = get(goalId);
        validate(request, goalId);
        Long departmentId = request.departmentId() != null ? request.departmentId() : current.departmentId();
        Long managerId = request.managerId() != null ? request.managerId() : current.managerId();
        BigDecimal requestedProgress = request.progressPercent() != null ? request.progressPercent() : current.progressPercent();
        String[] resolved = resolve(request.status(), requestedProgress);
        boolean wasCompleted = "COMPLETED".equals(current.status());
        boolean nowCompleted = "COMPLETED".equals(resolved[0]);
        Timestamp completedAt = nowCompleted
                ? (wasCompleted && current.completedAt() != null ? Timestamp.valueOf(current.completedAt()) : Timestamp.valueOf(LocalDateTime.now()))
                : null;
        jdbc.update("UPDATE pms_goal SET title = ?, description = ?, employee_id = ?, department_id = ?, category_id = ?,"
                        + " goal_type = ?, kra_id = ?, kpi_id = ?, weightage = ?, target_value = ?, unit = ?, progress_percent = ?,"
                        + " priority = ?, alignment_goal_id = ?, manager_id = ?, status = ?, start_date = ?, due_date = ?,"
                        + " completed_at = ?, updated_at = SYSTIMESTAMP, updated_by = ? WHERE goal_id = ?",
                request.title().trim(), blankToNull(request.description()), request.employeeId(), departmentId,
                request.categoryId(), request.goalType(), request.kraId(), request.kpiId(), request.weightage(),
                request.targetValue(), request.unit().trim(), new BigDecimal(resolved[1]), request.priority(),
                request.alignmentGoalId(), managerId, resolved[0], Date.valueOf(request.startDate()),
                Date.valueOf(request.dueDate()), completedAt, access.username(), goalId);
        record(goalId, "UPDATED", current.status() + " " + current.progressPercent() + "%", resolved[0] + " " + resolved[1] + "%");
        snapshot(goalId);
        auditLogService.record("PMS_GOAL", goalId, "UPDATE", summary(current.title(), current.status(), current.progressPercent()),
                summary(request.title(), resolved));
        return get(goalId);
    }

    @Transactional
    public GoalRow updateProgress(long goalId, ProgressRequest request) {
        GoalRow current = get(goalId);
        if (current.archived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived goals cannot be updated. Restore the goal first.");
        }
        if (!access.canManage()) {
            Long me = access.employeeIdOrNull();
            if (me == null || !me.equals(current.employeeId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update progress on your own goals");
            }
        }
        String requestedStatus = hasText(request.status()) ? request.status() : null;
        if (requestedStatus != null) {
            requireOneOf(requestedStatus, STATUSES, "status");
        }
        BigDecimal progress = request.progressPercent();
        String status;
        if (progress.compareTo(BigDecimal.valueOf(100)) >= 0) {
            status = "COMPLETED";
        } else if (progress.signum() == 0 && requestedStatus == null) {
            status = "NOT_STARTED";
        } else if (requestedStatus == null || "COMPLETED".equals(requestedStatus) || "NOT_STARTED".equals(requestedStatus)) {
            status = "BEHIND".equals(current.status()) ? "BEHIND" : "IN_PROGRESS";
        } else {
            status = requestedStatus;
        }
        boolean nowCompleted = "COMPLETED".equals(status);
        Timestamp completedAt = nowCompleted
                ? ("COMPLETED".equals(current.status()) && current.completedAt() != null
                ? Timestamp.valueOf(current.completedAt()) : Timestamp.valueOf(LocalDateTime.now()))
                : null;
        jdbc.update("UPDATE pms_goal SET progress_percent = ?, status = ?, completed_at = ?, updated_at = SYSTIMESTAMP,"
                + " updated_by = ? WHERE goal_id = ?", progress, status, completedAt, access.username(), goalId);
        String note = hasText(request.comment()) ? " (" + request.comment().trim() + ")" : "";
        record(goalId, "PROGRESS", current.status() + " " + current.progressPercent() + "%",
                status + " " + progress.stripTrailingZeros().toPlainString() + "%" + note);
        snapshot(goalId);
        auditLogService.record("PMS_GOAL", goalId, "PROGRESS", summary(current.title(), current.status(), current.progressPercent()),
                summary(current.title(), status, progress));
        return get(goalId);
    }

    @Transactional
    public GoalRow archive(long goalId, String reason) {
        requireManage();
        GoalRow current = get(goalId);
        if (current.archived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Goal is already archived");
        }
        String why = hasText(reason) ? reason.trim() : "Archived";
        jdbc.update("UPDATE pms_goal SET is_archived = 1, archived_at = SYSTIMESTAMP, archived_by = ?, archive_reason = ?,"
                + " updated_at = SYSTIMESTAMP, updated_by = ? WHERE goal_id = ?", access.username(), why, access.username(), goalId);
        record(goalId, "ARCHIVED", null, why);
        auditLogService.record("PMS_GOAL", goalId, "ARCHIVE", summary(current.title(), current.status(), current.progressPercent()), why);
        return get(goalId);
    }

    @Transactional
    public GoalRow restore(long goalId) {
        requireManage();
        GoalRow current = get(goalId);
        if (!current.archived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Goal is not archived");
        }
        jdbc.update("UPDATE pms_goal SET is_archived = 0, archived_at = NULL, archived_by = NULL, archive_reason = NULL,"
                + " updated_at = SYSTIMESTAMP, updated_by = ? WHERE goal_id = ?", access.username(), goalId);
        record(goalId, "RESTORED", null, current.status() + " " + current.progressPercent() + "%");
        snapshot(goalId);
        auditLogService.record("PMS_GOAL", goalId, "RESTORE", null, summary(current.title(), current.status(), current.progressPercent()));
        return get(goalId);
    }

    @Transactional
    public void delete(long goalId) {
        requireManage();
        GoalRow current = get(goalId);
        jdbc.update("DELETE FROM pms_goal WHERE goal_id = ?", goalId);
        auditLogService.record("PMS_GOAL", goalId, "DELETE", summary(current.title(), current.status(), current.progressPercent()), null);
    }

    // ------------------------------------------------------------------ alignment / calendar / export / lookups

    private record NodeRow(long goalId, String title, String type, String owner, String status, BigDecimal progress,
                           Long parentId, long childCount) {
    }

    private static final String NODE_SELECT = "SELECT g.goal_id, g.title, g.goal_type, e.display_name, g.status, g.progress_percent,"
            + " g.alignment_goal_id, (SELECT COUNT(*) FROM pms_goal ch WHERE ch.alignment_goal_id = g.goal_id AND ch.is_archived = 0)"
            + " FROM pms_goal g JOIN employee e ON e.employee_id = g.employee_id WHERE g.is_archived = 0";

    private final RowMapper<NodeRow> nodeMapper = (rs, n) -> new NodeRow(rs.getLong(1), rs.getString(2), rs.getString(3),
            rs.getString(4), rs.getString(5), rs.getBigDecimal(6), nullableLong(rs.getObject(7)), rs.getLong(8));

    public List<AlignmentNode> alignmentTree() {
        if (access.employeeOnly()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Goal alignment is not available for this role");
        }
        List<NodeRow> roots = jdbc.query(NODE_SELECT + " AND g.goal_type = 'COMPANY'"
                + " AND EXISTS (SELECT 1 FROM pms_goal ch WHERE ch.alignment_goal_id = g.goal_id AND ch.is_archived = 0)"
                + " ORDER BY (SELECT COUNT(*) FROM pms_goal ch WHERE ch.alignment_goal_id = g.goal_id AND ch.is_archived = 0) DESC,"
                + " g.goal_id FETCH FIRST 10 ROWS ONLY", nodeMapper);
        Map<Long, List<NodeRow>> childrenByParent = new HashMap<>();
        List<NodeRow> level = roots;
        int[] limits = {80, 160, 320};
        for (int depth = 0; depth < limits.length && !level.isEmpty(); depth++) {
            List<Object> ids = new ArrayList<>();
            StringBuilder in = new StringBuilder();
            for (NodeRow node : level) {
                if (node.childCount() > 0) {
                    in.append(ids.isEmpty() ? "?" : ",?");
                    ids.add(node.goalId());
                }
            }
            if (ids.isEmpty()) {
                break;
            }
            List<NodeRow> children = jdbc.query(NODE_SELECT + " AND g.alignment_goal_id IN (" + in + ")"
                    + " ORDER BY g.alignment_goal_id, g.goal_id FETCH FIRST " + limits[depth] + " ROWS ONLY", nodeMapper, ids.toArray());
            for (NodeRow child : children) {
                childrenByParent.computeIfAbsent(child.parentId(), key -> new ArrayList<>()).add(child);
            }
            level = children;
        }
        List<AlignmentNode> tree = new ArrayList<>();
        for (NodeRow root : roots) {
            tree.add(toNode(root, childrenByParent));
        }
        return tree;
    }

    private AlignmentNode toNode(NodeRow row, Map<Long, List<NodeRow>> childrenByParent) {
        List<AlignmentNode> children = new ArrayList<>();
        for (NodeRow child : childrenByParent.getOrDefault(row.goalId(), List.of())) {
            children.add(toNode(child, childrenByParent));
        }
        return new AlignmentNode(row.goalId(), row.title(), row.type(), row.owner(), row.status(), row.progress(),
                row.childCount(), children);
    }

    public List<CalendarEvent> calendar(String scope, LocalDate from, LocalDate to, String department) {
        if (from == null || to == null || to.isBefore(from) || from.plusDays(62).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Calendar range must be between 1 and 62 days");
        }
        Where w = where(scope, null);
        List<Object> args = new ArrayList<>(w.args());
        if (hasText(department)) {
            w = new Where(w.sql() + " AND COALESCE(dg.group_name, 'Others') = ?", args);
            args.add(department);
        }
        args.add(Date.valueOf(from));
        args.add(Date.valueOf(to));
        args.add(Date.valueOf(from));
        args.add(Date.valueOf(to));
        List<GoalRow> goals = jdbc.query(SELECT + FROM + w.sql()
                + " AND ((g.start_date BETWEEN ? AND ?) OR (g.due_date BETWEEN ? AND ?))"
                + " ORDER BY g.due_date, g.goal_id FETCH FIRST 600 ROWS ONLY", goalMapper, args.toArray());
        List<CalendarEvent> events = new ArrayList<>();
        for (GoalRow goal : goals) {
            if (!goal.startDate().isBefore(from) && !goal.startDate().isAfter(to)) {
                events.add(new CalendarEvent(goal.goalId(), goal.title(), "START", goal.startDate(), goal.status(),
                        goal.employeeName(), goal.priority()));
            }
            if (!goal.dueDate().isBefore(from) && !goal.dueDate().isAfter(to)) {
                events.add(new CalendarEvent(goal.goalId(), goal.title(), "DUE", goal.dueDate(), goal.status(),
                        goal.employeeName(), goal.priority()));
            }
        }
        events.sort((a, b) -> a.date().compareTo(b.date()));
        return events;
    }

    public String exportCsv(String scope, GoalFilter filter) {
        Where w = where(scope, filter);
        List<GoalRow> rows = jdbc.query(SELECT + FROM + w.sql() + " ORDER BY g.due_date, g.goal_id FETCH FIRST 5000 ROWS ONLY",
                goalMapper, w.args().toArray());
        StringBuilder csv = new StringBuilder("Goal ID,Title,Employee,Employee Code,Department,Category,Goal Type,Priority,"
                + "Weightage,Target,Unit,Progress %,Status,Start Date,Due Date,Manager\n");
        for (GoalRow g : rows) {
            csv.append(g.goalId()).append(',').append(PmsAnalyticsService.csv(g.title())).append(',')
                    .append(PmsAnalyticsService.csv(g.employeeName())).append(',').append(PmsAnalyticsService.csv(g.employeeCode()))
                    .append(',').append(PmsAnalyticsService.csv(g.department())).append(',')
                    .append(PmsAnalyticsService.csv(g.category())).append(',').append(g.goalType()).append(',')
                    .append(g.priority()).append(',').append(g.weightage()).append(',').append(g.targetValue()).append(',')
                    .append(PmsAnalyticsService.csv(g.unit())).append(',').append(g.progressPercent()).append(',')
                    .append(g.status()).append(',').append(g.startDate()).append(',').append(g.dueDate()).append(',')
                    .append(PmsAnalyticsService.csv(g.managerName())).append('\n');
        }
        return csv.toString();
    }

    public List<LookupItem> categories() {
        return jdbc.query("SELECT category_id, category_name FROM pms_goal_category WHERE status = 'ACTIVE' ORDER BY category_name",
                (rs, n) -> new LookupItem(rs.getLong(1), rs.getString(2), null, null, null));
    }

    public List<LookupItem> kras() {
        return jdbc.query("SELECT kra_id, kra_name, category_id FROM pms_kra WHERE status = 'ACTIVE' ORDER BY kra_name",
                (rs, n) -> new LookupItem(rs.getLong(1), rs.getString(2), nullableLong(rs.getObject(3)), null, null));
    }

    public List<LookupItem> kpis() {
        return jdbc.query("SELECT kpi_id, kpi_name, kra_id, unit, target_value FROM pms_kpi WHERE status = 'ACTIVE' ORDER BY kpi_name",
                (rs, n) -> new LookupItem(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getBigDecimal(5)));
    }

    // ------------------------------------------------------------------ helpers

    private void requireManage() {
        if (!access.canManage()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your role is not allowed to change goals");
        }
    }

    private void validate(GoalRequest r, Long goalId) {
        requireOneOf(r.goalType(), TYPES, "goal type");
        requireOneOf(r.priority(), PRIORITIES, "priority");
        requireOneOf(r.status(), STATUSES, "status");
        if (r.dueDate().isBefore(r.startDate())) {
            throw new IllegalArgumentException("Due date must be on or after the start date");
        }
        requireExists("employee", "employee_id", r.employeeId(), "Employee");
        requireExistsIfPresent("department", "department_id", r.departmentId(), "Department");
        requireExistsIfPresent("pms_goal_category", "category_id", r.categoryId(), "Category");
        requireExistsIfPresent("pms_kra", "kra_id", r.kraId(), "KRA");
        requireExistsIfPresent("pms_kpi", "kpi_id", r.kpiId(), "KPI");
        requireExistsIfPresent("employee", "employee_id", r.managerId(), "Manager");
        if (r.alignmentGoalId() != null) {
            requireExists("pms_goal", "goal_id", r.alignmentGoalId(), "Aligned goal");
            if (goalId != null) {
                Long cursor = r.alignmentGoalId();
                for (int hops = 0; cursor != null && hops < 20; hops++) {
                    if (cursor.equals(goalId)) {
                        throw new IllegalArgumentException("A goal cannot be aligned to itself or to one of its own sub-goals");
                    }
                    List<Long> parents = jdbc.query("SELECT alignment_goal_id FROM pms_goal WHERE goal_id = ?",
                            (rs, n) -> nullableLong(rs.getObject(1)), cursor);
                    cursor = parents.isEmpty() ? null : parents.get(0);
                }
            }
        }
    }

    /** Table and column names are internal literals, never user input. */
    private void requireExists(String table, String column, Long id, String label) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Long.class, id);
        if (count == null || count == 0) {
            throw new IllegalArgumentException(label + " " + id + " does not exist");
        }
    }

    private void requireExistsIfPresent(String table, String column, Long id, String label) {
        if (id != null) {
            requireExists(table, column, id, label);
        }
    }

    private static void requireOneOf(String value, Set<String> allowed, String label) {
        if (value == null || !allowed.contains(value)) {
            throw new IllegalArgumentException("Invalid " + label + ": " + value);
        }
    }

    /** Returns {status, progress}. Status and progress are kept consistent: 100 means completed, 0 not started. */
    private static String[] resolve(String status, BigDecimal progress) {
        BigDecimal value = progress == null ? BigDecimal.ZERO : progress;
        if ("COMPLETED".equals(status) || value.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return new String[]{"COMPLETED", "100"};
        }
        if ("NOT_STARTED".equals(status)) {
            return new String[]{"NOT_STARTED", "0"};
        }
        return new String[]{status, value.stripTrailingZeros().toPlainString()};
    }

    private void record(long goalId, String action, String oldValue, String newValue) {
        jdbc.update("INSERT INTO pms_goal_history (history_id, goal_id, action_type, old_value, new_value, changed_by)"
                        + " VALUES (pms_goal_history_seq.NEXTVAL, ?, ?, ?, ?, ?)", goalId, action, truncate(oldValue),
                truncate(newValue), access.username());
    }

    /** Keeps today's snapshot in step with the goal so dashboard KPIs, charts and tables stay consistent. */
    private void snapshot(long goalId) {
        jdbc.update("MERGE INTO pms_goal_progress t USING (SELECT goal_id, progress_percent, status FROM pms_goal WHERE goal_id = ?) s"
                + " ON (t.goal_id = s.goal_id AND t.recorded_on = TRUNC(SYSDATE))"
                + " WHEN MATCHED THEN UPDATE SET t.progress_percent = s.progress_percent, t.status = s.status, t.updated_by = ?"
                + " WHEN NOT MATCHED THEN INSERT (progress_id, goal_id, recorded_on, progress_percent, status, updated_by)"
                + " VALUES (pms_goal_progress_seq.NEXTVAL, s.goal_id, TRUNC(SYSDATE), s.progress_percent, s.status, ?)",
                goalId, access.username(), access.username());
    }

    private static Map<String, Object> summary(String title, String[] resolved) {
        return summary(title, resolved[0], new BigDecimal(resolved[1]));
    }

    private static Map<String, Object> summary(String title, String status, BigDecimal progress) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("status", status);
        map.put("progress", progress);
        return map;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static String truncate(String value) {
        return value == null || value.length() <= 500 ? value : value.substring(0, 500);
    }
}
