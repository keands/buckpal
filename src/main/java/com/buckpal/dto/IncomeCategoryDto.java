package com.buckpal.dto;

import com.buckpal.entity.IncomeCategory.IncomeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class IncomeCategoryDto {
    
    private Long id;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Budgeted amount is required")
    private BigDecimal budgetedAmount = BigDecimal.ZERO;
    
    private BigDecimal actualAmount = BigDecimal.ZERO;
    
    private String color = "#4CAF50";
    
    private String icon = "dollar-sign";
    
    private Integer displayOrder = 0;
    
    private Boolean isDefault = false;
    
    @NotNull(message = "Income type is required")
    private IncomeType incomeType = IncomeType.OTHER;
    
    private Long budgetId;
    
    // Calculated fields
    private BigDecimal variance;
    private BigDecimal usagePercentage;
    private Boolean isOverBudget;
    private Boolean isUnderBudget;
    
    // Count of linked transactions
    private Integer linkedTransactions;
    
    // Constructors
    public IncomeCategoryDto() {}
    
    public IncomeCategoryDto(String name, String description, IncomeType incomeType) {
        this.name = name;
        this.description = description;
        this.incomeType = incomeType;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getBudgetedAmount() { return budgetedAmount; }
    public void setBudgetedAmount(BigDecimal budgetedAmount) { 
        this.budgetedAmount = budgetedAmount != null ? budgetedAmount : BigDecimal.ZERO; 
    }
    
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { 
        this.actualAmount = actualAmount != null ? actualAmount : BigDecimal.ZERO; 
    }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    
    public IncomeType getIncomeType() { return incomeType; }
    public void setIncomeType(IncomeType incomeType) { this.incomeType = incomeType; }
    
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
    
    // Calculated fields getters and setters
    public BigDecimal getVariance() { return variance; }
    public void setVariance(BigDecimal variance) { this.variance = variance; }
    
    public BigDecimal getUsagePercentage() { return usagePercentage; }
    public void setUsagePercentage(BigDecimal usagePercentage) { this.usagePercentage = usagePercentage; }
    
    public Boolean getIsOverBudget() { return isOverBudget; }
    public void setIsOverBudget(Boolean isOverBudget) { this.isOverBudget = isOverBudget; }
    
    public Boolean getIsUnderBudget() { return isUnderBudget; }
    public void setIsUnderBudget(Boolean isUnderBudget) { this.isUnderBudget = isUnderBudget; }
    
    public Integer getLinkedTransactions() { return linkedTransactions; }
    public void setLinkedTransactions(Integer linkedTransactions) { 
        this.linkedTransactions = linkedTransactions; 
    }
}