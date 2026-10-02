package com.hrms.repository;

import com.hrms.entity.EmployeeLifecycleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeLifecycleEventRepository extends JpaRepository<EmployeeLifecycleEvent, Long> {
    List<EmployeeLifecycleEvent> findByEmployeeIdOrderByEventDateDesc(Long employeeId);
}
