import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { format } from 'date-fns'
import { apiClient } from '@/lib/api'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/advanced-select'
import { Switch } from '@/components/ui/switch'
import { 
  Save, 
  X, 
  Repeat,
  DollarSign,
  Sparkles,
  TrendingUp,
  AlertCircle
} from 'lucide-react'
import { IncomeTransaction, IncomeCategory, RecurrenceType } from '@/types/api'

// Validation schema
const incomeTransactionSchema = z.object({
  description: z.string().min(1, 'La description est requise').max(100, 'Description trop longue'),
  amount: z.number().min(0.01, 'Le montant doit être supérieur à 0').max(999999, 'Montant trop élevé'),
  transactionDate: z.date({ required_error: 'La date est requise' }),
  notes: z.string().max(500, 'Notes trop longues').optional(),
  recurrenceType: z.enum(['ONE_TIME', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY']),
  isRecurring: z.boolean(),
  incomeCategoryId: z.number().min(1, 'Une catégorie est requise')
})

type FormData = z.infer<typeof incomeTransactionSchema>

interface IncomeTransactionModalProps {
  budgetId: number
  transaction?: IncomeTransaction | null
  incomeCategories: IncomeCategory[]
  defaultCategoryId?: number
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

// Recurrence type options
const RECURRENCE_OPTIONS: Array<{ value: RecurrenceType; label: string; description: string }> = [
  { value: 'ONE_TIME', label: 'Une fois', description: 'Transaction unique' },
  { value: 'WEEKLY', label: 'Hebdomadaire', description: 'Chaque semaine' },
  { value: 'BIWEEKLY', label: 'Bimensuel', description: 'Toutes les 2 semaines' },
  { value: 'MONTHLY', label: 'Mensuel', description: 'Chaque mois' },
  { value: 'QUARTERLY', label: 'Trimestriel', description: 'Tous les 3 mois' },
  { value: 'YEARLY', label: 'Annuel', description: 'Chaque année' }
]

export default function IncomeTransactionModal({
  transaction,
  incomeCategories,
  defaultCategoryId,
  isOpen,
  onClose,
  onSuccess
}: IncomeTransactionModalProps) {
  const [isLoading, setIsLoading] = useState(false)
  const [suggestionsLoading, setSuggestionsLoading] = useState(false)
  const [suggestions, setSuggestions] = useState<IncomeCategory | null>(null)
  const isEditing = !!transaction

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors, isValid }
  } = useForm<FormData>({
    resolver: zodResolver(incomeTransactionSchema),
    defaultValues: {
      description: '',
      amount: 0,
      transactionDate: new Date(),
      notes: '',
      recurrenceType: 'ONE_TIME',
      isRecurring: false,
      incomeCategoryId: defaultCategoryId || 0
    }
  })

  const selectedCategoryId = watch('incomeCategoryId')
  const selectedRecurrenceType = watch('recurrenceType')
  const isRecurring = watch('isRecurring')
  const transactionDate = watch('transactionDate')

  // Load form data when editing
  useEffect(() => {
    if (transaction && isOpen) {
      reset({
        description: transaction.description,
        amount: transaction.amount,
        transactionDate: new Date(transaction.transactionDate),
        notes: transaction.notes || '',
        recurrenceType: transaction.recurrenceType,
        isRecurring: transaction.isRecurring,
        incomeCategoryId: transaction.incomeCategoryId
      })
    } else if (!transaction && isOpen) {
      // Reset form for new transaction
      reset({
        description: '',
        amount: 0,
        transactionDate: new Date(),
        notes: '',
        recurrenceType: 'ONE_TIME',
        isRecurring: false,
        incomeCategoryId: defaultCategoryId || (incomeCategories[0]?.id || 0)
      })
    }
  }, [transaction, isOpen, reset, defaultCategoryId, incomeCategories])

  // Auto-update recurrence when switch changes
  useEffect(() => {
    if (isRecurring && selectedRecurrenceType === 'ONE_TIME') {
      setValue('recurrenceType', 'MONTHLY')
    } else if (!isRecurring) {
      setValue('recurrenceType', 'ONE_TIME')
    }
  }, [isRecurring, selectedRecurrenceType, setValue])

  // Smart suggestions for income categorization
  const handleSmartSuggest = async () => {
    const description = watch('description')
    if (!description.trim()) return
    
    setSuggestionsLoading(true)
    try {
      const suggestion = await apiClient.suggestIncomeCategory(description)
      setSuggestions(suggestion)
    } catch (error) {
      console.error('Error getting income suggestions:', error)
      setSuggestions(null)
    } finally {
      setSuggestionsLoading(false)
    }
  }

  const applySuggestion = () => {
    if (suggestions) {
      setValue('incomeCategoryId', suggestions.id)
      setSuggestions(null)
    }
  }

  const onSubmit = async (data: FormData) => {
    setIsLoading(true)
    try {
      const transactionData = {
        ...data,
        transactionDate: format(data.transactionDate, 'yyyy-MM-dd'),
        source: 'Manual Entry'
      }

      if (isEditing && transaction) {
        await apiClient.updateIncomeTransaction(transaction.id, transactionData)
      } else {
        await apiClient.createIncomeTransaction(data.incomeCategoryId, transactionData)
      }

      // Submit feedback for smart assignment learning
      if (suggestions && suggestions.id !== data.incomeCategoryId) {
        try {
          await apiClient.submitIncomeFeedback({
            description: data.description,
            suggestedCategoryId: suggestions.id,
            chosenCategoryId: data.incomeCategoryId
          })
        } catch (feedbackError) {
          console.error('Error submitting feedback:', feedbackError)
          // Don't fail the transaction for feedback errors
        }
      }

      onSuccess()
      onClose()
    } catch (error: any) {
      console.error('Error saving income transaction:', error)
      // TODO: Show error toast
    } finally {
      setIsLoading(false)
    }
  }

  const handleClose = () => {
    if (!isLoading) {
      setSuggestions(null)
      onClose()
    }
  }

  const getSelectedCategory = () => {
    return incomeCategories.find(cat => cat.id === selectedCategoryId)
  }

  const getRecurrenceDescription = (type: RecurrenceType) => {
    return RECURRENCE_OPTIONS.find(opt => opt.value === type)?.description || ''
  }

  if (!isOpen) return null

  const selectedCategory = getSelectedCategory()

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <DialogTitle className="flex items-center space-x-2">
              <div className="flex items-center space-x-3">
                {selectedCategory && (
                  <div 
                    className="w-8 h-8 rounded-full flex items-center justify-center"
                    style={{ 
                      backgroundColor: selectedCategory.color + '20', 
                      color: selectedCategory.color 
                    }}
                  >
                    <DollarSign className="w-4 h-4" />
                  </div>
                )}
                <span>
                  {isEditing ? 'Modifier le revenu' : 'Nouveau revenu'}
                </span>
              </div>
            </DialogTitle>
            <Button 
              variant="ghost" 
              size="sm" 
              onClick={handleClose}
              disabled={isLoading}
            >
              <X className="w-4 h-4" />
            </Button>
          </div>
          {selectedCategory && (
            <p className="text-sm text-gray-600">
              Catégorie: {selectedCategory.name}
            </p>
          )}
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Basic Information */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="description">Description *</Label>
              <div className="flex space-x-2">
                <Input
                  id="description"
                  {...register('description')}
                  placeholder="ex: Salaire janvier, Prime projet..."
                  className={errors.description ? 'border-red-500' : ''}
                />
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleSmartSuggest}
                  disabled={suggestionsLoading || !watch('description')}
                >
                  {suggestionsLoading ? (
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                  ) : (
                    <Sparkles className="w-4 h-4" />
                  )}
                </Button>
              </div>
              {errors.description && (
                <p className="text-sm text-red-600 mt-1">{errors.description.message}</p>
              )}
            </div>

            <div>
              <Label htmlFor="amount">Montant *</Label>
              <div className="relative">
                <Input
                  id="amount"
                  type="number"
                  step="0.01"
                  {...register('amount', { valueAsNumber: true })}
                  placeholder="0.00"
                  className={`pr-8 ${errors.amount ? 'border-red-500' : ''}`}
                />
                <span className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 text-sm">
                  €
                </span>
              </div>
              {errors.amount && (
                <p className="text-sm text-red-600 mt-1">{errors.amount.message}</p>
              )}
            </div>
          </div>

          {/* Smart Suggestion */}
          {suggestions && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4">
              <div className="flex items-start space-x-3">
                <TrendingUp className="w-5 h-5 text-green-600 mt-0.5" />
                <div className="flex-1">
                  <h4 className="font-medium text-green-900 mb-1">Suggestion intelligente</h4>
                  <p className="text-sm text-green-800 mb-3">
                    Basé sur vos revenus précédents, cette transaction pourrait appartenir à la catégorie{' '}
                    <strong>{suggestions.name}</strong>.
                  </p>
                  <div className="flex justify-end space-x-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setSuggestions(null)}
                    >
                      Ignorer
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      onClick={applySuggestion}
                    >
                      Appliquer
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Category and Date */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="incomeCategoryId">Catégorie de revenus *</Label>
              <Select 
                value={selectedCategoryId?.toString()} 
                onValueChange={(value) => setValue('incomeCategoryId', parseInt(value))}
              >
                <SelectTrigger className={errors.incomeCategoryId ? 'border-red-500' : ''}>
                  <SelectValue placeholder="Sélectionner une catégorie" />
                </SelectTrigger>
                <SelectContent>
                  {incomeCategories.map((category) => (
                    <SelectItem key={category.id} value={category.id.toString()}>
                      <div className="flex items-center space-x-2">
                        <div 
                          className="w-3 h-3 rounded-full"
                          style={{ backgroundColor: category.color }}
                        />
                        <span>{category.name}</span>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.incomeCategoryId && (
                <p className="text-sm text-red-600 mt-1">Une catégorie est requise</p>
              )}
            </div>

            <div>
              <Label htmlFor="transactionDate">Date de transaction *</Label>
              <Input
                id="transactionDate"
                type="date"
                {...register('transactionDate', { 
                  valueAsDate: true,
                  setValueAs: (value) => value ? new Date(value) : undefined
                })}
                className={errors.transactionDate ? 'border-red-500' : ''}
              />
              {errors.transactionDate && (
                <p className="text-sm text-red-600 mt-1">La date est requise</p>
              )}
            </div>
          </div>

          {/* Recurrence Settings */}
          <div className="space-y-4">
            <div className="flex items-center space-x-3">
              <Switch
                checked={isRecurring}
                onCheckedChange={(checked) => setValue('isRecurring', checked)}
              />
              <Label className="flex items-center space-x-2 cursor-pointer">
                <Repeat className="w-4 h-4" />
                <span>Revenu récurrent</span>
              </Label>
            </div>

            {isRecurring && (
              <div>
                <Label>Fréquence</Label>
                <Select 
                  value={selectedRecurrenceType} 
                  onValueChange={(value: string) => setValue('recurrenceType', value as RecurrenceType)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {RECURRENCE_OPTIONS.filter(opt => opt.value !== 'ONE_TIME').map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        <div className="space-y-1">
                          <div className="font-medium">{option.label}</div>
                          <div className="text-xs text-gray-500">{option.description}</div>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-sm text-gray-500 mt-1">
                  {getRecurrenceDescription(selectedRecurrenceType)}
                </p>
              </div>
            )}
          </div>

          {/* Notes */}
          <div>
            <Label htmlFor="notes">Notes (optionnel)</Label>
            <Textarea
              id="notes"
              {...register('notes')}
              placeholder="Notes additionnelles sur ce revenu..."
              rows={3}
              className={errors.notes ? 'border-red-500' : ''}
            />
            {errors.notes && (
              <p className="text-sm text-red-600 mt-1">{errors.notes.message}</p>
            )}
          </div>

          {/* Preview */}
          <div className="bg-gray-50 border rounded-lg p-4">
            <h4 className="font-medium text-gray-700 mb-3">Aperçu</h4>
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                {selectedCategory && (
                  <div 
                    className="w-10 h-10 rounded-full flex items-center justify-center"
                    style={{ 
                      backgroundColor: selectedCategory.color + '20', 
                      color: selectedCategory.color 
                    }}
                  >
                    <DollarSign className="w-5 h-5" />
                  </div>
                )}
                <div>
                  <div className="font-medium">
                    {watch('description') || 'Description du revenu'}
                  </div>
                  <div className="text-sm text-gray-600">
                    {selectedCategory?.name || 'Catégorie'} • {' '}
                    {transactionDate ? format(transactionDate, 'dd/MM/yyyy') : 'Date'}
                    {isRecurring && (
                      <span className="ml-2 text-blue-600">
                        • {RECURRENCE_OPTIONS.find(opt => opt.value === selectedRecurrenceType)?.label}
                      </span>
                    )}
                  </div>
                  {watch('notes') && (
                    <div className="text-sm text-gray-500 mt-1">
                      {watch('notes')}
                    </div>
                  )}
                </div>
              </div>
              <div className="text-right">
                <div className="text-xl font-bold text-green-600">
                  +{watch('amount') || 0}€
                </div>
                {isRecurring && (
                  <div className="text-sm text-gray-500">
                    Récurrent
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Warning for past dates */}
          {transactionDate && transactionDate < new Date() && (
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3">
              <div className="flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-amber-600" />
                <span className="text-sm text-amber-800">
                  Cette transaction est dans le passé. Elle sera comptabilisée dans les statistiques historiques.
                </span>
              </div>
            </div>
          )}

          {/* Form Actions */}
          <div className="flex justify-end space-x-3 pt-4 border-t">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={isLoading}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              disabled={isLoading || !isValid}
            >
              {isLoading ? (
                <div className="flex items-center space-x-2">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                  <span>{isEditing ? 'Modification...' : 'Création...'}</span>
                </div>
              ) : (
                <div className="flex items-center space-x-2">
                  <Save className="w-4 h-4" />
                  <span>{isEditing ? 'Modifier' : 'Créer le revenu'}</span>
                </div>
              )}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}