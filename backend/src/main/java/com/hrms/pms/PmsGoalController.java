package com.hrms.pms;

import com.hrms.pms.PmsDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pms")
@Tag(name = "PMS Goals")
public class PmsGoalController {

    private final PmsGoalService goals;
    private final PmsAnalyticsService analytics;
    private final PmsAccess access;

    public PmsGoalController(PmsGoalService goals, PmsAnalyticsService analytics, PmsAccess access) {
        this.goals = goals;
        this.analytics = analytics;
        this.access = access;
    }

    private static PmsGoalService.GoalFilter filter(String search, String status, String department, String goalType,
                                                    String priority, Long categoryId, LocalDate dueFrom, LocalDate dueTo) {
        return new PmsGoalService.GoalFilter(search, status, department, goalType, priority, categoryId, dueFrom, dueTo);
    }

    @GetMapping("/goals")
    @Operation(summary = "Paged goal list. scope: ALL, MY, TEAM or ARCHIVED")
    public GoalPage list(
            @RequestParam(defaultValue = "ALL") String scope,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String goalType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return goals.list(scope, filter(search, status, department, goalType, priority, categoryId, dueFrom, dueTo),
                page, size, sort, direction);
    }

    @GetMapping("/goals/overview")
    @Operation(summary = "Goal Management overview: KPIs, trend, alignment, categories, departments, my goals, deadlines")
    public GoalOverview overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department) {
        Long me = access.employeeIdOrNull();
        return analytics.goalOverview(startDate, endDate, department, goals.myGoals(me, 4), me != null);
    }

    @GetMapping("/goals/categories-summary")
    @Operation(summary = "Goal counts by category, optionally for one department group")
    public List<CategoryCount> categoriesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department) {
        Period period = analytics.period(null, endDate, department);
        return analytics.categories(period.endDate(), period.department());
    }

    @GetMapping("/goals/alignment")
    @Operation(summary = "Company > Department > Team > Individual goal alignment tree")
    public List<AlignmentNode> alignment() {
        return goals.alignmentTree();
    }

    @GetMapping("/goals/calendar")
    @Operation(summary = "Goal start and due-date events for a date range of up to 62 days")
    public List<CalendarEvent> calendar(
            @RequestParam(defaultValue = "ALL") String scope,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String department) {
        return goals.calendar(scope, from, to, department);
    }

    @GetMapping("/goals/export")
    @Operation(summary = "Download goals as CSV using the same filters as the list")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "ALL") String scope,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String goalType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo) {
        String csv = goals.exportCsv(scope, filter(search, status, department, goalType, priority, categoryId, dueFrom, dueTo));
        return PmsDashboardController.csvResponse(csv, "goals-" + LocalDate.now() + ".csv");
    }

    @GetMapping("/goals/{goalId}")
    public GoalRow get(@PathVariable long goalId) {
        return goals.get(goalId);
    }

    @GetMapping("/goals/{goalId}/history")
    public List<GoalHistoryRow> history(@PathVariable long goalId) {
        return goals.history(goalId);
    }

    @PostMapping("/goals")
    @Operation(summary = "Create a goal")
    public ResponseEntity<GoalRow> create(@Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goals.create(request));
    }

    @PutMapping("/goals/{goalId}")
    @Operation(summary = "Update a goal")
    public GoalRow update(@PathVariable long goalId, @Valid @RequestBody GoalRequest request) {
        return goals.update(goalId, request);
    }

    @PatchMapping("/goals/{goalId}/progress")
    @Operation(summary = "Update goal progress; goal owners and managers may call this")
    public GoalRow progress(@PathVariable long goalId, @Valid @RequestBody ProgressRequest request) {
        return goals.updateProgress(goalId, request);
    }

    @PostMapping("/goals/{goalId}/archive")
    public GoalRow archive(@PathVariable long goalId, @Valid @RequestBody(required = false) ArchiveRequest request) {
        return goals.archive(goalId, request == null ? null : request.reason());
    }

    @PostMapping("/goals/{goalId}/restore")
    public GoalRow restore(@PathVariable long goalId) {
        return goals.restore(goalId);
    }

    @DeleteMapping("/goals/{goalId}")
    public ResponseEntity<Void> delete(@PathVariable long goalId) {
        goals.delete(goalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lookups/categories")
    public List<LookupItem> categories() {
        return goals.categories();
    }

    @GetMapping("/lookups/kras")
    public List<LookupItem> kras() {
        return goals.kras();
    }

    @GetMapping("/lookups/kpis")
    public List<LookupItem> kpis() {
        return goals.kpis();
    }
}
