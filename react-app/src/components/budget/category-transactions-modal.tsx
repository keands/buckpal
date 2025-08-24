import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { formatCurrencyI18n } from '@/lib/i18n-utils'
import { 
  X, 
  Search, 
  Filter, 
  Plus, 
  Download, 
  Edit, 
  Calendar,
  DollarSign,
  ShoppingCart,
  TrendingUp,
  AlertCircle,
  CheckCircle
} from 'lucide-react'

interface Transaction {
  id: number
  date: string
  description: string
  merchant?: string
  amount: number
  type: 'INCOME' | 'EXPENSE' | 'TRANSFER'
  isPending?: boolean
  account?: string
}

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

interface CategoryTransactionsModalProps {
  category: CategoryData
  transactions: Transaction[]
  isOpen: boolean
  onClose: () => void
  onEditTransaction?: (transaction: Transaction) => void
  onAddTransaction?: () => void
  onEditCategoryBudget?: (category: CategoryData) => void
}

export default function CategoryTransactionsModal({
  category,
  transactions,
  isOpen,
  onClose,
  onEditTransaction,
  onAddTransaction,
  onEditCategoryBudget
}: CategoryTransactionsModalProps) {
  const { t } = useTranslation()
  const [searchTerm, setSearchTerm] = useState('')
  const [sortBy, setSortBy] = useState<'date' | 'amount' | 'description'>('date')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc')

  if (!isOpen) return null

  // Filter and sort transactions
  const filteredTransactions = transactions
    .filter(transaction => 
      transaction.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (transaction.merchant && transaction.merchant.toLowerCase().includes(searchTerm.toLowerCase()))
    )
    .sort((a, b) => {
      let comparison = 0
      
      switch (sortBy) {
        case 'date':
          comparison = new Date(a.date).getTime() - new Date(b.date).getTime()
          break
        case 'amount':
          comparison = a.amount - b.amount
          break
        case 'description':
          comparison = a.description.localeCompare(b.description)
          break
      }
      
      return sortOrder === 'asc' ? comparison : -comparison
    })

  const totalTransactions = filteredTransactions.length
  const totalAmount = filteredTransactions.reduce((sum, t) => sum + Math.abs(t.amount), 0)
  const avgTransaction = totalTransactions > 0 ? totalAmount / totalTransactions : 0

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    })
  }

  const getTransactionIcon = (transaction: Transaction) => {
    if (transaction.isPending) return <Clock className="w-4 h-4 text-yellow-600" />
    if (transaction.type === 'INCOME') return <TrendingUp className="w-4 h-4 text-green-600" />
    if (transaction.type === 'EXPENSE') return <ShoppingCart className="w-4 h-4 text-red-600" />
    return <DollarSign className="w-4 h-4 text-blue-600" />
  }

  const getStatusColor = (usagePercentage: number, isOverBudget: boolean) => {
    if (isOverBudget) return 'text-red-600 bg-red-50 border-red-200'
    if (usagePercentage >= 90) return 'text-orange-600 bg-orange-50 border-orange-200'
    if (usagePercentage >= 75) return 'text-yellow-600 bg-yellow-50 border-yellow-200'
    return 'text-green-600 bg-green-50 border-green-200'
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden">
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
                {totalTransactions} {t('transactions.title').toLowerCase()} • {formatCurrencyI18n(totalAmount)}
              </p>
            </div>
          </div>
          <Button variant="outline" size="sm" onClick={onClose}>
            <X className="w-4 h-4" />
          </Button>
        </div>

        {/* Category Summary */}
        <div className="p-6 border-b bg-gray-50">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">{t('budget.allocated')}</p>
                  <p className="text-lg font-semibold">{formatCurrencyI18n(category.allocatedAmount)}</p>
                </div>
                <DollarSign className="w-5 h-5 text-blue-600" />
              </div>
            </Card>

            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">{t('budget.spent')}</p>
                  <p className="text-lg font-semibold">{formatCurrencyI18n(category.spentAmount)}</p>
                </div>
                <ShoppingCart className="w-5 h-5 text-red-600" />
              </div>
            </Card>

            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">{t('budget.remaining')}</p>
                  <p className={`text-lg font-semibold ${category.remainingAmount >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {formatCurrencyI18n(category.remainingAmount)}
                  </p>
                </div>
                {category.remainingAmount >= 0 ? 
                  <CheckCircle className="w-5 h-5 text-green-600" /> : 
                  <AlertCircle className="w-5 h-5 text-red-600" />
                }
              </div>
            </Card>

            <Card className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600">{t('budget.avgTransaction')}</p>
                  <p className="text-lg font-semibold">{formatCurrencyI18n(avgTransaction)}</p>
                </div>
                <TrendingUp className="w-5 h-5 text-purple-600" />
              </div>
            </Card>
          </div>

          {/* Progress Bar */}
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span>{t('budget.budgetProgress')}</span>
              <span className={category.isOverBudget ? 'text-red-600 font-medium' : ''}>
                {category.usagePercentage.toFixed(1)}%
              </span>
            </div>
            <Progress value={Math.min(category.usagePercentage, 100)} className="h-3" />
          </div>

          {/* Status Badge */}
          <div className="mt-4 flex items-center justify-between">
            <Badge className={getStatusColor(category.usagePercentage, category.isOverBudget)}>
              {category.isOverBudget ? t('budget.overBudget') : 
               category.usagePercentage >= 90 ? t('budget.nearLimit') : 
               t('budget.onTrack')}
            </Badge>

            <div className="flex space-x-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => onEditCategoryBudget?.(category)}
              >
                <Edit className="w-4 h-4 mr-2" />
                {t('budget.editBudget')}
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {/* TODO: Export transactions */}}
              >
                <Download className="w-4 h-4 mr-2" />
                {t('common.export')}
              </Button>
            </div>
          </div>
        </div>

        {/* Controls */}
        <div className="p-4 border-b bg-white">
          <div className="flex items-center justify-between space-x-4">
            <div className="flex items-center space-x-4 flex-1">
              <div className="relative flex-1 max-w-sm">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <Input
                  placeholder={t('transactions.searchPlaceholder')}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
              
              <div className="flex items-center space-x-2">
                <Filter className="w-4 h-4 text-gray-400" />
                <select
                  value={`${sortBy}-${sortOrder}`}
                  onChange={(e) => {
                    const [field, order] = e.target.value.split('-')
                    setSortBy(field as any)
                    setSortOrder(order as any)
                  }}
                  className="border rounded px-2 py-1 text-sm"
                >
                  <option value="date-desc">{t('transactions.sortByDateDesc')}</option>
                  <option value="date-asc">{t('transactions.sortByDateAsc')}</option>
                  <option value="amount-desc">{t('transactions.sortByAmountDesc')}</option>
                  <option value="amount-asc">{t('transactions.sortByAmountAsc')}</option>
                  <option value="description-asc">{t('transactions.sortByDescription')}</option>
                </select>
              </div>
            </div>

            <Button onClick={onAddTransaction} size="sm">
              <Plus className="w-4 h-4 mr-2" />
              {t('transactions.addTransaction')}
            </Button>
          </div>
        </div>

        {/* Transaction List */}
        <div className="flex-1 overflow-auto max-h-96">
          {filteredTransactions.length > 0 ? (
            <div className="divide-y">
              {filteredTransactions.map((transaction) => (
                <div
                  key={transaction.id}
                  className="p-4 hover:bg-gray-50 transition-colors cursor-pointer"
                  onClick={() => onEditTransaction?.(transaction)}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                      {getTransactionIcon(transaction)}
                      <div className="flex-1">
                        <div className="flex items-center space-x-2">
                          <p className="font-medium">{transaction.description}</p>
                          {transaction.isPending && (
                            <Badge variant="secondary" className="text-xs">
                              {t('transactions.pending')}
                            </Badge>
                          )}
                        </div>
                        <div className="flex items-center space-x-4 text-sm text-gray-600 mt-1">
                          <span className="flex items-center">
                            <Calendar className="w-3 h-3 mr-1" />
                            {formatDate(transaction.date)}
                          </span>
                          {transaction.merchant && (
                            <span>{transaction.merchant}</span>
                          )}
                          {transaction.account && (
                            <span>{transaction.account}</span>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className={`font-semibold ${
                        transaction.type === 'INCOME' ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {transaction.type === 'INCOME' ? '+' : '-'}{formatCurrencyI18n(Math.abs(transaction.amount))}
                      </p>
                      <p className="text-xs text-gray-500 capitalize">
                        {t(`transactions.transactionTypes.${transaction.type}`)}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="p-8 text-center text-gray-500">
              <ShoppingCart className="w-12 h-12 mx-auto text-gray-300 mb-4" />
              <p className="text-lg font-medium mb-2">
                {searchTerm ? t('transactions.noTransactionsFound') : t('transactions.noTransactionsInCategory')}
              </p>
              <p className="text-sm mb-4">
                {searchTerm ? 
                  t('transactions.tryDifferentSearch') : 
                  t('transactions.addFirstTransaction')
                }
              </p>
              {!searchTerm && (
                <Button onClick={onAddTransaction}>
                  <Plus className="w-4 h-4 mr-2" />
                  {t('transactions.addTransaction')}
                </Button>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t bg-gray-50">
          <div className="flex items-center justify-between text-sm text-gray-600">
            <span>
              {t('transactions.showingResults', { 
                count: filteredTransactions.length, 
                total: transactions.length 
              })}
            </span>
            <span>
              {t('transactions.totalAmount')}: {formatCurrencyI18n(totalAmount)}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}