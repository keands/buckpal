package com.buckpal.service;

import com.buckpal.entity.Budget;
import com.buckpal.entity.BudgetCategory;
import com.buckpal.entity.Category;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.BudgetCategoryRepository;
import com.buckpal.repository.BudgetRepository;
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
    private final BudgetService budgetService;
    
    @Autowired
    public TransactionAssignmentService(
            TransactionRepository transactionRepository,
            BudgetCategoryRepository budgetCategoryRepository,
            BudgetRepository budgetRepository,
            BudgetService budgetService) {
        this.transactionRepository = transactionRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.budgetRepository = budgetRepository;
        this.budgetService = budgetService;
    }
    
    /**
     * Hybrid approach: Auto-assign transactions to budget categories based on existing categories
     * Only considers transactions from the same month/year as the budget
     */
    public void autoAssignTransactions(User user, Long budgetId) {
        // Get the budget to know which month/year we're working with
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        // Get all unassigned transactions for the user
        List<Transaction> allUnassignedTransactions = transactionRepository
            .findByUserAndAssignmentStatus(user, Transaction.AssignmentStatus.UNASSIGNED);
        
        // Filter to only transactions from the budget's month/year
        List<Transaction> monthlyUnassignedTransactions = allUnassignedTransactions.stream()
            .filter(transaction -> {
                int transactionMonth = transaction.getTransactionDate().getMonthValue();
                int transactionYear = transaction.getTransactionDate().getYear();
                return transactionMonth == budget.getBudgetMonth() && 
                       transactionYear == budget.getBudgetYear();
            })
            .collect(Collectors.toList());
        
        for (Transaction transaction : monthlyUnassignedTransactions) {
            Optional<BudgetCategory> matchingBudgetCategory = findMatchingBudgetCategory(transaction, budgetId);
            
            if (matchingBudgetCategory.isPresent()) {
                BudgetCategory budgetCategory = matchingBudgetCategory.get();
                transaction.setBudgetCategory(budgetCategory);
                transaction.setAssignmentStatus(Transaction.AssignmentStatus.AUTO_ASSIGNED);
                transactionRepository.save(transaction);
                
                // Update budget amounts in real-time
                budgetService.updateBudgetAfterTransactionAssignment(budgetCategory.getId());
            } else {
                // Mark for manual review if no automatic match found
                transaction.setAssignmentStatus(Transaction.AssignmentStatus.NEEDS_REVIEW);
                transactionRepository.save(transaction);
            }
        }
    }
    
    /**
     * Find matching budget category based on transaction's existing category
     */
    private Optional<BudgetCategory> findMatchingBudgetCategory(Transaction transaction, Long budgetId) {
        if (transaction.getCategory() == null) {
            return Optional.empty();
        }
        
        Category transactionCategory = transaction.getCategory();
        String categoryKey = transactionCategory.getName();
        
        // Map transaction category keys to budget category keys
        String budgetCategoryKey = mapTransactionCategoryToBudgetCategory(categoryKey);
        if (budgetCategoryKey == null) {
            return Optional.empty();
        }
        
        // Find budget category with the mapped key
        return budgetCategoryRepository.findByBudgetIdAndName(budgetId, budgetCategoryKey);
    }
    
    /**
     * Map transaction category translation keys to budget category translation keys
     */
    private String mapTransactionCategoryToBudgetCategory(String transactionCategoryKey) {
        // Map from categories.* to budgetCategories.*
        switch (transactionCategoryKey) {
            // Income categories -> typically not assigned to budget categories in spending tracking
            case "categories.salary":
            case "categories.freelance":
            case "categories.investmentIncome":
            case "categories.otherIncome":
                return null; // Income typically doesn't go into budget expense categories
                
            // Essential expenses
            case "categories.housing":
                return "budgetCategories.housing";
            case "categories.utilities":
                return "budgetCategories.utilities";
            case "categories.groceries":
                return "budgetCategories.groceries";
            case "categories.transportation":
                return "budgetCategories.transportation";
            case "categories.insurance":
                return "budgetCategories.insurance";
            case "categories.healthcare":
                return "budgetCategories.healthcare";
            case "categories.debtPayments":
                return "budgetCategories.debtPayments";
                
            // Lifestyle expenses
            case "categories.diningOut":
                return "budgetCategories.diningOut";
            case "categories.entertainment":
                return "budgetCategories.entertainment";
            case "categories.shopping":
                return "budgetCategories.shopping";
            case "categories.personalCare":
                return "budgetCategories.personalCare";
            case "categories.hobbies":
                return "budgetCategories.hobbies";
            case "categories.travel":
                return "budgetCategories.travel";
                
            // Financial
            case "categories.savings":
                return "budgetCategories.emergencyFund"; // Map to emergency fund as primary savings
            case "categories.investments":
                return "budgetCategories.investments";
                
            // Other
            case "categories.education":
                return "budgetCategories.education";
            case "categories.giftsDonations":
                return "budgetCategories.giftsDonations";
            case "categories.businessExpenses":
                return "budgetCategories.businessExpenses";
            case "categories.miscellaneous":
                return "budgetCategories.miscellaneous";
                
            default:
                return null; // No mapping available
        }
    }
    
    /**
     * Manually assign transaction to budget category
     */
    public void manuallyAssignTransaction(User user, Long transactionId, Long budgetCategoryId) {
        Transaction transaction = transactionRepository.findByIdAndUser(transactionId, user)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        BudgetCategory budgetCategory = budgetCategoryRepository.findById(budgetCategoryId)
            .orElseThrow(() -> new RuntimeException("Budget category not found"));
        
        transaction.setBudgetCategory(budgetCategory);
        transaction.setAssignmentStatus(Transaction.AssignmentStatus.MANUALLY_ASSIGNED);
        transactionRepository.save(transaction);
        
        // Update budget amounts in real-time
        budgetService.updateBudgetAfterTransactionAssignment(budgetCategoryId);
    }
    
    /**
     * Override automatic assignment
     */
    public void overrideAssignment(User user, Long transactionId, Long newBudgetCategoryId) {
        Transaction transaction = transactionRepository.findByIdAndUser(transactionId, user)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        BudgetCategory newBudgetCategory = budgetCategoryRepository.findById(newBudgetCategoryId)
            .orElseThrow(() -> new RuntimeException("Budget category not found"));
        
        // Store old budget category for updates
        BudgetCategory oldBudgetCategory = transaction.getBudgetCategory();
        
        transaction.setBudgetCategory(newBudgetCategory);
        transaction.setAssignmentStatus(Transaction.AssignmentStatus.MANUALLY_ASSIGNED);
        transactionRepository.save(transaction);
        
        // Update both old and new budget categories
        if (oldBudgetCategory != null) {
            budgetService.updateBudgetAfterTransactionAssignment(oldBudgetCategory.getId());
        }
        budgetService.updateBudgetAfterTransactionAssignment(newBudgetCategoryId);
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
     */
    public List<Transaction> getUnassignedTransactions(User user) {
        return transactionRepository.findByUserAndAssignmentStatus(
            user, 
            Transaction.AssignmentStatus.UNASSIGNED
        );
    }
    
    /**
     * Get unassigned transactions for a specific month/year
     */
    public List<Transaction> getUnassignedTransactionsForMonth(User user, Integer month, Integer year) {
        List<Transaction> allUnassigned = transactionRepository.findByUserAndAssignmentStatus(
            user, Transaction.AssignmentStatus.UNASSIGNED
        );
        
        return allUnassigned.stream()
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
     */
    public List<Transaction> getTransactionsNeedingReviewForMonth(User user, Integer month, Integer year) {
        List<Transaction> allNeedingReview = transactionRepository.findByUserAndAssignmentStatus(
            user, Transaction.AssignmentStatus.NEEDS_REVIEW
        );
        
        return allNeedingReview.stream()
            .filter(transaction -> {
                int transactionMonth = transaction.getTransactionDate().getMonthValue();
                int transactionYear = transaction.getTransactionDate().getYear();
                return transactionMonth == month && transactionYear == year;
            })
            .collect(Collectors.toList());
    }
}