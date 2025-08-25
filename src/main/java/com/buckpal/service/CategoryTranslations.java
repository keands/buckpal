package com.buckpal.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized translation system for predefined categories
 * Makes it easy to add new languages by simply adding new translation maps
 */
public class CategoryTranslations {
    
    /**
     * Transaction category translations
     * Structure: Map<locale, Map<categoryKey, CategoryTranslation>>
     */
    private static final Map<String, Map<String, CategoryTranslation>> TRANSACTION_CATEGORIES = new HashMap<>();
    
    /**
     * Budget category template translations
     * Structure: Map<locale, List<BudgetCategoryTranslation>>
     */
    private static final Map<String, List<BudgetCategoryTranslation>> BUDGET_CATEGORIES = new HashMap<>();
    
    static {
        initializeTransactionCategories();
        initializeBudgetCategories();
    }
    
    // Transaction Category Translations
    private static void initializeTransactionCategories() {
        // English (default)
        Map<String, CategoryTranslation> enTransactions = new HashMap<>();
        enTransactions.put("salary", new CategoryTranslation("Salary", "Monthly salary income", "💰", "#22c55e"));
        enTransactions.put("freelance", new CategoryTranslation("Freelance", "Freelance work income", "💼", "#22c55e"));
        enTransactions.put("investment_income", new CategoryTranslation("Investment Income", "Dividends, interest, and other investment income", "📈", "#22c55e"));
        enTransactions.put("other_income", new CategoryTranslation("Other Income", "Miscellaneous income sources", "💸", "#22c55e"));
        enTransactions.put("housing", new CategoryTranslation("Housing", "Rent, mortgage, and housing costs", "🏠", "#ef4444"));
        enTransactions.put("utilities", new CategoryTranslation("Utilities", "Electricity, water, gas, internet", "⚡", "#ef4444"));
        enTransactions.put("groceries", new CategoryTranslation("Groceries", "Food and household essentials", "🛒", "#ef4444"));
        enTransactions.put("transportation", new CategoryTranslation("Transportation", "Public transport, gas, car maintenance", "🚗", "#ef4444"));
        enTransactions.put("insurance", new CategoryTranslation("Insurance", "Health, auto, home insurance", "🛡️", "#ef4444"));
        enTransactions.put("healthcare", new CategoryTranslation("Healthcare", "Medical expenses, pharmacy", "🏥", "#ef4444"));
        enTransactions.put("dining_out", new CategoryTranslation("Dining Out", "Restaurants and takeout", "🍽️", "#f59e0b"));
        enTransactions.put("entertainment", new CategoryTranslation("Entertainment", "Movies, events, subscriptions", "🎬", "#f59e0b"));
        enTransactions.put("shopping", new CategoryTranslation("Shopping", "Clothing, electronics, general shopping", "🛍️", "#f59e0b"));
        enTransactions.put("personal_care", new CategoryTranslation("Personal Care", "Haircuts, cosmetics, personal items", "💄", "#f59e0b"));
        enTransactions.put("hobbies", new CategoryTranslation("Hobbies", "Sports, crafts, hobby expenses", "🎨", "#f59e0b"));
        enTransactions.put("travel", new CategoryTranslation("Travel", "Vacations and travel expenses", "✈️", "#f59e0b"));
        enTransactions.put("savings", new CategoryTranslation("Savings", "Emergency fund and general savings", "🏦", "#3b82f6"));
        enTransactions.put("investments", new CategoryTranslation("Investments", "Stock purchases, retirement contributions", "💎", "#3b82f6"));
        enTransactions.put("debt_payments", new CategoryTranslation("Debt Payments", "Loan payments, credit card payments", "💳", "#ef4444"));
        enTransactions.put("education", new CategoryTranslation("Education", "Courses, books, learning expenses", "📚", "#8b5cf6"));
        enTransactions.put("gifts_donations", new CategoryTranslation("Gifts & Donations", "Gifts, charity, donations", "🎁", "#ec4899"));
        enTransactions.put("business_expenses", new CategoryTranslation("Business Expenses", "Work-related expenses", "📊", "#06b6d4"));
        enTransactions.put("taxes", new CategoryTranslation("Taxes", "Tax payments and related expenses", "📋", "#6b7280"));
        enTransactions.put("fees_charges", new CategoryTranslation("Fees & Charges", "Bank fees, service charges", "🏧", "#6b7280"));
        enTransactions.put("miscellaneous", new CategoryTranslation("Miscellaneous", "Other uncategorized expenses", "❓", "#6b7280"));
        TRANSACTION_CATEGORIES.put("en", enTransactions);
        
        // French
        Map<String, CategoryTranslation> frTransactions = new HashMap<>();
        frTransactions.put("salary", new CategoryTranslation("Salaire", "Salaire mensuel", "💰", "#22c55e"));
        frTransactions.put("freelance", new CategoryTranslation("Freelance", "Revenus freelance", "💼", "#22c55e"));
        frTransactions.put("investment_income", new CategoryTranslation("Revenus d'investissement", "Dividendes, intérêts et autres revenus d'investissement", "📈", "#22c55e"));
        frTransactions.put("other_income", new CategoryTranslation("Autres revenus", "Sources de revenus diverses", "💸", "#22c55e"));
        frTransactions.put("housing", new CategoryTranslation("Logement", "Loyer, prêt immobilier et frais de logement", "🏠", "#ef4444"));
        frTransactions.put("utilities", new CategoryTranslation("Services publics", "Électricité, eau, gaz, internet", "⚡", "#ef4444"));
        frTransactions.put("groceries", new CategoryTranslation("Alimentation", "Nourriture et produits de première nécessité", "🛒", "#ef4444"));
        frTransactions.put("transportation", new CategoryTranslation("Transport", "Transport public, essence, entretien voiture", "🚗", "#ef4444"));
        frTransactions.put("insurance", new CategoryTranslation("Assurance", "Assurance santé, auto, habitation", "🛡️", "#ef4444"));
        frTransactions.put("healthcare", new CategoryTranslation("Santé", "Frais médicaux, pharmacie", "🏥", "#ef4444"));
        frTransactions.put("dining_out", new CategoryTranslation("Restaurants", "Restaurants et plats à emporter", "🍽️", "#f59e0b"));
        frTransactions.put("entertainment", new CategoryTranslation("Divertissement", "Cinéma, événements, abonnements", "🎬", "#f59e0b"));
        frTransactions.put("shopping", new CategoryTranslation("Shopping", "Vêtements, électronique, achats généraux", "🛍️", "#f59e0b"));
        frTransactions.put("personal_care", new CategoryTranslation("Soins personnels", "Coiffure, cosmétiques, articles personnels", "💄", "#f59e0b"));
        frTransactions.put("hobbies", new CategoryTranslation("Loisirs", "Sports, bricolage, dépenses de loisirs", "🎨", "#f59e0b"));
        frTransactions.put("travel", new CategoryTranslation("Voyage", "Vacances et frais de voyage", "✈️", "#f59e0b"));
        frTransactions.put("savings", new CategoryTranslation("Épargne", "Fonds d'urgence et épargne générale", "🏦", "#3b82f6"));
        frTransactions.put("investments", new CategoryTranslation("Investissements", "Achat d'actions, cotisations retraite", "💎", "#3b82f6"));
        frTransactions.put("debt_payments", new CategoryTranslation("Remboursement dettes", "Remboursements d'emprunts et cartes de crédit", "💳", "#ef4444"));
        frTransactions.put("education", new CategoryTranslation("Éducation", "Cours, livres, dépenses d'apprentissage", "📚", "#8b5cf6"));
        frTransactions.put("gifts_donations", new CategoryTranslation("Cadeaux & Dons", "Cadeaux, charité, dons", "🎁", "#ec4899"));
        frTransactions.put("business_expenses", new CategoryTranslation("Frais professionnels", "Dépenses liées au travail", "📊", "#06b6d4"));
        frTransactions.put("taxes", new CategoryTranslation("Impôts", "Paiements d'impôts et frais connexes", "📋", "#6b7280"));
        frTransactions.put("fees_charges", new CategoryTranslation("Frais & Charges", "Frais bancaires, frais de service", "🏧", "#6b7280"));
        frTransactions.put("miscellaneous", new CategoryTranslation("Divers", "Autres dépenses non catégorisées", "❓", "#6b7280"));
        TRANSACTION_CATEGORIES.put("fr", frTransactions);
        
        // Spanish (example for extensibility)
        Map<String, CategoryTranslation> esTransactions = new HashMap<>();
        esTransactions.put("salary", new CategoryTranslation("Salario", "Salario mensual", "💰", "#22c55e"));
        esTransactions.put("freelance", new CategoryTranslation("Freelance", "Ingresos por trabajo independiente", "💼", "#22c55e"));
        esTransactions.put("investment_income", new CategoryTranslation("Ingresos de Inversión", "Dividendos, intereses y otros ingresos de inversión", "📈", "#22c55e"));
        esTransactions.put("other_income", new CategoryTranslation("Otros Ingresos", "Fuentes de ingresos diversas", "💸", "#22c55e"));
        esTransactions.put("housing", new CategoryTranslation("Vivienda", "Alquiler, hipoteca y costos de vivienda", "🏠", "#ef4444"));
        esTransactions.put("utilities", new CategoryTranslation("Servicios Públicos", "Electricidad, agua, gas, internet", "⚡", "#ef4444"));
        esTransactions.put("groceries", new CategoryTranslation("Alimentación", "Comida y productos esenciales del hogar", "🛒", "#ef4444"));
        // ... add more Spanish translations as needed
        TRANSACTION_CATEGORIES.put("es", esTransactions);
    }
    
    // Budget Category Translations
    private static void initializeBudgetCategories() {
        // English Budget Categories
        List<BudgetCategoryTranslation> enBudget = Arrays.asList(
            new BudgetCategoryTranslation("housing_rent", "Housing & Rent", "Rent, mortgage, property taxes", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#ef4444", "🏠", 25.0),
            new BudgetCategoryTranslation("utilities", "Utilities", "Electricity, water, gas, internet, phone", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#f97316", "⚡", 5.0),
            new BudgetCategoryTranslation("groceries", "Groceries", "Food and household essentials", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#22c55e", "🛒", 10.0),
            new BudgetCategoryTranslation("transportation", "Transportation", "Public transport, gas, car payments", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#3b82f6", "🚗", 8.0),
            new BudgetCategoryTranslation("insurance", "Insurance", "Health, auto, home insurance premiums", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#8b5cf6", "🛡️", 5.0),
            new BudgetCategoryTranslation("healthcare", "Healthcare", "Medical expenses, prescriptions", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#ec4899", "🏥", 3.0),
            new BudgetCategoryTranslation("debt_payments", "Minimum Debt Payments", "Required loan and credit card payments", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#dc2626", "💳", 5.0),
            new BudgetCategoryTranslation("dining_out", "Dining Out", "Restaurants, takeout, coffee", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#f59e0b", "🍽️", 8.0),
            new BudgetCategoryTranslation("entertainment", "Entertainment", "Movies, events, subscriptions, games", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#10b981", "🎬", 6.0),
            new BudgetCategoryTranslation("shopping", "Shopping", "Clothing, electronics, non-essential purchases", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#6366f1", "🛍️", 8.0),
            new BudgetCategoryTranslation("personal_care", "Personal Care", "Haircuts, cosmetics, spa, gym", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#f43f5e", "💄", 3.0),
            new BudgetCategoryTranslation("hobbies", "Hobbies", "Sports, crafts, hobby supplies", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#06b6d4", "🎨", 3.0),
            new BudgetCategoryTranslation("travel", "Travel & Vacation", "Trips, hotels, vacation expenses", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#8b5cf6", "✈️", 5.0),
            new BudgetCategoryTranslation("emergency_fund", "Emergency Fund", "Emergency savings (3-6 months expenses)", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.SAVINGS, "#059669", "🏦", 10.0),
            new BudgetCategoryTranslation("retirement", "Retirement", "401k, IRA, pension contributions", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.SAVINGS, "#7c3aed", "👴", 8.0),
            new BudgetCategoryTranslation("investments", "Investments", "Stocks, bonds, mutual funds", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.SAVINGS, "#0891b2", "📈", 5.0),
            new BudgetCategoryTranslation("goals", "Goals & Projects", "Saving for specific goals", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.SAVINGS, "#c2410c", "🎯", 2.0),
            new BudgetCategoryTranslation("education", "Education", "Courses, books, skill development", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#7c2d12", "📚", 2.0),
            new BudgetCategoryTranslation("gifts_donations", "Gifts & Donations", "Birthday gifts, charity, donations", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#be185d", "🎁", 2.0),
            new BudgetCategoryTranslation("business_expenses", "Business Expenses", "Work-related costs", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#374151", "📊", 1.0),
            new BudgetCategoryTranslation("miscellaneous", "Miscellaneous", "Other uncategorized expenses", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#6b7280", "❓", 1.0)
        );
        BUDGET_CATEGORIES.put("en", enBudget);
        
        // French Budget Categories
        List<BudgetCategoryTranslation> frBudget = Arrays.asList(
            new BudgetCategoryTranslation("housing_rent", "Logement & Loyer", "Loyer, prêt immobilier, taxes foncières", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#ef4444", "🏠", 25.0),
            new BudgetCategoryTranslation("utilities", "Services publics", "Électricité, eau, gaz, internet, téléphone", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#f97316", "⚡", 5.0),
            new BudgetCategoryTranslation("groceries", "Alimentation", "Nourriture et produits de première nécessité", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#22c55e", "🛒", 10.0),
            new BudgetCategoryTranslation("transportation", "Transport", "Transport public, essence, paiements voiture", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#3b82f6", "🚗", 8.0),
            new BudgetCategoryTranslation("insurance", "Assurance", "Primes assurance santé, auto, habitation", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#8b5cf6", "🛡️", 5.0),
            new BudgetCategoryTranslation("healthcare", "Santé", "Frais médicaux, ordonnances", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#ec4899", "🏥", 3.0),
            new BudgetCategoryTranslation("debt_payments", "Paiements dettes minimum", "Remboursements obligatoires emprunts et cartes", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#dc2626", "💳", 5.0),
            new BudgetCategoryTranslation("dining_out", "Restaurants", "Restaurants, plats à emporter, café", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#f59e0b", "🍽️", 8.0),
            new BudgetCategoryTranslation("entertainment", "Divertissement", "Cinéma, événements, abonnements, jeux", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#10b981", "🎬", 6.0),
            new BudgetCategoryTranslation("shopping", "Shopping", "Vêtements, électronique, achats non essentiels", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#6366f1", "🛍️", 8.0),
            new BudgetCategoryTranslation("personal_care", "Soins personnels", "Coiffure, cosmétiques, spa, salle de sport", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#f43f5e", "💄", 3.0),
            new BudgetCategoryTranslation("hobbies", "Loisirs", "Sports, bricolage, fournitures loisirs", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#06b6d4", "🎨", 3.0),
            new BudgetCategoryTranslation("travel", "Voyage & Vacances", "Voyages, hôtels, frais vacances", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#8b5cf6", "✈️", 5.0),
            new BudgetCategoryTranslation("emergency_fund", "Fonds d'urgence", "Épargne d'urgence (3-6 mois dépenses)", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.SAVINGS, "#059669", "🏦", 10.0),
            new BudgetCategoryTranslation("retirement", "Retraite", "Cotisations 401k, IRA, pension", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.SAVINGS, "#7c3aed", "👴", 8.0),
            new BudgetCategoryTranslation("investments", "Investissements", "Actions, obligations, fonds communs", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.SAVINGS, "#0891b2", "📈", 5.0),
            new BudgetCategoryTranslation("goals", "Objectifs & Projets", "Épargne pour objectifs spécifiques", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.SAVINGS, "#c2410c", "🎯", 2.0),
            new BudgetCategoryTranslation("education", "Éducation", "Cours, livres, développement compétences", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#7c2d12", "📚", 2.0),
            new BudgetCategoryTranslation("gifts_donations", "Cadeaux & Dons", "Cadeaux d'anniversaire, charité, dons", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#be185d", "🎁", 2.0),
            new BudgetCategoryTranslation("business_expenses", "Frais professionnels", "Coûts liés au travail", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS, "#374151", "📊", 1.0),
            new BudgetCategoryTranslation("miscellaneous", "Divers", "Autres dépenses non catégorisées", 
                CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS, "#6b7280", "❓", 1.0)
        );
        BUDGET_CATEGORIES.put("fr", frBudget);
    }
    
    // Public methods to get translations
    public static List<CategoryTranslation> getTransactionCategories(String locale) {
        Map<String, CategoryTranslation> translations = TRANSACTION_CATEGORIES.get(locale);
        if (translations == null) {
            translations = TRANSACTION_CATEGORIES.get("en"); // Fallback to English
        }
        return Arrays.asList(translations.values().toArray(new CategoryTranslation[0]));
    }
    
    public static List<BudgetCategoryTranslation> getBudgetCategories(String locale) {
        List<BudgetCategoryTranslation> translations = BUDGET_CATEGORIES.get(locale);
        if (translations == null) {
            translations = BUDGET_CATEGORIES.get("en"); // Fallback to English
        }
        return translations;
    }
    
    public static boolean isLocaleSupported(String locale) {
        return TRANSACTION_CATEGORIES.containsKey(locale) && BUDGET_CATEGORIES.containsKey(locale);
    }
    
    public static List<String> getSupportedLocales() {
        return Arrays.asList("en", "fr", "es"); // Add new locales as they are implemented
    }
    
    // Inner classes for structured translations
    public static class CategoryTranslation {
        public final String name;
        public final String description;
        public final String iconName;
        public final String colorCode;
        
        public CategoryTranslation(String name, String description, String iconName, String colorCode) {
            this.name = name;
            this.description = description;
            this.iconName = iconName;
            this.colorCode = colorCode;
        }
    }
    
    public static class BudgetCategoryTranslation {
        public final String key;
        public final String name;
        public final String description;
        public final CategoryInitializationService.BudgetCategoryTemplate.CategoryType categoryType;
        public final String colorCode;
        public final String iconName;
        public final Double suggestedPercentage;
        
        public BudgetCategoryTranslation(String key, String name, String description, 
                                       CategoryInitializationService.BudgetCategoryTemplate.CategoryType categoryType,
                                       String colorCode, String iconName, Double suggestedPercentage) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.categoryType = categoryType;
            this.colorCode = colorCode;
            this.iconName = iconName;
            this.suggestedPercentage = suggestedPercentage;
        }
        
        public CategoryInitializationService.BudgetCategoryTemplate toBudgetCategoryTemplate() {
            return new CategoryInitializationService.BudgetCategoryTemplate(
                name, description, categoryType, colorCode, iconName, suggestedPercentage
            );
        }
    }
}