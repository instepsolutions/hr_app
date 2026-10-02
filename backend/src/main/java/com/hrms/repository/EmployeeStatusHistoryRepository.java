package com.hrms.repository;

import com.hrms.entity.EmployeeStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeStatusHistoryRepository extends JpaRepository<EmployeeStatusHistory, Long> {
    List<EmployeeStatusHistory> findByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);
}
