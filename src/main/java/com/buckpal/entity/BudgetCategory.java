package com.buckpal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "budget_categories")
public class BudgetCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category_key", nullable = true) // Allow null during migration
    private BudgetCategoryKey categoryKey;
    
    @Size(max = 100)
    private String name; // Keep for backward compatibility, but derive from categoryKey
    
    @Size(max = 200)
    private String description;
    
    @Column(name = "allocated_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;
    
    @Column(name = "spent_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal spentAmount = BigDecimal.ZERO;
    
    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage; // Percentage of total income (0-100)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false)
    private BudgetCategoryType categoryType = BudgetCategoryType.EXPENSE;
    
    @Column(name = "color_code")
    private String colorCode;
    
    @Column(name = "icon_name")
    private String iconName;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    @JsonIgnore
    private Budget budget;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private BudgetCategory parentCategory;
    
    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<BudgetCategory> subCategories = new HashSet<>();
    
    @OneToMany(mappedBy = "budgetCategory", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Transaction> transactions = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Utility methods
    public BigDecimal getRemainingAmount() {
        return allocatedAmount.subtract(spentAmount);
    }
    
    public BigDecimal getUsagePercentage() {
        if (allocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.divide(allocatedAmount, 2, BigDecimal.ROUND_HALF_UP)
                         .multiply(new BigDecimal("100"));
    }
    
    public boolean isOverBudget() {
        return spentAmount.compareTo(allocatedAmount) > 0;
    }
    
    /**
     * Get detailed category distribution for this budget category
     * Groups transactions by their detailed Category and calculates amounts/percentages
     */
    @JsonIgnore
    public Map<Category, DetailedCategoryInfo> getDetailedCategoryDistribution() {
        if (transactions.isEmpty()) {
            return new HashMap<>();
        }
        
        // Group transactions by detailed category
        Map<Category, List<Transaction>> groupedTransactions = transactions.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(Transaction::getCategory));
        
        Map<Category, DetailedCategoryInfo> distribution = new HashMap<>();
        
        for (Map.Entry<Category, List<Transaction>> entry : groupedTransactions.entrySet()) {
            Category category = entry.getKey();
            List<Transaction> categoryTransactions = entry.getValue();
            
            BigDecimal totalAmount = categoryTransactions.stream()
                    .map(Transaction::getAmount)
                    .map(BigDecimal::abs) // Ensure positive amounts for display
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal percentage = spentAmount.compareTo(BigDecimal.ZERO) > 0
                    ? totalAmount.divide(spentAmount, 4, RoundingMode.HALF_UP)
                              .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            
            distribution.put(category, new DetailedCategoryInfo(
                    category.getName(),
                    totalAmount,
                    percentage,
                    categoryTransactions.size(),
                    category.getColorCode(),
                    category.getIconName()
            ));
        }
        
        return distribution;
    }
    
    /**
     * Get transactions that don't have a detailed category assigned
     */
    @JsonIgnore
    public List<Transaction> getUncategorizedTransactions() {
        return transactions.stream()
                .filter(t -> t.getCategory() == null)
                .collect(Collectors.toList());
    }
    
    /**
     * Get count of uncategorized transactions
     */
    public int getUncategorizedCount() {
        return (int) transactions.stream()
                .filter(t -> t.getCategory() == null)
                .count();
    }
    
    /**
     * Get percentage of transactions that are categorized
     */
    public BigDecimal getCategorizedPercentage() {
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        long categorizedCount = transactions.stream()
                .filter(t -> t.getCategory() != null)
                .count();
        
        return BigDecimal.valueOf(categorizedCount)
                .divide(BigDecimal.valueOf(transactions.size()), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
    
    /**
     * Inner class to hold detailed category information
     */
    public static class DetailedCategoryInfo {
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal percentage;
        private int transactionCount;
        private String colorCode;
        private String iconName;
        
        public DetailedCategoryInfo(String categoryName, BigDecimal amount, 
                                  BigDecimal percentage, int transactionCount,
                                  String colorCode, String iconName) {
            this.categoryName = categoryName;
            this.amount = amount;
            this.percentage = percentage;
            this.transactionCount = transactionCount;
            this.colorCode = colorCode;
            this.iconName = iconName;
        }
        
        // Getters
        public String getCategoryName() { return categoryName; }
        public BigDecimal getAmount() { return amount; }
        public BigDecimal getPercentage() { return percentage; }
        public int getTransactionCount() { return transactionCount; }
        public String getColorCode() { return colorCode; }
        public String getIconName() { return iconName; }
    }
    
    // Constructors
    public BudgetCategory() {}
    
    public BudgetCategory(String name, Budget budget, BudgetCategoryType categoryType) {
        this.name = name;
        this.budget = budget;
        this.categoryType = categoryType;
    }
    
    public BudgetCategory(BudgetCategoryKey categoryKey, Budget budget) {
        this.categoryKey = categoryKey;
        this.name = categoryKey.getI18nKey(); // Derive name from enum
        this.budget = budget;
        this.categoryType = categoryKey.getCategoryType();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public BudgetCategoryKey getCategoryKey() { return categoryKey; }
    public void setCategoryKey(BudgetCategoryKey categoryKey) { 
        this.categoryKey = categoryKey;
        if (categoryKey != null) {
            this.name = categoryKey.getI18nKey(); // Auto-sync name with categoryKey
            this.categoryType = categoryKey.getCategoryType(); // Auto-sync type
        }
    }
    
    public String getName() { 
        // Always return i18n key from enum if available, fallback to stored name
        return categoryKey != null ? categoryKey.getI18nKey() : name; 
    }
    
    public void setName(String name) { 
        this.name = name;
        // Try to sync with enum if the name matches an i18n key
        BudgetCategoryKey key = BudgetCategoryKey.fromI18nKey(name);
        if (key != null) {
            this.categoryKey = key;
        }
    }
    
    /**
     * Validate that custom categories don't use reserved i18n keys
     */
    public boolean isValidCustomCategoryName(String name) {
        // If no category key is set, this is a custom category
        if (this.categoryKey == null) {
            // Check if the name conflicts with any enum i18n key
            return BudgetCategoryKey.fromI18nKey(name) == null;
        }
        return true; // Standard categories are always valid
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }
    
    public BigDecimal getSpentAmount() { return spentAmount; }
    public void setSpentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; }
    
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    
    public BudgetCategoryType getCategoryType() { return categoryType; }
    public void setCategoryType(BudgetCategoryType categoryType) { this.categoryType = categoryType; }
    
    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }
    
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }
    
    public BudgetCategory getParentCategory() { return parentCategory; }
    public void setParentCategory(BudgetCategory parentCategory) { this.parentCategory = parentCategory; }
    
    public Set<BudgetCategory> getSubCategories() { return subCategories; }
    public void setSubCategories(Set<BudgetCategory> subCategories) { this.subCategories = subCategories; }
    
    public Set<Transaction> getTransactions() { return transactions; }
    public void setTransactions(Set<Transaction> transactions) { this.transactions = transactions; }
    
    public enum BudgetCategoryType {
        INCOME,     // Revenue categories
        EXPENSE,    // Spending categories  
        SAVINGS,    // Savings goals
        DEBT,       // Debt payments
        PROJECT     // Project-specific spending
    }
}