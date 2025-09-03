package com.buckpal.controller;

import com.buckpal.entity.Category;
import com.buckpal.entity.User;
import com.buckpal.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * Get detailed to budget category mapping
     */
    @GetMapping("/mapping")
    public ResponseEntity<Map<String, String>> getCategoryMapping(Authentication authentication) {
        try {
            Map<String, String> mapping = categoryService.getDetailedToBudgetCategoryMappingWithCustom();
            return ResponseEntity.ok(mapping);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get available icons for categories
     */
    @GetMapping("/icons")
    public ResponseEntity<List<String>> getAvailableIcons() {
        List<String> icons = List.of(
            "💰", "💼", "📈", "💸", // Income
            "🏠", "⚡", "🛒", "🚗", "🛡️", "🏥", // Needs
            "🍽️", "🎬", "🛍️", "💄", "🎨", "✈️", // Wants
            "🏦", "💎", "💳", // Savings/Investment
            "📚", "🎁", "📊", "📋", "🏧", "❓", // Other
            "🍕", "☕", "🎮", "🏃", "🚌", "⛽", // Additional
            "📱", "💻", "👔", "👗", "🧴", "🏋️"  // More options
        );
        return ResponseEntity.ok(icons);
    }
    
    /**
     * Get available colors for categories
     */
    @GetMapping("/colors")
    public ResponseEntity<List<String>> getAvailableColors() {
        List<String> colors = List.of(
            "#ef4444", "#f97316", "#f59e0b", "#eab308", "#84cc16", "#22c55e",
            "#10b981", "#14b8a6", "#06b6d4", "#0891b2", "#0284c7", "#3b82f6",
            "#6366f1", "#8b5cf6", "#a855f7", "#c026d3", "#d946ef", "#ec4899",
            "#f43f5e", "#6b7280"
        );
        return ResponseEntity.ok(colors);
    }

    /**
     * Get all categories for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<Category>> getUserCategories(Authentication authentication) {
        try {
            if (authentication == null) {
                // Fallback to all categories if no authentication
                List<Category> categories = categoryService.getAllCategories();
                return ResponseEntity.ok(categories);
            }
            
            User user = (User) authentication.getPrincipal();
            List<Category> categories = categoryService.getCategoriesForUser(user);
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get a specific category by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Optional<Category> category = categoryService.getCategoryById(id);
            
            if (category.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // If authenticated, check ownership
            if (authentication != null) {
                User user = (User) authentication.getPrincipal();
                Category cat = category.get();
                
                // Allow access if it's the user's category or a default category
                if (!cat.getUser().getId().equals(user.getId()) && !cat.getIsDefault()) {
                    return ResponseEntity.notFound().build();
                }
            }
            
            return ResponseEntity.ok(category.get());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create a new custom category
     */
    @PostMapping
    public ResponseEntity<Category> createCategory(
            @Valid @RequestBody CategoryCreateRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            Category category = new Category();
            category.setName(request.getName());
            category.setDescription(request.getDescription());
            category.setIconName(request.getIconName());
            category.setColorCode(request.getColorCode());
            category.setUser(user);
            category.setIsDefault(false); // Custom user category
            
            Category savedCategory = categoryService.saveCategory(category);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update an existing custom category
     */
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryCreateRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Optional<Category> categoryOpt = categoryService.getCategoryById(id);
            
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Category category = categoryOpt.get();
            
            // Check ownership and prevent editing default categories
            if (!category.getUser().getId().equals(user.getId()) || category.getIsDefault()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Update category properties
            category.setName(request.getName());
            category.setDescription(request.getDescription());
            category.setIconName(request.getIconName());
            category.setColorCode(request.getColorCode());
            
            Category updatedCategory = categoryService.saveCategory(category);
            return ResponseEntity.ok(updatedCategory);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete a custom category
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Optional<Category> categoryOpt = categoryService.getCategoryById(id);
            
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Category category = categoryOpt.get();
            
            // Check ownership and prevent deleting default categories
            if (!category.getUser().getId().equals(user.getId()) || category.getIsDefault()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Request DTO for creating/updating categories
     */
    public static class CategoryCreateRequest {
        private String name;
        private String description;
        private String iconName;
        private String colorCode;
        
        // Constructors
        public CategoryCreateRequest() {}
        
        public CategoryCreateRequest(String name, String description, String iconName, String colorCode) {
            this.name = name;
            this.description = description;
            this.iconName = iconName;
            this.colorCode = colorCode;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getIconName() { return iconName; }
        public void setIconName(String iconName) { this.iconName = iconName; }
        
        public String getColorCode() { return colorCode; }
        public void setColorCode(String colorCode) { this.colorCode = colorCode; }
    }
}