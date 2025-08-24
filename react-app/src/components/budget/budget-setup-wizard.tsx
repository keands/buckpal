import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/advanced-select'
import { Slider } from '@/components/ui/slider'
import { Switch } from '@/components/ui/switch'
import { formatCurrencyI18n } from '@/lib/i18n-utils'
import { ArrowLeft, ArrowRight, Check, PieChart, TrendingUp, Settings } from 'lucide-react'

interface BudgetSetupWizardProps {
  onComplete: (budgetData: any) => void
  onCancel: () => void
  previousMonthIncome?: number
}

export default function BudgetSetupWizard({ 
  onComplete, 
  onCancel, 
  previousMonthIncome = 0 
}: BudgetSetupWizardProps) {
  const { t } = useTranslation()
  const [step, setStep] = useState(1)
  const [budgetData, setBudgetData] = useState({
    month: new Date().getMonth() + 1,
    year: new Date().getFullYear(),
    projectedIncome: previousMonthIncome,
    budgetModel: 'RULE_50_30_20',
    useFromPrevious: false,
    customPercentages: {
      needs: 50,
      wants: 30,
      savings: 20
    }
  })

  const budgetModels = [
    {
      key: 'RULE_50_30_20',
      name: t('budget.models.rule50_30_20'),
      description: t('budget.models.rule50_30_20_desc'),
      percentages: { needs: 50, wants: 30, savings: 20 },
      icon: <PieChart className="w-5 h-5" />
    },
    {
      key: 'RULE_60_20_20',
      name: t('budget.models.rule60_20_20'),
      description: t('budget.models.rule60_20_20_desc'),
      percentages: { needs: 60, wants: 20, savings: 20 },
      icon: <TrendingUp className="w-5 h-5" />
    },
    {
      key: 'RULE_80_20',
      name: t('budget.models.rule80_20'),
      description: t('budget.models.rule80_20_desc'),
      percentages: { expenses: 80, savings: 20 },
      icon: <Settings className="w-5 h-5" />
    },
    {
      key: 'FRENCH_THIRDS',
      name: t('budget.models.frenchThirds'),
      description: t('budget.models.frenchThirds_desc'),
      percentages: { housing: 33.33, living: 33.33, savings: 33.34 },
      icon: <PieChart className="w-5 h-5" />
    },
    {
      key: 'CUSTOM',
      name: t('budget.models.custom'),
      description: t('budget.models.custom_desc'),
      percentages: {},
      icon: <Settings className="w-5 h-5" />
    }
  ]

  const months = [
    { value: 1, label: t('months.january') },
    { value: 2, label: t('months.february') },
    { value: 3, label: t('months.march') },
    { value: 4, label: t('months.april') },
    { value: 5, label: t('months.may') },
    { value: 6, label: t('months.june') },
    { value: 7, label: t('months.july') },
    { value: 8, label: t('months.august') },
    { value: 9, label: t('months.september') },
    { value: 10, label: t('months.october') },
    { value: 11, label: t('months.november') },
    { value: 12, label: t('months.december') }
  ]

  const handleNext = () => {
    if (step < 4) setStep(step + 1)
  }

  const handlePrevious = () => {
    if (step > 1) setStep(step - 1)
  }

  const handleComplete = () => {
    onComplete(budgetData)
  }

  const updateBudgetData = (field: string, value: any) => {
    setBudgetData(prev => ({ ...prev, [field]: value }))
  }

  const updateCustomPercentage = (category: string, value: number) => {
    setBudgetData(prev => ({
      ...prev,
      customPercentages: {
        ...prev.customPercentages,
        [category]: value
      }
    }))
  }

  const selectedModel = budgetModels.find(model => model.key === budgetData.budgetModel)

  const renderStep1 = () => (
    <div className="space-y-6">
      <div className="text-center">
        <h2 className="text-2xl font-bold">{t('budget.wizard.step1.title')}</h2>
        <p className="text-gray-600 mt-2">{t('budget.wizard.step1.description')}</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <Label htmlFor="month">{t('budget.wizard.month')}</Label>
          <Select value={budgetData.month.toString()} onValueChange={(value) => updateBudgetData('month', parseInt(value))}>
            <SelectTrigger>
              <SelectValue 
                placeholder={t('budget.wizard.selectMonth')}
                selectedValue={months.find(m => m.value === budgetData.month)?.label}
              />
            </SelectTrigger>
            <SelectContent>
              {months.map(month => (
                <SelectItem key={month.value} value={month.value.toString()}>
                  {month.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div>
          <Label htmlFor="year">{t('budget.wizard.year')}</Label>
          <Input
            id="year"
            type="number"
            value={budgetData.year}
            onChange={(e) => updateBudgetData('year', parseInt(e.target.value))}
            min="2020"
            max="2030"
          />
        </div>
      </div>

      {previousMonthIncome > 0 && (
        <Card className="bg-blue-50 border-blue-200">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <h4 className="font-medium">{t('budget.wizard.useFromPrevious')}</h4>
                <p className="text-sm text-gray-600">
                  {t('budget.wizard.previousIncome')}: {formatCurrencyI18n(previousMonthIncome)}
                </p>
              </div>
              <Switch
                checked={budgetData.useFromPrevious}
                onCheckedChange={(checked) => {
                  updateBudgetData('useFromPrevious', checked)
                  if (checked) {
                    updateBudgetData('projectedIncome', previousMonthIncome)
                  }
                }}
              />
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )

  const renderStep2 = () => (
    <div className="space-y-6">
      <div className="text-center">
        <h2 className="text-2xl font-bold">{t('budget.wizard.step2.title')}</h2>
        <p className="text-gray-600 mt-2">{t('budget.wizard.step2.description')}</p>
      </div>

      <div>
        <Label htmlFor="income">{t('budget.wizard.projectedIncome')}</Label>
        <Input
          id="income"
          type="number"
          value={budgetData.projectedIncome}
          onChange={(e) => updateBudgetData('projectedIncome', parseFloat(e.target.value) || 0)}
          step="0.01"
          min="0"
          className="text-lg"
        />
        <p className="text-sm text-gray-600 mt-1">
          {t('budget.wizard.incomeHelp')}
        </p>
      </div>

      <div className="bg-green-50 border border-green-200 rounded-lg p-4">
        <h4 className="font-medium text-green-800 mb-2">
          {t('budget.wizard.incomePreview')}
        </h4>
        <div className="text-2xl font-bold text-green-600">
          {formatCurrencyI18n(budgetData.projectedIncome)}
        </div>
      </div>
    </div>
  )

  const renderStep3 = () => (
    <div className="space-y-6">
      <div className="text-center">
        <h2 className="text-2xl font-bold">{t('budget.wizard.step3.title')}</h2>
        <p className="text-gray-600 mt-2">{t('budget.wizard.step3.description')}</p>
      </div>

      <div className="grid grid-cols-1 gap-4">
        {budgetModels.map(model => (
          <Card 
            key={model.key}
            className={`cursor-pointer transition-colors ${
              budgetData.budgetModel === model.key 
                ? 'border-blue-500 bg-blue-50' 
                : 'hover:border-gray-300'
            }`}
            onClick={() => updateBudgetData('budgetModel', model.key)}
          >
            <CardContent className="p-4">
              <div className="flex items-center space-x-4">
                <div className={`p-2 rounded-lg ${
                  budgetData.budgetModel === model.key ? 'bg-blue-100 text-blue-600' : 'bg-gray-100'
                }`}>
                  {model.icon}
                </div>
                <div className="flex-1">
                  <h4 className="font-medium">{model.name}</h4>
                  <p className="text-sm text-gray-600">{model.description}</p>
                  {Object.keys(model.percentages).length > 0 && (
                    <div className="flex space-x-4 mt-2">
                      {Object.entries(model.percentages).map(([key, value]) => (
                        <span key={key} className="text-xs bg-gray-200 px-2 py-1 rounded">
                          {t(`budget.categories.${key}`)}: {value}%
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )

  const renderStep4 = () => {
    if (budgetData.budgetModel === 'CUSTOM') {
      return (
        <div className="space-y-6">
          <div className="text-center">
            <h2 className="text-2xl font-bold">{t('budget.wizard.step4.customTitle')}</h2>
            <p className="text-gray-600 mt-2">{t('budget.wizard.step4.customDescription')}</p>
          </div>

          <div className="space-y-6">
            {Object.entries(budgetData.customPercentages).map(([category, percentage]) => (
              <div key={category} className="space-y-2">
                <div className="flex justify-between items-center">
                  <Label>{t(`budget.categories.${category}`)}</Label>
                  <span className="text-sm font-medium">{percentage}%</span>
                </div>
                <Slider
                  value={[percentage]}
                  onValueChange={(values) => updateCustomPercentage(category, values[0])}
                  max={100}
                  step={1}
                  className="w-full"
                />
                <div className="text-sm text-gray-600">
                  {formatCurrencyI18n((budgetData.projectedIncome * percentage) / 100)}
                </div>
              </div>
            ))}

            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
              <p className="text-yellow-800 text-sm">
                {t('budget.wizard.totalPercentage')}: {Object.values(budgetData.customPercentages).reduce((a, b) => a + b, 0)}%
              </p>
            </div>
          </div>
        </div>
      )
    }

    return (
      <div className="space-y-6">
        <div className="text-center">
          <h2 className="text-2xl font-bold">{t('budget.wizard.step4.title')}</h2>
          <p className="text-gray-600 mt-2">{t('budget.wizard.step4.description')}</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              {selectedModel?.icon}
              <span>{selectedModel?.name}</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="bg-gray-50 rounded-lg p-4">
              <h4 className="font-medium mb-3">{t('budget.wizard.budgetBreakdown')}</h4>
              <div className="space-y-2">
                {selectedModel && Object.entries(selectedModel.percentages).map(([category, percentage]) => (
                  <div key={category} className="flex justify-between">
                    <span>{t(`budget.categories.${category}`)}</span>
                    <div className="text-right">
                      <span className="font-medium">{percentage}%</span>
                      <div className="text-sm text-gray-600">
                        {formatCurrencyI18n((budgetData.projectedIncome * percentage) / 100)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="border-t pt-4">
              <div className="flex justify-between font-medium">
                <span>{t('budget.wizard.totalIncome')}</span>
                <span>{formatCurrencyI18n(budgetData.projectedIncome)}</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <Card className="w-full max-w-2xl mx-auto">
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>{t('budget.wizard.title')}</CardTitle>
          <div className="text-sm text-gray-500">
            {t('budget.wizard.stepOf', { current: step, total: 4 })}
          </div>
        </div>
        
        {/* Progress bar */}
        <div className="w-full bg-gray-200 rounded-full h-2">
          <div 
            className="bg-blue-600 h-2 rounded-full transition-all duration-300"
            style={{ width: `${(step / 4) * 100}%` }}
          />
        </div>
      </CardHeader>

      <CardContent className="min-h-[400px]">
        {step === 1 && renderStep1()}
        {step === 2 && renderStep2()}
        {step === 3 && renderStep3()}
        {step === 4 && renderStep4()}
      </CardContent>

      <div className="flex justify-between p-6 border-t">
        <Button variant="outline" onClick={step === 1 ? onCancel : handlePrevious}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          {step === 1 ? t('common.cancel') : t('common.previous')}
        </Button>
        
        {step === 4 ? (
          <Button onClick={handleComplete}>
            <Check className="w-4 h-4 mr-2" />
            {t('budget.wizard.createBudget')}
          </Button>
        ) : (
          <Button onClick={handleNext}>
            {t('common.next')}
            <ArrowRight className="w-4 h-4 ml-2" />
          </Button>
        )}
      </div>
    </Card>
  )
}