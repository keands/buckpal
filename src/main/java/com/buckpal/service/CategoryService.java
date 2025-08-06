package com.buckpal.service;

import com.buckpal.entity.Category;
import com.buckpal.entity.Transaction;
import com.buckpal.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    private final Map<String, String[]> categoryKeywords = new HashMap<String, String[]>() {{
        put("Food & Dining", new String[]{"restaurant", "cafe", "food", "dining", "pizza", "mcdonald", "burger", "grocery", "supermarket", "walmart", "target"});
        put("Transportation", new String[]{"uber", "lyft", "taxi", "gas", "fuel", "parking", "metro", "bus", "train", "airline"});
        put("Shopping", new String[]{"amazon", "ebay", "store", "shop", "retail", "clothing", "electronics", "mall"});
        put("Entertainment", new String[]{"netflix", "spotify", "movie", "theater", "game", "concert", "music", "streaming"});
        put("Bills & Utilities", new String[]{"electric", "gas", "water", "internet", "phone", "cable", "insurance", "utility"});
        put("Healthcare", new String[]{"pharmacy", "doctor", "hospital", "medical", "dental", "health", "clinic"});
        put("Income", new String[]{"salary", "payroll", "deposit", "refund", "bonus", "interest"});
        put("Transfer", new String[]{"transfer", "atm", "withdrawal", "deposit"});
    }};
    
    public void initializeDefaultCategories() {
        for (String categoryName : categoryKeywords.keySet()) {
            if (categoryRepository.findByName(categoryName).isEmpty()) {
                Category category = new Category();
                category.setName(categoryName);
                category.setDescription("Auto-generated category for " + categoryName.toLowerCase());
                category.setIsDefault(true);
                categoryRepository.save(category);
            }
        }
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