package com.buckpal.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DisplayName("BudgetCategory Entity Tests")
class BudgetCategoryTest {
    
    private BudgetCategory budgetCategory;
    private Budget budget;
    private User user;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        
        budget = new Budget();
        budget.setUser(user);
        budget.setBudgetMonth(1);
        budget.setBudgetYear(2024);
        
        budgetCategory = new BudgetCategory();
        budgetCategory.setName("Housing");
        budgetCategory.setBudget(budget);
        budgetCategory.setAllocatedAmount(new BigDecimal("1500.00"));
        budgetCategory.setSpentAmount(new BigDecimal("1200.00"));
        budgetCategory.setCategoryType(BudgetCategory.BudgetCategoryType.EXPENSE);
    }
    
    @Test
    @DisplayName("Should create budget category with default values")
    void shouldCreateBudgetCategoryWithDefaultValues() {
        BudgetCategory newCategory = new BudgetCategory();
        
        assertThat(newCategory.getAllocatedAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(newCategory.getSpentAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(newCategory.getCategoryType()).isEqualTo(BudgetCategory.BudgetCategoryType.EXPENSE);
        assertThat(newCategory.getSortOrder()).isEqualTo(0);
        assertThat(newCategory.getIsActive()).isTrue();
        assertThat(newCategory.getSubCategories()).isNotNull().isEmpty();
        assertThat(newCategory.getTransactions()).isNotNull().isEmpty();
    }
    
    @Test
    @DisplayName("Should create budget category with constructor")
    void shouldCreateBudgetCategoryWithConstructor() {
        BudgetCategory newCategory = new BudgetCategory("Food", budget, BudgetCategory.BudgetCategoryType.EXPENSE);
        
        assertThat(newCategory.getName()).isEqualTo("Food");
        assertThat(newCategory.getBudget()).isEqualTo(budget);
        assertThat(newCategory.getCategoryType()).isEqualTo(BudgetCategory.BudgetCategoryType.EXPENSE);
    }
    
    @Test
    @DisplayName("Should calculate remaining amount correctly")
    void shouldCalculateRemainingAmountCorrectly() {
        BigDecimal remainingAmount = budgetCategory.getRemainingAmount();
        
        assertThat(remainingAmount).isEqualTo(new BigDecimal("300.00"));
    }
    
    @Test
    @DisplayName("Should calculate negative remaining amount when overspent")
    void shouldCalculateNegativeRemainingAmountWhenOverspent() {
        budgetCategory.setSpentAmount(new BigDecimal("1700.00"));
        
        BigDecimal remainingAmount = budgetCategory.getRemainingAmount();
        
        assertThat(remainingAmount).isEqualTo(new BigDecimal("-200.00"));
    }
    
    @Test
    @DisplayName("Should calculate usage percentage correctly")
    void shouldCalculateUsagePercentageCorrectly() {
        BigDecimal usagePercentage = budgetCategory.getUsagePercentage();
        
        assertThat(usagePercentage).isEqualTo(new BigDecimal("80.00"));
    }
    
    @Test
    @DisplayName("Should return zero usage percentage when allocated amount is zero")
    void shouldReturnZeroUsagePercentageWhenAllocatedAmountIsZero() {
        budgetCategory.setAllocatedAmount(BigDecimal.ZERO);
        budgetCategory.setSpentAmount(new BigDecimal("100.00"));
        
        BigDecimal usagePercentage = budgetCategory.getUsagePercentage();
        
        assertThat(usagePercentage).isEqualTo(BigDecimal.ZERO);
    }
    
    @Test
    @DisplayName("Should detect over-budget correctly")
    void shouldDetectOverBudgetCorrectly() {
        budgetCategory.setSpentAmount(new BigDecimal("1600.00"));
        
        assertThat(budgetCategory.isOverBudget()).isTrue();
    }
    
    @Test
    @DisplayName("Should detect within-budget correctly")
    void shouldDetectWithinBudgetCorrectly() {
        assertThat(budgetCategory.isOverBudget()).isFalse();
    }
    
    @Test
    @DisplayName("Should handle exact budget amount")
    void shouldHandleExactBudgetAmount() {
        budgetCategory.setSpentAmount(new BigDecimal("1500.00"));
        
        assertThat(budgetCategory.isOverBudget()).isFalse();
        assertThat(budgetCategory.getRemainingAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(budgetCategory.getUsagePercentage()).isEqualTo(new BigDecimal("100.00"));
    }
    
    @Test
    @DisplayName("Should set created timestamp on persist")
    void shouldSetCreatedTimestampOnPersist() {
        LocalDateTime beforePersist = LocalDateTime.now();
        
        budgetCategory.onCreate();
        
        assertThat(budgetCategory.getCreatedAt()).isAfter(beforePersist.minusSeconds(1));
        assertThat(budgetCategory.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
    
    @Test
    @DisplayName("Should set updated timestamp on update")
    void shouldSetUpdatedTimestampOnUpdate() {
        budgetCategory.setCreatedAt(LocalDateTime.now().minusHours(1));
        LocalDateTime beforeUpdate = LocalDateTime.now();
        
        budgetCategory.onUpdate();
        
        assertThat(budgetCategory.getUpdatedAt()).isAfter(beforeUpdate.minusSeconds(1));
        assertThat(budgetCategory.getUpdatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
    
    @Test
    @DisplayName("Should support hierarchical relationships")
    void shouldSupportHierarchicalRelationships() {
        BudgetCategory parentCategory = new BudgetCategory("Housing", budget, BudgetCategory.BudgetCategoryType.EXPENSE);
        BudgetCategory subCategory = new BudgetCategory("Utilities", budget, BudgetCategory.BudgetCategoryType.EXPENSE);
        
        subCategory.setParentCategory(parentCategory);
        parentCategory.getSubCategories().add(subCategory);
        
        assertThat(subCategory.getParentCategory()).isEqualTo(parentCategory);
        assertThat(parentCategory.getSubCategories()).contains(subCategory);
    }
    
    @Test
    @DisplayName("Should validate category type enum values")
    void shouldValidateCategoryTypeEnumValues() {
        BudgetCategory.BudgetCategoryType[] expectedTypes = {
            BudgetCategory.BudgetCategoryType.INCOME,
            BudgetCategory.BudgetCategoryType.EXPENSE,
            BudgetCategory.BudgetCategoryType.SAVINGS,
            BudgetCategory.BudgetCategoryType.DEBT,
            BudgetCategory.BudgetCategoryType.PROJECT
        };
        
        BudgetCategory.BudgetCategoryType[] actualTypes = BudgetCategory.BudgetCategoryType.values();
        
        assertThat(actualTypes).containsExactly(expectedTypes);
    }
    
    @Test
    @DisplayName("Should handle percentage calculations correctly")
    void shouldHandlePercentageCalculationsCorrectly() {
        budgetCategory.setPercentage(new BigDecimal("30.0"));
        
        assertThat(budgetCategory.getPercentage()).isEqualTo(new BigDecimal("30.0"));
    }
    
    @Test
    @DisplayName("Should handle color and icon properties")
    void shouldHandleColorAndIconProperties() {
        budgetCategory.setColorCode("#FF5733");
        budgetCategory.setIconName("home");
        
        assertThat(budgetCategory.getColorCode()).isEqualTo("#FF5733");
        assertThat(budgetCategory.getIconName()).isEqualTo("home");
    }
    
    @Test
    @DisplayName("Should handle sort order for display ordering")
    void shouldHandleSortOrderForDisplayOrdering() {
        budgetCategory.setSortOrder(10);
        
        assertThat(budgetCategory.getSortOrder()).isEqualTo(10);
    }
    
    @Test
    @DisplayName("Should handle active/inactive state")
    void shouldHandleActiveInactiveState() {
        budgetCategory.setIsActive(false);
        
        assertThat(budgetCategory.getIsActive()).isFalse();
    }
}