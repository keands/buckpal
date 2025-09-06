import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { apiClient } from '@/lib/api'

interface DetailedCategory {
  id: number
  name: string
  description?: string
  colorCode?: string
  iconName?: string
  isDefault?: boolean
}

interface CategoryMapping {
  [detailedCategoryName: string]: string // Maps to budget category name
}

interface CategoriesContextType {
  detailedCategories: DetailedCategory[]
  categoryMapping: CategoryMapping
  loading: boolean
  error: string | null
  refetchCategories: () => Promise<void>
}

const CategoriesContext = createContext<CategoriesContextType | undefined>(undefined)

interface CategoriesProviderProps {
  children: ReactNode
}

export function CategoriesProvider({ children }: CategoriesProviderProps) {
  const [detailedCategories, setDetailedCategories] = useState<DetailedCategory[]>([])
  const [categoryMapping, setCategoryMapping] = useState<CategoryMapping>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadCategoryData = async () => {
    try {
      setLoading(true)
      setError(null)
      
      // Utiliser le nouveau système de mapping des catégories
      const groupedCategories = await apiClient.getCategoriesGroupedByBudgetCategory()
      
      // Aplatir toutes les catégories depuis les groupes
      const allCategories: DetailedCategory[] = []
      const mapping: CategoryMapping = {}
      
      for (const [budgetKey, categories] of Object.entries(groupedCategories)) {
        categories.forEach(category => {
          allCategories.push(category)
          // Créer le mapping inverse pour compatibilité
          mapping[category.name] = budgetKey
        })
      }
      
      setDetailedCategories(allCategories)
      setCategoryMapping(mapping)

    } catch (err) {
      console.error('Failed to load category data:', err)
      setError(err instanceof Error ? err.message : 'Failed to load categories')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCategoryData()
  }, [])

  const refetchCategories = async () => {
    await loadCategoryData()
  }

  const value: CategoriesContextType = {
    detailedCategories,
    categoryMapping,
    loading,
    error,
    refetchCategories
  }

  return (
    <CategoriesContext.Provider value={value}>
      {children}
    </CategoriesContext.Provider>
  )
}

export function useCategories(): CategoriesContextType {
  const context = useContext(CategoriesContext)
  if (context === undefined) {
    throw new Error('useCategories must be used within a CategoriesProvider')
  }
  return context
}

// Hook for components that only need the categories list
export function useDetailedCategories(): DetailedCategory[] {
  const { detailedCategories } = useCategories()
  return detailedCategories
}

// Hook for components that only need the mapping
export function useCategoryMapping(): CategoryMapping {
  const { categoryMapping } = useCategories()
  return categoryMapping
}