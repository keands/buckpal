import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import IncomeManagement from './income-management'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/advanced-select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { formatCurrencyI18n } from '@/lib/i18n-utils'
import { translateBudgetCategoryName } from '@/lib/budget-utils'
import { apiClient } from '@/lib/api'
import { 
  PieChart, 
  Pie, 
  Cell, 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  Legend
} from 'recharts'
import { 
  TrendingUp, 
  TrendingDown, 
  AlertTriangle, 
  CheckCircle, 
  DollarSign,
  Target,
  Calendar,
  PieChartIcon,
  Trash2
} from 'lucide-react'

interface BudgetData {
  id: number
  name: string
  budgetMonth: number
  budgetYear: number
  projectedIncome: number
  actualIncome: number
  totalAllocatedAmount: number
  totalSpentAmount: number
  budgetModel: string
  categories: CategoryData[]
  isOverBudget: boolean
  usagePercentage: number
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

interface BudgetProgressDashboardProps {
  budget: BudgetData
  availableBudgets?: BudgetData[]
  onCategoryClick?: (category: CategoryData) => void
  onBudgetChange?: (budgetId: number) => void
  onBudgetDelete?: (budgetId: number) => void
}

const COLORS = [
  '#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', 
  '#06B6D4', '#84CC16', '#F97316', '#EC4899', '#6B7280'
]

const DEFAULT_CATEGORY_COLORS: { [key: string]: string } = {
  'INCOME': '#10B981',
  'EXPENSE': '#EF4444',
  'SAVINGS': '#3B82F6',
  'DEBT': '#F59E0B',
  'PROJECT': '#8B5CF6'
}

export default function BudgetProgressDashboard({ 
  budget, 
  availableBudgets = [],
  onCategoryClick,
  onBudgetChange,
  onBudgetDelete
}: BudgetProgressDashboardProps) {
  const { t } = useTranslation()
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  const handleDeleteBudget = async () => {
    if (!onBudgetDelete) return
    
    setIsDeleting(true)
    try {
      await apiClient.deleteBudget(budget.id)
      onBudgetDelete(budget.id)
      setShowDeleteDialog(false)
    } catch (error) {
      console.error('Failed to delete budget:', error)
      // TODO: Show error toast/notification
    } finally {
      setIsDeleting(false)
    }
  }

  const formatBudgetOption = (budget: BudgetData) => {
    const monthName = t(`months.${getMonthKey(budget.budgetMonth)}`)
    return `${monthName} ${budget.budgetYear}`
  }

  const getMonthKey = (monthNumber: number) => {
    const months = [
      'january', 'february', 'march', 'april', 'may', 'june',
      'july', 'august', 'september', 'october', 'november', 'december'
    ]
    return months[monthNumber - 1]
  }

  const getBudgetModelLabel = (budgetModel: string) => {
    const modelMapping: { [key: string]: string } = {
      'RULE_50_30_20': 'rule50_30_20',
      'RULE_60_20_20': 'rule60_20_20', 
      'RULE_80_20': 'rule80_20',
      'ENVELOPE': 'envelope',
      'ZERO_BASED': 'zero_based',
      'FRENCH_THIRDS': 'frenchThirds',
      'CUSTOM': 'custom'
    }
    
    const translationKey = modelMapping[budgetModel] || budgetModel.toLowerCase()
    return t(`budget.models.${translationKey}`)
  }

  // Prepare data for pie chart
  const pieChartData = budget.categories
    .filter(cat => cat.spentAmount > 0)
    .map((category, index) => ({
      name: translateBudgetCategoryName(category.name, t),
      value: category.spentAmount,
      percentage: ((category.spentAmount / budget.totalSpentAmount) * 100).toFixed(1),
      color: category.colorCode || DEFAULT_CATEGORY_COLORS[category.categoryType] || COLORS[index % COLORS.length],
      category: category
    }))

  // Prepare data for budget vs actual bar chart
  const barChartData = budget.categories.map(category => {
    const translatedName = translateBudgetCategoryName(category.name, t);
    return {
      name: translatedName.length > 10 ? translatedName.substring(0, 10) + '...' : translatedName,
      allocated: category.allocatedAmount,
      spent: category.spentAmount,
      remaining: category.remainingAmount,
      fullName: translatedName
    };
  })

  const overBudgetCategories = budget.categories.filter(cat => cat.isOverBudget)
  const nearLimitCategories = budget.categories.filter(cat => 
    !cat.isOverBudget && cat.usagePercentage >= 80
  )

  const getBudgetStatusColor = (usagePercentage: number, isOverBudget: boolean) => {
    if (isOverBudget) return 'text-red-600 bg-red-50'
    if (usagePercentage >= 90) return 'text-orange-600 bg-orange-50'
    if (usagePercentage >= 75) return 'text-yellow-600 bg-yellow-50'
    return 'text-green-600 bg-green-50'
  }

  const getBudgetStatusIcon = (usagePercentage: number, isOverBudget: boolean) => {
    if (isOverBudget) return <AlertTriangle className="w-4 h-4" />
    if (usagePercentage >= 90) return <TrendingUp className="w-4 h-4" />
    return <CheckCircle className="w-4 h-4" />
  }

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-white p-3 rounded-lg shadow-lg border">
          <p className="font-medium">{data.fullName || label}</p>
          {payload.map((entry: any, index: number) => (
            <p key={index} style={{ color: entry.color }}>
              {`${entry.name}: ${formatCurrencyI18n(entry.value)}`}
            </p>
          ))}
        </div>
      )
    }
    return null
  }

  const PieTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0]
      return (
        <div className="bg-white p-3 rounded-lg shadow-lg border">
          <p className="font-medium">{data.payload.name}</p>
          <p style={{ color: data.payload.color }}>
            {formatCurrencyI18n(data.value)} ({data.payload.percentage}%)
          </p>
        </div>
      )
    }
    return null
  }

  return (
    <div className="space-y-6">
      {/* Budget Selection Header */}
      <Card>
        <CardContent className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold">{formatBudgetOption(budget)}</h2>
              <p className="text-sm text-gray-600">
                {t('budget.budgetModel')}: {getBudgetModelLabel(budget.budgetModel)}
              </p>
            </div>
            
            <div className="flex items-center space-x-2">
              {availableBudgets.length > 1 && (
                <>
                  <label className="text-sm font-medium text-gray-700">
                    {t('budget.selectPeriod')}:
                  </label>
                  <Select
                    value={budget.id.toString()}
                    onValueChange={(value) => onBudgetChange?.(parseInt(value))}
                  >
                    <SelectTrigger className="w-48">
                      <SelectValue selectedValue={formatBudgetOption(budget)} />
                    </SelectTrigger>
                    <SelectContent>
                      {availableBudgets.map((b) => (
                        <SelectItem key={b.id} value={b.id.toString()}>
                          {formatBudgetOption(b)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </>
              )}
              
              {onBudgetDelete && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowDeleteDialog(true)}
                  className="text-red-600 hover:text-red-700 hover:bg-red-50"
                >
                  <Trash2 className="w-4 h-4 mr-1" />
                  {t('common.delete')}
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              {t('budget.projectedIncome')}
            </CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              {formatCurrencyI18n(budget.projectedIncome)}
            </div>
            {budget.actualIncome > 0 && (
              <div className="flex items-center text-xs text-muted-foreground mt-1">
                {budget.actualIncome >= budget.projectedIncome ? (
                  <TrendingUp className="w-3 h-3 text-green-500 mr-1" />
                ) : (
                  <TrendingDown className="w-3 h-3 text-red-500 mr-1" />
                )}
                {t('budget.actualIncome')}: {formatCurrencyI18n(budget.actualIncome)}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              {t('budget.totalSpent')}
            </CardTitle>
            <Target className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${
              budget.isOverBudget ? 'text-red-600' : 'text-blue-600'
            }`}>
              {formatCurrencyI18n(budget.totalSpentAmount)}
            </div>
            <div className="text-xs text-muted-foreground mt-1">
              {budget.usagePercentage.toFixed(1)}% {t('budget.ofIncome')}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              {t('budget.remaining')}
            </CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${
              budget.projectedIncome - budget.totalSpentAmount >= 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              {formatCurrencyI18n(budget.projectedIncome - budget.totalSpentAmount)}
            </div>
            <div className="text-xs text-muted-foreground mt-1">
              {((budget.projectedIncome - budget.totalSpentAmount) / budget.projectedIncome * 100).toFixed(1)}% {t('budget.remaining')}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              {t('budget.budgetHealth')}
            </CardTitle>
            <PieChartIcon className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-lg font-bold flex items-center space-x-2 ${
              getBudgetStatusColor(budget.usagePercentage, budget.isOverBudget)
            }`}>
              {getBudgetStatusIcon(budget.usagePercentage, budget.isOverBudget)}
              <span>
                {budget.isOverBudget ? t('budget.overBudget') : 
                 budget.usagePercentage >= 90 ? t('budget.nearLimit') : 
                 t('budget.onTrack')}
              </span>
            </div>
          </CardContent>
        </Card>
      </div>

      {(overBudgetCategories.length > 0 || nearLimitCategories.length > 0) && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <AlertTriangle className="w-5 h-5 text-orange-600" />
              <span>{t('budget.budgetAlerts')}</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {overBudgetCategories.map((category) => (
                <div key={`over-${category.id}`} className="flex items-center space-x-3 p-3 bg-red-50 border border-red-200 rounded-lg">
                  <AlertTriangle className="w-5 h-5 text-red-600" />
                  <div className="flex-1">
                    <p className="font-medium text-red-800">
                      {t('budget.categoryOverBudget', { category: translateBudgetCategoryName(category.name, t) })}
                    </p>
                    <p className="text-sm text-red-600">
                      {formatCurrencyI18n(category.spentAmount - category.allocatedAmount)} {t('budget.overLimit')}
                    </p>
                  </div>
                </div>
              ))}
              
              {nearLimitCategories.map((category) => (
                <div key={`near-${category.id}`} className="flex items-center space-x-3 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <TrendingUp className="w-5 h-5 text-yellow-600" />
                  <div className="flex-1">
                    <p className="font-medium text-yellow-800">
                      {t('budget.categoryNearLimit', { category: translateBudgetCategoryName(category.name, t) })}
                    </p>
                    <p className="text-sm text-yellow-600">
                      {formatCurrencyI18n(category.remainingAmount)} {t('budget.remaining')} ({category.usagePercentage.toFixed(1)}% {t('budget.used')})
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Income Management - Above Charts */}
      <div className="mb-6">
        <IncomeManagement budgetId={budget.id} />
      </div>
      
      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Spending Distribution Pie Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <PieChartIcon className="w-5 h-5" />
              <span>{t('budget.spendingDistribution')}</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={pieChartData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({name, percentage}) => `${name} (${percentage}%)`}
                    outerRadius={100}
                    fill="#8884d8"
                    dataKey="value"
                    onClick={onCategoryClick ? (data) => onCategoryClick(data.category) : undefined}
                    className={onCategoryClick ? 'cursor-pointer' : ''}
                  >
                    {pieChartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip content={<PieTooltip />} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        {/* Budget vs Actual Bar Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Target className="w-5 h-5" />
              <span>{t('budget.budgetVsActual')}</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={barChartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis 
                    dataKey="name" 
                    angle={-45}
                    textAnchor="end"
                    height={80}
                  />
                  <YAxis />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend />
                  <Bar 
                    dataKey="allocated" 
                    fill="#3B82F6" 
                    name={t('budget.allocated')}
                  />
                  <Bar 
                    dataKey="spent" 
                    fill="#EF4444" 
                    name={t('budget.spent')}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Category Progress Details */}
      <Card>
        <CardHeader>
          <CardTitle>{t('budget.categoryProgress')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {budget.categories.map((category) => (
              <div 
                key={category.id}
                className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 transition-colors"
              >
                <div className="flex-1">
                  <div className="flex items-center justify-between mb-2">
                    <h4 className="font-medium">{translateBudgetCategoryName(category.name, t)}</h4>
                    <div className="flex items-center space-x-2">
                      {category.isOverBudget && (
                        <Badge variant="destructive" className="text-xs">
                          {t('budget.overBudget')}
                        </Badge>
                      )}
                      {!category.isOverBudget && category.usagePercentage >= 80 && (
                        <Badge variant="secondary" className="text-xs bg-yellow-100 text-yellow-800">
                          {t('budget.nearLimit')}
                        </Badge>
                      )}
                      <span className="text-sm text-gray-600">
                        {category.usagePercentage.toFixed(1)}%
                      </span>
                    </div>
                  </div>
                  <div className="mb-2">
                    <Progress 
                      value={Math.min(category.usagePercentage, 100)} 
                      className="h-2"
                    />
                  </div>
                  <div className="flex items-center justify-between text-sm text-gray-600">
                    <span>
                      {formatCurrencyI18n(category.spentAmount)} / {formatCurrencyI18n(category.allocatedAmount)}
                    </span>
                    <span className={category.remainingAmount >= 0 ? 'text-green-600' : 'text-red-600'}>
                      {category.remainingAmount >= 0 ? '+' : ''}{formatCurrencyI18n(category.remainingAmount)} {t('budget.remaining')}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>


      {/* Delete Confirmation Dialog */}
      <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center space-x-2 text-red-600">
              <Trash2 className="w-5 h-5" />
              <span>{t('budget.deleteBudget')}</span>
            </DialogTitle>
            <DialogDescription className="text-gray-600 mt-2">
              {t('budget.deleteBudgetConfirmation', { 
                budget: formatBudgetOption(budget) 
              })}
            </DialogDescription>
          </DialogHeader>
          
          <div className="flex items-center space-x-2 mt-6">
            <Button
              variant="outline"
              onClick={() => setShowDeleteDialog(false)}
              disabled={isDeleting}
              className="flex-1"
            >
              {t('common.cancel')}
            </Button>
            <Button
              variant="destructive"
              onClick={handleDeleteBudget}
              disabled={isDeleting}
              className="flex-1"
            >
              {isDeleting ? t('common.deleting') : t('common.delete')}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}