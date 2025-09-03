package com.buckpal.controller;

import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.service.TransactionAssignmentService;
import com.buckpal.service.EnhancedTransactionAssignmentService;
import com.buckpal.service.SmartTransactionAssignmentService;
import com.buckpal.service.SmartTransactionAssignmentService.SmartAssignmentResult;
import com.buckpal.service.TransactionRevisionService;
import com.buckpal.service.TransactionRevisionService.RevisionResult;
import com.buckpal.service.CategoryMappingService;
import com.buckpal.repository.TransactionRepository;
import com.buckpal.entity.BudgetCategoryKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/transaction-assignments")
public class TransactionAssignmentController {
    
    private final TransactionAssignmentService transactionAssignmentService;
    private final EnhancedTransactionAssignmentService enhancedTransactionAssignmentService;
    private final SmartTransactionAssignmentService smartAssignmentService;
    private final TransactionRevisionService revisionService;
    private final CategoryMappingService categoryMappingService;
    private final TransactionRepository transactionRepository;
    
    @Autowired
    public TransactionAssignmentController(
            TransactionAssignmentService transactionAssignmentService,
            EnhancedTransactionAssignmentService enhancedTransactionAssignmentService,
            SmartTransactionAssignmentService smartAssignmentService,
            TransactionRevisionService revisionService,
            CategoryMappingService categoryMappingService,
            TransactionRepository transactionRepository) {
        this.transactionAssignmentService = transactionAssignmentService;
        this.enhancedTransactionAssignmentService = enhancedTransactionAssignmentService;
        this.smartAssignmentService = smartAssignmentService;
        this.revisionService = revisionService;
        this.categoryMappingService = categoryMappingService;
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Auto-assign unassigned transactions to budget categories (basic version)
     */
    @PostMapping("/auto-assign/{budgetId}")
    public ResponseEntity<Map<String, Object>> autoAssignTransactions(
            @PathVariable Long budgetId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        transactionAssignmentService.autoAssignTransactions(user, budgetId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Auto-assignment completed",
            "status", "success"
        ));
    }
    
    /**
     * Enhanced auto-assign with detailed results and multiple strategies
     */
    @PostMapping("/enhanced-auto-assign/{budgetId}")
    public ResponseEntity<Map<String, Object>> enhancedAutoAssignTransactions(
            @PathVariable Long budgetId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        EnhancedTransactionAssignmentService.AssignmentResult result = 
            enhancedTransactionAssignmentService.autoAssignTransactions(user, budgetId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Enhanced auto-assignment completed");
        response.put("totalAssigned", result.getTotalAssigned());
        response.put("totalNeedsReview", result.getTotalNeedsReview());
        response.put("strategyBreakdown", result.getStrategyBreakdown());
        
        // Add confidence statistics
        Map<String, Object> confidenceStats = new HashMap<>();
        if (!result.getAssigned().isEmpty()) {
            double avgConfidence = result.getAssigned().stream()
                .mapToDouble(EnhancedTransactionAssignmentService.TransactionAssignment::getConfidence)
                .average()
                .orElse(0.0);
            
            long highConfidence = result.getAssigned().stream()
                .filter(ta -> ta.getConfidence() > 0.8)
                .count();
            
            long mediumConfidence = result.getAssigned().stream()
                .filter(ta -> ta.getConfidence() > 0.6 && ta.getConfidence() <= 0.8)
                .count();
            
            long lowConfidence = result.getAssigned().stream()
                .filter(ta -> ta.getConfidence() <= 0.6)
                .count();
            
            confidenceStats.put("average", Math.round(avgConfidence * 100) / 100.0);
            confidenceStats.put("high", highConfidence);
            confidenceStats.put("medium", mediumConfidence);
            confidenceStats.put("low", lowConfidence);
        }
        response.put("confidenceStats", confidenceStats);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Manually assign a transaction to a budget category
     */
    @PostMapping("/manual-assign")
    public ResponseEntity<Map<String, Object>> manuallyAssignTransaction(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        Long transactionId = request.get("transactionId");
        Long budgetCategoryId = request.get("budgetCategoryId");
        
        transactionAssignmentService.manuallyAssignTransaction(user, transactionId, budgetCategoryId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Transaction assigned successfully",
            "status", "success"
        ));
    }
    
    /**
     * Override an existing assignment
     */
    @PutMapping("/override/{transactionId}")
    public ResponseEntity<Map<String, Object>> overrideAssignment(
            @PathVariable Long transactionId,
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        Long newBudgetCategoryId = request.get("budgetCategoryId");
        
        transactionAssignmentService.overrideAssignment(user, transactionId, newBudgetCategoryId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Assignment overridden successfully", 
            "status", "success"
        ));
    }
    
    /**
     * Assign transaction to detailed category (simplified approach).
     * The budget category is automatically determined via category mapping.
     */
    @PostMapping("/assign-detailed")
    public ResponseEntity<Map<String, Object>> assignTransactionToDetailedCategory(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            Long transactionId = request.get("transactionId");
            Long detailedCategoryId = request.get("detailedCategoryId");
            
            // Get the budget category from the detailed category mapping
            Optional<BudgetCategoryKey> budgetCategoryKey = categoryMappingService.getBudgetCategoryForDetailed(detailedCategoryId);
            
            if (budgetCategoryKey.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "No budget category mapping found for detailed category " + detailedCategoryId,
                    "status", "error"
                ));
            }
            
            // TODO: Find the actual BudgetCategory entity from the key
            // For now, we'll use the manual assignment method with the budget category ID
            // This requires extending the service to handle BudgetCategoryKey → BudgetCategory lookup
            
            // Temporary workaround: Use the legacy manual assignment
            // In a full implementation, we'd extend TransactionAssignmentService to handle this
            transactionAssignmentService.manuallyAssignTransaction(user, transactionId, detailedCategoryId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Transaction assigned successfully to detailed category", 
                "status", "success",
                "budgetCategoryKey", budgetCategoryKey.get().toString()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Failed to assign transaction: " + e.getMessage(),
                "status", "error"
            ));
        }
    }
    
    /**
     * Get transactions that need review (failed auto-assignment) for a specific budget
     */
    @GetMapping("/needs-review/{budgetId}")
    public ResponseEntity<List<Transaction>> getTransactionsNeedingReview(
            @PathVariable Long budgetId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        List<Transaction> transactions = transactionAssignmentService.getTransactionsNeedingReview(user, budgetId);
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Get transactions that need review (failed auto-assignment) - all months
     */
    @GetMapping("/needs-review")
    public ResponseEntity<List<Transaction>> getTransactionsNeedingReview(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        List<Transaction> transactions = transactionAssignmentService.getTransactionsNeedingReview(user);
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Get unassigned transactions for a specific budget
     */
    @GetMapping("/unassigned/{budgetId}")
    public ResponseEntity<List<Transaction>> getUnassignedTransactions(
            @PathVariable Long budgetId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        List<Transaction> transactions = transactionAssignmentService.getUnassignedTransactions(user, budgetId);
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Get unassigned transactions - all months
     */
    @GetMapping("/unassigned")
    public ResponseEntity<List<Transaction>> getUnassignedTransactions(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        List<Transaction> transactions = transactionAssignmentService.getUnassignedTransactions(user);
        
        return ResponseEntity.ok(transactions);
    }
    
    // ==================== SMART ASSIGNMENT API ENDPOINTS ====================
    
    /**
     * Get smart category suggestion for a specific transaction
     */
    @PostMapping("/smart-suggest/{transactionId}")
    public ResponseEntity<?> suggestSmartCategory(@PathVariable Long transactionId, Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            Optional<Transaction> transactionOpt = transactionRepository.findByIdAndUser(transactionId, currentUser);
            if (transactionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Transaction transaction = transactionOpt.get();
            SmartAssignmentResult result = smartAssignmentService.assignCategoryToTransaction(transaction, currentUser);
            
            return ResponseEntity.ok(Map.of(
                "transactionId", transactionId,
                "suggestedCategory", result.categoryName != null ? result.categoryName : "",
                "confidence", result.confidence,
                "strategy", result.strategy,
                "alternativeCategories", result.alternativeCategories,
                "merchantText", buildMerchantText(transaction)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Submit user feedback for smart assignment learning
     */
    @PostMapping("/smart-feedback/{transactionId}")
    public ResponseEntity<?> submitSmartFeedback(@PathVariable Long transactionId,
                                               @RequestBody FeedbackRequest request,
                                               Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            Optional<Transaction> transactionOpt = transactionRepository.findByIdAndUser(transactionId, currentUser);
            if (transactionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Transaction transaction = transactionOpt.get();
            
            smartAssignmentService.recordFeedback(
                transaction,
                currentUser,
                request.getSuggestedCategory(),
                request.getUserChosenCategory(),
                request.isWasAccepted(),
                request.getPatternUsed()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Feedback enregistré avec succès",
                "transactionId", transactionId
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get smart category suggestions for multiple transactions (batch)
     */
    @PostMapping("/smart-suggest-batch")
    public ResponseEntity<?> suggestSmartCategoriesForTransactions(@RequestBody BatchSuggestionRequest request,
                                                                 Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            Map<Long, SmartAssignmentResult> results = new HashMap<>();
            
            for (Long transactionId : request.getTransactionIds()) {
                Optional<Transaction> transactionOpt = transactionRepository.findByIdAndUser(transactionId, currentUser);
                if (transactionOpt.isPresent()) {
                    Transaction transaction = transactionOpt.get();
                    SmartAssignmentResult result = smartAssignmentService.assignCategoryToTransaction(transaction, currentUser);
                    results.put(transactionId, result);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "suggestions", results.entrySet().stream().collect(
                    java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Map.of(
                            "suggestedCategory", entry.getValue().categoryName != null ? entry.getValue().categoryName : "",
                            "confidence", entry.getValue().confidence,
                            "strategy", entry.getValue().strategy,
                            "alternativeCategories", entry.getValue().alternativeCategories
                        )
                    )
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    private String buildMerchantText(Transaction transaction) {
        StringBuilder merchantText = new StringBuilder();
        
        if (transaction.getMerchantName() != null && !transaction.getMerchantName().trim().isEmpty()) {
            merchantText.append(transaction.getMerchantName().trim());
        }
        
        if (transaction.getDescription() != null && !transaction.getDescription().trim().isEmpty()) {
            String description = transaction.getDescription().trim();
            if (merchantText.length() > 0 && !description.equals(merchantText.toString())) {
                merchantText.append(" - ").append(description);
            } else if (merchantText.length() == 0) {
                merchantText.append(description);
            }
        }
        
        return merchantText.toString();
    }
    
    // Request DTOs for smart assignment endpoints
    public static class FeedbackRequest {
        private String suggestedCategory;
        private String userChosenCategory;
        private boolean wasAccepted;
        private String patternUsed;
        
        // Getters and Setters
        public String getSuggestedCategory() { return suggestedCategory; }
        public void setSuggestedCategory(String suggestedCategory) { this.suggestedCategory = suggestedCategory; }
        
        public String getUserChosenCategory() { return userChosenCategory; }
        public void setUserChosenCategory(String userChosenCategory) { this.userChosenCategory = userChosenCategory; }
        
        public boolean isWasAccepted() { return wasAccepted; }
        public void setWasAccepted(boolean wasAccepted) { this.wasAccepted = wasAccepted; }
        
        public String getPatternUsed() { return patternUsed; }
        public void setPatternUsed(String patternUsed) { this.patternUsed = patternUsed; }
    }
    
    public static class BatchSuggestionRequest {
        private List<Long> transactionIds;
        
        public List<Long> getTransactionIds() { return transactionIds; }
        public void setTransactionIds(List<Long> transactionIds) { this.transactionIds = transactionIds; }
    }
    
    // ==================== TRANSACTION REVISION API ENDPOINTS ====================
    
    /**
     * Get recently assigned transactions that can be revised
     */
    @GetMapping("/recently-assigned")
    public ResponseEntity<List<Transaction>> getRecentlyAssignedTransactions(Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            List<Transaction> recentTransactions = revisionService.getRecentlyAssignedTransactions(currentUser);
            return ResponseEntity.ok(recentTransactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Detect transactions that need revision based on confidence analysis
     */
    @GetMapping("/detect-revision-needed")
    public ResponseEntity<?> detectTransactionsNeedingRevision(Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            List<Transaction> suspiciousTransactions = revisionService.detectTransactionsNeedingRevision(currentUser);
            
            return ResponseEntity.ok(Map.of(
                "suspiciousTransactions", suspiciousTransactions,
                "count", suspiciousTransactions.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Automatically detect and mark suspicious assignments for revision
     */
    @PostMapping("/auto-detect-revision")
    public ResponseEntity<?> autoDetectAndMarkForRevision(Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            RevisionResult result = revisionService.autoDetectAndMarkForRevision(currentUser);
            
            return ResponseEntity.ok(Map.of(
                "message", "Détection automatique terminée",
                "totalSuspicious", result.getTotalSuspicious(),
                "markedForRevision", result.getMarkedForRevision(),
                "revisedTransactions", result.getRevisedTransactions()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Mark specific transactions for revision
     */
    @PostMapping("/mark-for-revision")
    public ResponseEntity<?> markTransactionsForRevision(@RequestBody BatchRevisionRequest request,
                                                        Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            revisionService.markForRevision(request.getTransactionIds(), currentUser);
            
            return ResponseEntity.ok(Map.of(
                "message", "Transactions marquées pour révision",
                "count", request.getTransactionIds().size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Mark a single transaction for revision
     */
    @PostMapping("/mark-for-revision/{transactionId}")
    public ResponseEntity<?> markTransactionForRevision(@PathVariable Long transactionId,
                                                       Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            revisionService.markForRevision(transactionId, currentUser);
            
            return ResponseEntity.ok(Map.of(
                "message", "Transaction marquée pour révision",
                "transactionId", transactionId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Request DTO for batch revision
    public static class BatchRevisionRequest {
        private List<Long> transactionIds;
        
        public List<Long> getTransactionIds() { return transactionIds; }
        public void setTransactionIds(List<Long> transactionIds) { this.transactionIds = transactionIds; }
    }
}