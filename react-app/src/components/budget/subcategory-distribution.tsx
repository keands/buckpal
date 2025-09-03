import { useState, useEffect } from "react";
import { apiClient } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { PieChart, BarChart3, AlertTriangle } from 'lucide-react'
import type { BudgetCategory } from '@/types/api'

interface DetailedCategoryDistribution {
  categoryId: number
  categoryName: string
  totalSpent: number
  detailedDistribution: Record<string, {
    amount: number
    percentage: number
    transactionCount: number
    colorCode?: string
    iconName?: string
  }>
  uncategorizedCount: number
  categorizedPercentage: string
}

interface SubcategoryDistributionProps {
  budgetId: number
  budgetCategory: BudgetCategory
  className?: string
}

export function SubcategoryDistribution({
  budgetId,
  budgetCategory,
  className = ""
}: SubcategoryDistributionProps) {
  const [distribution, setDistribution] = useState<DetailedCategoryDistribution | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const loadDistribution = async () => {
      try {
        setLoading(true)
        setError(null)
        const data = await apiClient.getBudgetCategoryDetailedDistribution(budgetId, budgetCategory.id)
        setDistribution(data)
      } catch (err) {
        console.error('Failed to load subcategory distribution:', err)
        setError('Erreur lors du chargement des sous-catégories')
      } finally {
        setLoading(false)
      }
    }

    loadDistribution()
  }, [budgetId, budgetCategory.id])

  if (loading) {
    return (
      <Card className={className}>
        <CardContent className="p-4">
          <div className="flex items-center gap-2">
            <div className="animate-pulse h-4 bg-gray-300 rounded w-full"></div>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (error) {
    return (
      <Card className={`border-red-200 bg-red-50 ${className}`}>
        <CardContent className="p-4">
          <div className="flex items-center gap-2 text-red-600">
            <AlertTriangle className="w-4 h-4" />
            <span className="text-sm">{error}</span>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (!distribution || Object.keys(distribution.detailedDistribution).length === 0) {
    return (
      <Card className={`border-gray-200 bg-gray-50 ${className}`}>
        <CardContent className="p-4">
          <div className="text-center text-gray-600">
            <BarChart3 className="w-8 h-8 mx-auto mb-2 text-gray-400" />
            <p className="text-sm">Aucune transaction catégorisée</p>
            {distribution && distribution.uncategorizedCount > 0 && (
              <p className="text-xs text-gray-500 mt-1">
                {distribution.uncategorizedCount} transaction(s) non catégorisée(s)
              </p>
            )}
          </div>
        </CardContent>
      </Card>
    )
  }

  const subcategories = Object.entries(distribution.detailedDistribution)
  const hasMultipleCategories = subcategories.length > 1

  // Sort by amount descending
  const sortedSubcategories = subcategories.sort(([, a], [, b]) => b.amount - a.amount)

  return (
    <Card className={className}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base flex items-center gap-2">
            <div 
              className="w-4 h-4 rounded-full" 
              style={{ backgroundColor: budgetCategory.colorCode || '#6366f1' }}
            />
            {budgetCategory.name}
          </CardTitle>
          <Badge variant="outline" className="text-xs">
            {parseFloat(distribution.categorizedPercentage).toFixed(0)}% catégorisé
          </Badge>
        </div>
        <CardDescription className="text-sm">
          €{budgetCategory.spentAmount?.toFixed(2) || '0.00'} / €{budgetCategory.allocatedAmount?.toFixed(2) || '0.00'}
        </CardDescription>
      </CardHeader>

      <CardContent className="pt-0">
        {/* Progress bar for budget usage */}
        <div className="mb-4">
          <Progress 
            value={budgetCategory.usagePercentage || 0} 
            className="h-2"
            // Use red color if over budget
            color={budgetCategory.isOverBudget ? "bg-red-500" : "bg-blue-500"}
          />
          <div className="flex justify-between text-xs text-gray-500 mt-1">
            <span>{budgetCategory.usagePercentage?.toFixed(0) || 0}% utilisé</span>
            <span>Reste: €{budgetCategory.remainingAmount?.toFixed(2) || '0.00'}</span>
          </div>
        </div>

        {/* Subcategory breakdown */}
        {hasMultipleCategories && (
          <div className="space-y-3">
            <h4 className="text-sm font-medium text-gray-700 flex items-center gap-2">
              <PieChart className="w-4 h-4" />
              Répartition par sous-catégorie
            </h4>
            
            <div className="space-y-2">
              {sortedSubcategories.map(([categoryName, info]) => (
                <div key={categoryName} className="flex items-center justify-between">
                  <div className="flex items-center gap-2 flex-1">
                    {info.iconName && (
                      <span className="text-sm">{info.iconName}</span>
                    )}
                    <div 
                      className="w-3 h-3 rounded-full flex-shrink-0" 
                      style={{ backgroundColor: info.colorCode || '#94a3b8' }}
                    />
                    <span className="text-sm font-medium truncate">{categoryName}</span>
                  </div>
                  
                  <div className="flex items-center gap-2 ml-2">
                    <Badge 
                      variant="secondary" 
                      className="text-xs px-2 py-0.5"
                    >
                      {info.percentage.toFixed(0)}%
                    </Badge>
                    <span className="text-sm font-semibold text-gray-900 w-16 text-right">
                      €{info.amount.toFixed(0)}
                    </span>
                  </div>
                </div>
              ))}
            </div>

            {/* Visual progress bar for subcategories */}
            <div className="w-full bg-gray-200 rounded-full h-2 mt-3">
              <div className="flex h-full rounded-full overflow-hidden">
                {sortedSubcategories.map(([categoryName, info], index) => (
                  <div
                    key={categoryName}
                    className="h-full transition-all duration-300 hover:opacity-80"
                    style={{
                      backgroundColor: info.colorCode || ['#3b82f6', '#ef4444', '#22c55e', '#f59e0b', '#8b5cf6'][index % 5],
                      width: `${info.percentage}%`
                    }}
                    title={`${categoryName}: €${info.amount.toFixed(2)} (${info.percentage.toFixed(1)}%)`}
                  />
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Single category display */}
        {!hasMultipleCategories && sortedSubcategories.length === 1 && (
          <div className="text-center py-2">
            <div className="flex items-center justify-center gap-2 text-sm text-gray-600">
              <div 
                className="w-3 h-3 rounded-full" 
                style={{ backgroundColor: sortedSubcategories[0][1].colorCode || '#6366f1' }}
              />
              <span>Tout dans: <strong>{sortedSubcategories[0][0]}</strong></span>
            </div>
          </div>
        )}

        {/* Uncategorized warning */}
        {distribution.uncategorizedCount > 0 && (
          <div className="mt-3 p-2 bg-orange-50 border border-orange-200 rounded text-sm">
            <div className="flex items-center gap-2 text-orange-700">
              <AlertTriangle className="w-4 h-4" />
              <span>
                {distribution.uncategorizedCount} transaction(s) à catégoriser
              </span>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}