package com.buckpal.dto;

import com.buckpal.entity.IncomeTransaction.RecurrenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class IncomeTransactionDto {
    
    private Long id;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;
    
    private String notes;
    
    private String source = "Manual Entry";
    
    private String sourceReference;
    
    @NotNull(message = "Recurrence type is required")
    private RecurrenceType recurrenceType = RecurrenceType.ONE_TIME;
    
    private Boolean isRecurring = false;
    
    @NotNull(message = "Income category ID is required")
    private Long incomeCategoryId;
    
    // Additional fields for display
    private String incomeCategoryName;
    private String incomeCategoryColor;
    private String incomeCategoryIcon;
    
    private Long userId;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public IncomeTransactionDto() {}
    
    public IncomeTransactionDto(String description, BigDecimal amount, LocalDate transactionDate) {
        this.description = description;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }
    
    // Helper methods
    public boolean isFromCurrentMonth() {
        if (transactionDate == null) return false;
        LocalDate now = LocalDate.now();
        return transactionDate.getYear() == now.getYear() && 
               transactionDate.getMonth() == now.getMonth();
    }
    
    public boolean isFromMonth(int month, int year) {
        if (transactionDate == null) return false;
        return transactionDate.getYear() == year && 
               transactionDate.getMonthValue() == month;
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
    
    public Long getIncomeCategoryId() { return incomeCategoryId; }
    public void setIncomeCategoryId(Long incomeCategoryId) { this.incomeCategoryId = incomeCategoryId; }
    
    public String getIncomeCategoryName() { return incomeCategoryName; }
    public void setIncomeCategoryName(String incomeCategoryName) { this.incomeCategoryName = incomeCategoryName; }
    
    public String getIncomeCategoryColor() { return incomeCategoryColor; }
    public void setIncomeCategoryColor(String incomeCategoryColor) { this.incomeCategoryColor = incomeCategoryColor; }
    
    public String getIncomeCategoryIcon() { return incomeCategoryIcon; }
    public void setIncomeCategoryIcon(String incomeCategoryIcon) { this.incomeCategoryIcon = incomeCategoryIcon; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}