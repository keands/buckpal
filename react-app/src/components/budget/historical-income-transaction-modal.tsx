import { useState, useEffect } from 'react'
import { apiClient } from '@/lib/api'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Checkbox } from '@/components/ui/checkbox'
import { 
  Search, 
  X, 
  Calendar,
  DollarSign,
  Check,
  AlertCircle,
  Sparkles
} from 'lucide-react'
import { IncomeCategory } from '@/types/api'

interface HistoricalTransaction {
  id: number
  description: string
  amount: number
  transactionDate: string
  transactionType: 'INCOME' | 'EXPENSE' | 'TRANSFER'
  accountName?: string
  categoryName?: string
}

interface HistoricalIncomeTransactionModalProps {
  incomeCategory: IncomeCategory
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function HistoricalIncomeTransactionModal({
  incomeCategory,
  isOpen,
  onClose,
  onSuccess
}: HistoricalIncomeTransactionModalProps) {
  const [availableTransactions, setAvailableTransactions] = useState<HistoricalTransaction[]>([])
  const [suggestedTransactions, setSuggestedTransactions] = useState<HistoricalTransaction[]>([])
  const [selectedTransactionIds, setSelectedTransactionIds] = useState<number[]>([])
  const [searchQuery, setSearchQuery] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showSuggestions, setShowSuggestions] = useState(true)

  // Load transactions when modal opens
  useEffect(() => {
    if (isOpen && incomeCategory) {
      loadTransactions()
    }
  }, [isOpen, incomeCategory])

  const loadTransactions = async () => {
    setIsLoading(true)
    try {
      // Load suggested transactions for this category
      const suggested = await apiClient.getSuggestedTransactionsForCategory(incomeCategory.id)
      setSuggestedTransactions(suggested)

      // Load all available income transactions
      const available = await apiClient.getAvailableIncomeTransactions()
      setAvailableTransactions(available)
      
      setShowSuggestions(suggested.length > 0)
    } catch (error) {
      console.error('Error loading transactions:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleTransactionToggle = (transactionId: number) => {
    setSelectedTransactionIds(prev => 
      prev.includes(transactionId)
        ? prev.filter(id => id !== transactionId)
        : [...prev, transactionId]
    )
  }

  const handleSelectAllSuggested = () => {
    const suggestedIds = suggestedTransactions.map(t => t.id)
    setSelectedTransactionIds(prev => {
      const newIds = [...prev]
      suggestedIds.forEach(id => {
        if (!newIds.includes(id)) {
          newIds.push(id)
        }
      })
      return newIds
    })
  }

  const handleSubmit = async () => {
    if (selectedTransactionIds.length === 0) return

    setIsSubmitting(true)
    try {
      await apiClient.linkHistoricalTransactions(incomeCategory.id, selectedTransactionIds)
      onSuccess()
      onClose()
      
      // Reset state
      setSelectedTransactionIds([])
      setSearchQuery('')
    } catch (error) {
      console.error('Error linking transactions:', error)
      // TODO: Show error toast
    } finally {
      setIsSubmitting(false)
    }
  }

  const filteredTransactions = availableTransactions.filter(transaction =>
    transaction.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
    transaction.accountName?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const getSelectedTotal = () => {
    return availableTransactions
      .filter(t => selectedTransactionIds.includes(t.id))
      .reduce((sum, t) => sum + t.amount, 0)
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount)
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('fr-FR')
  }

  if (!isOpen) return null

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <DialogTitle className="flex items-center space-x-2">
              <div 
                className="w-8 h-8 rounded-full flex items-center justify-center"
                style={{ backgroundColor: incomeCategory.color + '20', color: incomeCategory.color }}
              >
                <DollarSign className="w-4 h-4" />
              </div>
              <span>Ajouter des transactions à "{incomeCategory.name}"</span>
            </DialogTitle>
            <Button 
              variant="ghost" 
              size="sm" 
              onClick={onClose}
              disabled={isSubmitting}
            >
              <X className="w-4 h-4" />
            </Button>
          </div>
        </DialogHeader>

        <div className="space-y-6">
          {/* Instructions */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start space-x-3">
              <AlertCircle className="w-5 h-5 text-blue-600 mt-0.5" />
              <div className="text-sm text-blue-800">
                <p className="font-medium mb-1">Sélectionnez les transactions de revenus à associer</p>
                <p>Choisissez parmi vos transactions existantes celles qui correspondent à cette catégorie de revenus. Elles seront automatiquement liées et mises à jour dans vos statistiques.</p>
              </div>
            </div>
          </div>

          {/* Smart suggestions */}
          {showSuggestions && suggestedTransactions.length > 0 && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center space-x-2">
                  <Sparkles className="w-5 h-5 text-green-600" />
                  <h3 className="font-semibold text-green-900">
                    Suggestions intelligentes ({suggestedTransactions.length})
                  </h3>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleSelectAllSuggested}
                  className="text-green-700 border-green-300 hover:bg-green-100"
                >
                  Tout sélectionner
                </Button>
              </div>
              
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {suggestedTransactions.map((transaction) => (
                  <div key={transaction.id} className="flex items-center space-x-3 p-2 bg-white rounded border">
                    <Checkbox
                      checked={selectedTransactionIds.includes(transaction.id)}
                      onCheckedChange={() => handleTransactionToggle(transaction.id)}
                    />
                    <Calendar className="w-4 h-4 text-gray-400" />
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium truncate">{transaction.description}</div>
                      <div className="text-xs text-gray-500">
                        {formatDate(transaction.transactionDate)} • {transaction.accountName}
                      </div>
                    </div>
                    <div className="text-sm font-semibold text-green-600">
                      {formatCurrency(transaction.amount)}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Search */}
          <div className="space-y-3">
            <div className="flex items-center space-x-2">
              <h3 className="font-semibold">Toutes les transactions de revenus</h3>
              <span className="text-sm text-gray-500">({availableTransactions.length} disponibles)</span>
            </div>
            
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                placeholder="Rechercher par description ou compte..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>

          {/* Transactions list */}
          <div className="space-y-2 max-h-96 overflow-y-auto border rounded-lg">
            {isLoading ? (
              <div className="p-8 text-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
                <p className="mt-2 text-gray-600">Chargement des transactions...</p>
              </div>
            ) : filteredTransactions.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                {searchQuery ? 'Aucune transaction trouvée' : 'Aucune transaction de revenus disponible'}
              </div>
            ) : (
              filteredTransactions.map((transaction) => (
                <div 
                  key={transaction.id} 
                  className={`flex items-center space-x-3 p-3 border-b last:border-b-0 hover:bg-gray-50 ${
                    selectedTransactionIds.includes(transaction.id) ? 'bg-blue-50' : ''
                  }`}
                >
                  <Checkbox
                    checked={selectedTransactionIds.includes(transaction.id)}
                    onCheckedChange={() => handleTransactionToggle(transaction.id)}
                  />
                  <Calendar className="w-4 h-4 text-gray-400" />
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium truncate">{transaction.description}</div>
                    <div className="text-xs text-gray-500">
                      {formatDate(transaction.transactionDate)} • {transaction.accountName}
                      {transaction.categoryName && ` • ${transaction.categoryName}`}
                    </div>
                  </div>
                  <div className="text-sm font-semibold">
                    {formatCurrency(transaction.amount)}
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Selection summary */}
          {selectedTransactionIds.length > 0 && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-blue-900">
                    {selectedTransactionIds.length} transaction{selectedTransactionIds.length > 1 ? 's' : ''} sélectionnée{selectedTransactionIds.length > 1 ? 's' : ''}
                  </p>
                  <p className="text-sm text-blue-700">
                    Total: {formatCurrency(getSelectedTotal())}
                  </p>
                </div>
                <Check className="w-5 h-5 text-blue-600" />
              </div>
            </div>
          )}
        </div>

        {/* Actions */}
        <div className="flex justify-end space-x-3 pt-4 border-t">
          <Button
            variant="outline"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Annuler
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={selectedTransactionIds.length === 0 || isSubmitting}
          >
            {isSubmitting ? (
              <div className="flex items-center space-x-2">
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                <span>Association...</span>
              </div>
            ) : (
              <div className="flex items-center space-x-2">
                <Check className="w-4 h-4" />
                <span>
                  Associer {selectedTransactionIds.length > 0 ? `(${selectedTransactionIds.length})` : ''}
                </span>
              </div>
            )}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}