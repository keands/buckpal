package com.buckpal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "budgets", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "budget_month", "budget_year"}))
public class Budget {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "budget_month", nullable = false)
    private Integer budgetMonth; // 1-12
    
    @NotNull
    @Column(name = "budget_year", nullable = false)
    private Integer budgetYear;
    
    @Column(name = "projected_income", precision = 15, scale = 2)
    private BigDecimal projectedIncome = BigDecimal.ZERO;
    
    @Column(name = "actual_income", precision = 15, scale = 2)
    private BigDecimal actualIncome = BigDecimal.ZERO;
    
    @Column(name = "total_allocated_amount", precision = 15, scale = 2)
    private BigDecimal totalAllocatedAmount = BigDecimal.ZERO;
    
    @Column(name = "total_spent_amount", precision = 15, scale = 2)
    private BigDecimal totalSpentAmount = BigDecimal.ZERO;
    
    @Column(name = "notes")
    private String notes;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "budget_model", nullable = false)
    private BudgetModel budgetModel = BudgetModel.CUSTOM;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<BudgetCategory> budgetCategories = new HashSet<>();
    
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<IncomeCategory> incomeCategories = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Utility methods
    public BigDecimal getRemainingIncome() {
        return projectedIncome.subtract(totalAllocatedAmount);
    }
    
    public BigDecimal getUsagePercentage() {
        if (projectedIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalSpentAmount.divide(projectedIncome, 2, BigDecimal.ROUND_HALF_UP)
                             .multiply(new BigDecimal("100"));
    }
    
    public boolean isOverBudget() {
        return totalSpentAmount.compareTo(projectedIncome) > 0;
    }
    
    public String getBudgetPeriod() {
        return String.format("%04d-%02d", budgetYear, budgetMonth);
    }
    
    public BigDecimal getIncomeVariance() {
        return actualIncome.subtract(projectedIncome);
    }
    
    // Constructors
    public Budget() {}
    
    public Budget(User user, Integer budgetMonth, Integer budgetYear, BudgetModel budgetModel) {
        this.user = user;
        this.budgetMonth = budgetMonth;
        this.budgetYear = budgetYear;
        this.budgetModel = budgetModel;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getBudgetMonth() { return budgetMonth; }
    public void setBudgetMonth(Integer budgetMonth) { this.budgetMonth = budgetMonth; }
    
    public Integer getBudgetYear() { return budgetYear; }
    public void setBudgetYear(Integer budgetYear) { this.budgetYear = budgetYear; }
    
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
    
    public BudgetModel getBudgetModel() { return budgetModel; }
    public void setBudgetModel(BudgetModel budgetModel) { this.budgetModel = budgetModel; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Set<BudgetCategory> getBudgetCategories() { return budgetCategories; }
    public void setBudgetCategories(Set<BudgetCategory> budgetCategories) { this.budgetCategories = budgetCategories; }
    
    public Set<IncomeCategory> getIncomeCategories() { return incomeCategories; }
    public void setIncomeCategories(Set<IncomeCategory> incomeCategories) { this.incomeCategories = incomeCategories; }
    
    // Income utility methods
    public BigDecimal getTotalBudgetedIncome() {
        return incomeCategories.stream()
                .map(IncomeCategory::getBudgetedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getTotalActualIncome() {
        return incomeCategories.stream()
                .map(IncomeCategory::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getIncomeVarianceByCategories() {
        return getTotalActualIncome().subtract(getTotalBudgetedIncome());
    }
    
    // Method to add income category
    public void addIncomeCategory(IncomeCategory incomeCategory) {
        incomeCategories.add(incomeCategory);
        incomeCategory.setBudget(this);
    }
    
    public void removeIncomeCategory(IncomeCategory incomeCategory) {
        incomeCategories.remove(incomeCategory);
        incomeCategory.setBudget(null);
    }
    
    public enum BudgetModel {
        RULE_50_30_20,    // 50% needs, 30% wants, 20% savings
        RULE_60_20_20,    // 60% needs, 20% wants, 20% savings  
        RULE_80_20,       // 80% expenses, 20% savings
        ENVELOPE,         // Fixed amounts per category
        ZERO_BASED,       // Every euro assigned
        FRENCH_THIRDS,    // 1/3 housing, 1/3 living, 1/3 savings
        CUSTOM            // User-defined percentages
    }
}