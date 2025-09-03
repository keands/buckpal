import { TFunction } from 'react-i18next';

/**
 * Utility functions for budget category translation and mapping
 */

/**
 * Translates a budget category name using i18n
 * Handles both legacy French names and internationalization keys
 */
export function translateBudgetCategoryName(categoryName: string, t: TFunction): string {
  // Handle internationalization keys (budgetCategories.*)
  if (categoryName.startsWith('budgetCategories.')) {
    return t(categoryName);
  }
  
  // Handle legacy French names - map to translation keys
  const legacyMapping: { [key: string]: string } = {
    'Besoins': 'budgetCategories.needs',
    'Ce que je veux': 'budgetCategories.wants', 
    'Épargne': 'budgetCategories.savings',
    'Projets Perso': 'budgetCategories.personalProjects',
    'Dépenses': 'budgetCategories.expenses',
    'Logement': 'budgetCategories.housing',
    'Quotidien': 'budgetCategories.living'
  };
  
  const translationKey = legacyMapping[categoryName] || `budgetCategories.${categoryName.toLowerCase()}`;
  return t(translationKey, { defaultValue: categoryName });
}

/**
 * Translates a detailed category name using i18n
 * Removes "categories." prefix if present and translates
 */
export function translateDetailedCategoryName(categoryName: string, t: TFunction): string {
  // Remove "categories." prefix if present
  const cleanName = categoryName.startsWith('categories.') 
    ? categoryName.replace('categories.', '') 
    : categoryName;
  
  // Try to get translation, fallback to clean name if not found
  return t(`categories.${cleanName}`, { defaultValue: cleanName });
}

/**
 * Maps budget model enum values to translation keys
 */
export function getBudgetModelTranslationKey(model: string): string {
  const modelMappings: { [key: string]: string } = {
    'RULE_50_30_20': 'budget.models.rule50_30_20',
    'RULE_60_20_20': 'budget.models.rule60_20_20',
    'RULE_80_20': 'budget.models.rule80_20',
    'ENVELOPE': 'budget.models.envelope',
    'ZERO_BASED': 'budget.models.zero_based',
    'FRENCH_THIRDS': 'budget.models.frenchThirds',
    'RULE_PERSONAL_PROJECTS': 'budget.models.rulePersonalProjects',
    'CUSTOM': 'budget.models.custom'
  };
  
  return modelMappings[model] || `budget.models.${model.toLowerCase()}`;
}

/**
 * Maps budget model enum values to description translation keys
 */
export function getBudgetModelDescriptionKey(model: string): string {
  const descMappings: { [key: string]: string } = {
    'RULE_50_30_20': 'budget.models.rule50_30_20_desc',
    'RULE_60_20_20': 'budget.models.rule60_20_20_desc',
    'RULE_80_20': 'budget.models.rule80_20_desc',
    'ENVELOPE': 'budget.models.envelope_desc',
    'ZERO_BASED': 'budget.models.zero_based_desc',
    'FRENCH_THIRDS': 'budget.models.frenchThirds_desc',
    'RULE_PERSONAL_PROJECTS': 'budget.models.rulePersonalProjects_desc',
    'CUSTOM': 'budget.models.custom_desc'
  };
  
  return descMappings[model] || `budget.models.${model.toLowerCase()}_desc`;
}

/**
 * Gets the color for a budget category
 */
export function getBudgetCategoryColor(categoryName: string): string {
  const colorMappings: { [key: string]: string } = {
    'budgetCategories.needs': '#ef4444', // Red
    'budgetCategories.wants': '#10b981', // Green
    'budgetCategories.savings': '#3b82f6', // Blue
    'budgetCategories.personalProjects': '#9333ea', // Purple
    'budgetCategories.expenses': '#f59e0b', // Amber
    'budgetCategories.housing': '#ef4444', // Red
    'budgetCategories.living': '#10b981', // Green
    // Legacy support
    'Besoins': '#ef4444',
    'Ce que je veux': '#10b981', 
    'Épargne': '#3b82f6',
    'Projets Perso': '#9333ea'
  };
  
  return colorMappings[categoryName] || '#6366f1'; // Default indigo
}

/**
 * Gets the icon for a budget category
 */
export function getBudgetCategoryIcon(categoryName: string): string {
  const iconMappings: { [key: string]: string } = {
    'budgetCategories.needs': '🏠',
    'budgetCategories.wants': '🎯',
    'budgetCategories.savings': '💰',
    'budgetCategories.personalProjects': '🚀',
    'budgetCategories.expenses': '💸',
    'budgetCategories.housing': '🏠',
    'budgetCategories.living': '🎯',
    // Legacy support
    'Besoins': '🏠',
    'Ce que je veux': '🎯',
    'Épargne': '💰',
    'Projets Perso': '🚀'
  };
  
  return iconMappings[categoryName] || '📊';
}