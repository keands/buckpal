package com.buckpal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "income_categories")
public class IncomeCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal budgetedAmount = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal actualAmount = BigDecimal.ZERO;
    
    @Column(length = 7)
    private String color = "#4CAF50"; // Default green color
    
    @Column(length = 50)
    private String icon = "dollar-sign"; // Default icon
    
    @Column(nullable = false)
    private Integer displayOrder = 0;
    
    @Column(nullable = false)
    private Boolean isDefault = false;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncomeType incomeType = IncomeType.OTHER;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id")
    @JsonIgnore
    private Budget budget;
    
    @OneToMany(mappedBy = "incomeCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IncomeTransaction> incomeTransactions = new ArrayList<>();
    
    // Constructors
    public IncomeCategory() {}
    
    public IncomeCategory(String name, String description, IncomeType incomeType) {
        this.name = name;
        this.description = description;
        this.incomeType = incomeType;
    }
    
    // Calculated fields
    public BigDecimal getVariance() {
        return actualAmount.subtract(budgetedAmount);
    }
    
    public BigDecimal getUsagePercentage() {
        if (budgetedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return actualAmount.divide(budgetedAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }
    
    public boolean isOverBudget() {
        return actualAmount.compareTo(budgetedAmount) > 0;
    }
    
    public boolean isUnderBudget() {
        return actualAmount.compareTo(budgetedAmount) < 0;
    }
    
    // Method to add income transaction and update actual amount
    public void addIncomeTransaction(IncomeTransaction transaction) {
        incomeTransactions.add(transaction);
        transaction.setIncomeCategory(this);
        updateActualAmount();
    }
    
    public void removeIncomeTransaction(IncomeTransaction transaction) {
        incomeTransactions.remove(transaction);
        transaction.setIncomeCategory(null);
        updateActualAmount();
    }
    
    private void updateActualAmount() {
        this.actualAmount = incomeTransactions.stream()
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Income type enum
    public enum IncomeType {
        SALARY("Salaire", "Revenus salariaux", "#2E7D32", "briefcase"),
        BUSINESS("Entreprise", "Revenus d'entreprise", "#1976D2", "building"),
        INVESTMENT("Investissement", "Revenus d'investissement", "#7B1FA2", "trending-up"),
        OTHER("Autre", "Autres revenus", "#616161", "dollar-sign");
        
        private final String displayName;
        private final String description;
        private final String defaultColor;
        private final String defaultIcon;
        
        IncomeType(String displayName, String description, String defaultColor, String defaultIcon) {
            this.displayName = displayName;
            this.description = description;
            this.defaultColor = defaultColor;
            this.defaultIcon = defaultIcon;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getDefaultColor() { return defaultColor; }
        public String getDefaultIcon() { return defaultIcon; }
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
    
    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }
    
    public List<IncomeTransaction> getIncomeTransactions() { return incomeTransactions; }
    public void setIncomeTransactions(List<IncomeTransaction> incomeTransactions) { 
        this.incomeTransactions = incomeTransactions; 
    }
}