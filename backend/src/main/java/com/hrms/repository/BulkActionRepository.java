package com.hrms.repository;

import com.hrms.entity.BulkAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkActionRepository extends JpaRepository<BulkAction, Long> {
}
