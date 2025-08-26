package com.buckpal.controller;

import com.buckpal.dto.IncomeCategoryDto;
import com.buckpal.dto.IncomeTransactionDto;
import com.buckpal.entity.IncomeCategory;
import com.buckpal.entity.IncomeTransaction;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.service.IncomeManagementService;
import com.buckpal.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/income")
public class IncomeController {
    
    private final IncomeManagementService incomeService;
    private final BudgetService budgetService;
    
    @Autowired
    public IncomeController(IncomeManagementService incomeService, BudgetService budgetService) {
        this.incomeService = incomeService;
        this.budgetService = budgetService;
    }
    
    // ====== INCOME CATEGORY ENDPOINTS ======
    
    /**
     * Get income categories for a specific budget
     */
    @GetMapping("/budgets/{budgetId}/categories")
    public ResponseEntity<List<IncomeCategoryDto>> getIncomeCategories(
            @PathVariable Long budgetId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            List<IncomeCategory> categories = budgetService.getBudgetIncomeCategories(user, budgetId);
            List<IncomeCategoryDto> dtos = categories.stream()
                    .map(this::convertToDto)
                    .toList();
            
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Create a new income category
     */
    @PostMapping("/budgets/{budgetId}/categories")
    public ResponseEntity<IncomeCategoryDto> createIncomeCategory(
            @PathVariable Long budgetId,
            @Valid @RequestBody IncomeCategoryDto categoryDto,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            IncomeCategory category = convertToEntity(categoryDto);
            IncomeCategory created = incomeService.createIncomeCategory(user, budgetId, category);
            
            // Update budget totals
            budgetService.updateBudgetAfterIncomeChange(budgetId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update an income category
     */
    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<IncomeCategoryDto> updateIncomeCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody IncomeCategoryDto categoryDto,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            IncomeCategory updateData = convertToEntity(categoryDto);
            IncomeCategory updated = incomeService.updateIncomeCategory(user, categoryId, updateData);
            
            // Update budget totals
            budgetService.updateBudgetAfterIncomeChange(updated.getBudget().getId());
            
            return ResponseEntity.ok(convertToDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete an income category
     */
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteIncomeCategory(
            @PathVariable Long categoryId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            // Get budget ID before deletion
            Optional<IncomeCategory> category = incomeService.getIncomeCategoryById(user, categoryId);
            Long budgetId = category.map(c -> c.getBudget().getId()).orElse(null);
            
            incomeService.deleteIncomeCategory(user, categoryId);
            
            // Update budget totals
            if (budgetId != null) {
                budgetService.updateBudgetAfterIncomeChange(budgetId);
            }
            
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get income category by ID
     */
    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<IncomeCategoryDto> getIncomeCategory(
            @PathVariable Long categoryId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        Optional<IncomeCategory> category = incomeService.getIncomeCategoryById(user, categoryId);
        return category.map(c -> ResponseEntity.ok(convertToDto(c)))
                      .orElse(ResponseEntity.notFound().build());
    }
    
    // ====== INCOME TRANSACTION ENDPOINTS ======
    
    /**
     * Get income transactions for a user
     */
    @GetMapping("/transactions")
    public ResponseEntity<Page<IncomeTransactionDto>> getIncomeTransactions(
            Authentication authentication,
            Pageable pageable) {
        User user = (User) authentication.getPrincipal();
        
        Page<IncomeTransaction> transactions = incomeService.getIncomeTransactions(user, pageable);
        Page<IncomeTransactionDto> dtos = transactions.map(this::convertToDto);
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get income transactions for a specific month
     */
    @GetMapping("/transactions/month/{year}/{month}")
    public ResponseEntity<List<IncomeTransactionDto>> getIncomeTransactionsForMonth(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        List<IncomeTransaction> transactions = incomeService.getIncomeTransactionsForMonth(user, month, year);
        List<IncomeTransactionDto> dtos = transactions.stream()
                .map(this::convertToDto)
                .toList();
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get total income for a specific month
     */
    @GetMapping("/transactions/month/{year}/{month}/total")
    public ResponseEntity<Map<String, BigDecimal>> getTotalIncomeForMonth(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        BigDecimal total = incomeService.getTotalIncomeForMonth(user, month, year);
        return ResponseEntity.ok(Map.of("totalIncome", total));
    }
    
    /**
     * Create a new income transaction
     */
    @PostMapping("/categories/{categoryId}/transactions")
    public ResponseEntity<IncomeTransactionDto> createIncomeTransaction(
            @PathVariable Long categoryId,
            @Valid @RequestBody IncomeTransactionDto transactionDto,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            IncomeTransaction transaction = convertToEntity(transactionDto);
            IncomeTransaction created = incomeService.createIncomeTransaction(user, categoryId, transaction);
            
            // Update budget totals
            budgetService.updateBudgetAfterIncomeChange(created.getIncomeCategory().getBudget().getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update an income transaction
     */
    @PutMapping("/transactions/{transactionId}")
    public ResponseEntity<IncomeTransactionDto> updateIncomeTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody IncomeTransactionDto transactionDto,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            IncomeTransaction updateData = convertToEntity(transactionDto);
            IncomeTransaction updated = incomeService.updateIncomeTransaction(user, transactionId, updateData);
            
            // Update budget totals
            budgetService.updateBudgetAfterIncomeChange(updated.getIncomeCategory().getBudget().getId());
            
            return ResponseEntity.ok(convertToDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete an income transaction
     */
    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<Void> deleteIncomeTransaction(
            @PathVariable Long transactionId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            // Get budget ID before deletion
            Optional<IncomeTransaction> transaction = incomeService.getIncomeTransactionById(user, transactionId);
            Long budgetId = transaction.map(t -> t.getIncomeCategory().getBudget().getId()).orElse(null);
            
            incomeService.deleteIncomeTransaction(user, transactionId);
            
            // Update budget totals
            if (budgetId != null) {
                budgetService.updateBudgetAfterIncomeChange(budgetId);
            }
            
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get income transactions for a specific category
     */
    @GetMapping("/categories/{categoryId}/transactions")
    public ResponseEntity<List<IncomeTransactionDto>> getIncomeTransactionsForCategory(
            @PathVariable Long categoryId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            List<IncomeTransaction> transactions = incomeService.getIncomeTransactionsForCategory(user, categoryId);
            List<IncomeTransactionDto> dtos = transactions.stream()
                    .map(this::convertToDto)
                    .toList();
            
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // ====== SMART ASSIGNMENT ENDPOINTS ======
    
    /**
     * Get category suggestion for income transaction
     */
    @PostMapping("/transactions/suggest-category")
    public ResponseEntity<IncomeCategoryDto> suggestIncomeCategory(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        String description = request.get("description");
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        IncomeCategory suggestion = incomeService.suggestIncomeCategory(user, description);
        if (suggestion == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(convertToDto(suggestion));
    }
    
    /**
     * Submit feedback for income categorization learning
     */
    @PostMapping("/transactions/feedback")
    public ResponseEntity<Map<String, String>> submitIncomeFeedback(
            @RequestBody Map<String, Object> feedback,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            String description = (String) feedback.get("description");
            Long suggestedCategoryId = Long.valueOf(feedback.get("suggestedCategoryId").toString());
            Long chosenCategoryId = Long.valueOf(feedback.get("chosenCategoryId").toString());
            
            incomeService.recordIncomeAssignmentFeedback(user, description, suggestedCategoryId, chosenCategoryId);
            
            return ResponseEntity.ok(Map.of("message", "Feedback recorded successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid feedback data"));
        }
    }
    
    /**
     * Get income statistics for budget
     */
    @GetMapping("/budgets/{budgetId}/statistics")
    public ResponseEntity<Map<String, BigDecimal>> getIncomeStatistics(
            @PathVariable Long budgetId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Map<String, BigDecimal> stats = budgetService.getIncomeStatistics(user, budgetId);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // ====== TRANSACTION LINKING ENDPOINTS ======
    
    /**
     * Get available income transactions from user's history
     */
    @GetMapping("/available-transactions")
    public ResponseEntity<List<Map<String, Object>>> getAvailableIncomeTransactions(
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        List<Transaction> transactions = incomeService.getAvailableIncomeTransactions(user);
        List<Map<String, Object>> dtos = transactions.stream()
                .map(this::convertTransactionToDto)
                .toList();
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get suggested income transactions for a category
     */
    @GetMapping("/categories/{categoryId}/suggested-transactions")
    public ResponseEntity<List<Map<String, Object>>> getSuggestedTransactionsForCategory(
            @PathVariable Long categoryId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Optional<IncomeCategory> category = incomeService.getIncomeCategoryById(user, categoryId);
            if (category.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            List<Transaction> transactions = incomeService.suggestIncomeTransactionsForCategory(user, category.get());
            List<Map<String, Object>> dtos = transactions.stream()
                    .map(this::convertTransactionToDto)
                    .toList();
            
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Link historical transactions to income category
     */
    @PostMapping("/categories/{categoryId}/link-transactions")
    public ResponseEntity<List<Map<String, Object>>> linkHistoricalTransactions(
            @PathVariable Long categoryId,
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            List<Long> transactionIds = request.get("transactionIds");
            if (transactionIds == null || transactionIds.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<Transaction> linkedTransactions = incomeService
                    .linkTransactionsToIncomeCategory(user, categoryId, transactionIds);
            
            List<Map<String, Object>> dtos = linkedTransactions.stream()
                    .map(this::convertTransactionToDto)
                    .toList();
            
            // Update budget totals - get category from first transaction
            if (!linkedTransactions.isEmpty() && linkedTransactions.get(0).getIncomeCategory() != null) {
                budgetService.updateBudgetAfterIncomeChange(
                    linkedTransactions.get(0).getIncomeCategory().getBudget().getId()
                );
            }
            
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ====== CONVERSION METHODS ======
    
    private IncomeCategoryDto convertToDto(IncomeCategory entity) {
        IncomeCategoryDto dto = new IncomeCategoryDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setBudgetedAmount(entity.getBudgetedAmount());
        dto.setActualAmount(entity.getActualAmount());
        dto.setColor(entity.getColor());
        dto.setIcon(entity.getIcon());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setIsDefault(entity.getIsDefault());
        dto.setIncomeType(entity.getIncomeType());
        dto.setBudgetId(entity.getBudget() != null ? entity.getBudget().getId() : null);
        
        // Calculated fields
        dto.setVariance(entity.getVariance());
        dto.setUsagePercentage(entity.getUsagePercentage());
        dto.setIsOverBudget(entity.isOverBudget());
        dto.setIsUnderBudget(entity.isUnderBudget());
        
        return dto;
    }
    
    private IncomeCategory convertToEntity(IncomeCategoryDto dto) {
        IncomeCategory entity = new IncomeCategory();
        if (dto.getId() != null) entity.setId(dto.getId());
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getBudgetedAmount() != null) entity.setBudgetedAmount(dto.getBudgetedAmount());
        if (dto.getActualAmount() != null) entity.setActualAmount(dto.getActualAmount());
        if (dto.getColor() != null) entity.setColor(dto.getColor());
        if (dto.getIcon() != null) entity.setIcon(dto.getIcon());
        if (dto.getDisplayOrder() != null) entity.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getIsDefault() != null) entity.setIsDefault(dto.getIsDefault());
        if (dto.getIncomeType() != null) entity.setIncomeType(dto.getIncomeType());
        
        return entity;
    }
    
    private IncomeTransactionDto convertToDto(IncomeTransaction entity) {
        IncomeTransactionDto dto = new IncomeTransactionDto();
        dto.setId(entity.getId());
        dto.setDescription(entity.getDescription());
        dto.setAmount(entity.getAmount());
        dto.setTransactionDate(entity.getTransactionDate());
        dto.setNotes(entity.getNotes());
        dto.setSource(entity.getSource());
        dto.setSourceReference(entity.getSourceReference());
        dto.setRecurrenceType(entity.getRecurrenceType());
        dto.setIsRecurring(entity.getIsRecurring());
        dto.setIncomeCategoryId(entity.getIncomeCategory() != null ? entity.getIncomeCategory().getId() : null);
        dto.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        // Additional display fields
        if (entity.getIncomeCategory() != null) {
            dto.setIncomeCategoryName(entity.getIncomeCategory().getName());
            dto.setIncomeCategoryColor(entity.getIncomeCategory().getColor());
            dto.setIncomeCategoryIcon(entity.getIncomeCategory().getIcon());
        }
        
        return dto;
    }
    
    private IncomeTransaction convertToEntity(IncomeTransactionDto dto) {
        IncomeTransaction entity = new IncomeTransaction();
        if (dto.getId() != null) entity.setId(dto.getId());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getAmount() != null) entity.setAmount(dto.getAmount());
        if (dto.getTransactionDate() != null) entity.setTransactionDate(dto.getTransactionDate());
        if (dto.getNotes() != null) entity.setNotes(dto.getNotes());
        if (dto.getSource() != null) entity.setSource(dto.getSource());
        if (dto.getSourceReference() != null) entity.setSourceReference(dto.getSourceReference());
        if (dto.getRecurrenceType() != null) entity.setRecurrenceType(dto.getRecurrenceType());
        if (dto.getIsRecurring() != null) entity.setIsRecurring(dto.getIsRecurring());
        
        return entity;
    }
    
    private Map<String, Object> convertTransactionToDto(Transaction transaction) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", transaction.getId());
        dto.put("description", transaction.getDescription());
        dto.put("amount", transaction.getAmount());
        dto.put("transactionDate", transaction.getTransactionDate());
        dto.put("transactionType", transaction.getTransactionType());
        dto.put("accountId", transaction.getAccount() != null ? transaction.getAccount().getId() : null);
        dto.put("accountName", transaction.getAccount() != null ? transaction.getAccount().getName() : null);
        
        // Add category info if available
        if (transaction.getCategory() != null) {
            dto.put("categoryId", transaction.getCategory().getId());
            dto.put("categoryName", transaction.getCategory().getName());
        }
        
        return dto;
    }
}