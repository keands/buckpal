package com.buckpal.repository;

import com.buckpal.entity.Budget;
import com.buckpal.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BudgetRepository Integration Tests")
class BudgetRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    private User testUser;
    private Budget january2024Budget;
    private Budget february2024Budget;
    private Budget march2024Budget;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedpassword");
        entityManager.persistAndFlush(testUser);
        
        january2024Budget = new Budget(testUser, 1, 2024, Budget.BudgetModel.RULE_50_30_20);
        january2024Budget.setProjectedIncome(new BigDecimal("5000.00"));
        january2024Budget.setIsActive(true);
        
        february2024Budget = new Budget(testUser, 2, 2024, Budget.BudgetModel.RULE_60_20_20);
        february2024Budget.setProjectedIncome(new BigDecimal("5200.00"));
        february2024Budget.setIsActive(true);
        
        march2024Budget = new Budget(testUser, 3, 2024, Budget.BudgetModel.CUSTOM);
        march2024Budget.setProjectedIncome(new BigDecimal("4800.00"));
        march2024Budget.setIsActive(false);
        
        entityManager.persistAndFlush(january2024Budget);
        entityManager.persistAndFlush(february2024Budget);
        entityManager.persistAndFlush(march2024Budget);
    }
    
    @Test
    @DisplayName("Should find budgets by user ordered by year and month descending")
    void shouldFindBudgetsByUserOrderedByYearAndMonthDesc() {
        List<Budget> budgets = budgetRepository.findByUserOrderByBudgetYearDescBudgetMonthDesc(testUser);
        
        assertThat(budgets).hasSize(3);
        assertThat(budgets.get(0).getBudgetMonth()).isEqualTo(3);
        assertThat(budgets.get(1).getBudgetMonth()).isEqualTo(2);
        assertThat(budgets.get(2).getBudgetMonth()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should find only active budgets by user")
    void shouldFindOnlyActiveBudgetsByUser() {
        List<Budget> activeBudgets = budgetRepository.findByUserAndIsActiveTrue(testUser);
        
        assertThat(activeBudgets).hasSize(2);
        assertThat(activeBudgets).extracting(Budget::getBudgetMonth).containsExactly(1, 2);
    }
    
    @Test
    @DisplayName("Should find budget by user, month, and year")
    void shouldFindBudgetByUserMonthAndYear() {
        Optional<Budget> budget = budgetRepository.findByUserAndBudgetMonthAndBudgetYear(testUser, 2, 2024);
        
        assertThat(budget).isPresent();
        assertThat(budget.get().getBudgetModel()).isEqualTo(Budget.BudgetModel.RULE_60_20_20);
        assertThat(budget.get().getProjectedIncome()).isEqualTo(new BigDecimal("5200.00"));
    }
    
    @Test
    @DisplayName("Should return empty when budget not found")
    void shouldReturnEmptyWhenBudgetNotFound() {
        Optional<Budget> budget = budgetRepository.findByUserAndBudgetMonthAndBudgetYear(testUser, 12, 2024);
        
        assertThat(budget).isEmpty();
    }
    
    @Test
    @DisplayName("Should find active budget by user, month, and year")
    void shouldFindActiveBudgetByUserMonthAndYear() {
        Optional<Budget> activeBudget = budgetRepository.findByUserAndBudgetMonthAndBudgetYearAndIsActiveTrue(testUser, 1, 2024);
        
        assertThat(activeBudget).isPresent();
        assertThat(activeBudget.get().getIsActive()).isTrue();
    }
    
    @Test
    @DisplayName("Should return empty for inactive budget when searching for active only")
    void shouldReturnEmptyForInactiveBudgetWhenSearchingForActiveOnly() {
        Optional<Budget> activeBudget = budgetRepository.findByUserAndBudgetMonthAndBudgetYearAndIsActiveTrue(testUser, 3, 2024);
        
        assertThat(activeBudget).isEmpty();
    }
    
    @Test
    @DisplayName("Should find budgets by user and year")
    void shouldFindBudgetsByUserAndYear() {
        List<Budget> budgets2024 = budgetRepository.findByUserAndBudgetYear(testUser, 2024);
        
        assertThat(budgets2024).hasSize(3);
        assertThat(budgets2024).allMatch(budget -> budget.getBudgetYear().equals(2024));
    }
    
    @Test
    @DisplayName("Should find budgets by user and date range within same year")
    void shouldFindBudgetsByUserAndDateRangeWithinSameYear() {
        List<Budget> budgets = budgetRepository.findByUserAndDateRange(testUser, 2024, 1, 2);
        
        assertThat(budgets).hasSize(2);
        assertThat(budgets).extracting(Budget::getBudgetMonth).containsExactly(2, 1);
    }
    
    @Test
    @DisplayName("Should find budgets by user and date range across years")
    void shouldFindBudgetsByUserAndDateRangeAcrossYears() {
        Budget december2023Budget = new Budget(testUser, 12, 2023, Budget.BudgetModel.CUSTOM);
        entityManager.persistAndFlush(december2023Budget);
        
        Budget january2025Budget = new Budget(testUser, 1, 2025, Budget.BudgetModel.ENVELOPE);
        entityManager.persistAndFlush(january2025Budget);
        
        List<Budget> budgets = budgetRepository.findByUserAndDateRangeAcrossYears(
            testUser, 2023, 12, 2024, 2
        );
        
        assertThat(budgets).hasSize(3);
        assertThat(budgets.get(0).getBudgetYear()).isEqualTo(2024);
        assertThat(budgets.get(0).getBudgetMonth()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should find current month budget using default method")
    void shouldFindCurrentMonthBudgetUsingDefaultMethod() {
        LocalDate now = LocalDate.now();
        Budget currentMonthBudget = new Budget(testUser, now.getMonthValue(), now.getYear(), Budget.BudgetModel.CUSTOM);
        currentMonthBudget.setIsActive(true);
        entityManager.persistAndFlush(currentMonthBudget);
        
        Optional<Budget> foundBudget = budgetRepository.findCurrentMonthBudget(testUser);
        
        assertThat(foundBudget).isPresent();
        assertThat(foundBudget.get().getBudgetMonth()).isEqualTo(now.getMonthValue());
        assertThat(foundBudget.get().getBudgetYear()).isEqualTo(now.getYear());
    }
    
    @Test
    @DisplayName("Should find previous month budget using default method")
    void shouldFindPreviousMonthBudgetUsingDefaultMethod() {
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        Budget previousMonthBudget = new Budget(testUser, previousMonth.getMonthValue(), previousMonth.getYear(), Budget.BudgetModel.FRENCH_THIRDS);
        entityManager.persistAndFlush(previousMonthBudget);
        
        Optional<Budget> foundBudget = budgetRepository.findPreviousMonthBudget(testUser);
        
        assertThat(foundBudget).isPresent();
        assertThat(foundBudget.get().getBudgetMonth()).isEqualTo(previousMonth.getMonthValue());
        assertThat(foundBudget.get().getBudgetYear()).isEqualTo(previousMonth.getYear());
    }
    
    @Test
    @DisplayName("Should handle empty results for user with no budgets")
    void shouldHandleEmptyResultsForUserWithNoBudgets() {
        User newUser = new User();
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setEmail("new@example.com");
        newUser.setPassword("hashedpassword");
        entityManager.persistAndFlush(newUser);
        
        List<Budget> budgets = budgetRepository.findByUserOrderByBudgetYearDescBudgetMonthDesc(newUser);
        Optional<Budget> currentBudget = budgetRepository.findCurrentMonthBudget(newUser);
        
        assertThat(budgets).isEmpty();
        assertThat(currentBudget).isEmpty();
    }
    
    @Test
    @DisplayName("Should enforce unique constraint on user, month, and year")
    void shouldEnforceUniqueConstraintOnUserMonthAndYear() {
        Budget duplicateBudget = new Budget(testUser, 1, 2024, Budget.BudgetModel.ZERO_BASED);
        
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicateBudget);
        }).isInstanceOf(Exception.class);
    }
}