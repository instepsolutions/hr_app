package com.hrms.service;

import com.hrms.entity.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeSpecification {

    public static Specification<Employee> filterEmployees(
            String search,
            Long departmentId,
            Long locationId,
            Long employmentTypeId,
            String status,
            LocalDate dateOfJoiningFrom,
            LocalDate dateOfJoiningTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";
                Predicate byEmployeeCode = cb.like(cb.lower(root.get("employeeCode")), likePattern);
                Predicate byName = cb.like(cb.lower(root.get("displayName")), likePattern);
                Predicate byEmail = cb.like(cb.lower(root.get("officialEmail")), likePattern);
                predicates.add(cb.or(byEmployeeCode, byName, byEmail));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("departmentId"), departmentId));
            }

            if (locationId != null) {
                predicates.add(cb.equal(root.get("locationId"), locationId));
            }

            if (employmentTypeId != null) {
                predicates.add(cb.equal(root.get("employmentTypeId"), employmentTypeId));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("employeeStatus"), status));
            }

            if (dateOfJoiningFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfJoining"), dateOfJoiningFrom));
            }

            if (dateOfJoiningTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfJoining"), dateOfJoiningTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
