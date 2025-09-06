package com.buckpal.repository;

import com.buckpal.entity.IncomeCategory;
import com.buckpal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, Long> {
    
    /**
     * Find all income categories for a specific budget
     */
    List<IncomeCategory> findByBudgetIdOrderByDisplayOrderAsc(Long budgetId);
    
    
    /**
     * Find income category by ID and user (security check)
     */
    @Query("SELECT ic FROM IncomeCategory ic JOIN ic.budget b WHERE ic.id = :id AND b.user = :user")
    Optional<IncomeCategory> findByIdAndUser(@Param("id") Long id, @Param("user") User user);
    
    
    
    
    
    
    /**
     * Find income categories by user and income type across all budgets
     */
    @Query("SELECT ic FROM IncomeCategory ic JOIN ic.budget b WHERE b.user = :user AND ic.incomeType = :incomeType")
    List<IncomeCategory> findByUserAndIncomeType(@Param("user") User user, 
                                                 @Param("incomeType") IncomeCategory.IncomeType incomeType);
    
}