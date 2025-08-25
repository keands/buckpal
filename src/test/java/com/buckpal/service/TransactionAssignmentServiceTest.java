package com.buckpal.service;

import com.buckpal.entity.Budget;
import com.buckpal.entity.BudgetCategory;
import com.buckpal.entity.Category;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.BudgetCategoryRepository;
import com.buckpal.repository.BudgetRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionAssignmentServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private BudgetCategoryRepository budgetCategoryRepository;
    
    @Mock
    private BudgetRepository budgetRepository;
    
    @Mock
    private BudgetService budgetService;
    
    @InjectMocks
    private TransactionAssignmentService transactionAssignmentService;
    
    private User testUser;
    private Transaction testTransaction;
    private BudgetCategory testBudgetCategory;
    private Category testCategory;
    private Budget testBudget;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("categories.groceries"); // Use translation key
        
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setAmount(new BigDecimal("50.00"));
        testTransaction.setDescription("Grocery shopping");
        testTransaction.setTransactionDate(LocalDate.now());
        testTransaction.setTransactionType(Transaction.TransactionType.EXPENSE);
        testTransaction.setCategory(testCategory);
        testTransaction.setAssignmentStatus(Transaction.AssignmentStatus.UNASSIGNED);
        
        testBudgetCategory = new BudgetCategory();
        testBudgetCategory.setId(1L);
        testBudgetCategory.setName("budgetCategories.groceries"); // Use budget translation key
        testBudgetCategory.setAllocatedAmount(new BigDecimal("300.00"));
        testBudgetCategory.setSpentAmount(BigDecimal.ZERO);
        
        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setBudgetMonth(LocalDate.now().getMonthValue()); // Current month
        testBudget.setBudgetYear(LocalDate.now().getYear()); // Current year
        testBudget.setUser(testUser);
    }
    
    @Test
    void autoAssignTransactions_ShouldAssignMatchingTransactions() {
        // Given
        Long budgetId = 1L;
        List<Transaction> unassignedTransactions = Arrays.asList(testTransaction);
        
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));
        when(transactionRepository.findByUserAndAssignmentStatus(testUser, Transaction.AssignmentStatus.UNASSIGNED))
            .thenReturn(unassignedTransactions);
        when(budgetCategoryRepository.findByBudgetIdAndName(budgetId, "budgetCategories.groceries"))
            .thenReturn(Optional.of(testBudgetCategory));
        when(transactionRepository.save(any(Transaction.class)))
            .thenReturn(testTransaction);
        
        // When
        transactionAssignmentService.autoAssignTransactions(testUser, budgetId);
        
        // Then
        verify(transactionRepository).save(argThat(transaction -> 
            transaction.getBudgetCategory().equals(testBudgetCategory) &&
            transaction.getAssignmentStatus() == Transaction.AssignmentStatus.AUTO_ASSIGNED
        ));
        verify(budgetService).updateBudgetAfterTransactionAssignment(testBudgetCategory.getId());
    }
    
    @Test
    void autoAssignTransactions_ShouldMarkAsNeedsReview_WhenNoMatchFound() {
        // Given
        Long budgetId = 1L;
        List<Transaction> unassignedTransactions = Arrays.asList(testTransaction);
        
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));
        when(transactionRepository.findByUserAndAssignmentStatus(testUser, Transaction.AssignmentStatus.UNASSIGNED))
            .thenReturn(unassignedTransactions);
        when(budgetCategoryRepository.findByBudgetIdAndName(budgetId, "budgetCategories.groceries"))
            .thenReturn(Optional.empty()); // No matching budget category
        
        // When
        transactionAssignmentService.autoAssignTransactions(testUser, budgetId);
        
        // Then
        verify(transactionRepository).save(argThat(transaction -> 
            transaction.getAssignmentStatus() == Transaction.AssignmentStatus.NEEDS_REVIEW
        ));
        verify(budgetService, never()).updateBudgetAfterTransactionAssignment(any());
    }
    
    @Test
    void manuallyAssignTransaction_ShouldAssignTransactionToBudgetCategory() {
        // Given
        Long transactionId = 1L;
        Long budgetCategoryId = 1L;
        
        when(transactionRepository.findByIdAndUser(transactionId, testUser))
            .thenReturn(Optional.of(testTransaction));
        when(budgetCategoryRepository.findById(budgetCategoryId))
            .thenReturn(Optional.of(testBudgetCategory));
        when(transactionRepository.save(any(Transaction.class)))
            .thenReturn(testTransaction);
        
        // When
        transactionAssignmentService.manuallyAssignTransaction(testUser, transactionId, budgetCategoryId);
        
        // Then
        verify(transactionRepository).save(argThat(transaction -> 
            transaction.getBudgetCategory().equals(testBudgetCategory) &&
            transaction.getAssignmentStatus() == Transaction.AssignmentStatus.MANUALLY_ASSIGNED
        ));
        verify(budgetService).updateBudgetAfterTransactionAssignment(budgetCategoryId);
    }
    
    @Test
    void manuallyAssignTransaction_ShouldThrowException_WhenTransactionNotFound() {
        // Given
        Long transactionId = 999L;
        Long budgetCategoryId = 1L;
        
        when(transactionRepository.findByIdAndUser(transactionId, testUser))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(RuntimeException.class, () -> 
            transactionAssignmentService.manuallyAssignTransaction(testUser, transactionId, budgetCategoryId)
        );
        
        verify(transactionRepository, never()).save(any());
        verify(budgetService, never()).updateBudgetAfterTransactionAssignment(any());
    }
    
    @Test
    void overrideAssignment_ShouldUpdateBothOldAndNewBudgetCategories() {
        // Given
        Long transactionId = 1L;
        Long newBudgetCategoryId = 2L;
        
        BudgetCategory oldBudgetCategory = new BudgetCategory();
        oldBudgetCategory.setId(1L);
        oldBudgetCategory.setName("Old Category");
        
        BudgetCategory newBudgetCategory = new BudgetCategory();
        newBudgetCategory.setId(2L);
        newBudgetCategory.setName("New Category");
        
        testTransaction.setBudgetCategory(oldBudgetCategory);
        
        when(transactionRepository.findByIdAndUser(transactionId, testUser))
            .thenReturn(Optional.of(testTransaction));
        when(budgetCategoryRepository.findById(newBudgetCategoryId))
            .thenReturn(Optional.of(newBudgetCategory));
        when(transactionRepository.save(any(Transaction.class)))
            .thenReturn(testTransaction);
        
        // When
        transactionAssignmentService.overrideAssignment(testUser, transactionId, newBudgetCategoryId);
        
        // Then
        verify(transactionRepository).save(argThat(transaction -> 
            transaction.getBudgetCategory().equals(newBudgetCategory) &&
            transaction.getAssignmentStatus() == Transaction.AssignmentStatus.MANUALLY_ASSIGNED
        ));
        verify(budgetService).updateBudgetAfterTransactionAssignment(oldBudgetCategory.getId());
        verify(budgetService).updateBudgetAfterTransactionAssignment(newBudgetCategoryId);
    }
    
    @Test
    void getTransactionsNeedingReview_ShouldReturnCorrectTransactions() {
        // Given
        List<Transaction> reviewTransactions = Arrays.asList(testTransaction);
        when(transactionRepository.findByUserAndAssignmentStatus(testUser, Transaction.AssignmentStatus.NEEDS_REVIEW))
            .thenReturn(reviewTransactions);
        
        // When
        List<Transaction> result = transactionAssignmentService.getTransactionsNeedingReview(testUser);
        
        // Then
        assertEquals(reviewTransactions, result);
        verify(transactionRepository).findByUserAndAssignmentStatus(testUser, Transaction.AssignmentStatus.NEEDS_REVIEW);
    }
    
    @Test
    void getUnassignedTransactions_ShouldReturnCorrectTransactions() {
        // Given
        List<Transaction> unassignedTransactions = Arrays.asList(testTransaction);
        when(transactionRepository.findByUserAndAssignmentStatus(testUser, Transaction.AssignmentStatus.UNASSIGNED))
            .thenReturn(unassignedTransactions);
        
        // When
        List<Transaction> result = transactionAssignmentService.getUnassignedTransactions(testUser);
        
        // Then
        assertEquals(unassignedTransactions, result);
        verify(transactionRepository).findByUserAndAssignmentStatus(testUser, Transaction.AssignmentStatus.UNASSIGNED);
    }
}