package com.buckpal.service;

import com.buckpal.entity.Category;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private CategoryInitializationService categoryInitializationService;
    
    private final Map<String, String[]> categoryKeywords = new HashMap<String, String[]>() {{
        put("categories.groceries", new String[]{"lidl", "carrefour", "leclerc", "auchan", "monoprix", "restaurant", "cafe", "food", "dining", "pizza", "mcdonald", "burger", "grocery", "supermarket", "walmart", "target"});
        put("categories.transportation", new String[]{"uber", "lyft", "taxi", "gas", "fuel", "parking", "metro", "bus", "train", "airline", "essence", "sncf"});
        put("categories.shopping", new String[]{"amazon", "ebay", "store", "shop", "retail", "clothing", "electronics", "mall", "fnac", "zara", "h&m"});
        put("categories.entertainment", new String[]{"netflix", "spotify", "movie", "theater", "game", "concert", "music", "streaming", "cinema"});
        put("categories.utilities", new String[]{"electric", "gas", "water", "internet", "phone", "cable", "insurance", "utility", "edf", "engie", "orange", "sfr"});
        put("categories.healthcare", new String[]{"pharmacy", "doctor", "hospital", "medical", "dental", "health", "clinic", "pharmacie", "medecin"});
        put("categories.housing", new String[]{"rent", "loyer", "mortgage", "housing", "logement"});
        put("categories.diningOut", new String[]{"restaurant", "cafe", "bar", "brasserie"});
        put("categories.personalCare", new String[]{"coiffeur", "beaute", "sephora", "pharmacy"});
        put("categories.savings", new String[]{"epargne", "savings", "livret"});
        put("categories.salary", new String[]{"salary", "payroll", "deposit", "refund", "bonus", "interest", "salaire"});
        put("Transfer", new String[]{"transfer", "atm", "withdrawal", "deposit", "virement"});
    }};
    
    /**
     * Initialize default categories for a new user
     */
    public void initializeCategoriesForUser(User user) {
        if (!categoryInitializationService.hasCategories(user)) {
            categoryInitializationService.createPredefinedTransactionCategories(user);
        }
    }
    
    /**
     * Get the detailed category mapping for budget categories
     * Maps detailed categories to budget category types
     */
    public Map<String, String> getDetailedToBudgetCategoryMapping() {
        Map<String, String> mapping = new HashMap<>();
        
        // REVENUS (Income) - These are income categories, not expense categories
        // They should not be mapped to budget categories as they represent money coming in
        // Note: Income categories are handled separately in the UI and are not part of budget allocation
        
        // BESOINS (Needs) - Essential expenses
        mapping.put("categories.groceries", "budgetCategories.needs");
        mapping.put("categories.housing", "budgetCategories.needs");
        mapping.put("categories.utilities", "budgetCategories.needs");
        mapping.put("categories.transportation", "budgetCategories.needs");
        mapping.put("categories.insurance", "budgetCategories.needs");
        mapping.put("categories.healthcare", "budgetCategories.needs");
        mapping.put("categories.debtPayments", "budgetCategories.needs");
        mapping.put("categories.businessExpenses", "budgetCategories.needs");
        mapping.put("categories.taxes", "budgetCategories.needs");
        mapping.put("categories.feesCharges", "budgetCategories.needs");
        
        // CE QUE JE VEUX (Wants) - Non-essential expenses  
        mapping.put("categories.shopping", "budgetCategories.wants");
        mapping.put("categories.entertainment", "budgetCategories.wants");
        mapping.put("categories.diningOut", "budgetCategories.wants");
        mapping.put("categories.personalCare", "budgetCategories.wants");
        mapping.put("categories.hobbies", "budgetCategories.wants");
        mapping.put("categories.travel", "budgetCategories.wants");
        mapping.put("categories.education", "budgetCategories.wants");
        mapping.put("categories.giftsDonations", "budgetCategories.wants");
        mapping.put("categories.miscellaneous", "budgetCategories.wants");
        
        // ÉPARGNE (Savings)
        mapping.put("categories.savings", "budgetCategories.savings");
        mapping.put("categories.investments", "budgetCategories.savings");
        
        return mapping;
    }
    
    /**
     * Get the detailed category mapping including custom categories
     * For API endpoint use - includes default mapping for custom categories
     */
    public Map<String, String> getDetailedToBudgetCategoryMappingWithCustom() {
        Map<String, String> baseMappingTemplate = getDetailedToBudgetCategoryMapping();
        Map<String, String> actualMapping = new HashMap<>();
        
        // Get all actual categories from database
        List<Category> allCategories = getAllCategories();
        
        // Define income categories to exclude from budget mapping
        Set<String> incomeCategories = Set.of(
            "categories.salary",
            "categories.freelance", 
            "categories.investmentIncome",
            "categories.otherIncome"
        );
        
        // Only include mappings for categories that actually exist in database
        for (Category category : allCategories) {
            String categoryName = category.getName();
            
            // Skip income categories - they are not budget expense categories
            if (incomeCategories.contains(categoryName)) {
                continue;
            }
            
            if (categoryName.startsWith("categories.")) {
                // For predefined categories, use the base mapping if available
                String budgetCategory = baseMappingTemplate.getOrDefault(categoryName, "budgetCategories.needs");
                actualMapping.put(categoryName, budgetCategory);
            } else {
                // For custom categories, map to "Projets Perso"
                actualMapping.put(categoryName, "budgetCategories.personalProjects");
            }
        }
        
        return actualMapping;
    }
    
    /**
     * Auto-assign detailed category to transaction and suggest budget category
     */
    public CategoryAssignmentResult autoAssignTransaction(Transaction transaction) {
        Category detailedCategory = categorizeTransaction(transaction);
        
        // Get suggested budget category based on detailed category
        String suggestedBudgetCategory = getDetailedToBudgetCategoryMapping()
                .getOrDefault(detailedCategory.getName(), "Besoins");
        
        return new CategoryAssignmentResult(
            detailedCategory,
            suggestedBudgetCategory,
            calculateConfidence(transaction, detailedCategory)
        );
    }
    
    /**
     * Calculate confidence score for auto-assignment
     */
    private double calculateConfidence(Transaction transaction, Category assignedCategory) {
        String description = transaction.getDescription().toLowerCase();
        String merchantName = transaction.getMerchantName() != null ? 
            transaction.getMerchantName().toLowerCase() : "";
        
        String[] keywords = categoryKeywords.get(assignedCategory.getName());
        if (keywords == null) return 0.5; // Default confidence
        
        int matchingKeywords = 0;
        for (String keyword : keywords) {
            if (description.contains(keyword) || merchantName.contains(keyword)) {
                matchingKeywords++;
            }
        }
        
        // Higher confidence with more keyword matches
        return Math.min(0.9, 0.5 + (matchingKeywords * 0.15));
    }
    
    /**
     * Result class for category assignment
     */
    public static class CategoryAssignmentResult {
        private Category detailedCategory;
        private String suggestedBudgetCategory;
        private double confidence;
        
        public CategoryAssignmentResult(Category detailedCategory, 
                                      String suggestedBudgetCategory, 
                                      double confidence) {
            this.detailedCategory = detailedCategory;
            this.suggestedBudgetCategory = suggestedBudgetCategory;
            this.confidence = confidence;
        }
        
        // Getters
        public Category getDetailedCategory() { return detailedCategory; }
        public String getSuggestedBudgetCategory() { return suggestedBudgetCategory; }
        public double getConfidence() { return confidence; }
    }
    
    public Category categorizeTransaction(Transaction transaction) {
        String description = transaction.getDescription().toLowerCase();
        String merchantName = transaction.getMerchantName() != null ? 
            transaction.getMerchantName().toLowerCase() : "";
        
        for (Map.Entry<String, String[]> entry : categoryKeywords.entrySet()) {
            String categoryName = entry.getKey();
            String[] keywords = entry.getValue();
            
            for (String keyword : keywords) {
                if (description.contains(keyword) || merchantName.contains(keyword)) {
                    Optional<Category> category = categoryRepository.findByName(categoryName);
                    if (category.isPresent()) {
                        return category.get();
                    }
                }
            }
        }
        
        // Default category for uncategorized transactions
        return categoryRepository.findByName("Other")
            .orElseGet(() -> {
                Category defaultCategory = new Category();
                defaultCategory.setName("Other");
                defaultCategory.setDescription("Uncategorized transactions");
                defaultCategory.setIsDefault(true);
                return categoryRepository.save(defaultCategory);
            });
    }
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    public List<Category> getCategoriesForUser(User user) {
        return categoryRepository.findByUser(user);
    }
    
    public List<Category> getMainCategoriesForUser(User user) {
        return categoryRepository.findByUserAndParentCategoryIsNull(user);
    }
    
    public List<Category> getMainCategories() {
        return categoryRepository.findByParentCategoryIsNull();
    }
    
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
    
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }
    
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}