package com.buckpal.repository;

import com.buckpal.entity.Account;
import com.buckpal.entity.BudgetCategory;
import com.buckpal.entity.BudgetCategoryKey;
import com.buckpal.entity.Category;
import com.buckpal.entity.IncomeCategory;
import com.buckpal.entity.ProjectCategory;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.Transaction.TransactionType;
import com.buckpal.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByAccountAndTransactionDateBetween(
        Account account, LocalDate startDate, LocalDate endDate);
    
    Page<Transaction> findByAccount(Account account, Pageable pageable);
    
    List<Transaction> findByAccount(Account account);
    
    Long countByAccount(Account account);
    
    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN t.transactionType = 'INCOME' THEN t.amount 
                WHEN t.transactionType = 'EXPENSE' THEN -ABS(t.amount)
                WHEN t.transactionType = 'TRANSFER' THEN t.amount
                ELSE 0 
            END
        ), 0) 
        FROM Transaction t 
        WHERE t.account = :account
        """)
    BigDecimal calculateBalanceByAccount(@Param("account") Account account);
    
    List<Transaction> findByAccountAndCategory(Account account, Category category);
    
    List<Transaction> findByAccountAndTransactionType(Account account, TransactionType transactionType);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.user = :user AND t.transactionType = :transactionType ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserAndTransactionType(@Param("user") User user, @Param("transactionType") TransactionType transactionType);
    
    Optional<Transaction> findByPlaidTransactionId(String plaidTransactionId);
    
    @Query("SELECT t FROM Transaction t JOIN FETCH t.account LEFT JOIN FETCH t.category WHERE t.account IN :accounts ORDER BY t.transactionDate DESC")
    Page<Transaction> findByAccountsOrderByTransactionDateDesc(
        @Param("accounts") List<Account> accounts, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t JOIN FETCH t.account LEFT JOIN FETCH t.category WHERE t.account IN :accounts AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByAccountsAndDateRange(
        @Param("accounts") List<Account> accounts, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t FROM Transaction t JOIN FETCH t.account LEFT JOIN FETCH t.category WHERE t.account IN :accounts AND t.transactionDate = :date ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findByAccountInAndTransactionDateOrderByTransactionDateDesc(
        @Param("accounts") List<Account> accounts, 
        @Param("date") LocalDate date);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account IN :accounts AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    Double sumAmountByAccountsAndTypeAndDateRange(
        @Param("accounts") List<Account> accounts,
        @Param("type") TransactionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.transactionDate = :date AND t.amount = :amount AND t.description = :description")
    Optional<Transaction> findByAccountIdAndTransactionDateAndAmountAndDescription(
        @Param("accountId") Long accountId,
        @Param("date") LocalDate date,
        @Param("amount") BigDecimal amount,
        @Param("description") String description);
    
    @Query("""
        SELECT t.transactionDate as date,
               SUM(CASE WHEN t.transactionType = 'INCOME' THEN t.amount ELSE 0 END) as totalIncome,
               SUM(CASE WHEN t.transactionType = 'EXPENSE' THEN ABS(t.amount) ELSE 0 END) as totalExpense,
               SUM(CASE WHEN t.transactionType = 'INCOME' THEN t.amount WHEN t.transactionType = 'EXPENSE' THEN -ABS(t.amount) ELSE 0 END) as netAmount,
               COUNT(t) as transactionCount
        FROM Transaction t
        WHERE t.account IN :accounts
        AND t.transactionDate BETWEEN :startDate AND :endDate
        GROUP BY t.transactionDate
        ORDER BY t.transactionDate
        """)
    List<Object[]> findCalendarDataRawByAccountsAndDateRange(
        @Param("accounts") List<Account> accounts,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    List<Transaction> findByProjectCategoriesContaining(ProjectCategory projectCategory);
    
    @Query("SELECT t FROM Transaction t JOIN t.projectCategories pc WHERE pc = :projectCategory")
    List<Transaction> findByProjectCategory(@Param("projectCategory") ProjectCategory projectCategory);
    
    @Query("SELECT t FROM Transaction t JOIN t.projectCategories pc WHERE pc = :projectCategory AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByProjectCategoryAndDateRange(
        @Param("projectCategory") ProjectCategory projectCategory,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t JOIN t.projectCategories pc WHERE pc = :projectCategory AND t.transactionType = :type")
    Double sumAmountByProjectCategoryAndType(
        @Param("projectCategory") ProjectCategory projectCategory,
        @Param("type") TransactionType type);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t JOIN t.projectCategories pc WHERE pc = :projectCategory AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    Double sumAmountByProjectCategoryAndTypeAndDateRange(
        @Param("projectCategory") ProjectCategory projectCategory,
        @Param("type") TransactionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(t) FROM Transaction t JOIN t.projectCategories pc WHERE pc = :projectCategory")
    Long countByProjectCategory(@Param("projectCategory") ProjectCategory projectCategory);
    
    @Query("SELECT t FROM Transaction t JOIN t.projectCategories pc WHERE pc IN :projectCategories AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByProjectCategoriesInAndDateRange(
        @Param("projectCategories") List<ProjectCategory> projectCategories,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    // Transaction Assignment queries
    @Query("SELECT t FROM Transaction t WHERE t.account.user = :user")
    List<Transaction> findByUser(@Param("user") User user);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.user = :user AND t.assignmentStatus = :status")
    List<Transaction> findByUserAndAssignmentStatus(
        @Param("user") User user,
        @Param("status") Transaction.AssignmentStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.account.user = :user")
    Optional<Transaction> findByIdAndUser(@Param("id") Long id, @Param("user") User user);
    
    // Budget category assignment queries - Modern approach using category mapping
    @Query("""
        SELECT t FROM Transaction t 
        INNER JOIN t.category c 
        WHERE c.budgetCategoryKey = :budgetCategoryKey
        AND t.account.user = :user
        """)
    List<Transaction> findByBudgetCategory(
        @Param("budgetCategoryKey") BudgetCategoryKey budgetCategoryKey, 
        @Param("user") User user);
    
    
    // Bulk unassign transactions from budget categories before budget deletion
    // Simplified approach: If transaction is assigned to this budget's categories, unassign it
    @Modifying
    @Query("""
    UPDATE Transaction t SET t.category = NULL, t.assignmentStatus = 'UNASSIGNED' 
        WHERE t.account.user = :user
        AND t.category IS NOT NULL
        AND t.category.budgetCategoryKey IN (
            SELECT bc.categoryKey FROM BudgetCategory bc 
            WHERE bc.budget.id = :budgetId AND bc.budget.user = :user
        )
        """)
    void unassignTransactionsFromBudget(
        @Param("budgetId") Long budgetId,
        @Param("user") User user);
        
    // Legacy compatibility methods - convert old method calls to new approach
    default List<Transaction> findByBudgetCategory(BudgetCategory budgetCategory) {
        if (budgetCategory.getCategoryKey() == null) {
            return new ArrayList<>();
        }
        return findByBudgetCategory(budgetCategory.getCategoryKey(), budgetCategory.getBudget().getUser());
    }
    
    
    // Find income transactions not yet linked to income categories
    @Query("""
        SELECT t FROM Transaction t 
        WHERE t.account.user = :user 
        AND t.transactionType = 'INCOME' 
        AND t.incomeCategory IS NULL
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findUnlinkedIncomeTransactionsByUser(@Param("user") User user);
    
    // Find income transactions not yet linked to income categories within a date range
    @Query("""
        SELECT t FROM Transaction t 
        WHERE t.account.user = :user 
        AND t.transactionType = 'INCOME' 
        AND t.incomeCategory IS NULL
        AND t.transactionDate BETWEEN :startDate AND :endDate
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findUnlinkedIncomeTransactionsByUserAndDateRange(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    // Find income transactions by user and date range (for historical analysis)
    @Query("""
        SELECT t FROM Transaction t 
        WHERE t.account.user = :user 
        AND t.transactionType = 'INCOME'
        AND t.transactionDate BETWEEN :startDate AND :endDate
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findIncomeTransactionsByUserAndDateRange(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    // Find income transactions by category
    @Query("""
        SELECT t FROM Transaction t 
        WHERE t.incomeCategory = :incomeCategory
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findByIncomeCategory(@Param("incomeCategory") IncomeCategory incomeCategory);
    
    // Find income transactions by category and date range
    @Query("""
        SELECT t FROM Transaction t 
        WHERE t.incomeCategory = :incomeCategory
        AND t.transactionDate BETWEEN :startDate AND :endDate
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findByIncomeCategoryAndDateRange(
        @Param("incomeCategory") IncomeCategory incomeCategory,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    // Calculate total income for category
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t 
        WHERE t.incomeCategory = :incomeCategory
        """)
    BigDecimal sumAmountByIncomeCategory(@Param("incomeCategory") IncomeCategory incomeCategory);
    
    // Methods for intelligent assignment
    @Query("SELECT t FROM Transaction t WHERE t.account.user = :user AND t.detailedCategoryId IS NULL")
    List<Transaction> findByUserAndDetailedCategoryIsNull(@Param("user") User user);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.user = :user")
    long countByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.user = :user AND t.detailedCategoryId IS NOT NULL")
    long countByUserAndDetailedCategoryIdIsNotNull(@Param("user") User user);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.user = :user AND t.needsReview = true")
    long countByUserAndNeedsReviewTrue(@Param("user") User user);
    
    /**
     * Calculate spent amounts by budget category via category mapping
     * This joins transactions -> categories -> budget category mapping to get totals
     */
    @Query("""
        SELECT c.budgetCategoryKey, SUM(ABS(t.amount))
        FROM Transaction t 
        JOIN t.category c 
        WHERE t.account.user = :user 
        AND t.transactionType = 'EXPENSE'
        AND t.transactionDate BETWEEN :startDate AND :endDate
        AND c.budgetCategoryKey IS NOT NULL
        GROUP BY c.budgetCategoryKey
        """)
    List<Object[]> calculateSpentAmountsByBudgetCategory(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Debug query to check transactions and their category assignments
     */
    @Query("""
        SELECT t.id, t.amount, t.transactionType, t.transactionDate, 
               c.name, c.budgetCategoryKey,
               CASE WHEN t.category IS NULL THEN 'NO_CATEGORY' ELSE 'HAS_CATEGORY' END
        FROM Transaction t 
        LEFT JOIN t.category c 
        WHERE t.account.user = :user 
        AND t.transactionDate BETWEEN :startDate AND :endDate
        ORDER BY t.transactionDate DESC
        """)
    List<Object[]> debugTransactionCategories(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Optimized query to find transactions by budget category key and date range
     * Uses SQL join instead of loading all transactions into memory
     */
    @Query("""
        SELECT t FROM Transaction t 
        JOIN t.category c 
        WHERE t.account.user = :user 
        AND c.budgetCategoryKey = :budgetCategoryKey
        AND t.transactionDate BETWEEN :startDate AND :endDate
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findTransactionsByBudgetCategoryKeyAndDateRange(
        @Param("user") User user,
        @Param("budgetCategoryKey") BudgetCategoryKey budgetCategoryKey,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}