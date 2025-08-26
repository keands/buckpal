package com.buckpal.service;

import com.buckpal.entity.*;
import com.buckpal.repository.IncomeCategoryRepository;
import com.buckpal.repository.IncomeTransactionRepository;
import com.buckpal.repository.BudgetRepository;
import com.buckpal.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class IncomeService {
    
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final IncomeTransactionRepository incomeTransactionRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    
    @Autowired
    public IncomeService(IncomeCategoryRepository incomeCategoryRepository,
                        IncomeTransactionRepository incomeTransactionRepository,
                        BudgetRepository budgetRepository,
                        TransactionRepository transactionRepository) {
        this.incomeCategoryRepository = incomeCategoryRepository;
        this.incomeTransactionRepository = incomeTransactionRepository;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }
    
    // ====== INCOME CATEGORY METHODS ======
    
    /**
     * Create default income categories for a budget
     */
    public void createDefaultIncomeCategories(Budget budget) {
        int order = 0;
        for (IncomeCategory.IncomeType type : IncomeCategory.IncomeType.values()) {
            IncomeCategory category = new IncomeCategory();
            category.setName(type.getDisplayName());
            category.setDescription(type.getDescription());
            category.setIncomeType(type);
            category.setColor(type.getDefaultColor());
            category.setIcon(type.getDefaultIcon());
            category.setDisplayOrder(order++);
            category.setIsDefault(true);
            category.setBudget(budget);
            
            incomeCategoryRepository.save(category);
            budget.addIncomeCategory(category);
        }
    }
    
    /**
     * Get all income categories for a budget
     */
    public List<IncomeCategory> getIncomeCategories(Budget budget) {
        return incomeCategoryRepository.findByBudgetIdOrderByDisplayOrderAsc(budget.getId());
    }
    
    /**
     * Create a new income category
     */
    public IncomeCategory createIncomeCategory(User user, Long budgetId, IncomeCategory incomeCategory) {
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        // Check if name already exists
        if (incomeCategoryRepository.existsByBudgetIdAndName(budgetId, incomeCategory.getName())) {
            throw new RuntimeException("Income category with this name already exists");
        }
        
        // Set default values
        if (incomeCategory.getDisplayOrder() == null) {
            long count = incomeCategoryRepository.countByBudgetId(budgetId);
            incomeCategory.setDisplayOrder((int) count);
        }
        
        incomeCategory.setBudget(budget);
        IncomeCategory saved = incomeCategoryRepository.save(incomeCategory);
        budget.addIncomeCategory(saved);
        
        return saved;
    }
    
    /**
     * Update an income category
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
        
        return incomeCategoryRepository.save(category);
    }
    
    /**
     * Delete an income category
     */
    public void deleteIncomeCategory(User user, Long categoryId) {
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new RuntimeException("Income category not found"));
        
        // Delete associated transactions first
        incomeTransactionRepository.deleteByIncomeCategoryId(categoryId);
        
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
    
    /**
     * Update income category actual amounts based on associated transactions
     */
    private void updateIncomeCategoryActualAmounts(IncomeCategory category) {
        List<IncomeTransaction> transactions = incomeTransactionRepository
                .findByIncomeCategoryIdOrderByTransactionDateDesc(category.getId());
        
        BigDecimal actualAmount = transactions.stream()
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        category.setActualAmount(actualAmount);
        incomeCategoryRepository.save(category);
    }
    
    // ====== INCOME TRANSACTION METHODS ======
    
    /**
     * Create a new income transaction
     */
    public IncomeTransaction createIncomeTransaction(User user, Long categoryId, IncomeTransaction transaction) {
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new RuntimeException("Income category not found"));
        
        transaction.setIncomeCategory(category);
        transaction.setUser(user);
        
        // Set default source if not provided
        if (transaction.getSource() == null) {
            transaction.setSource("Manual Entry");
        }
        
        IncomeTransaction saved = incomeTransactionRepository.save(transaction);
        
        // Update category actual amount
        category.addIncomeTransaction(saved);
        incomeCategoryRepository.save(category);
        
        return saved;
    }
    
    /**
     * Update an income transaction
     */
    public IncomeTransaction updateIncomeTransaction(User user, Long transactionId, IncomeTransaction updateData) {
        IncomeTransaction transaction = incomeTransactionRepository.findByIdAndUser(transactionId, user)
                .orElseThrow(() -> new RuntimeException("Income transaction not found"));
        
        // Store old category for amount recalculation
        IncomeCategory oldCategory = transaction.getIncomeCategory();
        
        // Update fields
        if (updateData.getDescription() != null) {
            transaction.setDescription(updateData.getDescription());
        }
        if (updateData.getAmount() != null) {
            transaction.setAmount(updateData.getAmount());
        }
        if (updateData.getTransactionDate() != null) {
            transaction.setTransactionDate(updateData.getTransactionDate());
        }
        if (updateData.getNotes() != null) {
            transaction.setNotes(updateData.getNotes());
        }
        if (updateData.getRecurrenceType() != null) {
            transaction.setRecurrenceType(updateData.getRecurrenceType());
        }
        if (updateData.getIsRecurring() != null) {
            transaction.setIsRecurring(updateData.getIsRecurring());
        }
        
        IncomeTransaction saved = incomeTransactionRepository.save(transaction);
        
        // Recalculate category amounts
        oldCategory.removeIncomeTransaction(saved);
        oldCategory.addIncomeTransaction(saved);
        incomeCategoryRepository.save(oldCategory);
        
        return saved;
    }
    
    /**
     * Delete an income transaction
     */
    public void deleteIncomeTransaction(User user, Long transactionId) {
        IncomeTransaction transaction = incomeTransactionRepository.findByIdAndUser(transactionId, user)
                .orElseThrow(() -> new RuntimeException("Income transaction not found"));
        
        IncomeCategory category = transaction.getIncomeCategory();
        category.removeIncomeTransaction(transaction);
        incomeCategoryRepository.save(category);
        
        incomeTransactionRepository.delete(transaction);
    }
    
    /**
     * Get income transactions for a user
     */
    public List<IncomeTransaction> getIncomeTransactions(User user) {
        return incomeTransactionRepository.findByUserOrderByTransactionDateDesc(user);
    }
    
    /**
     * Get income transactions with pagination
     */
    public Page<IncomeTransaction> getIncomeTransactions(User user, Pageable pageable) {
        return incomeTransactionRepository.findByUserOrderByTransactionDateDesc(user, pageable);
    }
    
    /**
     * Get income transactions for a specific month
     */
    public List<IncomeTransaction> getIncomeTransactionsForMonth(User user, Integer month, Integer year) {
        return incomeTransactionRepository.findByUserAndMonth(user, month, year);
    }
    
    /**
     * Get total income for a month
     */
    public BigDecimal getTotalIncomeForMonth(User user, Integer month, Integer year) {
        return incomeTransactionRepository.getTotalIncomeForMonth(user, month, year);
    }
    
    /**
     * Get income transactions for a specific category
     */
    public List<IncomeTransaction> getIncomeTransactionsForCategory(User user, Long categoryId) {
        // Verify category belongs to user
        incomeCategoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new RuntimeException("Income category not found"));
        
        return incomeTransactionRepository.findByIncomeCategoryIdOrderByTransactionDateDesc(categoryId);
    }
    
    /**
     * Get income transaction by ID
     */
    public Optional<IncomeTransaction> getIncomeTransactionById(User user, Long transactionId) {
        return incomeTransactionRepository.findByIdAndUser(transactionId, user);
    }
    
    // ====== SMART ASSIGNMENT METHODS ======
    
    /**
     * Suggest income category based on transaction description
     */
    public IncomeCategory suggestIncomeCategory(User user, String description) {
        // Find similar transactions
        List<IncomeTransaction> similar = incomeTransactionRepository
                .findSimilarIncomeTransactions(user, description);
        
        if (!similar.isEmpty()) {
            // Return the category of the most recent similar transaction
            return similar.get(0).getIncomeCategory();
        }
        
        // Fallback to default "Other" category
        List<IncomeCategory> otherCategories = incomeCategoryRepository
                .findByUserAndIncomeType(user, IncomeCategory.IncomeType.OTHER);
        
        return otherCategories.isEmpty() ? null : otherCategories.get(0);
    }
    
    /**
     * Learn from user's income categorization
     */
    public void recordIncomeAssignmentFeedback(User user, String description, 
                                              Long suggestedCategoryId, Long chosenCategoryId) {
        // This could be enhanced with a feedback learning system similar to expenses
        // For now, it's just a placeholder for future ML implementation
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
     * Create IncomeTransaction from existing Transaction and link to category
     */
    public IncomeTransaction createIncomeTransactionFromHistoricalTransaction(
            User user, Long categoryId, Long transactionId) {
        
        // Verify the category belongs to the user
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new IllegalArgumentException("Income category not found"));
        
        // Verify the transaction belongs to the user and is unlinked
        Transaction sourceTransaction = transactionRepository.findByIdAndUser(transactionId, user)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        if (!sourceTransaction.getTransactionType().equals(Transaction.TransactionType.INCOME)) {
            throw new IllegalArgumentException("Transaction is not an income transaction");
        }
        
        // Check if already linked
        List<IncomeTransaction> existing = incomeTransactionRepository
                .findBySourceTransaction(sourceTransaction);
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Transaction is already linked to an income category");
        }
        
        // Create the income transaction
        IncomeTransaction incomeTransaction = new IncomeTransaction();
        incomeTransaction.setDescription(sourceTransaction.getDescription());
        incomeTransaction.setAmount(sourceTransaction.getAmount());
        incomeTransaction.setTransactionDate(sourceTransaction.getTransactionDate());
        incomeTransaction.setSource("Historical Transaction");
        incomeTransaction.setSourceReference("Transaction ID: " + sourceTransaction.getId());
        incomeTransaction.setRecurrenceType(IncomeTransaction.RecurrenceType.ONE_TIME);
        incomeTransaction.setIsRecurring(false);
        incomeTransaction.setIncomeCategory(category);
        incomeTransaction.setUser(user);
        incomeTransaction.setSourceTransaction(sourceTransaction);
        
        IncomeTransaction saved = incomeTransactionRepository.save(incomeTransaction);
        
        // Update category actual amount
        updateIncomeCategoryActualAmounts(category);
        
        return saved;
    }
    
    /**
     * Bulk create IncomeTransactions from multiple historical Transactions
     */
    @Transactional
    public List<IncomeTransaction> createIncomeTransactionsFromHistoricalTransactions(
            User user, Long categoryId, List<Long> transactionIds) {
        
        return transactionIds.stream()
                .map(transactionId -> createIncomeTransactionFromHistoricalTransaction(user, categoryId, transactionId))
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
}