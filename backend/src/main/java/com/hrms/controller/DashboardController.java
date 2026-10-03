package com.hrms.controller;

import com.hrms.dto.DashboardOverview;
import com.hrms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get Oracle-backed dashboard metrics, charts, approvals, people, announcements, and events")
    public ResponseEntity<DashboardOverview> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            , @RequestParam(defaultValue = "6") int months
    ) {
        if (months != 6 && months != 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dashboard range must be 6 or 12 months");
        }
        return ResponseEntity.ok(dashboardService.getOverview(date, months));
    }
}
