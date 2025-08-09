package com.buckpal.repository;

import com.buckpal.entity.Account;
import com.buckpal.entity.Category;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.Transaction.TransactionType;
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
    
    List<Transaction> findByAccountAndCategory(Account account, Category category);
    
    List<Transaction> findByAccountAndTransactionType(Account account, TransactionType transactionType);
    
    Optional<Transaction> findByPlaidTransactionId(String plaidTransactionId);
    
    @Query("SELECT t FROM Transaction t WHERE t.account IN :accounts ORDER BY t.transactionDate DESC")
    Page<Transaction> findByAccountsOrderByTransactionDateDesc(
        @Param("accounts") List<Account> accounts, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.account IN :accounts AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByAccountsAndDateRange(
        @Param("accounts") List<Account> accounts, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
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
}