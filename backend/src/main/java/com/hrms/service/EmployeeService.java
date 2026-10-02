package com.hrms.service;

import com.hrms.dto.*;
import com.hrms.entity.*;
import com.hrms.exception.DuplicateResourceException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeStatusHistoryRepository employeeStatusHistoryRepository;
    private final EmployeeLifecycleEventRepository employeeLifecycleEventRepository;
    private final BulkActionRepository bulkActionRepository;
    private final BulkActionEmployeeRepository bulkActionEmployeeRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final DesignationRepository designationRepository;
    private final EmploymentTypeRepository employmentTypeRepository;
    private final EmployeeMapper employeeMapper;
    private final AuditLogService auditLogService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            EmployeeProfileRepository employeeProfileRepository,
            EmployeeStatusHistoryRepository employeeStatusHistoryRepository,
            EmployeeLifecycleEventRepository employeeLifecycleEventRepository,
            BulkActionRepository bulkActionRepository,
            BulkActionEmployeeRepository bulkActionEmployeeRepository,
            DepartmentRepository departmentRepository,
            LocationRepository locationRepository,
            DesignationRepository designationRepository,
            EmploymentTypeRepository employmentTypeRepository,
            EmployeeMapper employeeMapper,
            AuditLogService auditLogService
    ) {
        this.employeeRepository = employeeRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.employeeStatusHistoryRepository = employeeStatusHistoryRepository;
        this.employeeLifecycleEventRepository = employeeLifecycleEventRepository;
        this.bulkActionRepository = bulkActionRepository;
        this.bulkActionEmployeeRepository = bulkActionEmployeeRepository;
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.designationRepository = designationRepository;
        this.employmentTypeRepository = employmentTypeRepository;
        this.employeeMapper = employeeMapper;
        this.auditLogService = auditLogService;
    }

    public Page<EmployeeResponse> getEmployees(
            String search,
            Long departmentId,
            Long locationId,
            Long employmentTypeId,
            String status,
            LocalDate dateOfJoiningFrom,
            LocalDate dateOfJoiningTo,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Specification<Employee> spec = EmployeeSpecification.filterEmployees(
                search,
                departmentId,
                locationId,
                employmentTypeId,
                status,
                dateOfJoiningFrom,
                dateOfJoiningTo
        );

        Sort.Direction direction = sortDirection != null && sortDirection.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        String resolvedSort = (sortBy == null || sortBy.isBlank()) ? "employeeId" : sortBy;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSort));

        Page<Employee> employeePage = employeeRepository.findAll(spec, pageable);
        return employeePage.map(employeeMapper::toResponse);
    }

    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toResponse(employee);
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.findByOfficialEmail(request.getOfficialEmail()).isPresent()) {
            throw new DuplicateResourceException("Official email already exists: " + request.getOfficialEmail());
        }
        if (employeeRepository.findByEmployeeCode(request.getEmployeeCode()).isPresent()) {
            throw new DuplicateResourceException("Employee code already exists: " + request.getEmployeeCode());
        }

        Employee employee = employeeMapper.toEntity(request, null);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        employee.setCreatedBy("SYSTEM");
        employee.setUpdatedBy("SYSTEM");
        employee.setEmployeeStatus(request.getEmployeeStatus() == null ? "ACTIVE" : request.getEmployeeStatus());
        if (employee.getDisplayName() == null || employee.getDisplayName().isBlank()) {
            employee.setDisplayName(employee.getFirstName() + " " + employee.getLastName());
        }

        Employee saved = employeeRepository.save(employee);
        createStatusHistory(saved.getEmployeeId(), null, saved.getEmployeeStatus(), "INITIAL", "Employee created");
        auditLogService.record("EMPLOYEE", saved.getEmployeeId(), "CREATE", null, employeeAuditData(saved));
        return employeeMapper.toResponse(saved);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        employeeRepository.findByOfficialEmail(request.getOfficialEmail())
            .filter(existing -> !existing.getEmployeeId().equals(id))
            .ifPresent(existing -> { throw new DuplicateResourceException("Official email already exists: " + request.getOfficialEmail()); });
        employeeRepository.findByEmployeeCode(request.getEmployeeCode())
            .filter(existing -> !existing.getEmployeeId().equals(id))
            .ifPresent(existing -> { throw new DuplicateResourceException("Employee code already exists: " + request.getEmployeeCode()); });

        Map<String, Object> oldData = employeeAuditData(employee);
        String previousStatus = employee.getEmployeeStatus();
        Employee updatedEmployee = employeeMapper.toEntity(request, employee);
        updatedEmployee.setUpdatedAt(LocalDateTime.now());
        updatedEmployee.setUpdatedBy("SYSTEM");
        Employee saved = employeeRepository.save(updatedEmployee);

        if (!previousStatus.equals(saved.getEmployeeStatus())) {
            createStatusHistory(saved.getEmployeeId(), previousStatus, saved.getEmployeeStatus(), "UPDATE", "Employee updated");
        }
        auditLogService.record("EMPLOYEE", saved.getEmployeeId(), "UPDATE", oldData, employeeAuditData(saved));

        return employeeMapper.toResponse(saved);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        Map<String, Object> oldData = employeeAuditData(employee);
        String oldStatus = employee.getEmployeeStatus();
        if (!"INACTIVE".equals(oldStatus)) {
            employee.setEmployeeStatus("INACTIVE");
            employee.setUpdatedAt(LocalDateTime.now());
            employee.setUpdatedBy("SYSTEM");
            employeeRepository.save(employee);
            createStatusHistory(id, oldStatus, "INACTIVE", "DEACTIVATED", "Employee deactivated from directory");
        }
        auditLogService.record("EMPLOYEE", id, "DEACTIVATE", oldData, employeeAuditData(employee));
    }

    @Transactional
    public EmployeeResponse updateEmployeeStatus(Long id, StatusUpdateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        String oldStatus = employee.getEmployeeStatus();
        employee.setEmployeeStatus(request.getStatus());
        employee.setUpdatedAt(LocalDateTime.now());
        employee.setUpdatedBy("SYSTEM");
        Employee saved = employeeRepository.save(employee);
        createStatusHistory(id, oldStatus, request.getStatus(), request.getReason(), request.getRemarks());
        auditLogService.record("EMPLOYEE", id, "STATUS_UPDATE",
            Map.of("employeeStatus", oldStatus), Map.of("employeeStatus", saved.getEmployeeStatus()));
        return employeeMapper.toResponse(saved);
    }

    public EmployeeStatisticsResponse getEmployeeStatistics() {
        long total = employeeRepository.count();
        long active = employeeRepository.count((root, query, cb) -> cb.equal(root.get("employeeStatus"), "ACTIVE"));
        long onLeave = employeeRepository.count((root, query, cb) -> cb.equal(root.get("employeeStatus"), "ON_LEAVE"));
        long exited = employeeRepository.count((root, query, cb) -> cb.equal(root.get("employeeStatus"), "EXITED"));

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        long newJoiners = employeeRepository.count((root, query, cb) ->
            cb.between(root.get("dateOfJoining"), startOfMonth, today));

        long probation = employeeRepository.count((root, query, cb) ->
                cb.equal(root.get("employeeStatus"), "PROBATION"));

        return new EmployeeStatisticsResponse(total, active, newJoiners, onLeave, probation, exited);
    }

    public List<Department> getDepartments() {
        return departmentRepository.findAll();
    }

    public List<Location> getLocations() {
        return locationRepository.findAll();
    }

    public List<Designation> getDesignations() {
        return designationRepository.findAll();
    }

    public List<EmploymentType> getEmploymentTypes() {
        return employmentTypeRepository.findAll();
    }

    public EmployeeProfileResponse getEmployeeProfile(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        EmployeeProfile profile = employeeProfileRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> new EmployeeProfile());

        return employeeMapper.toProfileResponse(profile);
    }

    public List<EmployeeProfileResponse> getEmployeeProfiles(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) return List.of();
        return employeeProfileRepository.findByEmployeeIdIn(employeeIds).stream()
                .map(employeeMapper::toProfileResponse)
                .toList();
    }

    @Transactional
    public EmployeeProfileResponse updateEmployeeProfile(Long employeeId, EmployeeProfileRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        request.setEmployeeId(employeeId);
        EmployeeProfile existing = employeeProfileRepository.findByEmployeeId(employeeId).orElse(null);
        EmployeeProfile profile = employeeMapper.toProfileEntity(request, existing);
        profile.setEmployeeId(employeeId);
        profile.setUpdatedAt(LocalDateTime.now());
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(LocalDateTime.now());
        }
        EmployeeProfile saved = employeeProfileRepository.save(profile);
        auditLogService.record("EMPLOYEE_PROFILE", saved.getProfileId(), "UPDATE", null,
            Map.of("employeeId", employeeId, "updatedFields", List.of(
                "bloodGroup", "maritalStatus", "nationality", "emergencyContactName",
                "emergencyContactNumber", "emergencyContactRelation", "currentAddress",
                "permanentAddress", "city", "state", "postalCode")));
        return employeeMapper.toProfileResponse(saved);
    }

    public List<EmployeeLifecycleEvent> getEmployeeLifecycle(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }
        return employeeLifecycleEventRepository.findByEmployeeIdOrderByEventDateDesc(employeeId);
    }

    public List<EmployeeStatusHistory> getStatusHistory(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }
        return employeeStatusHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId);
    }

        public LifecycleStatisticsResponse getLifecycleStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate newHireCutoff = today.minusDays(30);
        LocalDate confirmationCutoff = today.plusDays(30);
        List<Employee> employees = employeeRepository.findAll();
        List<EmployeeLifecycleEvent> events = employeeLifecycleEventRepository.findAll();

        long newHires = employees.stream()
            .filter(employee -> employee.getDateOfJoining() != null
                && !employee.getDateOfJoining().isBefore(newHireCutoff)
                && !employee.getDateOfJoining().isAfter(today))
            .count();
        long activeEmployees = employees.stream()
            .filter(employee -> "ACTIVE".equals(employee.getEmployeeStatus()))
            .count();
        long onboarding = employees.stream()
            .filter(employee -> "PROBATION".equals(employee.getEmployeeStatus()))
            .count();
        long dueForConfirmation = employees.stream()
            .filter(employee -> employee.getConfirmationDate() != null
                && !employee.getConfirmationDate().isBefore(today)
                && !employee.getConfirmationDate().isAfter(confirmationCutoff))
            .count();
        long exits = employees.stream()
            .filter(employee -> "EXITED".equals(employee.getEmployeeStatus())
                || "RESIGNED".equals(employee.getEmployeeStatus()))
            .count();

        Set<Long> developmentEmployees = new HashSet<>();
        Map<String, Set<Long>> stageEmployees = new LinkedHashMap<>();
        stageEmployees.put("Attract & Recruit", new HashSet<>());
        stageEmployees.put("Onboard", new HashSet<>());
        stageEmployees.put("Engage & Manage", new HashSet<>());
        stageEmployees.put("Develop", new HashSet<>());
        stageEmployees.put("Retain & Reward", new HashSet<>());
        stageEmployees.put("Exit", new HashSet<>());

        for (EmployeeLifecycleEvent event : events) {
            String stage = switch (event.getEventType()) {
            case "RECRUITMENT", "OFFER" -> "Attract & Recruit";
            case "JOINING", "ONBOARDING" -> "Onboard";
            case "PROBATION", "CONFIRMATION", "TRANSFER", "NOTICE_PERIOD" -> "Engage & Manage";
            case "TRAINING", "PERFORMANCE" -> "Develop";
            case "PROMOTION", "REWARD" -> "Retain & Reward";
            case "EXIT", "EXIT_INTERVIEW", "FULL_AND_FINAL" -> "Exit";
            default -> null;
            };
            if (stage != null) stageEmployees.get(stage).add(event.getEmployeeId());
            if (("TRAINING".equals(event.getEventType()) || "PERFORMANCE".equals(event.getEventType()))
                && event.getEventDate() != null
                && !event.getEventDate().isBefore(today.minusDays(365))) {
            developmentEmployees.add(event.getEmployeeId());
            }
        }

        Map<String, Long> stageCounts = new LinkedHashMap<>();
        stageEmployees.forEach((stage, ids) -> stageCounts.put(stage, (long) ids.size()));
        return new LifecycleStatisticsResponse(
            newHires,
            onboarding,
            activeEmployees,
            (long) developmentEmployees.size(),
            dueForConfirmation,
            exits,
            stageCounts
        );
        }

        public List<EmployeeLifecycleEvent> getLifecycleEvents() {
        return employeeLifecycleEventRepository.findAll();
        }

    public List<Employee> getOrganizationTree() {
        return employeeRepository.findAll();
    }

    @Transactional
    public BulkActionResponse createBulkAction(BulkActionRequest request) {
        if (request.getEmployeeIds() == null || request.getEmployeeIds().isEmpty()) {
            throw new IllegalArgumentException("Employee IDs are required");
        }

        BulkAction bulkAction = new BulkAction();
        bulkAction.setActionType(request.getActionType());
        bulkAction.setRequestedBy("SYSTEM");
        bulkAction.setRequestedAt(LocalDateTime.now());
        bulkAction.setTotalEmployees(request.getEmployeeIds().size());
        bulkAction.setStatus("IN_PROGRESS");
        bulkAction.setRemarks(request.getRemarks());
        BulkAction saved = bulkActionRepository.save(bulkAction);

        int successCount = 0;
        int failureCount = 0;
        String actionType = request.getActionType().trim().toUpperCase(Locale.ROOT);

        for (Long employeeId : request.getEmployeeIds()) {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                failureCount++;
                continue;
            }

            BulkActionEmployee baEmployee = new BulkActionEmployee();
            baEmployee.setBulkActionId(saved.getBulkActionId());
            baEmployee.setEmployeeId(employeeId);
            baEmployee.setOldValue(getBulkActionOldValue(employee, actionType));
            baEmployee.setNewValue(request.getNewValue());
            baEmployee.setProcessedAt(LocalDateTime.now());

            try {
                String nextStatus = applyBulkAction(employee, actionType, request.getNewValue());
                employee.setUpdatedAt(LocalDateTime.now());
                employee.setUpdatedBy("SYSTEM");
                employeeRepository.save(employee);
                if (nextStatus != null && !nextStatus.equals(baEmployee.getOldValue())) {
                    createStatusHistory(employeeId, baEmployee.getOldValue(), nextStatus, "BULK_ACTION", request.getRemarks());
                }
                baEmployee.setNewValue(getBulkActionNewValue(employee, actionType));
                baEmployee.setStatus("SUCCESS");
                successCount++;
            } catch (IllegalArgumentException | ResourceNotFoundException ex) {
                baEmployee.setStatus("FAILED");
                baEmployee.setErrorMessage(ex.getMessage());
                failureCount++;
            }
            bulkActionEmployeeRepository.save(baEmployee);
        }

        saved.setSuccessfulCount(successCount);
        saved.setFailedCount(failureCount);
        saved.setStatus(successCount == 0 ? "FAILED" : "COMPLETED");
        saved.setCompletedAt(LocalDateTime.now());
        bulkActionRepository.save(saved);

        BulkActionResponse response = new BulkActionResponse();
        response.setBulkActionId(saved.getBulkActionId());
        response.setActionType(saved.getActionType());
        response.setStatus(saved.getStatus());
        response.setTotalEmployees(saved.getTotalEmployees());
        response.setSuccessfulCount(saved.getSuccessfulCount());
        response.setFailedCount(saved.getFailedCount());
        response.setRemarks(saved.getRemarks());
        response.setEmployeeIds(request.getEmployeeIds());
        auditLogService.record("BULK_ACTION", saved.getBulkActionId(), "EXECUTE", null, Map.of(
            "actionType", saved.getActionType(),
            "totalEmployees", saved.getTotalEmployees(),
            "successfulCount", saved.getSuccessfulCount(),
            "failedCount", saved.getFailedCount()
        ));
        return response;
    }

    private String applyBulkAction(Employee employee, String actionType, String newValue) {
        switch (actionType) {
            case "UPDATE_DEPARTMENT" -> employee.setDepartmentId(requireExistingId(newValue, "Department", departmentRepository::existsById));
            case "UPDATE_DESIGNATION" -> employee.setDesignationId(requireExistingId(newValue, "Designation", designationRepository::existsById));
            case "UPDATE_LOCATION" -> employee.setLocationId(requireExistingId(newValue, "Location", locationRepository::existsById));
            case "UPDATE_REPORTING_MANAGER" -> {
                Long managerId = requireExistingId(newValue, "Reporting manager", employeeRepository::existsById);
                if (managerId.equals(employee.getEmployeeId())) {
                    throw new IllegalArgumentException("An employee cannot report to themselves");
                }
                employee.setReportingManagerId(managerId);
            }
            case "CHANGE_STATUS", "ACTIVATE_EMPLOYEES", "DEACTIVATE_EMPLOYEES" -> {
                String status = switch (actionType) {
                    case "ACTIVATE_EMPLOYEES" -> "ACTIVE";
                    case "DEACTIVATE_EMPLOYEES" -> "INACTIVE";
                    default -> newValue == null ? "" : newValue.trim().toUpperCase(Locale.ROOT);
                };
                if (!List.of("ACTIVE", "ON_LEAVE", "PROBATION", "NOTICE_PERIOD", "RESIGNED", "EXITED", "INACTIVE").contains(status)) {
                    throw new IllegalArgumentException("Unsupported employee status: " + status);
                }
                employee.setEmployeeStatus(status);
                return status;
            }
            default -> throw new IllegalArgumentException("Unsupported bulk action: " + actionType);
        }
        return null;
    }

    private Long requireExistingId(String value, String resourceName, java.util.function.Predicate<Long> exists) {
        try {
            Long id = Long.valueOf(value);
            if (exists.test(id)) return id;
        } catch (NumberFormatException | NullPointerException ignored) {
        }
        throw new ResourceNotFoundException(resourceName + " not found");
    }

    private String getBulkActionOldValue(Employee employee, String actionType) {
        return switch (actionType) {
            case "UPDATE_DEPARTMENT" -> employee.getDepartmentId() == null ? null : employee.getDepartmentId().toString();
            case "UPDATE_DESIGNATION" -> employee.getDesignationId() == null ? null : employee.getDesignationId().toString();
            case "UPDATE_LOCATION" -> employee.getLocationId() == null ? null : employee.getLocationId().toString();
            case "UPDATE_REPORTING_MANAGER" -> employee.getReportingManagerId() == null ? null : employee.getReportingManagerId().toString();
            default -> employee.getEmployeeStatus();
        };
    }

    private String getBulkActionNewValue(Employee employee, String actionType) {
        return getBulkActionOldValue(employee, actionType);
    }

    private Map<String, Object> employeeAuditData(Employee employee) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("employeeCode", employee.getEmployeeCode());
        data.put("employeeStatus", employee.getEmployeeStatus());
        data.put("departmentId", employee.getDepartmentId());
        data.put("designationId", employee.getDesignationId());
        data.put("locationId", employee.getLocationId());
        data.put("employmentTypeId", employee.getEmploymentTypeId());
        data.put("reportingManagerId", employee.getReportingManagerId());
        return data;
    }

    public BulkAction getBulkActionById(Long id) {
        return bulkActionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bulk action not found with id: " + id));
    }

    public List<BulkAction> getBulkActionHistory() {
        return bulkActionRepository.findAll();
    }

    private void createStatusHistory(Long employeeId, String oldStatus, String newStatus, String reason, String remarks) {
        if (newStatus == null || newStatus.isBlank()) return;

        EmployeeStatusHistory history = new EmployeeStatusHistory();
        history.setEmployeeId(employeeId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setEffectiveDate(LocalDate.now());
        history.setReason(reason);
        history.setRemarks(remarks);
        history.setChangedBy("SYSTEM");
        history.setCreatedAt(LocalDateTime.now());
        employeeStatusHistoryRepository.save(history);
    }
}
