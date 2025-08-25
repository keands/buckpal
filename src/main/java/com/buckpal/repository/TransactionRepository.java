package com.buckpal.repository;

import com.buckpal.entity.Account;
import com.buckpal.entity.BudgetCategory;
import com.buckpal.entity.Category;
import com.buckpal.entity.ProjectCategory;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.Transaction.TransactionType;
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
    
    // Budget category assignment queries
    @Query("SELECT t FROM Transaction t WHERE t.budgetCategory = :budgetCategory AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByBudgetCategoryAndDateRange(
        @Param("budgetCategory") BudgetCategory budgetCategory,
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
}