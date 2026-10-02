package com.hrms.repository;

import com.hrms.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    Optional<EmployeeProfile> findByEmployeeId(Long employeeId);

    List<EmployeeProfile> findByEmployeeIdIn(List<Long> employeeIds);
}
