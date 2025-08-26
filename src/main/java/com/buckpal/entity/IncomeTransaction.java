package com.buckpal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "income_transactions")
public class IncomeTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private LocalDate transactionDate;
    
    @Column(length = 500)
    private String notes;
    
    @Column(length = 100)
    private String source; // e.g., "Manual Entry", "Bank Import", "Payroll System"
    
    @Column(length = 100)
    private String sourceReference; // External reference ID if imported
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceType recurrenceType = RecurrenceType.ONE_TIME;
    
    @Column(nullable = false)
    private Boolean isRecurring = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_category_id", nullable = false)
    @JsonIgnore
    private IncomeCategory incomeCategory;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    // Reference to the original transaction from which this income transaction was created
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_transaction_id")
    @JsonIgnore
    private Transaction sourceTransaction;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public IncomeTransaction() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public IncomeTransaction(String description, BigDecimal amount, LocalDate transactionDate, User user) {
        this();
        this.description = description;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.user = user;
    }
    
    // Lifecycle callbacks
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Calculated fields
    public boolean isFromCurrentMonth() {
        LocalDate now = LocalDate.now();
        return transactionDate.getYear() == now.getYear() && 
               transactionDate.getMonth() == now.getMonth();
    }
    
    public boolean isFromMonth(int month, int year) {
        return transactionDate.getYear() == year && 
               transactionDate.getMonthValue() == month;
    }
    
    // Helper method for smart assignment
    public String getCleanDescription() {
        return description != null ? description.trim().toLowerCase() : "";
    }
    
    // Recurrence type enum
    public enum RecurrenceType {
        ONE_TIME("Une fois", "Transaction unique"),
        WEEKLY("Hebdomadaire", "Chaque semaine"),
        BIWEEKLY("Bimensuel", "Toutes les deux semaines"),
        MONTHLY("Mensuel", "Chaque mois"),
        QUARTERLY("Trimestriel", "Tous les trois mois"),
        YEARLY("Annuel", "Chaque année");
        
        private final String displayName;
        private final String description;
        
        RecurrenceType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
    
    public RecurrenceType getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(RecurrenceType recurrenceType) { this.recurrenceType = recurrenceType; }
    
    public Boolean getIsRecurring() { return isRecurring; }
    public void setIsRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; }
    
    public IncomeCategory getIncomeCategory() { return incomeCategory; }
    public void setIncomeCategory(IncomeCategory incomeCategory) { this.incomeCategory = incomeCategory; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Transaction getSourceTransaction() { return sourceTransaction; }
    public void setSourceTransaction(Transaction sourceTransaction) { this.sourceTransaction = sourceTransaction; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}