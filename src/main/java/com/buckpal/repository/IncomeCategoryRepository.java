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
     * Find all income categories for a user across all budgets
     */
    @Query("SELECT ic FROM IncomeCategory ic JOIN ic.budget b WHERE b.user = :user ORDER BY ic.displayOrder ASC")
    List<IncomeCategory> findByUser(@Param("user") User user);
    
    /**
     * Find income category by ID and user (security check)
     */
    @Query("SELECT ic FROM IncomeCategory ic JOIN ic.budget b WHERE ic.id = :id AND b.user = :user")
    Optional<IncomeCategory> findByIdAndUser(@Param("id") Long id, @Param("user") User user);
    
    /**
     * Find default income categories for a budget
     */
    List<IncomeCategory> findByBudgetIdAndIsDefaultTrueOrderByDisplayOrderAsc(Long budgetId);
    
    /**
     * Find income categories by type for a specific budget
     */
    @Query("SELECT ic FROM IncomeCategory ic WHERE ic.budget.id = :budgetId AND ic.incomeType = :incomeType ORDER BY ic.displayOrder ASC")
    List<IncomeCategory> findByBudgetIdAndIncomeType(@Param("budgetId") Long budgetId, 
                                                     @Param("incomeType") IncomeCategory.IncomeType incomeType);
    
    /**
     * Find income categories with actual income > 0 for a budget
     */
    @Query("SELECT ic FROM IncomeCategory ic WHERE ic.budget.id = :budgetId AND ic.actualAmount > 0 ORDER BY ic.actualAmount DESC")
    List<IncomeCategory> findActiveBudgetIncomeCategories(@Param("budgetId") Long budgetId);
    
    /**
     * Get count of income categories for a budget
     */
    long countByBudgetId(Long budgetId);
    
    /**
     * Check if income category name exists for a budget (for validation)
     */
    boolean existsByBudgetIdAndName(Long budgetId, String name);
    
    /**
     * Find income categories by user and income type across all budgets
     */
    @Query("SELECT ic FROM IncomeCategory ic JOIN ic.budget b WHERE b.user = :user AND ic.incomeType = :incomeType")
    List<IncomeCategory> findByUserAndIncomeType(@Param("user") User user, 
                                                 @Param("incomeType") IncomeCategory.IncomeType incomeType);
    
    /**
     * Find income categories for specific budget period
     */
    @Query("SELECT ic FROM IncomeCategory ic JOIN ic.budget b WHERE b.user = :user AND b.budgetMonth = :month AND b.budgetYear = :year")
    List<IncomeCategory> findByUserAndBudgetPeriod(@Param("user") User user, 
                                                   @Param("month") Integer month, 
                                                   @Param("year") Integer year);
}