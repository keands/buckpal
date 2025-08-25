package com.buckpal.service;

import com.buckpal.entity.Budget;
import com.buckpal.entity.BudgetCategory;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.BudgetRepository;
import com.buckpal.repository.BudgetCategoryRepository;
import com.buckpal.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
    
    @Mock
    private BudgetRepository budgetRepository;
    
    @Mock
    private BudgetCategoryRepository budgetCategoryRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private CategoryInitializationService categoryInitializationService;
    
    @InjectMocks
    private BudgetService budgetService;
    
    private User testUser;
    private Budget testBudget;
    private BudgetCategory testBudgetCategory;
    private Transaction testTransaction;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        
        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setBudgetMonth(12);
        testBudget.setBudgetYear(2024);
        testBudget.setProjectedIncome(new BigDecimal("5000.00"));
        testBudget.setTotalSpentAmount(new BigDecimal("1200.00"));
        testBudget.setUser(testUser);
        
        testBudgetCategory = new BudgetCategory();
        testBudgetCategory.setId(1L);
        testBudgetCategory.setName("Groceries");
        testBudgetCategory.setAllocatedAmount(new BigDecimal("500.00"));
        testBudgetCategory.setSpentAmount(new BigDecimal("300.00"));
        testBudgetCategory.setBudget(testBudget);
        
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setAmount(new BigDecimal("50.00"));
        testTransaction.setDescription("Grocery shopping");
        testTransaction.setTransactionDate(LocalDate.now());
        testTransaction.setTransactionType(Transaction.TransactionType.EXPENSE);
        testTransaction.setBudgetCategory(testBudgetCategory);
        
        // Set up bidirectional relationships
        Set<Transaction> transactions = new HashSet<>();
        transactions.add(testTransaction);
        testBudgetCategory.setTransactions(transactions);
        
        Set<BudgetCategory> categories = new HashSet<>();
        categories.add(testBudgetCategory);
        testBudget.setBudgetCategories(categories);
    }
    
    @Test
    void recalculateBudgetCategorySpentAmount_ShouldUpdateCategoryAndBudget() {
        // Given
        Long budgetCategoryId = 1L;
        
        when(budgetCategoryRepository.findById(budgetCategoryId))
            .thenReturn(Optional.of(testBudgetCategory));
        when(budgetCategoryRepository.save(any(BudgetCategory.class)))
            .thenReturn(testBudgetCategory);
        when(budgetRepository.save(any(Budget.class)))
            .thenReturn(testBudget);
        
        // When
        budgetService.recalculateBudgetCategorySpentAmount(budgetCategoryId);
        
        // Then
        verify(budgetCategoryRepository).save(argThat(category -> 
            category.getSpentAmount().equals(new BigDecimal("50.00")) // Amount from test transaction
        ));
        verify(budgetRepository).save(any(Budget.class));
    }
    
    @Test
    void recalculateBudgetCategorySpentAmount_ShouldThrowException_WhenCategoryNotFound() {
        // Given
        Long budgetCategoryId = 999L;
        
        when(budgetCategoryRepository.findById(budgetCategoryId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(RuntimeException.class, () -> 
            budgetService.recalculateBudgetCategorySpentAmount(budgetCategoryId)
        );
        
        verify(budgetCategoryRepository, never()).save(any());
        verify(budgetRepository, never()).save(any());
    }
    
    @Test
    void recalculateBudgetTotalSpentAmount_ShouldUpdateBudgetTotal() {
        // Given
        BudgetCategory category2 = new BudgetCategory();
        category2.setId(2L);
        category2.setSpentAmount(new BigDecimal("200.00"));
        
        Set<BudgetCategory> categories = new HashSet<>();
        categories.add(testBudgetCategory); // 300.00
        categories.add(category2);          // 200.00
        testBudget.setBudgetCategories(categories);
        
        when(budgetRepository.save(any(Budget.class)))
            .thenReturn(testBudget);
        
        // When
        budgetService.recalculateBudgetTotalSpentAmount(testBudget);
        
        // Then
        verify(budgetRepository).save(argThat(budget -> 
            budget.getTotalSpentAmount().equals(new BigDecimal("500.00")) // 300 + 200
        ));
    }
    
    @Test
    void recalculateAllBudgetCategories_ShouldRecalculateAllCategories() {
        // Given
        Long budgetId = 1L;
        
        when(budgetRepository.findById(budgetId))
            .thenReturn(Optional.of(testBudget));
        when(budgetCategoryRepository.save(any(BudgetCategory.class)))
            .thenReturn(testBudgetCategory);
        when(budgetRepository.save(any(Budget.class)))
            .thenReturn(testBudget);
        
        // When
        budgetService.recalculateAllBudgetCategories(budgetId);
        
        // Then
        verify(budgetCategoryRepository, times(testBudget.getBudgetCategories().size())).save(any(BudgetCategory.class));
        verify(budgetRepository).save(any(Budget.class));
    }
    
    @Test
    void recalculateAllBudgetCategories_ShouldThrowException_WhenBudgetNotFound() {
        // Given
        Long budgetId = 999L;
        
        when(budgetRepository.findById(budgetId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(RuntimeException.class, () -> 
            budgetService.recalculateAllBudgetCategories(budgetId)
        );
        
        verify(budgetCategoryRepository, never()).save(any());
        verify(budgetRepository, never()).save(any());
    }
    
    @Test
    void updateBudgetAfterTransactionAssignment_ShouldRecalculateCategorySpentAmount() {
        // Given
        Long budgetCategoryId = 1L;
        
        when(budgetCategoryRepository.findById(budgetCategoryId))
            .thenReturn(Optional.of(testBudgetCategory));
        when(budgetCategoryRepository.save(any(BudgetCategory.class)))
            .thenReturn(testBudgetCategory);
        when(budgetRepository.save(any(Budget.class)))
            .thenReturn(testBudget);
        
        // When
        budgetService.updateBudgetAfterTransactionAssignment(budgetCategoryId);
        
        // Then
        verify(budgetCategoryRepository).save(any(BudgetCategory.class));
        verify(budgetRepository).save(any(Budget.class));
    }
    
    @Test
    void getBudgetCategoryTemplates_ShouldReturnTemplatesFromInitializationService() {
        // Given
        var mockTemplates = Arrays.asList(
            new CategoryInitializationService.BudgetCategoryTemplate(
                "Housing", "Rent and utilities", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, 
                "#ef4444", "🏠", 30.0
            )
        );
        
        when(categoryInitializationService.getPredefinedBudgetCategoryTemplates())
            .thenReturn(mockTemplates);
        
        // When
        var result = budgetService.getBudgetCategoryTemplates();
        
        // Then
        assertEquals(mockTemplates, result);
        verify(categoryInitializationService).getPredefinedBudgetCategoryTemplates();
    }
}