package com.buckpal.controller;

import com.buckpal.dto.BudgetDto;
import com.buckpal.entity.Budget;
import com.buckpal.entity.User;
import com.buckpal.service.BudgetService;
import com.buckpal.service.CategoryInitializationService;
import com.buckpal.service.HistoricalIncomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    
    private final BudgetService budgetService;
    private final HistoricalIncomeService historicalIncomeService;
    
    @Autowired
    public BudgetController(BudgetService budgetService, HistoricalIncomeService historicalIncomeService) {
        this.budgetService = budgetService;
        this.historicalIncomeService = historicalIncomeService;
    }
    
    @PostMapping
    public ResponseEntity<BudgetDto> createBudget(
            @Valid @RequestBody BudgetDto budgetDto,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        BudgetDto createdBudget = budgetService.createBudget(user, budgetDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBudget);
    }
    
    @GetMapping
    public ResponseEntity<List<BudgetDto>> getUserBudgets(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<BudgetDto> budgets = budgetService.getUserBudgets(user);
        return ResponseEntity.ok(budgets);
    }
    
    @GetMapping("/current")
    public ResponseEntity<BudgetDto> getCurrentMonthBudget(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<BudgetDto> budget = budgetService.getCurrentMonthBudget(user);
        return budget.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/previous")
    public ResponseEntity<BudgetDto> getPreviousMonthBudget(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<BudgetDto> budget = budgetService.getPreviousMonthBudget(user);
        return budget.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BudgetDto> getBudgetById(
            @PathVariable Long id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<BudgetDto> budget = budgetService.getBudgetById(user, id);
        return budget.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{year}/{month}")
    public ResponseEntity<BudgetDto> getBudget(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<BudgetDto> budget = budgetService.getBudget(user, month, year);
        return budget.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetDto> updateBudget(
            @PathVariable Long budgetId,
            @Valid @RequestBody BudgetDto budgetDto,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            BudgetDto updatedBudget = budgetService.updateBudget(user, budgetId, budgetDto);
            return ResponseEntity.ok(updatedBudget);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable Long budgetId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            budgetService.deleteBudget(user, budgetId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/from-previous")
    public ResponseEntity<BudgetDto> createBudgetFromPrevious(
            @RequestParam Integer month,
            @RequestParam Integer year,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        BudgetDto budget = budgetService.createBudgetFromPrevious(user, month, year);
        return ResponseEntity.status(HttpStatus.CREATED).body(budget);
    }
    
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> getBudgetModels() {
        Map<String, Object> models = Map.of(
            "models", Budget.BudgetModel.values(),
            "descriptions", Map.of(
                "RULE_50_30_20", "50% needs, 30% wants, 20% savings",
                "RULE_60_20_20", "60% needs, 20% wants, 20% savings",
                "RULE_80_20", "80% expenses, 20% savings",
                "ENVELOPE", "Fixed amounts per category",
                "ZERO_BASED", "Every euro assigned to a category",
                "FRENCH_THIRDS", "1/3 housing, 1/3 living, 1/3 savings",
                "CUSTOM", "User-defined percentages"
            )
        );
        return ResponseEntity.ok(models);
    }
    
    @GetMapping("/models/{model}/percentages")
    public ResponseEntity<Map<String, BigDecimal>> getBudgetModelPercentages(@PathVariable String model) {
        try {
            Budget.BudgetModel budgetModel = Budget.BudgetModel.valueOf(model.toUpperCase());
            Map<String, BigDecimal> percentages = budgetService.getBudgetModelPercentages(budgetModel);
            return ResponseEntity.ok(percentages);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/over-budget")
    public ResponseEntity<List<BudgetDto>> getOverBudgetUsers(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<BudgetDto> overBudgetBudgets = budgetService.getOverBudgetUsers(user);
        return ResponseEntity.ok(overBudgetBudgets);
    }
    
    @PostMapping("/setup-wizard")
    public ResponseEntity<BudgetDto> setupBudgetWizard(
            @RequestBody Map<String, Object> wizardData,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        // Extract wizard data
        Integer month = (Integer) wizardData.get("month");
        Integer year = (Integer) wizardData.get("year");
        String modelStr = (String) wizardData.get("budgetModel");
        BigDecimal projectedIncome = new BigDecimal(wizardData.get("projectedIncome").toString());
        Boolean useFromPrevious = (Boolean) wizardData.getOrDefault("useFromPrevious", false);
        
        BudgetDto budgetDto = new BudgetDto();
        
        if (useFromPrevious) {
            // Create from previous month
            budgetDto = budgetService.createBudgetFromPrevious(user, month, year);
        } else {
            // Create new budget
            budgetDto.setBudgetMonth(month);
            budgetDto.setBudgetYear(year);
            budgetDto.setProjectedIncome(projectedIncome);
            budgetDto.setBudgetModel(Budget.BudgetModel.valueOf(modelStr.toUpperCase()));
            budgetDto = budgetService.createBudget(user, budgetDto);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetDto);
    }
    
    /**
     * Get predefined budget category templates
     */
    @GetMapping("/category-templates")
    public ResponseEntity<List<CategoryInitializationService.BudgetCategoryTemplate>> getBudgetCategoryTemplates() {
        List<CategoryInitializationService.BudgetCategoryTemplate> templates = budgetService.getBudgetCategoryTemplates();
        return ResponseEntity.ok(templates);
    }
    
    // ====== HISTORICAL INCOME ENDPOINTS ======
    
    /**
     * Analyze historical income for a budget period
     */
    @GetMapping("/historical-income/{year}/{month}")
    public ResponseEntity<HistoricalIncomeService.HistoricalIncomeAnalysis> analyzeHistoricalIncome(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            HistoricalIncomeService.HistoricalIncomeAnalysis analysis = 
                    historicalIncomeService.analyzeHistoricalIncome(user, month, year);
            
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Check if budget should detect historical income
     */
    @GetMapping("/{budgetId}/should-detect-historical-income")
    public ResponseEntity<Map<String, Boolean>> shouldDetectHistoricalIncome(
            @PathVariable Long budgetId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Optional<BudgetDto> budgetDto = budgetService.getBudgetById(user, budgetId);
            if (budgetDto.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert DTO to entity for service call
            Budget budget = new Budget();
            budget.setId(budgetDto.get().getId());
            budget.setBudgetMonth(budgetDto.get().getBudgetMonth());
            budget.setBudgetYear(budgetDto.get().getBudgetYear());
            budget.setActualIncome(budgetDto.get().getActualIncome());
            
            boolean shouldDetect = historicalIncomeService.shouldDetectHistoricalIncome(user, budget);
            
            return ResponseEntity.ok(Map.of("shouldDetect", shouldDetect));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Apply historical income to budget
     */
    @PostMapping("/{budgetId}/apply-historical-income")
    public ResponseEntity<BudgetDto> applyHistoricalIncome(
            @PathVariable Long budgetId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            // Get budget
            Optional<BudgetDto> budgetDto = budgetService.getBudgetById(user, budgetId);
            if (budgetDto.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Extract request parameters
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> suggestionsData = 
                    (List<Map<String, Object>>) request.get("suggestions");
            Boolean usePartialIncome = (Boolean) request.getOrDefault("usePartialIncome", false);
            
            // Convert DTO to entity
            Budget budget = new Budget();
            budget.setId(budgetDto.get().getId());
            budget.setBudgetMonth(budgetDto.get().getBudgetMonth());
            budget.setBudgetYear(budgetDto.get().getBudgetYear());
            budget.setUser(user);
            
            // Convert suggestions (simplified for demo - in real implementation, 
            // you'd properly deserialize the suggestions)
            List<HistoricalIncomeService.IncomeCategorySuggestion> suggestions = new ArrayList<>();
            // TODO: Properly convert suggestions from request data
            
            // Apply historical income
            historicalIncomeService.applyHistoricalIncome(user, budget, suggestions, usePartialIncome);
            
            // Return updated budget
            BudgetDto updated = budgetService.getBudgetById(user, budgetId).orElse(budgetDto.get());
            return ResponseEntity.ok(updated);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get income comparison with previous months
     */
    @GetMapping("/income-comparison/{year}/{month}")
    public ResponseEntity<HistoricalIncomeService.IncomeComparison> getIncomeComparison(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestParam(defaultValue = "6") Integer monthsBack,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            HistoricalIncomeService.IncomeComparison comparison = 
                    historicalIncomeService.getIncomeComparison(user, month, year, monthsBack);
            
            return ResponseEntity.ok(comparison);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Find recurring income patterns
     */
    @GetMapping("/recurring-patterns/{year}/{month}")
    public ResponseEntity<List<HistoricalIncomeService.IncomePattern>> getRecurringIncomePatterns(
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            List<HistoricalIncomeService.IncomePattern> patterns = 
                    historicalIncomeService.findRecurringIncomePatterns(user, month, year);
            
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}