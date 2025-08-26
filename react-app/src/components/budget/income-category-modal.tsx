import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { apiClient } from '@/lib/api'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { 
  DollarSign, 
  Briefcase, 
  Building, 
  TrendingUp, 
  Save, 
  X,
  Palette,
  Sparkles 
} from 'lucide-react'
import { IncomeCategory, IncomeType } from '@/types/api'

// Validation schema
const incomeCategorySchema = z.object({
  name: z.string().min(1, 'Le nom est requis').max(50, 'Le nom ne peut pas dépasser 50 caractères'),
  description: z.string().max(200, 'La description ne peut pas dépasser 200 caractères').optional(),
  budgetedAmount: z.number().min(0, 'Le montant budgété doit être positif').max(999999, 'Montant trop élevé'),
  incomeType: z.enum(['SALARY', 'BUSINESS', 'INVESTMENT', 'OTHER']),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Couleur invalide'),
  icon: z.string().min(1, 'Une icône est requise')
})

type FormData = z.infer<typeof incomeCategorySchema>

interface IncomeCategoryModalProps {
  budgetId: number
  category?: IncomeCategory | null
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

// Predefined colors for income categories
const INCOME_COLORS = [
  { name: 'Vert', value: '#22C55E', description: 'Revenus positifs' },
  { name: 'Bleu', value: '#3B82F6', description: 'Revenus professionnels' },
  { name: 'Violet', value: '#8B5CF6', description: 'Investissements' },
  { name: 'Indigo', value: '#6366F1', description: 'Revenus passifs' },
  { name: 'Emeraude', value: '#10B981', description: 'Salaires' },
  { name: 'Cyan', value: '#06B6D4', description: 'Business' },
  { name: 'Orange', value: '#F97316', description: 'Projets' },
  { name: 'Rose', value: '#EC4899', description: 'Bonus' }
]

// Predefined icons for income categories
const INCOME_ICONS = [
  { name: 'dollar-sign', icon: DollarSign, label: 'Dollar' },
  { name: 'briefcase', icon: Briefcase, label: 'Porte-documents' },
  { name: 'building', icon: Building, label: 'Bâtiment' },
  { name: 'trending-up', icon: TrendingUp, label: 'Tendance croissante' }
]

export default function IncomeCategoryModal({ 
  budgetId, 
  category, 
  isOpen, 
  onClose, 
  onSuccess 
}: IncomeCategoryModalProps) {
  const [isLoading, setIsLoading] = useState(false)
  const [suggestionsLoading, setSuggestionsLoading] = useState(false)
  const [suggestions, setSuggestions] = useState<string[]>([])
  const isEditing = !!category

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors, isValid }
  } = useForm<FormData>({
    resolver: zodResolver(incomeCategorySchema),
    defaultValues: {
      name: '',
      description: '',
      budgetedAmount: 0,
      incomeType: 'OTHER',
      color: '#22C55E',
      icon: 'dollar-sign'
    }
  })

  const selectedType = watch('incomeType')
  const selectedColor = watch('color')
  const selectedIcon = watch('icon')

  // Load form data when editing
  useEffect(() => {
    if (category && isOpen) {
      reset({
        name: category.name,
        description: category.description || '',
        budgetedAmount: category.budgetedAmount,
        incomeType: category.incomeType,
        color: category.color,
        icon: category.icon
      })
    } else if (!category && isOpen) {
      // Reset form for new category
      reset({
        name: '',
        description: '',
        budgetedAmount: 0,
        incomeType: 'OTHER',
        color: '#22C55E',
        icon: 'dollar-sign'
      })
    }
  }, [category, isOpen, reset])

  // Auto-suggest based on income type
  useEffect(() => {
    const defaultsByType: Record<IncomeType, { color: string; icon: string; suggestions: string[] }> = {
      'SALARY': {
        color: '#22C55E',
        icon: 'briefcase',
        suggestions: ['Salaire principal', 'Salaire temps partiel', 'Prime annuelle', 'Heures supplémentaires']
      },
      'BUSINESS': {
        color: '#3B82F6',
        icon: 'building',
        suggestions: ['Chiffre d\'affaires', 'Prestations clients', 'Ventes produits', 'Services freelance']
      },
      'INVESTMENT': {
        color: '#8B5CF6',
        icon: 'trending-up',
        suggestions: ['Dividendes actions', 'Intérêts livret', 'Plus-values', 'Revenus locatifs']
      },
      'OTHER': {
        color: '#6B7280',
        icon: 'dollar-sign',
        suggestions: ['Allocations', 'Remboursements', 'Cadeaux argent', 'Vente occasionnelle']
      }
    }

    const defaults = defaultsByType[selectedType]
    if (defaults && !isEditing) {
      setValue('color', defaults.color)
      setValue('icon', defaults.icon)
      setSuggestions(defaults.suggestions)
    }
  }, [selectedType, setValue, isEditing])

  // Smart suggestions for name
  const handleSmartSuggest = async () => {
    if (!watch('name')) return
    
    setSuggestionsLoading(true)
    try {
      // This would use the smart suggestion API
      // For now, we'll use the predefined suggestions
      const defaults = {
        'SALARY': ['Salaire ' + watch('name'), 'Prime ' + watch('name')],
        'BUSINESS': ['CA ' + watch('name'), 'Revenus ' + watch('name')],
        'INVESTMENT': ['Dividendes ' + watch('name'), 'Intérêts ' + watch('name')],
        'OTHER': ['Revenus ' + watch('name'), 'Entrées ' + watch('name')]
      }
      
      setSuggestions(defaults[selectedType] || [])
    } catch (error) {
      console.error('Error getting suggestions:', error)
    } finally {
      setSuggestionsLoading(false)
    }
  }

  const onSubmit = async (data: FormData) => {
    setIsLoading(true)
    try {
      if (isEditing && category) {
        await apiClient.updateIncomeCategory(category.id, data)
      } else {
        await apiClient.createIncomeCategory(budgetId, data)
      }
      
      onSuccess()
      onClose()
    } catch (error: any) {
      console.error('Error saving income category:', error)
      // TODO: Show error toast
    } finally {
      setIsLoading(false)
    }
  }

  const handleClose = () => {
    if (!isLoading) {
      onClose()
    }
  }

  const getIncomeTypeLabel = (type: IncomeType) => {
    const labels = {
      'SALARY': 'Salaire',
      'BUSINESS': 'Entreprise',
      'INVESTMENT': 'Investissement', 
      'OTHER': 'Autre'
    }
    return labels[type]
  }

  const getIncomeTypeIcon = (type: IncomeType) => {
    const icons = {
      'SALARY': Briefcase,
      'BUSINESS': Building,
      'INVESTMENT': TrendingUp,
      'OTHER': DollarSign
    }
    return icons[type]
  }

  if (!isOpen) return null

  const IconComponent = INCOME_ICONS.find(i => i.name === selectedIcon)?.icon || DollarSign

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <DialogTitle className="flex items-center space-x-2">
              <div 
                className="w-8 h-8 rounded-full flex items-center justify-center"
                style={{ backgroundColor: selectedColor + '20', color: selectedColor }}
              >
                <IconComponent className="w-4 h-4" />
              </div>
              <span>
                {isEditing ? 'Modifier la catégorie' : 'Nouvelle catégorie de revenus'}
              </span>
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
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Basic Information */}
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="name">Nom de la catégorie *</Label>
                <div className="flex space-x-2">
                  <Input
                    id="name"
                    {...register('name')}
                    placeholder="ex: Salaire principal"
                    className={errors.name ? 'border-red-500' : ''}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleSmartSuggest}
                    disabled={suggestionsLoading || !watch('name')}
                  >
                    <Sparkles className="w-4 h-4" />
                  </Button>
                </div>
                {errors.name && (
                  <p className="text-sm text-red-600 mt-1">{errors.name.message}</p>
                )}
              </div>

              <div>
                <Label htmlFor="budgetedAmount">Montant budgété *</Label>
                <div className="relative">
                  <Input
                    id="budgetedAmount"
                    type="number"
                    step="0.01"
                    {...register('budgetedAmount', { valueAsNumber: true })}
                    placeholder="0.00"
                    className={`pr-8 ${errors.budgetedAmount ? 'border-red-500' : ''}`}
                  />
                  <span className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 text-sm">
                    €
                  </span>
                </div>
                {errors.budgetedAmount && (
                  <p className="text-sm text-red-600 mt-1">{errors.budgetedAmount.message}</p>
                )}
              </div>
            </div>

            <div>
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                {...register('description')}
                placeholder="Description optionnelle de cette source de revenus..."
                rows={3}
                className={errors.description ? 'border-red-500' : ''}
              />
              {errors.description && (
                <p className="text-sm text-red-600 mt-1">{errors.description.message}</p>
              )}
            </div>
          </div>

          {/* Smart Suggestions */}
          {suggestions.length > 0 && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <h4 className="font-medium text-blue-900 mb-2">Suggestions intelligentes</h4>
              <div className="flex flex-wrap gap-2">
                {suggestions.map((suggestion, index) => (
                  <Button
                    key={index}
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setValue('name', suggestion)}
                    className="text-blue-700 border-blue-300 hover:bg-blue-100"
                  >
                    {suggestion}
                  </Button>
                ))}
              </div>
            </div>
          )}

          {/* Income Type Selection */}
          <div>
            <Label>Type de revenus *</Label>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-2">
              {(['SALARY', 'BUSINESS', 'INVESTMENT', 'OTHER'] as IncomeType[]).map((type) => {
                const Icon = getIncomeTypeIcon(type)
                const isSelected = selectedType === type
                return (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setValue('incomeType', type)}
                    className={`p-3 rounded-lg border-2 transition-all ${
                      isSelected
                        ? 'border-blue-500 bg-blue-50 text-blue-700'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <Icon className="w-5 h-5 mx-auto mb-2" />
                    <div className="text-sm font-medium">{getIncomeTypeLabel(type)}</div>
                  </button>
                )
              })}
            </div>
          </div>

          {/* Color Selection */}
          <div>
            <Label className="flex items-center space-x-2">
              <Palette className="w-4 h-4" />
              <span>Couleur</span>
            </Label>
            <div className="grid grid-cols-4 md:grid-cols-8 gap-2 mt-2">
              {INCOME_COLORS.map((color) => (
                <button
                  key={color.value}
                  type="button"
                  onClick={() => setValue('color', color.value)}
                  className={`w-10 h-10 rounded-full border-2 transition-all ${
                    selectedColor === color.value
                      ? 'border-gray-800 scale-110'
                      : 'border-gray-300 hover:scale-105'
                  }`}
                  style={{ backgroundColor: color.value }}
                  title={`${color.name} - ${color.description}`}
                />
              ))}
            </div>
          </div>

          {/* Icon Selection */}
          <div>
            <Label>Icône</Label>
            <div className="grid grid-cols-4 gap-3 mt-2">
              {INCOME_ICONS.map((iconData) => {
                const Icon = iconData.icon
                const isSelected = selectedIcon === iconData.name
                return (
                  <button
                    key={iconData.name}
                    type="button"
                    onClick={() => setValue('icon', iconData.name)}
                    className={`p-3 rounded-lg border-2 transition-all ${
                      isSelected
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <Icon 
                      className="w-5 h-5 mx-auto mb-1" 
                      style={{ color: isSelected ? selectedColor : undefined }}
                    />
                    <div className="text-xs">{iconData.label}</div>
                  </button>
                )
              })}
            </div>
          </div>

          {/* Preview */}
          <div className="bg-gray-50 border rounded-lg p-4">
            <h4 className="font-medium text-gray-700 mb-3">Aperçu</h4>
            <div className="flex items-center space-x-3">
              <div 
                className="w-10 h-10 rounded-full flex items-center justify-center"
                style={{ backgroundColor: selectedColor + '20', color: selectedColor }}
              >
                <IconComponent className="w-5 h-5" />
              </div>
              <div className="flex-1">
                <div className="font-medium">
                  {watch('name') || 'Nom de la catégorie'}
                </div>
                <div className="text-sm text-gray-600">
                  {getIncomeTypeLabel(selectedType)} • {watch('budgetedAmount')}€ budgété
                </div>
                {watch('description') && (
                  <div className="text-sm text-gray-500 mt-1">
                    {watch('description')}
                  </div>
                )}
              </div>
            </div>
          </div>

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
                  <span>{isEditing ? 'Modifier' : 'Créer la catégorie'}</span>
                </div>
              )}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}