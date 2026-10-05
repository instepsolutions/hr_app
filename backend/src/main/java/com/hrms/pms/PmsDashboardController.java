package com.hrms.pms;

import com.hrms.pms.PmsDtos.DashboardOverview;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/pms/dashboard")
@Tag(name = "PMS Dashboard")
public class PmsDashboardController {

    private final PmsAnalyticsService analytics;

    public PmsDashboardController(PmsAnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/overview")
    @Operation(summary = "Oracle-backed PMS dashboard: KPIs, goal progress, appraisals, ratings, departments, PIP, deadlines")
    public DashboardOverview overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department) {
        return analytics.dashboard(startDate, endDate, department);
    }

    @GetMapping("/goal-trend")
    @Operation(summary = "Goal status trend as DAILY, WEEKLY or MONTHLY points")
    public Object goalTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAILY") String granularity,
            @RequestParam(required = false) String department) {
        PmsDtos.Period period = analytics.period(startDate, endDate, department);
        return analytics.trend(period.startDate(), period.endDate(), granularity, period.department());
    }

    @GetMapping("/summary")
    @Operation(summary = "KPI cards only")
    public Object summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department) {
        return analytics.dashboard(startDate, endDate, department).kpis();
    }

    @GetMapping("/appraisal-status")
    @Operation(summary = "Appraisal stage breakdown")
    public Object appraisalStatus(@RequestParam(required = false) String department) {
        return analytics.dashboard(null, null, department).appraisalStatus();
    }

    @GetMapping("/rating-distribution")
    @Operation(summary = "Rating distribution of completed appraisals")
    public Object ratingDistribution(@RequestParam(required = false) String department) {
        return analytics.dashboard(null, null, department).ratingDistribution();
    }

    @GetMapping("/department-performance")
    @Operation(summary = "Average rating by department group")
    public Object departmentPerformance(@RequestParam(required = false) String department) {
        return analytics.dashboard(null, null, department).departmentPerformance();
    }

    @GetMapping("/top-performers")
    @Operation(summary = "Top five completed appraisals by rating")
    public Object topPerformers(@RequestParam(required = false) String department) {
        return analytics.dashboard(null, null, department).topPerformers();
    }

    @GetMapping("/pip-overview")
    @Operation(summary = "Performance improvement plan overview")
    public Object pipOverview(@RequestParam(required = false) String department) {
        return analytics.dashboard(null, null, department).pip();
    }

    @GetMapping("/deadlines")
    @Operation(summary = "Upcoming appraisal deadlines")
    public Object deadlines(@RequestParam(required = false) String department) {
        return analytics.dashboard(null, null, department).deadlines();
    }

    @GetMapping("/export")
    @Operation(summary = "Download the dashboard as CSV")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department) {
        DashboardOverview overview = analytics.dashboard(startDate, endDate, department);
        return csvResponse(analytics.dashboardCsv(overview), "pms-dashboard-" + overview.period().endDate() + ".csv");
    }

    static ResponseEntity<byte[]> csvResponse(String csv, String fileName) {
        byte[] body = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
