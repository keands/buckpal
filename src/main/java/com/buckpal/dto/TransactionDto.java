package com.buckpal.dto;

import com.buckpal.entity.Transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionDto {
    
    private Long id;
    private BigDecimal amount;
    private String description;
    private String merchantName;
    private LocalDate transactionDate;
    private TransactionType transactionType;
    private Boolean isPending;
    private Long accountId;
    private String accountName;
    private Long categoryId;
    private String categoryName;
    
    public TransactionDto() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    
    public Boolean getIsPending() { return isPending; }
    public void setIsPending(Boolean isPending) { this.isPending = isPending; }
    
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}