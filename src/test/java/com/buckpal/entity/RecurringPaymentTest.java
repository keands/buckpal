package com.buckpal.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

class RecurringPaymentTest {

    private RecurringPayment recurringPayment;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        
        recurringPayment = new RecurringPayment();
        recurringPayment.setName("Test Payment");
        recurringPayment.setDescription("Test Description");
        recurringPayment.setAmount(new BigDecimal("100.00"));
        recurringPayment.setPaymentType(RecurringPayment.PaymentType.EXPENSE);
        recurringPayment.setFrequency(RecurringPayment.PaymentFrequency.MONTHLY);
        recurringPayment.setStartDate(LocalDate.now());
        recurringPayment.setUser(user);
    }

    @Test
    void testRecurringPaymentCreation() {
        assertNotNull(recurringPayment);
        assertEquals("Test Payment", recurringPayment.getName());
        assertEquals("Test Description", recurringPayment.getDescription());
        assertEquals(new BigDecimal("100.00"), recurringPayment.getAmount());
        assertEquals(RecurringPayment.PaymentType.EXPENSE, recurringPayment.getPaymentType());
        assertEquals(RecurringPayment.PaymentFrequency.MONTHLY, recurringPayment.getFrequency());
        assertTrue(recurringPayment.getIsActive());
        assertNotNull(recurringPayment.getCreatedAt());
    }

    @Test
    void testPaymentDatesInPeriod() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);
        recurringPayment.setStartDate(startDate);
        
        List<LocalDate> paymentDates = recurringPayment.getPaymentDatesInPeriod(startDate, endDate);
        
        assertNotNull(paymentDates);
        assertFalse(paymentDates.isEmpty());
        // For monthly frequency, we expect 3 payments: Jan 1, Feb 1, Mar 1
        assertEquals(3, paymentDates.size());
    }

    @Test
    void testAmountForDate() {
        LocalDate testDate = LocalDate.now();
        recurringPayment.setEscalationRate(BigDecimal.ZERO);
        
        BigDecimal amount = recurringPayment.getAmountForDate(testDate);
        
        assertEquals(new BigDecimal("100.00"), amount);
    }

    @Test
    void testAmountWithEscalation() {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate testDate = LocalDate.of(2024, 1, 1); // 1 year later
        recurringPayment.setStartDate(startDate);
        recurringPayment.setEscalationRate(new BigDecimal("5.0")); // 5% annual increase
        
        BigDecimal amount = recurringPayment.getAmountForDate(testDate);
        
        // Amount should be 100 * (1 + 5%) = 105
        assertEquals(new BigDecimal("105.00"), amount);
    }

    @Test
    void testTotalRemainingAmount() {
        recurringPayment.setRemainingPayments(12);
        recurringPayment.setEscalationRate(BigDecimal.ZERO);
        
        BigDecimal totalRemaining = recurringPayment.getTotalRemainingAmount();
        
        // 12 payments of 100 = 1200
        assertEquals(new BigDecimal("1200.00"), totalRemaining);
    }

    @Test
    void testPaymentTypeProperties() {
        RecurringPayment.PaymentType income = RecurringPayment.PaymentType.INCOME;
        
        assertEquals("Revenu", income.getDisplayName());
        assertEquals("#22c55e", income.getDefaultColor());
        assertEquals("trending-up", income.getDefaultIcon());
    }

    @Test
    void testPaymentFrequencyProperties() {
        RecurringPayment.PaymentFrequency monthly = RecurringPayment.PaymentFrequency.MONTHLY;
        
        assertEquals("Mensuel", monthly.getDisplayName());
        assertEquals(1, monthly.getMonthsInterval());
        assertFalse(monthly.isWeekly());
    }

    @Test
    void testWeeklyFrequency() {
        RecurringPayment.PaymentFrequency weekly = RecurringPayment.PaymentFrequency.WEEKLY;
        
        assertTrue(weekly.isWeekly());
        assertEquals("Hebdomadaire", weekly.getDisplayName());
    }

    @Test
    void testCreatedAtTimestamp() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        RecurringPayment newPayment = new RecurringPayment();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        
        assertNotNull(newPayment.getCreatedAt());
        assertTrue(newPayment.getCreatedAt().isAfter(before));
        assertTrue(newPayment.getCreatedAt().isBefore(after));
    }

    @Test
    void testUpdatedAtField() {
        assertNull(recurringPayment.getUpdatedAt());
        
        LocalDateTime updateTime = LocalDateTime.now();
        recurringPayment.setUpdatedAt(updateTime);
        
        assertEquals(updateTime, recurringPayment.getUpdatedAt());
    }

    @Test
    void testDefaultValues() {
        RecurringPayment newPayment = new RecurringPayment();
        
        assertTrue(newPayment.getIsActive());
        assertEquals("#6366f1", newPayment.getColor());
        assertEquals("repeat", newPayment.getIcon());
        assertEquals(BigDecimal.ZERO, newPayment.getEscalationRate());
        assertNotNull(newPayment.getCreatedAt());
    }
}