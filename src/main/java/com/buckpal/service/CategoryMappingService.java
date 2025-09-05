package com.buckpal.service;

import com.buckpal.entity.BudgetCategoryKey;
import com.buckpal.entity.Category;
import com.buckpal.entity.User;
import com.buckpal.repository.BudgetRepository;
import com.buckpal.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Service for managing the mapping between detailed categories and budget categories.
 * This service handles both automatic mappings (predefined by the system) and 
 * user-defined mappings (custom categories created by users).
 */
@Service
public class CategoryMappingService {

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private BudgetService budgetService;

    /**
     * Get the budget category key for a detailed category.
     * Used when assigning transactions to determine which budget category to update.
     */
    public Optional<BudgetCategoryKey> getBudgetCategoryForDetailed(Long detailedCategoryId) {
        return categoryRepository.findById(detailedCategoryId)
                .map(Category::getBudgetCategoryKey);
    }

    /**
     * Get all categories grouped by their budget category mapping.
     * Automatically initializes categories if none are mapped yet.
     */
    public Map<BudgetCategoryKey, List<Category>> getCategoriesGroupedByBudgetCategory() {
        // Check if categories need initialization (first time access)
        if (needsInitialization()) {
            initializeDefaultMappings();
        }
        
        List<Category> allCategories = categoryRepository.findAll();
        
        return allCategories.stream()
                .filter(category -> category.getBudgetCategoryKey() != null)
                .collect(Collectors.groupingBy(Category::getBudgetCategoryKey));
    }

    /**
     * Check if the system needs category initialization.
     * Returns true if no categories have budget category mappings yet.
     */
    private boolean needsInitialization() {
        return categoryRepository.findAll().stream()
                .noneMatch(category -> category.getBudgetCategoryKey() != null);
    }

    /**
     * Update the budget category mapping for a detailed category
     */
    @Transactional
    public void updateCategoryMapping(Long detailedCategoryId, BudgetCategoryKey budgetCategoryKey) {
        Category category = categoryRepository.findById(detailedCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + detailedCategoryId));
        
        category.setBudgetCategoryKey(budgetCategoryKey);
        category.setIsAutoMapped(false); // Mark as user-defined mapping
        
        categoryRepository.save(category);
        
        // Recalculate affected budgets since mapping changed
        recalculateAffectedBudgets();
    }

    /**
     * Create a new custom category and map it to a budget category
     */
    @Transactional
    public Category createCustomCategory(String name, String description, BudgetCategoryKey budgetCategoryKey, 
                                       String iconName, String colorCode) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setBudgetCategoryKey(budgetCategoryKey);
        category.setIconName(iconName);
        category.setColorCode(colorCode);
        category.setIsDefault(false);
        category.setIsAutoMapped(false); // User-created category
        
        Category savedCategory = categoryRepository.save(category);
        
        // Recalculate affected budgets since new mapping was created
        recalculateAffectedBudgets();
        
        return savedCategory;
    }

    /**
     * Delete a custom category (only non-default categories can be deleted)
     */
    @Transactional
    public void deleteCustomCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        
        // Only allow deletion of non-default (custom) categories
        if (category.getIsDefault()) {
            throw new IllegalStateException("Cannot delete default system categories");
        }
        
        categoryRepository.delete(category);
    }

    /**
     * Update a custom category (only non-default categories can be updated)
     */
    @Transactional
    public Category updateCustomCategory(Long categoryId, String name, String description, 
                                       BudgetCategoryKey budgetCategoryKey, String iconName, String colorCode) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        
        // Only allow modification of non-default (custom) categories
        if (category.getIsDefault()) {
            throw new IllegalStateException("Cannot modify default system categories");
        }
        
        // Update the category fields
        category.setName(name);
        category.setDescription(description);
        category.setBudgetCategoryKey(budgetCategoryKey);
        category.setIconName(iconName);
        category.setColorCode(colorCode);
        
        Category savedCategory = categoryRepository.save(category);
        
        // Recalculate affected budgets since mapping was updated
        recalculateAffectedBudgets();
        
        return savedCategory;
    }

    /**
     * Initialize default mappings for system categories.
     * Also cleans up duplicate categories to ensure unique category names.
     */
    @Transactional
    public void initializeDefaultMappings() {
        // First, clean up duplicates to ensure uniqueness
        cleanupDuplicateCategories();
        
        Map<String, BudgetCategoryKey> defaultMappings = getDefaultCategoryMappings();
        
        // Get all unmapped categories to avoid duplicate processing
        List<Category> unmappedCategories = getUnmappedCategories();
        
        for (Category category : unmappedCategories) {
            String categoryName = category.getName();
            BudgetCategoryKey budgetKey = defaultMappings.get(categoryName);
            
            if (budgetKey != null) {
                // Apply the default mapping
                category.setBudgetCategoryKey(budgetKey);
                category.setIsAutoMapped(true);
                categoryRepository.save(category);
            }
        }
        
        // Recalculate affected budgets since mappings were initialized
        recalculateAffectedBudgets();
    }

    /**
     * Clean up duplicate categories by keeping the most recent one for each name.
     * This ensures category name uniqueness across the system.
     */
    @Transactional
    private void cleanupDuplicateCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        Map<String, Category> uniqueCategories = new HashMap<>();
        List<Category> categoriesToDelete = new ArrayList<>();
        
        for (Category category : allCategories) {
            String categoryName = category.getName();
            
            if (uniqueCategories.containsKey(categoryName)) {
                Category existing = uniqueCategories.get(categoryName);
                
                // Keep the more recent category (by createdAt) or the one with mapping
                if (category.getCreatedAt().isAfter(existing.getCreatedAt()) || 
                    (category.getBudgetCategoryKey() != null && existing.getBudgetCategoryKey() == null)) {
                    categoriesToDelete.add(existing);
                    uniqueCategories.put(categoryName, category);
                } else {
                    categoriesToDelete.add(category);
                }
            } else {
                uniqueCategories.put(categoryName, category);
            }
        }
        
        // Delete duplicate categories
        if (!categoriesToDelete.isEmpty()) {
            categoryRepository.deleteAll(categoriesToDelete);
        }
    }

    /**
     * Get the default mapping of category names to budget category keys.
     * This represents the system's intelligent mapping of detailed categories.
     */
    private Map<String, BudgetCategoryKey> getDefaultCategoryMappings() {
        Map<String, BudgetCategoryKey> mappings = new java.util.HashMap<>();
        
        // Needs (Besoins) - Essential expenses
        mappings.put("categories.housing", BudgetCategoryKey.NEEDS);
        mappings.put("categories.utilities", BudgetCategoryKey.NEEDS);
        mappings.put("categories.groceries", BudgetCategoryKey.NEEDS);
        mappings.put("categories.healthcare", BudgetCategoryKey.NEEDS);
        mappings.put("categories.insurance", BudgetCategoryKey.NEEDS);
        mappings.put("categories.transportation", BudgetCategoryKey.NEEDS);
        mappings.put("categories.childcare", BudgetCategoryKey.NEEDS);
        mappings.put("categories.minimumDebtPayment", BudgetCategoryKey.NEEDS);

        // Wants (Loisirs) - Discretionary spending
        mappings.put("categories.diningOut", BudgetCategoryKey.WANTS);
        mappings.put("categories.entertainment", BudgetCategoryKey.WANTS);
        mappings.put("categories.shopping", BudgetCategoryKey.WANTS);
        mappings.put("categories.hobbies", BudgetCategoryKey.WANTS);
        mappings.put("categories.travel", BudgetCategoryKey.WANTS);
        mappings.put("categories.subscriptions", BudgetCategoryKey.WANTS);
        mappings.put("categories.giftsDonations", BudgetCategoryKey.WANTS);
        mappings.put("categories.personalCare", BudgetCategoryKey.WANTS);

        // Savings (Épargne) - Savings and investments
        mappings.put("categories.emergencyFund", BudgetCategoryKey.SAVINGS);
        mappings.put("categories.retirement", BudgetCategoryKey.SAVINGS);
        mappings.put("categories.investments", BudgetCategoryKey.SAVINGS);
        mappings.put("categories.savingsGoals", BudgetCategoryKey.SAVINGS);

        // Projects (Projets Perso) - Personal projects and goals
        mappings.put("categories.homeImprovement", BudgetCategoryKey.PROJECTS);
        mappings.put("categories.education", BudgetCategoryKey.PROJECTS);
        mappings.put("categories.businessExpenses", BudgetCategoryKey.PROJECTS);
        mappings.put("categories.largePurchases", BudgetCategoryKey.PROJECTS);
        
        return mappings;
    }

    /**
     * Get categories that have no budget category mapping (unmapped categories)
     */
    public List<Category> getUnmappedCategories() {
        return categoryRepository.findAll().stream()
                .filter(category -> category.getBudgetCategoryKey() == null)
                .collect(Collectors.toList());
    }

    /**
     * Get statistics about category mappings
     */
    public CategoryMappingStats getMappingStats() {
        List<Category> allCategories = categoryRepository.findAll();
        
        long totalCategories = allCategories.size();
        long mappedCategories = allCategories.stream()
                .filter(category -> category.getBudgetCategoryKey() != null)
                .count();
        long autoMappedCategories = allCategories.stream()
                .filter(category -> Boolean.TRUE.equals(category.getIsAutoMapped()))
                .count();
        long userMappedCategories = mappedCategories - autoMappedCategories;
        
        return new CategoryMappingStats(totalCategories, mappedCategories, 
                                      autoMappedCategories, userMappedCategories);
    }

    /**
     * Statistics class for category mappings
     */
    public static class CategoryMappingStats {
        private final long totalCategories;
        private final long mappedCategories;
        private final long autoMappedCategories;
        private final long userMappedCategories;

        public CategoryMappingStats(long totalCategories, long mappedCategories, 
                                  long autoMappedCategories, long userMappedCategories) {
            this.totalCategories = totalCategories;
            this.mappedCategories = mappedCategories;
            this.autoMappedCategories = autoMappedCategories;
            this.userMappedCategories = userMappedCategories;
        }

        // Getters
        public long getTotalCategories() { return totalCategories; }
        public long getMappedCategories() { return mappedCategories; }
        public long getAutoMappedCategories() { return autoMappedCategories; }
        public long getUserMappedCategories() { return userMappedCategories; }
        public long getUnmappedCategories() { return totalCategories - mappedCategories; }
        public double getMappingPercentage() { 
            return totalCategories > 0 ? (mappedCategories * 100.0) / totalCategories : 0.0; 
        }
    }
    
    /**
     * Recalculate budget progress for all existing budgets after category mapping changes
     */
    private void recalculateAffectedBudgets() {
        // Get all budgets and recalculate their spent amounts
        budgetRepository.findAll().forEach(budget -> {
            budgetService.recalculateBudgetSpentAmountsFromCategoryMapping(budget);
        });
    }
}