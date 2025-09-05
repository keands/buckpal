package com.buckpal.entity;

/**
 * Enum representing standardized budget category keys for internationalization.
 * Each category has an i18n key that maps to translations in the frontend.
 * This eliminates the need to store translated text directly in the database.
 */
public enum BudgetCategoryKey {
    // Main budget categories (4 categories model)
    NEEDS("budgetCategories.needs"),
    WANTS("budgetCategories.wants"), 
    SAVINGS("budgetCategories.savings"),
    PROJECTS("budgetCategories.personalProjects"),
    
    // Legacy/Alternative categories
    DEBT("budgetCategories.debt"),
    EMERGENCY("budgetCategories.emergency"),
    INVESTMENTS("budgetCategories.investments"),
    HOUSING("budgetCategories.housing"),
    TRANSPORTATION("budgetCategories.transportation"),
    FOOD("budgetCategories.food"),
    UTILITIES("budgetCategories.utilities"),
    HEALTHCARE("budgetCategories.healthcare"),
    ENTERTAINMENT("budgetCategories.entertainment"),
    CLOTHING("budgetCategories.clothing"),
    EDUCATION("budgetCategories.education"),
    GIFTS("budgetCategories.gifts"),
    SUBSCRIPTIONS("budgetCategories.subscriptions"),
    MISCELLANEOUS("budgetCategories.miscellaneous");
    
    private final String i18nKey;
    
    BudgetCategoryKey(String i18nKey) {
        this.i18nKey = i18nKey;
    }
    
    public String getI18nKey() {
        return i18nKey;
    }
    
    /**
     * Get BudgetCategoryKey from i18n key string
     * @param i18nKey The internationalization key
     * @return The corresponding BudgetCategoryKey or null if not found
     */
    public static BudgetCategoryKey fromI18nKey(String i18nKey) {
        if (i18nKey == null) return null;
        
        for (BudgetCategoryKey key : values()) {
            if (key.getI18nKey().equals(i18nKey)) {
                return key;
            }
        }
        return null;
    }
    
    /**
     * Check if this category is a main budget category (4 categories model)
     */
    public boolean isMainCategory() {
        return this == NEEDS || this == WANTS || this == SAVINGS || this == PROJECTS;
    }
    
    /**
     * Get the category type based on the budget category
     */
    public BudgetCategory.BudgetCategoryType getCategoryType() {
        switch (this) {
            case SAVINGS:
            case EMERGENCY:
            case INVESTMENTS:
                return BudgetCategory.BudgetCategoryType.SAVINGS;
            case DEBT:
                return BudgetCategory.BudgetCategoryType.DEBT;
            case PROJECTS:
                return BudgetCategory.BudgetCategoryType.PROJECT;
            default:
                return BudgetCategory.BudgetCategoryType.EXPENSE;
        }
    }
}