package com.buckpal.repository;

import com.buckpal.entity.RecurringPayment;
import com.buckpal.entity.RecurringPaymentHistory;
import com.buckpal.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringPaymentHistoryRepository extends JpaRepository<RecurringPaymentHistory, Long> {
    
    // Basic queries
    List<RecurringPaymentHistory> findByRecurringPaymentOrderByDueDateDesc(RecurringPayment recurringPayment);
    
    List<RecurringPaymentHistory> findByRecurringPaymentAndStatus(RecurringPayment recurringPayment, 
                                                                 RecurringPaymentHistory.PaymentStatus status);
    
    // History for user's payments
    @Query("SELECT rph FROM RecurringPaymentHistory rph " +
           "JOIN rph.recurringPayment rp " +
           "WHERE rp.user = :user " +
           "ORDER BY rph.dueDate DESC")
    List<RecurringPaymentHistory> findByUserOrderByDueDateDesc(@Param("user") User user);
    
    // History in date range
    @Query("SELECT rph FROM RecurringPaymentHistory rph " +
           "JOIN rph.recurringPayment rp " +
           "WHERE rp.user = :user " +
           "AND rph.dueDate BETWEEN :startDate AND :endDate " +
           "ORDER BY rph.dueDate DESC")
    List<RecurringPaymentHistory> findByUserAndDateRange(@Param("user") User user, 
                                                         @Param("startDate") LocalDate startDate, 
                                                         @Param("endDate") LocalDate endDate);
    
    // Overdue payments
    @Query("SELECT rph FROM RecurringPaymentHistory rph " +
           "JOIN rph.recurringPayment rp " +
           "WHERE rp.user = :user " +
           "AND rph.status IN ('PLANNED', 'OVERDUE') " +
           "AND rph.dueDate < :currentDate " +
           "ORDER BY rph.dueDate ASC")
    List<RecurringPaymentHistory> findOverduePayments(@Param("user") User user, 
                                                     @Param("currentDate") LocalDate currentDate);
    
    // Upcoming payments (next 30 days)
    @Query("SELECT rph FROM RecurringPaymentHistory rph " +
           "JOIN rph.recurringPayment rp " +
           "WHERE rp.user = :user " +
           "AND rph.status = 'PLANNED' " +
           "AND rph.dueDate BETWEEN :currentDate AND :futureDate " +
           "ORDER BY rph.dueDate ASC")
    List<RecurringPaymentHistory> findUpcomingPayments(@Param("user") User user, 
                                                      @Param("currentDate") LocalDate currentDate, 
                                                      @Param("futureDate") LocalDate futureDate);
    
    // Monthly statistics
    @Query("SELECT " +
           "YEAR(rph.dueDate) as year, " +
           "MONTH(rph.dueDate) as month, " +
           "rp.paymentType as paymentType, " +
           "COUNT(rph) as count, " +
           "COALESCE(SUM(rph.plannedAmount), 0) as plannedTotal, " +
           "COALESCE(SUM(rph.actualAmount), 0) as actualTotal " +
           "FROM RecurringPaymentHistory rph " +
           "JOIN rph.recurringPayment rp " +
           "WHERE rp.user = :user " +
           "AND rph.dueDate BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(rph.dueDate), MONTH(rph.dueDate), rp.paymentType " +
           "ORDER BY year, month, paymentType")
    List<Object[]> getMonthlyStatistics(@Param("user") User user, 
                                       @Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);
    
    // Payment status statistics
    @Query("SELECT " +
           "rph.status as status, " +
           "COUNT(rph) as count, " +
           "COALESCE(SUM(rph.plannedAmount), 0) as plannedTotal, " +
           "COALESCE(SUM(rph.actualAmount), 0) as actualTotal " +
           "FROM RecurringPaymentHistory rph " +
           "JOIN rph.recurringPayment rp " +
           "WHERE rp.user = :user " +
           "AND rph.dueDate BETWEEN :startDate AND :endDate " +
           "GROUP BY rph.status " +
           "ORDER BY status")
    List<Object[]> getPaymentStatusStatistics(@Param("user") User user, 
                                             @Param("startDate") LocalDate startDate, 
                                             @Param("endDate") LocalDate endDate);
    
    // Paginated queries
    @Query("SELECT rph FROM RecurringPaymentHistory rph " +
           "JOIN rph.recurringPayment rp " +
           "WHERE rp.user = :user " +
           "ORDER BY rph.dueDate DESC")
    Page<RecurringPaymentHistory> findByUserOrderByDueDateDesc(@Param("user") User user, Pageable pageable);
    
    // Check if history exists for a specific payment and date
    boolean existsByRecurringPaymentAndDueDate(RecurringPayment recurringPayment, LocalDate dueDate);
    
    // Find by payment and due date
    List<RecurringPaymentHistory> findByRecurringPaymentAndDueDate(RecurringPayment recurringPayment, LocalDate dueDate);
}