package com.buckpal.controller;

import com.buckpal.entity.BudgetCategoryKey;
import com.buckpal.entity.Category;
import com.buckpal.service.CategoryMappingService;
import com.buckpal.service.CategoryMappingService.CategoryMappingStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing category mappings between detailed categories and budget categories.
 * Handles the relationship between specific categories (like "Restaurant") and 
 * budget categories (like "Wants/Loisirs").
 */
@RestController
@RequestMapping("/api/category-mappings")
public class CategoryMappingController {

    @Autowired
    private CategoryMappingService categoryMappingService;

    /**
     * Get all categories grouped by their budget category mapping.
     * Used for the category management UI.
     */
    @GetMapping("/grouped")
    public ResponseEntity<Map<BudgetCategoryKey, List<Category>>> getCategoriesGroupedByBudgetCategory() {
        Map<BudgetCategoryKey, List<Category>> grouped = categoryMappingService.getCategoriesGroupedByBudgetCategory();
        return ResponseEntity.ok(grouped);
    }

    /**
     * Get the budget category for a specific detailed category.
     * Used during transaction assignment to determine which budget category to update.
     */
    @GetMapping("/budget-category/{detailedCategoryId}")
    public ResponseEntity<BudgetCategoryKey> getBudgetCategoryForDetailed(@PathVariable Long detailedCategoryId) {
        return categoryMappingService.getBudgetCategoryForDetailed(detailedCategoryId)
                .map(budgetKey -> ResponseEntity.ok(budgetKey))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update the budget category mapping for a detailed category.
     * Used when user reassigns categories via drag & drop or dropdown.
     */
    @PutMapping("/update-mapping")
    public ResponseEntity<Map<String, String>> updateCategoryMapping(
            @RequestBody UpdateMappingRequest request) {
        try {
            categoryMappingService.updateCategoryMapping(
                request.getDetailedCategoryId(), 
                request.getBudgetCategoryKey()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Category mapping updated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", "false",
                "message", "Failed to update category mapping: " + e.getMessage()
            ));
        }
    }

    /**
     * Create a new custom category with budget category mapping.
     * Used when user creates new categories via the "+ Add category" button.
     */
    @PostMapping("/create-custom-category")
    public ResponseEntity<Category> createCustomCategory(@RequestBody CreateCustomCategoryRequest request) {
        try {
            Category newCategory = categoryMappingService.createCustomCategory(
                request.getName(),
                request.getDescription(),
                request.getBudgetCategoryKey(),
                request.getIconName(),
                request.getColorCode()
            );
            
            return ResponseEntity.ok(newCategory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a custom category (only non-default categories can be deleted).
     * Used when user clicks the delete button on custom categories.
     */
    @DeleteMapping("/delete-custom-category/{categoryId}")
    public ResponseEntity<Map<String, String>> deleteCustomCategory(@PathVariable Long categoryId) {
        try {
            categoryMappingService.deleteCustomCategory(categoryId);
            
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Category deleted successfully"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", "false",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", "false",
                "message", "Failed to delete category: " + e.getMessage()
            ));
        }
    }

    /**
     * Update a custom category (only non-default categories can be updated).
     * Used when user edits custom categories.
     */
    @PutMapping("/update-custom-category/{categoryId}")
    public ResponseEntity<Category> updateCustomCategory(
            @PathVariable Long categoryId,
            @RequestBody UpdateCustomCategoryRequest request) {
        try {
            Category updatedCategory = categoryMappingService.updateCustomCategory(
                categoryId,
                request.getName(),
                request.getDescription(),
                request.getBudgetCategoryKey(),
                request.getIconName(),
                request.getColorCode()
            );
            
            return ResponseEntity.ok(updatedCategory);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get categories that have no budget category mapping.
     * Used for showing unmapped categories in the management UI.
     */
    @GetMapping("/unmapped")
    public ResponseEntity<List<Category>> getUnmappedCategories() {
        List<Category> unmapped = categoryMappingService.getUnmappedCategories();
        return ResponseEntity.ok(unmapped);
    }

    /**
     * Get mapping statistics for admin/debug purposes.
     */
    @GetMapping("/stats")
    public ResponseEntity<CategoryMappingStats> getMappingStats() {
        CategoryMappingStats stats = categoryMappingService.getMappingStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Initialize default mappings (admin endpoint).
     * Should be called during application setup or data migration.
     */
    @PostMapping("/initialize-defaults")
    public ResponseEntity<Map<String, String>> initializeDefaultMappings() {
        try {
            categoryMappingService.initializeDefaultMappings();
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Default mappings initialized successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", "false",
                "message", "Failed to initialize default mappings: " + e.getMessage()
            ));
        }
    }

    // Request DTOs
    public static class UpdateMappingRequest {
        private Long detailedCategoryId;
        private BudgetCategoryKey budgetCategoryKey;

        // Constructors
        public UpdateMappingRequest() {}
        
        public UpdateMappingRequest(Long detailedCategoryId, BudgetCategoryKey budgetCategoryKey) {
            this.detailedCategoryId = detailedCategoryId;
            this.budgetCategoryKey = budgetCategoryKey;
        }

        // Getters and Setters
        public Long getDetailedCategoryId() { return detailedCategoryId; }
        public void setDetailedCategoryId(Long detailedCategoryId) { this.detailedCategoryId = detailedCategoryId; }

        public BudgetCategoryKey getBudgetCategoryKey() { return budgetCategoryKey; }
        public void setBudgetCategoryKey(BudgetCategoryKey budgetCategoryKey) { this.budgetCategoryKey = budgetCategoryKey; }
    }

    public static class CreateCustomCategoryRequest {
        private String name;
        private String description;
        private BudgetCategoryKey budgetCategoryKey;
        private String iconName;
        private String colorCode;

        // Constructors
        public CreateCustomCategoryRequest() {}

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BudgetCategoryKey getBudgetCategoryKey() { return budgetCategoryKey; }
        public void setBudgetCategoryKey(BudgetCategoryKey budgetCategoryKey) { this.budgetCategoryKey = budgetCategoryKey; }

        public String getIconName() { return iconName; }
        public void setIconName(String iconName) { this.iconName = iconName; }

        public String getColorCode() { return colorCode; }
        public void setColorCode(String colorCode) { this.colorCode = colorCode; }
    }

    public static class UpdateCustomCategoryRequest {
        private String name;
        private String description;
        private BudgetCategoryKey budgetCategoryKey;
        private String iconName;
        private String colorCode;

        // Constructors
        public UpdateCustomCategoryRequest() {}

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BudgetCategoryKey getBudgetCategoryKey() { return budgetCategoryKey; }
        public void setBudgetCategoryKey(BudgetCategoryKey budgetCategoryKey) { this.budgetCategoryKey = budgetCategoryKey; }

        public String getIconName() { return iconName; }
        public void setIconName(String iconName) { this.iconName = iconName; }

        public String getColorCode() { return colorCode; }
        public void setColorCode(String colorCode) { this.colorCode = colorCode; }
    }
}