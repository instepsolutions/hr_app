package com.hrms.repository;

import com.hrms.entity.BulkActionEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkActionEmployeeRepository extends JpaRepository<BulkActionEmployee, Long> {
    List<BulkActionEmployee> findByBulkActionId(Long bulkActionId);
}
