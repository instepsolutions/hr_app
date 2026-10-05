package com.hrms.pms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.pms.PmsWorkspaceDtos.CommentRow;
import com.hrms.pms.PmsWorkspaceDtos.DepartmentMapping;
import com.hrms.pms.PmsWorkspaceDtos.EmployeeKpiMapping;
import com.hrms.pms.PmsWorkspaceDtos.EmployeeKraMapping;
import com.hrms.pms.PmsWorkspaceDtos.EmployeeMapping;
import com.hrms.pms.PmsWorkspaceDtos.EmployeeMappingRequest;
import com.hrms.pms.PmsWorkspaceDtos.IdMappingRequest;
import com.hrms.pms.PmsWorkspaceDtos.KpiRequest;
import com.hrms.pms.PmsWorkspaceDtos.KpiRow;
import com.hrms.pms.PmsWorkspaceDtos.KraRequest;
import com.hrms.pms.PmsWorkspaceDtos.KraRow;
import com.hrms.pms.PmsWorkspaceDtos.SetupHistoryRow;
import com.hrms.pms.PmsWorkspaceDtos.TimelineStage;
import com.hrms.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.hrms.pms.PmsWorkspaceDtos.*;

@Service
public class PmsWorkspaceService {

    private static final Set<String> KRA_STATUSES = Set.of("ACTIVE", "INACTIVE", "DRAFT", "UNDER_REVIEW");
    private static final Set<String> KRA_OWNERS = Set.of("MANAGER", "EMPLOYEE", "SHARED", "HR");
    private static final Set<String> MEASUREMENTS = Set.of("NUMBER", "PERCENTAGE", "CURRENCY", "RATING", "BOOLEAN", "RATIO");
    private static final Set<String> FREQUENCIES = Set.of("DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY");
    private static final List<String> TIMELINE_STAGES = List.of("SELF_APPRAISAL", "MANAGER_REVIEW", "HR_REVIEW", "FINALIZATION");
    private static final String KRA_SELECT = "SELECT k.kra_id, k.kra_name, k.category_id, c.category_name, k.description,"
            + " COALESCE(k.department_id, (SELECT MIN(m.department_id) FROM pms_kra_department m WHERE m.kra_id = k.kra_id)),"
            + " COALESCE(d.department_name, 'Unmapped'), k.owner_type, k.owner_employee_id,"
            + " COALESCE(o.display_name, CASE k.owner_type WHEN 'HR' THEN 'Human Resources' ELSE k.owner_type END),"
            + " k.weightage, k.status, k.effective_date,"
            + " (SELECT COUNT(*) FROM pms_kpi kp WHERE kp.kra_id = k.kra_id),"
            + " (SELECT COUNT(DISTINCT m.employee_id) FROM pms_employee_kra m WHERE m.kra_id = k.kra_id),"
            + " TRUNC(k.created_at) FROM pms_kra k LEFT JOIN pms_goal_category c ON c.category_id = k.category_id"
            + " LEFT JOIN department d ON d.department_id = COALESCE(k.department_id,"
            + " (SELECT MIN(m.department_id) FROM pms_kra_department m WHERE m.kra_id = k.kra_id))"
            + " LEFT JOIN employee o ON o.employee_id = k.owner_employee_id";
    private static final String KPI_SELECT = "SELECT kp.kpi_id, kp.kpi_name, kp.kra_id, k.kra_name, kp.measurement_type,"
            + " kp.target_value, kp.unit, kp.weightage, COALESCE(kp.department_id, k.department_id),"
            + " COALESCE(d.department_name, 'Unmapped'), kp.owner_type, kp.owner_employee_id,"
            + " COALESCE(o.display_name, CASE kp.owner_type WHEN 'HR' THEN 'Human Resources' ELSE kp.owner_type END),"
            + " kp.frequency, kp.status, TRUNC(kp.created_at) FROM pms_kpi kp JOIN pms_kra k ON k.kra_id = kp.kra_id"
            + " LEFT JOIN department d ON d.department_id = COALESCE(kp.department_id, k.department_id)"
            + " LEFT JOIN employee o ON o.employee_id = kp.owner_employee_id";

    private final JdbcTemplate jdbc;
    private final PmsAccess access;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public PmsWorkspaceService(JdbcTemplate jdbc, PmsAccess access, AuditLogService auditLogService,
                               ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.access = access;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public List<KraRow> listKras(String search, Long departmentId, Long categoryId, String status, String owner,
                                 String sort, String direction) {
        requireManage();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        addSearch(where, args, search, "LOWER(k.kra_name) LIKE ? OR LOWER(NVL(k.description, ' ')) LIKE ?");
        addOptional(where, args, departmentId, "EXISTS (SELECT 1 FROM pms_kra_department m WHERE m.kra_id = k.kra_id AND m.department_id = ?)");
        addOptional(where, args, categoryId, "k.category_id = ?");
        addTextFilter(where, args, status, "k.status = ?");
        addTextFilter(where, args, owner, "k.owner_type = ?");
        List<KraRow> rows = jdbc.query(KRA_SELECT + where + " ORDER BY " + kraSort(sort) + sortDirection(direction),
                (rs, rowNum) -> mapKra(rs), args.toArray());
        if (departmentId != null) {
            rows = rows.stream().filter(row -> jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pms_kra_department WHERE kra_id = ? AND department_id = ?",
                    Integer.class, row.kraId(), departmentId) > 0).toList();
        }
        return rows;
    }

    @Transactional
    public KraRow createKra(KraRequest request) {
        requireManage();
        validateKra(request);
        long id = nextId("pms_kra_seq");
        String actor = access.username();
        jdbc.update("INSERT INTO pms_kra (kra_id, kra_name, category_id, description, status, department_id, owner_type,"
                        + " owner_employee_id, weightage, effective_date, created_by, updated_by)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, request.kraName().trim(), request.categoryId(), blankToNull(request.description()), request.status(),
                request.departmentId(), request.ownerType(), request.ownerEmployeeId(), request.weightage(),
                Date.valueOf(request.effectiveDate()), actor, actor);
        if (request.departmentId() != null) {
            jdbc.update("INSERT INTO pms_kra_department (kra_id, department_id, mapped_by) VALUES (?, ?, ?)",
                    id, request.departmentId(), actor);
        }
        recordSetup("CREATED", "KRA", id, request.kraName(), null, request.status());
        auditLogService.record("PMS_KRA", id, "CREATE", null, Map.of("name", request.kraName(), "status", request.status()));
        return findKra(id);
    }

    @Transactional
    public KraRow updateKra(long id, KraRequest request) {
        requireManage();
        validateKra(request);
        KraRow old = findKra(id);
        jdbc.update("UPDATE pms_kra SET kra_name = ?, category_id = ?, description = ?, status = ?, department_id = ?,"
                        + " owner_type = ?, owner_employee_id = ?, weightage = ?, effective_date = ?, updated_by = ?,"
                        + " updated_at = SYSTIMESTAMP WHERE kra_id = ?",
                request.kraName().trim(), request.categoryId(), blankToNull(request.description()), request.status(),
                request.departmentId(), request.ownerType(), request.ownerEmployeeId(), request.weightage(),
                Date.valueOf(request.effectiveDate()), access.username(), id);
        replaceDepartmentKraMappings(id, request.departmentId());
        recordSetup("UPDATED", "KRA", id, request.kraName(), old.status(), request.status());
        auditLogService.record("PMS_KRA", id, "UPDATE", Map.of("name", old.kraName(), "status", old.status()),
                Map.of("name", request.kraName(), "status", request.status()));
        return findKra(id);
    }

    @Transactional
    public void deleteKra(long id) {
        requireManage();
        KraRow row = findKra(id);
        int goalLinks = count("SELECT COUNT(*) FROM pms_goal WHERE kra_id = ?", id);
        int kpiGoalLinks = count("SELECT COUNT(*) FROM pms_goal g JOIN pms_kpi k ON k.kpi_id = g.kpi_id WHERE k.kra_id = ?", id);
        if (goalLinks > 0 || kpiGoalLinks > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This KRA is linked to goals and cannot be deleted.");
        }
        jdbc.update("DELETE FROM pms_kpi WHERE kra_id = ?", id);
        jdbc.update("DELETE FROM pms_kra WHERE kra_id = ?", id);
        recordSetup("DELETED", "KRA", id, row.kraName(), row.status(), null);
        auditLogService.record("PMS_KRA", id, "DELETE", Map.of("name", row.kraName()), null);
    }

    @Transactional
    public KraRow duplicateKra(long id) {
        requireManage();
        KraRow row = findKra(id);
        KraRequest copy = new KraRequest(uniqueName(row.kraName(), "KRA"), row.categoryId(), row.description(),
                row.departmentId(), row.ownerType(), row.ownerEmployeeId(), row.weightage(), "DRAFT",
                LocalDate.now());
        return createKra(copy);
    }

    public List<KpiRow> listKpis(String search, Long departmentId, Long kraId, String status, String owner,
                                 String sort, String direction) {
        requireManage();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        addSearch(where, args, search, "LOWER(kp.kpi_name) LIKE ? OR LOWER(k.kra_name) LIKE ?");
        addOptional(where, args, departmentId, "EXISTS (SELECT 1 FROM pms_kpi_department m WHERE m.kpi_id = kp.kpi_id AND m.department_id = ?)");
        addOptional(where, args, kraId, "kp.kra_id = ?");
        addTextFilter(where, args, status, "kp.status = ?");
        addTextFilter(where, args, owner, "kp.owner_type = ?");
        List<KpiRow> rows = jdbc.query(KPI_SELECT + where + " ORDER BY " + kpiSort(sort) + sortDirection(direction),
                (rs, rowNum) -> mapKpi(rs), args.toArray());
        if (departmentId != null) {
            rows = rows.stream().filter(row -> jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pms_kpi_department WHERE kpi_id = ? AND department_id = ?",
                    Integer.class, row.kpiId(), departmentId) > 0).toList();
        }
        return rows;
    }

    @Transactional
    public KpiRow createKpi(KpiRequest request) {
        requireManage();
        validateKpi(request);
        long id = nextId("pms_kpi_seq");
        String actor = access.username();
        Long departmentId = request.departmentId() == null ? kraDepartment(request.kraId()) : request.departmentId();
        jdbc.update("INSERT INTO pms_kpi (kpi_id, kra_id, kpi_name, unit, target_value, status, department_id,"
                        + " owner_type, owner_employee_id, measurement_type, frequency, weightage, created_by, updated_by)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, request.kraId(), request.kpiName().trim(), blankToNull(request.unit()), request.targetValue(),
                request.status(), departmentId, request.ownerType(), request.ownerEmployeeId(), request.measurementType(),
                request.frequency(), request.weightage(), actor, actor);
        if (departmentId != null) {
            jdbc.update("INSERT INTO pms_kpi_department (kpi_id, department_id, mapped_by) VALUES (?, ?, ?)", id,
                    departmentId, actor);
        }
        recordSetup("CREATED", "KPI", id, request.kpiName(), null, request.status());
        auditLogService.record("PMS_KPI", id, "CREATE", null, Map.of("name", request.kpiName(), "kraId", request.kraId()));
        return findKpi(id);
    }

    @Transactional
    public KpiRow updateKpi(long id, KpiRequest request) {
        requireManage();
        validateKpi(request);
        KpiRow old = findKpi(id);
        Long departmentId = request.departmentId() == null ? kraDepartment(request.kraId()) : request.departmentId();
        jdbc.update("UPDATE pms_kpi SET kra_id = ?, kpi_name = ?, unit = ?, target_value = ?, status = ?,"
                        + " department_id = ?, owner_type = ?, owner_employee_id = ?, measurement_type = ?, frequency = ?,"
                        + " weightage = ?, updated_by = ?, updated_at = SYSTIMESTAMP WHERE kpi_id = ?",
                request.kraId(), request.kpiName().trim(), blankToNull(request.unit()), request.targetValue(),
                request.status(), departmentId, request.ownerType(), request.ownerEmployeeId(), request.measurementType(),
                request.frequency(), request.weightage(), access.username(), id);
        replaceDepartmentKpiMappings(id, departmentId);
        recordSetup("UPDATED", "KPI", id, request.kpiName(), old.status(), request.status());
        auditLogService.record("PMS_KPI", id, "UPDATE", Map.of("name", old.kpiName(), "status", old.status()),
                Map.of("name", request.kpiName(), "status", request.status()));
        return findKpi(id);
    }

    @Transactional
    public void deleteKpi(long id) {
        requireManage();
        KpiRow row = findKpi(id);
        if (count("SELECT COUNT(*) FROM pms_goal WHERE kpi_id = ?", id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This KPI is linked to goals and cannot be deleted.");
        }
        jdbc.update("DELETE FROM pms_kpi WHERE kpi_id = ?", id);
        recordSetup("DELETED", "KPI", id, row.kpiName(), row.status(), null);
        auditLogService.record("PMS_KPI", id, "DELETE", Map.of("name", row.kpiName()), null);
    }

    @Transactional
    public KpiRow duplicateKpi(long id) {
        requireManage();
        KpiRow row = findKpi(id);
        return createKpi(new KpiRequest(row.kraId(), uniqueName(row.kpiName(), "KPI"), row.measurementType(),
                row.targetValue(), row.unit(), row.weightage(), row.departmentId(), row.ownerType(),
                row.ownerEmployeeId(), row.frequency(), "DRAFT"));
    }

    public DepartmentMapping departmentMappings(long departmentId) {
        requireManage();
        String department = jdbc.query("SELECT department_name FROM department WHERE department_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, departmentId);
        if (department == null) throw new ResourceNotFoundException("Department not found");
        List<KraRow> kras = listKras(null, departmentId, null, null, null, "kraName", "asc");
        List<KpiRow> kpis = listKpis(null, departmentId, null, null, null, "kpiName", "asc");
        return new DepartmentMapping(departmentId, department, kras, kpis);
    }

    @Transactional
    public DepartmentMapping saveDepartmentMappings(long departmentId, IdMappingRequest request) {
        requireManage();
        departmentMappings(departmentId);
        List<Long> kraIds = request.kraIds() == null ? List.of() : request.kraIds().stream().distinct().toList();
        List<Long> kpiIds = request.kpiIds() == null ? List.of() : request.kpiIds().stream().distinct().toList();
        kraIds.forEach(id -> ensureExists("SELECT COUNT(*) FROM pms_kra WHERE kra_id = ?", id, "KRA"));
        kpiIds.forEach(id -> ensureExists("SELECT COUNT(*) FROM pms_kpi WHERE kpi_id = ?", id, "KPI"));
        List<Long> oldKras = jdbc.query("SELECT kra_id FROM pms_kra_department WHERE department_id = ?",
                (rs, row) -> rs.getLong(1), departmentId);
        List<Long> oldKpis = jdbc.query("SELECT kpi_id FROM pms_kpi_department WHERE department_id = ?",
                (rs, row) -> rs.getLong(1), departmentId);
        jdbc.update("DELETE FROM pms_kra_department WHERE department_id = ?", departmentId);
        jdbc.update("DELETE FROM pms_kpi_department WHERE department_id = ?", departmentId);
        String actor = access.username();
        kraIds.forEach(id -> jdbc.update("INSERT INTO pms_kra_department (kra_id, department_id, mapped_by) VALUES (?, ?, ?)", id, departmentId, actor));
        kpiIds.forEach(id -> jdbc.update("INSERT INTO pms_kpi_department (kpi_id, department_id, mapped_by) VALUES (?, ?, ?)", id, departmentId, actor));
        recordSetup("DEPARTMENT_MAPPED", "KRA/KPI", departmentId, "Department mappings", oldKras + ";" + oldKpis,
                kraIds + ";" + kpiIds);
        return departmentMappings(departmentId);
    }

    public EmployeeMapping employeeMappings(long employeeId) {
        requireManage();
        return loadEmployeeMapping(employeeId);
    }

    @Transactional
    public EmployeeMapping saveEmployeeMappings(long employeeId, EmployeeMappingRequest request) {
        requireManage();
        EmployeeMapping before = loadEmployeeMapping(employeeId);
        List<EmployeeKraMapping> kras = request.kras() == null ? List.of() : request.kras();
        List<EmployeeKpiMapping> kpis = request.kpis() == null ? List.of() : request.kpis();
        kras.forEach(mapping -> validateMapping(mapping.kraId(), mapping.weightage(), "KRA"));
        kpis.forEach(mapping -> validateMapping(mapping.kpiId(), mapping.weightage(), "KPI"));
        jdbc.update("DELETE FROM pms_employee_kra WHERE employee_id = ?", employeeId);
        jdbc.update("DELETE FROM pms_employee_kpi WHERE employee_id = ?", employeeId);
        String actor = access.username();
        kras.forEach(mapping -> jdbc.update("INSERT INTO pms_employee_kra (employee_id, kra_id, weightage, mapped_by)"
                + " VALUES (?, ?, ?, ?)", employeeId, mapping.kraId(), mapping.weightage(), actor));
        kpis.forEach(mapping -> jdbc.update("INSERT INTO pms_employee_kpi (employee_id, kpi_id, weightage, mapped_by)"
                + " VALUES (?, ?, ?, ?)", employeeId, mapping.kpiId(), mapping.weightage(), actor));
        recordSetup("EMPLOYEE_MAPPED", "KRA/KPI", employeeId, before.employeeName(), mappingNames(before),
                kras.stream().map(EmployeeKraMapping::kraId).toList() + ";" + kpis.stream().map(EmployeeKpiMapping::kpiId).toList());
        return loadEmployeeMapping(employeeId);
    }

    public Map<String, Object> alignment() {
        requireManage();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", jdbc.query("SELECT k.kra_id, k.kra_name, kp.kpi_id, kp.kpi_name, COUNT(DISTINCT ek.employee_id)"
                        + " FROM pms_kra k LEFT JOIN pms_kpi kp ON kp.kra_id = k.kra_id"
                        + " LEFT JOIN pms_employee_kpi ek ON ek.kpi_id = kp.kpi_id"
                        + " GROUP BY k.kra_id, k.kra_name, kp.kpi_id, kp.kpi_name ORDER BY k.kra_name, kp.kpi_name",
            (rs, row) -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("kraId", rs.getLong(1)); item.put("kra", rs.getString(2));
                item.put("kpiId", nullableLong(rs, 3)); item.put("kpi", Objects.toString(rs.getString(4), "Not mapped"));
                item.put("employees", rs.getLong(5)); return item;
            }));
        result.put("summary", alignmentSummary());
        return result;
    }

    public List<SetupHistoryRow> setupHistory(int requestedLimit) {
        requireManage();
        int limit = Math.max(1, Math.min(requestedLimit, 200));
        return jdbc.query("SELECT history_id, action_type, module_name, item_id, item_title, old_value, new_value,"
                        + " changed_by, changed_at FROM pms_setup_history ORDER BY changed_at DESC, history_id DESC"
                        + " FETCH FIRST " + limit + " ROWS ONLY",
                (rs, row) -> new SetupHistoryRow(rs.getLong(1), rs.getString(2), rs.getString(3),
                        nullableLong(rs, 4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                        rs.getTimestamp(9).toLocalDateTime()));
    }

    public Map<String, Object> setupOverview(LocalDate startDate, LocalDate endDate, Long departmentId) {
        requireManage();
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(180) : startDate;
        if (end.isBefore(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must follow start date");
        List<KraRow> kras = listKras(null, departmentId, null, null, null, "kraName", "asc");
        List<KpiRow> kpis = listKpis(null, departmentId, null, null, null, "kpiName", "asc");
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalKras", kras.size());
        summary.put("totalKpis", kpis.size());
        summary.put("departments", count("SELECT COUNT(*) FROM department"));
        summary.put("activeKraSets", kras.stream().filter(row -> "ACTIVE".equals(row.status())).count());
        summary.put("activeKpiSets", kpis.stream().filter(row -> "ACTIVE".equals(row.status())).count());
        summary.put("employeesMapped", count("SELECT COUNT(DISTINCT employee_id) FROM pms_employee_kra"));
        data.put("summary", summary);
        data.put("kraDistribution", kraDistribution(departmentId));
        data.put("kpiStatus", statusCounts(kpis));
        data.put("trend", setupTrend(start, end, departmentId));
        data.put("categories", categoryCounts(kras));
        data.put("recent", recentSetup(departmentId));
        data.put("kpiByStatus", statusCounts(kpis));
        data.put("alignment", alignmentSummary());
        data.put("ownership", ownershipCounts(kras));
        data.put("departments", jdbc.query("SELECT department_id, department_name FROM department ORDER BY department_name",
                (rs, row) -> Map.of("departmentId", rs.getLong(1), "departmentName", rs.getString(2))));
        return data;
    }

    public String setupCsv() {
        requireManage();
        StringBuilder csv = new StringBuilder("Type,ID,Title,Category,Department,Owner,Status,Weightage,Created Date\r\n");
        for (KraRow row : listKras(null, null, null, null, null, "kraName", "asc")) {
            appendCsv(csv, "KRA", row.kraId(), row.kraName(), row.categoryName(), row.department(), row.owner(),
                    row.status(), row.weightage(), row.createdDate());
        }
        for (KpiRow row : listKpis(null, null, null, null, null, "kpiName", "asc")) {
            appendCsv(csv, "KPI", row.kpiId(), row.kpiName(), row.kraName(), row.department(), row.owner(),
                    row.status(), row.weightage(), row.createdDate());
        }
        return csv.toString();
    }

    public Map<String, Object> appraisalOverview(Long departmentId) {
        if (!access.canManage()) {
            long employeeId = access.requireEmployeeId();
            Long ownDepartment = jdbc.queryForObject("SELECT department_id FROM employee WHERE employee_id = ?", Long.class, employeeId);
            if (departmentId != null && !departmentId.equals(ownDepartment)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your department's appraisal overview.");
            }
            departmentId = ownDepartment;
        }
        String departmentFilter = departmentId == null ? "" : " AND e.department_id = ?";
        List<Object> args = departmentId == null ? new ArrayList<>() : new ArrayList<>(List.of(departmentId));
        String eligible = "e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')";
        long total = queryCount("SELECT COUNT(*) FROM employee e WHERE " + eligible + departmentFilter, args);
        long submitted = queryCount("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + " WHERE a.self_status = 'SUBMITTED' AND " + eligible + departmentFilter, args);
        long drafts = queryCount("SELECT COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                + " WHERE a.self_status = 'DRAFT' AND " + eligible + departmentFilter, args);
        long started = submitted + drafts;
        long pending = Math.max(0, total - submitted);
        Long avg = jdbc.queryForObject("SELECT ROUND(AVG(a.self_rating) * 100) FROM pms_appraisal a"
                + " JOIN employee e ON e.employee_id = a.employee_id WHERE a.self_rating IS NOT NULL AND "
                + eligible + departmentFilter, Long.class, args.toArray());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", Map.of("totalEmployees", total, "submitted", submitted, "pending", pending,
                "notStarted", Math.max(0, total - started), "submissionRate", total == 0 ? 0 : round1(submitted * 100.0 / total),
                "averageSelfRating", avg == null ? 0 : avg / 100.0));
        response.put("ratingDistribution", ratingBuckets(departmentFilter, args));
        response.put("departments", appraisalDepartments(departmentId));
        response.put("recent", recentSubmissions(departmentFilter, args));
        response.put("timeline", activeTimelines(departmentId));
        response.put("cycles", activeCycles(departmentId));
        response.put("insights", appraisalInsights(total, submitted, pending, departmentId));
        response.put("myAppraisal", access.employeeIdOrNull() == null ? null : myAppraisal());
        return response;
    }

    @Transactional
    public Map<String, Object> myAppraisal() {
        long employeeId = access.requireEmployeeId();
        Map<String, Object> employee = jdbc.query("SELECT e.employee_id, e.employee_code, e.display_name,"
                        + " e.department_id, d.department_name, ds.designation_name, e.reporting_manager_id, m.display_name"
                        + " FROM employee e LEFT JOIN department d ON d.department_id = e.department_id"
                        + " LEFT JOIN designation ds ON ds.designation_id = e.designation_id"
                        + " LEFT JOIN employee m ON m.employee_id = e.reporting_manager_id WHERE e.employee_id = ?",
                rs -> {
                    if (!rs.next()) throw new ResourceNotFoundException("Employee not found");
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("employeeId", rs.getLong(1)); row.put("employeeCode", rs.getString(2));
                    row.put("employeeName", rs.getString(3)); row.put("departmentId", nullableLong(rs, 4));
                    row.put("department", rs.getString(5)); row.put("designation", rs.getString(6));
                    row.put("managerId", nullableLong(rs, 7)); row.put("manager", rs.getString(8));
                    return row;
                }, employeeId);
        Long appraisalId = findMyAppraisalId(employeeId);
        if (appraisalId == null) {
            Long cycleId = matchingCycleId(employeeId);
            if (cycleId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active appraisal cycle is configured for this department");
            appraisalId = nextId("pms_appraisal_seq");
            jdbc.update("INSERT INTO pms_appraisal (appraisal_id, cycle_id, employee_id, stage, self_status, created_by, updated_by)"
                    + " VALUES (?, ?, ?, 'YET_TO_START', 'NOT_STARTED', ?, ?)", appraisalId, cycleId, employeeId,
                    access.username(), access.username());
        }
        long id = appraisalId;
        Map<String, Object> result = new LinkedHashMap<>(employee);
        result.put("appraisalId", id);
        result.put("status", jdbc.queryForObject("SELECT self_status FROM pms_appraisal WHERE appraisal_id = ?", String.class, id));
        result.put("selfRating", jdbc.queryForObject("SELECT self_rating FROM pms_appraisal WHERE appraisal_id = ?", BigDecimal.class, id));
        result.put("submittedAt", jdbc.query("SELECT self_submitted_at FROM pms_appraisal WHERE appraisal_id = ?", rs -> {
            if (!rs.next()) return null;
            Timestamp submitted = rs.getTimestamp(1);
            return submitted == null ? null : submitted.toLocalDateTime().toString();
        }, id));
        String json = jdbc.queryForObject("SELECT self_data FROM pms_appraisal WHERE appraisal_id = ?", String.class, id);
        result.put("data", readJson(json));
        result.put("goals", jdbc.query("SELECT g.goal_id, g.title, g.kra_id, k.kra_name, g.kpi_id, kp.kpi_name,"
                        + " g.target_value, g.unit, g.weightage, g.progress_percent FROM pms_goal g"
                        + " LEFT JOIN pms_kra k ON k.kra_id = g.kra_id LEFT JOIN pms_kpi kp ON kp.kpi_id = g.kpi_id"
                        + " WHERE g.employee_id = ? AND g.is_archived = 0 ORDER BY g.due_date, g.goal_id",
            (rs, row) -> {
                Map<String, Object> goal = new LinkedHashMap<>();
                goal.put("goalId", rs.getLong(1)); goal.put("title", rs.getString(2));
                goal.put("kraId", nullableLong(rs, 3)); goal.put("kra", Objects.toString(rs.getString(4), ""));
                goal.put("kpiId", nullableLong(rs, 5)); goal.put("kpi", Objects.toString(rs.getString(6), ""));
                goal.put("target", rs.getBigDecimal(7)); goal.put("unit", Objects.toString(rs.getString(8), ""));
                goal.put("weightage", rs.getBigDecimal(9)); goal.put("progress", rs.getBigDecimal(10));
                return goal;
            }, employeeId));
        result.put("timeline", timelineForEmployee(employeeId));
        result.put("comments", appraisalComments(id));
        return result;
    }

    @Transactional
    public Map<String, Object> saveMyAppraisal(Map<String, Object> payload) {
        long employeeId = access.requireEmployeeId();
        Map<String, Object> current = myAppraisal();
        long appraisalId = ((Number) current.get("appraisalId")).longValue();
        if ("SUBMITTED".equals(current.get("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Submitted appraisals are locked.");
        }
        BigDecimal rating = decimal(payload.getOrDefault("overallRating", payload.get("selfRating")));
        if (rating != null && (rating.compareTo(BigDecimal.ONE) < 0 || rating.compareTo(BigDecimal.valueOf(5)) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Self rating must be between 1 and 5.");
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appraisal data is not valid JSON.");
        }
        jdbc.update("UPDATE pms_appraisal SET self_status = 'DRAFT', self_rating = ?, self_data = ?,"
                + " self_updated_at = SYSTIMESTAMP, updated_at = SYSTIMESTAMP, updated_by = ? WHERE appraisal_id = ? AND employee_id = ?",
                rating, json, access.username(), appraisalId, employeeId);
        return myAppraisal();
    }

    @Transactional
    public Map<String, Object> submitMyAppraisal() {
        Map<String, Object> current = myAppraisal();
        long id = ((Number) current.get("appraisalId")).longValue();
        if ("SUBMITTED".equals(current.get("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This appraisal has already been submitted.");
        }
        if (current.get("selfRating") == null || current.get("data") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Save a draft with an overall self rating before submitting.");
        }
        jdbc.update("UPDATE pms_appraisal SET self_status = 'SUBMITTED', stage = 'SELF_APPRAISAL',"
                + " self_submitted_at = SYSTIMESTAMP, updated_at = SYSTIMESTAMP, updated_by = ? WHERE appraisal_id = ?",
                access.username(), id);
        recordSetup("APPRAISAL_SUBMITTED", "APPRAISAL", id, Objects.toString(current.get("employeeName"), "Employee"),
                "DRAFT", "SUBMITTED");
        return myAppraisal();
    }

    @Transactional
    public Map<String, Object> withdrawMyAppraisal() {
        Map<String, Object> current = myAppraisal();
        long id = ((Number) current.get("appraisalId")).longValue();
        if (!"SUBMITTED".equals(current.get("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only submitted appraisals can be withdrawn.");
        }
        if (!withinSelfAppraisalPeriod(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Withdrawal is only allowed during the self-appraisal period.");
        }
        jdbc.update("UPDATE pms_appraisal SET self_status = 'DRAFT', stage = 'SELF_APPRAISAL', self_submitted_at = NULL,"
                + " updated_at = SYSTIMESTAMP, updated_by = ? WHERE appraisal_id = ?", access.username(), id);
        recordSetup("APPRAISAL_WITHDRAWN", "APPRAISAL", id, Objects.toString(current.get("employeeName"), "Employee"),
                "SUBMITTED", "DRAFT");
        return myAppraisal();
    }

    public List<CommentRow> appraisalComments(long appraisalId) {
        requireAppraisalAccess(appraisalId);
        return jdbc.query("SELECT comment_id, appraisal_id, author_name, author_role, comment_text, created_at"
                        + " FROM pms_appraisal_comment WHERE appraisal_id = ? ORDER BY created_at, comment_id",
                (rs, row) -> new CommentRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getTimestamp(6).toLocalDateTime()), appraisalId);
    }

    @Transactional
    public CommentRow addAppraisalComment(long appraisalId, String text) {
        requireAppraisalAccess(appraisalId);
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty() || trimmed.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must contain 1 to 2000 characters.");
        }
        String role = commentRole(appraisalId);
        long id = nextId("pms_appraisal_comment_seq");
        String author = access.username();
        jdbc.update("INSERT INTO pms_appraisal_comment (comment_id, appraisal_id, author_name, author_role, comment_text)"
                + " VALUES (?, ?, ?, ?, ?)", id, appraisalId, author, role, trimmed);
        return jdbc.queryForObject("SELECT comment_id, appraisal_id, author_name, author_role, comment_text, created_at"
                        + " FROM pms_appraisal_comment WHERE comment_id = ?",
                (rs, row) -> new CommentRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getTimestamp(6).toLocalDateTime()), id);
    }

    @Transactional
    public List<TimelineStage> updateTimeline(long cycleId, List<TimelineStage> stages) {
        requireManage();
        ensureExists("SELECT COUNT(*) FROM pms_appraisal_cycle WHERE cycle_id = ?", cycleId, "Appraisal cycle");
        validateTimeline(stages);
        jdbc.update("DELETE FROM pms_appraisal_timeline WHERE cycle_id = ?", cycleId);
        for (TimelineStage stage : stages) {
            jdbc.update("INSERT INTO pms_appraisal_timeline (cycle_id, stage_code, start_date, end_date, updated_by)"
                    + " VALUES (?, ?, ?, ?, ?)", cycleId, stage.stageCode(), Date.valueOf(stage.startDate()),
                    Date.valueOf(stage.endDate()), access.username());
        }
        recordSetup("TIMELINE_UPDATED", "APPRAISAL", cycleId, "Appraisal timeline", null, stages.toString());
        return timeline(cycleId);
    }

    public String appraisalCsv(Long departmentId) {
        requireManage();
        String filter = departmentId == null ? "" : " AND e.department_id = ?";
        StringBuilder csv = new StringBuilder("Employee ID,Employee,Department,Designation,Status,Self Rating,Submitted At,Cycle\r\n");
        List<Object> args = departmentId == null ? List.of() : List.of(departmentId);
        jdbc.query("SELECT e.employee_code, e.display_name, d.department_name, ds.designation_name, a.self_status,"
                        + " a.self_rating, a.self_submitted_at, c.cycle_name FROM pms_appraisal a"
                        + " JOIN employee e ON e.employee_id = a.employee_id JOIN pms_appraisal_cycle c ON c.cycle_id = a.cycle_id"
                        + " LEFT JOIN department d ON d.department_id = e.department_id"
                        + " LEFT JOIN designation ds ON ds.designation_id = e.designation_id"
                        + " WHERE e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')" + filter
                        + " ORDER BY d.department_name, e.display_name", (RowCallbackHandler) rs -> {
                    while (rs.next()) appendCsv(csv, rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getBigDecimal(6), rs.getTimestamp(7), rs.getString(8));
                }, args.toArray());
        return csv.toString();
    }

    private Map<String, Object> alignmentSummary() {
        Map<String, Long> counts = new HashMap<>();
        jdbc.query("SELECT CASE"
                + " WHEN EXISTS (SELECT 1 FROM pms_employee_kpi ek JOIN pms_kpi kp ON kp.kpi_id = ek.kpi_id"
                + " JOIN pms_employee_kra er ON er.employee_id = ek.employee_id AND er.kra_id = kp.kra_id"
                + " WHERE ek.employee_id = e.employee_id AND ek.status = 'ACTIVE' AND er.status = 'ACTIVE') THEN 'ALIGNED'"
                + " WHEN EXISTS (SELECT 1 FROM pms_employee_kra er WHERE er.employee_id = e.employee_id AND er.status = 'ACTIVE')"
                + " AND EXISTS (SELECT 1 FROM pms_employee_kpi ek WHERE ek.employee_id = e.employee_id AND ek.status = 'ACTIVE') THEN 'PARTIAL'"
                + " WHEN EXISTS (SELECT 1 FROM pms_employee_kra er WHERE er.employee_id = e.employee_id AND er.status = 'ACTIVE')"
                + " OR EXISTS (SELECT 1 FROM pms_employee_kpi ek WHERE ek.employee_id = e.employee_id AND ek.status = 'ACTIVE') THEN 'NOT_ALIGNED'"
                + " ELSE 'NOT_MAPPED' END AS alignment_status, COUNT(*) FROM employee e"
                + " WHERE e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED') GROUP BY CASE"
                + " WHEN EXISTS (SELECT 1 FROM pms_employee_kpi ek JOIN pms_kpi kp ON kp.kpi_id = ek.kpi_id"
                + " JOIN pms_employee_kra er ON er.employee_id = ek.employee_id AND er.kra_id = kp.kra_id"
                + " WHERE ek.employee_id = e.employee_id AND ek.status = 'ACTIVE' AND er.status = 'ACTIVE') THEN 'ALIGNED'"
                + " WHEN EXISTS (SELECT 1 FROM pms_employee_kra er WHERE er.employee_id = e.employee_id AND er.status = 'ACTIVE')"
                + " AND EXISTS (SELECT 1 FROM pms_employee_kpi ek WHERE ek.employee_id = e.employee_id AND ek.status = 'ACTIVE') THEN 'PARTIAL'"
                + " WHEN EXISTS (SELECT 1 FROM pms_employee_kra er WHERE er.employee_id = e.employee_id AND er.status = 'ACTIVE')"
                + " OR EXISTS (SELECT 1 FROM pms_employee_kpi ek WHERE ek.employee_id = e.employee_id AND ek.status = 'ACTIVE') THEN 'NOT_ALIGNED'"
                + " ELSE 'NOT_MAPPED' END", (RowCallbackHandler) rs -> counts.put(rs.getString(1), rs.getLong(2)));
        long aligned = counts.getOrDefault("ALIGNED", 0L);
        long partially = counts.getOrDefault("PARTIAL", 0L);
        long notAligned = counts.getOrDefault("NOT_ALIGNED", 0L);
        long notMapped = counts.getOrDefault("NOT_MAPPED", 0L);
        long mapped = aligned + partially + notAligned;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aligned", aligned); result.put("partiallyAligned", partially);
        result.put("notAligned", notAligned); result.put("notMapped", notMapped);
        result.put("alignmentScore", mapped == 0 ? 0 : round1(aligned * 100.0 / mapped));
        return result;
    }

    private List<Map<String, Object>> kraDistribution(Long departmentId) {
        String condition = departmentId == null ? "" : " WHERE m.department_id = ?";
        List<Object> args = departmentId == null ? List.of() : List.of(departmentId);
        return jdbc.query("SELECT d.department_name, COUNT(DISTINCT m.kra_id) FROM pms_kra_department m"
                        + " JOIN department d ON d.department_id = m.department_id" + condition
                        + " GROUP BY d.department_name ORDER BY COUNT(DISTINCT m.kra_id) DESC", (rs, row) -> {
                    Map<String, Object> item = new LinkedHashMap<>(); item.put("department", rs.getString(1));
                    item.put("count", rs.getLong(2)); return item;
                }, args.toArray());
    }

    private List<Map<String, Object>> statusCounts(List<KpiRow> kpis) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : List.of("ACTIVE", "UNDER_REVIEW", "DRAFT", "INACTIVE")) {
            counts.put(status, kpis.stream().filter(row -> status.equals(row.status())).count());
        }
        return counts.entrySet().stream().map(entry -> Map.<String, Object>of("status", entry.getKey(), "count", entry.getValue())).toList();
    }

    private List<Map<String, Object>> setupTrend(LocalDate start, LocalDate end, Long departmentId) {
        List<Map<String, Object>> points = new ArrayList<>();
        YearMonth month = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        while (!month.isAfter(last)) {
            LocalDate from = month.atDay(1).isBefore(start) ? start : month.atDay(1);
            LocalDate to = month.atEndOfMonth().isAfter(end) ? end : month.atEndOfMonth();
            List<Object> args = new ArrayList<>(List.of(Date.valueOf(from), Date.valueOf(to)));
            String dept = "";
            if (departmentId != null) {
                dept = " AND department_id = ?";
                args.add(departmentId);
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", month.toString()); point.put("kra", queryCount("SELECT COUNT(*) FROM pms_kra WHERE TRUNC(created_at) BETWEEN ? AND ?" + dept, args));
            point.put("kpi", queryCount("SELECT COUNT(*) FROM pms_kpi WHERE TRUNC(created_at) BETWEEN ? AND ?" + dept, args));
            points.add(point); month = month.plusMonths(1);
        }
        return points;
    }

    private List<Map<String, Object>> categoryCounts(List<KraRow> kras) {
        Map<String, Long> categories = new LinkedHashMap<>();
        kras.forEach(row -> categories.merge(Objects.toString(row.categoryName(), "Uncategorized"), 1L, Long::sum));
        return categories.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> Map.<String, Object>of("category", entry.getKey(), "count", entry.getValue())).toList();
    }

    private List<Map<String, Object>> recentSetup(Long departmentId) {
        String deptKra = departmentId == null ? "" : " AND k.department_id = ?";
        String deptKpi = departmentId == null ? "" : " AND kp.department_id = ?";
        List<Object> args = new ArrayList<>();
        if (departmentId != null) { args.add(departmentId); args.add(departmentId); }
        return jdbc.query("SELECT * FROM (SELECT k.kra_name, 'KRA' AS item_type, d.department_name, k.status,"
                        + " TRUNC(k.created_at) AS setup_date FROM pms_kra k LEFT JOIN department d ON d.department_id = k.department_id"
                        + " WHERE 1 = 1" + deptKra + " UNION ALL SELECT kp.kpi_name, 'KPI', d.department_name, kp.status,"
                        + " TRUNC(kp.created_at) FROM pms_kpi kp LEFT JOIN department d ON d.department_id = kp.department_id"
                        + " WHERE 1 = 1" + deptKpi + ") ORDER BY setup_date DESC FETCH FIRST 10 ROWS ONLY",
                (rs, row) -> Map.of("title", rs.getString(1), "type", rs.getString(2),
                        "department", Objects.toString(rs.getString(3), "Unmapped"), "status", rs.getString(4),
                        "setupDate", rs.getDate(5).toLocalDate().toString()), args.toArray());
    }

    private List<Map<String, Object>> ownershipCounts(List<KraRow> kras) {
        Map<String, Long> owners = new LinkedHashMap<>();
        kras.forEach(row -> owners.merge(row.ownerType(), 1L, Long::sum));
        return owners.entrySet().stream().map(entry -> Map.<String, Object>of("owner", entry.getKey(), "count", entry.getValue())).toList();
    }

    private List<Map<String, Object>> ratingBuckets(String departmentFilter, List<Object> args) {
        Map<Integer, Long> counts = new HashMap<>();
        jdbc.query("SELECT ROUND(a.self_rating), COUNT(*) FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                        + " WHERE a.self_rating IS NOT NULL AND e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')"
                        + departmentFilter + " GROUP BY ROUND(a.self_rating)", (RowCallbackHandler) rs -> {
                    counts.put(rs.getInt(1), rs.getLong(2));
                }, args.toArray());
        List<Map<String, Object>> result = new ArrayList<>();
        for (int rating = 1; rating <= 5; rating++) {
            result.add(Map.of("rating", rating, "count", counts.getOrDefault(rating, 0L)));
        }
        return result;
    }

    private List<Map<String, Object>> appraisalDepartments(Long departmentId) {
        List<Object> args = new ArrayList<>();
        String filter = "";
        if (departmentId != null) { filter = " AND d.department_id = ?"; args.add(departmentId); }
        return jdbc.query("SELECT d.department_id, d.department_name, COUNT(DISTINCT e.employee_id),"
                        + " COUNT(DISTINCT CASE WHEN a.self_status = 'SUBMITTED' THEN e.employee_id END),"
                        + " COUNT(DISTINCT CASE WHEN a.self_status = 'DRAFT' THEN e.employee_id END),"
                        + " NVL(AVG(a.self_rating), 0) FROM department d JOIN employee e ON e.department_id = d.department_id"
                        + " LEFT JOIN pms_appraisal a ON a.employee_id = e.employee_id"
                        + " WHERE e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')" + filter
                        + " GROUP BY d.department_id, d.department_name ORDER BY d.department_name",
                (rs, row) -> {
                    long total = rs.getLong(3), sent = rs.getLong(4), pending = Math.max(0, total - sent);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("departmentId", rs.getLong(1)); item.put("department", rs.getString(2));
                    item.put("totalEmployees", total); item.put("submitted", sent); item.put("pending", pending);
                    item.put("submissionRate", total == 0 ? 0 : round1(sent * 100.0 / total));
                    item.put("averageRating", round2(rs.getDouble(6))); return item;
                }, args.toArray());
    }

    private List<Map<String, Object>> recentSubmissions(String departmentFilter, List<Object> args) {
        return jdbc.query("SELECT e.display_name, d.department_name, a.self_submitted_at, a.self_status"
                        + " FROM pms_appraisal a JOIN employee e ON e.employee_id = a.employee_id"
                        + " LEFT JOIN department d ON d.department_id = e.department_id"
                        + " WHERE e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')" + departmentFilter
                        + " ORDER BY CASE WHEN a.self_submitted_at IS NULL THEN 1 ELSE 0 END, a.self_submitted_at DESC, e.display_name"
                        + " FETCH FIRST 12 ROWS ONLY", (rs, row) -> {
                    Map<String, Object> item = new LinkedHashMap<>(); item.put("employeeName", rs.getString(1));
                    item.put("department", rs.getString(2)); Timestamp submitted = rs.getTimestamp(3);
                    item.put("submittedAt", submitted == null ? null : submitted.toLocalDateTime().toString());
                    item.put("status", rs.getString(4)); return item;
                }, args.toArray());
    }

    private List<Map<String, Object>> appraisalInsights(long total, long submitted, long pending, Long departmentId) {
        List<Map<String, Object>> insights = new ArrayList<>();
        insights.add(Map.of("label", "Submission rate", "value", total == 0 ? "0%" : round1(submitted * 100.0 / total) + "%"));
        Long average = jdbc.queryForObject("SELECT ROUND(AVG(self_rating) * 100) FROM pms_appraisal WHERE self_rating IS NOT NULL", Long.class);
        insights.add(Map.of("label", "Average self rating", "value", average == null ? "No ratings yet" : String.format(Locale.ROOT, "%.2f / 5", average / 100.0)));
        Map<String, Object> lowSubmission = appraisalDepartments(departmentId).stream()
                .min(Comparator.comparingDouble(row -> ((Number) row.get("submissionRate")).doubleValue())).orElse(null);
        if (lowSubmission != null) insights.add(Map.of("label", "Lowest submission", "value", lowSubmission.get("department")));
        insights.add(Map.of("label", "Pending employees", "value", pending));
        return insights;
    }

    private List<Map<String, Object>> activeCycles(Long departmentId) {
        List<Object> args = new ArrayList<>();
        String where = " WHERE c.status = 'ACTIVE'";
        if (departmentId != null) { where += " AND EXISTS (SELECT 1 FROM employee e WHERE e.department_id = ? AND COALESCE((SELECT group_name FROM pms_department_group_v v WHERE v.department_id = e.department_id), 'Others') = c.dept_group)"; args.add(departmentId); }
        return jdbc.query("SELECT c.cycle_id, c.cycle_name, c.dept_group, c.start_date, c.due_date FROM pms_appraisal_cycle c"
                        + where + " ORDER BY c.start_date DESC", (rs, row) -> {
                    Map<String, Object> cycle = new LinkedHashMap<>(); cycle.put("cycleId", rs.getLong(1));
                    cycle.put("cycleName", rs.getString(2)); cycle.put("departmentGroup", rs.getString(3));
                    cycle.put("startDate", rs.getDate(4).toLocalDate().toString());
                    cycle.put("endDate", rs.getDate(5).toLocalDate().toString());
                    cycle.put("stages", timeline(rs.getLong(1))); return cycle;
                }, args.toArray());
    }

    private List<Map<String, Object>> activeTimelines(Long departmentId) {
        return activeCycles(departmentId).stream().map(cycle -> {
            Map<String, Object> item = new LinkedHashMap<>(cycle);
            item.put("cycleName", cycle.get("cycleName"));
            return item;
        }).toList();
    }

    private List<TimelineStage> timeline(long cycleId) {
        return jdbc.query("SELECT stage_code, start_date, end_date FROM pms_appraisal_timeline WHERE cycle_id = ?"
                        + " ORDER BY CASE stage_code WHEN 'SELF_APPRAISAL' THEN 1 WHEN 'MANAGER_REVIEW' THEN 2"
                        + " WHEN 'HR_REVIEW' THEN 3 ELSE 4 END", (rs, row) -> new TimelineStage(rs.getString(1),
                        rs.getDate(2).toLocalDate(), rs.getDate(3).toLocalDate()), cycleId);
    }

    private List<Map<String, Object>> timelineForEmployee(long employeeId) {
        List<Long> cycles = jdbc.query("SELECT a.cycle_id FROM pms_appraisal a JOIN pms_appraisal_cycle c"
                + " ON c.cycle_id = a.cycle_id WHERE a.employee_id = ? AND c.status = 'ACTIVE'"
                + " ORDER BY c.due_date DESC FETCH FIRST 1 ROWS ONLY",
            (rs, row) -> rs.getLong(1), employeeId);
        Long cycleId = cycles.isEmpty() ? null : cycles.get(0);
        if (cycleId == null) return List.of();
        return activeCycles(null).stream().filter(cycle -> ((Number) cycle.get("cycleId")).longValue() == cycleId)
                .map(cycle -> (List<Map<String, Object>>) cycle.get("stages")).findFirst().orElse(List.of());
    }

    private Long findMyAppraisalId(long employeeId) {
        List<Long> ids = jdbc.query("SELECT a.appraisal_id FROM pms_appraisal a JOIN pms_appraisal_cycle c ON c.cycle_id = a.cycle_id"
                        + " WHERE a.employee_id = ? AND c.status = 'ACTIVE' ORDER BY c.due_date DESC FETCH FIRST 1 ROWS ONLY",
                (rs, row) -> rs.getLong(1), employeeId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long matchingCycleId(long employeeId) {
        List<Long> ids = jdbc.query("SELECT c.cycle_id FROM employee e JOIN pms_department_group_v dg ON dg.department_id = e.department_id"
                        + " JOIN pms_appraisal_cycle c ON c.dept_group = dg.group_name AND c.status = 'ACTIVE'"
                        + " WHERE e.employee_id = ? ORDER BY c.due_date DESC FETCH FIRST 1 ROWS ONLY",
                (rs, row) -> rs.getLong(1), employeeId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private boolean withinSelfAppraisalPeriod(long appraisalId) {
        Integer valid = jdbc.queryForObject("SELECT COUNT(*) FROM pms_appraisal_timeline t JOIN pms_appraisal a"
                + " ON a.cycle_id = t.cycle_id WHERE a.appraisal_id = ? AND t.stage_code = 'SELF_APPRAISAL'"
                + " AND TRUNC(SYSDATE) BETWEEN t.start_date AND t.end_date", Integer.class, appraisalId);
        return valid != null && valid > 0;
    }

    private String commentRole(long appraisalId) {
        if (access.canManage()) return "HR";
        Long employeeId = access.employeeIdOrNull();
        if (employeeId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee access is required.");
        Long ownerId = jdbc.queryForObject("SELECT employee_id FROM pms_appraisal WHERE appraisal_id = ?", Long.class, appraisalId);
        if (ownerId.equals(employeeId)) return "EMPLOYEE";
        Integer isManager = jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE employee_id = ? AND reporting_manager_id = ?",
                Integer.class, ownerId, employeeId);
        if (isManager != null && isManager > 0) return "MANAGER";
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot comment on this appraisal.");
    }

    private void requireAppraisalAccess(long appraisalId) {
        if (access.canManage()) return;
        commentRole(appraisalId);
    }

    private KraRow findKra(long id) {
        KraRow row = jdbc.query(KRA_SELECT + " WHERE k.kra_id = ?", rs -> rs.next() ? mapKra(rs) : null, id);
        return row == null ? throwNotFound("KRA", id) : row;
    }

    private KpiRow findKpi(long id) {
        KpiRow row = jdbc.query(KPI_SELECT + " WHERE kp.kpi_id = ?", rs -> rs.next() ? mapKpi(rs) : null, id);
        return row == null ? throwNotFound("KPI", id) : row;
    }

    private KraRow mapKra(ResultSet rs) throws SQLException {
        return new KraRow(rs.getLong(1), rs.getString(2), nullableLong(rs, 3), rs.getString(4), rs.getString(5),
                nullableLong(rs, 6), rs.getString(7), rs.getString(8), nullableLong(rs, 9), rs.getString(10),
                rs.getBigDecimal(11), rs.getString(12), localDate(rs, 13), rs.getLong(14), rs.getLong(15), localDate(rs, 16));
    }

    private KpiRow mapKpi(ResultSet rs) throws SQLException {
        return new KpiRow(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getString(5),
                rs.getBigDecimal(6), rs.getString(7), rs.getBigDecimal(8), nullableLong(rs, 9), rs.getString(10),
                rs.getString(11), nullableLong(rs, 12), rs.getString(13), rs.getString(14), rs.getString(15), localDate(rs, 16));
    }

    private EmployeeMapping loadEmployeeMapping(long employeeId) {
        Map<String, Object> employee = jdbc.query("SELECT e.employee_code, e.display_name, d.department_name,"
                        + " ds.designation_name, m.display_name FROM employee e LEFT JOIN department d ON d.department_id = e.department_id"
                        + " LEFT JOIN designation ds ON ds.designation_id = e.designation_id"
                        + " LEFT JOIN employee m ON m.employee_id = e.reporting_manager_id WHERE e.employee_id = ?",
                rs -> {
                    if (!rs.next()) throw new ResourceNotFoundException("Employee not found");
                    Map<String, Object> result = new HashMap<>();
                    result.put("code", rs.getString(1)); result.put("name", rs.getString(2));
                    result.put("department", rs.getString(3)); result.put("designation", rs.getString(4));
                    result.put("manager", rs.getString(5)); return result;
                }, employeeId);
        List<KraRow> kras = jdbc.query(KRA_SELECT + " JOIN pms_employee_kra em ON em.kra_id = k.kra_id"
                        + " WHERE em.employee_id = ? AND em.status = 'ACTIVE' ORDER BY k.kra_name",
                (rs, row) -> mapKra(rs), employeeId);
        List<KpiRow> kpis = jdbc.query(KPI_SELECT + " JOIN pms_employee_kpi em ON em.kpi_id = kp.kpi_id"
                        + " WHERE em.employee_id = ? AND em.status = 'ACTIVE' ORDER BY kp.kpi_name",
                (rs, row) -> mapKpi(rs), employeeId);
        Map<Long, BigDecimal> kraWeights = new HashMap<>();
        jdbc.query("SELECT kra_id, weightage FROM pms_employee_kra WHERE employee_id = ? AND status = 'ACTIVE'",
                (RowCallbackHandler) rs -> kraWeights.put(rs.getLong(1), rs.getBigDecimal(2)), employeeId);
        Map<Long, BigDecimal> kpiWeights = new HashMap<>();
        jdbc.query("SELECT kpi_id, weightage FROM pms_employee_kpi WHERE employee_id = ? AND status = 'ACTIVE'",
                (RowCallbackHandler) rs -> kpiWeights.put(rs.getLong(1), rs.getBigDecimal(2)), employeeId);
        kras = kras.stream().map(row -> new KraRow(row.kraId(), row.kraName(), row.categoryId(), row.categoryName(),
            row.description(), row.departmentId(), row.department(), row.ownerType(), row.ownerEmployeeId(), row.owner(),
            kraWeights.getOrDefault(row.kraId(), row.weightage()), row.status(), row.effectiveDate(), row.kpisLinked(),
            row.mappedEmployees(), row.createdDate())).toList();
        kpis = kpis.stream().map(row -> new KpiRow(row.kpiId(), row.kpiName(), row.kraId(), row.kraName(),
            row.measurementType(), row.targetValue(), row.unit(), kpiWeights.getOrDefault(row.kpiId(), row.weightage()),
            row.departmentId(), row.department(), row.ownerType(), row.ownerEmployeeId(), row.owner(), row.frequency(),
            row.status(), row.createdDate())).toList();
        return new EmployeeMapping(employeeId, (String) employee.get("code"), (String) employee.get("name"),
                (String) employee.get("department"), (String) employee.get("designation"), (String) employee.get("manager"),
                kras, kpis);
    }

    private void replaceDepartmentKraMappings(long kraId, Long departmentId) {
        jdbc.update("DELETE FROM pms_kra_department WHERE kra_id = ?", kraId);
        if (departmentId != null) jdbc.update("INSERT INTO pms_kra_department (kra_id, department_id, mapped_by) VALUES (?, ?, ?)",
                kraId, departmentId, access.username());
    }

    private void replaceDepartmentKpiMappings(long kpiId, Long departmentId) {
        jdbc.update("DELETE FROM pms_kpi_department WHERE kpi_id = ?", kpiId);
        if (departmentId != null) jdbc.update("INSERT INTO pms_kpi_department (kpi_id, department_id, mapped_by) VALUES (?, ?, ?)",
                kpiId, departmentId, access.username());
    }

    private Long kraDepartment(long kraId) {
        List<Long> ids = jdbc.query("SELECT department_id FROM pms_kra WHERE kra_id = ?", (rs, row) -> nullableLong(rs, 1), kraId);
        if (ids.isEmpty()) throw new ResourceNotFoundException("KRA not found");
        return ids.get(0);
    }

    private void validateKra(KraRequest request) {
        requireValue(request.status(), KRA_STATUSES, "KRA status");
        requireValue(request.ownerType(), KRA_OWNERS, "owner type");
        if ("EMPLOYEE".equals(request.ownerType()) && request.ownerEmployeeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an employee owner.");
        }
    }

    private void validateKpi(KpiRequest request) {
        requireValue(request.status(), KRA_STATUSES, "KPI status");
        requireValue(request.ownerType(), KRA_OWNERS, "owner type");
        requireValue(request.measurementType(), MEASUREMENTS, "measurement type");
        requireValue(request.frequency(), FREQUENCIES, "frequency");
        ensureExists("SELECT COUNT(*) FROM pms_kra WHERE kra_id = ?", request.kraId(), "KRA");
        if ("EMPLOYEE".equals(request.ownerType()) && request.ownerEmployeeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an employee owner.");
        }
    }

    private void validateMapping(long id, BigDecimal weightage, String type) {
        String table = "KRA".equals(type) ? "pms_kra" : "pms_kpi";
        String key = "KRA".equals(type) ? "kra_id" : "kpi_id";
        ensureExists("SELECT COUNT(*) FROM " + table + " WHERE " + key + " = ?", id, type);
        if (weightage == null || weightage.compareTo(BigDecimal.ZERO) < 0 || weightage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mapping weightage must be between 0 and 100.");
        }
    }

    private void validateTimeline(List<TimelineStage> stages) {
        if (stages == null || stages.size() != TIMELINE_STAGES.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configure all four appraisal stages.");
        }
        List<TimelineStage> ordered = stages.stream().sorted(Comparator.comparing(stage -> TIMELINE_STAGES.indexOf(stage.stageCode()))).toList();
        LocalDate previousEnd = null;
        for (int index = 0; index < ordered.size(); index++) {
            TimelineStage stage = ordered.get(index);
            if (!TIMELINE_STAGES.get(index).equals(stage.stageCode()) || stage.startDate() == null || stage.endDate() == null
                    || stage.endDate().isBefore(stage.startDate()) || (previousEnd != null && !stage.startDate().isAfter(previousEnd))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timeline stages must be valid, ordered and non-overlapping.");
            }
            previousEnd = stage.endDate();
        }
    }

    private void addSearch(StringBuilder where, List<Object> args, String search, String expressions) {
        if (search == null || search.isBlank()) return;
        String[] parts = expressions.split(" OR ");
        where.append(" AND (");
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) where.append(" OR ");
            where.append(parts[index]); args.add("%" + search.trim().toLowerCase(Locale.ROOT) + "%");
        }
        where.append(')');
    }

    private void addOptional(StringBuilder where, List<Object> args, Long value, String expression) {
        if (value != null) { where.append(" AND ").append(expression); args.add(value); }
    }

    private void addTextFilter(StringBuilder where, List<Object> args, String value, String expression) {
        if (value != null && !value.isBlank()) { where.append(" AND ").append(expression); args.add(value.trim().toUpperCase(Locale.ROOT)); }
    }

    private String kraSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "status" -> "k.status"; case "department" -> "d.department_name";
            case "createdDate" -> "k.created_at"; case "category" -> "c.category_name"; default -> "k.kra_name";
        };
    }

    private String kpiSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "status" -> "kp.status"; case "department" -> "d.department_name";
            case "createdDate" -> "kp.created_at"; case "kra" -> "k.kra_name"; default -> "kp.kpi_name";
        };
    }

    private String sortDirection(String direction) { return "desc".equalsIgnoreCase(direction) ? " DESC" : " ASC"; }

    private void recordSetup(String action, String module, long itemId, String title, String oldValue, String newValue) {
        jdbc.update("INSERT INTO pms_setup_history (history_id, action_type, module_name, item_id, item_title,"
                        + " old_value, new_value, changed_by) VALUES (pms_setup_history_seq.NEXTVAL, ?, ?, ?, ?, ?, ?, ?)",
                action, module, itemId, title, oldValue, newValue, access.username());
    }

    private void requireManage() {
        if (!access.canManage()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "HR performance-management access is required.");
    }

    private long nextId(String sequence) {
        if (!Set.of("pms_kra_seq", "pms_kpi_seq", "pms_appraisal_seq", "pms_appraisal_comment_seq").contains(sequence)) {
            throw new IllegalArgumentException("Unsupported sequence");
        }
        Long id = jdbc.queryForObject("SELECT " + sequence + ".NEXTVAL FROM dual", Long.class);
        return id == null ? 0 : id;
    }

    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private long queryCount(String sql, List<?> args) {
        Long value = jdbc.queryForObject(sql, Long.class, args.toArray());
        return value == null ? 0 : value;
    }

    private void ensureExists(String sql, Object id, String name) {
        if (count(sql, id) == 0) throw new ResourceNotFoundException(name + " not found");
    }

    private void requireValue(String value, Set<String> values, String label) {
        if (value == null || !values.contains(value.trim().toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported " + label + ".");
        }
    }

    private String uniqueName(String base, String type) {
        String suffix = " Copy " + System.currentTimeMillis();
        return base.substring(0, Math.min(base.length(), 160 - suffix.length())).trim() + suffix;
    }

    private String mappingNames(EmployeeMapping mapping) {
        return mapping.kras().stream().map(KraRow::kraName).toList() + ";" + mapping.kpis().stream().map(KpiRow::kpiName).toList();
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private static BigDecimal decimal(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try { return new BigDecimal(value.toString()); }
        catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Self rating must be numeric."); }
    }

    private static LocalDate localDate(ResultSet rs, int column) throws SQLException {
        Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private static Long nullableLong(ResultSet rs, int column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).longValue();
    }

    private static Long nullableLong(ResultSet rs, int column, boolean ignored) throws SQLException { return nullableLong(rs, column); }

    private static Long nullableLong(Map<String, Object> value, String key) {
        Object item = value.get(key); return item == null ? null : ((Number) item).longValue();
    }

    private static double round1(double value) { return Math.round(value * 10.0) / 10.0; }
    private static double round2(double value) { return Math.round(value * 100.0) / 100.0; }

    private static void appendCsv(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) csv.append(',');
            String value = Objects.toString(values[index], "");
            if (value.startsWith("=") || value.startsWith("+") || value.startsWith("-") || value.startsWith("@")) value = "'" + value;
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private static Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return new ObjectMapper().readValue(json, Map.class); }
        catch (JsonProcessingException ex) { return Map.of(); }
    }

    private <T> T throwNotFound(String type, long id) {
        throw new ResourceNotFoundException(type + " " + id + " was not found");
    }
}