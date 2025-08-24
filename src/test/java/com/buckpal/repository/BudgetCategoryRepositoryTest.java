package com.buckpal.repository;

import com.buckpal.entity.Budget;
import com.buckpal.entity.BudgetCategory;
import com.buckpal.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BudgetCategoryRepository Integration Tests")
class BudgetCategoryRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private BudgetCategoryRepository budgetCategoryRepository;
    
    private User testUser;
    private Budget testBudget;
    private BudgetCategory housingCategory;
    private BudgetCategory rentCategory;
    private BudgetCategory utilitiesCategory;
    private BudgetCategory foodCategory;
    private BudgetCategory incomeCategory;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedpassword");
        entityManager.persistAndFlush(testUser);
        
        testBudget = new Budget(testUser, 1, 2024, Budget.BudgetModel.RULE_50_30_20);
        entityManager.persistAndFlush(testBudget);
        
        // Parent category
        housingCategory = new BudgetCategory("Housing", testBudget, BudgetCategory.BudgetCategoryType.EXPENSE);
        housingCategory.setAllocatedAmount(new BigDecimal("2000.00"));
        housingCategory.setSpentAmount(new BigDecimal("1800.00"));
        housingCategory.setSortOrder(1);
        entityManager.persistAndFlush(housingCategory);
        
        // Child categories
        rentCategory = new BudgetCategory("Rent", testBudget, BudgetCategory.BudgetCategoryType.EXPENSE);
        rentCategory.setParentCategory(housingCategory);
        rentCategory.setAllocatedAmount(new BigDecimal("1500.00"));
        rentCategory.setSpentAmount(new BigDecimal("1500.00"));
        rentCategory.setSortOrder(1);
        entityManager.persistAndFlush(rentCategory);
        
        utilitiesCategory = new BudgetCategory("Utilities", testBudget, BudgetCategory.BudgetCategoryType.EXPENSE);
        utilitiesCategory.setParentCategory(housingCategory);
        utilitiesCategory.setAllocatedAmount(new BigDecimal("300.00"));
        utilitiesCategory.setSpentAmount(new BigDecimal("250.00"));
        utilitiesCategory.setSortOrder(2);
        entityManager.persistAndFlush(utilitiesCategory);
        
        // Standalone category
        foodCategory = new BudgetCategory("Food", testBudget, BudgetCategory.BudgetCategoryType.EXPENSE);
        foodCategory.setAllocatedAmount(new BigDecimal("800.00"));
        foodCategory.setSpentAmount(new BigDecimal("900.00")); // Over budget
        foodCategory.setSortOrder(2);
        entityManager.persistAndFlush(foodCategory);
        
        // Income category
        incomeCategory = new BudgetCategory("Salary", testBudget, BudgetCategory.BudgetCategoryType.INCOME);
        incomeCategory.setAllocatedAmount(new BigDecimal("5000.00"));
        incomeCategory.setSpentAmount(new BigDecimal("5000.00"));
        incomeCategory.setSortOrder(0);
        entityManager.persistAndFlush(incomeCategory);
    }
    
    @Test
    @DisplayName("Should find categories by budget ordered by sort order and name")
    void shouldFindCategoriesByBudgetOrderedBySortOrderAndName() {
        List<BudgetCategory> categories = budgetCategoryRepository.findByBudgetOrderBySortOrderAscNameAsc(testBudget);
        
        assertThat(categories).hasSize(5);
        assertThat(categories.get(0).getName()).isEqualTo("Salary"); // Sort order 0
        assertThat(categories.get(1).getName()).isEqualTo("Housing"); // Sort order 1
        assertThat(categories.get(2).getName()).isEqualTo("Rent"); // Sort order 1
    }
    
    @Test
    @DisplayName("Should find only active categories")
    void shouldFindOnlyActiveCategories() {
        foodCategory.setIsActive(false);
        entityManager.persistAndFlush(foodCategory);
        
        List<BudgetCategory> activeCategories = budgetCategoryRepository.findByBudgetAndIsActiveTrueOrderBySortOrderAscNameAsc(testBudget);
        
        assertThat(activeCategories).hasSize(4);
        assertThat(activeCategories).extracting(BudgetCategory::getName)
            .doesNotContain("Food");
    }
    
    @Test
    @DisplayName("Should find only parent categories")
    void shouldFindOnlyParentCategories() {
        List<BudgetCategory> parentCategories = budgetCategoryRepository.findByBudgetAndParentCategoryIsNullOrderBySortOrderAscNameAsc(testBudget);
        
        assertThat(parentCategories).hasSize(3);
        assertThat(parentCategories).extracting(BudgetCategory::getName)
            .containsExactly("Salary", "Housing", "Food");
    }
    
    @Test
    @DisplayName("Should find active parent categories only")
    void shouldFindActiveParentCategoriesOnly() {
        foodCategory.setIsActive(false);
        entityManager.persistAndFlush(foodCategory);
        
        List<BudgetCategory> activeParentCategories = budgetCategoryRepository.findByBudgetAndParentCategoryIsNullAndIsActiveTrueOrderBySortOrderAscNameAsc(testBudget);
        
        assertThat(activeParentCategories).hasSize(2);
        assertThat(activeParentCategories).extracting(BudgetCategory::getName)
            .containsExactly("Salary", "Housing");
    }
    
    @Test
    @DisplayName("Should find subcategories by parent category")
    void shouldFindSubcategoriesByParentCategory() {
        List<BudgetCategory> subCategories = budgetCategoryRepository.findByParentCategoryOrderBySortOrderAscNameAsc(housingCategory);
        
        assertThat(subCategories).hasSize(2);
        assertThat(subCategories).extracting(BudgetCategory::getName)
            .containsExactly("Rent", "Utilities");
    }
    
    @Test
    @DisplayName("Should find categories by budget and category type")
    void shouldFindCategoriesByBudgetAndCategoryType() {
        List<BudgetCategory> expenseCategories = budgetCategoryRepository.findByBudgetAndCategoryTypeOrderBySortOrderAscNameAsc(
            testBudget, BudgetCategory.BudgetCategoryType.EXPENSE
        );
        
        assertThat(expenseCategories).hasSize(4);
        assertThat(expenseCategories).allMatch(cat -> 
            cat.getCategoryType() == BudgetCategory.BudgetCategoryType.EXPENSE
        );
    }
    
    @Test
    @DisplayName("Should find category by budget and name")
    void shouldFindCategoryByBudgetAndName() {
        Optional<BudgetCategory> category = budgetCategoryRepository.findByBudgetAndName(testBudget, "Housing");
        
        assertThat(category).isPresent();
        assertThat(category.get().getAllocatedAmount()).isEqualTo(new BigDecimal("2000.00"));
    }
    
    @Test
    @DisplayName("Should find over-budget categories")
    void shouldFindOverBudgetCategories() {
        List<BudgetCategory> overBudgetCategories = budgetCategoryRepository.findOverBudgetCategories(testBudget);
        
        assertThat(overBudgetCategories).hasSize(1);
        assertThat(overBudgetCategories.get(0).getName()).isEqualTo("Food");
        assertThat(overBudgetCategories.get(0).getSpentAmount()).isGreaterThan(overBudgetCategories.get(0).getAllocatedAmount());
    }
    
    @Test
    @DisplayName("Should find categories near budget limit")
    void shouldFindCategoriesNearBudgetLimit() {
        BigDecimal threshold = new BigDecimal("0.90"); // 90% threshold
        
        List<BudgetCategory> nearLimitCategories = budgetCategoryRepository.findCategoriesNearBudgetLimit(testBudget, threshold);
        
        assertThat(nearLimitCategories).hasSize(4); // Housing (90%), Rent (100%), Food (112.5%), Salary (100%)
        assertThat(nearLimitCategories).extracting(BudgetCategory::getName)
            .contains("Rent", "Food", "Housing", "Salary");
    }
    
    @Test
    @DisplayName("Should sum allocated amount by category type")
    void shouldSumAllocatedAmountByCategoryType() {
        BigDecimal totalExpenseAllocated = budgetCategoryRepository.sumAllocatedAmountByCategoryType(
            testBudget, BudgetCategory.BudgetCategoryType.EXPENSE
        );
        
        assertThat(totalExpenseAllocated).isEqualTo(new BigDecimal("4600.00")); // 2000 + 1500 + 300 + 800 (all expense categories)
    }
    
    @Test
    @DisplayName("Should sum spent amount by category type")
    void shouldSumSpentAmountByCategoryType() {
        BigDecimal totalExpenseSpent = budgetCategoryRepository.sumSpentAmountByCategoryType(
            testBudget, BudgetCategory.BudgetCategoryType.EXPENSE
        );
        
        assertThat(totalExpenseSpent).isEqualTo(new BigDecimal("4450.00")); // 1800 + 1500 + 250 + 900 (all expense categories)
    }
    
    @Test
    @DisplayName("Should count subcategories")
    void shouldCountSubcategories() {
        Long subcategoryCount = budgetCategoryRepository.countByParentCategory(housingCategory);
        
        assertThat(subcategoryCount).isEqualTo(2L);
    }
    
    @Test
    @DisplayName("Should check if category has linked transactions")
    void shouldCheckIfCategoryHasLinkedTransactions() {
        // This test assumes Transaction entity is properly set up
        boolean hasTransactions = budgetCategoryRepository.hasLinkedTransactions(foodCategory);
        
        assertThat(hasTransactions).isFalse(); // No transactions linked in test setup
    }
    
    @Test
    @DisplayName("Should handle empty results when no categories match criteria")
    void shouldHandleEmptyResultsWhenNoCategoriesMatchCriteria() {
        List<BudgetCategory> savingsCategories = budgetCategoryRepository.findByBudgetAndCategoryTypeOrderBySortOrderAscNameAsc(
            testBudget, BudgetCategory.BudgetCategoryType.SAVINGS
        );
        
        assertThat(savingsCategories).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle budget categories with zero amounts")
    void shouldHandleBudgetCategoriesWithZeroAmounts() {
        BudgetCategory zeroCategory = new BudgetCategory("Zero Category", testBudget, BudgetCategory.BudgetCategoryType.EXPENSE);
        zeroCategory.setAllocatedAmount(BigDecimal.ZERO);
        zeroCategory.setSpentAmount(BigDecimal.ZERO);
        entityManager.persistAndFlush(zeroCategory);
        
        List<BudgetCategory> overBudgetCategories = budgetCategoryRepository.findOverBudgetCategories(testBudget);
        BigDecimal threshold = new BigDecimal("0.50");
        List<BudgetCategory> nearLimitCategories = budgetCategoryRepository.findCategoriesNearBudgetLimit(testBudget, threshold);
        
        assertThat(overBudgetCategories).doesNotContain(zeroCategory);
        assertThat(nearLimitCategories).doesNotContain(zeroCategory);
    }
}