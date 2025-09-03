package com.buckpal.service;

import com.buckpal.entity.Category;
import com.buckpal.entity.User;
import com.buckpal.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class CategoryInitializationService {
    
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public CategoryInitializationService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Create predefined transaction categories for a new user
     */
    public void createPredefinedTransactionCategories(User user) {
        // Use translation keys instead of hardcoded text
        // Frontend will handle the actual translation based on user's locale
        List<CategoryData> predefinedCategories = getPredefinedTransactionCategories();
        
        for (CategoryData categoryData : predefinedCategories) {
            Category category = new Category();
            category.setName(categoryData.name);
            category.setDescription(categoryData.description);
            category.setIconName(categoryData.iconName);
            category.setColorCode(categoryData.colorCode);
            category.setUser(user);
            category.setIsDefault(true); // Mark as predefined
            
            categoryRepository.save(category);
        }
    }
    
    /**
     * Get predefined transaction categories with translation keys
     * Frontend will translate these based on user's locale preference
     */
    private List<CategoryData> getPredefinedTransactionCategories() {
        return Arrays.asList(
            // Income Categories  
            new CategoryData("categories.salary", "categories.salary.description", "💰", "#22c55e", null),
            new CategoryData("categories.freelance", "categories.freelance.description", "💼", "#22c55e", null),
            new CategoryData("categories.investmentIncome", "categories.investmentIncome.description", "📈", "#22c55e", null),
            new CategoryData("categories.otherIncome", "categories.otherIncome.description", "💸", "#22c55e", null),
            
            // Essential Expenses
            new CategoryData("categories.housing", "categories.housing.description", "🏠", "#ef4444", null),
            new CategoryData("categories.utilities", "categories.utilities.description", "⚡", "#ef4444", null),
            new CategoryData("categories.groceries", "categories.groceries.description", "🛒", "#ef4444", null),
            new CategoryData("categories.transportation", "categories.transportation.description", "🚗", "#ef4444", null),
            new CategoryData("categories.insurance", "categories.insurance.description", "🛡️", "#ef4444", null),
            new CategoryData("categories.healthcare", "categories.healthcare.description", "🏥", "#ef4444", null),
            
            // Lifestyle Expenses  
            new CategoryData("categories.diningOut", "categories.diningOut.description", "🍽️", "#f59e0b", null),
            new CategoryData("categories.entertainment", "categories.entertainment.description", "🎬", "#f59e0b", null),
            new CategoryData("categories.shopping", "categories.shopping.description", "🛍️", "#f59e0b", null),
            new CategoryData("categories.personalCare", "categories.personalCare.description", "💄", "#f59e0b", null),
            new CategoryData("categories.hobbies", "categories.hobbies.description", "🎨", "#f59e0b", null),
            new CategoryData("categories.travel", "categories.travel.description", "✈️", "#f59e0b", null),
            
            // Financial
            new CategoryData("categories.savings", "categories.savings.description", "🏦", "#3b82f6", null),
            new CategoryData("categories.investments", "categories.investments.description", "💎", "#3b82f6", null),
            new CategoryData("categories.debtPayments", "categories.debtPayments.description", "💳", "#ef4444", null),
            
            // Other
            new CategoryData("categories.education", "categories.education.description", "📚", "#8b5cf6", null),
            new CategoryData("categories.giftsDonations", "categories.giftsDonations.description", "🎁", "#ec4899", null),
            new CategoryData("categories.businessExpenses", "categories.businessExpenses.description", "📊", "#06b6d4", null),
            new CategoryData("categories.taxes", "categories.taxes.description", "📋", "#6b7280", null),
            new CategoryData("categories.feesCharges", "categories.feesCharges.description", "🏧", "#6b7280", null),
            new CategoryData("categories.miscellaneous", "categories.miscellaneous.description", "❓", "#6b7280", null)
        );
    }
    
    /**
     * Check if user already has predefined categories
     */
    public boolean hasCategories(User user) {
        return categoryRepository.countByUser(user) > 0;
    }
    
    /**
     * Get predefined budget category templates (not user-specific)
     */
    public List<BudgetCategoryTemplate> getPredefinedBudgetCategoryTemplates() {
        return getPredefinedBudgetCategoryTemplates("en");
    }
    
    /**
     * Get predefined budget category templates with translation keys
     */
    public List<BudgetCategoryTemplate> getPredefinedBudgetCategoryTemplates(String locale) {
        // Return templates with translation keys - frontend will handle actual translation
        return Arrays.asList(
            // NEEDS (Essential expenses) - 50% of income typically
            new BudgetCategoryTemplate("budgetCategories.housing", "budgetCategories.housing.description", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#ef4444", "🏠", 25.0),
            new BudgetCategoryTemplate("budgetCategories.utilities", "budgetCategories.utilities.description", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#f97316", "⚡", 5.0),
            new BudgetCategoryTemplate("budgetCategories.groceries", "budgetCategories.groceries.description", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#22c55e", "🛒", 10.0),
            new BudgetCategoryTemplate("budgetCategories.transportation", "budgetCategories.transportation.description", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#3b82f6", "🚗", 8.0),
            new BudgetCategoryTemplate("budgetCategories.insurance", "budgetCategories.insurance.description", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#8b5cf6", "🛡️", 5.0),
            new BudgetCategoryTemplate("budgetCategories.healthcare", "budgetCategories.healthcare.description", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#ec4899", "🏥", 3.0),
            new BudgetCategoryTemplate("budgetCategories.debtPayments", "budgetCategories.debtPayments.description", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#dc2626", "💳", 5.0),
                
            // WANTS (Lifestyle expenses) - 30% of income typically  
            new BudgetCategoryTemplate("budgetCategories.diningOut", "budgetCategories.diningOut.description", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#f59e0b", "🍽️", 8.0),
            new BudgetCategoryTemplate("budgetCategories.entertainment", "budgetCategories.entertainment.description", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#10b981", "🎬", 6.0),
            new BudgetCategoryTemplate("budgetCategories.shopping", "budgetCategories.shopping.description", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#6366f1", "🛍️", 8.0),
            new BudgetCategoryTemplate("budgetCategories.personalCare", "budgetCategories.personalCare.description", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#f43f5e", "💄", 3.0),
            new BudgetCategoryTemplate("budgetCategories.hobbies", "budgetCategories.hobbies.description", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#06b6d4", "🎨", 3.0),
            new BudgetCategoryTemplate("budgetCategories.travel", "budgetCategories.travel.description", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#8b5cf6", "✈️", 5.0),
                
            // SAVINGS & INVESTMENTS - 20% of income typically
            new BudgetCategoryTemplate("budgetCategories.emergencyFund", "budgetCategories.emergencyFund.description", 
                BudgetCategoryTemplate.CategoryType.SAVINGS, "#059669", "🏦", 10.0),
            new BudgetCategoryTemplate("budgetCategories.retirement", "budgetCategories.retirement.description", 
                BudgetCategoryTemplate.CategoryType.SAVINGS, "#7c3aed", "👴", 8.0),
            new BudgetCategoryTemplate("budgetCategories.investments", "budgetCategories.investments.description", 
                BudgetCategoryTemplate.CategoryType.SAVINGS, "#0891b2", "📈", 5.0),
            new BudgetCategoryTemplate("budgetCategories.goals", "budgetCategories.goals.description", 
                BudgetCategoryTemplate.CategoryType.SAVINGS, "#c2410c", "🎯", 2.0),
                
            // ADDITIONAL CATEGORIES
            new BudgetCategoryTemplate("budgetCategories.education", "budgetCategories.education.description", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#7c2d12", "📚", 2.0),
            new BudgetCategoryTemplate("budgetCategories.giftsDonations", "budgetCategories.giftsDonations.description", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#be185d", "🎁", 2.0),
            new BudgetCategoryTemplate("budgetCategories.businessExpenses", "budgetCategories.businessExpenses.description", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#374151", "📊", 1.0),
            new BudgetCategoryTemplate("budgetCategories.miscellaneous", "budgetCategories.miscellaneous.description", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#6b7280", "❓", 1.0),
                
            // PROJETS PERSO - For custom user projects
            new BudgetCategoryTemplate("budgetCategories.personalProjects", "budgetCategories.personalProjects.description", 
                BudgetCategoryTemplate.CategoryType.PERSONAL_PROJECTS, "#9333ea", "🚀", 10.0)
        );
    }
    
    private List<BudgetCategoryTemplate> getEnglishBudgetCategoryTemplates() {
        return Arrays.asList(
            // NEEDS (Essential expenses) - 50% of income typically
            new BudgetCategoryTemplate("Housing & Rent", "Rent, mortgage, property taxes", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#ef4444", "🏠", 25.0),
            new BudgetCategoryTemplate("Utilities", "Electricity, water, gas, internet, phone", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#f97316", "⚡", 5.0),
            new BudgetCategoryTemplate("Groceries", "Food and household essentials", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#22c55e", "🛒", 10.0),
            new BudgetCategoryTemplate("Transportation", "Public transport, gas, car payments", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#3b82f6", "🚗", 8.0),
            new BudgetCategoryTemplate("Insurance", "Health, auto, home insurance premiums", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#8b5cf6", "🛡️", 5.0),
            new BudgetCategoryTemplate("Healthcare", "Medical expenses, prescriptions", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#ec4899", "🏥", 3.0),
            new BudgetCategoryTemplate("Minimum Debt Payments", "Required loan and credit card payments", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#dc2626", "💳", 5.0),
                
            // WANTS (Lifestyle expenses) - 30% of income typically  
            new BudgetCategoryTemplate("Dining Out", "Restaurants, takeout, coffee", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#f59e0b", "🍽️", 8.0),
            new BudgetCategoryTemplate("Entertainment", "Movies, events, subscriptions, games", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#10b981", "🎬", 6.0),
            new BudgetCategoryTemplate("Shopping", "Clothing, electronics, non-essential purchases", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#6366f1", "🛍️", 8.0),
            new BudgetCategoryTemplate("Personal Care", "Haircuts, cosmetics, spa, gym", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#f43f5e", "💄", 3.0),
            new BudgetCategoryTemplate("Hobbies", "Sports, crafts, hobby supplies", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#06b6d4", "🎨", 3.0),
            new BudgetCategoryTemplate("Travel & Vacation", "Trips, hotels, vacation expenses", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#8b5cf6", "✈️", 5.0),
                
            // SAVINGS & INVESTMENTS - 20% of income typically
            new BudgetCategoryTemplate("Emergency Fund", "Emergency savings (3-6 months expenses)", 
                BudgetCategoryTemplate.CategoryType.SAVINGS, "#059669", "🏦", 10.0),
            new BudgetCategoryTemplate("Retirement", "401k, IRA, pension contributions", 
                BudgetCategoryTemplate.CategoryType.SAVINGS, "#7c3aed", "👴", 8.0),
            new BudgetCategoryTemplate("Investments", "Stocks, bonds, mutual funds", 
                BudgetCategoryTemplate.CategoryType.SAVINGS, "#0891b2", "📈", 5.0),
            new BudgetCategoryTemplate("Goals & Projects", "Saving for specific goals", 
                BudgetCategoryTemplate.CategoryType.SAVINGS, "#c2410c", "🎯", 2.0),
                
            // ADDITIONAL CATEGORIES
            new BudgetCategoryTemplate("Education", "Courses, books, skill development", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#7c2d12", "📚", 2.0),
            new BudgetCategoryTemplate("Gifts & Donations", "Birthday gifts, charity, donations", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#be185d", "🎁", 2.0),
            new BudgetCategoryTemplate("Business Expenses", "Work-related costs", 
                BudgetCategoryTemplate.CategoryType.NEEDS, "#374151", "📊", 1.0),
            new BudgetCategoryTemplate("Miscellaneous", "Other uncategorized expenses", 
                BudgetCategoryTemplate.CategoryType.WANTS, "#6b7280", "❓", 1.0),
                
            // PERSONAL PROJECTS - For custom user projects
            new BudgetCategoryTemplate("Personal Projects", "Custom user projects and goals", 
                BudgetCategoryTemplate.CategoryType.PERSONAL_PROJECTS, "#9333ea", "🚀", 10.0)
        );
    }
    
    private static class CategoryData {
        final String name;
        final String description;
        final String iconName;
        final String colorCode;
        final String parentName; // For hierarchical categories
        
        CategoryData(String name, String description, String iconName, String colorCode, String parentName) {
            this.name = name;
            this.description = description;
            this.iconName = iconName;
            this.colorCode = colorCode;
            this.parentName = parentName;
        }
    }
    
    public static class BudgetCategoryTemplate {
        public enum CategoryType {
            NEEDS, WANTS, SAVINGS, PERSONAL_PROJECTS
        }
        
        public final String name;
        public final String description;
        public final CategoryType categoryType;
        public final String colorCode;
        public final String iconName;
        public final Double suggestedPercentage;
        
        public BudgetCategoryTemplate(String name, String description, CategoryType categoryType, 
                                    String colorCode, String iconName, Double suggestedPercentage) {
            this.name = name;
            this.description = description;
            this.categoryType = categoryType;
            this.colorCode = colorCode;
            this.iconName = iconName;
            this.suggestedPercentage = suggestedPercentage;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public CategoryType getCategoryType() { return categoryType; }
        public String getColorCode() { return colorCode; }
        public String getIconName() { return iconName; }
        public Double getSuggestedPercentage() { return suggestedPercentage; }
    }
}