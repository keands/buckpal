package com.buckpal.dto;

import com.buckpal.entity.BudgetCategory.BudgetCategoryType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BudgetCategoryDto {
    
    private Long id;
    
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String name;
    
    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;
    
    @NotNull(message = "Allocated amount is required")
    @DecimalMin(value = "0.0", message = "Allocated amount must be positive")
    private BigDecimal allocatedAmount = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", message = "Spent amount must be positive")
    private BigDecimal spentAmount = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", message = "Percentage must be between 0 and 100")
    private BigDecimal percentage;
    
    @NotNull(message = "Category type is required")
    private BudgetCategoryType categoryType = BudgetCategoryType.EXPENSE;
    
    private String colorCode;
    
    private String iconName;
    
    private Integer sortOrder = 0;
    
    private Boolean isActive = true;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private Long budgetId;
    
    private Long parentCategoryId;
    
    private String parentCategoryName;
    
    private List<BudgetCategoryDto> subCategories;
    
    private BigDecimal remainingAmount;
    private BigDecimal usagePercentage;
    private Boolean isOverBudget;
    private Integer transactionCount;
    
    public BudgetCategoryDto() {}
    
    public BudgetCategoryDto(String name, BudgetCategoryType categoryType) {
        this.name = name;
        this.categoryType = categoryType;
    }
    
    // Computed properties
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
    
    public Boolean getIsOverBudget() {
        return spentAmount.compareTo(allocatedAmount) > 0;
    }
    
    public String getDisplayName() {
        if (parentCategoryName != null) {
            return parentCategoryName + " > " + name;
        }
        return name;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
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
    
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
    
    public Long getParentCategoryId() { return parentCategoryId; }
    public void setParentCategoryId(Long parentCategoryId) { this.parentCategoryId = parentCategoryId; }
    
    public String getParentCategoryName() { return parentCategoryName; }
    public void setParentCategoryName(String parentCategoryName) { this.parentCategoryName = parentCategoryName; }
    
    public List<BudgetCategoryDto> getSubCategories() { return subCategories; }
    public void setSubCategories(List<BudgetCategoryDto> subCategories) { this.subCategories = subCategories; }
    
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
    public void setUsagePercentage(BigDecimal usagePercentage) { this.usagePercentage = usagePercentage; }
    public void setIsOverBudget(Boolean isOverBudget) { this.isOverBudget = isOverBudget; }
    
    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }
}