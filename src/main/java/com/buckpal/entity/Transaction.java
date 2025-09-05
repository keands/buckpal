package com.buckpal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "plaid_transaction_id", unique = true)
    private String plaidTransactionId;
    
    @NotNull
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Size(max = 200)
    private String description;
    
    @Column(name = "merchant_name")
    private String merchantName;
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;
    
    @Column(name = "is_pending", nullable = false)
    private Boolean isPending = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnore
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "transaction_project_categories",
        joinColumns = @JoinColumn(name = "transaction_id"),
        inverseJoinColumns = @JoinColumn(name = "project_category_id")
    )
    @JsonIgnore
    private Set<ProjectCategory> projectCategories = new HashSet<>();
    
    
    // Income category assignment (for income tracking)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_category_id")
    private IncomeCategory incomeCategory;
    
    // Assignment status for hybrid approach
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status")
    private AssignmentStatus assignmentStatus = AssignmentStatus.UNASSIGNED;
    
    // New fields for intelligent assignment with category mapping system
    @Column(name = "detailed_category_id")
    private Long detailedCategoryId;
    
    @Column(name = "assignment_confidence", precision = 3, scale = 2)
    private BigDecimal assignmentConfidence;
    
    @Column(name = "needs_review")
    private Boolean needsReview = false;
    
    public enum AssignmentStatus {
        UNASSIGNED,     // Not assigned to any budget category
        AUTO_ASSIGNED,  // Automatically assigned based on category matching
        MANUALLY_ASSIGNED, // Manually assigned by user
        NEEDS_REVIEW,   // Requires user review
        RECENTLY_ASSIGNED // Recently assigned, available for revision
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Transaction() {}
    
    public Transaction(BigDecimal amount, String description, LocalDate transactionDate, 
                      TransactionType transactionType, Account account) {
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate;
        this.transactionType = transactionType;
        this.account = account;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPlaidTransactionId() { return plaidTransactionId; }
    public void setPlaidTransactionId(String plaidTransactionId) { this.plaidTransactionId = plaidTransactionId; }
    
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
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public Set<ProjectCategory> getProjectCategories() { return projectCategories; }
    public void setProjectCategories(Set<ProjectCategory> projectCategories) { this.projectCategories = projectCategories; }
    
    
    public IncomeCategory getIncomeCategory() { return incomeCategory; }
    public void setIncomeCategory(IncomeCategory incomeCategory) { this.incomeCategory = incomeCategory; }
    
    public AssignmentStatus getAssignmentStatus() { return assignmentStatus; }
    public void setAssignmentStatus(AssignmentStatus assignmentStatus) { this.assignmentStatus = assignmentStatus; }
    
    // New getters and setters for intelligent assignment
    public Long getDetailedCategoryId() { return detailedCategoryId; }
    public void setDetailedCategoryId(Long detailedCategoryId) { this.detailedCategoryId = detailedCategoryId; }
    
    public BigDecimal getAssignmentConfidence() { return assignmentConfidence; }
    public void setAssignmentConfidence(BigDecimal assignmentConfidence) { this.assignmentConfidence = assignmentConfidence; }
    
    public Boolean getNeedsReview() { return needsReview; }
    public void setNeedsReview(Boolean needsReview) { this.needsReview = needsReview; }
    
    public enum TransactionType {
        INCOME,
        EXPENSE,
        TRANSFER
    }
}