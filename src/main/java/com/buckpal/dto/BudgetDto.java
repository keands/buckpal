package com.buckpal.dto;

import com.buckpal.entity.Budget.BudgetModel;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BudgetDto {
    
    private Long id;
    
    @NotNull
    @Min(value = 1, message = "Budget month must be between 1 and 12")
    @Max(value = 12, message = "Budget month must be between 1 and 12")
    private Integer budgetMonth;
    
    @NotNull
    @Min(value = 2000, message = "Budget year must be a valid year")
    private Integer budgetYear;
    
    @NotNull
    private BudgetModel budgetModel;
    
    @DecimalMin(value = "0.0", message = "Projected income must be positive")
    private BigDecimal projectedIncome = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", message = "Actual income must be positive") 
    private BigDecimal actualIncome = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", message = "Total allocated amount must be positive")
    private BigDecimal totalAllocatedAmount = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", message = "Total spent amount must be positive")
    private BigDecimal totalSpentAmount = BigDecimal.ZERO;
    
    private String notes;
    
    private Boolean isActive = true;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private List<BudgetCategoryDto> budgetCategories;
    
    private BigDecimal remainingAmount;
    private BigDecimal usagePercentage;
    private Boolean isOverBudget;
    
    public BudgetDto() {}
    
    public BudgetDto(Integer budgetMonth, Integer budgetYear, BudgetModel budgetModel) {
        this.budgetMonth = budgetMonth;
        this.budgetYear = budgetYear;
        this.budgetModel = budgetModel;
    }
    
    // Computed properties
    public BigDecimal getRemainingAmount() {
        return projectedIncome.subtract(totalAllocatedAmount);
    }
    
    public BigDecimal getUsagePercentage() {
        if (projectedIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalSpentAmount.divide(projectedIncome, 2, BigDecimal.ROUND_HALF_UP)
                             .multiply(new BigDecimal("100"));
    }
    
    public Boolean getIsOverBudget() {
        return totalSpentAmount.compareTo(projectedIncome) > 0;
    }
    
    public String getBudgetPeriod() {
        return String.format("%04d-%02d", budgetYear, budgetMonth);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getBudgetMonth() { return budgetMonth; }
    public void setBudgetMonth(Integer budgetMonth) { this.budgetMonth = budgetMonth; }
    
    public Integer getBudgetYear() { return budgetYear; }
    public void setBudgetYear(Integer budgetYear) { this.budgetYear = budgetYear; }
    
    public BudgetModel getBudgetModel() { return budgetModel; }
    public void setBudgetModel(BudgetModel budgetModel) { this.budgetModel = budgetModel; }
    
    public BigDecimal getProjectedIncome() { return projectedIncome; }
    public void setProjectedIncome(BigDecimal projectedIncome) { this.projectedIncome = projectedIncome; }
    
    public BigDecimal getActualIncome() { return actualIncome; }
    public void setActualIncome(BigDecimal actualIncome) { this.actualIncome = actualIncome; }
    
    public BigDecimal getTotalAllocatedAmount() { return totalAllocatedAmount; }
    public void setTotalAllocatedAmount(BigDecimal totalAllocatedAmount) { this.totalAllocatedAmount = totalAllocatedAmount; }
    
    public BigDecimal getTotalSpentAmount() { return totalSpentAmount; }
    public void setTotalSpentAmount(BigDecimal totalSpentAmount) { this.totalSpentAmount = totalSpentAmount; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<BudgetCategoryDto> getBudgetCategories() { return budgetCategories; }
    public void setBudgetCategories(List<BudgetCategoryDto> budgetCategories) { this.budgetCategories = budgetCategories; }
    
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
    public void setUsagePercentage(BigDecimal usagePercentage) { this.usagePercentage = usagePercentage; }
    public void setIsOverBudget(Boolean isOverBudget) { this.isOverBudget = isOverBudget; }
}