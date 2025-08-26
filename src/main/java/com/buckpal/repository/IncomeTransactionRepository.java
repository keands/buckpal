package com.buckpal.repository;

import com.buckpal.entity.IncomeTransaction;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeTransactionRepository extends JpaRepository<IncomeTransaction, Long> {
    
    /**
     * Find all income transactions for a user
     */
    List<IncomeTransaction> findByUserOrderByTransactionDateDesc(User user);
    
    /**
     * Find income transactions for a specific income category
     */
    List<IncomeTransaction> findByIncomeCategoryIdOrderByTransactionDateDesc(Long incomeCategoryId);
    
    /**
     * Find income transaction by ID and user (security check)
     */
    @Query("SELECT it FROM IncomeTransaction it WHERE it.id = :id AND it.user = :user")
    Optional<IncomeTransaction> findByIdAndUser(@Param("id") Long id, @Param("user") User user);
    
    /**
     * Find income transactions by user with pagination
     */
    Page<IncomeTransaction> findByUserOrderByTransactionDateDesc(User user, Pageable pageable);
    
    /**
     * Find income transactions for a specific month and year
     */
    @Query("SELECT it FROM IncomeTransaction it WHERE it.user = :user " +
           "AND EXTRACT(MONTH FROM it.transactionDate) = :month " +
           "AND EXTRACT(YEAR FROM it.transactionDate) = :year " +
           "ORDER BY it.transactionDate DESC")
    List<IncomeTransaction> findByUserAndMonth(@Param("user") User user, 
                                               @Param("month") Integer month, 
                                               @Param("year") Integer year);
    
    /**
     * Get total income for a user in a specific month
     */
    @Query("SELECT COALESCE(SUM(it.amount), 0) FROM IncomeTransaction it WHERE it.user = :user " +
           "AND EXTRACT(MONTH FROM it.transactionDate) = :month " +
           "AND EXTRACT(YEAR FROM it.transactionDate) = :year")
    BigDecimal getTotalIncomeForMonth(@Param("user") User user, 
                                      @Param("month") Integer month, 
                                      @Param("year") Integer year);
    
    /**
     * Find income transactions by date range
     */
    @Query("SELECT it FROM IncomeTransaction it WHERE it.user = :user " +
           "AND it.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY it.transactionDate DESC")
    List<IncomeTransaction> findByUserAndDateRange(@Param("user") User user, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
    
    /**
     * Find income transactions by income category and date range
     */
    @Query("SELECT it FROM IncomeTransaction it WHERE it.incomeCategory.id = :incomeCategoryId " +
           "AND it.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY it.transactionDate DESC")
    List<IncomeTransaction> findByIncomeCategoryAndDateRange(@Param("incomeCategoryId") Long incomeCategoryId,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);
    
    /**
     * Find recurring income transactions
     */
    List<IncomeTransaction> findByUserAndIsRecurringTrueOrderByTransactionDateDesc(User user);
    
    /**
     * Find income transactions by source (e.g., "Bank Import", "Manual Entry")
     */
    List<IncomeTransaction> findByUserAndSourceOrderByTransactionDateDesc(User user, String source);
    
    /**
     * Find income transactions by user and category ID for a specific budget period
     */
    @Query("SELECT it FROM IncomeTransaction it JOIN it.incomeCategory ic JOIN ic.budget b " +
           "WHERE it.user = :user AND ic.id = :categoryId " +
           "AND EXTRACT(MONTH FROM it.transactionDate) = :month " +
           "AND EXTRACT(YEAR FROM it.transactionDate) = :year " +
           "ORDER BY it.transactionDate DESC")
    List<IncomeTransaction> findByUserAndCategoryForPeriod(@Param("user") User user,
                                                           @Param("categoryId") Long categoryId,
                                                           @Param("month") Integer month,
                                                           @Param("year") Integer year);
    
    /**
     * Get income statistics for user by month
     */
    @Query("SELECT EXTRACT(MONTH FROM it.transactionDate) as month, " +
           "EXTRACT(YEAR FROM it.transactionDate) as year, " +
           "COUNT(it) as count, " +
           "SUM(it.amount) as total " +
           "FROM IncomeTransaction it WHERE it.user = :user " +
           "GROUP BY EXTRACT(YEAR FROM it.transactionDate), EXTRACT(MONTH FROM it.transactionDate) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getIncomeStatsByUser(@Param("user") User user);
    
    /**
     * Find similar income transactions for smart assignment (by description pattern)
     */
    @Query("SELECT it FROM IncomeTransaction it WHERE it.user = :user " +
           "AND LOWER(it.description) LIKE LOWER(CONCAT('%', :pattern, '%')) " +
           "ORDER BY it.transactionDate DESC")
    List<IncomeTransaction> findSimilarIncomeTransactions(@Param("user") User user, 
                                                          @Param("pattern") String pattern);
    
    /**
     * Count income transactions for an income category
     */
    long countByIncomeCategoryId(Long incomeCategoryId);
    
    /**
     * Delete all income transactions for a specific income category
     */
    void deleteByIncomeCategoryId(Long incomeCategoryId);
    
    /**
     * Find income transactions by source transaction
     */
    List<IncomeTransaction> findBySourceTransaction(Transaction sourceTransaction);
}