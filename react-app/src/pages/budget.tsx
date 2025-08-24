import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/contexts/auth-context'
import { apiClient } from '@/lib/api'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { PieChart, Plus, Settings } from 'lucide-react'
import BudgetSetupWizard from '@/components/budget/budget-setup-wizard'
import BudgetProgressDashboard from '@/components/budget/budget-progress-dashboard'
import CategoryTransactionsModal from '@/components/budget/category-transactions-modal'

export default function BudgetPage() {
  const { t } = useTranslation()
  const { isAuthenticated } = useAuth()
  const [showWizard, setShowWizard] = useState(false)
  const [showDashboard, setShowDashboard] = useState(true)
  const [selectedBudgetId, setSelectedBudgetId] = useState<number | null>(null)
  const [selectedCategory, setSelectedCategory] = useState<any>(null)
  const [showCategoryModal, setShowCategoryModal] = useState(false)
  const [budgets, setBudgets] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [previousMonthIncome, setPreviousMonthIncome] = useState<number>(0)

  // Helper functions
  const getMonthName = (monthNumber: number) => {
    const months = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ]
    return months[monthNumber - 1] || 'Unknown'
  }

  // Transform backend budget data to frontend format
  const transformBudgetData = (backendBudgets: any[]) => {
    return backendBudgets.map(budget => ({
      ...budget,
      // Add computed name for display
      name: `${getMonthName(budget.budgetMonth)} ${budget.budgetYear} Budget`,
      // Map budgetCategories to categories for compatibility
      categories: budget.budgetCategories || [],
      // Ensure numeric values are numbers, not strings
      projectedIncome: Number(budget.projectedIncome),
      actualIncome: Number(budget.actualIncome),
      totalAllocatedAmount: Number(budget.totalAllocatedAmount),
      totalSpentAmount: Number(budget.totalSpentAmount),
      usagePercentage: budget.usagePercentage ? Number(budget.usagePercentage) : 0,
      remainingAmount: budget.remainingAmount ? Number(budget.remainingAmount) : 0,
      isOverBudget: Boolean(budget.isOverBudget)
    }))
  }

  // Load budgets from API
  useEffect(() => {
    const loadBudgets = async () => {
      if (!isAuthenticated) return
      
      try {
        setIsLoading(true)
        
        // Fetch budgets first
        const budgetData = await apiClient.getBudgets()
        const transformedBudgets = transformBudgetData(budgetData)
        setBudgets(transformedBudgets)
        
        // Try to get previous month income, but don't fail if none exists
        try {
          const previousBudget = await apiClient.getPreviousMonthBudget()
          if (previousBudget) {
            setPreviousMonthIncome(Number(previousBudget.actualIncome) || Number(previousBudget.projectedIncome) || 0)
          }
        } catch (error: any) {
          // No previous budget exists yet - this is normal for first-time users
          console.log('No previous month budget found (normal for new users):', error.response?.status)
          setPreviousMonthIncome(0)
        }
        
        // Set first budget as selected if none selected
        if (transformedBudgets.length > 0 && selectedBudgetId === null) {
          setSelectedBudgetId(transformedBudgets[0].id)
        }
        setError(null)
      } catch (error: any) {
        console.error('Error loading budgets:', error)
        // Show error for debugging - API might not be implemented yet
        setError(`Failed to load budgets: ${error.response?.status || 'Network error'}`)
        setBudgets([])
      } finally {
        setIsLoading(false)
      }
    }

    loadBudgets()
  }, [isAuthenticated])



  // Use only real budgets from API
  const availableBudgets = budgets
  const selectedBudget = availableBudgets.find(b => b.id === selectedBudgetId) || availableBudgets[0]
  
  // Set default selection if needed
  if (selectedBudgetId === null && availableBudgets.length > 0) {
    setSelectedBudgetId(availableBudgets[0].id)
  }

  // Render modal first (before any early returns)
  const wizardModal = showWizard && (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-auto">
        <BudgetSetupWizard
          onComplete={async (budgetData) => {
            try {
              // Call the backend API to create the budget
              const createdBudget = await apiClient.createBudgetFromWizard(budgetData)
              
              // Refresh the budgets list
              const allBudgets = await apiClient.getBudgets()
              const transformedBudgets = transformBudgetData(allBudgets)
              setBudgets(transformedBudgets)
              
              // Select the newly created budget
              setSelectedBudgetId(createdBudget.id)
              
              setShowWizard(false)
              setError(null)
            } catch (error: any) {
              console.error('Error creating budget:', error)
              setError(`Failed to create budget: ${error.response?.data?.message || error.message || 'Unknown error'}`)
            }
          }}
          onCancel={() => setShowWizard(false)}
          previousMonthIncome={previousMonthIncome}
        />
      </div>
    </div>
  )
  
  // Show loading state
  if (isLoading) {
    return (
      <>
        <div className="flex items-center justify-center min-h-64">
          <div className="text-lg">{t('common.loading')}</div>
        </div>
        {wizardModal}
      </>
    )
  }
  
  // Show error state
  if (error) {
    return (
      <>
        <div className="space-y-6">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <p className="text-red-800">{error}</p>
          </div>
          <div className="text-center">
            <Button onClick={() => window.location.reload()}>
              Retry
            </Button>
          </div>
        </div>
        {wizardModal}
      </>
    )
  }
  
  // Show empty state if no budgets
  if (availableBudgets.length === 0) {
    return (
      <>
        <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{t('budget.title')}</h1>
            <p className="text-gray-600">{t('budget.subtitle')}</p>
          </div>
        </div>
        
        <Card className="border-blue-200 bg-blue-50">
          <CardContent className="p-8 text-center">
            <PieChart className="w-16 h-16 mx-auto text-blue-600 mb-4" />
            <h3 className="text-lg font-semibold text-blue-900 mb-2">
              No budgets found
            </h3>
            <p className="text-blue-700 mb-4">
              Create your first budget to start tracking your expenses
            </p>
            <Button onClick={() => setShowWizard(true)} className="bg-blue-600 hover:bg-blue-700">
              <Plus className="w-4 h-4 mr-2" />
              {t('budget.createBudget')}
            </Button>
          </CardContent>
        </Card>
        </div>
        {wizardModal}
      </>
    )
  }

  

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{t('budget.title')}</h1>
          <p className="text-gray-600">{t('budget.subtitle')}</p>
        </div>
        
        <div className="flex space-x-2">
          {availableBudgets.length > 0 && (
            <>
              <Button 
                variant="outline"
                onClick={() => setShowDashboard(!showDashboard)}
              >
                <PieChart className="w-4 h-4 mr-2" />
                {showDashboard ? 'Hide Dashboard' : 'Show Dashboard'}
              </Button>
              <Button variant="outline">
                <Settings className="w-4 h-4 mr-2" />
                {t('budget.budgetModels')}
              </Button>
              <Button onClick={() => setShowWizard(true)}>
                <Plus className="w-4 h-4 mr-2" />
                {t('budget.createBudget')}
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Budget Progress Dashboard */}
      {showDashboard && (
        <BudgetProgressDashboard 
          budget={selectedBudget}
          availableBudgets={availableBudgets}
          onCategoryClick={(category) => {
            setSelectedCategory(category)
            setShowCategoryModal(true)
          }}
          onBudgetChange={(budgetId) => {
            setSelectedBudgetId(budgetId)
          }}
        />
      )}

      {/* Category Transactions Modal */}
      {showCategoryModal && selectedCategory && (
        <CategoryTransactionsModal
          category={selectedCategory}
          transactions={[]} // TODO: Fetch real transactions from API
          isOpen={showCategoryModal}
          onClose={() => {
            setShowCategoryModal(false)
            setSelectedCategory(null)
          }}
          onEditTransaction={(transaction) => {
            console.log('Edit transaction:', transaction)
            // TODO: Open transaction edit modal
          }}
          onAddTransaction={() => {
            console.log('Add new transaction to category:', selectedCategory?.name)
            // TODO: Open add transaction modal
          }}
          onEditCategoryBudget={(category) => {
            console.log('Edit category budget:', category.name)
            // TODO: Open budget edit modal
          }}
        />
      )}

      {wizardModal}
    </div>
  )
}