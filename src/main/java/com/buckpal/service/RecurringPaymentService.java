package com.buckpal.service;

import com.buckpal.entity.RecurringPayment;
import com.buckpal.entity.RecurringPaymentHistory;
import com.buckpal.entity.User;
import com.buckpal.repository.RecurringPaymentRepository;
import com.buckpal.repository.RecurringPaymentHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecurringPaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecurringPaymentService.class);
    
    @Autowired
    private RecurringPaymentRepository recurringPaymentRepository;
    
    @Autowired
    private RecurringPaymentHistoryRepository historyRepository;
    
    // CRUD Operations
    public RecurringPayment createRecurringPayment(RecurringPayment payment, User user) {
        payment.setUser(user);
        
        // Set default color and icon based on payment type
        if (payment.getColor() == null) {
            payment.setColor(payment.getPaymentType().getDefaultColor());
        }
        if (payment.getIcon() == null) {
            payment.setIcon(payment.getPaymentType().getDefaultIcon());
        }
        
        RecurringPayment savedPayment = recurringPaymentRepository.save(payment);
        
        // Generate initial history entries for the next 12 months
        generateFuturePaymentHistory(savedPayment, LocalDate.now(), LocalDate.now().plusMonths(12));
        
        logger.info("Created recurring payment: {} for user: {}", savedPayment.getName(), user.getEmail());
        return savedPayment;
    }
    
    public RecurringPayment updateRecurringPayment(Long paymentId, RecurringPayment updatedPayment, User user) {
        RecurringPayment existingPayment = recurringPaymentRepository.findByIdAndUser(paymentId, user)
            .orElseThrow(() -> new RuntimeException("Recurring payment not found"));
        
        // Update fields
        existingPayment.setName(updatedPayment.getName());
        existingPayment.setDescription(updatedPayment.getDescription());
        existingPayment.setAmount(updatedPayment.getAmount());
        existingPayment.setPaymentType(updatedPayment.getPaymentType());
        existingPayment.setFrequency(updatedPayment.getFrequency());
        existingPayment.setStartDate(updatedPayment.getStartDate());
        existingPayment.setEndDate(updatedPayment.getEndDate());
        existingPayment.setRemainingPayments(updatedPayment.getRemainingPayments());
        existingPayment.setEscalationRate(updatedPayment.getEscalationRate());
        existingPayment.setColor(updatedPayment.getColor());
        existingPayment.setIcon(updatedPayment.getIcon());
        existingPayment.setCategory(updatedPayment.getCategory());
        
        RecurringPayment savedPayment = recurringPaymentRepository.save(existingPayment);
        
        // Regenerate future payment history
        regenerateFuturePaymentHistory(savedPayment);
        
        logger.info("Updated recurring payment: {} for user: {}", savedPayment.getName(), user.getEmail());
        return savedPayment;
    }
    
    public void deleteRecurringPayment(Long paymentId, User user) {
        RecurringPayment payment = recurringPaymentRepository.findByIdAndUser(paymentId, user)
            .orElseThrow(() -> new RuntimeException("Recurring payment not found"));
        
        recurringPaymentRepository.delete(payment);
        logger.info("Deleted recurring payment: {} for user: {}", payment.getName(), user.getEmail());
    }
    
    public void deactivateRecurringPayment(Long paymentId, User user) {
        RecurringPayment payment = recurringPaymentRepository.findByIdAndUser(paymentId, user)
            .orElseThrow(() -> new RuntimeException("Recurring payment not found"));
        
        payment.setIsActive(false);
        recurringPaymentRepository.save(payment);
        
        logger.info("Deactivated recurring payment: {} for user: {}", payment.getName(), user.getEmail());
    }
    
    // Query Operations
    public List<RecurringPayment> getUserRecurringPayments(User user) {
        return recurringPaymentRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public List<RecurringPayment> getActiveRecurringPayments(User user) {
        return recurringPaymentRepository.findByUserAndIsActiveTrue(user);
    }
    
    public List<RecurringPayment> getRecurringPaymentsByType(User user, RecurringPayment.PaymentType paymentType) {
        return recurringPaymentRepository.findByUserAndPaymentTypeAndIsActiveTrue(user, paymentType);
    }
    
    public Optional<RecurringPayment> getRecurringPaymentById(Long paymentId, User user) {
        return recurringPaymentRepository.findByIdAndUser(paymentId, user);
    }
    
    public Page<RecurringPayment> getRecurringPaymentsPaged(User user, Pageable pageable) {
        return recurringPaymentRepository.findByUserAndIsActiveTrueOrderByCreatedAtDesc(user, pageable);
    }
    
    // Business Logic
    public List<RecurringPayment> getActivePaymentsForPeriod(User user, LocalDate startDate, LocalDate endDate) {
        return recurringPaymentRepository.findActivePaymentsInPeriod(user, startDate, endDate);
    }
    
    public List<RecurringPayment> getPaymentsEndingSoon(User user) {
        LocalDate now = LocalDate.now();
        LocalDate threeMonthsLater = now.plusMonths(3);
        return recurringPaymentRepository.findPaymentsEndingSoon(user, now, threeMonthsLater);
    }
    
    public Map<RecurringPayment.PaymentType, BigDecimal> getMonthlyAmountsByType(User user) {
        Map<RecurringPayment.PaymentType, BigDecimal> amounts = new HashMap<>();
        
        for (RecurringPayment.PaymentType type : RecurringPayment.PaymentType.values()) {
            List<RecurringPayment> payments = getRecurringPaymentsByType(user, type);
            BigDecimal monthlyAmount = payments.stream()
                .map(this::getMonthlyEquivalentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            amounts.put(type, monthlyAmount);
        }
        
        return amounts;
    }
    
    private BigDecimal getMonthlyEquivalentAmount(RecurringPayment payment) {
        BigDecimal amount = payment.getAmount();
        
        switch (payment.getFrequency()) {
            case WEEKLY:
                return amount.multiply(new BigDecimal("4.33")); // ~4.33 weeks per month
            case MONTHLY:
                return amount;
            case BIMONTHLY:
                return amount.divide(new BigDecimal("2"), 2, BigDecimal.ROUND_HALF_UP);
            case QUARTERLY:
                return amount.divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP);
            case BIANNUAL:
                return amount.divide(new BigDecimal("6"), 2, BigDecimal.ROUND_HALF_UP);
            case ANNUAL:
                return amount.divide(new BigDecimal("12"), 2, BigDecimal.ROUND_HALF_UP);
            default:
                return amount;
        }
    }
    
    // Budget Integration
    public Map<String, Object> getBudgetProjection(User user, LocalDate startDate, int monthsAhead) {
        LocalDate endDate = startDate.plusMonths(monthsAhead);
        List<RecurringPayment> activePayments = getActivePaymentsForPeriod(user, startDate, endDate);
        
        Map<String, Object> projection = new HashMap<>();
        List<Map<String, Object>> monthlyProjections = new ArrayList<>();
        
        for (int month = 0; month < monthsAhead; month++) {
            LocalDate monthStart = startDate.plusMonths(month);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            
            Map<String, Object> monthlyData = calculateMonthlyProjection(activePayments, monthStart, monthEnd);
            monthlyData.put("month", monthStart);
            monthlyData.put("monthName", monthStart.getMonth().name());
            monthlyData.put("year", monthStart.getYear());
            
            monthlyProjections.add(monthlyData);
        }
        
        projection.put("monthlyProjections", monthlyProjections);
        projection.put("totalProjectedIncome", monthlyProjections.stream()
            .map(m -> (BigDecimal) m.get("totalIncome"))
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        projection.put("totalProjectedExpenses", monthlyProjections.stream()
            .map(m -> (BigDecimal) m.get("totalExpenses"))
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        return projection;
    }
    
    private Map<String, Object> calculateMonthlyProjection(List<RecurringPayment> payments, 
                                                         LocalDate monthStart, LocalDate monthEnd) {
        Map<String, Object> monthlyData = new HashMap<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        
        List<Map<String, Object>> paymentDetails = new ArrayList<>();
        
        for (RecurringPayment payment : payments) {
            List<LocalDate> paymentDates = payment.getPaymentDatesInPeriod(monthStart, monthEnd);
            
            if (!paymentDates.isEmpty()) {
                BigDecimal monthlyAmount = BigDecimal.ZERO;
                
                for (LocalDate date : paymentDates) {
                    BigDecimal amount = payment.getAmountForDate(date);
                    monthlyAmount = monthlyAmount.add(amount);
                }
                
                if (payment.getPaymentType() == RecurringPayment.PaymentType.INCOME) {
                    totalIncome = totalIncome.add(monthlyAmount);
                } else {
                    totalExpenses = totalExpenses.add(monthlyAmount);
                }
                
                Map<String, Object> paymentDetail = new HashMap<>();
                paymentDetail.put("paymentId", payment.getId());
                paymentDetail.put("name", payment.getName());
                paymentDetail.put("type", payment.getPaymentType());
                paymentDetail.put("amount", monthlyAmount);
                paymentDetail.put("dates", paymentDates);
                paymentDetail.put("frequency", payment.getFrequency());
                
                paymentDetails.add(paymentDetail);
            }
        }
        
        monthlyData.put("totalIncome", totalIncome);
        monthlyData.put("totalExpenses", totalExpenses);
        monthlyData.put("netAmount", totalIncome.subtract(totalExpenses));
        monthlyData.put("paymentDetails", paymentDetails);
        
        return monthlyData;
    }
    
    // History Management
    public void generateFuturePaymentHistory(RecurringPayment payment, LocalDate fromDate, LocalDate toDate) {
        List<LocalDate> paymentDates = payment.getPaymentDatesInPeriod(fromDate, toDate);
        
        for (LocalDate date : paymentDates) {
            // Check if history already exists
            if (!historyRepository.existsByRecurringPaymentAndDueDate(payment, date)) {
                BigDecimal amount = payment.getAmountForDate(date);
                RecurringPaymentHistory history = new RecurringPaymentHistory(payment, date, amount);
                historyRepository.save(history);
            }
        }
        
        logger.debug("Generated {} payment history entries for payment: {}", 
                    paymentDates.size(), payment.getName());
    }
    
    public void regenerateFuturePaymentHistory(RecurringPayment payment) {
        // Remove future planned payments
        LocalDate today = LocalDate.now();
        List<RecurringPaymentHistory> futureHistory = historyRepository.findByRecurringPaymentAndStatus(
            payment, RecurringPaymentHistory.PaymentStatus.PLANNED)
            .stream()
            .filter(h -> h.getDueDate().isAfter(today))
            .collect(Collectors.toList());
        
        historyRepository.deleteAll(futureHistory);
        
        // Regenerate for next 12 months
        generateFuturePaymentHistory(payment, today, today.plusMonths(12));
    }
    
    public List<RecurringPaymentHistory> getUpcomingPayments(User user, int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(daysAhead);
        return historyRepository.findUpcomingPayments(user, today, futureDate);
    }
    
    public List<RecurringPaymentHistory> getOverduePayments(User user) {
        return historyRepository.findOverduePayments(user, LocalDate.now());
    }
    
    // Statistics and Analytics
    public Map<String, Object> getPaymentStatistics(User user) {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic counts
        long totalActive = recurringPaymentRepository.findByUserAndIsActiveTrue(user).size();
        stats.put("totalActivePayments", totalActive);
        
        // By type
        Map<RecurringPayment.PaymentType, Long> countsByType = new HashMap<>();
        Map<RecurringPayment.PaymentType, BigDecimal> amountsByType = new HashMap<>();
        
        for (RecurringPayment.PaymentType type : RecurringPayment.PaymentType.values()) {
            long count = recurringPaymentRepository.countActivePaymentsByType(user, type);
            BigDecimal amount = recurringPaymentRepository.sumActiveAmountsByType(user, type);
            
            countsByType.put(type, count);
            amountsByType.put(type, amount);
        }
        
        stats.put("countsByType", countsByType);
        stats.put("amountsByType", amountsByType);
        stats.put("monthlyAmountsByType", getMonthlyAmountsByType(user));
        
        // Ending soon
        List<RecurringPayment> endingSoon = getPaymentsEndingSoon(user);
        stats.put("paymentsEndingSoon", endingSoon.size());
        stats.put("endingSoonDetails", endingSoon);
        
        // Overdue
        List<RecurringPaymentHistory> overdue = getOverduePayments(user);
        stats.put("overduePayments", overdue.size());
        stats.put("overdueAmount", overdue.stream()
            .map(RecurringPaymentHistory::getOutstandingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        return stats;
    }
    
    // Maintenance Operations
    @Transactional
    public void updateOverduePayments() {
        LocalDate today = LocalDate.now();
        
        // Find all planned payments that are overdue
        List<RecurringPaymentHistory> overduePayments = historyRepository
            .findAll()
            .stream()
            .filter(h -> h.getStatus() == RecurringPaymentHistory.PaymentStatus.PLANNED)
            .filter(h -> h.getDueDate().isBefore(today))
            .collect(Collectors.toList());
        
        for (RecurringPaymentHistory payment : overduePayments) {
            payment.markAsOverdue();
            historyRepository.save(payment);
        }
        
        logger.info("Updated {} overdue payments", overduePayments.size());
    }
    
    @Transactional
    public void generateMonthlyPaymentHistory() {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusMonths(2);
        
        List<RecurringPayment> allActivePayments = recurringPaymentRepository
            .findAll()
            .stream()
            .filter(RecurringPayment::getIsActive)
            .collect(Collectors.toList());
        
        for (RecurringPayment payment : allActivePayments) {
            generateFuturePaymentHistory(payment, today, futureDate);
        }
        
        logger.info("Generated payment history for {} active payments", allActivePayments.size());
    }
}