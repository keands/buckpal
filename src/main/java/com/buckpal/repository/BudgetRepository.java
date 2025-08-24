package com.buckpal.repository;

import com.buckpal.entity.Budget;
import com.buckpal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    List<Budget> findByUserOrderByBudgetYearDescBudgetMonthDesc(User user);
    
    List<Budget> findByUserAndIsActiveTrue(User user);
    
    Optional<Budget> findByUserAndBudgetMonthAndBudgetYear(User user, Integer budgetMonth, Integer budgetYear);
    
    Optional<Budget> findByUserAndBudgetMonthAndBudgetYearAndIsActiveTrue(User user, Integer budgetMonth, Integer budgetYear);
    
    List<Budget> findByUserAndBudgetYear(User user, Integer budgetYear);
    
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.budgetYear = :year AND b.budgetMonth BETWEEN :startMonth AND :endMonth ORDER BY b.budgetYear DESC, b.budgetMonth DESC")
    List<Budget> findByUserAndDateRange(@Param("user") User user, 
                                       @Param("year") Integer year,
                                       @Param("startMonth") Integer startMonth,
                                       @Param("endMonth") Integer endMonth);
    
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND " +
           "((b.budgetYear = :startYear AND b.budgetMonth >= :startMonth) OR " +
           "(b.budgetYear > :startYear AND b.budgetYear < :endYear) OR " +
           "(b.budgetYear = :endYear AND b.budgetMonth <= :endMonth)) " +
           "ORDER BY b.budgetYear DESC, b.budgetMonth DESC")
    List<Budget> findByUserAndDateRangeAcrossYears(@Param("user") User user,
                                                  @Param("startYear") Integer startYear,
                                                  @Param("startMonth") Integer startMonth,
                                                  @Param("endYear") Integer endYear,
                                                  @Param("endMonth") Integer endMonth);
    
    // Helper method to find current month budget
    default Optional<Budget> findCurrentMonthBudget(User user) {
        LocalDate now = LocalDate.now();
        return findByUserAndBudgetMonthAndBudgetYearAndIsActiveTrue(user, now.getMonthValue(), now.getYear());
    }
    
    // Helper method to find previous month budget for income projection
    default Optional<Budget> findPreviousMonthBudget(User user) {
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        return findByUserAndBudgetMonthAndBudgetYear(user, previousMonth.getMonthValue(), previousMonth.getYear());
    }
}