package com.hrms.controller;

import com.hrms.dto.BulkActionRequest;
import com.hrms.dto.BulkActionResponse;
import com.hrms.entity.BulkAction;
import com.hrms.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Bulk Actions")
public class BulkActionController {

    private final EmployeeService employeeService;

    public BulkActionController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/bulk-actions")
    @Operation(summary = "Create and execute a bulk action")
    public ResponseEntity<BulkActionResponse> createBulkAction(@Valid @RequestBody BulkActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createBulkAction(request));
    }

    @GetMapping("/bulk-actions/{id}")
    @Operation(summary = "Get bulk action by ID")
    public ResponseEntity<BulkAction> getBulkActionById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getBulkActionById(id));
    }

    @GetMapping("/bulk-actions/history")
    @Operation(summary = "Get bulk action history")
    public ResponseEntity<List<BulkAction>> getBulkActionHistory() {
        return ResponseEntity.ok(employeeService.getBulkActionHistory());
    }
}
