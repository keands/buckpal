package com.buckpal.repository;

import com.buckpal.entity.Budget;
import com.buckpal.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {
    
    List<BudgetCategory> findByBudgetOrderBySortOrderAscNameAsc(Budget budget);
    
    List<BudgetCategory> findByBudgetAndIsActiveTrueOrderBySortOrderAscNameAsc(Budget budget);
    
    List<BudgetCategory> findByBudgetAndParentCategoryIsNullOrderBySortOrderAscNameAsc(Budget budget);
    
    List<BudgetCategory> findByBudgetAndParentCategoryIsNullAndIsActiveTrueOrderBySortOrderAscNameAsc(Budget budget);
    
    List<BudgetCategory> findByParentCategoryOrderBySortOrderAscNameAsc(BudgetCategory parentCategory);
    
    List<BudgetCategory> findByBudgetAndCategoryTypeOrderBySortOrderAscNameAsc(Budget budget, BudgetCategory.BudgetCategoryType categoryType);
    
    Optional<BudgetCategory> findByBudgetAndName(Budget budget, String name);
    
    @Query("SELECT bc FROM BudgetCategory bc WHERE bc.budget = :budget AND bc.spentAmount > bc.allocatedAmount")
    List<BudgetCategory> findOverBudgetCategories(@Param("budget") Budget budget);
    
    @Query("SELECT bc FROM BudgetCategory bc WHERE bc.budget = :budget AND " +
           "bc.allocatedAmount > 0 AND (bc.spentAmount / bc.allocatedAmount) >= :threshold")
    List<BudgetCategory> findCategoriesNearBudgetLimit(@Param("budget") Budget budget, 
                                                      @Param("threshold") BigDecimal threshold);
    
    @Query("SELECT SUM(bc.allocatedAmount) FROM BudgetCategory bc WHERE bc.budget = :budget AND bc.categoryType = :categoryType")
    BigDecimal sumAllocatedAmountByCategoryType(@Param("budget") Budget budget, 
                                               @Param("categoryType") BudgetCategory.BudgetCategoryType categoryType);
    
    @Query("SELECT SUM(bc.spentAmount) FROM BudgetCategory bc WHERE bc.budget = :budget AND bc.categoryType = :categoryType")
    BigDecimal sumSpentAmountByCategoryType(@Param("budget") Budget budget, 
                                           @Param("categoryType") BudgetCategory.BudgetCategoryType categoryType);
    
    // Count subcategories
    Long countByParentCategory(BudgetCategory parentCategory);
    
    // Check if category has transactions linked
    @Query("SELECT COUNT(t) > 0 FROM BudgetCategory bc JOIN bc.transactions t WHERE bc = :category")
    boolean hasLinkedTransactions(@Param("category") BudgetCategory category);
}