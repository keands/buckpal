package com.buckpal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recurring_payments")
public class RecurringPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentFrequency frequency;
    
    @Column(nullable = false)
    private LocalDate startDate;
    
    @Column
    private LocalDate endDate;
    
    @Column
    private Integer remainingPayments;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(length = 7)
    private String color = "#6366f1";
    
    @Column(length = 50)
    private String icon = "repeat";
    
    @Column
    private BigDecimal escalationRate = BigDecimal.ZERO; // Augmentation annuelle en %
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @OneToMany(mappedBy = "recurringPayment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RecurringPaymentHistory> paymentHistory = new ArrayList<>();
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime updatedAt;
    
    // Enum pour le type de paiement
    public enum PaymentType {
        INCOME("Revenu", "Revenus récurrents comme salaires, pensions", "#22c55e", "trending-up"),
        EXPENSE("Dépense", "Dépenses récurrentes comme crédits, abonnements", "#ef4444", "trending-down"),
        CREDIT("Crédit", "Remboursements de crédit avec capital et intérêts", "#f59e0b", "credit-card"),
        SUBSCRIPTION("Abonnement", "Abonnements et services récurrents", "#8b5cf6", "refresh-cw");
        
        private final String displayName;
        private final String description;
        private final String defaultColor;
        private final String defaultIcon;
        
        PaymentType(String displayName, String description, String defaultColor, String defaultIcon) {
            this.displayName = displayName;
            this.description = description;
            this.defaultColor = defaultColor;
            this.defaultIcon = defaultIcon;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getDefaultColor() { return defaultColor; }
        public String getDefaultIcon() { return defaultIcon; }
    }
    
    // Enum pour la fréquence
    public enum PaymentFrequency {
        MONTHLY(1, "Mensuel", "Tous les mois"),
        BIMONTHLY(2, "Bimestriel", "Tous les 2 mois"),
        QUARTERLY(3, "Trimestriel", "Tous les 3 mois"),
        BIANNUAL(6, "Semestriel", "Tous les 6 mois"),
        ANNUAL(12, "Annuel", "Une fois par an"),
        WEEKLY(0, "Hebdomadaire", "Toutes les semaines"); // 0 = special case
        
        private final int monthsInterval;
        private final String displayName;
        private final String description;
        
        PaymentFrequency(int monthsInterval, String displayName, String description) {
            this.monthsInterval = monthsInterval;
            this.displayName = displayName;
            this.description = description;
        }
        
        public int getMonthsInterval() { return monthsInterval; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        
        public boolean isWeekly() { return this == WEEKLY; }
    }
    
    // Constructors
    public RecurringPayment() {}
    
    public RecurringPayment(String name, BigDecimal amount, PaymentType paymentType, 
                          PaymentFrequency frequency, LocalDate startDate, User user) {
        this.name = name;
        this.amount = amount;
        this.paymentType = paymentType;
        this.frequency = frequency;
        this.startDate = startDate;
        this.user = user;
    }
    
    // Business methods
    public boolean isActiveAt(LocalDate date) {
        if (!isActive) return false;
        if (date.isBefore(startDate)) return false;
        if (endDate != null && date.isAfter(endDate)) return false;
        return true;
    }
    
    public LocalDate getNextPaymentDate(LocalDate fromDate) {
        if (!isActiveAt(fromDate)) return null;
        
        LocalDate nextDate = startDate;
        
        if (frequency.isWeekly()) {
            // Calculate weekly payments
            while (nextDate.isBefore(fromDate) || nextDate.equals(fromDate)) {
                nextDate = nextDate.plusWeeks(1);
            }
        } else {
            // Calculate monthly-based payments
            while (nextDate.isBefore(fromDate) || nextDate.equals(fromDate)) {
                nextDate = nextDate.plusMonths(frequency.getMonthsInterval());
            }
        }
        
        if (endDate != null && nextDate.isAfter(endDate)) return null;
        return nextDate;
    }
    
    public List<LocalDate> getPaymentDatesInPeriod(LocalDate startPeriod, LocalDate endPeriod) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        // Avancer jusqu'au début de la période
        while (currentDate.isBefore(startPeriod)) {
            if (frequency.isWeekly()) {
                currentDate = currentDate.plusWeeks(1);
            } else {
                currentDate = currentDate.plusMonths(frequency.getMonthsInterval());
            }
        }
        
        // Collecter les dates dans la période
        while (!currentDate.isAfter(endPeriod)) {
            if (endDate == null || !currentDate.isAfter(endDate)) {
                if (isActiveAt(currentDate)) {
                    dates.add(currentDate);
                }
            }
            
            if (frequency.isWeekly()) {
                currentDate = currentDate.plusWeeks(1);
            } else {
                currentDate = currentDate.plusMonths(frequency.getMonthsInterval());
            }
        }
        
        return dates;
    }
    
    public BigDecimal getAmountForDate(LocalDate date) {
        if (!isActiveAt(date)) return BigDecimal.ZERO;
        
        // Apply escalation if applicable
        if (escalationRate.compareTo(BigDecimal.ZERO) > 0) {
            long yearsDiff = startDate.until(date).getYears();
            if (yearsDiff > 0) {
                BigDecimal escalationMultiplier = BigDecimal.ONE.add(
                    escalationRate.divide(new BigDecimal("100"))
                ).pow((int) yearsDiff);
                return amount.multiply(escalationMultiplier);
            }
        }
        
        return amount;
    }
    
    public int getTotalRemainingPayments(LocalDate fromDate) {
        if (remainingPayments != null) {
            return Math.max(0, remainingPayments);
        }
        
        if (endDate == null) return Integer.MAX_VALUE; // Illimité
        
        return getPaymentDatesInPeriod(fromDate, endDate).size();
    }
    
    public BigDecimal getTotalRemainingAmount(LocalDate fromDate) {
        List<LocalDate> futureDates = getPaymentDatesInPeriod(fromDate, 
            endDate != null ? endDate : fromDate.plusYears(10));
        
        return futureDates.stream()
            .map(this::getAmountForDate)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    
    public PaymentFrequency getFrequency() { return frequency; }
    public void setFrequency(PaymentFrequency frequency) { this.frequency = frequency; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public Integer getRemainingPayments() { return remainingPayments; }
    public void setRemainingPayments(Integer remainingPayments) { this.remainingPayments = remainingPayments; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public BigDecimal getEscalationRate() { return escalationRate; }
    public void setEscalationRate(BigDecimal escalationRate) { this.escalationRate = escalationRate; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public List<RecurringPaymentHistory> getPaymentHistory() { return paymentHistory; }
    public void setPaymentHistory(List<RecurringPaymentHistory> paymentHistory) { this.paymentHistory = paymentHistory; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}