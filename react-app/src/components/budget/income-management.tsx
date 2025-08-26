import { useState, useEffect } from 'react'
import { apiClient } from '@/lib/api'
import IncomeCategoryModal from './income-category-modal'
import HistoricalIncomeTransactionModal from './historical-income-transaction-modal'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Plus, Edit3, Trash2, PieChart, DollarSign, Briefcase, Building, TrendingUp, Receipt } from 'lucide-react'
import { IncomeCategory, IncomeStatistics, IncomeType } from '@/types/api'

interface IncomeManagementProps {
  budgetId: number
  onIncomeChange?: () => void
}

export default function IncomeManagement({ budgetId, onIncomeChange }: IncomeManagementProps) {
  const [incomeCategories, setIncomeCategories] = useState<IncomeCategory[]>([])
  const [incomeStats, setIncomeStats] = useState<IncomeStatistics | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showAddCategory, setShowAddCategory] = useState(false)
  const [editingCategory, setEditingCategory] = useState<IncomeCategory | null>(null)
  const [showLinkTransactions, setShowLinkTransactions] = useState(false)
  const [selectedCategoryForLinking, setSelectedCategoryForLinking] = useState<IncomeCategory | null>(null)

  // Load income data
  const loadIncomeData = async () => {
    try {
      setIsLoading(true)
      
      const [categories, statistics] = await Promise.all([
        apiClient.getIncomeCategories(budgetId),
        apiClient.getIncomeStatistics(budgetId)
      ])
      
      setIncomeCategories(categories)
      setIncomeStats(statistics)
      setError(null)
    } catch (error: any) {
      console.error('Error loading income data:', error)
      setError('Failed to load income data')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadIncomeData()
  }, [budgetId])

  // Helper functions
  const getIncomeTypeIcon = (type: IncomeType) => {
    switch (type) {
      case 'SALARY':
        return <Briefcase className="w-4 h-4" />
      case 'BUSINESS':
        return <Building className="w-4 h-4" />
      case 'INVESTMENT':
        return <TrendingUp className="w-4 h-4" />
      default:
        return <DollarSign className="w-4 h-4" />
    }
  }

  const getIncomeTypeLabel = (type: IncomeType) => {
    switch (type) {
      case 'SALARY':
        return 'Salaire'
      case 'BUSINESS':
        return 'Entreprise'
      case 'INVESTMENT':
        return 'Investissement'
      default:
        return 'Autre'
    }
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount)
  }

  const handleDeleteCategory = async (categoryId: number) => {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette catégorie de revenus ?')) {
      return
    }

    try {
      await apiClient.deleteIncomeCategory(categoryId)
      await loadIncomeData()
      onIncomeChange?.()
    } catch (error: any) {
      console.error('Error deleting income category:', error)
      setError('Failed to delete income category')
    }
  }

  const handleCategoryUpdate = async () => {
    await loadIncomeData()
    onIncomeChange?.()
    setEditingCategory(null)
    setShowAddCategory(false)
  }

  const handleTransactionUpdate = async () => {
    await loadIncomeData()
    onIncomeChange?.()
    setShowLinkTransactions(false)
    setSelectedCategoryForLinking(null)
  }

  const handleLinkTransactions = (category: IncomeCategory) => {
    setSelectedCategoryForLinking(category)
    setShowLinkTransactions(true)
  }

  if (isLoading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-center">Chargement des revenus...</div>
        </CardContent>
      </Card>
    )
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-center text-red-600">{error}</div>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header with statistics */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center space-x-2">
              <PieChart className="w-5 h-5" />
              <span>Gestion des Revenus</span>
            </CardTitle>
            <Button onClick={() => setShowAddCategory(true)} size="sm">
              <Plus className="w-4 h-4 mr-2" />
              Ajouter un revenu
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {incomeStats && (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-blue-50 p-3 rounded-lg">
                <div className="text-sm text-blue-600 font-medium">Budgété</div>
                <div className="text-lg font-semibold text-blue-900">
                  {formatCurrency(incomeStats.totalBudgeted)}
                </div>
              </div>
              <div className="bg-green-50 p-3 rounded-lg">
                <div className="text-sm text-green-600 font-medium">Réel</div>
                <div className="text-lg font-semibold text-green-900">
                  {formatCurrency(incomeStats.totalActual)}
                </div>
              </div>
              <div className={`p-3 rounded-lg ${
                incomeStats.variance >= 0 ? 'bg-green-50' : 'bg-red-50'
              }`}>
                <div className={`text-sm font-medium ${
                  incomeStats.variance >= 0 ? 'text-green-600' : 'text-red-600'
                }`}>
                  Écart
                </div>
                <div className={`text-lg font-semibold ${
                  incomeStats.variance >= 0 ? 'text-green-900' : 'text-red-900'
                }`}>
                  {incomeStats.variance >= 0 ? '+' : ''}{formatCurrency(incomeStats.variance)}
                </div>
              </div>
              <div className={`p-3 rounded-lg ${
                incomeStats.variancePercentage >= 0 ? 'bg-green-50' : 'bg-red-50'
              }`}>
                <div className={`text-sm font-medium ${
                  incomeStats.variancePercentage >= 0 ? 'text-green-600' : 'text-red-600'
                }`}>
                  % Écart
                </div>
                <div className={`text-lg font-semibold ${
                  incomeStats.variancePercentage >= 0 ? 'text-green-900' : 'text-red-900'
                }`}>
                  {incomeStats.variancePercentage >= 0 ? '+' : ''}{incomeStats.variancePercentage.toFixed(1)}%
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Income categories list */}
      <div className="space-y-4">
        {incomeCategories.length === 0 ? (
          <Card>
            <CardContent className="p-8 text-center">
              <PieChart className="w-12 h-12 mx-auto text-gray-400 mb-4" />
              <h3 className="text-lg font-semibold text-gray-700 mb-2">
                Aucune catégorie de revenus
              </h3>
              <p className="text-gray-500 mb-4">
                Commencez par ajouter vos sources de revenus pour mieux gérer votre budget.
              </p>
              <Button onClick={() => setShowAddCategory(true)}>
                <Plus className="w-4 h-4 mr-2" />
                Ajouter votre premier revenu
              </Button>
            </CardContent>
          </Card>
        ) : (
          incomeCategories.map((category) => (
            <Card key={category.id} className="hover:shadow-md transition-shadow">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-4 flex-1">
                    {/* Icon and type */}
                    <div 
                      className="w-10 h-10 rounded-full flex items-center justify-center"
                      style={{ backgroundColor: category.color + '20', color: category.color }}
                    >
                      {getIncomeTypeIcon(category.incomeType)}
                    </div>
                    
                    {/* Category info */}
                    <div className="flex-1">
                      <div className="flex items-center space-x-2">
                        <h4 className="font-semibold">{category.name}</h4>
                        <span className="text-xs px-2 py-1 bg-gray-100 rounded-full">
                          {getIncomeTypeLabel(category.incomeType)}
                        </span>
                        {category.isDefault && (
                          <span className="text-xs px-2 py-1 bg-blue-100 text-blue-700 rounded-full">
                            Par défaut
                          </span>
                        )}
                      </div>
                      {category.description && (
                        <p className="text-sm text-gray-600 mt-1">{category.description}</p>
                      )}
                    </div>
                    
                    {/* Amounts */}
                    <div className="text-right">
                      <div className="font-semibold">
                        {formatCurrency(category.actualAmount)} / {formatCurrency(category.budgetedAmount)}
                      </div>
                      <div className={`text-sm ${
                        category.variance >= 0 ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {category.variance >= 0 ? '+' : ''}{formatCurrency(category.variance)}
                        {' '}({category.usagePercentage.toFixed(1)}%)
                      </div>
                    </div>
                  </div>
                  
                  {/* Actions */}
                  <div className="flex items-center space-x-1 ml-4">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleLinkTransactions(category)}
                      title="Lier des transactions"
                      className="text-green-600 hover:text-green-700 hover:bg-green-50"
                    >
                      <Receipt className="w-4 h-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setEditingCategory(category)}
                      title="Modifier la catégorie"
                    >
                      <Edit3 className="w-4 h-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDeleteCategory(category.id)}
                      title="Supprimer la catégorie"
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
                
                {/* Progress bar */}
                <div className="mt-3">
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div 
                      className="h-2 rounded-full transition-all"
                      style={{ 
                        width: `${Math.min(100, Math.max(0, category.usagePercentage))}%`,
                        backgroundColor: category.color 
                      }}
                    />
                  </div>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* Add/Edit Category Modal */}
      {(showAddCategory || editingCategory) && (
        <IncomeCategoryModal
          budgetId={budgetId}
          category={editingCategory}
          isOpen={showAddCategory || !!editingCategory}
          onClose={() => {
            setShowAddCategory(false)
            setEditingCategory(null)
          }}
          onSuccess={handleCategoryUpdate}
        />
      )}

      {/* Link Transactions Modal */}
      {showLinkTransactions && selectedCategoryForLinking && (
        <HistoricalIncomeTransactionModal
          incomeCategory={selectedCategoryForLinking}
          isOpen={showLinkTransactions}
          onClose={() => {
            setShowLinkTransactions(false)
            setSelectedCategoryForLinking(null)
          }}
          onSuccess={handleTransactionUpdate}
        />
      )}
    </div>
  )
}