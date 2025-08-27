import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/contexts/auth-context'
import { apiClient } from '@/lib/api'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { PieChart, Plus } from 'lucide-react'
import BudgetSetupWizard from '@/components/budget/budget-setup-wizard'
import BudgetProgressDashboard from '@/components/budget/budget-progress-dashboard'
import CategoryTransactionsModal from '@/components/budget/category-transactions-modal'
import { TransactionAssignment } from '@/components/budget/transaction-assignment'

export default function BudgetPage() {
  const { t } = useTranslation()
  const { isAuthenticated } = useAuth()
  const [showWizard, setShowWizard] = useState(false)
  const [showDashboard, setShowDashboard] = useState(true)
  const [selectedBudgetId, setSelectedBudgetId] = useState<number | null>(null)
  const [selectedCategory, setSelectedCategory] = useState<any>(null)
  const [showCategoryModal, setShowCategoryModal] = useState(false)
  const [categoryTransactions, setCategoryTransactions] = useState<any[]>([])
  const [loadingCategoryTransactions, setLoadingCategoryTransactions] = useState(false)
  const [budgets, setBudgets] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [previousMonthIncome, setPreviousMonthIncome] = useState<number>(0)
  const [, setCurrentMonthBudget] = useState<any>(null)
  const [showCurrentMonthPrompt, setShowCurrentMonthPrompt] = useState(false)

  // Load transactions for a specific category
  const loadCategoryTransactions = async (category: any) => {
    if (!selectedBudgetId) return
    
    try {
      setLoadingCategoryTransactions(true)
      const transactions = await apiClient.getTransactionsByCategory(selectedBudgetId, category.id)
      setCategoryTransactions(transactions)
    } catch (error: any) {
      console.error('Error loading category transactions:', error)
      setCategoryTransactions([])
    } finally {
      setLoadingCategoryTransactions(false)
    }
  }

  // Helper functions
  const getMonthName = (monthNumber: number) => {
    const months = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ]
    return months[monthNumber - 1] || 'Unknown'
  }

  const getCurrentMonthInfo = () => {
    const now = new Date()
    return {
      month: now.getMonth() + 1, // JavaScript months are 0-based
      year: now.getFullYear(),
      monthName: getMonthName(now.getMonth() + 1)
    }
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
  const loadBudgets = async () => {
      if (!isAuthenticated) return
      
      try {
        setIsLoading(true)
        
        // First, try to get current month budget
        let currentBudget = null
        try {
          currentBudget = await apiClient.getCurrentMonthBudget()
          if (currentBudget) {
            const transformedCurrentBudget = transformBudgetData([currentBudget])[0]
            setCurrentMonthBudget(transformedCurrentBudget)
            setSelectedBudgetId(transformedCurrentBudget.id)
            setShowCurrentMonthPrompt(false)
          }
        } catch (error: any) {
          // Current month budget doesn't exist - show prompt
          console.log('No current month budget found:', error.response?.status)
          setCurrentMonthBudget(null)
          setShowCurrentMonthPrompt(true)
        }
        
        // Fetch all budgets
        const budgetData = await apiClient.getBudgets()
        const transformedBudgets = transformBudgetData(budgetData)
        setBudgets(transformedBudgets)
        
        // If no current month budget, select most recent budget or show all
        if (!currentBudget && transformedBudgets.length > 0 && selectedBudgetId === null) {
          // Sort budgets by year/month descending to get most recent
          const sortedBudgets = transformedBudgets.sort((a, b) => {
            if (a.budgetYear !== b.budgetYear) {
              return b.budgetYear - a.budgetYear
            }
            return b.budgetMonth - a.budgetMonth
          })
          setSelectedBudgetId(sortedBudgets[0].id)
        }
        
        // Try to get previous month income for wizard
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
        
        setError(null)
      } catch (error: any) {
        console.error('Error loading budgets:', error)
        setError(`Failed to load budgets: ${error.response?.status || 'Network error'}`)
        setBudgets([])
      } finally {
        setIsLoading(false)
      }
    }

  // Optimized function to refresh only the current budget without full reload
  const refreshCurrentBudgetOnly = async () => {
    if (!selectedBudgetId || !isAuthenticated) return
    
    try {
      // Fetch only the current budget data
      const updatedBudget = await apiClient.getBudget(selectedBudgetId)
      const transformedBudget = transformBudgetData([updatedBudget])[0]
      
      // Update budgets list with the refreshed budget
      setBudgets(prev => prev.map(budget => 
        budget.id === selectedBudgetId ? transformedBudget : budget
      ))
      
      // Update current month budget if it's the selected one
      const currentInfo = getCurrentMonthInfo()
      if (transformedBudget.budgetMonth === currentInfo.month && 
          transformedBudget.budgetYear === currentInfo.year) {
        setCurrentMonthBudget(transformedBudget)
      }
      
    } catch (error) {
      console.error('Failed to refresh current budget:', error)
      // Fallback to full reload only if targeted refresh fails
      await loadBudgets()
    }
  }

  useEffect(() => {
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
              
              // Refresh all budget data (this handles current month detection)
              await loadBudgets()
              
              // Select the newly created budget
              setSelectedBudgetId(createdBudget.id)
              
              // Hide the prompt if we just created current month budget
              const currentInfo = getCurrentMonthInfo()
              if (createdBudget.budgetMonth === currentInfo.month && createdBudget.budgetYear === currentInfo.year) {
                setShowCurrentMonthPrompt(false)
                setCurrentMonthBudget(transformBudgetData([createdBudget])[0])
              }
              
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
              <Button onClick={() => setShowWizard(true)}>
                <Plus className="w-4 h-4 mr-2" />
                {t('budget.createBudget')}
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Current Month Budget Prompt - Yellow Warning */}
      {showCurrentMonthPrompt && (
        <Card className="border-yellow-300 bg-yellow-50">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-8 h-8 bg-yellow-200 rounded-full flex items-center justify-center">
                  <Plus className="w-4 h-4 text-yellow-800" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-yellow-900">
                    {t('budget.currentMonthPrompt.title', { 
                      month: getCurrentMonthInfo().monthName, 
                      year: getCurrentMonthInfo().year 
                    })}
                  </h3>
                  <p className="text-yellow-700">
                    {t('budget.currentMonthPrompt.description')}
                  </p>
                </div>
              </div>
              <Button 
                onClick={() => {
                  setShowWizard(true)
                }} 
                className="bg-yellow-600 hover:bg-yellow-700 text-white"
              >
                <Plus className="w-4 h-4 mr-2" />
                {t('budget.currentMonthPrompt.createButton', { month: getCurrentMonthInfo().monthName })}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Budget Progress Dashboard */}
      {showDashboard && (
        <BudgetProgressDashboard 
          budget={selectedBudget}
          availableBudgets={availableBudgets}
          onCategoryClick={async (category) => {
            setSelectedCategory(category)
            setShowCategoryModal(true)
            await loadCategoryTransactions(category)
          }}
          onBudgetChange={(budgetId) => {
            setSelectedBudgetId(budgetId)
          }}
          onBudgetDelete={async (budgetId) => {
            // Refresh budgets after deletion
            await loadBudgets()
            // If deleted budget was selected, clear selection to trigger auto-selection
            if (budgetId === selectedBudgetId) {
              setSelectedBudgetId(null)
            }
          }}
        />
      )}


      {/* Transaction Assignment */}
      {selectedBudget && (
        <TransactionAssignment
          budget={selectedBudget}
          onAssignmentComplete={() => {
            // Refresh only the current budget to update dashboard data without scroll issues
            refreshCurrentBudgetOnly()
          }}
        />
      )}

      {/* Category Transactions Modal */}
      {showCategoryModal && selectedCategory && (
        <CategoryTransactionsModal
          category={selectedCategory}
          transactions={categoryTransactions}
          isOpen={showCategoryModal}
          isLoading={loadingCategoryTransactions}
          onClose={() => {
            setShowCategoryModal(false)
            setSelectedCategory(null)
            setCategoryTransactions([])
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