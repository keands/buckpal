package com.buckpal.service;

import com.buckpal.entity.Budget;
import com.buckpal.entity.BudgetCategory;
import com.buckpal.entity.BudgetCategoryKey;
import com.buckpal.entity.Category;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.BudgetCategoryRepository;
import com.buckpal.repository.BudgetRepository;
import com.buckpal.repository.CategoryRepository;
import com.buckpal.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionAssignmentService {
    
    private final TransactionRepository transactionRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetService budgetService;
    
    @Autowired
    public TransactionAssignmentService(
            TransactionRepository transactionRepository,
            BudgetCategoryRepository budgetCategoryRepository,
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            BudgetService budgetService) {
        this.transactionRepository = transactionRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.budgetService = budgetService;
    }

    /**
     * Manually assign transaction to budget category
     * Modern implementation using category mapping system
     */
    public void manuallyAssignTransaction(User user, Long transactionId, Long budgetCategoryId) {
        Transaction transaction = transactionRepository.findByIdAndUser(transactionId, user)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        BudgetCategory budgetCategory = budgetCategoryRepository.findById(budgetCategoryId)
            .orElseThrow(() -> new RuntimeException("Budget category not found"));
        
        // Find or create a detailed category that maps to this budget category
        Category detailedCategory = findOrCreateDetailedCategoryForBudgetCategory(budgetCategory);
        
        // Use the modern assignment approach
        transaction.setCategory(detailedCategory);
        transaction.setDetailedCategoryId(detailedCategory.getId());
        transaction.setAssignmentStatus(Transaction.AssignmentStatus.MANUALLY_ASSIGNED);
        
        transactionRepository.save(transaction);
        
        // Update budget progress using modern category mapping approach
        budgetService.recalculateBudgetSpentAmountsFromCategoryMapping(budgetCategory.getBudget());
    }
    
    /**
     * Assign a transaction directly to a detailed category
     */
    public void assignTransactionToDetailedCategory(User user, Long transactionId, Long detailedCategoryId) {
        Transaction transaction = transactionRepository.findByIdAndUser(transactionId, user)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        Category detailedCategory = categoryRepository.findById(detailedCategoryId)
            .orElseThrow(() -> new RuntimeException("Detailed category not found"));
        
        // Set the detailed category
        transaction.setCategory(detailedCategory);
        transaction.setDetailedCategoryId(detailedCategoryId);
        transaction.setAssignmentStatus(Transaction.AssignmentStatus.MANUALLY_ASSIGNED);
        
        transactionRepository.save(transaction);
        
        // Update budget progress by recalculating spent amounts via category mapping
        // Find user's current budget and trigger recalculation
        updateBudgetProgressAfterCategoryAssignment(user, transaction.getTransactionDate());
    }
    
    /**
     * Update budget progress after a transaction is assigned to a detailed category
     */
    private void updateBudgetProgressAfterCategoryAssignment(User user, java.time.LocalDate transactionDate) {
        // Find the budget for the transaction's month
        // Recalculate all budget category spent amounts using the new mapping approach
        budgetRepository.findByUserAndBudgetMonthAndBudgetYear(user, transactionDate.getMonthValue(), transactionDate.getYear())
            .ifPresent(budgetService::recalculateBudgetSpentAmountsFromCategoryMapping);
    }
    
    /**
     * Find or create a detailed category that maps to the given budget category
     * This ensures compatibility between manual assignment and the modern category mapping system
     */
    private Category findOrCreateDetailedCategoryForBudgetCategory(BudgetCategory budgetCategory) {
        BudgetCategoryKey budgetCategoryKey = budgetCategory.getCategoryKey();
        
        if (budgetCategoryKey == null) {
            // Legacy budget category without proper mapping - create a generic category
            return findOrCreateGenericCategory(budgetCategory.getName());
        }
        
        // Find existing detailed category that maps to this budget category key for this user
        Optional<Category> existingCategory = categoryRepository.findByBudgetCategoryKeyAndUser(
            budgetCategoryKey, 
            budgetCategory.getBudget().getUser()
        );
            
        if (existingCategory.isPresent()) {
            return existingCategory.get();
        }
        
        // Create a new detailed category that maps to this budget category
        Category newCategory = new Category();
        newCategory.setName(budgetCategoryKey.getI18nKey());
        newCategory.setBudgetCategoryKey(budgetCategoryKey);
        newCategory.setUser(budgetCategory.getBudget().getUser());
        
        return categoryRepository.save(newCategory);
    }
    
    /**
     * Create a generic category for legacy budget categories without proper mapping
     */
    private Category findOrCreateGenericCategory(String budgetCategoryName) {
        // Create a generic category name based on the budget category
        String genericCategoryName = "categories." + budgetCategoryName.toLowerCase().replaceAll(" ", "");
        
        // Try to find existing category with this name
        Optional<Category> existingGeneric = categoryRepository.findByName(genericCategoryName);
        if (existingGeneric.isPresent()) {
            return existingGeneric.get();
        }
        
        // Create new generic category - Note: This is a fallback for legacy data and should be rare
        Category newCategory = new Category();
        newCategory.setName(genericCategoryName);
        // No budgetCategoryKey for legacy categories
        
        return categoryRepository.save(newCategory);
    }
    
    /**
     * Override existing assignment with a new budget category
     * Modern implementation using category mapping system
     */
    public void overrideAssignment(User user, Long transactionId, Long newBudgetCategoryId) {
        Transaction transaction = transactionRepository.findByIdAndUser(transactionId, user)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        BudgetCategory newBudgetCategory = budgetCategoryRepository.findById(newBudgetCategoryId)
            .orElseThrow(() -> new RuntimeException("Budget category not found"));
        
        // Find or create a detailed category that maps to the new budget category
        Category newDetailedCategory = findOrCreateDetailedCategoryForBudgetCategory(newBudgetCategory);
        
        // Store old budget info for recalculation
        Budget oldBudget = null;
        if (transaction.getCategory() != null && transaction.getCategory().getBudgetCategoryKey() != null) {
            // Find the budget that contained the old category assignment
            oldBudget = budgetRepository.findByUserAndBudgetMonthAndBudgetYear(
                user, 
                transaction.getTransactionDate().getMonthValue(), 
                transaction.getTransactionDate().getYear()
            ).orElse(null);
        }
        
        // Use the modern assignment approach
        transaction.setCategory(newDetailedCategory);
        transaction.setDetailedCategoryId(newDetailedCategory.getId());
        transaction.setAssignmentStatus(Transaction.AssignmentStatus.MANUALLY_ASSIGNED);
        
        transactionRepository.save(transaction);
        
        // Update budget progress using modern category mapping approach
        // Recalculate both old and new budgets if they're different
        if (oldBudget != null && !oldBudget.equals(newBudgetCategory.getBudget())) {
            budgetService.recalculateBudgetSpentAmountsFromCategoryMapping(oldBudget);
        }
        budgetService.recalculateBudgetSpentAmountsFromCategoryMapping(newBudgetCategory.getBudget());
    }
    
    /**
     * Get transactions that need review (automatic assignment failed)
     */
    public List<Transaction> getTransactionsNeedingReview(User user) {
        return transactionRepository.findByUserAndAssignmentStatus(
            user, 
            Transaction.AssignmentStatus.NEEDS_REVIEW
        );
    }
    
    /**
     * Get unassigned transactions for a specific budget month
     */
    public List<Transaction> getUnassignedTransactions(User user, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new RuntimeException("Budget not found"));
            
        return getUnassignedTransactionsForMonth(user, budget.getBudgetMonth(), budget.getBudgetYear());
    }
    
    /**
     * Get unassigned transactions (all months) - kept for backward compatibility
     * Only returns EXPENSE transactions - income transactions use separate income management system
     */
    public List<Transaction> getUnassignedTransactions(User user) {
        List<Transaction> unassigned = transactionRepository.findByUserAndAssignmentStatus(
            user, 
            Transaction.AssignmentStatus.UNASSIGNED
        );
        
        return unassigned.stream()
            .filter(transaction -> transaction.getTransactionType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.toList());
    }
    
    /**
     * Get unassigned transactions for a specific month/year
     * Only returns EXPENSE transactions - income transactions use separate income management system
     */
    public List<Transaction> getUnassignedTransactionsForMonth(User user, Integer month, Integer year) {
        List<Transaction> allUnassigned = transactionRepository.findByUserAndAssignmentStatus(
            user, Transaction.AssignmentStatus.UNASSIGNED
        );
        
        return allUnassigned.stream()
            .filter(transaction -> transaction.getTransactionType() == Transaction.TransactionType.EXPENSE)
            .filter(transaction -> {
                int transactionMonth = transaction.getTransactionDate().getMonthValue();
                int transactionYear = transaction.getTransactionDate().getYear();
                return transactionMonth == month && transactionYear == year;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get transactions needing review for a specific budget month
     */
    public List<Transaction> getTransactionsNeedingReview(User user, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new RuntimeException("Budget not found"));
            
        return getTransactionsNeedingReviewForMonth(user, budget.getBudgetMonth(), budget.getBudgetYear());
    }
    
    /**
     * Get transactions needing review for a specific month/year
     * Only returns EXPENSE transactions - income transactions use separate income management system
     */
    public List<Transaction> getTransactionsNeedingReviewForMonth(User user, Integer month, Integer year) {
        List<Transaction> allNeedingReview = transactionRepository.findByUserAndAssignmentStatus(
            user, Transaction.AssignmentStatus.NEEDS_REVIEW
        );
        
        return allNeedingReview.stream()
            .filter(transaction -> transaction.getTransactionType() == Transaction.TransactionType.EXPENSE)
            .filter(transaction -> {
                int transactionMonth = transaction.getTransactionDate().getMonthValue();
                int transactionYear = transaction.getTransactionDate().getYear();
                return transactionMonth == month && transactionYear == year;
            })
            .collect(Collectors.toList());
    }
}