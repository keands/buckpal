import { useState } from "react";
import { useTranslation } from 'react-i18next';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/advanced-select'
import { Badge } from '@/components/ui/badge'
import { translateBudgetCategoryName, translateDetailedCategoryName } from '@/lib/budget-utils'
import { useCategories } from '@/contexts/categories-context'
import type { BudgetCategory } from '@/types/api'

// Interfaces moved to categories-context.tsx

interface TwoLevelCategorySelectProps {
  budgetCategories: BudgetCategory[]
  onSelectionChange: (budgetCategoryId: number, detailedCategoryId: number) => void
  placeholder?: string
  className?: string
  disabled?: boolean
}


export function TwoLevelCategorySelect({
  budgetCategories,
  onSelectionChange,
  placeholder = "Sélectionner une catégorie",
  className = "",
  disabled = false
}: TwoLevelCategorySelectProps) {
  const { t } = useTranslation();
  const { detailedCategories, categoryMapping, loading, error } = useCategories()
  const [selectedBudgetCategory, setSelectedBudgetCategory] = useState<string>("")
  const [selectedDetailed, setSelectedDetailed] = useState<string>("")

  // Use utility functions for translation
  const getTranslatedCategoryName = (categoryName: string) => translateDetailedCategoryName(categoryName, t)
  const getTranslatedBudgetCategoryName = (budgetCategoryName: string) => translateBudgetCategoryName(budgetCategoryName, t)

  // Categories are now loaded via context - no local loading needed!

  const handleBudgetCategoryChange = (budgetCategoryId: string) => {
    setSelectedBudgetCategory(budgetCategoryId)
    setSelectedDetailed("") // Reset detailed selection
  }

  const handleDetailedCategoryChange = (detailedCategoryId: string) => {
    setSelectedDetailed(detailedCategoryId)
    
    // Call callback with both IDs
    if (selectedBudgetCategory && detailedCategoryId) {
      onSelectionChange(parseInt(selectedBudgetCategory), parseInt(detailedCategoryId))
    }
  }

  const getDisplayValue = () => {
    if (!selectedBudgetCategory || !selectedDetailed) {
      return placeholder
    }

    const budgetCategory = budgetCategories.find(bc => bc.id.toString() === selectedBudgetCategory)
    const detailedCategory = detailedCategories.find(dc => dc.id.toString() === selectedDetailed)
    
    const budgetCategoryName = budgetCategory ? getTranslatedBudgetCategoryName(budgetCategory.name) : ''
    const detailedCategoryName = detailedCategory ? getTranslatedCategoryName(detailedCategory.name) : ''
    
    return `${budgetCategoryName} → ${detailedCategoryName}`
  }

  if (loading) {
    return (
      <div className={`w-64 ${className}`}>
        <div className="opacity-50">
          <Select>
            <SelectTrigger>
              <SelectValue placeholder="Chargement des catégories..." />
            </SelectTrigger>
          </Select>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className={`w-64 ${className}`}>
        <div className="text-red-500 text-sm p-2 border border-red-200 rounded">
          Erreur: {error}
        </div>
      </div>
    )
  }

  return (
    <div className={`space-y-2 ${className}`}>
      {/* Step 1: Budget Category Selection */}
      <div>
        <label className="text-sm font-medium text-gray-700 mb-1 block">
          1. Catégorie budgétaire
        </label>
        <div className={disabled ? "opacity-50 pointer-events-none" : ""}>
          <Select
            value={selectedBudgetCategory}
            onValueChange={handleBudgetCategoryChange}
          >
          <SelectTrigger>
            <SelectValue placeholder="Besoins, Loisirs, Épargne..." />
          </SelectTrigger>
          <SelectContent>
            {budgetCategories.map((category) => (
              <SelectItem key={category.id} value={category.id.toString()}>
                <div className="flex items-center gap-2">
                  <div 
                    className="w-3 h-3 rounded-full" 
                    style={{ backgroundColor: category.colorCode || '#6366f1' }}
                  />
                  <span>{getTranslatedBudgetCategoryName(category.name)}</span>
                  <Badge variant="outline" className="ml-auto text-xs">
                    {category.spentAmount ? `€${category.spentAmount.toFixed(0)}` : '€0'}
                  </Badge>
                </div>
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        </div>
      </div>

      {/* Step 2: Detailed Category Selection - Always show all categories */}
      <div>
        <label className="text-sm font-medium text-gray-700 mb-1 block">
          2. Catégorie détaillée
        </label>
        <div className={(disabled || detailedCategories.length === 0) ? "opacity-50 pointer-events-none" : ""}>
            <Select
              value={selectedDetailed}
              onValueChange={handleDetailedCategoryChange}
            >
            <SelectTrigger>
              <SelectValue 
                placeholder={
                  detailedCategories.length === 0 
                    ? "Aucune catégorie disponible"
                    : "Toutes les catégories disponibles..."
                } 
              />
            </SelectTrigger>
            <SelectContent>
              {detailedCategories.map((category) => (
                <SelectItem key={category.id} value={category.id.toString()}>
                  <div className="flex items-center gap-2">
                    {category.iconName && (
                      <span className="text-sm">{category.iconName}</span>
                    )}
                    <span>{getTranslatedCategoryName(category.name)}</span>
                  </div>
                </SelectItem>
              ))}
          </SelectContent>
          </Select>
          </div>
        </div>

      {/* Selected Summary */}
      {selectedBudgetCategory && selectedDetailed && (
        <div className="p-2 bg-green-50 border border-green-200 rounded text-sm">
          <span className="font-medium text-green-800">Sélection: </span>
          <span className="text-green-700">{getDisplayValue()}</span>
        </div>
      )}

      {/* Category Management Link */}
      {selectedBudgetCategory && (
        <div className="text-center">
          <button
            type="button"
            className="text-xs text-blue-600 hover:text-blue-700 underline"
            onClick={() => {
              // TODO: Open category management modal or navigate to page
              console.log('Open category management');
            }}
          >
            + Créer une nouvelle catégorie personnalisée
          </button>
        </div>
      )}
    </div>
  )
}