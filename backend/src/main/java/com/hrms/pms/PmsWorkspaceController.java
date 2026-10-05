package com.hrms.pms;

import com.hrms.pms.PmsWorkspaceDtos.CommentRequest;
import com.hrms.pms.PmsWorkspaceDtos.CommentRow;
import com.hrms.pms.PmsWorkspaceDtos.DepartmentMapping;
import com.hrms.pms.PmsWorkspaceDtos.EmployeeMapping;
import com.hrms.pms.PmsWorkspaceDtos.EmployeeMappingRequest;
import com.hrms.pms.PmsWorkspaceDtos.IdMappingRequest;
import com.hrms.pms.PmsWorkspaceDtos.KpiRequest;
import com.hrms.pms.PmsWorkspaceDtos.KpiRow;
import com.hrms.pms.PmsWorkspaceDtos.KraRequest;
import com.hrms.pms.PmsWorkspaceDtos.KraRow;
import com.hrms.pms.PmsWorkspaceDtos.SetupHistoryRow;
import com.hrms.pms.PmsWorkspaceDtos.TimelineStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pms")
@Tag(name = "PMS Workspace")
public class PmsWorkspaceController {

    private final PmsWorkspaceService workspace;

    public PmsWorkspaceController(PmsWorkspaceService workspace) {
        this.workspace = workspace;
    }

    @GetMapping("/setup/overview")
    @Operation(summary = "KRA/KPI setup metrics, charts, recent activity and alignment")
    public Map<String, Object> setupOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId) {
        return workspace.setupOverview(startDate, endDate, departmentId);
    }

    @GetMapping("/setup/kras")
    public List<KraRow> kras(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String owner,
            @RequestParam(defaultValue = "kraName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return workspace.listKras(search, departmentId, categoryId, status, owner, sort, direction);
    }

    @PostMapping("/setup/kras")
    public ResponseEntity<KraRow> createKra(@Valid @RequestBody KraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspace.createKra(request));
    }

    @PutMapping("/setup/kras/{id}")
    public KraRow updateKra(@PathVariable long id, @Valid @RequestBody KraRequest request) {
        return workspace.updateKra(id, request);
    }

    @DeleteMapping("/setup/kras/{id}")
    public ResponseEntity<Void> deleteKra(@PathVariable long id) {
        workspace.deleteKra(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/setup/kras/{id}/duplicate")
    public ResponseEntity<KraRow> duplicateKra(@PathVariable long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspace.duplicateKra(id));
    }

    @GetMapping("/setup/kpis")
    public List<KpiRow> kpis(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long kraId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String owner,
            @RequestParam(defaultValue = "kpiName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return workspace.listKpis(search, departmentId, kraId, status, owner, sort, direction);
    }

    @PostMapping("/setup/kpis")
    public ResponseEntity<KpiRow> createKpi(@Valid @RequestBody KpiRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspace.createKpi(request));
    }

    @PutMapping("/setup/kpis/{id}")
    public KpiRow updateKpi(@PathVariable long id, @Valid @RequestBody KpiRequest request) {
        return workspace.updateKpi(id, request);
    }

    @DeleteMapping("/setup/kpis/{id}")
    public ResponseEntity<Void> deleteKpi(@PathVariable long id) {
        workspace.deleteKpi(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/setup/kpis/{id}/duplicate")
    public ResponseEntity<KpiRow> duplicateKpi(@PathVariable long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspace.duplicateKpi(id));
    }

    @GetMapping("/setup/departments/{departmentId}/mappings")
    public DepartmentMapping departmentMappings(@PathVariable long departmentId) {
        return workspace.departmentMappings(departmentId);
    }

    @PutMapping("/setup/departments/{departmentId}/mappings")
    public DepartmentMapping saveDepartmentMappings(@PathVariable long departmentId,
                                                     @RequestBody IdMappingRequest request) {
        return workspace.saveDepartmentMappings(departmentId, request);
    }

    @GetMapping("/setup/employees/{employeeId}/mappings")
    public EmployeeMapping employeeMappings(@PathVariable long employeeId) {
        return workspace.employeeMappings(employeeId);
    }

    @PutMapping("/setup/employees/{employeeId}/mappings")
    public EmployeeMapping saveEmployeeMappings(@PathVariable long employeeId,
                                               @RequestBody EmployeeMappingRequest request) {
        return workspace.saveEmployeeMappings(employeeId, request);
    }

    @GetMapping("/setup/alignment")
    public Map<String, Object> alignment() {
        return workspace.alignment();
    }

    @GetMapping("/setup/history")
    public List<SetupHistoryRow> setupHistory(@RequestParam(defaultValue = "50") int limit) {
        return workspace.setupHistory(limit);
    }

    @GetMapping("/setup/export")
    @Operation(summary = "Download the current KRA/KPI setup data as CSV")
    public ResponseEntity<byte[]> exportSetup() {
        return PmsDashboardController.csvResponse(workspace.setupCsv(), "kra-kpi-setup-" + LocalDate.now() + ".csv");
    }

    @GetMapping("/appraisals/overview")
    public Map<String, Object> appraisalOverview(@RequestParam(required = false) Long departmentId) {
        return workspace.appraisalOverview(departmentId);
    }

    @GetMapping("/appraisals/mine")
    public Map<String, Object> myAppraisal() {
        return workspace.myAppraisal();
    }

    @PutMapping("/appraisals/mine")
    public Map<String, Object> saveMyAppraisal(@RequestBody Map<String, Object> payload) {
        return workspace.saveMyAppraisal(payload);
    }

    @PostMapping("/appraisals/mine/submit")
    public Map<String, Object> submitMyAppraisal() {
        return workspace.submitMyAppraisal();
    }

    @PostMapping("/appraisals/mine/withdraw")
    public Map<String, Object> withdrawMyAppraisal() {
        return workspace.withdrawMyAppraisal();
    }

    @GetMapping("/appraisals/{appraisalId}/comments")
    public List<CommentRow> appraisalComments(@PathVariable long appraisalId) {
        return workspace.appraisalComments(appraisalId);
    }

    @PostMapping("/appraisals/{appraisalId}/comments")
    public ResponseEntity<CommentRow> addAppraisalComment(@PathVariable long appraisalId,
                                                          @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspace.addAppraisalComment(appraisalId, request.commentText()));
    }

    @PutMapping("/appraisals/cycles/{cycleId}/timeline")
    public List<TimelineStage> updateTimeline(@PathVariable long cycleId,
                                             @RequestBody List<TimelineStage> stages) {
        return workspace.updateTimeline(cycleId, stages);
    }

    @GetMapping("/appraisals/export")
    public ResponseEntity<byte[]> exportAppraisals(@RequestParam(required = false) Long departmentId) {
        return PmsDashboardController.csvResponse(workspace.appraisalCsv(departmentId),
                "self-appraisal-report-" + LocalDate.now() + ".csv");
    }
}