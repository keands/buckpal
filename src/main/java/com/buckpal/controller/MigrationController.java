package com.buckpal.controller;

import com.buckpal.service.BudgetCategoryMigrationService;
import com.buckpal.service.BudgetCategoryMigrationService.MigrationResult;
import com.buckpal.service.BudgetCategoryMigrationService.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for database migration operations.
 * These endpoints should be used carefully and typically only during maintenance windows.
 */
@RestController
@RequestMapping("/api/admin/migration")
@PreAuthorize("hasRole('ADMIN')") // Restrict to admin users only
public class MigrationController {

    @Autowired
    private BudgetCategoryMigrationService migrationService;

    /**
     * Migrate budget categories from string names to enum keys
     */
    @PostMapping("/budget-categories/migrate")
    public ResponseEntity<Map<String, Object>> migrateBudgetCategories() {
        try {
            MigrationResult result = migrationService.migrateCategoriesToEnumKeys();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Migration completed successfully",
                "migrated", result.getMigratedCount(),
                "skipped", result.getSkippedCount(),
                "errors", result.getErrorCount(),
                "total", result.getTotalCount()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Migration failed: " + e.getMessage(),
                "error", e.getClass().getSimpleName()
            ));
        }
    }
    
    /**
     * Validate consistency of budget categories with enum keys
     */
    @GetMapping("/budget-categories/validate")
    public ResponseEntity<Map<String, Object>> validateBudgetCategories() {
        try {
            ValidationResult result = migrationService.validateCategoryConsistency();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", result.getValidCount(),
                "inconsistent", result.getInconsistentCount(),
                "total", result.getTotalCount(),
                "allValid", result.isAllValid()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Validation failed: " + e.getMessage(),
                "error", e.getClass().getSimpleName()
            ));
        }
    }
    
    /**
     * Get migration status and information
     */
    @GetMapping("/budget-categories/status")
    public ResponseEntity<Map<String, Object>> getMigrationStatus() {
        try {
            ValidationResult result = migrationService.validateCategoryConsistency();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "migrationNeeded", !result.isAllValid() || result.getTotalCount() == 0,
                "status", result.isAllValid() ? "completed" : "pending",
                "details", result.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Status check failed: " + e.getMessage(),
                "error", e.getClass().getSimpleName()
            ));
        }
    }
}