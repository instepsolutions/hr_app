package com.hrms.controller;

import com.hrms.dto.*;
import com.hrms.entity.Department;
import com.hrms.entity.Designation;
import com.hrms.entity.EmploymentType;
import com.hrms.entity.Location;
import com.hrms.entity.Employee;
import com.hrms.entity.EmployeeLifecycleEvent;
import com.hrms.entity.EmployeeProfile;
import com.hrms.entity.EmployeeStatusHistory;
import com.hrms.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Tag(name = "Employee Management")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employees")
    @Operation(summary = "Get employees with search, filtering, pagination and sorting")
    public ResponseEntity<PageResponse<EmployeeResponse>> getEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long employmentTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfJoiningFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfJoiningTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "employeeId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
        if (dateOfJoiningFrom != null && dateOfJoiningTo != null && dateOfJoiningFrom.isAfter(dateOfJoiningTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of joining start must not be after end");
        }
        if (!Set.of("employeeId", "employeeCode", "displayName", "firstName", "lastName", "dateOfJoining", "employeeStatus", "createdAt", "updatedAt").contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported employee sort field");
        }
        return ResponseEntity.ok(PageResponse.from(employeeService.getEmployees(
                search, departmentId, locationId, employmentTypeId, status,
                dateOfJoiningFrom, dateOfJoiningTo, page, size, sortBy, sortDirection
        )));
    }

    @GetMapping("/employees/{id}")
    @Operation(summary = "Get employee by ID")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PostMapping("/employees")
    @Operation(summary = "Create employee")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    @PutMapping("/employees/{id}")
    @Operation(summary = "Update employee")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/employees/{id}")
    @Operation(summary = "Delete employee")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/employees/{id}/status")
    @Operation(summary = "Update employee status")
    public ResponseEntity<EmployeeResponse> updateEmployeeStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployeeStatus(id, request));
    }

    @GetMapping("/employees/statistics")
    @Operation(summary = "Get employee statistics dashboard values")
    public ResponseEntity<EmployeeStatisticsResponse> getEmployeeStatistics() {
        return ResponseEntity.ok(employeeService.getEmployeeStatistics());
    }

    @GetMapping("/departments")
    @Operation(summary = "Get all departments")
    public ResponseEntity<List<Department>> getDepartments() {
        return ResponseEntity.ok(employeeService.getDepartments());
    }

    @GetMapping("/locations")
    @Operation(summary = "Get all locations")
    public ResponseEntity<List<Location>> getLocations() {
        return ResponseEntity.ok(employeeService.getLocations());
    }

    @GetMapping("/designations")
    @Operation(summary = "Get all designations")
    public ResponseEntity<List<Designation>> getDesignations() {
        return ResponseEntity.ok(employeeService.getDesignations());
    }

    @GetMapping("/employment-types")
    @Operation(summary = "Get all employment types")
    public ResponseEntity<List<EmploymentType>> getEmploymentTypes() {
        return ResponseEntity.ok(employeeService.getEmploymentTypes());
    }

    @GetMapping("/employees/{id}/profile")
    @Operation(summary = "Get employee profile")
    public ResponseEntity<EmployeeProfileResponse> getEmployeeProfile(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeProfile(id));
    }

    @GetMapping("/employee-profiles")
    @Operation(summary = "Get profiles for a set of employee IDs")
    public ResponseEntity<List<EmployeeProfileResponse>> getEmployeeProfiles(@RequestParam List<Long> employeeIds) {
        return ResponseEntity.ok(employeeService.getEmployeeProfiles(employeeIds));
    }

    @PutMapping("/employees/{id}/profile")
    @Operation(summary = "Update employee profile")
    public ResponseEntity<EmployeeProfileResponse> updateEmployeeProfile(@PathVariable Long id, @Valid @RequestBody EmployeeProfileRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployeeProfile(id, request));
    }

    @GetMapping("/employees/{id}/lifecycle")
    @Operation(summary = "Get employee lifecycle events")
    public ResponseEntity<List<EmployeeLifecycleEvent>> getEmployeeLifecycle(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeLifecycle(id));
    }

    @GetMapping("/employees/{id}/status-history")
    @Operation(summary = "Get employee status history")
    public ResponseEntity<List<EmployeeStatusHistory>> getStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getStatusHistory(id));
    }

    @GetMapping("/lifecycle/statistics")
    @Operation(summary = "Get employee lifecycle statistics and stage counts")
    public ResponseEntity<LifecycleStatisticsResponse> getLifecycleStatistics() {
        return ResponseEntity.ok(employeeService.getLifecycleStatistics());
    }

    @GetMapping("/lifecycle/events")
    @Operation(summary = "Get all employee lifecycle events")
    public ResponseEntity<List<EmployeeLifecycleEvent>> getLifecycleEvents() {
        return ResponseEntity.ok(employeeService.getLifecycleEvents());
    }

    @GetMapping("/organization/tree")
    @Operation(summary = "Get organization hierarchy data")
    public ResponseEntity<List<Employee>> getOrganizationTree() {
        return ResponseEntity.ok(employeeService.getOrganizationTree());
    }
}
