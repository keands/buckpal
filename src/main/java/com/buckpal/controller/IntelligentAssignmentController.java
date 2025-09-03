package com.buckpal.controller;

import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.service.IntelligentAssignmentMigrationService;
import com.buckpal.service.SmartTransactionAssignmentService;
import com.buckpal.service.SmartTransactionAssignmentService.SmartAssignmentResult;
import com.buckpal.repository.TransactionRepository;
import com.buckpal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for intelligent transaction assignment with the new category mapping system
 */
@RestController
@RequestMapping("/api/intelligent-assignment")
public class IntelligentAssignmentController {

    @Autowired
    private SmartTransactionAssignmentService smartAssignmentService;
    
    @Autowired
    private IntelligentAssignmentMigrationService migrationService;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Get intelligent category suggestion for a specific transaction
     */
    @PostMapping("/suggest/{transactionId}")
    public ResponseEntity<SmartAssignmentResult> suggestCategoryForTransaction(
            @PathVariable Long transactionId,
            Authentication authentication) {
        
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }
            
            Transaction transaction = transactionRepository.findById(transactionId).orElse(null);
            if (transaction == null) {
                return ResponseEntity.notFound().build();
            }
            
            SmartAssignmentResult result = smartAssignmentService.assignCategoryToTransaction(transaction, user);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Perform bulk intelligent assignment for unassigned transactions
     */
    @PostMapping("/bulk-assign")
    public ResponseEntity<Map<String, Object>> performBulkIntelligentAssignment(
            Authentication authentication) {
        
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // Get unassigned transactions for this user
            List<Transaction> unassignedTransactions = transactionRepository.findByUserAndDetailedCategoryIsNull(user);
            
            int totalTransactions = unassignedTransactions.size();
            int assigned = 0;
            int needsReview = 0;
            
            Map<String, Integer> strategyCounts = new HashMap<>();
            
            for (Transaction transaction : unassignedTransactions) {
                SmartAssignmentResult result = smartAssignmentService.assignCategoryToTransaction(transaction, user);
                
                if (result.categoryId != null && result.confidence.doubleValue() >= 0.7) {
                    // High confidence - auto assign
                    transaction.setDetailedCategoryId(result.categoryId);
                    transaction.setAssignmentConfidence(result.confidence);
                    transactionRepository.save(transaction);
                    assigned++;
                } else if (result.categoryId != null) {
                    // Low confidence - mark for review
                    transaction.setDetailedCategoryId(result.categoryId);
                    transaction.setAssignmentConfidence(result.confidence);
                    transaction.setNeedsReview(true);
                    transactionRepository.save(transaction);
                    needsReview++;
                }
                
                // Count strategies
                strategyCounts.put(result.strategy, strategyCounts.getOrDefault(result.strategy, 0) + 1);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalTransactions", totalTransactions);
            response.put("assigned", assigned);
            response.put("needsReview", needsReview);
            response.put("unassigned", totalTransactions - assigned - needsReview);
            response.put("strategyBreakdown", strategyCounts);
            response.put("success", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors de l'assignation intelligente: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Migrate legacy patterns to new category system
     */
    @PostMapping("/migrate-patterns")
    public ResponseEntity<Map<String, Object>> migrateLegacyPatterns() {
        try {
            IntelligentAssignmentMigrationService.MigrationResult result = migrationService.migrateAllPatterns();
            
            Map<String, Object> response = new HashMap<>();
            response.put("migrated", result.getMigrated());
            response.put("failed", result.getFailed());
            response.put("total", result.getTotal());
            response.put("successRate", result.getSuccessRate() * 100);
            response.put("message", result.toString());
            response.put("success", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors de la migration: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get intelligent assignment statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getIntelligentAssignmentStats(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // Get transaction statistics
            long totalTransactions = transactionRepository.countByUser(user);
            long assignedTransactions = transactionRepository.countByUserAndDetailedCategoryIdIsNotNull(user);
            long needsReviewTransactions = transactionRepository.countByUserAndNeedsReviewTrue(user);
            long unassignedTransactions = totalTransactions - assignedTransactions;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTransactions", totalTransactions);
            stats.put("assignedTransactions", assignedTransactions);
            stats.put("needsReviewTransactions", needsReviewTransactions);
            stats.put("unassignedTransactions", unassignedTransactions);
            stats.put("assignmentRate", totalTransactions > 0 ? (double) assignedTransactions / totalTransactions : 0.0);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}