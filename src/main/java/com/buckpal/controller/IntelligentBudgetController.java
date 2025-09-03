package com.buckpal.controller;

import com.buckpal.entity.User;
import com.buckpal.service.IntelligentBudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for intelligent budget suggestions and pattern analysis
 */
@RestController
@RequestMapping("/api/intelligent-budget")
public class IntelligentBudgetController {
    
    private final IntelligentBudgetService intelligentBudgetService;
    
    @Autowired
    public IntelligentBudgetController(IntelligentBudgetService intelligentBudgetService) {
        this.intelligentBudgetService = intelligentBudgetService;
    }
    
    /**
     * Get income patterns analysis for user
     */
    @GetMapping("/income-patterns")
    public ResponseEntity<List<IntelligentBudgetService.IncomePattern>> getIncomePatterns(
            @RequestParam(defaultValue = "12") int monthsBack,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        try {
            List<IntelligentBudgetService.IncomePattern> patterns = 
                intelligentBudgetService.analyzeIncomePatterns(user, monthsBack);
            
            return ResponseEntity.ok(patterns);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get intelligent suggestions for income transaction categorization
     */
    @PostMapping("/suggest-income-category")
    public ResponseEntity<List<IntelligentBudgetService.IncomeSuggestion>> suggestIncomeCategory(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        String transactionDescription = request.get("description");
        
        if (transactionDescription == null || transactionDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<IntelligentBudgetService.IncomeSuggestion> suggestions = 
                intelligentBudgetService.suggestIncomeCategories(user, transactionDescription);
            
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generate smart budget template based on user's history
     */
    @GetMapping("/smart-budget-template")
    public ResponseEntity<IntelligentBudgetService.SmartBudgetTemplate> getSmartBudgetTemplate(
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        try {
            IntelligentBudgetService.SmartBudgetTemplate template = 
                intelligentBudgetService.generateSmartBudgetTemplate(user);
            
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get AI insights for budget creation wizard
     */
    @GetMapping("/wizard-insights")
    public ResponseEntity<Map<String, Object>> getWizardInsights(
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        try {
            // Get smart template
            IntelligentBudgetService.SmartBudgetTemplate template = 
                intelligentBudgetService.generateSmartBudgetTemplate(user);
            
            // Get recent patterns
            List<IntelligentBudgetService.IncomePattern> patterns = 
                intelligentBudgetService.analyzeIncomePatterns(user, 6);
            
            // Create insights response
            Map<String, Object> insights = Map.of(
                "hasHistoricalData", !template.getSuggestedCategories().isEmpty(),
                "suggestedTemplate", template,
                "recentPatterns", patterns.subList(0, Math.min(5, patterns.size())),
                "confidence", calculateOverallConfidence(patterns),
                "message", generateInsightMessage(template, patterns)
            );
            
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper methods
    
    private double calculateOverallConfidence(List<IntelligentBudgetService.IncomePattern> patterns) {
        if (patterns.isEmpty()) return 0.0;
        
        return patterns.stream()
                .mapToDouble(IntelligentBudgetService.IncomePattern::getConfidenceScore)
                .average()
                .orElse(0.0);
    }
    
    private String generateInsightMessage(IntelligentBudgetService.SmartBudgetTemplate template, 
                                        List<IntelligentBudgetService.IncomePattern> patterns) {
        
        if (template.getSuggestedCategories().isEmpty()) {
            return "C'est votre premier budget ! Je vais vous guider dans la création de vos catégories de revenus.";
        }
        
        int categoryCount = template.getSuggestedCategories().size();
        int patternCount = patterns.size();
        
        if (patternCount >= 5) {
            return String.format("J'ai analysé vos %d derniers budgets et identifié %d patterns récurrents. " +
                    "Je peux pré-remplir %d catégories de revenus basées sur votre historique.", 
                    6, patternCount, categoryCount);
        } else if (categoryCount > 0) {
            return String.format("Basé sur vos budgets précédents, je suggère %d catégories de revenus " +
                    "avec des montants estimés.", categoryCount);
        } else {
            return "Je n'ai pas trouvé assez de données historiques, mais je peux vous aider " +
                    "à créer vos catégories de revenus.";
        }
    }
}