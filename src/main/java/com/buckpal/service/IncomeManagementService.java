package com.buckpal.service;

import com.buckpal.entity.*;
import com.buckpal.repository.IncomeCategoryRepository;
import com.buckpal.repository.TransactionRepository;
import com.buckpal.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Simplified Income Management Service
 * Uses Transaction entity directly instead of separate IncomeTransaction
 */
@Service
@Transactional
public class IncomeManagementService {
    
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    
    @Autowired
    public IncomeManagementService(IncomeCategoryRepository incomeCategoryRepository,
                                  TransactionRepository transactionRepository,
                                  BudgetRepository budgetRepository) {
        this.incomeCategoryRepository = incomeCategoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }
    
    // ====== INCOME CATEGORY METHODS ======
    
    /**
     * Create a new income category
     */
    public IncomeCategory createIncomeCategory(User user, Long budgetId, IncomeCategory category) {
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        category.setBudget(budget);
        category.setUser(user);
        
        return incomeCategoryRepository.save(category);
    }
    
    /**
     * Update an existing income category
     */
    public IncomeCategory updateIncomeCategory(User user, Long categoryId, IncomeCategory updateData) {
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new RuntimeException("Income category not found"));
        
        // Update fields
        if (updateData.getName() != null) {
            category.setName(updateData.getName());
        }
        if (updateData.getDescription() != null) {
            category.setDescription(updateData.getDescription());
        }
        if (updateData.getBudgetedAmount() != null) {
            category.setBudgetedAmount(updateData.getBudgetedAmount());
        }
        if (updateData.getColor() != null) {
            category.setColor(updateData.getColor());
        }
        if (updateData.getIcon() != null) {
            category.setIcon(updateData.getIcon());
        }
        if (updateData.getDisplayOrder() != null) {
            category.setDisplayOrder(updateData.getDisplayOrder());
        }
        
        IncomeCategory saved = incomeCategoryRepository.save(category);
        
        // Update actual amounts from linked transactions
        updateIncomeCategoryActualAmounts(saved);
        
        return saved;
    }
    
    /**
     * Delete an income category
     */
    public void deleteIncomeCategory(User user, Long categoryId) {
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new RuntimeException("Income category not found"));
        
        // Unlink all transactions from this category
        List<Transaction> linkedTransactions = transactionRepository.findByIncomeCategory(category);
        linkedTransactions.forEach(transaction -> {
            transaction.setIncomeCategory(null);
            transactionRepository.save(transaction);
        });
        
        // Remove from budget
        category.getBudget().removeIncomeCategory(category);
        
        incomeCategoryRepository.delete(category);
    }
    
    /**
     * Get income category by ID (with user security check)
     */
    public Optional<IncomeCategory> getIncomeCategoryById(User user, Long categoryId) {
        return incomeCategoryRepository.findByIdAndUser(categoryId, user);
    }
    
    // ====== TRANSACTION LINKING METHODS ======
    
    /**
     * Get available income transactions from user's transaction history
     * These are transactions with INCOME type that haven't been linked to income categories yet
     */
    public List<Transaction> getAvailableIncomeTransactions(User user) {
        return transactionRepository.findUnlinkedIncomeTransactionsByUser(user);
    }
    
    /**
     * Link historical transaction to income category
     */
    public Transaction linkTransactionToIncomeCategory(User user, Long categoryId, Long transactionId) {
        
        // Verify the category belongs to the user
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new IllegalArgumentException("Income category not found"));
        
        // Verify the transaction belongs to the user
        Transaction transaction = transactionRepository.findByIdAndUser(transactionId, user)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        if (!transaction.getTransactionType().equals(Transaction.TransactionType.INCOME)) {
            throw new IllegalArgumentException("Transaction is not an income transaction");
        }
        
        if (transaction.getIncomeCategory() != null) {
            throw new IllegalArgumentException("Transaction is already linked to an income category");
        }
        
        // Link the transaction to the category
        transaction.setIncomeCategory(category);
        Transaction saved = transactionRepository.save(transaction);
        
        // Update category actual amount
        updateIncomeCategoryActualAmounts(category);
        
        return saved;
    }
    
    /**
     * Bulk link multiple transactions to income category
     */
    @Transactional
    public List<Transaction> linkTransactionsToIncomeCategory(User user, Long categoryId, List<Long> transactionIds) {
        return transactionIds.stream()
                .map(transactionId -> linkTransactionToIncomeCategory(user, categoryId, transactionId))
                .toList();
    }
    
    /**
     * Suggest income transactions for a category based on description patterns
     */
    public List<Transaction> suggestIncomeTransactionsForCategory(User user, IncomeCategory category) {
        List<Transaction> availableTransactions = getAvailableIncomeTransactions(user);
        
        // Simple pattern matching based on category name and type
        String categoryName = category.getName().toLowerCase();
        IncomeCategory.IncomeType categoryType = category.getIncomeType();
        
        return availableTransactions.stream()
                .filter(transaction -> {
                    String description = transaction.getDescription().toLowerCase();
                    
                    // Match by category name
                    if (description.contains(categoryName)) {
                        return true;
                    }
                    
                    // Match by income type patterns
                    switch (categoryType) {
                        case SALARY:
                            return description.contains("salaire") || 
                                   description.contains("paie") || 
                                   description.contains("virement") ||
                                   description.contains("salary");
                                   
                        case BUSINESS:
                            return description.contains("facture") || 
                                   description.contains("client") || 
                                   description.contains("prestation") ||
                                   description.contains("business");
                                   
                        case INVESTMENT:
                            return description.contains("dividende") || 
                                   description.contains("interet") || 
                                   description.contains("plus-value") ||
                                   description.contains("investment");
                                   
                        case OTHER:
                        default:
                            return true; // Show all for "Other" category
                    }
                })
                .toList();
    }
    
    /**
     * Get transactions for an income category
     */
    public List<Transaction> getTransactionsForCategory(User user, Long categoryId) {
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new IllegalArgumentException("Income category not found"));
        
        return transactionRepository.findByIncomeCategory(category);
    }
    
    /**
     * Update income category actual amounts based on linked transactions
     */
    private void updateIncomeCategoryActualAmounts(IncomeCategory category) {
        BigDecimal actualAmount = transactionRepository.sumAmountByIncomeCategory(category);
        category.setActualAmount(actualAmount);
        incomeCategoryRepository.save(category);
    }
    
    /**
     * Smart category suggestion for income transaction
     */
    public IncomeCategory suggestIncomeCategory(User user, String description) {
        // Look for similar transactions that are already categorized
        List<Transaction> userIncomeTransactions = transactionRepository
                .findByUserAndTransactionType(user, Transaction.TransactionType.INCOME);
        
        for (Transaction transaction : userIncomeTransactions) {
            if (transaction.getIncomeCategory() != null && 
                transaction.getDescription().toLowerCase().contains(description.toLowerCase().substring(0, Math.min(description.length(), 5)))) {
                return transaction.getIncomeCategory();
            }
        }
        
        // Fallback to default "Other" category
        List<IncomeCategory> otherCategories = incomeCategoryRepository
                .findByUserAndIncomeType(user, IncomeCategory.IncomeType.OTHER);
        
        return otherCategories.isEmpty() ? null : otherCategories.get(0);
    }
}