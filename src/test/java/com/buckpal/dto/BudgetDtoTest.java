package com.buckpal.dto;

import com.buckpal.entity.Budget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BudgetDto Tests")
class BudgetDtoTest {
    
    private BudgetDto budgetDto;
    
    @BeforeEach
    void setUp() {
        budgetDto = new BudgetDto();
        budgetDto.setBudgetMonth(3);
        budgetDto.setBudgetYear(2024);
        budgetDto.setBudgetModel(Budget.BudgetModel.RULE_50_30_20);
        budgetDto.setProjectedIncome(new BigDecimal("5000.00"));
        budgetDto.setActualIncome(new BigDecimal("4800.00"));
        budgetDto.setTotalAllocatedAmount(new BigDecimal("4500.00"));
        budgetDto.setTotalSpentAmount(new BigDecimal("3200.00"));
    }
    
    @Test
    @DisplayName("Should create budget DTO with constructor")
    void shouldCreateBudgetDtoWithConstructor() {
        BudgetDto dto = new BudgetDto(6, 2024, Budget.BudgetModel.RULE_60_20_20);
        
        assertThat(dto.getBudgetMonth()).isEqualTo(6);
        assertThat(dto.getBudgetYear()).isEqualTo(2024);
        assertThat(dto.getBudgetModel()).isEqualTo(Budget.BudgetModel.RULE_60_20_20);
    }
    
    @Test
    @DisplayName("Should calculate remaining amount correctly")
    void shouldCalculateRemainingAmountCorrectly() {
        BigDecimal remainingAmount = budgetDto.getRemainingAmount();
        
        assertThat(remainingAmount).isEqualTo(new BigDecimal("500.00"));
    }
    
    @Test
    @DisplayName("Should calculate negative remaining amount when over-allocated")
    void shouldCalculateNegativeRemainingAmountWhenOverAllocated() {
        budgetDto.setTotalAllocatedAmount(new BigDecimal("6000.00"));
        
        BigDecimal remainingAmount = budgetDto.getRemainingAmount();
        
        assertThat(remainingAmount).isEqualTo(new BigDecimal("-1000.00"));
    }
    
    @Test
    @DisplayName("Should calculate usage percentage correctly")
    void shouldCalculateUsagePercentageCorrectly() {
        BigDecimal usagePercentage = budgetDto.getUsagePercentage();
        
        assertThat(usagePercentage).isEqualTo(new BigDecimal("64.00"));
    }
    
    @Test
    @DisplayName("Should return zero usage percentage when projected income is zero")
    void shouldReturnZeroUsagePercentageWhenProjectedIncomeIsZero() {
        budgetDto.setProjectedIncome(BigDecimal.ZERO);
        budgetDto.setTotalSpentAmount(new BigDecimal("1000.00"));
        
        BigDecimal usagePercentage = budgetDto.getUsagePercentage();
        
        assertThat(usagePercentage).isEqualTo(BigDecimal.ZERO);
    }
    
    @Test
    @DisplayName("Should detect over-budget correctly")
    void shouldDetectOverBudgetCorrectly() {
        budgetDto.setTotalSpentAmount(new BigDecimal("6000.00"));
        
        Boolean isOverBudget = budgetDto.getIsOverBudget();
        
        assertThat(isOverBudget).isTrue();
    }
    
    @Test
    @DisplayName("Should detect within-budget correctly")
    void shouldDetectWithinBudgetCorrectly() {
        Boolean isOverBudget = budgetDto.getIsOverBudget();
        
        assertThat(isOverBudget).isFalse();
    }
    
    @Test
    @DisplayName("Should format budget period correctly")
    void shouldFormatBudgetPeriodCorrectly() {
        String budgetPeriod = budgetDto.getBudgetPeriod();
        
        assertThat(budgetPeriod).isEqualTo("2024-03");
    }
    
    @Test
    @DisplayName("Should handle single-digit months in budget period")
    void shouldHandleSingleDigitMonthsInBudgetPeriod() {
        budgetDto.setBudgetMonth(9);
        
        String budgetPeriod = budgetDto.getBudgetPeriod();
        
        assertThat(budgetPeriod).isEqualTo("2024-09");
    }
    
    @Test
    @DisplayName("Should handle default values correctly")
    void shouldHandleDefaultValuesCorrectly() {
        BudgetDto newDto = new BudgetDto();
        
        assertThat(newDto.getProjectedIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(newDto.getActualIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(newDto.getTotalAllocatedAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(newDto.getTotalSpentAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(newDto.getIsActive()).isTrue();
        assertThat(newDto.getRemainingAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(newDto.getUsagePercentage()).isEqualTo(BigDecimal.ZERO);
        assertThat(newDto.getIsOverBudget()).isFalse();
    }
    
    @Test
    @DisplayName("Should handle timestamp fields correctly")
    void shouldHandleTimestampFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        budgetDto.setCreatedAt(now);
        budgetDto.setUpdatedAt(now.plusHours(1));
        
        assertThat(budgetDto.getCreatedAt()).isEqualTo(now);
        assertThat(budgetDto.getUpdatedAt()).isAfter(budgetDto.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should handle notes field")
    void shouldHandleNotesField() {
        String notes = "This is a test budget for March 2024";
        budgetDto.setNotes(notes);
        
        assertThat(budgetDto.getNotes()).isEqualTo(notes);
    }
    
    @Test
    @DisplayName("Should handle budget categories list")
    void shouldHandleBudgetCategoriesList() {
        BudgetCategoryDto category1 = new BudgetCategoryDto();
        category1.setName("Housing");
        
        BudgetCategoryDto category2 = new BudgetCategoryDto();
        category2.setName("Food");
        
        budgetDto.setBudgetCategories(java.util.List.of(category1, category2));
        
        assertThat(budgetDto.getBudgetCategories()).hasSize(2);
        assertThat(budgetDto.getBudgetCategories()).extracting("name")
            .containsExactly("Housing", "Food");
    }
    
    @Test
    @DisplayName("Should set computed properties manually for serialization")
    void shouldSetComputedPropertiesManuallyForSerialization() {
        budgetDto.setRemainingAmount(new BigDecimal("123.45"));
        budgetDto.setUsagePercentage(new BigDecimal("67.89"));
        budgetDto.setIsOverBudget(true);
        
        // These are typically set by service layer for API responses
        assertThat(budgetDto.getRemainingAmount()).isEqualTo(new BigDecimal("123.45"));
        assertThat(budgetDto.getUsagePercentage()).isEqualTo(new BigDecimal("67.89"));
        assertThat(budgetDto.getIsOverBudget()).isTrue();
    }
}