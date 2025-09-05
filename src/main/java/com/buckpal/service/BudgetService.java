package com.buckpal.service;

import com.buckpal.dto.BudgetDto;
import com.buckpal.dto.BudgetCategoryDto;
import com.buckpal.entity.*;
import com.buckpal.repository.BudgetRepository;
import com.buckpal.repository.BudgetCategoryRepository;
import com.buckpal.repository.TransactionRepository;
import com.buckpal.service.CategoryInitializationService.BudgetCategoryTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BudgetService {
    
    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryInitializationService categoryInitializationService;
    private final IncomeManagementService incomeService;
    private final IntelligentBudgetService intelligentBudgetService;
    
    @Autowired
    public BudgetService(BudgetRepository budgetRepository, 
                        BudgetCategoryRepository budgetCategoryRepository,
                        TransactionRepository transactionRepository,
                        CategoryInitializationService categoryInitializationService,
                        IncomeManagementService incomeService,
                        IntelligentBudgetService intelligentBudgetService) {
        this.budgetRepository = budgetRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.transactionRepository = transactionRepository;
        this.categoryInitializationService = categoryInitializationService;
        this.incomeService = incomeService;
        this.intelligentBudgetService = intelligentBudgetService;
    }
    
    public BudgetDto createBudget(User user, BudgetDto budgetDto) {
        Budget budget = new Budget();
        mapDtoToEntity(budgetDto, budget);
        budget.setUser(user);
        
        // Apply budget model percentages if not custom
        if (budgetDto.getBudgetModel() != Budget.BudgetModel.CUSTOM) {
            applyBudgetModel(budget, budgetDto.getBudgetModel());
        }
        
        Budget savedBudget = budgetRepository.save(budget);
        
        // Auto-create income categories based on historical patterns
        createIncomeCategoriesFromHistory(user, savedBudget);
        
        return mapEntityToDto(savedBudget);
    }
    
    public Optional<BudgetDto> getCurrentMonthBudget(User user) {
        System.out.println("=== getCurrentMonthBudget called for user: " + user.getEmail() + " ===");
        return budgetRepository.findCurrentMonthBudget(user)
                             .map(budget -> {
                                 System.out.println("Found current budget: " + budget.getBudgetMonth() + "/" + budget.getBudgetYear());
                                 recalculateBudgetSpentAmountsFromCategoryMapping(budget);
                                 return mapEntityToDto(budget);
                             });
    }
    
    public Optional<BudgetDto> getPreviousMonthBudget(User user) {
        return budgetRepository.findPreviousMonthBudget(user)
                             .map(this::mapEntityToDto);
    }
    
    public BudgetDto createBudgetFromPrevious(User user, Integer month, Integer year) {
        Optional<Budget> previousBudget = budgetRepository.findPreviousMonthBudget(user);
        
        BudgetDto newBudgetDto = new BudgetDto();
        newBudgetDto.setBudgetMonth(month);
        newBudgetDto.setBudgetYear(year);
        
        if (previousBudget.isPresent()) {
            Budget prev = previousBudget.get();
            newBudgetDto.setProjectedIncome(prev.getActualIncome().compareTo(BigDecimal.ZERO) > 0 
                ? prev.getActualIncome() : prev.getProjectedIncome());
            newBudgetDto.setBudgetModel(prev.getBudgetModel());
        } else {
            newBudgetDto.setProjectedIncome(BigDecimal.ZERO);
            newBudgetDto.setBudgetModel(Budget.BudgetModel.CUSTOM);
        }
        
        return createBudget(user, newBudgetDto);
    }
    
    public List<BudgetDto> getUserBudgets(User user) {
        System.out.println("=== getUserBudgets called for user: " + user.getEmail() + " ===");
        return budgetRepository.findByUserOrderByBudgetYearDescBudgetMonthDesc(user)
                              .stream()
                              .map(this::mapEntityToDto)
                              .collect(Collectors.toList());
    }
    
    public Optional<BudgetDto> getBudget(User user, Integer month, Integer year) {
        return budgetRepository.findByUserAndBudgetMonthAndBudgetYear(user, month, year)
                              .map(budget -> {
                                  // Recalculate spent amounts before returning
                                  recalculateBudgetSpentAmountsFromCategoryMapping(budget);
                                  return mapEntityToDto(budget);
                              });
    }
    
    public Optional<BudgetDto> getBudgetById(User user, Long budgetId) {
        System.out.println("=== getBudgetById called for user: " + user.getEmail() + ", budgetId: " + budgetId + " ===");
        return budgetRepository.findById(budgetId)
                              .filter(budget -> {
                                  System.out.println("Found budget: " + budget.getBudgetMonth() + "/" + budget.getBudgetYear());
                                  return budget.getUser().getId().equals(user.getId());
                              })
                              .map(budget -> {
                                  System.out.println("About to recalculate budget spent amounts...");
                                  // Recalculate spent amounts before returning
                                  recalculateBudgetSpentAmountsFromCategoryMapping(budget);
                                  System.out.println("Recalculation completed, converting to DTO");
                                  return mapEntityToDto(budget);
                              });
    }
    
    public BudgetDto updateBudget(User user, Long budgetId, BudgetDto budgetDto) {
        Optional<Budget> existingBudget = budgetRepository.findById(budgetId);
        if (existingBudget.isEmpty() || !existingBudget.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Budget not found or access denied");
        }
        
        Budget budget = existingBudget.get();
        mapDtoToEntity(budgetDto, budget);
        
        // Reapply budget model if changed
        if (budgetDto.getBudgetModel() != Budget.BudgetModel.CUSTOM) {
            applyBudgetModel(budget, budgetDto.getBudgetModel());
        }
        
        Budget savedBudget = budgetRepository.save(budget);
        return mapEntityToDto(savedBudget);
    }
    
    public void deleteBudget(User user, Long budgetId) {
        Optional<Budget> existingBudget = budgetRepository.findById(budgetId);
        if (existingBudget.isEmpty() || !existingBudget.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Budget not found or access denied");
        }

        // First, unassign all transactions that are assigned to this budget's categories
        // Use bulk update for better performance
        transactionRepository.unassignTransactionsFromBudget(budgetId, user);
        
        // Now safe to delete the budget (cascades will handle budget categories)
        budgetRepository.deleteById(budgetId);
    }
    
    public Map<String, BigDecimal> getBudgetModelPercentages(Budget.BudgetModel model) {
        return switch (model) {
            case RULE_50_30_20 -> Map.of(
                "budgetCategories.needs", new BigDecimal("50.0"),
                "budgetCategories.wants", new BigDecimal("30.0"),
                "budgetCategories.savings", new BigDecimal("20.0")
            );
            case RULE_60_20_20 -> Map.of(
                "budgetCategories.needs", new BigDecimal("60.0"),
                "budgetCategories.wants", new BigDecimal("20.0"),
                "budgetCategories.savings", new BigDecimal("20.0")
            );
            case RULE_80_20 -> Map.of(
                "budgetCategories.expenses", new BigDecimal("80.0"),
                "budgetCategories.savings", new BigDecimal("20.0")
            );
            case FRENCH_THIRDS -> Map.of(
                "budgetCategories.housing", new BigDecimal("33.33"),
                "budgetCategories.living", new BigDecimal("33.33"),
                "budgetCategories.savings", new BigDecimal("33.34")
            );
            case RULE_PERSONAL_PROJECTS -> Map.of(
                "budgetCategories.needs", new BigDecimal("45.0"),
                "budgetCategories.wants", new BigDecimal("25.0"),
                "budgetCategories.savings", new BigDecimal("20.0"),
                "budgetCategories.personalProjects", new BigDecimal("10.0")
            );
            default -> Map.of();
        };
    }
    
    public List<BudgetDto> getOverBudgetUsers(User user) {
        return budgetRepository.findByUserAndIsActiveTrue(user)
                              .stream()
                              .filter(Budget::isOverBudget)
                              .map(this::mapEntityToDto)
                              .collect(Collectors.toList());
    }
    
    private void applyBudgetModel(Budget budget, Budget.BudgetModel model) {
        Map<String, BigDecimal> percentages = getBudgetModelPercentages(model);
        BigDecimal income = budget.getProjectedIncome();
        
        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        
        // Clear existing categories and create new ones based on model
        budget.getBudgetCategories().clear();
        
        for (Map.Entry<String, BigDecimal> entry : percentages.entrySet()) {
            String categoryName = entry.getKey();
            BigDecimal percentage = entry.getValue();
            BigDecimal amount = income.multiply(percentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                                    .setScale(2, RoundingMode.HALF_UP);
            
            // Try to find the corresponding enum key
            BudgetCategoryKey categoryKey = BudgetCategoryKey.fromI18nKey(categoryName);
            
            BudgetCategory category;
            if (categoryKey != null) {
                // Use enum-based constructor for standardized categories
                category = new BudgetCategory(categoryKey, budget);
            } else {
                // Fallback to legacy constructor for custom categories
                // Validate that custom name doesn't conflict with reserved keys
                category = new BudgetCategory();
                category.setName(categoryName);
                category.setBudget(budget);
                category.setCategoryType(getCategoryType(categoryName));
                
                // Double-check validation
                if (!category.isValidCustomCategoryName(categoryName)) {
                    throw new IllegalArgumentException(
                        "Custom category name '" + categoryName + 
                        "' conflicts with reserved i18n key. Please use a different name."
                    );
                }
            }
            
            category.setAllocatedAmount(amount);
            category.setPercentage(percentage);
            category.setSortOrder(getSortOrder(entry.getKey()));
            
            budget.getBudgetCategories().add(category);
        }
        
        // Calculate total allocated amount
        BigDecimal totalAllocated = budget.getBudgetCategories().stream()
                                         .map(BudgetCategory::getAllocatedAmount)
                                         .reduce(BigDecimal.ZERO, BigDecimal::add);
        budget.setTotalAllocatedAmount(totalAllocated);
    }
    
    private BudgetCategory.BudgetCategoryType getCategoryType(String categoryName) {
        return switch (categoryName.toLowerCase()) {
            case "savings" -> BudgetCategory.BudgetCategoryType.SAVINGS;
            case "needs", "housing", "living", "expenses" -> BudgetCategory.BudgetCategoryType.EXPENSE;
            case "wants" -> BudgetCategory.BudgetCategoryType.EXPENSE;
            default -> BudgetCategory.BudgetCategoryType.EXPENSE;
        };
    }
    
    private Integer getSortOrder(String categoryName) {
        return switch (categoryName.toLowerCase()) {
            case "needs", "housing" -> 1;
            case "wants", "living" -> 2;
            case "savings" -> 3;
            case "expenses" -> 4;
            default -> 5;
        };
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private BudgetDto mapEntityToDto(Budget budget) {
        BudgetDto dto = new BudgetDto();
        dto.setId(budget.getId());
        dto.setBudgetMonth(budget.getBudgetMonth());
        dto.setBudgetYear(budget.getBudgetYear());
        dto.setBudgetModel(budget.getBudgetModel());
        dto.setProjectedIncome(budget.getProjectedIncome());
        dto.setActualIncome(budget.getActualIncome());
        dto.setTotalAllocatedAmount(budget.getTotalAllocatedAmount());
        dto.setTotalSpentAmount(budget.getTotalSpentAmount());
        dto.setNotes(budget.getNotes());
        dto.setIsActive(budget.getIsActive());
        dto.setCreatedAt(budget.getCreatedAt());
        dto.setUpdatedAt(budget.getUpdatedAt());
        
        // Set computed properties
        dto.setRemainingAmount(budget.getRemainingIncome());
        dto.setUsagePercentage(budget.getUsagePercentage());
        dto.setIsOverBudget(budget.isOverBudget());
        
        // Map budget categories
        List<BudgetCategoryDto> categoryDtos = budget.getBudgetCategories().stream()
                                                    .map(this::mapCategoryEntityToDto)
                                                    .collect(Collectors.toList());
        dto.setBudgetCategories(categoryDtos);
        
        return dto;
    }
    
    private BudgetCategoryDto mapCategoryEntityToDto(BudgetCategory category) {
        BudgetCategoryDto dto = new BudgetCategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setAllocatedAmount(category.getAllocatedAmount());
        dto.setSpentAmount(category.getSpentAmount());
        dto.setPercentage(category.getPercentage());
        dto.setCategoryType(category.getCategoryType());
        dto.setColorCode(category.getColorCode());
        dto.setIconName(category.getIconName());
        dto.setSortOrder(category.getSortOrder());
        dto.setIsActive(category.getIsActive());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        
        // Set computed properties
        dto.setRemainingAmount(category.getRemainingAmount());
        dto.setUsagePercentage(category.getUsagePercentage());
        dto.setIsOverBudget(category.isOverBudget());
        
        if (category.getParentCategory() != null) {
            dto.setParentCategoryId(category.getParentCategory().getId());
            dto.setParentCategoryName(category.getParentCategory().getName());
        }
        
        return dto;
    }
    
    private void mapDtoToEntity(BudgetDto dto, Budget entity) {
        entity.setBudgetMonth(dto.getBudgetMonth());
        entity.setBudgetYear(dto.getBudgetYear());
        entity.setBudgetModel(dto.getBudgetModel());
        entity.setProjectedIncome(dto.getProjectedIncome());
        entity.setActualIncome(dto.getActualIncome());
        entity.setTotalAllocatedAmount(dto.getTotalAllocatedAmount());
        entity.setTotalSpentAmount(dto.getTotalSpentAmount());
        entity.setNotes(dto.getNotes());
        entity.setIsActive(dto.getIsActive());
    }
    
    // recalculateBudgetCategorySpentAmount() removed - use recalculateBudgetSpentAmountsFromCategoryMapping() instead
    
    // recalculateBudgetTotalSpentAmount() removed - integrated into recalculateBudgetSpentAmountsFromCategoryMapping()
    
    // recalculateAllBudgetCategories() removed - use recalculateBudgetSpentAmountsFromCategoryMapping() instead
    
    /**
     * Calculate spent amount for a budget category using modern category mapping system
     */
    private BigDecimal calculateSpentAmountForCategory(BudgetCategory category) {
        // Use modern category mapping system with inner join
        Budget budget = category.getBudget();
        BudgetCategoryKey categoryKey = category.getCategoryKey();
        
        // If no category key mapping, return zero
        if (categoryKey == null) {
            return BigDecimal.ZERO;
        }
        
        // Calculate date range for the budget month
        LocalDate startDate = LocalDate.of(budget.getBudgetYear(), budget.getBudgetMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // Query transactions via category mapping with inner join
        List<Transaction> assignedTransactions = transactionRepository.findTransactionsByBudgetCategoryKeyAndDateRange(
            budget.getUser(), categoryKey, startDate, endDate);
        
        return assignedTransactions.stream()
            .filter(transaction -> Transaction.TransactionType.EXPENSE.equals(transaction.getTransactionType()))
            .map(transaction -> transaction.getAmount().abs()) // Ensure positive amounts
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // updateBudgetAfterTransactionAssignment() removed - use recalculateBudgetSpentAmountsFromCategoryMapping() instead
    
    /**
     * Create budget categories from predefined templates
     */
    public void createBudgetCategoriesFromTemplates(Budget budget) {
        List<BudgetCategoryTemplate> templates = categoryInitializationService.getPredefinedBudgetCategoryTemplates();
        
        for (BudgetCategoryTemplate template : templates) {
            BudgetCategory category = new BudgetCategory();
            category.setName(template.getName());
            category.setDescription(template.getDescription());
            category.setColorCode(template.getColorCode());
            category.setIconName(template.getIconName());
            category.setBudget(budget);
            
            // Convert template category type to entity category type
            BudgetCategory.BudgetCategoryType entityType;
            switch (template.getCategoryType()) {
                case NEEDS -> entityType = BudgetCategory.BudgetCategoryType.EXPENSE;
                case WANTS -> entityType = BudgetCategory.BudgetCategoryType.EXPENSE;
                case SAVINGS -> entityType = BudgetCategory.BudgetCategoryType.SAVINGS;
                default -> entityType = BudgetCategory.BudgetCategoryType.EXPENSE;
            }
            category.setCategoryType(entityType);
            
            // Calculate allocated amount based on suggested percentage and income
            BigDecimal suggestedPercentage = BigDecimal.valueOf(template.getSuggestedPercentage());
            BigDecimal allocatedAmount = budget.getProjectedIncome()
                .multiply(suggestedPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            category.setAllocatedAmount(allocatedAmount);
            
            category.setSpentAmount(BigDecimal.ZERO);
            category.setIsActive(true);
            
            budgetCategoryRepository.save(category);
        }
        
        // Note: Income categories are now created automatically via createIncomeCategoriesFromHistory()
        // No need for default categories here
    }
    
    /**
     * Get predefined budget category templates for frontend
     */
    public List<BudgetCategoryTemplate> getBudgetCategoryTemplates() {
        return categoryInitializationService.getPredefinedBudgetCategoryTemplates();
    }
    
    // ====== INCOME INTEGRATION METHODS ======
    
    /**
     * Get income categories for a budget
     */
    public List<IncomeCategory> getBudgetIncomeCategories(User user, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        return incomeService.getIncomeCategories(budget);
    }
    
    /**
     * Update budget after income transaction changes
     */
    public void updateBudgetAfterIncomeChange(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        // Recalculate total budgeted and actual income from categories
        BigDecimal totalBudgetedIncome = budget.getTotalBudgetedIncome();
        BigDecimal totalActualIncome = budget.getTotalActualIncome();
        
        // Update budget income fields if they differ significantly
        if (totalBudgetedIncome.compareTo(BigDecimal.ZERO) > 0) {
            budget.setProjectedIncome(totalBudgetedIncome);
        }
        if (totalActualIncome.compareTo(BigDecimal.ZERO) > 0) {
            budget.setActualIncome(totalActualIncome);
        }
        
        budgetRepository.save(budget);
    }
    
    /**
     * Check if budget has income categories configured
     */
    public boolean hasIncomeCategories(User user, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        return !budget.getIncomeCategories().isEmpty();
    }
    
    /**
     * Get income variance for budget (actual vs budgeted income)
     */
    public BigDecimal getIncomeVariance(User user, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        return budget.getIncomeVarianceByCategories();
    }
    
    /**
     * Get total income statistics for budget
     */
    public Map<String, BigDecimal> getIncomeStatistics(User user, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        BigDecimal totalBudgeted = budget.getTotalBudgetedIncome();
        BigDecimal totalActual = budget.getTotalActualIncome();
        BigDecimal variance = totalActual.subtract(totalBudgeted);
        BigDecimal variancePercentage = totalBudgeted.compareTo(BigDecimal.ZERO) > 0 
            ? variance.divide(totalBudgeted, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;
        
        Map<String, BigDecimal> stats = Map.of(
            "totalBudgeted", totalBudgeted,
            "totalActual", totalActual,
            "variance", variance,
            "variancePercentage", variancePercentage
        );
        
        return stats;
    }
    
    /**
     * Get transactions for a specific budget category using the category mapping system
     */
    public List<Transaction> getBudgetCategoryTransactions(User user, Long budgetId, Long categoryId) {
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        // Find the budget category by ID and verify it belongs to the user's budget
        BudgetCategory budgetCategory = budgetCategoryRepository.findById(categoryId)
                .filter(c -> c.getBudget().getId().equals(budgetId) && c.getBudget().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Budget category not found"));
        
        // Get the budget category key
        BudgetCategoryKey categoryKey = budgetCategory.getCategoryKey();
        if (categoryKey == null) {
            return new ArrayList<>(); // No transactions if no category key mapping exists
        }
        
        // Calculate date range for the budget period
        LocalDate startDate = LocalDate.of(budget.getBudgetYear(), budget.getBudgetMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // Find transactions via category mapping system using optimized SQL query
        return transactionRepository.findTransactionsByBudgetCategoryKeyAndDateRange(
                user, categoryKey, startDate, endDate);
    }
    
    /**
     * Auto-create income categories based on historical patterns
     */
    private void createIncomeCategoriesFromHistory(User user, Budget budget) {
        try {
            // Get smart template with historical income categories
            IntelligentBudgetService.SmartBudgetTemplate template = 
                intelligentBudgetService.generateSmartBudgetTemplate(user);
            
            // Check if we have historical data
            if (!template.getSuggestedCategories().isEmpty()) {
                // Create income categories based on historical patterns
                for (IntelligentBudgetService.SuggestedIncomeCategory suggestedCategory : template.getSuggestedCategories()) {
                    IncomeCategory incomeCategory = new IncomeCategory();
                    incomeCategory.setName(suggestedCategory.getCategoryName());
                    incomeCategory.setDescription("Auto-créé basé sur l'historique");
                    incomeCategory.setIncomeType(suggestedCategory.getIncomeType());
                    incomeCategory.setBudgetedAmount(suggestedCategory.getSuggestedAmount());
                    incomeCategory.setActualAmount(BigDecimal.ZERO);
                    incomeCategory.setColor(suggestedCategory.getColor());
                    incomeCategory.setIcon(suggestedCategory.getIcon());
                    incomeCategory.setDisplayOrder(template.getSuggestedCategories().indexOf(suggestedCategory));
                    incomeCategory.setIsDefault(false);
                    incomeCategory.setBudget(budget);
                    
                    incomeService.createIncomeCategory(user, budget.getId(), incomeCategory);
                }
                
                // Update projected income based on historical data
                if (template.getTotalSuggestedIncome().compareTo(BigDecimal.ZERO) > 0) {
                    budget.setProjectedIncome(template.getTotalSuggestedIncome());
                    budgetRepository.save(budget);
                }
            } else {
                // No historical data - create default categories
                incomeService.createDefaultIncomeCategories(budget);
            }
            
        } catch (Exception e) {
            // If intelligent creation fails, fall back to default categories
            System.err.println("Failed to create income categories from history: " + e.getMessage());
            incomeService.createDefaultIncomeCategories(budget);
        }
    }
    
    /**
     * Update all budget category spent amounts using SQL join with category mapping
     * This method calculates expenses by joining transactions -> categories -> budget category mapping
     */
    public void recalculateBudgetSpentAmountsFromCategoryMapping(Budget budget) {
        
        // Calculate date range for the budget month
        LocalDate startDate = LocalDate.of(budget.getBudgetYear(), budget.getBudgetMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // Get spent amounts grouped by budget category key via SQL join
        List<Object[]> spentByCategory = transactionRepository.calculateSpentAmountsByBudgetCategory(
            budget.getUser(), startDate, endDate);
        
        // Convert results to map for easy lookup
        Map<BudgetCategoryKey, BigDecimal> spentAmounts;
        try {
            spentAmounts = spentByCategory.stream()
                .collect(Collectors.toMap(
                    row -> (BudgetCategoryKey) row[0],
                    row -> (BigDecimal) row[1],
                    BigDecimal::add  // In case of duplicates, sum them
                ));
        } catch (Exception e) {
            System.err.println("ERROR creating spent amounts map: " + e.getMessage());
            e.printStackTrace();
            return; // Exit early if there's an error
        }
        
        // Update each budget category with the calculated spent amount
        for (BudgetCategory budgetCategory : budget.getBudgetCategories()) {
            if (budgetCategory.getCategoryKey() != null) {
                BigDecimal spentAmount = spentAmounts.getOrDefault(budgetCategory.getCategoryKey(), BigDecimal.ZERO);
                BigDecimal oldAmount = budgetCategory.getSpentAmount();
                budgetCategory.setSpentAmount(spentAmount);
                budgetCategoryRepository.save(budgetCategory);
            } else {
            }
        }
        
        // Recalculate budget total (integrated here instead of separate method)
        BigDecimal totalSpent = budget.getBudgetCategories().stream()
            .map(BudgetCategory::getSpentAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        budget.setTotalSpentAmount(totalSpent);
        budgetRepository.save(budget);
    }
    
    /**
     * Debug method to check why budget calculation is not working
     */
    public void debugBudgetCalculation(Budget budget) {
        System.out.println("=== DEBUG BUDGET CALCULATION ===");
        System.out.println("Budget: " + budget.getBudgetMonth() + "/" + budget.getBudgetYear() + " for user: " + budget.getUser().getEmail());
        
        // Calculate date range for the budget month
        LocalDate startDate = LocalDate.of(budget.getBudgetYear(), budget.getBudgetMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        System.out.println("Date range: " + startDate + " to " + endDate);
        
        // Debug transaction categories
        List<Object[]> debugData = transactionRepository.debugTransactionCategories(
            budget.getUser(), startDate, endDate);
        
        System.out.println("Found " + debugData.size() + " transactions:");
        int count = 0;
        for (Object[] row : debugData) {
            System.out.println("TX ID: " + row[0] + ", Amount: " + row[1] + ", Type: " + row[2] + 
                             ", Date: " + row[3] + ", Category: " + row[4] + 
                             ", BudgetKey: " + row[5] + ", Status: " + row[6]);
            count++;
            if (count >= 10) { // Limit to first 10 for readability
                System.out.println("... (" + (debugData.size() - 10) + " more transactions)");
                break;
            }
        }
        
        // Test the actual calculation query
        List<Object[]> spentByCategory = transactionRepository.calculateSpentAmountsByBudgetCategory(
            budget.getUser(), startDate, endDate);
        
        System.out.println("Calculation results: " + spentByCategory.size() + " budget categories");
        for (Object[] row : spentByCategory) {
            System.out.println("BudgetKey: " + row[0] + ", Amount: " + row[1]);
        }
        
        System.out.println("=== END DEBUG ===");
    }
}