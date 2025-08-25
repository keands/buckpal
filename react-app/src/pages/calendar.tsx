import { useState, useEffect } from 'react'
import { useAuth } from '@/contexts/auth-context'
import { apiClient } from '@/lib/api'
import { formatCurrency, cn } from '@/lib/utils'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { 
  ChevronLeft, 
  ChevronRight, 
  Calendar as CalendarIcon,
  TrendingUp,
  TrendingDown,
  ArrowUpRight,
  ArrowDownLeft,
  X,
  Edit3,
  Save,
  Trash2
} from 'lucide-react'
import type { CalendarDay, Transaction, Account, Category, Budget, BudgetCategory } from '@/types/api'
import { startOfMonth, endOfMonth, format, addMonths, subMonths, eachDayOfInterval, getDay, isToday, parseISO } from 'date-fns'
import { fr } from 'date-fns/locale'

export default function CalendarPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth()
  const [calendarData, setCalendarData] = useState<CalendarDay[]>([])
  const [currentMonth, setCurrentMonth] = useState(new Date())
  const [isLoading, setIsLoading] = useState(true)
  const [selectedDay, setSelectedDay] = useState<CalendarDay | null>(null)
  const [dayTransactions, setDayTransactions] = useState<Transaction[]>([])
  const [loadingTransactions, setLoadingTransactions] = useState(false)
  const [showTransactionPanel, setShowTransactionPanel] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Edit modal state
  const [showEditModal, setShowEditModal] = useState(false)
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null)
  const [accounts, setAccounts] = useState<Account[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [budgetCategories, setBudgetCategories] = useState<BudgetCategory[]>([])
  const [saving, setSaving] = useState(false)
  
  // Form state
  const [formData, setFormData] = useState({
    description: '',
    amount: '',
    transactionType: 'EXPENSE' as 'INCOME' | 'EXPENSE' | 'TRANSFER',
    accountId: '',
    categoryId: '',
    budgetCategoryId: '',
    transactionDate: ''
  })

  useEffect(() => {
    if (isAuthenticated && !authLoading) {
      loadCalendarData()
    }
  }, [currentMonth, isAuthenticated, authLoading])

  useEffect(() => {
    if (isAuthenticated && !authLoading) {
      loadAccountsAndCategories()
    }
  }, [isAuthenticated, authLoading])

  const loadAccountsAndCategories = async () => {
    try {
      const [accountsData, categoriesData, budgetsData] = await Promise.all([
        apiClient.getAccounts(),
        apiClient.getCategories(),
        apiClient.getBudgets()
      ])
      setAccounts(accountsData)
      setCategories(categoriesData)
      setBudgets(budgetsData)
      
      // Extract all budget categories from all budgets
      const allBudgetCategories = budgetsData.flatMap(budget => budget.budgetCategories || [])
      setBudgetCategories(allBudgetCategories)
      
      setError(null)
    } catch (error) {
      console.error('Error loading accounts and categories:', error)
      setError('Erreur lors du chargement des comptes et catégories')
    }
  }

  const loadCalendarData = async () => {
    try {
      setIsLoading(true)
      
      const start = startOfMonth(currentMonth)
      const end = endOfMonth(currentMonth)
      
      const startDate = format(start, 'yyyy-MM-dd')
      const endDate = format(end, 'yyyy-MM-dd')
      
      const data = await apiClient.getCalendarData(startDate, endDate)
      setCalendarData(data)
      setError(null)
    } catch (error) {
      console.error('Error loading calendar data:', error)
      setError('Erreur lors du chargement des données du calendrier')
      setCalendarData([])
    } finally {
      setIsLoading(false)
    }
  }


  const loadDayTransactions = async (date: string) => {
    try {
      setLoadingTransactions(true)
      const transactions = await apiClient.getTransactionsByDate(date)
      setDayTransactions(transactions)
      setError(null)
    } catch (error) {
      console.error('Error loading day transactions:', error)
      setError('Erreur lors du chargement des transactions de la journée')
      setDayTransactions([])
    } finally {
      setLoadingTransactions(false)
    }
  }

  const goToPreviousMonth = () => {
    setCurrentMonth(subMonths(currentMonth, 1))
  }

  const goToNextMonth = () => {
    setCurrentMonth(addMonths(currentMonth, 1))
  }

  const openTransactionPanel = (dayData: CalendarDay) => {
    setSelectedDay(dayData)
    setShowTransactionPanel(true)
    loadDayTransactions(dayData.date)
  }

  const closeTransactionPanel = () => {
    setShowTransactionPanel(false)
    setSelectedDay(null)
    setDayTransactions([])
  }

  const openEditModal = (transaction: Transaction) => {
    setEditingTransaction(transaction)
    setFormData({
      description: transaction.description || '',
      amount: Math.abs(transaction.amount).toString(),
      transactionType: transaction.transactionType,
      accountId: transaction.accountId?.toString() || '',
      categoryId: transaction.categoryId?.toString() || '',
      budgetCategoryId: transaction.budgetCategoryId?.toString() || '',
      transactionDate: transaction.transactionDate?.split('T')[0] || '' // Extract date part
    })
    setShowEditModal(true)
  }

  const closeEditModal = () => {
    setShowEditModal(false)
    setEditingTransaction(null)
    setFormData({
      description: '',
      amount: '',
      transactionType: 'EXPENSE',
      accountId: '',
      categoryId: '',
      budgetCategoryId: '',
      transactionDate: ''
    })
  }

  const handleSaveTransaction = async () => {
    if (!editingTransaction) return

    try {
      setSaving(true)
      
      const updatedTransaction: Partial<Transaction> = {
        description: formData.description,
        amount: formData.transactionType === 'EXPENSE' ? -Math.abs(Number(formData.amount)) : Math.abs(Number(formData.amount)),
        transactionType: formData.transactionType,
        transactionDate: formData.transactionDate,
        accountId: formData.accountId ? Number(formData.accountId) : undefined,
        categoryId: formData.categoryId ? Number(formData.categoryId) : undefined
      }

      await apiClient.updateTransaction(editingTransaction.id, updatedTransaction)
      
      // Refresh transactions in the sidebar
      if (selectedDay) {
        await loadDayTransactions(selectedDay.date)
      }
      
      // Refresh calendar data
      await loadCalendarData()
      
      closeEditModal()
      setError(null)
    } catch (error) {
      console.error('Error updating transaction:', error)
      setError('Erreur lors de la mise à jour de la transaction')
    } finally {
      setSaving(false)
    }
  }

  const handleDeleteTransaction = async () => {
    if (!editingTransaction) return
    
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette transaction ?')) {
      return
    }

    try {
      setSaving(true)
      
      await apiClient.deleteTransaction(editingTransaction.id)
      
      // Refresh transactions in the sidebar
      if (selectedDay) {
        await loadDayTransactions(selectedDay.date)
      }
      
      // Refresh calendar data
      await loadCalendarData()
      
      closeEditModal()
      setError(null)
    } catch (error) {
      console.error('Error deleting transaction:', error)
      setError('Erreur lors de la suppression de la transaction')
    } finally {
      setSaving(false)
    }
  }

  const getCalendarDays = () => {
    const start = startOfMonth(currentMonth)
    const end = endOfMonth(currentMonth)
    
    // Get all days of the month
    const daysOfMonth = eachDayOfInterval({ start, end })
    
    // Add padding days from previous month
    const startDay = getDay(start)
    const paddingDays = startDay === 0 ? 6 : startDay - 1 // Monday = 0
    
    const calendarDays = []
    
    // Add padding from previous month
    for (let i = paddingDays - 1; i >= 0; i--) {
      const paddingDate = new Date(start)
      paddingDate.setDate(start.getDate() - i - 1)
      calendarDays.push({ date: paddingDate, isCurrentMonth: false })
    }
    
    // Add current month days
    daysOfMonth.forEach(date => {
      calendarDays.push({ date, isCurrentMonth: true })
    })
    
    // Add padding to complete the last week (42 days total for 6 weeks)
    while (calendarDays.length < 42) {
      const lastDate: Date = calendarDays[calendarDays.length - 1].date
      const nextDate: Date = new Date(lastDate)
      nextDate.setDate(lastDate.getDate() + 1)
      calendarDays.push({ date: nextDate, isCurrentMonth: false })
    }
    
    return calendarDays
  }

  const getDayData = (date: Date): CalendarDay | undefined => {
    const dateStr = format(date, 'yyyy-MM-dd')
    return calendarData.find(day => day.date === dateStr)
  }

  const getDayColor = (dayData: CalendarDay | undefined) => {
    if (!dayData || dayData.transactionCount === 0) return 'bg-gray-50'
    
    const { netAmount } = dayData
    if (netAmount > 0) return 'bg-green-100 border-green-200'
    if (netAmount < 0) return 'bg-red-100 border-red-200'
    return 'bg-gray-100 border-gray-200'
  }

  const monthlyStats = calendarData.reduce(
    (acc, day) => ({
      totalIncome: acc.totalIncome + day.totalIncome,
      totalExpense: acc.totalExpense + day.totalExpense,
      netAmount: acc.netAmount + day.netAmount,
      transactionCount: acc.transactionCount + day.transactionCount,
    }),
    { totalIncome: 0, totalExpense: 0, netAmount: 0, transactionCount: 0 }
  )

  if (authLoading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-lg">Authentification...</div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-lg">Redirection...</div>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-lg">Chargement du calendrier...</div>
      </div>
    )
  }

  const calendarDays = getCalendarDays()

  return (
    <div className="space-y-6">
      {/* Error Banner */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-start">
            <div className="flex-shrink-0">
              <X className="h-5 w-5 text-red-400" />
            </div>
            <div className="ml-3 flex-1">
              <p className="text-sm text-red-800">{error}</p>
            </div>
            <div className="ml-auto pl-3">
              <button
                type="button"
                className="inline-flex rounded-md bg-red-50 p-1.5 text-red-500 hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-600 focus:ring-offset-2 focus:ring-offset-red-50"
                onClick={() => setError(null)}
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Calendrier</h1>
          <p className="text-gray-600">Vue mensuelle de vos transactions</p>
        </div>
        
        <div className="flex items-center space-x-3">
          <Button variant="outline" onClick={goToPreviousMonth}>
            <ChevronLeft className="w-4 h-4" />
          </Button>
          <h2 className="text-xl font-semibold text-gray-900 min-w-[200px] text-center">
            {format(currentMonth, 'MMMM yyyy', { locale: fr })}
          </h2>
          <Button variant="outline" onClick={goToNextMonth}>
            <ChevronRight className="w-4 h-4" />
          </Button>
        </div>
      </div>

      {/* Monthly Summary */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Revenus du mois</CardTitle>
            <TrendingUp className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              +{formatCurrency(monthlyStats.totalIncome)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Dépenses du mois</CardTitle>
            <TrendingDown className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">
              -{formatCurrency(monthlyStats.totalExpense)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Solde net</CardTitle>
            <CalendarIcon className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${
              monthlyStats.netAmount >= 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              {monthlyStats.netAmount >= 0 ? '+' : ''}
              {formatCurrency(monthlyStats.netAmount)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Transactions</CardTitle>
            <CalendarIcon className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {monthlyStats.transactionCount}
            </div>
            <p className="text-xs text-muted-foreground">
              Ce mois-ci
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Calendar Grid */}
      <Card>
        <CardContent className="p-6">
          {/* Day Headers */}
          <div className="grid grid-cols-7 gap-2 mb-4">
            {['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'].map(day => (
              <div key={day} className="text-center text-sm font-medium text-gray-500 py-2">
                {day}
              </div>
            ))}
          </div>

          {/* Calendar Days */}
          <div className="grid grid-cols-7 gap-2">
            {calendarDays.map(({ date, isCurrentMonth }, index) => {
              const dayData = getDayData(date)
              const dayNumber = date.getDate()
              
              return (
                <div
                  key={index}
                  className={cn(
                    'h-24 border rounded-lg p-2 cursor-pointer hover:shadow-md transition-shadow',
                    isCurrentMonth ? getDayColor(dayData) : 'bg-gray-50 text-gray-400',
                    isToday(date) && isCurrentMonth && 'ring-2 ring-blue-500',
                    selectedDay?.date === format(date, 'yyyy-MM-dd') && 'ring-2 ring-primary'
                  )}
                  onClick={() => {
                    if (dayData && isCurrentMonth) {
                      openTransactionPanel(dayData)
                    }
                  }}
                >
                  <div className="flex justify-between items-start h-full">
                    <span className={cn(
                      'text-sm font-medium',
                      !isCurrentMonth && 'text-gray-400'
                    )}>
                      {dayNumber}
                    </span>
                    
                    {dayData && dayData.transactionCount > 0 && isCurrentMonth && (
                      <div className="flex flex-col items-end space-y-1 text-xs">
                        {dayData.totalIncome > 0 && (
                          <div className="flex items-center text-green-600">
                            <ArrowUpRight className="w-3 h-3 mr-1" />
                            <span>{formatCurrency(dayData.totalIncome)}</span>
                          </div>
                        )}
                        {dayData.totalExpense > 0 && (
                          <div className="flex items-center text-red-600">
                            <ArrowDownLeft className="w-3 h-3 mr-1" />
                            <span>{formatCurrency(dayData.totalExpense)}</span>
                          </div>
                        )}
                        <div className="text-gray-500 text-xs">
                          {dayData.transactionCount} trans.
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </CardContent>
      </Card>

      {/* Transaction Sidebar Panel */}
      {showTransactionPanel && selectedDay && (
        <>
          {/* Backdrop */}
          <div 
            className="fixed inset-0 bg-black/20 z-40"
            onClick={closeTransactionPanel}
          />
          
          {/* Slide-out Panel */}
          <div className="fixed right-0 top-0 h-full w-96 bg-white shadow-xl z-50 transform transition-transform duration-300 ease-in-out">
            <div className="flex flex-col h-full">
              {/* Panel Header */}
              <div className="flex items-center justify-between p-4 border-b bg-gray-50">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">
                    {format(parseISO(selectedDay.date), 'EEEE d MMMM', { locale: fr })}
                  </h3>
                  <p className="text-sm text-gray-600">
                    {selectedDay.transactionCount} transaction{selectedDay.transactionCount > 1 ? 's' : ''}
                  </p>
                </div>
                <Button 
                  variant="ghost" 
                  size="sm"
                  onClick={closeTransactionPanel}
                  className="hover:bg-gray-200"
                >
                  <X className="w-4 h-4" />
                </Button>
              </div>
              
              {/* Summary Cards */}
              <div className="p-4 space-y-3 border-b">
                <div className="flex items-center justify-between p-3 bg-green-50 rounded-lg">
                  <div className="flex items-center space-x-2">
                    <ArrowUpRight className="w-4 h-4 text-green-600" />
                    <span className="text-sm font-medium">Revenus</span>
                  </div>
                  <span className="text-green-600 font-bold text-sm">
                    +{formatCurrency(selectedDay.totalIncome)}
                  </span>
                </div>
                
                <div className="flex items-center justify-between p-3 bg-red-50 rounded-lg">
                  <div className="flex items-center space-x-2">
                    <ArrowDownLeft className="w-4 h-4 text-red-600" />
                    <span className="text-sm font-medium">Dépenses</span>
                  </div>
                  <span className="text-red-600 font-bold text-sm">
                    -{formatCurrency(selectedDay.totalExpense)}
                  </span>
                </div>
                
                <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <div className="flex items-center space-x-2">
                    <CalendarIcon className="w-4 h-4 text-gray-600" />
                    <span className="text-sm font-medium">Solde net</span>
                  </div>
                  <span className={`font-bold text-sm ${
                    selectedDay.netAmount >= 0 ? 'text-green-600' : 'text-red-600'
                  }`}>
                    {selectedDay.netAmount >= 0 ? '+' : ''}
                    {formatCurrency(selectedDay.netAmount)}
                  </span>
                </div>
              </div>
              
              {/* Transactions List */}
              <div className="flex-1 overflow-y-auto p-4">
                <h4 className="text-sm font-semibold text-gray-900 mb-3">Transactions</h4>
                
                {loadingTransactions ? (
                  <div className="text-center py-8">
                    <div className="text-sm text-gray-600">Chargement...</div>
                  </div>
                ) : dayTransactions.length > 0 ? (
                  <div className="space-y-3">
                    {dayTransactions.map((transaction) => (
                      <div key={transaction.id} className="p-3 border rounded-lg hover:bg-gray-50 transition-colors group">
                        <div className="flex items-start justify-between">
                          <div className="flex items-start space-x-3 flex-1">
                            <div className={cn(
                              'w-2 h-2 rounded-full mt-2',
                              transaction.transactionType === 'INCOME' ? 'bg-green-500' : 'bg-red-500'
                            )} />
                            <div className="flex-1 min-w-0">
                              <div className="font-medium text-sm text-gray-900 truncate">
                                {transaction.description || 'Transaction'}
                              </div>
                              <div className="text-xs text-gray-500 mt-1">
                                {transaction.accountName || 'Compte inconnu'}
                              </div>
                              {transaction.categoryName && (
                                <div className="text-xs text-gray-400 mt-1">
                                  {transaction.categoryName}
                                </div>
                              )}
                            </div>
                          </div>
                          
                          <div className="flex items-start space-x-2">
                            <div className="text-right">
                              <div className={cn(
                                'font-semibold text-sm',
                                transaction.transactionType === 'INCOME' ? 'text-green-600' : 'text-red-600'
                              )}>
                                {transaction.transactionType === 'INCOME' ? '+' : '-'}
                                {formatCurrency(Math.abs(transaction.amount))}
                              </div>
                              <div className="text-xs text-gray-500 mt-1">
                                {transaction.transactionDate ? format(parseISO(transaction.transactionDate), 'HH:mm', { locale: fr }) : '--:--'}
                              </div>
                            </div>
                            
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => openEditModal(transaction)}
                              className="opacity-0 group-hover:opacity-100 transition-opacity p-1 h-6 w-6"
                            >
                              <Edit3 className="w-3 h-3" />
                            </Button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-8">
                    <div className="text-sm text-gray-600">Aucune transaction ce jour-là</div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}

      {/* Edit Transaction Modal */}
      {showEditModal && editingTransaction && (
        <>
          {/* Modal Backdrop */}
          <div 
            className="fixed inset-0 bg-black/50 z-50"
            onClick={closeEditModal}
          />
          
          {/* Modal Content */}
          <div className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-white rounded-lg shadow-xl z-50 w-full max-w-md max-h-[90vh] overflow-y-auto">
            <div className="flex flex-col">
              {/* Modal Header */}
              <div className="flex items-center justify-between p-4 border-b">
                <h3 className="text-lg font-semibold text-gray-900">
                  Modifier la transaction
                </h3>
                <Button 
                  variant="ghost" 
                  size="sm"
                  onClick={closeEditModal}
                  className="hover:bg-gray-100"
                >
                  <X className="w-4 h-4" />
                </Button>
              </div>
              
              {/* Modal Body */}
              <div className="p-4 space-y-4">
                {/* Description */}
                <div className="space-y-2">
                  <Label htmlFor="description">Description</Label>
                  <Textarea
                    id="description"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Description de la transaction"
                    rows={2}
                  />
                </div>

                {/* Amount */}
                <div className="space-y-2">
                  <Label htmlFor="amount">Montant</Label>
                  <Input
                    id="amount"
                    type="number"
                    step="0.01"
                    value={formData.amount}
                    onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                    placeholder="0.00"
                  />
                </div>

                {/* Transaction Type */}
                <div className="space-y-2">
                  <Label htmlFor="transactionType">Type</Label>
                  <Select 
                    id="transactionType"
                    value={formData.transactionType} 
                    onChange={(e) => setFormData({ ...formData, transactionType: e.target.value as 'INCOME' | 'EXPENSE' | 'TRANSFER' })}
                  >
                    <option value="INCOME">Revenu</option>
                    <option value="EXPENSE">Dépense</option>
                    <option value="TRANSFER">Transfert</option>
                  </Select>
                </div>

                {/* Account */}
                <div className="space-y-2">
                  <Label htmlFor="account">Compte</Label>
                  <Select 
                    id="account"
                    value={formData.accountId} 
                    onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
                  >
                    <option value="">Sélectionner un compte</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id.toString()}>
                        {account.name}
                      </option>
                    ))}
                  </Select>
                </div>

                {/* Category */}
                <div className="space-y-2">
                  <Label htmlFor="category">Catégorie (optionnel)</Label>
                  <Select 
                    id="category"
                    value={formData.categoryId} 
                    onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                  >
                    <option value="">Aucune catégorie</option>
                    {categories.map((category) => (
                      <option key={category.id} value={category.id.toString()}>
                        {category.name}
                      </option>
                    ))}
                  </Select>
                </div>

                {/* Budget Category */}
                <div className="space-y-2">
                  <Label htmlFor="budgetCategory">Catégorie de budget (optionnel)</Label>
                  <Select 
                    id="budgetCategory"
                    value={formData.budgetCategoryId} 
                    onChange={(e) => setFormData({ ...formData, budgetCategoryId: e.target.value })}
                  >
                    <option value="">Aucune catégorie de budget</option>
                    {budgetCategories.map((budgetCategory) => (
                      <option key={budgetCategory.id} value={budgetCategory.id.toString()}>
                        {budgetCategory.name}
                      </option>
                    ))}
                  </Select>
                </div>

                {/* Date */}
                <div className="space-y-2">
                  <Label htmlFor="transactionDate">Date</Label>
                  <Input
                    id="transactionDate"
                    type="date"
                    value={formData.transactionDate}
                    onChange={(e) => setFormData({ ...formData, transactionDate: e.target.value })}
                  />
                </div>
              </div>
              
              {/* Modal Footer */}
              <div className="flex items-center justify-between p-4 border-t bg-gray-50">
                <Button
                  variant="destructive"
                  onClick={handleDeleteTransaction}
                  disabled={saving}
                  className="flex items-center space-x-2"
                >
                  <Trash2 className="w-4 h-4" />
                  <span>Supprimer</span>
                </Button>
                
                <div className="flex space-x-2">
                  <Button
                    variant="outline"
                    onClick={closeEditModal}
                    disabled={saving}
                  >
                    Annuler
                  </Button>
                  <Button
                    onClick={handleSaveTransaction}
                    disabled={saving || !formData.amount || !formData.accountId}
                    className="flex items-center space-x-2"
                  >
                    <Save className="w-4 h-4" />
                    <span>{saving ? 'Enregistrement...' : 'Enregistrer'}</span>
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}