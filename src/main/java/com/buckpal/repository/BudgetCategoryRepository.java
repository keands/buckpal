package com.buckpal.repository;

import com.buckpal.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {
    // All query methods removed - only basic JpaRepository methods (findById, save, findAll, etc.) remain
}