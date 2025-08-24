package com.buckpal.controller;

import com.buckpal.dto.BudgetDto;
import com.buckpal.entity.Budget;
import com.buckpal.entity.User;
import com.buckpal.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    
    private final BudgetService budgetService;
    
    @Autowired
    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
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
}