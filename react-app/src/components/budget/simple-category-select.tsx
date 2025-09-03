import { useState } from "react";
import { useTranslation } from 'react-i18next';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/advanced-select'
import { translateDetailedCategoryName } from '@/lib/budget-utils'
import { useCategories } from '@/contexts/categories-context'
import type { Category } from '@/types/api'

interface SimpleCategorySelectProps {
  onSelectionChange: (detailedCategoryId: number) => void
  placeholder?: string
  className?: string
  disabled?: boolean
}

export function SimpleCategorySelect({
  onSelectionChange,
  placeholder = "Sélectionner une catégorie",
  className = "",
  disabled = false
}: SimpleCategorySelectProps) {
  const { t } = useTranslation();
  const { detailedCategories, loading, error } = useCategories()
  const [selectedCategory, setSelectedCategory] = useState<string>("")

  const getTranslatedCategoryName = (categoryName: string) => translateDetailedCategoryName(categoryName, t)

  const handleCategoryChange = (categoryId: string) => {
    setSelectedCategory(categoryId)
    onSelectionChange(parseInt(categoryId))
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
    <div className={`${className}`}>
      <div className={disabled ? "opacity-50 pointer-events-none" : ""}>
        <Select
          value={selectedCategory}
          onValueChange={handleCategoryChange}
        >
          <SelectTrigger>
            <SelectValue placeholder={
              detailedCategories.length === 0 
                ? "Aucune catégorie disponible"
                : placeholder
            } />
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
  )
}