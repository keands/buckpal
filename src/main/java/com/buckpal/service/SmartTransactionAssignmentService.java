package com.buckpal.service;

import com.buckpal.entity.Category;
import com.buckpal.entity.MerchantPattern;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.entity.UserAssignmentFeedback;
import com.buckpal.repository.CategoryRepository;
import com.buckpal.repository.MerchantPatternRepository;
import com.buckpal.repository.UserAssignmentFeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class SmartTransactionAssignmentService {
    
    private final MerchantPatternRepository merchantPatternRepository;
    private final UserAssignmentFeedbackRepository feedbackRepository;
    private final IntelligentAssignmentMigrationService migrationService;
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public SmartTransactionAssignmentService(MerchantPatternRepository merchantPatternRepository,
                                           UserAssignmentFeedbackRepository feedbackRepository,
                                           IntelligentAssignmentMigrationService migrationService,
                                           CategoryRepository categoryRepository) {
        this.merchantPatternRepository = merchantPatternRepository;
        this.feedbackRepository = feedbackRepository;
        this.migrationService = migrationService;
        this.categoryRepository = categoryRepository;
    }
    
    public SmartAssignmentResult assignCategoryToTransaction(Transaction transaction, User user) {
        String merchantText = buildMerchantText(transaction);
        
        // Migrate any legacy patterns first
        migrationService.migrateAllPatterns();
        
        // Find matching patterns using specificity and confidence scoring
        List<MerchantPattern> matchingPatterns = findMatchingPatterns(merchantText);
        
        if (matchingPatterns.isEmpty()) {
            return new SmartAssignmentResult((Long) null, BigDecimal.ZERO, "NO_PATTERN_MATCH", Collections.emptyList());
        }
        
        // Apply conflict resolution
        PatternMatchResult bestMatch = resolvePatternConflicts(matchingPatterns, transaction, user);
        
        if (bestMatch == null) {
            return new SmartAssignmentResult((Long) null, BigDecimal.ZERO, "CONFLICT_RESOLUTION_FAILED", 
                getAlternativeCategoryIds(matchingPatterns));
        }
        
        // Return categoryId instead of categoryName
        Long categoryId = bestMatch.pattern.getCategoryId();
        if (categoryId == null) {
            // Fallback: try to convert categoryName to categoryId
            categoryId = migrationService.findCategoryIdByName(bestMatch.pattern.getCategoryName()).orElse(null);
        }
        
        return new SmartAssignmentResult(
            categoryId,
            bestMatch.finalConfidence,
            bestMatch.strategy,
            getAlternativeCategoryIds(matchingPatterns)
        );
    }
    
    private List<Long> getAlternativeCategoryIds(List<MerchantPattern> patterns) {
        return patterns.stream()
            .map(pattern -> {
                Long categoryId = pattern.getCategoryId();
                if (categoryId == null) {
                    // Fallback: convert categoryName to categoryId
                    return migrationService.findCategoryIdByName(pattern.getCategoryName()).orElse(null);
                }
                return categoryId;
            })
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }
    
    private String buildMerchantText(Transaction transaction) {
        StringBuilder merchantText = new StringBuilder();
        
        if (transaction.getMerchantName() != null && !transaction.getMerchantName().trim().isEmpty()) {
            merchantText.append(transaction.getMerchantName().trim().toUpperCase());
        }
        
        if (transaction.getDescription() != null && !transaction.getDescription().trim().isEmpty()) {
            String description = transaction.getDescription().trim().toUpperCase();
            if (merchantText.length() > 0 && !description.equals(merchantText.toString())) {
                merchantText.append(" ").append(description);
            } else if (merchantText.length() == 0) {
                merchantText.append(description);
            }
        }
        
        return merchantText.toString();
    }
    
    private List<MerchantPattern> findMatchingPatterns(String merchantText) {
        return merchantPatternRepository.findMatchingPatternsWithMinConfidence(
            merchantText, new BigDecimal("0.3")
        );
    }
    
    private PatternMatchResult resolvePatternConflicts(List<MerchantPattern> patterns, Transaction transaction, User user) {
        if (patterns.size() == 1) {
            MerchantPattern pattern = patterns.get(0);
            return new PatternMatchResult(pattern, pattern.getConfidenceScore(), "SINGLE_MATCH");
        }
        
        // Multiple patterns found - apply conflict resolution
        
        // Strategy 1: Specificity-weighted scoring
        PatternMatchResult specificityResult = resolveBySpecificity(patterns);
        if (specificityResult.finalConfidence.compareTo(new BigDecimal("0.8")) >= 0) {
            return specificityResult;
        }
        
        // Strategy 2: User feedback history
        PatternMatchResult feedbackResult = resolveByUserFeedback(patterns, user);
        if (feedbackResult != null && feedbackResult.finalConfidence.compareTo(new BigDecimal("0.75")) >= 0) {
            return feedbackResult;
        }
        
        // Strategy 3: Pattern accuracy history
        PatternMatchResult accuracyResult = resolveByAccuracyHistory(patterns);
        if (accuracyResult.finalConfidence.compareTo(new BigDecimal("0.7")) >= 0) {
            return accuracyResult;
        }
        
        // Strategy 4: Amount-based validation (if applicable)
        PatternMatchResult amountResult = resolveByAmountValidation(patterns, transaction);
        if (amountResult != null) {
            return amountResult;
        }
        
        // Fallback: Return highest specificity pattern with adjusted confidence
        MerchantPattern fallbackPattern = patterns.stream()
            .max(Comparator.comparing(MerchantPattern::getSpecificityScore)
                .thenComparing(MerchantPattern::getConfidenceScore))
            .orElse(null);
        
        if (fallbackPattern != null) {
            BigDecimal adjustedConfidence = fallbackPattern.getConfidenceScore()
                .multiply(new BigDecimal("0.6")); // Reduce confidence due to conflict
            return new PatternMatchResult(fallbackPattern, adjustedConfidence, "FALLBACK_SPECIFICITY");
        }
        
        return null;
    }
    
    private PatternMatchResult resolveBySpecificity(List<MerchantPattern> patterns) {
        // Calculate weighted score: (specificity * 0.6) + (confidence * 0.4)
        MerchantPattern bestPattern = patterns.stream()
            .max((p1, p2) -> {
                BigDecimal score1 = calculateSpecificityWeightedScore(p1);
                BigDecimal score2 = calculateSpecificityWeightedScore(p2);
                return score1.compareTo(score2);
            })
            .orElse(null);
        
        if (bestPattern != null) {
            BigDecimal weightedScore = calculateSpecificityWeightedScore(bestPattern);
            return new PatternMatchResult(bestPattern, weightedScore, "SPECIFICITY_WEIGHTED");
        }
        
        return null;
    }
    
    private BigDecimal calculateSpecificityWeightedScore(MerchantPattern pattern) {
        // Normalize specificity score (assume max is around 20)
        BigDecimal normalizedSpecificity = new BigDecimal(pattern.getSpecificityScore())
            .divide(new BigDecimal("20"), 4, RoundingMode.HALF_UP);
        
        return normalizedSpecificity.multiply(new BigDecimal("0.6"))
            .add(pattern.getConfidenceScore().multiply(new BigDecimal("0.4")));
    }
    
    private PatternMatchResult resolveByUserFeedback(List<MerchantPattern> patterns, User user) {
        Map<String, UserFeedbackStats> feedbackStats = calculateUserFeedbackStats(patterns, user);
        
        String bestCategory = feedbackStats.entrySet().stream()
            .filter(entry -> entry.getValue().totalFeedbacks >= 3) // Minimum feedback threshold
            .max((e1, e2) -> e1.getValue().acceptanceRate.compareTo(e2.getValue().acceptanceRate))
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (bestCategory != null) {
            MerchantPattern bestPattern = patterns.stream()
                .filter(p -> p.getCategoryName().equals(bestCategory))
                .findFirst()
                .orElse(null);
            
            if (bestPattern != null) {
                UserFeedbackStats stats = feedbackStats.get(bestCategory);
                BigDecimal feedbackConfidence = stats.acceptanceRate.multiply(new BigDecimal("0.9"))
                    .add(new BigDecimal("0.1")); // Base confidence
                return new PatternMatchResult(bestPattern, feedbackConfidence, "USER_FEEDBACK_HISTORY");
            }
        }
        
        return null;
    }
    
    private Map<String, UserFeedbackStats> calculateUserFeedbackStats(List<MerchantPattern> patterns, User user) {
        Map<String, UserFeedbackStats> stats = new HashMap<>();
        
        for (MerchantPattern pattern : patterns) {
            List<UserAssignmentFeedback> feedbacks = feedbackRepository
                .findByUserAndSuggestedCategory(user, pattern.getCategoryName());
            
            if (!feedbacks.isEmpty()) {
                long totalFeedbacks = feedbacks.size();
                long acceptedFeedbacks = feedbacks.stream()
                    .mapToLong(f -> f.getWasAccepted() ? 1 : 0)
                    .sum();
                
                BigDecimal acceptanceRate = new BigDecimal(acceptedFeedbacks)
                    .divide(new BigDecimal(totalFeedbacks), 4, RoundingMode.HALF_UP);
                
                stats.put(pattern.getCategoryName(), 
                    new UserFeedbackStats(totalFeedbacks, acceptanceRate));
            }
        }
        
        return stats;
    }
    
    private PatternMatchResult resolveByAccuracyHistory(List<MerchantPattern> patterns) {
        MerchantPattern bestAccuracyPattern = patterns.stream()
            .filter(p -> p.getTotalMatches() >= 5) // Minimum match threshold
            .max(Comparator.comparing(MerchantPattern::getAccuracyRate))
            .orElse(null);
        
        if (bestAccuracyPattern != null && 
            bestAccuracyPattern.getAccuracyRate().compareTo(new BigDecimal("0.7")) >= 0) {
            
            BigDecimal accuracyConfidence = bestAccuracyPattern.getAccuracyRate()
                .multiply(new BigDecimal("0.8"))
                .add(new BigDecimal("0.2"));
            
            return new PatternMatchResult(bestAccuracyPattern, accuracyConfidence, "ACCURACY_HISTORY");
        }
        
        return null;
    }
    
    private PatternMatchResult resolveByAmountValidation(List<MerchantPattern> patterns, Transaction transaction) {
        // This would validate against typical amount ranges for categories
        // For now, we'll implement a basic version
        
        BigDecimal amount = transaction.getAmount();
        final String bestCategory;
        final BigDecimal confidence;
        
        // Simple amount-based category validation
        if (amount.compareTo(new BigDecimal("5")) <= 0) {
            // Small amounts - likely transport or quick food
            bestCategory = findCategoryInPatterns(patterns, Arrays.asList("Transport", "Restaurant"));
            confidence = new BigDecimal("0.65");
        } else if (amount.compareTo(new BigDecimal("100")) >= 0) {
            // Large amounts - likely groceries, bills, or shopping
            bestCategory = findCategoryInPatterns(patterns, Arrays.asList("Alimentation", "Factures", "Vêtements"));
            confidence = new BigDecimal("0.6");
        } else {
            bestCategory = null;
            confidence = BigDecimal.ZERO;
        }
        
        if (bestCategory != null) {
            MerchantPattern bestPattern = patterns.stream()
                .filter(p -> p.getCategoryName().equals(bestCategory))
                .findFirst()
                .orElse(null);
            
            if (bestPattern != null) {
                return new PatternMatchResult(bestPattern, confidence, "AMOUNT_VALIDATION");
            }
        }
        
        return null;
    }
    
    private String findCategoryInPatterns(List<MerchantPattern> patterns, List<String> preferredCategories) {
        return patterns.stream()
            .map(MerchantPattern::getCategoryName)
            .filter(preferredCategories::contains)
            .findFirst()
            .orElse(null);
    }
    
    public void recordFeedback(Transaction transaction, User user, String suggestedCategory, 
                              String userChosenCategory, boolean wasAccepted, String patternUsed) {
        
        UserAssignmentFeedback feedback = new UserAssignmentFeedback(
            transaction, user, suggestedCategory, userChosenCategory, wasAccepted
        );
        feedback.setPatternMatched(patternUsed);
        feedbackRepository.save(feedback);
        
        // Update pattern statistics
        if (patternUsed != null) {
            Optional<MerchantPattern> patternOpt = merchantPatternRepository
                .findByPatternAndCategoryName(patternUsed, suggestedCategory);
            
            if (patternOpt.isPresent()) {
                MerchantPattern pattern = patternOpt.get();
                pattern.recordMatch(wasAccepted);
                merchantPatternRepository.save(pattern);
            }
        }
        
        // Learn new patterns if user chose a different category
        if (!wasAccepted && !suggestedCategory.equals(userChosenCategory)) {
            learnNewPattern(transaction, userChosenCategory);
        }
    }
    
    private void learnNewPattern(Transaction transaction, String categoryName) {
        String merchantText = buildMerchantText(transaction);
        
        // Extract most specific part of merchant text as new pattern
        List<String> potentialPatterns = extractPotentialPatterns(merchantText);
        
        for (String potentialPattern : potentialPatterns) {
            // Check if pattern already exists
            if (merchantPatternRepository.findByPatternAndCategoryName(potentialPattern, categoryName).isEmpty()) {
                // Calculate specificity based on pattern length and uniqueness
                int specificity = Math.min(potentialPattern.length(), 15);
                
                MerchantPattern newPattern = new MerchantPattern(potentialPattern, categoryName, specificity);
                newPattern.setConfidenceScore(new BigDecimal("0.5")); // Start with medium confidence
                merchantPatternRepository.save(newPattern);
                break; // Only create one new pattern per feedback
            }
        }
    }
    
    private List<String> extractPotentialPatterns(String merchantText) {
        List<String> patterns = new ArrayList<>();
        
        // Remove common noise words
        String cleaned = merchantText.replaceAll("\\b(DE|DU|LA|LE|LES|ET|POUR|AVEC|SANS)\\b", " ")
                                   .replaceAll("\\s+", " ")
                                   .trim();
        
        String[] words = cleaned.split("\\s+");
        
        // Single significant words (length >= 4)
        for (String word : words) {
            if (word.length() >= 4 && !isCommonWord(word)) {
                patterns.add(word);
            }
        }
        
        // Two-word combinations
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].length() >= 3 && words[i + 1].length() >= 3) {
                patterns.add(words[i] + " " + words[i + 1]);
            }
        }
        
        // Sort by specificity (longer patterns first)
        patterns.sort((p1, p2) -> Integer.compare(p2.length(), p1.length()));
        
        return patterns.stream().limit(3).collect(Collectors.toList()); // Limit to top 3 patterns
    }
    
    private boolean isCommonWord(String word) {
        Set<String> commonWords = Set.of(
            "ACHAT", "VENTE", "PAIEMENT", "CARTE", "CB", "RETRAIT", "VIREMENT", 
            "PRELEVEMENT", "DEPOT", "SOLDE", "CREDIT", "DEBIT", "FRANCE", "PARIS",
            "SUCCURSALE", "AGENCE", "SERVICE", "CLIENT", "NUMERO", "DATE"
        );
        return commonWords.contains(word);
    }
    
    /**
     * Learn from user assignment to create or update patterns
     */
    public void learnFromAssignment(Transaction transaction, Long categoryId, boolean wasCorrect) {
        String merchantText = buildMerchantText(transaction);
        
        // Find or create pattern
        Optional<MerchantPattern> existingPattern = merchantPatternRepository.findByPatternAndCategoryId(merchantText, categoryId);
        
        MerchantPattern pattern;
        if (existingPattern.isPresent()) {
            pattern = existingPattern.get();
        } else {
            // Create new pattern
            pattern = new MerchantPattern(merchantText, categoryId, calculateSpecificityScore(merchantText));
        }
        
        // Record the match result
        pattern.recordMatch(wasCorrect);
        merchantPatternRepository.save(pattern);
    }
    
    /**
     * Batch learn from multiple user assignments
     */
    public void batchLearnFromAssignments(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            if (transaction.getDetailedCategoryId() != null) {
                // Learn from this assignment (assume it's correct since user did it)
                learnFromAssignment(transaction, transaction.getDetailedCategoryId(), true);
            }
        }
    }
    
    /**
     * Calculate specificity score based on pattern length and word count
     */
    private Integer calculateSpecificityScore(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return 1;
        }
        
        String[] words = pattern.trim().split("\\s+");
        int wordCount = words.length;
        int totalLength = pattern.length();
        
        // Base score on word count (more words = more specific)
        int score = wordCount * 10;
        
        // Add length bonus (longer patterns are generally more specific)
        if (totalLength > 20) score += 5;
        if (totalLength > 40) score += 5;
        
        // Bonus for specific merchant indicators
        String upperPattern = pattern.toUpperCase();
        if (upperPattern.contains("SARL") || upperPattern.contains("SAS") || 
            upperPattern.contains("EURL") || upperPattern.contains("SA ")) {
            score += 15; // Company indicators are very specific
        }
        
        return Math.min(score, 100); // Cap at 100
    }
    
    // Result classes
    public static class SmartAssignmentResult {
        public final Long categoryId;
        public final String categoryName; // Keep for backward compatibility
        public final BigDecimal confidence;
        public final String strategy;
        public final List<Long> alternativeCategoryIds;
        public final List<String> alternativeCategories; // Keep for backward compatibility
        
        public SmartAssignmentResult(Long categoryId, BigDecimal confidence, 
                                   String strategy, List<Long> alternativeCategoryIds) {
            this.categoryId = categoryId;
            this.categoryName = null; // Legacy field
            this.confidence = confidence;
            this.strategy = strategy;
            this.alternativeCategoryIds = alternativeCategoryIds;
            this.alternativeCategories = Collections.emptyList(); // Legacy field
        }
        
        // Legacy constructor for backward compatibility
        public SmartAssignmentResult(String categoryName, BigDecimal confidence, 
                                   String strategy, List<String> alternativeCategories) {
            this.categoryId = null;
            this.categoryName = categoryName;
            this.confidence = confidence;
            this.strategy = strategy;
            this.alternativeCategoryIds = Collections.emptyList();
            this.alternativeCategories = alternativeCategories;
        }
    }
    
    private static class PatternMatchResult {
        public final MerchantPattern pattern;
        public final BigDecimal finalConfidence;
        public final String strategy;
        
        public PatternMatchResult(MerchantPattern pattern, BigDecimal finalConfidence, String strategy) {
            this.pattern = pattern;
            this.finalConfidence = finalConfidence;
            this.strategy = strategy;
        }
    }
    
    private static class UserFeedbackStats {
        public final long totalFeedbacks;
        public final BigDecimal acceptanceRate;
        
        public UserFeedbackStats(long totalFeedbacks, BigDecimal acceptanceRate) {
            this.totalFeedbacks = totalFeedbacks;
            this.acceptanceRate = acceptanceRate;
        }
    }
}