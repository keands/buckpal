package com.buckpal.service;

import com.buckpal.entity.BudgetCategory;
import com.buckpal.entity.BudgetCategoryKey;
import com.buckpal.repository.BudgetCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service to migrate existing budget categories from string names to enum keys.
 * This service handles the transition from the legacy string-based category names
 * to the new enum-based system for better data consistency and internationalization.
 */
@Service
public class BudgetCategoryMigrationService {

    @Autowired
    private BudgetCategoryRepository budgetCategoryRepository;

    /**
     * Migrate all existing budget categories to use enum keys where possible.
     * This method should be called once during the upgrade process.
     */
    @Transactional
    public MigrationResult migrateCategoriesToEnumKeys() {
        List<BudgetCategory> allCategories = budgetCategoryRepository.findAll();
        
        int migratedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        
        for (BudgetCategory category : allCategories) {
            try {
                // Skip if already has enum key
                if (category.getCategoryKey() != null) {
                    skippedCount++;
                    continue;
                }
                
                // Try to find matching enum key
                BudgetCategoryKey enumKey = BudgetCategoryKey.fromI18nKey(category.getName());
                
                if (enumKey != null) {
                    // Set the enum key - this will auto-sync name and type
                    category.setCategoryKey(enumKey);
                    budgetCategoryRepository.save(category);
                    migratedCount++;
                } else {
                    // Category name doesn't match any enum - keep as custom category
                    skippedCount++;
                }
            } catch (Exception e) {
                errorCount++;
                System.err.println("Error migrating category " + category.getId() + ": " + e.getMessage());
            }
        }
        
        return new MigrationResult(migratedCount, skippedCount, errorCount);
    }
    
    /**
     * Validate that all categories with enum keys have consistent data
     */
    @Transactional(readOnly = true)
    public ValidationResult validateCategoryConsistency() {
        List<BudgetCategory> categoriesWithKeys = budgetCategoryRepository.findAll()
                .stream()
                .filter(cat -> cat.getCategoryKey() != null)
                .toList();
        
        int validCount = 0;
        int inconsistentCount = 0;
        
        for (BudgetCategory category : categoriesWithKeys) {
            String expectedName = category.getCategoryKey().getI18nKey();
            BudgetCategory.BudgetCategoryType expectedType = category.getCategoryKey().getCategoryType();
            
            boolean nameConsistent = expectedName.equals(category.getName());
            boolean typeConsistent = expectedType.equals(category.getCategoryType());
            
            if (nameConsistent && typeConsistent) {
                validCount++;
            } else {
                inconsistentCount++;
                System.out.println("Inconsistent category " + category.getId() + 
                    ": expected name=" + expectedName + ", actual=" + category.getName() +
                    ", expected type=" + expectedType + ", actual=" + category.getCategoryType());
            }
        }
        
        return new ValidationResult(validCount, inconsistentCount);
    }
    
    /**
     * Result of migration operation
     */
    public static class MigrationResult {
        private final int migratedCount;
        private final int skippedCount;
        private final int errorCount;
        
        public MigrationResult(int migratedCount, int skippedCount, int errorCount) {
            this.migratedCount = migratedCount;
            this.skippedCount = skippedCount;
            this.errorCount = errorCount;
        }
        
        public int getMigratedCount() { return migratedCount; }
        public int getSkippedCount() { return skippedCount; }
        public int getErrorCount() { return errorCount; }
        public int getTotalCount() { return migratedCount + skippedCount + errorCount; }
        
        @Override
        public String toString() {
            return String.format("Migration Result: %d migrated, %d skipped, %d errors (total: %d)",
                    migratedCount, skippedCount, errorCount, getTotalCount());
        }
    }
    
    /**
     * Result of validation operation
     */
    public static class ValidationResult {
        private final int validCount;
        private final int inconsistentCount;
        
        public ValidationResult(int validCount, int inconsistentCount) {
            this.validCount = validCount;
            this.inconsistentCount = inconsistentCount;
        }
        
        public int getValidCount() { return validCount; }
        public int getInconsistentCount() { return inconsistentCount; }
        public int getTotalCount() { return validCount + inconsistentCount; }
        public boolean isAllValid() { return inconsistentCount == 0; }
        
        @Override
        public String toString() {
            return String.format("Validation Result: %d valid, %d inconsistent (total: %d)",
                    validCount, inconsistentCount, getTotalCount());
        }
    }
}