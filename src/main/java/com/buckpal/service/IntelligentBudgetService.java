package com.buckpal.service;

import com.buckpal.entity.*;
import com.buckpal.entity.IncomeCategory.IncomeType;
import com.buckpal.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent Budget Service for analyzing patterns and providing smart suggestions
 */
@Service
@Transactional(readOnly = true)
public class IntelligentBudgetService {
    
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final IncomeCategoryRepository incomeCategoryRepository;
    
    @Autowired
    public IntelligentBudgetService(BudgetRepository budgetRepository,
                                   TransactionRepository transactionRepository,
                                   IncomeCategoryRepository incomeCategoryRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.incomeCategoryRepository = incomeCategoryRepository;
    }
    
    /**
     * Analyze income patterns from user's previous budgets
     */
    public List<IncomePattern> analyzeIncomePatterns(User user, int analysisMonthsBack) {
        // Get user's budgets from the last X months
        List<Budget> recentBudgets = getUserRecentBudgets(user, analysisMonthsBack);
        
        if (recentBudgets.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, PatternAnalysis> patternMap = new HashMap<>();
        
        // Analyze each budget's income transactions
        for (Budget budget : recentBudgets) {
            List<IncomeCategory> incomeCategories = new ArrayList<>(budget.getIncomeCategories());
            
            for (IncomeCategory category : incomeCategories) {
                // Get transactions for this category
                List<Transaction> transactions = transactionRepository.findByIncomeCategory(category);
                
                for (Transaction transaction : transactions) {
                    String description = normalizeDescription(transaction.getDescription());
                    
                    PatternAnalysis pattern = patternMap.computeIfAbsent(description, 
                        k -> new PatternAnalysis(description));
                    
                    pattern.addOccurrence(category, transaction.getAmount());
                }
            }
        }
        
        // Convert to IncomePattern objects and sort by confidence
        return patternMap.values().stream()
                .filter(p -> p.getOccurrenceCount() >= 1) // Learn from first categorization
                .map(this::convertToIncomePattern)
                .sorted((a, b) -> Double.compare(b.getConfidenceScore(), a.getConfidenceScore()))
                .limit(20) // Top 20 patterns
                .collect(Collectors.toList());
    }
    
    /**
     * Get intelligent suggestions for income categorization
     */
    public List<IncomeSuggestion> suggestIncomeCategories(User user, String transactionDescription) {
        List<IncomePattern> patterns = analyzeIncomePatterns(user, 12); // Last 12 months
        
        String normalizedDescription = normalizeDescription(transactionDescription);
        
        List<IncomeSuggestion> historicalSuggestions = patterns.stream()
                .filter(pattern -> isDescriptionMatch(pattern.getTransactionPattern(), normalizedDescription))
                .map(pattern -> new IncomeSuggestion(
                    pattern.getMostLikelyCategoryName(),
                    pattern.getMostLikelyCategoryType(),
                    pattern.getConfidenceScore(),
                    pattern.getAverageAmount(),
                    pattern.getOccurrenceCount() == 1 ? 
                        "Basé sur votre dernière catégorisation de cette transaction" : 
                        "Basé sur " + pattern.getOccurrenceCount() + " transactions similaires"
                ))
                .sorted((a, b) -> Double.compare(b.getConfidenceScore(), a.getConfidenceScore()))
                .collect(Collectors.toList());
        
        // If no historical suggestions, add keyword-based suggestions
        if (historicalSuggestions.isEmpty()) {
            historicalSuggestions.addAll(getKeywordBasedSuggestions(normalizedDescription));
        }
        
        return historicalSuggestions.stream()
                .limit(3) // Top 3 suggestions
                .collect(Collectors.toList());
    }
    
    private List<IncomeSuggestion> getKeywordBasedSuggestions(String description) {
        List<IncomeSuggestion> suggestions = new ArrayList<>();
        
        // Define common income patterns
        Map<String, SuggestedCategory> commonPatterns = Map.of(
            "salaire|salary|paie|virement", new SuggestedCategory("Salaire", IncomeType.SALARY, "Transaction ressemble à un salaire"),
            "freelance|consultant|honoraires|prestation", new SuggestedCategory("Freelance", IncomeType.BUSINESS, "Transaction ressemble à du freelance"),
            "dividende|dividend|investissement", new SuggestedCategory("Investissements", IncomeType.INVESTMENT, "Transaction ressemble à des revenus d'investissement"),
            "loyer|rental|location", new SuggestedCategory("Revenus locatifs", IncomeType.OTHER, "Transaction ressemble à des revenus locatifs"),
            "bonus|prime|commission", new SuggestedCategory("Bonus", IncomeType.SALARY, "Transaction ressemble à un bonus ou prime")
        );
        
        for (Map.Entry<String, SuggestedCategory> entry : commonPatterns.entrySet()) {
            String[] keywords = entry.getKey().split("\\|");
            SuggestedCategory category = entry.getValue();
            
            if (Arrays.stream(keywords).anyMatch(keyword -> description.contains(keyword))) {
                suggestions.add(new IncomeSuggestion(
                    category.name,
                    category.type,
                    0.7, // Moderate confidence for keyword matching
                    BigDecimal.ZERO, // No historical amount
                    category.reasoning
                ));
            }
        }
        
        return suggestions;
    }
    
    private static class SuggestedCategory {
        final String name;
        final IncomeType type;
        final String reasoning;
        
        SuggestedCategory(String name, IncomeType type, String reasoning) {
            this.name = name;
            this.type = type;
            this.reasoning = reasoning;
        }
    }
    
    /**
     * Get smart budget template based on user's income history
     */
    public SmartBudgetTemplate generateSmartBudgetTemplate(User user) {
        List<Budget> recentBudgets = getUserRecentBudgets(user, 6); // Last 6 months
        
        if (recentBudgets.isEmpty()) {
            return new SmartBudgetTemplate(Collections.emptyList(), BigDecimal.ZERO);
        }
        
        Map<String, CategorySummary> categorySummaries = new HashMap<>();
        
        for (Budget budget : recentBudgets) {
            for (IncomeCategory category : budget.getIncomeCategories()) {
                CategorySummary summary = categorySummaries.computeIfAbsent(
                    category.getName(), k -> new CategorySummary(category));
                
                summary.addBudgetData(category.getBudgetedAmount(), category.getActualAmount());
            }
        }
        
        // Create suggested categories with average amounts
        List<SuggestedIncomeCategory> suggestedCategories = categorySummaries.values().stream()
                .filter(summary -> summary.getOccurrenceCount() >= 2) // Appears in at least 2 budgets
                .map(this::convertToSuggestedCategory)
                .sorted((a, b) -> a.getCategoryName().compareToIgnoreCase(b.getCategoryName()))
                .collect(Collectors.toList());
        
        BigDecimal totalSuggestedIncome = suggestedCategories.stream()
                .map(SuggestedIncomeCategory::getSuggestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new SmartBudgetTemplate(suggestedCategories, totalSuggestedIncome);
    }
    
    // === Helper Methods ===
    
    private List<Budget> getUserRecentBudgets(User user, int monthsBack) {
        LocalDate cutoffDate = LocalDate.now().minusMonths(monthsBack);
        
        return budgetRepository.findByUserOrderByBudgetYearDescBudgetMonthDesc(user).stream()
                .filter(budget -> {
                    LocalDate budgetDate = LocalDate.of(budget.getBudgetYear(), budget.getBudgetMonth(), 1);
                    return budgetDate.isAfter(cutoffDate);
                })
                .sorted((a, b) -> {
                    LocalDate dateA = LocalDate.of(a.getBudgetYear(), a.getBudgetMonth(), 1);
                    LocalDate dateB = LocalDate.of(b.getBudgetYear(), b.getBudgetMonth(), 1);
                    return dateB.compareTo(dateA); // Most recent first
                })
                .collect(Collectors.toList());
    }
    
    private String normalizeDescription(String description) {
        if (description == null) return "";
        
        String normalized = description.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", "") // Remove special characters
                .replaceAll("\\s+", " "); // Normalize spaces
        
        return normalized.substring(0, Math.min(normalized.length(), 50)); // Limit length
    }
    
    private boolean isDescriptionMatch(String pattern, String description) {
        String[] patternWords = pattern.split("\\s+");
        String[] descWords = description.split("\\s+");
        
        // Enhanced matching: more flexible similarity detection
        long matchCount = Arrays.stream(patternWords)
                .filter(word -> word.length() > 2) // Skip short words
                .filter(word -> Arrays.stream(descWords).anyMatch(w -> 
                    w.contains(word) || word.contains(w) || 
                    calculateSimilarity(word, w) > 0.8)) // Add fuzzy matching
                .count();
        
        // Lower threshold for better matching (30% instead of 50%)
        return matchCount >= Math.max(1, patternWords.length * 0.3);
    }
    
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.length() < 3 || s2.length() < 3) return 0.0;
        
        // Simple Levenshtein-based similarity
        int maxLen = Math.max(s1.length(), s2.length());
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLen;
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i-1][j], dp[i][j-1]), dp[i-1][j-1]);
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }
    
    private IncomePattern convertToIncomePattern(PatternAnalysis analysis) {
        return new IncomePattern(
            analysis.getPattern(),
            analysis.getMostFrequentCategory().getName(),
            analysis.getMostFrequentCategory().getIncomeType(),
            analysis.calculateConfidenceScore(),
            analysis.getAverageAmount(),
            analysis.getOccurrenceCount()
        );
    }
    
    private SuggestedIncomeCategory convertToSuggestedCategory(CategorySummary summary) {
        return new SuggestedIncomeCategory(
            summary.getCategoryName(),
            summary.getIncomeType(),
            summary.getAverageActualAmount(),
            summary.getAverageBudgetedAmount(),
            summary.getOccurrenceCount(),
            summary.getColor(),
            summary.getIcon()
        );
    }
    
    // === Inner Classes for Data Transfer ===
    
    public static class IncomePattern {
        private final String transactionPattern;
        private final String mostLikelyCategoryName;
        private final IncomeCategory.IncomeType mostLikelyCategoryType;
        private final double confidenceScore;
        private final BigDecimal averageAmount;
        private final int occurrenceCount;
        
        public IncomePattern(String transactionPattern, String categoryName, 
                           IncomeCategory.IncomeType categoryType, double confidenceScore,
                           BigDecimal averageAmount, int occurrenceCount) {
            this.transactionPattern = transactionPattern;
            this.mostLikelyCategoryName = categoryName;
            this.mostLikelyCategoryType = categoryType;
            this.confidenceScore = confidenceScore;
            this.averageAmount = averageAmount;
            this.occurrenceCount = occurrenceCount;
        }
        
        // Getters
        public String getTransactionPattern() { return transactionPattern; }
        public String getMostLikelyCategoryName() { return mostLikelyCategoryName; }
        public IncomeCategory.IncomeType getMostLikelyCategoryType() { return mostLikelyCategoryType; }
        public double getConfidenceScore() { return confidenceScore; }
        public BigDecimal getAverageAmount() { return averageAmount; }
        public int getOccurrenceCount() { return occurrenceCount; }
    }
    
    public static class IncomeSuggestion {
        private final String categoryName;
        private final IncomeCategory.IncomeType categoryType;
        private final double confidenceScore;
        private final BigDecimal suggestedAmount;
        private final String reasoning;
        
        public IncomeSuggestion(String categoryName, IncomeCategory.IncomeType categoryType,
                               double confidenceScore, BigDecimal suggestedAmount, String reasoning) {
            this.categoryName = categoryName;
            this.categoryType = categoryType;
            this.confidenceScore = confidenceScore;
            this.suggestedAmount = suggestedAmount;
            this.reasoning = reasoning;
        }
        
        // Getters
        public String getCategoryName() { return categoryName; }
        public IncomeCategory.IncomeType getCategoryType() { return categoryType; }
        public double getConfidenceScore() { return confidenceScore; }
        public BigDecimal getSuggestedAmount() { return suggestedAmount; }
        public String getReasoning() { return reasoning; }
    }
    
    public static class SmartBudgetTemplate {
        private final List<SuggestedIncomeCategory> suggestedCategories;
        private final BigDecimal totalSuggestedIncome;
        
        public SmartBudgetTemplate(List<SuggestedIncomeCategory> categories, BigDecimal totalIncome) {
            this.suggestedCategories = categories;
            this.totalSuggestedIncome = totalIncome;
        }
        
        // Getters
        public List<SuggestedIncomeCategory> getSuggestedCategories() { return suggestedCategories; }
        public BigDecimal getTotalSuggestedIncome() { return totalSuggestedIncome; }
    }
    
    public static class SuggestedIncomeCategory {
        private final String categoryName;
        private final IncomeCategory.IncomeType incomeType;
        private final BigDecimal suggestedAmount;
        private final BigDecimal averageBudgetedAmount;
        private final int historicalOccurrences;
        private final String color;
        private final String icon;
        
        public SuggestedIncomeCategory(String categoryName, IncomeCategory.IncomeType incomeType,
                                     BigDecimal suggestedAmount, BigDecimal averageBudgetedAmount,
                                     int historicalOccurrences, String color, String icon) {
            this.categoryName = categoryName;
            this.incomeType = incomeType;
            this.suggestedAmount = suggestedAmount;
            this.averageBudgetedAmount = averageBudgetedAmount;
            this.historicalOccurrences = historicalOccurrences;
            this.color = color;
            this.icon = icon;
        }
        
        // Getters
        public String getCategoryName() { return categoryName; }
        public IncomeCategory.IncomeType getIncomeType() { return incomeType; }
        public BigDecimal getSuggestedAmount() { return suggestedAmount; }
        public BigDecimal getAverageBudgetedAmount() { return averageBudgetedAmount; }
        public int getHistoricalOccurrences() { return historicalOccurrences; }
        public String getColor() { return color; }
        public String getIcon() { return icon; }
    }
    
    // === Internal Analysis Classes ===
    
    private static class PatternAnalysis {
        private final String pattern;
        private final Map<IncomeCategory, Integer> categoryOccurrences = new HashMap<>();
        private final Map<IncomeCategory, BigDecimal> categoryAmounts = new HashMap<>();
        
        public PatternAnalysis(String pattern) {
            this.pattern = pattern;
        }
        
        public void addOccurrence(IncomeCategory category, BigDecimal amount) {
            categoryOccurrences.merge(category, 1, Integer::sum);
            categoryAmounts.merge(category, amount, BigDecimal::add);
        }
        
        public IncomeCategory getMostFrequentCategory() {
            return categoryOccurrences.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }
        
        public double calculateConfidenceScore() {
            if (categoryOccurrences.isEmpty()) return 0.0;
            
            int totalOccurrences = categoryOccurrences.values().stream().mapToInt(Integer::intValue).sum();
            int maxCategoryCount = categoryOccurrences.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            
            return (double) maxCategoryCount / totalOccurrences;
        }
        
        public BigDecimal getAverageAmount() {
            IncomeCategory mostFrequent = getMostFrequentCategory();
            if (mostFrequent == null) return BigDecimal.ZERO;
            
            BigDecimal totalAmount = categoryAmounts.get(mostFrequent);
            int occurrences = categoryOccurrences.get(mostFrequent);
            
            return totalAmount.divide(BigDecimal.valueOf(occurrences), 2, BigDecimal.ROUND_HALF_UP);
        }
        
        public int getOccurrenceCount() {
            return categoryOccurrences.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        public String getPattern() { return pattern; }
    }
    
    private static class CategorySummary {
        private final String categoryName;
        private final IncomeCategory.IncomeType incomeType;
        private final String color;
        private final String icon;
        private BigDecimal totalBudgetedAmount = BigDecimal.ZERO;
        private BigDecimal totalActualAmount = BigDecimal.ZERO;
        private int occurrenceCount = 0;
        
        public CategorySummary(IncomeCategory category) {
            this.categoryName = category.getName();
            this.incomeType = category.getIncomeType();
            this.color = category.getColor();
            this.icon = category.getIcon();
        }
        
        public void addBudgetData(BigDecimal budgeted, BigDecimal actual) {
            if (budgeted != null) this.totalBudgetedAmount = this.totalBudgetedAmount.add(budgeted);
            if (actual != null) this.totalActualAmount = this.totalActualAmount.add(actual);
            this.occurrenceCount++;
        }
        
        public BigDecimal getAverageBudgetedAmount() {
            return occurrenceCount > 0 ? 
                totalBudgetedAmount.divide(BigDecimal.valueOf(occurrenceCount), 2, BigDecimal.ROUND_HALF_UP) : 
                BigDecimal.ZERO;
        }
        
        public BigDecimal getAverageActualAmount() {
            return occurrenceCount > 0 ? 
                totalActualAmount.divide(BigDecimal.valueOf(occurrenceCount), 2, BigDecimal.ROUND_HALF_UP) : 
                BigDecimal.ZERO;
        }
        
        // Getters
        public String getCategoryName() { return categoryName; }
        public IncomeCategory.IncomeType getIncomeType() { return incomeType; }
        public String getColor() { return color; }
        public String getIcon() { return icon; }
        public int getOccurrenceCount() { return occurrenceCount; }
    }
}