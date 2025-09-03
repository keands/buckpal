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
  Sparkles,
  Brain,
  Lightbulb
} from 'lucide-react'
import { IncomeCategory, Transaction, IncomeSuggestion } from '@/types/api'
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
  const [availableTransactions, setAvailableTransactions] = useState<Transaction[]>([])
  const [suggestedTransactions, setSuggestedTransactions] = useState<Transaction[]>([])
  const [assignedTransactions, setAssignedTransactions] = useState<Transaction[]>([])
  const [selectedTransactionIds, setSelectedTransactionIds] = useState<number[]>([])
  const [selectedAssignedIds, setSelectedAssignedIds] = useState<number[]>([])
  const [searchQuery, setSearchQuery] = useState('')
  const [activeTab, setActiveTab] = useState<'add' | 'manage'>('add')
  const [isLoading, setIsLoading] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showSuggestions, setShowSuggestions] = useState(true)
  const [intelligentSuggestions, setIntelligentSuggestions] = useState<{[key: string]: IncomeSuggestion[]}>({})
  const [loadingSuggestions, setLoadingSuggestions] = useState<{[key: string]: boolean}>({})

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

      // Load available income transactions for this budget period
      const available = await apiClient.getAvailableIncomeTransactions(incomeCategory.budgetId)
      setAvailableTransactions(available)

      // Load already assigned transactions for this category
      const assigned = await apiClient.getTransactionsForIncomeCategory(incomeCategory.id)
      setAssignedTransactions(assigned)
      
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

  const handleAssignedTransactionToggle = (transactionId: number) => {
    setSelectedAssignedIds(prev => 
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

  const handleUnlinkTransactions = async () => {
    if (selectedAssignedIds.length === 0) return
    
    setIsSubmitting(true)
    try {
      await apiClient.unlinkTransactionsFromCategories(selectedAssignedIds)
      // Reload transactions to refresh both lists
      await loadTransactions()
      setSelectedAssignedIds([])
      onSuccess() // Notify parent to refresh data
    } catch (error) {
      console.error('Error unlinking transactions:', error)
      // TODO: Show error toast
    } finally {
      setIsSubmitting(false)
    }
  }

  // Load intelligent suggestions for a transaction
  const loadIntelligentSuggestions = async (transaction: Transaction) => {
    if (!transaction.description || intelligentSuggestions[transaction.id] || loadingSuggestions[transaction.id]) {
      return
    }

    setLoadingSuggestions(prev => ({ ...prev, [transaction.id]: true }))
    
    try {
      const suggestions = await apiClient.suggestIncomeCategoryFromDescription(transaction.description)
      setIntelligentSuggestions(prev => ({ 
        ...prev, 
        [transaction.id]: suggestions.slice(0, 2) // Top 2 suggestions
      }))
    } catch (error) {
      console.error('Error loading intelligent suggestions:', error)
    } finally {
      setLoadingSuggestions(prev => ({ ...prev, [transaction.id]: false }))
    }
  }

  const filteredTransactions = availableTransactions.filter(transaction =>
    (transaction.description?.toLowerCase().includes(searchQuery.toLowerCase()) ?? false) ||
    transaction.accountName?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const filteredAssignedTransactions = assignedTransactions.filter(transaction =>
    (transaction.description?.toLowerCase().includes(searchQuery.toLowerCase()) ?? false) ||
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
              <span>Gérer les transactions - "{incomeCategory.name}"</span>
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

        {/* Tabs */}
        <div className="flex space-x-1 bg-gray-100 p-1 rounded-lg">
          <button
            className={`flex-1 px-3 py-2 text-sm font-medium rounded-md transition-colors ${
              activeTab === 'add' 
                ? 'bg-white text-gray-900 shadow-sm' 
                : 'text-gray-600 hover:text-gray-900'
            }`}
            onClick={() => setActiveTab('add')}
          >
            Ajouter des transactions
          </button>
          <button
            className={`flex-1 px-3 py-2 text-sm font-medium rounded-md transition-colors ${
              activeTab === 'manage' 
                ? 'bg-white text-gray-900 shadow-sm' 
                : 'text-gray-600 hover:text-gray-900'
            }`}
            onClick={() => setActiveTab('manage')}
          >
            Gérer assignées ({assignedTransactions.length})
          </button>
        </div>

        <div className="space-y-6">
          {/* Add Transactions Tab */}
          {activeTab === 'add' && (
            <>
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
                      <div className="text-sm font-medium truncate">{transaction.description || 'Sans description'}</div>
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
                  className={`border-b last:border-b-0 hover:bg-gray-50 ${
                    selectedTransactionIds.includes(transaction.id) ? 'bg-blue-50' : ''
                  }`}
                >
                  {/* Main transaction row */}
                  <div className="flex items-center space-x-3 p-3">
                    <Checkbox
                      checked={selectedTransactionIds.includes(transaction.id)}
                      onCheckedChange={() => handleTransactionToggle(transaction.id)}
                    />
                    <Calendar className="w-4 h-4 text-gray-400" />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center space-x-2">
                        <span className="text-sm font-medium truncate">
                          {transaction.description || 'Sans description'}
                        </span>
                        {/* Button to load intelligent suggestions */}
                        {!intelligentSuggestions[transaction.id] && !loadingSuggestions[transaction.id] && (
                          <button
                            onClick={() => loadIntelligentSuggestions(transaction)}
                            className="p-1 text-purple-500 hover:bg-purple-50 rounded"
                            title="Suggestions intelligentes"
                          >
                            <Brain className="w-3 h-3" />
                          </button>
                        )}
                        {loadingSuggestions[transaction.id] && (
                          <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-purple-600"></div>
                        )}
                      </div>
                      <div className="text-xs text-gray-500">
                        {formatDate(transaction.transactionDate)} • {transaction.accountName}
                        {transaction.categoryName && ` • ${transaction.categoryName}`}
                      </div>
                    </div>
                    <div className="text-sm font-semibold">
                      {formatCurrency(transaction.amount)}
                    </div>
                  </div>

                  {/* Intelligent suggestions */}
                  {intelligentSuggestions[transaction.id] && intelligentSuggestions[transaction.id].length > 0 && (
                    <div className="px-3 pb-2 bg-purple-50 border-t">
                      <div className="flex items-center space-x-1 mb-1 pt-2">
                        <Lightbulb className="w-3 h-3 text-purple-500" />
                        <span className="text-xs font-medium text-purple-600">Suggestions IA :</span>
                      </div>
                      <div className="flex flex-wrap gap-1">
                        {intelligentSuggestions[transaction.id].map((suggestion, index) => (
                          <button
                            key={index}
                            className="text-xs px-2 py-1 bg-purple-100 text-purple-700 rounded-full hover:bg-purple-200 transition-colors"
                            title={`${suggestion.reasoning} (Confiance: ${Math.round(suggestion.confidenceScore * 100)}%)`}
                          >
                            {suggestion.categoryName} ({Math.round(suggestion.confidenceScore * 100)}%)
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
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
            </>
          )}

          {/* Manage Assigned Transactions Tab */}
          {activeTab === 'manage' && (
            <>
              {/* Instructions for manage tab */}
              <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
                <div className="flex items-start space-x-3">
                  <AlertCircle className="w-5 h-5 text-orange-600 mt-0.5" />
                  <div className="text-sm text-orange-800">
                    <p className="font-medium mb-1">Gérer les transactions déjà assignées</p>
                    <p>Sélectionnez les transactions que vous souhaitez délier de cette catégorie de revenus. Elles redeviendront disponibles pour être assignées ailleurs.</p>
                  </div>
                </div>
              </div>

              {/* Search */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                <Input
                  type="text"
                  placeholder="Rechercher dans les transactions assignées..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                />
              </div>

              {/* Assigned Transactions List */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <h3 className="font-medium text-gray-900">
                    Transactions assignées ({filteredAssignedTransactions.length})
                  </h3>
                </div>

                {isLoading ? (
                  <div className="flex items-center justify-center py-12">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                  </div>
                ) : filteredAssignedTransactions.length === 0 ? (
                  <div className="text-center py-12">
                    <div className="text-gray-500">
                      {assignedTransactions.length === 0 
                        ? "Aucune transaction assignée à cette catégorie" 
                        : "Aucune transaction trouvée avec ces critères"}
                    </div>
                  </div>
                ) : (
                  <div className="border rounded-lg divide-y divide-gray-200 max-h-96 overflow-y-auto">
                    {filteredAssignedTransactions.map((transaction) => (
                      <div
                        key={transaction.id}
                        className={`p-4 hover:bg-gray-50 transition-colors ${
                          selectedAssignedIds.includes(transaction.id) ? 'bg-blue-50 border-blue-200' : ''
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-3">
                            <Checkbox 
                              checked={selectedAssignedIds.includes(transaction.id)}
                              onCheckedChange={() => handleAssignedTransactionToggle(transaction.id)}
                            />
                            <div className="flex-1">
                              <div className="flex items-center justify-between">
                                <p className="font-medium text-gray-900 truncate">
                                  {transaction.description}
                                </p>
                                <div className="text-right ml-4">
                                  <p className="font-semibold text-green-600">
                                    {formatCurrency(transaction.amount)}
                                  </p>
                                </div>
                              </div>
                              <div className="flex items-center space-x-4 mt-1">
                                <div className="flex items-center space-x-1 text-sm text-gray-500">
                                  <Calendar className="w-3 h-3" />
                                  <span>{formatDate(transaction.transactionDate)}</span>
                                </div>
                                {transaction.accountName && (
                                  <span className="text-sm text-gray-500">
                                    {transaction.accountName}
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Unlink summary */}
              {selectedAssignedIds.length > 0 && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-medium text-red-900">
                        {selectedAssignedIds.length} transaction{selectedAssignedIds.length > 1 ? 's' : ''} à délier
                      </p>
                      <p className="text-sm text-red-700">
                        Ces transactions redeviendront disponibles pour être assignées ailleurs
                      </p>
                    </div>
                    <X className="w-5 h-5 text-red-600" />
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Actions */}
        <div className="flex justify-end space-x-3 pt-4 border-t">
          <Button
            variant="outline"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Fermer
          </Button>
          
          {activeTab === 'add' && (
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
          )}
          
          {activeTab === 'manage' && (
            <Button
              onClick={handleUnlinkTransactions}
              disabled={selectedAssignedIds.length === 0 || isSubmitting}
              variant="destructive"
            >
              {isSubmitting ? (
                <div className="flex items-center space-x-2">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                  <span>Déliaison...</span>
                </div>
              ) : (
                <div className="flex items-center space-x-2">
                  <X className="w-4 h-4" />
                  <span>
                    Délier {selectedAssignedIds.length > 0 ? `(${selectedAssignedIds.length})` : ''}
                  </span>
                </div>
              )}
            </Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}