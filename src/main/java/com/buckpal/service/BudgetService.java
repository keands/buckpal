package com.buckpal.service;

import com.buckpal.dto.BudgetDto;
import com.buckpal.dto.BudgetCategoryDto;
import com.buckpal.entity.Budget;
import com.buckpal.entity.BudgetCategory;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
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
    
    @Autowired
    public BudgetService(BudgetRepository budgetRepository, 
                        BudgetCategoryRepository budgetCategoryRepository,
                        TransactionRepository transactionRepository,
                        CategoryInitializationService categoryInitializationService) {
        this.budgetRepository = budgetRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.transactionRepository = transactionRepository;
        this.categoryInitializationService = categoryInitializationService;
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
        return mapEntityToDto(savedBudget);
    }
    
    public Optional<BudgetDto> getCurrentMonthBudget(User user) {
        return budgetRepository.findCurrentMonthBudget(user)
                             .map(this::mapEntityToDto);
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
        return budgetRepository.findByUserOrderByBudgetYearDescBudgetMonthDesc(user)
                              .stream()
                              .map(this::mapEntityToDto)
                              .collect(Collectors.toList());
    }
    
    public Optional<BudgetDto> getBudget(User user, Integer month, Integer year) {
        return budgetRepository.findByUserAndBudgetMonthAndBudgetYear(user, month, year)
                              .map(this::mapEntityToDto);
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
        
        budgetRepository.deleteById(budgetId);
    }
    
    public Map<String, BigDecimal> getBudgetModelPercentages(Budget.BudgetModel model) {
        return switch (model) {
            case RULE_50_30_20 -> Map.of(
                "needs", new BigDecimal("50.0"),
                "wants", new BigDecimal("30.0"),
                "savings", new BigDecimal("20.0")
            );
            case RULE_60_20_20 -> Map.of(
                "needs", new BigDecimal("60.0"),
                "wants", new BigDecimal("20.0"),
                "savings", new BigDecimal("20.0")
            );
            case RULE_80_20 -> Map.of(
                "expenses", new BigDecimal("80.0"),
                "savings", new BigDecimal("20.0")
            );
            case FRENCH_THIRDS -> Map.of(
                "housing", new BigDecimal("33.33"),
                "living", new BigDecimal("33.33"),
                "savings", new BigDecimal("33.34")
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
            String categoryName = capitalizeFirst(entry.getKey());
            BigDecimal percentage = entry.getValue();
            BigDecimal amount = income.multiply(percentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                                    .setScale(2, RoundingMode.HALF_UP);
            
            BudgetCategory category = new BudgetCategory();
            category.setName(categoryName);
            category.setBudget(budget);
            category.setAllocatedAmount(amount);
            category.setPercentage(percentage);
            category.setCategoryType(getCategoryType(entry.getKey()));
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
    
    /**
     * Recalculate spent amounts for a specific budget category based on assigned transactions
     */
    public void recalculateBudgetCategorySpentAmount(Long budgetCategoryId) {
        BudgetCategory category = budgetCategoryRepository.findById(budgetCategoryId)
            .orElseThrow(() -> new RuntimeException("Budget category not found"));
        
        // Calculate total spent amount from assigned transactions
        BigDecimal totalSpent = calculateSpentAmountForCategory(category);
        
        // Update category spent amount
        category.setSpentAmount(totalSpent);
        budgetCategoryRepository.save(category);
        
        // Update the parent budget's total spent amount
        recalculateBudgetTotalSpentAmount(category.getBudget());
    }
    
    /**
     * Recalculate total spent amount for an entire budget
     */
    public void recalculateBudgetTotalSpentAmount(Budget budget) {
        BigDecimal totalSpent = budget.getBudgetCategories().stream()
            .map(BudgetCategory::getSpentAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        budget.setTotalSpentAmount(totalSpent);
        budgetRepository.save(budget);
    }
    
    /**
     * Recalculate all budget categories for a specific budget
     */
    public void recalculateAllBudgetCategories(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        // Recalculate each category
        for (BudgetCategory category : budget.getBudgetCategories()) {
            BigDecimal totalSpent = calculateSpentAmountForCategory(category);
            category.setSpentAmount(totalSpent);
            budgetCategoryRepository.save(category);
        }
        
        // Recalculate budget total
        recalculateBudgetTotalSpentAmount(budget);
    }
    
    /**
     * Calculate spent amount for a budget category from assigned transactions
     */
    private BigDecimal calculateSpentAmountForCategory(BudgetCategory category) {
        // Use direct database query to avoid lazy loading issues
        // Sum up all EXPENSE transactions assigned to this budget category for the budget's month
        Budget budget = category.getBudget();
        
        // Calculate date range for the budget month
        LocalDate startDate = LocalDate.of(budget.getBudgetYear(), budget.getBudgetMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // Query transactions directly from repository
        List<Transaction> assignedTransactions = transactionRepository.findByBudgetCategoryAndDateRange(
            category, startDate, endDate);
        
        return assignedTransactions.stream()
            .filter(transaction -> Transaction.TransactionType.EXPENSE.equals(transaction.getTransactionType()))
            .map(transaction -> transaction.getAmount().abs()) // Ensure positive amounts
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Update budget category spent amount when a transaction is assigned/unassigned
     */
    public void updateBudgetAfterTransactionAssignment(Long budgetCategoryId) {
        recalculateBudgetCategorySpentAmount(budgetCategoryId);
    }
    
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
    }
    
    /**
     * Get predefined budget category templates for frontend
     */
    public List<BudgetCategoryTemplate> getBudgetCategoryTemplates() {
        return categoryInitializationService.getPredefinedBudgetCategoryTemplates();
    }
}