package com.buckpal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_payment_history")
public class RecurringPaymentHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_payment_id", nullable = false)
    @JsonIgnore
    private RecurringPayment recurringPayment;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal plannedAmount;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal actualAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Column
    private LocalDate paidDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction linkedTransaction;
    
    @Column(length = 500)
    private String notes;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime updatedAt;
    
    // Enum pour le statut du paiement
    public enum PaymentStatus {
        PLANNED("Planifié", "Paiement prévu mais pas encore effectué", "#6b7280"),
        PAID("Payé", "Paiement effectué et confirmé", "#22c55e"),
        OVERDUE("En retard", "Paiement en retard", "#ef4444"),
        CANCELLED("Annulé", "Paiement annulé", "#9ca3af"),
        SKIPPED("Ignoré", "Paiement volontairement ignoré", "#f59e0b"),
        PARTIAL("Partiel", "Paiement partiel effectué", "#f97316");
        
        private final String displayName;
        private final String description;
        private final String color;
        
        PaymentStatus(String displayName, String description, String color) {
            this.displayName = displayName;
            this.description = description;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getColor() { return color; }
    }
    
    // Constructors
    public RecurringPaymentHistory() {}
    
    public RecurringPaymentHistory(RecurringPayment recurringPayment, LocalDate dueDate, BigDecimal plannedAmount) {
        this.recurringPayment = recurringPayment;
        this.dueDate = dueDate;
        this.plannedAmount = plannedAmount;
        this.status = PaymentStatus.PLANNED;
    }
    
    // Business methods
    public boolean isOverdue() {
        return status == PaymentStatus.PLANNED && dueDate.isBefore(LocalDate.now());
    }
    
    public boolean isPaid() {
        return status == PaymentStatus.PAID || status == PaymentStatus.PARTIAL;
    }
    
    public BigDecimal getVariance() {
        if (actualAmount == null) return BigDecimal.ZERO;
        return actualAmount.subtract(plannedAmount);
    }
    
    public BigDecimal getOutstandingAmount() {
        if (actualAmount == null) return plannedAmount;
        if (status == PaymentStatus.PAID) return BigDecimal.ZERO;
        return plannedAmount.subtract(actualAmount);
    }
    
    public void markAsPaid(BigDecimal paidAmount, LocalDate paymentDate, Transaction transaction) {
        this.actualAmount = paidAmount;
        this.paidDate = paymentDate;
        this.linkedTransaction = transaction;
        this.status = paidAmount.compareTo(plannedAmount) >= 0 ? 
            PaymentStatus.PAID : PaymentStatus.PARTIAL;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsOverdue() {
        if (status == PaymentStatus.PLANNED && isOverdue()) {
            this.status = PaymentStatus.OVERDUE;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public RecurringPayment getRecurringPayment() { return recurringPayment; }
    public void setRecurringPayment(RecurringPayment recurringPayment) { this.recurringPayment = recurringPayment; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    
    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public void setPlannedAmount(BigDecimal plannedAmount) { this.plannedAmount = plannedAmount; }
    
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
    
    public Transaction getLinkedTransaction() { return linkedTransaction; }
    public void setLinkedTransaction(Transaction linkedTransaction) { this.linkedTransaction = linkedTransaction; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}