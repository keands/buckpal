import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Transaction } from '@/types/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { formatCurrencyI18n } from '@/lib/i18n-utils'
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
  Legend
} from 'recharts'
import {
  X,
  Search,
  Filter,
  Edit,
  Trash2,
  Calendar,
  DollarSign,
  ShoppingCart,
  TrendingUp,
  AlertCircle,
  CheckCircle,
  Clock,
  PieChartIcon,
  ArrowLeft,
  MoreHorizontal
} from 'lucide-react'

interface CategoryData {
  id: number
  name: string
  allocatedAmount: number
  spentAmount: number
  remainingAmount: number
  usagePercentage: number
  isOverBudget: boolean
  categoryType: string
  colorCode?: string
}

interface SubcategoryData {
  id: number
  name: string
  amount: number
  percentage: number
  transactionCount: number
  colorCode?: string
  iconName?: string
}

interface EnhancedCategoryModalProps {
  category: CategoryData
  transactions: Transaction[]
  subcategories: SubcategoryData[]
  isOpen: boolean
  isLoading?: boolean
  onClose: () => void
  onEditTransaction?: (transaction: Transaction) => void
  onDeleteTransaction?: (transactionId: number) => void
  onAddTransaction?: () => void
  onEditCategoryBudget?: (category: CategoryData) => void
}

type ViewMode = 'overview' | 'analytics'
type ResponsiveView = 'pie' | 'transactions' | 'details'

const COLORS = [
  '#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6',
  '#06B6D4', '#84CC16', '#F97316', '#EC4899', '#6B7280'
]

export default function EnhancedCategoryModal({
  category,
  transactions,
  subcategories,
  isOpen,
  isLoading = false,
  onClose,
  onEditTransaction,
  onDeleteTransaction,
  onAddTransaction,
  onEditCategoryBudget
}: EnhancedCategoryModalProps) {
  const { t } = useTranslation()
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedSubcategory, setSelectedSubcategory] = useState<string | null>(null)
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null)
  const [viewMode, setViewMode] = useState<ViewMode>('analytics')
  const [mobileView, setMobileView] = useState<ResponsiveView>('pie')

  if (!isOpen) return null

  // Prepare pie chart data - use subcategories if available, otherwise create from transactions
  const pieChartData = useMemo(() => {
    if (subcategories.length > 0) {
      return subcategories.map((subcat, index) => ({
        name: subcat.name,
        value: subcat.amount,
        percentage: subcat.percentage,
        count: subcat.transactionCount,
        color: subcat.colorCode || COLORS[index % COLORS.length],
        subcategory: subcat
      }))
    }

    // Fallback: create simple pie from transactions grouped by detailed category
    const transactionGroups = transactions.reduce((acc, transaction) => {
      // Use detailed category name (categoryName) if available, otherwise fallback to budget category or type
      const groupKey = transaction.categoryName ||
                      transaction.budgetCategoryName ||
                      transaction.transactionType ||
                      'Non catégorisé'

      if (!acc[groupKey]) {
        acc[groupKey] = {
          amount: 0,
          count: 0,
          transactions: []
        }
      }
      acc[groupKey].amount += Math.abs(transaction.amount)
      acc[groupKey].count += 1
      acc[groupKey].transactions.push(transaction)
      return acc
    }, {} as Record<string, { amount: number; count: number; transactions: Transaction[] }>)

    const totalAmount = Object.values(transactionGroups).reduce((sum, group) => sum + group.amount, 0)

    return Object.entries(transactionGroups).map(([name, group], index) => ({
      name,
      value: group.amount,
      percentage: (group.amount / totalAmount) * 100,
      count: group.count,
      color: COLORS[index % COLORS.length],
      subcategory: null
    }))
  }, [subcategories, transactions])

  console.log('📊 Pie chart data:', pieChartData)
  console.log('📈 Subcategories:', subcategories)
  console.log('💳 Transactions sample:', transactions.slice(0, 3))
  console.log('🏷️ Categories found in transactions:', [...new Set(transactions.map(t => t.categoryName).filter(Boolean))])
  console.log('🏪 Merchants found in transactions:', [...new Set(transactions.map(t => t.merchantName).filter(Boolean))])

  // Filter transactions based on selected subcategory and search
  const filteredTransactions = useMemo(() => {
    let filtered = transactions

    // Filter by selected subcategory/category
    if (selectedSubcategory) {
      filtered = filtered.filter(transaction => {
        const transactionCategory = transaction.categoryName ||
                                   transaction.budgetCategoryName ||
                                   transaction.transactionType ||
                                   'Non catégorisé'
        return transactionCategory === selectedSubcategory
      })
    }

    // Filter by search term
    if (searchTerm) {
      filtered = filtered.filter(transaction =>
        (transaction.description?.toLowerCase().includes(searchTerm.toLowerCase()) ?? false) ||
        (transaction.merchantName?.toLowerCase().includes(searchTerm.toLowerCase()) ?? false)
      )
    }

    return filtered.sort((a, b) => new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime())
  }, [transactions, selectedSubcategory, searchTerm])

  const handlePieClick = (data: any) => {
    const subcategoryName = data.name
    setSelectedSubcategory(selectedSubcategory === subcategoryName ? null : subcategoryName)
    setMobileView('transactions') // Switch to transactions view on mobile
  }

  const handleTransactionClick = (transaction: Transaction) => {
    setSelectedTransaction(transaction)
    setMobileView('details') // Switch to details view on mobile
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    })
  }

  const getTransactionIcon = (transaction: Transaction) => {
    if (transaction.isPending) return <Clock className="w-4 h-4 text-yellow-600" />
    if (transaction.transactionType === 'INCOME') return <TrendingUp className="w-4 h-4 text-green-600" />
    if (transaction.transactionType === 'EXPENSE') return <ShoppingCart className="w-4 h-4 text-red-600" />
    return <DollarSign className="w-4 h-4 text-blue-600" />
  }

  const PieTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-white p-3 rounded-lg shadow-lg border">
          <p className="font-medium">{data.name}</p>
          <p style={{ color: data.color }}>
            {formatCurrencyI18n(data.value)} ({data.percentage.toFixed(1)}%)
          </p>
          <p className="text-sm text-gray-600">
            {data.count} transaction{data.count > 1 ? 's' : ''}
          </p>
        </div>
      )
    }
    return null
  }

  // Mobile Navigation
  const MobileNavigation = () => (
    <div className="md:hidden border-b p-4">
      <div className="flex items-center justify-between mb-4">
        <Button variant="ghost" size="sm" onClick={onClose}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          Retour
        </Button>
        <div className="flex space-x-1">
          <Button
            variant={mobileView === 'pie' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setMobileView('pie')}
          >
            <PieChartIcon className="w-4 h-4" />
          </Button>
          <Button
            variant={mobileView === 'transactions' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setMobileView('transactions')}
          >
            <ShoppingCart className="w-4 h-4" />
          </Button>
          <Button
            variant={mobileView === 'details' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setMobileView('details')}
            disabled={!selectedTransaction}
          >
            <Edit className="w-4 h-4" />
          </Button>
        </div>
      </div>
    </div>
  )

  // Pie Chart Column
  const PieChartColumn = () => (
    <div className={`${mobileView !== 'pie' ? 'hidden md:block' : ''}`}>
      <Card className="h-full">
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <PieChartIcon className="w-5 h-5" />
            <span>Répartition</span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="h-80 flex items-center justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              <span className="ml-3 text-gray-600">Chargement...</span>
            </div>
          ) : pieChartData.length > 0 ? (
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={pieChartData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({name, percentage}) => `${name} (${percentage.toFixed(1)}%)`}
                    outerRadius={100}
                    fill="#8884d8"
                    dataKey="value"
                    onClick={handlePieClick}
                    className="cursor-pointer"
                  >
                    {pieChartData.map((entry, index) => (
                      <Cell
                        key={`cell-${index}`}
                        fill={entry.color}
                        stroke={selectedSubcategory === entry.name ? '#000' : 'none'}
                        strokeWidth={selectedSubcategory === entry.name ? 2 : 0}
                      />
                    ))}
                  </Pie>
                  <Tooltip content={<PieTooltip />} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-80 flex items-center justify-center text-gray-500">
              <div className="text-center">
                <PieChartIcon className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                <p className="text-lg font-medium mb-2">Aucune sous-catégorie</p>
                <p className="text-sm">Créez un diagramme simple avec toutes les transactions</p>
              </div>
            </div>
          )}

          {/* Subcategory List */}
          <div className="mt-4 space-y-2 max-h-32 overflow-y-auto">
            {subcategories.map((subcat, index) => (
              <div
                key={subcat.id}
                className={`flex items-center justify-between p-2 rounded cursor-pointer transition-colors ${
                  selectedSubcategory === subcat.name
                    ? 'bg-blue-50 border border-blue-200'
                    : 'hover:bg-gray-50'
                }`}
                onClick={() => setSelectedSubcategory(
                  selectedSubcategory === subcat.name ? null : subcat.name
                )}
              >
                <div className="flex items-center space-x-2">
                  <div
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: subcat.colorCode || COLORS[index % COLORS.length] }}
                  />
                  <span className="text-sm font-medium">{subcat.name}</span>
                </div>
                <div className="text-right">
                  <div className="text-sm font-semibold">{formatCurrencyI18n(subcat.amount)}</div>
                  <div className="text-xs text-gray-500">{subcat.transactionCount} trans.</div>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  )

  // Transactions List Column
  const TransactionsColumn = () => (
    <div className={`${mobileView !== 'transactions' ? 'hidden md:block' : ''}`}>
      <Card className="h-full">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center space-x-2">
              <ShoppingCart className="w-5 h-5" />
              <span>
                Transactions {selectedSubcategory && `- ${selectedSubcategory}`}
              </span>
            </CardTitle>
            {selectedSubcategory && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setSelectedSubcategory(null)}
              >
                Tout afficher
              </Button>
            )}
          </div>

          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            <Input
              placeholder="Rechercher une transaction..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10"
            />
          </div>
        </CardHeader>

        <CardContent>
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {isLoading ? (
              <div className="flex items-center justify-center py-8">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
                <span className="ml-2 text-gray-600">Chargement...</span>
              </div>
            ) : filteredTransactions.length > 0 ? (
              filteredTransactions.map((transaction) => (
                <div
                  key={transaction.id}
                  className={`p-3 border rounded-lg cursor-pointer transition-colors ${
                    selectedTransaction?.id === transaction.id
                      ? 'bg-blue-50 border-blue-200'
                      : 'hover:bg-gray-50'
                  }`}
                  onClick={() => handleTransactionClick(transaction)}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      {getTransactionIcon(transaction)}
                      <div>
                        <div className="font-medium text-sm">{transaction.description}</div>
                        <div className="flex items-center space-x-2 text-xs text-gray-600">
                          <Calendar className="w-3 h-3" />
                          <span>{formatDate(transaction.transactionDate)}</span>
                          {transaction.merchantName && (
                            <span>• {transaction.merchantName}</span>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className={`font-semibold text-sm ${
                        transaction.transactionType === 'INCOME' ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {transaction.transactionType === 'INCOME' ? '+' : '-'}
                        {formatCurrencyI18n(Math.abs(transaction.amount))}
                      </div>
                      {transaction.categoryName && (
                        <Badge variant="outline" className="text-xs mt-1">
                          {transaction.categoryName}
                        </Badge>
                      )}
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <div className="text-center py-8 text-gray-500">
                <ShoppingCart className="w-8 h-8 mx-auto mb-2 text-gray-300" />
                <p>Aucune transaction trouvée</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )

  // Details/Actions Column
  const DetailsColumn = () => (
    <div className={`${mobileView !== 'details' ? 'hidden md:block' : ''}`}>
      <Card className="h-full">
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <Edit className="w-5 h-5" />
            <span>Détails</span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {selectedTransaction ? (
            <div className="space-y-4">
              {/* Transaction Details */}
              <div className="border rounded-lg p-4">
                <div className="flex items-center justify-between mb-3">
                  <h4 className="font-medium">{selectedTransaction.description}</h4>
                  <div className={`font-bold text-lg ${
                    selectedTransaction.transactionType === 'INCOME' ? 'text-green-600' : 'text-red-600'
                  }`}>
                    {selectedTransaction.transactionType === 'INCOME' ? '+' : '-'}
                    {formatCurrencyI18n(Math.abs(selectedTransaction.amount))}
                  </div>
                </div>

                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-600">Date:</span>
                    <span>{formatDate(selectedTransaction.transactionDate)}</span>
                  </div>
                  {selectedTransaction.merchantName && (
                    <div className="flex justify-between">
                      <span className="text-gray-600">Marchand:</span>
                      <span>{selectedTransaction.merchantName}</span>
                    </div>
                  )}
                  {selectedTransaction.categoryName && (
                    <div className="flex justify-between">
                      <span className="text-gray-600">Catégorie:</span>
                      <Badge variant="outline">{selectedTransaction.categoryName}</Badge>
                    </div>
                  )}
                  {selectedTransaction.accountName && (
                    <div className="flex justify-between">
                      <span className="text-gray-600">Compte:</span>
                      <span>{selectedTransaction.accountName}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Actions */}
              <div className="space-y-2">
                <Button
                  onClick={() => onEditTransaction?.(selectedTransaction)}
                  className="w-full"
                  variant="outline"
                >
                  <Edit className="w-4 h-4 mr-2" />
                  Modifier la transaction
                </Button>

                <Button
                  onClick={() => onDeleteTransaction?.(selectedTransaction.id)}
                  className="w-full"
                  variant="outline"
                  className="text-red-600 hover:text-red-700 hover:bg-red-50"
                >
                  <Trash2 className="w-4 h-4 mr-2" />
                  Supprimer
                </Button>
              </div>
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              <MoreHorizontal className="w-8 h-8 mx-auto mb-2 text-gray-300" />
              <p>Sélectionnez une transaction pour voir les détails</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-7xl max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b">
          <div className="flex items-center space-x-4">
            <div
              className="w-4 h-4 rounded-full"
              style={{ backgroundColor: category.colorCode || '#3B82F6' }}
            />
            <div>
              <h2 className="text-xl font-semibold">{category.name}</h2>
              <p className="text-sm text-gray-600">
                {filteredTransactions.length} transaction{filteredTransactions.length > 1 ? 's' : ''} • {formatCurrencyI18n(category.spentAmount)}
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onEditCategoryBudget?.(category)}
            >
              <Edit className="w-4 h-4 mr-2" />
              Éditer le budget
            </Button>
            <Button variant="outline" size="sm" onClick={onClose}>
              <X className="w-4 h-4" />
            </Button>
          </div>
        </div>

        {/* Category Summary */}
        <div className="p-6 border-b bg-gray-50">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">{formatCurrencyI18n(category.allocatedAmount)}</div>
              <div className="text-sm text-gray-600">Alloué</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-red-600">{formatCurrencyI18n(category.spentAmount)}</div>
              <div className="text-sm text-gray-600">Dépensé</div>
            </div>
            <div className="text-center">
              <div className={`text-2xl font-bold ${category.remainingAmount >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                {formatCurrencyI18n(category.remainingAmount)}
              </div>
              <div className="text-sm text-gray-600">Restant</div>
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span>Progression du budget</span>
              <span className={category.isOverBudget ? 'text-red-600 font-medium' : ''}>
                {category.usagePercentage.toFixed(1)}%
              </span>
            </div>
            <Progress value={Math.min(category.usagePercentage, 100)} className="h-3" />
          </div>
        </div>

        {/* Mobile Navigation */}
        <MobileNavigation />

        {/* Main Content - 3 Column Layout */}
        <div className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 h-[500px]">
            {/* Column 1: Pie Chart */}
            <PieChartColumn />

            {/* Column 2: Transactions List */}
            <TransactionsColumn />

            {/* Column 3: Details/Actions */}
            <DetailsColumn />
          </div>
        </div>
      </div>
    </div>
  )
}