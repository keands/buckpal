package com.buckpal.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DisplayName("Budget Entity Tests")
class BudgetTest {
    
    private Budget budget;
    private User user;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setPassword("password");
        
        budget = new Budget();
        budget.setUser(user);
        budget.setBudgetMonth(1);
        budget.setBudgetYear(2024);
        budget.setProjectedIncome(new BigDecimal("5000.00"));
        budget.setActualIncome(new BigDecimal("4800.00"));
        budget.setBudgetModel(Budget.BudgetModel.RULE_50_30_20);
    }
    
    @Test
    @DisplayName("Should create budget with default values")
    void shouldCreateBudgetWithDefaultValues() {
        Budget newBudget = new Budget();
        
        assertThat(newBudget.getProjectedIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(newBudget.getActualIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(newBudget.getTotalAllocatedAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(newBudget.getTotalSpentAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(newBudget.getBudgetModel()).isEqualTo(Budget.BudgetModel.CUSTOM);
        assertThat(newBudget.getIsActive()).isTrue();
    }
    
    @Test
    @DisplayName("Should create budget with constructor")
    void shouldCreateBudgetWithConstructor() {
        Budget newBudget = new Budget(user, 3, 2024, Budget.BudgetModel.RULE_60_20_20);
        
        assertThat(newBudget.getUser()).isEqualTo(user);
        assertThat(newBudget.getBudgetMonth()).isEqualTo(3);
        assertThat(newBudget.getBudgetYear()).isEqualTo(2024);
        assertThat(newBudget.getBudgetModel()).isEqualTo(Budget.BudgetModel.RULE_60_20_20);
    }
    
    @Test
    @DisplayName("Should calculate remaining income correctly")
    void shouldCalculateRemainingIncomeCorrectly() {
        budget.setTotalAllocatedAmount(new BigDecimal("3000.00"));
        
        BigDecimal remainingIncome = budget.getRemainingIncome();
        
        assertThat(remainingIncome).isEqualTo(new BigDecimal("2000.00"));
    }
    
    @Test
    @DisplayName("Should calculate negative remaining income when over-allocated")
    void shouldCalculateNegativeRemainingIncomeWhenOverAllocated() {
        budget.setTotalAllocatedAmount(new BigDecimal("6000.00"));
        
        BigDecimal remainingIncome = budget.getRemainingIncome();
        
        assertThat(remainingIncome).isEqualTo(new BigDecimal("-1000.00"));
    }
    
    @Test
    @DisplayName("Should calculate usage percentage correctly")
    void shouldCalculateUsagePercentageCorrectly() {
        budget.setTotalSpentAmount(new BigDecimal("2500.00"));
        
        BigDecimal usagePercentage = budget.getUsagePercentage();
        
        assertThat(usagePercentage).isEqualTo(new BigDecimal("50.00"));
    }
    
    @Test
    @DisplayName("Should return zero usage percentage when projected income is zero")
    void shouldReturnZeroUsagePercentageWhenProjectedIncomeIsZero() {
        budget.setProjectedIncome(BigDecimal.ZERO);
        budget.setTotalSpentAmount(new BigDecimal("100.00"));
        
        BigDecimal usagePercentage = budget.getUsagePercentage();
        
        assertThat(usagePercentage).isEqualTo(BigDecimal.ZERO);
    }
    
    @Test
    @DisplayName("Should detect over-budget correctly")
    void shouldDetectOverBudgetCorrectly() {
        budget.setTotalSpentAmount(new BigDecimal("5500.00"));
        
        assertThat(budget.isOverBudget()).isTrue();
    }
    
    @Test
    @DisplayName("Should detect within-budget correctly")
    void shouldDetectWithinBudgetCorrectly() {
        budget.setTotalSpentAmount(new BigDecimal("4000.00"));
        
        assertThat(budget.isOverBudget()).isFalse();
    }
    
    @Test
    @DisplayName("Should format budget period correctly")
    void shouldFormatBudgetPeriodCorrectly() {
        String budgetPeriod = budget.getBudgetPeriod();
        
        assertThat(budgetPeriod).isEqualTo("2024-01");
    }
    
    @Test
    @DisplayName("Should calculate income variance correctly")
    void shouldCalculateIncomeVarianceCorrectly() {
        BigDecimal variance = budget.getIncomeVariance();
        
        assertThat(variance).isEqualTo(new BigDecimal("-200.00"));
    }
    
    @Test
    @DisplayName("Should calculate positive income variance")
    void shouldCalculatePositiveIncomeVariance() {
        budget.setActualIncome(new BigDecimal("5200.00"));
        
        BigDecimal variance = budget.getIncomeVariance();
        
        assertThat(variance).isEqualTo(new BigDecimal("200.00"));
    }
    
    @Test
    @DisplayName("Should set created timestamp on persist")
    void shouldSetCreatedTimestampOnPersist() {
        LocalDateTime beforePersist = LocalDateTime.now();
        
        budget.onCreate();
        
        assertThat(budget.getCreatedAt()).isAfter(beforePersist.minusSeconds(1));
        assertThat(budget.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
    
    @Test
    @DisplayName("Should set updated timestamp on update")
    void shouldSetUpdatedTimestampOnUpdate() {
        budget.setCreatedAt(LocalDateTime.now().minusHours(1));
        LocalDateTime beforeUpdate = LocalDateTime.now();
        
        budget.onUpdate();
        
        assertThat(budget.getUpdatedAt()).isAfter(beforeUpdate.minusSeconds(1));
        assertThat(budget.getUpdatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
    
    @Test
    @DisplayName("Should validate budget model enum values")
    void shouldValidateBudgetModelEnumValues() {
        Budget.BudgetModel[] expectedModels = {
            Budget.BudgetModel.RULE_50_30_20,
            Budget.BudgetModel.RULE_60_20_20,
            Budget.BudgetModel.RULE_80_20,
            Budget.BudgetModel.ENVELOPE,
            Budget.BudgetModel.ZERO_BASED,
            Budget.BudgetModel.FRENCH_THIRDS,
            Budget.BudgetModel.CUSTOM
        };
        
        Budget.BudgetModel[] actualModels = Budget.BudgetModel.values();
        
        assertThat(actualModels).containsExactly(expectedModels);
    }
}