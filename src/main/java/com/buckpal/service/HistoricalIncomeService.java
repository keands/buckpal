package com.buckpal.service;

import com.buckpal.entity.Budget;
import com.buckpal.entity.IncomeCategory;
import com.buckpal.entity.IncomeTransaction;
import com.buckpal.entity.User;
import com.buckpal.repository.BudgetRepository;
import com.buckpal.repository.IncomeCategoryRepository;
import com.buckpal.repository.IncomeTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistoricalIncomeService {
    
    private final BudgetRepository budgetRepository;
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final IncomeTransactionRepository incomeTransactionRepository;
    
    @Autowired
    public HistoricalIncomeService(BudgetRepository budgetRepository,
                                  IncomeCategoryRepository incomeCategoryRepository,
                                  IncomeTransactionRepository incomeTransactionRepository) {
        this.budgetRepository = budgetRepository;
        this.incomeCategoryRepository = incomeCategoryRepository;
        this.incomeTransactionRepository = incomeTransactionRepository;
    }
    
    /**
     * Detect and analyze historical income for a user
     * Used when creating budgets for past months
     */
    public HistoricalIncomeAnalysis analyzeHistoricalIncome(User user, Integer targetMonth, Integer targetYear) {
        // Get all income transactions for the target month
        List<IncomeTransaction> historicalTransactions = incomeTransactionRepository
                .findByUserAndMonth(user, targetMonth, targetYear);
        
        if (historicalTransactions.isEmpty()) {
            return new HistoricalIncomeAnalysis(false, BigDecimal.ZERO, new ArrayList<>(), new ArrayList<>());
        }
        
        // Calculate total historical income
        BigDecimal totalHistoricalIncome = historicalTransactions.stream()
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Group transactions by pattern/source for categorization
        List<IncomePattern> patterns = analyzeIncomePatterns(historicalTransactions);
        
        // Suggest income categories based on patterns
        List<IncomeCategorySuggestion> suggestions = suggestIncomeCategoriesFromPatterns(patterns);
        
        return new HistoricalIncomeAnalysis(true, totalHistoricalIncome, patterns, suggestions);
    }
    
    /**
     * Check if a budget month should have historical income detection
     */
    public boolean shouldDetectHistoricalIncome(User user, Budget budget) {
        // Check if actualIncome is 0 (condition from specifications)
        if (budget.getActualIncome().compareTo(BigDecimal.ZERO) != 0) {
            return false;
        }
        
        // Check if there are income transactions for that period
        List<IncomeTransaction> transactions = incomeTransactionRepository
                .findByUserAndMonth(user, budget.getBudgetMonth(), budget.getBudgetYear());
        
        return !transactions.isEmpty();
    }
    
    /**
     * Apply historical income to budget categories
     */
    public void applyHistoricalIncome(User user, Budget budget, List<IncomeCategorySuggestion> suggestions, 
                                     boolean usePartialIncome) {
        
        for (IncomeCategorySuggestion suggestion : suggestions) {
            // Find or create income category
            IncomeCategory category = findOrCreateIncomeCategory(budget, suggestion);
            
            if (!usePartialIncome) {
                // Use full suggested amount
                category.setBudgetedAmount(suggestion.getSuggestedAmount());
                category.setActualAmount(suggestion.getActualAmount());
            } else {
                // User can override with partial amounts - keep budgeted as is
                category.setActualAmount(suggestion.getActualAmount());
            }
            
            incomeCategoryRepository.save(category);
        }
        
        // Update budget totals
        updateBudgetIncomeFromCategories(budget);
    }
    
    /**
     * Get income comparison with previous months
     */
    public IncomeComparison getIncomeComparison(User user, Integer targetMonth, Integer targetYear, int monthsBack) {
        List<MonthlyIncomeData> monthlyData = new ArrayList<>();
        
        // Current target month
        BigDecimal targetIncome = incomeTransactionRepository.getTotalIncomeForMonth(user, targetMonth, targetYear);
        monthlyData.add(new MonthlyIncomeData(targetMonth, targetYear, targetIncome));
        
        // Previous months
        LocalDate currentDate = LocalDate.of(targetYear, targetMonth, 1);
        for (int i = 1; i <= monthsBack; i++) {
            LocalDate previousMonth = currentDate.minusMonths(i);
            BigDecimal income = incomeTransactionRepository.getTotalIncomeForMonth(
                    user, previousMonth.getMonthValue(), previousMonth.getYear());
            monthlyData.add(new MonthlyIncomeData(previousMonth.getMonthValue(), previousMonth.getYear(), income));
        }
        
        // Calculate statistics
        BigDecimal averageIncome = monthlyData.stream()
                .skip(1) // Skip target month
                .map(MonthlyIncomeData::getTotalIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(monthsBack), 2, RoundingMode.HALF_UP);
        
        BigDecimal variance = targetIncome.subtract(averageIncome);
        BigDecimal variancePercentage = averageIncome.compareTo(BigDecimal.ZERO) > 0 
                ? variance.divide(averageIncome, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        
        return new IncomeComparison(monthlyData, averageIncome, variance, variancePercentage);
    }
    
    /**
     * Find income patterns from similar periods
     */
    public List<IncomePattern> findRecurringIncomePatterns(User user, Integer targetMonth, Integer targetYear) {
        // Look for same month in previous years
        List<IncomeTransaction> sameMonthPreviousYears = new ArrayList<>();
        for (int year = targetYear - 3; year < targetYear; year++) {
            sameMonthPreviousYears.addAll(
                    incomeTransactionRepository.findByUserAndMonth(user, targetMonth, year));
        }
        
        return analyzeIncomePatterns(sameMonthPreviousYears);
    }
    
    // ====== PRIVATE HELPER METHODS ======
    
    private List<IncomePattern> analyzeIncomePatterns(List<IncomeTransaction> transactions) {
        Map<String, List<IncomeTransaction>> groupedBySource = transactions.stream()
                .collect(Collectors.groupingBy(t -> normalizeIncomeSource(t.getDescription())));
        
        return groupedBySource.entrySet().stream()
                .map(entry -> {
                    String pattern = entry.getKey();
                    List<IncomeTransaction> transactionGroup = entry.getValue();
                    
                    BigDecimal totalAmount = transactionGroup.stream()
                            .map(IncomeTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal averageAmount = totalAmount.divide(
                            BigDecimal.valueOf(transactionGroup.size()), 2, RoundingMode.HALF_UP);
                    
                    IncomeCategory.IncomeType suggestedType = inferIncomeType(pattern, averageAmount);
                    
                    return new IncomePattern(pattern, transactionGroup.size(), totalAmount, 
                                           averageAmount, suggestedType, transactionGroup);
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());
    }
    
    private List<IncomeCategorySuggestion> suggestIncomeCategoriesFromPatterns(List<IncomePattern> patterns) {
        return patterns.stream()
                .map(pattern -> {
                    String categoryName = generateCategoryName(pattern);
                    String description = generateCategoryDescription(pattern);
                    
                    return new IncomeCategorySuggestion(
                            categoryName, description, pattern.getSuggestedType(),
                            pattern.getTotalAmount(), pattern.getTotalAmount(),
                            pattern.getAverageAmount(), pattern.getFrequency(),
                            pattern.getTransactions());
                })
                .collect(Collectors.toList());
    }
    
    private IncomeCategory findOrCreateIncomeCategory(Budget budget, IncomeCategorySuggestion suggestion) {
        // Try to find existing category with same name
        Optional<IncomeCategory> existing = budget.getIncomeCategories().stream()
                .filter(cat -> cat.getName().equalsIgnoreCase(suggestion.getCategoryName()))
                .findFirst();
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create new category
        IncomeCategory newCategory = new IncomeCategory();
        newCategory.setName(suggestion.getCategoryName());
        newCategory.setDescription(suggestion.getDescription());
        newCategory.setIncomeType(suggestion.getIncomeType());
        newCategory.setColor(suggestion.getIncomeType().getDefaultColor());
        newCategory.setIcon(suggestion.getIncomeType().getDefaultIcon());
        newCategory.setBudget(budget);
        newCategory.setIsDefault(false);
        
        // Set display order
        int maxOrder = budget.getIncomeCategories().stream()
                .mapToInt(IncomeCategory::getDisplayOrder)
                .max().orElse(-1);
        newCategory.setDisplayOrder(maxOrder + 1);
        
        budget.addIncomeCategory(newCategory);
        return newCategory;
    }
    
    private void updateBudgetIncomeFromCategories(Budget budget) {
        BigDecimal totalBudgeted = budget.getTotalBudgetedIncome();
        BigDecimal totalActual = budget.getTotalActualIncome();
        
        if (totalBudgeted.compareTo(BigDecimal.ZERO) > 0) {
            budget.setProjectedIncome(totalBudgeted);
        }
        if (totalActual.compareTo(BigDecimal.ZERO) > 0) {
            budget.setActualIncome(totalActual);
        }
        
        budgetRepository.save(budget);
    }
    
    private String normalizeIncomeSource(String description) {
        if (description == null) return "Unknown";
        
        String normalized = description.toLowerCase().trim();
        
        // Common salary patterns
        if (normalized.contains("salaire") || normalized.contains("salary") || 
            normalized.contains("paie") || normalized.contains("payroll")) {
            return "Salaire";
        }
        
        // Freelance/business patterns  
        if (normalized.contains("freelance") || normalized.contains("facture") ||
            normalized.contains("prestation") || normalized.contains("consulting")) {
            return "Activité indépendante";
        }
        
        // Investment patterns
        if (normalized.contains("dividende") || normalized.contains("intérêt") ||
            normalized.contains("placement") || normalized.contains("investment")) {
            return "Investissement";
        }
        
        // Benefits/allowances
        if (normalized.contains("allocation") || normalized.contains("aide") ||
            normalized.contains("prestation") || normalized.contains("caf")) {
            return "Aides et allocations";
        }
        
        // Default to description first words
        String[] words = normalized.split("\\s+");
        return words.length > 0 ? words[0].substring(0, 1).toUpperCase() + 
                                 words[0].substring(1) : "Autre";
    }
    
    private IncomeCategory.IncomeType inferIncomeType(String pattern, BigDecimal amount) {
        String lowerPattern = pattern.toLowerCase();
        
        // Salary indicators
        if (lowerPattern.contains("salaire") || lowerPattern.contains("paie")) {
            return IncomeCategory.IncomeType.SALARY;
        }
        
        // Business/freelance indicators
        if (lowerPattern.contains("freelance") || lowerPattern.contains("activité") ||
            lowerPattern.contains("facture") || lowerPattern.contains("prestation")) {
            return IncomeCategory.IncomeType.BUSINESS;
        }
        
        // Investment indicators
        if (lowerPattern.contains("investissement") || lowerPattern.contains("dividende") ||
            lowerPattern.contains("intérêt") || lowerPattern.contains("placement")) {
            return IncomeCategory.IncomeType.INVESTMENT;
        }
        
        // Amount-based inference (higher amounts more likely to be salary)
        if (amount.compareTo(new BigDecimal("2000")) > 0) {
            return IncomeCategory.IncomeType.SALARY;
        }
        
        return IncomeCategory.IncomeType.OTHER;
    }
    
    private String generateCategoryName(IncomePattern pattern) {
        switch (pattern.getSuggestedType()) {
            case SALARY -> {
                return "Salaire - " + pattern.getPatternName();
            }
            case BUSINESS -> {
                return "Activité - " + pattern.getPatternName();
            }
            case INVESTMENT -> {
                return "Placement - " + pattern.getPatternName();
            }
            default -> {
                return pattern.getPatternName();
            }
        }
    }
    
    private String generateCategoryDescription(IncomePattern pattern) {
        return String.format("Revenus détectés: %d transactions, total %.2f€", 
                           pattern.getFrequency(), pattern.getTotalAmount());
    }
    
    // ====== RESULT CLASSES ======
    
    public static class HistoricalIncomeAnalysis {
        private final boolean hasHistoricalData;
        private final BigDecimal totalHistoricalIncome;
        private final List<IncomePattern> patterns;
        private final List<IncomeCategorySuggestion> suggestions;
        
        public HistoricalIncomeAnalysis(boolean hasHistoricalData, BigDecimal totalHistoricalIncome, 
                                       List<IncomePattern> patterns, List<IncomeCategorySuggestion> suggestions) {
            this.hasHistoricalData = hasHistoricalData;
            this.totalHistoricalIncome = totalHistoricalIncome;
            this.patterns = patterns;
            this.suggestions = suggestions;
        }
        
        // Getters
        public boolean hasHistoricalData() { return hasHistoricalData; }
        public BigDecimal getTotalHistoricalIncome() { return totalHistoricalIncome; }
        public List<IncomePattern> getPatterns() { return patterns; }
        public List<IncomeCategorySuggestion> getSuggestions() { return suggestions; }
    }
    
    public static class IncomePattern {
        private final String patternName;
        private final int frequency;
        private final BigDecimal totalAmount;
        private final BigDecimal averageAmount;
        private final IncomeCategory.IncomeType suggestedType;
        private final List<IncomeTransaction> transactions;
        
        public IncomePattern(String patternName, int frequency, BigDecimal totalAmount, 
                           BigDecimal averageAmount, IncomeCategory.IncomeType suggestedType,
                           List<IncomeTransaction> transactions) {
            this.patternName = patternName;
            this.frequency = frequency;
            this.totalAmount = totalAmount;
            this.averageAmount = averageAmount;
            this.suggestedType = suggestedType;
            this.transactions = transactions;
        }
        
        // Getters
        public String getPatternName() { return patternName; }
        public int getFrequency() { return frequency; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getAverageAmount() { return averageAmount; }
        public IncomeCategory.IncomeType getSuggestedType() { return suggestedType; }
        public List<IncomeTransaction> getTransactions() { return transactions; }
    }
    
    public static class IncomeCategorySuggestion {
        private final String categoryName;
        private final String description;
        private final IncomeCategory.IncomeType incomeType;
        private final BigDecimal suggestedAmount;
        private final BigDecimal actualAmount;
        private final BigDecimal averageAmount;
        private final int frequency;
        private final List<IncomeTransaction> sourceTransactions;
        
        public IncomeCategorySuggestion(String categoryName, String description, 
                                       IncomeCategory.IncomeType incomeType,
                                       BigDecimal suggestedAmount, BigDecimal actualAmount,
                                       BigDecimal averageAmount, int frequency,
                                       List<IncomeTransaction> sourceTransactions) {
            this.categoryName = categoryName;
            this.description = description;
            this.incomeType = incomeType;
            this.suggestedAmount = suggestedAmount;
            this.actualAmount = actualAmount;
            this.averageAmount = averageAmount;
            this.frequency = frequency;
            this.sourceTransactions = sourceTransactions;
        }
        
        // Getters
        public String getCategoryName() { return categoryName; }
        public String getDescription() { return description; }
        public IncomeCategory.IncomeType getIncomeType() { return incomeType; }
        public BigDecimal getSuggestedAmount() { return suggestedAmount; }
        public BigDecimal getActualAmount() { return actualAmount; }
        public BigDecimal getAverageAmount() { return averageAmount; }
        public int getFrequency() { return frequency; }
        public List<IncomeTransaction> getSourceTransactions() { return sourceTransactions; }
    }
    
    public static class IncomeComparison {
        private final List<MonthlyIncomeData> monthlyData;
        private final BigDecimal averageIncome;
        private final BigDecimal variance;
        private final BigDecimal variancePercentage;
        
        public IncomeComparison(List<MonthlyIncomeData> monthlyData, BigDecimal averageIncome,
                               BigDecimal variance, BigDecimal variancePercentage) {
            this.monthlyData = monthlyData;
            this.averageIncome = averageIncome;
            this.variance = variance;
            this.variancePercentage = variancePercentage;
        }
        
        // Getters
        public List<MonthlyIncomeData> getMonthlyData() { return monthlyData; }
        public BigDecimal getAverageIncome() { return averageIncome; }
        public BigDecimal getVariance() { return variance; }
        public BigDecimal getVariancePercentage() { return variancePercentage; }
    }
    
    public static class MonthlyIncomeData {
        private final Integer month;
        private final Integer year;
        private final BigDecimal totalIncome;
        
        public MonthlyIncomeData(Integer month, Integer year, BigDecimal totalIncome) {
            this.month = month;
            this.year = year;
            this.totalIncome = totalIncome;
        }
        
        // Getters
        public Integer getMonth() { return month; }
        public Integer getYear() { return year; }
        public BigDecimal getTotalIncome() { return totalIncome; }
    }
}