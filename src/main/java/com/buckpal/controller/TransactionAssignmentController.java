package com.buckpal.controller;

import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.service.TransactionAssignmentService;
import com.buckpal.service.EnhancedTransactionAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/transaction-assignments")
public class TransactionAssignmentController {
    
    private final TransactionAssignmentService transactionAssignmentService;
    private final EnhancedTransactionAssignmentService enhancedTransactionAssignmentService;
    
    @Autowired
    public TransactionAssignmentController(
            TransactionAssignmentService transactionAssignmentService,
            EnhancedTransactionAssignmentService enhancedTransactionAssignmentService) {
        this.transactionAssignmentService = transactionAssignmentService;
        this.enhancedTransactionAssignmentService = enhancedTransactionAssignmentService;
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
}