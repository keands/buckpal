package com.buckpal.repository;

import com.buckpal.entity.RecurringPayment;
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
public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, Long> {
    
    // Basic queries
    List<RecurringPayment> findByUserAndIsActiveTrue(User user);
    
    List<RecurringPayment> findByUserOrderByCreatedAtDesc(User user);
    
    Optional<RecurringPayment> findByIdAndUser(Long id, User user);
    
    List<RecurringPayment> findByUserAndPaymentType(User user, RecurringPayment.PaymentType paymentType);
    
    List<RecurringPayment> findByUserAndPaymentTypeAndIsActiveTrue(User user, RecurringPayment.PaymentType paymentType);
    
    // Active payments for a specific period
    @Query("SELECT rp FROM RecurringPayment rp WHERE rp.user = :user " +
           "AND rp.isActive = true " +
           "AND rp.startDate <= :endDate " +
           "AND (rp.endDate IS NULL OR rp.endDate >= :startDate)")
    List<RecurringPayment> findActivePaymentsInPeriod(
            @Param("user") User user, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);
    
    // Payments ending soon (within next 3 months)
    @Query("SELECT rp FROM RecurringPayment rp WHERE rp.user = :user " +
           "AND rp.isActive = true " +
           "AND rp.endDate IS NOT NULL " +
           "AND rp.endDate BETWEEN :now AND :threeMonthsLater " +
           "ORDER BY rp.endDate ASC")
    List<RecurringPayment> findPaymentsEndingSoon(@Param("user") User user, 
                                                 @Param("now") LocalDate now, 
                                                 @Param("threeMonthsLater") LocalDate threeMonthsLater);
    
    // Sum of active recurring amounts by type
    @Query("SELECT COALESCE(SUM(rp.amount), 0) FROM RecurringPayment rp " +
           "WHERE rp.user = :user AND rp.paymentType = :paymentType AND rp.isActive = true")
    BigDecimal sumActiveAmountsByType(@Param("user") User user, 
                                     @Param("paymentType") RecurringPayment.PaymentType paymentType);
    
    // Count active payments by type
    @Query("SELECT COUNT(rp) FROM RecurringPayment rp " +
           "WHERE rp.user = :user AND rp.paymentType = :paymentType AND rp.isActive = true")
    long countActivePaymentsByType(@Param("user") User user, 
                                  @Param("paymentType") RecurringPayment.PaymentType paymentType);
    
    // Search payments
    @Query("SELECT rp FROM RecurringPayment rp WHERE rp.user = :user " +
           "AND (LOWER(rp.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(rp.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<RecurringPayment> searchPayments(@Param("user") User user, @Param("searchTerm") String searchTerm);
    
    // Paginated queries
    Page<RecurringPayment> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user, Pageable pageable);
    
    Page<RecurringPayment> findByUserAndPaymentTypeOrderByCreatedAtDesc(User user, 
                                                                       RecurringPayment.PaymentType paymentType, 
                                                                       Pageable pageable);
    
    // Statistics
    @Query("SELECT " +
           "rp.paymentType as paymentType, " +
           "COUNT(rp) as count, " +
           "COALESCE(SUM(rp.amount), 0) as totalAmount " +
           "FROM RecurringPayment rp " +
           "WHERE rp.user = :user AND rp.isActive = true " +
           "GROUP BY rp.paymentType")
    List<Object[]> getPaymentStatsByType(@Param("user") User user);
    
    // Credits specific queries
    @Query("SELECT rp FROM RecurringPayment rp WHERE rp.user = :user " +
           "AND rp.paymentType = 'CREDIT' AND rp.isActive = true " +
           "AND (rp.endDate IS NULL OR rp.endDate >= :currentDate) " +
           "ORDER BY rp.endDate ASC NULLS LAST")
    List<RecurringPayment> findActiveCredits(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    // Total remaining debt
    @Query("SELECT COALESCE(SUM(rp.amount * " +
           "CASE " +
           "  WHEN rp.remainingPayments IS NOT NULL THEN rp.remainingPayments " +
           "  WHEN rp.endDate IS NOT NULL THEN " +
           "    CASE rp.frequency " +
           "      WHEN 'MONTHLY' THEN MONTHS_BETWEEN(rp.endDate, :currentDate) " +
           "      WHEN 'QUARTERLY' THEN MONTHS_BETWEEN(rp.endDate, :currentDate) / 3 " +
           "      WHEN 'ANNUAL' THEN MONTHS_BETWEEN(rp.endDate, :currentDate) / 12 " +
           "      ELSE 1 " +
           "    END " +
           "  ELSE 0 " +
           "END), 0) " +
           "FROM RecurringPayment rp " +
           "WHERE rp.user = :user AND rp.paymentType = 'CREDIT' AND rp.isActive = true")
    BigDecimal getTotalRemainingDebt(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
}