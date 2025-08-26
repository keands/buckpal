package com.buckpal.service;

import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.entity.Transaction.AssignmentStatus;
import com.buckpal.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionRevisionService {
    
    private final TransactionRepository transactionRepository;
    
    @Autowired
    public TransactionRevisionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Detect transactions that need revision based on confidence and assignment quality
     */
    public List<Transaction> detectTransactionsNeedingRevision(User user) {
        List<Transaction> allUserTransactions = transactionRepository.findByUser(user);
        
        return allUserTransactions.stream()
            .filter(this::shouldReviseTransaction)
            .collect(Collectors.toList());
    }
    
    /**
     * Get recently assigned transactions (last 24 hours) that can be revised
     */
    public List<Transaction> getRecentlyAssignedTransactions(User user) {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        
        return transactionRepository.findByUser(user).stream()
            .filter(t -> t.getUpdatedAt() != null && t.getUpdatedAt().isAfter(yesterday))
            .filter(t -> t.getAssignmentStatus() == AssignmentStatus.AUTO_ASSIGNED || 
                        t.getAssignmentStatus() == AssignmentStatus.RECENTLY_ASSIGNED)
            .collect(Collectors.toList());
    }
    
    /**
     * Mark transactions for revision (put back in NEEDS_REVIEW status)
     */
    public void markForRevision(List<Long> transactionIds, User user) {
        for (Long transactionId : transactionIds) {
            transactionRepository.findByIdAndUser(transactionId, user)
                .ifPresent(transaction -> {
                    transaction.setAssignmentStatus(AssignmentStatus.NEEDS_REVIEW);
                    transactionRepository.save(transaction);
                });
        }
    }
    
    /**
     * Mark a single transaction for revision
     */
    public void markForRevision(Long transactionId, User user) {
        transactionRepository.findByIdAndUser(transactionId, user)
            .ifPresent(transaction -> {
                transaction.setAssignmentStatus(AssignmentStatus.NEEDS_REVIEW);
                transactionRepository.save(transaction);
            });
    }
    
    /**
     * Automatically detect and mark suspicious assignments for revision
     */
    public RevisionResult autoDetectAndMarkForRevision(User user) {
        List<Transaction> suspiciousTransactions = detectTransactionsNeedingRevision(user);
        
        int markedCount = 0;
        for (Transaction transaction : suspiciousTransactions) {
            if (shouldAutoMarkForRevision(transaction)) {
                transaction.setAssignmentStatus(AssignmentStatus.NEEDS_REVIEW);
                transactionRepository.save(transaction);
                markedCount++;
            }
        }
        
        return new RevisionResult(
            suspiciousTransactions.size(),
            markedCount,
            suspiciousTransactions.stream()
                .filter(this::shouldAutoMarkForRevision)
                .collect(Collectors.toList())
        );
    }
    
    /**
     * Check if a transaction should be revised based on various criteria
     */
    private boolean shouldReviseTransaction(Transaction transaction) {
        // Skip if already needs review or unassigned
        if (transaction.getAssignmentStatus() == AssignmentStatus.NEEDS_REVIEW ||
            transaction.getAssignmentStatus() == AssignmentStatus.UNASSIGNED) {
            return false;
        }
        
        // Check for suspicious patterns that indicate low-quality assignment
        return hasLowConfidenceIndicators(transaction) ||
               hasInconsistentCategorization(transaction) ||
               hasRecentAssignmentWithoutUserValidation(transaction);
    }
    
    /**
     * Check if transaction should be automatically marked for revision
     */
    private boolean shouldAutoMarkForRevision(Transaction transaction) {
        // Only auto-mark AUTO_ASSIGNED transactions, preserve manual assignments
        if (transaction.getAssignmentStatus() != AssignmentStatus.AUTO_ASSIGNED &&
            transaction.getAssignmentStatus() != AssignmentStatus.RECENTLY_ASSIGNED) {
            return false;
        }
        
        // Auto-mark if confidence indicators are very low
        return hasVeryLowConfidenceIndicators(transaction);
    }
    
    /**
     * Detect low confidence indicators in transaction assignment
     */
    private boolean hasLowConfidenceIndicators(Transaction transaction) {
        // Check for generic category assignments that might be wrong
        if (transaction.getBudgetCategory() != null) {
            String categoryName = transaction.getBudgetCategory().getName().toLowerCase();
            
            // Generic categories often indicate uncertain assignments
            if (categoryName.contains("divers") || 
                categoryName.contains("autre") || 
                categoryName.contains("général") ||
                categoryName.contains("miscellaneous") ||
                categoryName.contains("other")) {
                return true;
            }
        }
        
        // Check for inconsistent merchant/category combinations
        String description = (transaction.getDescription() != null ? 
            transaction.getDescription() : "").toLowerCase();
        String merchantName = (transaction.getMerchantName() != null ? 
            transaction.getMerchantName() : "").toLowerCase();
        
        // Example: Restaurant pattern but assigned to Transport
        if ((description.contains("restaurant") || merchantName.contains("restaurant") ||
             description.contains("café") || merchantName.contains("café")) &&
            transaction.getBudgetCategory() != null &&
            transaction.getBudgetCategory().getName().toLowerCase().contains("transport")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Detect very low confidence indicators for auto-marking
     */
    private boolean hasVeryLowConfidenceIndicators(Transaction transaction) {
        // More strict criteria for automatic revision
        if (transaction.getBudgetCategory() != null) {
            String categoryName = transaction.getBudgetCategory().getName().toLowerCase();
            
            // Only auto-mark truly generic categories
            return categoryName.equals("divers") || 
                   categoryName.equals("autre") || 
                   categoryName.equals("other") ||
                   categoryName.equals("miscellaneous");
        }
        
        return false;
    }
    
    /**
     * Check for inconsistent categorization patterns
     */
    private boolean hasInconsistentCategorization(Transaction transaction) {
        // This would compare with similar transactions from the same merchant
        // For now, implement basic logic
        
        if (transaction.getMerchantName() == null || transaction.getBudgetCategory() == null) {
            return false;
        }
        
        // Find other transactions from same merchant
        // If most are in different category, this might be inconsistent
        // This is a simplified implementation - could be enhanced with ML
        
        return false; // Placeholder for now
    }
    
    /**
     * Check if transaction was recently assigned without user validation
     */
    private boolean hasRecentAssignmentWithoutUserValidation(Transaction transaction) {
        // Check if assigned in last 24 hours and status is AUTO_ASSIGNED
        if (transaction.getUpdatedAt() == null) {
            return false;
        }
        
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        boolean isRecent = transaction.getUpdatedAt().isAfter(yesterday);
        boolean isAutoAssigned = transaction.getAssignmentStatus() == AssignmentStatus.AUTO_ASSIGNED;
        
        return isRecent && isAutoAssigned;
    }
    
    // Result class for revision operations
    public static class RevisionResult {
        private final int totalSuspicious;
        private final int markedForRevision;
        private final List<Transaction> revisedTransactions;
        
        public RevisionResult(int totalSuspicious, int markedForRevision, List<Transaction> revisedTransactions) {
            this.totalSuspicious = totalSuspicious;
            this.markedForRevision = markedForRevision;
            this.revisedTransactions = revisedTransactions;
        }
        
        public int getTotalSuspicious() { return totalSuspicious; }
        public int getMarkedForRevision() { return markedForRevision; }
        public List<Transaction> getRevisedTransactions() { return revisedTransactions; }
    }
}