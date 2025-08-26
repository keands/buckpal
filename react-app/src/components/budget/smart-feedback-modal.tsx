import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/advanced-select'
import { CheckCircle, X, AlertTriangle, Brain, Target, TrendingUp } from 'lucide-react'
import type { Transaction } from '@/types/api'

interface SmartSuggestion {
  transactionId: number
  suggestedCategory: string
  confidence: number
  strategy: string
  alternativeCategories: string[]
  merchantText: string
}

interface SmartFeedbackModalProps {
  transactions: Transaction[]
  suggestions: Record<number, SmartSuggestion>
  onFeedback: (transactionId: number, feedback: {
    suggestedCategory: string
    userChosenCategory: string
    wasAccepted: boolean
    patternUsed?: string
  }) => Promise<void>
  onClose: () => void
  availableCategories: Array<{ id: number; name: string; colorCode?: string }>
}

export function SmartFeedbackModal({
  transactions,
  suggestions,
  onFeedback,
  onClose,
  availableCategories
}: SmartFeedbackModalProps) {
  const { t } = useTranslation()
  const [feedbackStates, setFeedbackStates] = useState<Record<number, {
    userChoice?: string
    submitted: boolean
  }>>({})
  
  const getConfidenceColor = (confidence: number) => {
    if (confidence >= 0.8) return 'text-green-600 bg-green-50 border-green-200'
    if (confidence >= 0.6) return 'text-yellow-600 bg-yellow-50 border-yellow-200'
    return 'text-red-600 bg-red-50 border-red-200'
  }
  
  const getStrategyIcon = (strategy: string) => {
    switch (strategy) {
      case 'SPECIFICITY_WEIGHTED':
        return <Target className="w-4 h-4" />
      case 'USER_FEEDBACK_HISTORY':
        return <Brain className="w-4 h-4" />
      case 'ACCURACY_HISTORY':
        return <TrendingUp className="w-4 h-4" />
      default:
        return <AlertTriangle className="w-4 h-4" />
    }
  }
  
  const handleAcceptSuggestion = async (transaction: Transaction) => {
    const suggestion = suggestions[transaction.id]
    if (!suggestion) return
    
    try {
      await onFeedback(transaction.id, {
        suggestedCategory: suggestion.suggestedCategory,
        userChosenCategory: suggestion.suggestedCategory,
        wasAccepted: true,
        patternUsed: suggestion.strategy
      })
      
      setFeedbackStates(prev => ({
        ...prev,
        [transaction.id]: { submitted: true, userChoice: suggestion.suggestedCategory }
      }))
    } catch (error) {
      console.error('Failed to submit positive feedback:', error)
    }
  }
  
  const handleRejectSuggestion = async (transaction: Transaction, userChosenCategory: string) => {
    const suggestion = suggestions[transaction.id]
    if (!suggestion) return
    
    try {
      await onFeedback(transaction.id, {
        suggestedCategory: suggestion.suggestedCategory,
        userChosenCategory: userChosenCategory,
        wasAccepted: false,
        patternUsed: suggestion.strategy
      })
      
      setFeedbackStates(prev => ({
        ...prev,
        [transaction.id]: { submitted: true, userChoice: userChosenCategory }
      }))
    } catch (error) {
      console.error('Failed to submit negative feedback:', error)
    }
  }
  
  const handleCategoryChange = (transactionId: number, categoryName: string) => {
    setFeedbackStates(prev => ({
      ...prev,
      [transactionId]: { ...prev[transactionId], userChoice: categoryName }
    }))
  }
  
  const pendingFeedbacks = transactions.filter(t => 
    suggestions[t.id] && !feedbackStates[t.id]?.submitted
  )
  
  const completedFeedbacks = transactions.filter(t =>
    suggestions[t.id] && feedbackStates[t.id]?.submitted
  )

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-hidden">
        <div className="flex justify-between items-center p-6 border-b">
          <h2 className="text-xl font-semibold flex items-center gap-2">
            <Brain className="w-5 h-5 text-purple-600" />
            {t('budget.smartFeedback.title')}
          </h2>
          <Button variant="outline" size="sm" onClick={onClose}>
            <X className="w-4 h-4" />
          </Button>
        </div>
        
        <div className="p-6 overflow-y-auto max-h-[calc(90vh-120px)]">
          {/* Instructions */}
          <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-blue-800 text-sm">
              {t('budget.smartFeedback.instructions')}
            </p>
          </div>
          
          {/* Pending Feedback Items */}
          {pendingFeedbacks.length > 0 && (
            <div className="space-y-4 mb-6">
              <h3 className="text-lg font-medium text-gray-900">
                {t('budget.smartFeedback.pendingReview')} ({pendingFeedbacks.length})
              </h3>
              
              {pendingFeedbacks.map((transaction) => {
                const suggestion = suggestions[transaction.id]
                const feedbackState = feedbackStates[transaction.id] || {}
                
                return (
                  <Card key={transaction.id} className="border-l-4 border-l-purple-500">
                    <CardHeader className="pb-3">
                      <div className="flex justify-between items-start">
                        <div>
                          <CardTitle className="text-base">
                            {transaction.description || transaction.merchantName}
                          </CardTitle>
                          <p className="text-sm text-gray-600 mt-1">
                            {suggestion.merchantText}
                          </p>
                        </div>
                        <div className="text-right">
                          <div className="font-semibold">
                            {transaction.transactionType === 'EXPENSE' ? '-' : ''}€{Math.abs(transaction.amount).toFixed(2)}
                          </div>
                          <div className="text-sm text-gray-500">{transaction.transactionDate}</div>
                        </div>
                      </div>
                    </CardHeader>
                    
                    <CardContent className="pt-0">
                      {/* AI Suggestion */}
                      <div className="mb-4 p-3 bg-purple-50 border border-purple-200 rounded-lg">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            {getStrategyIcon(suggestion.strategy)}
                            <span className="font-medium text-purple-800">
                              {t('budget.smartFeedback.aiSuggestion')}
                            </span>
                          </div>
                          <Badge 
                            className={`${getConfidenceColor(suggestion.confidence)} border`}
                          >
                            {Math.round(suggestion.confidence * 100)}% {t('budget.smartFeedback.confidence')}
                          </Badge>
                        </div>
                        
                        <div className="flex items-center justify-between">
                          <span className="text-lg font-semibold text-purple-900">
                            {suggestion.suggestedCategory}
                          </span>
                          <div className="text-xs text-purple-600">
                            {t(`budget.smartFeedback.strategies.${suggestion.strategy}`, suggestion.strategy)}
                          </div>
                        </div>
                        
                        {suggestion.alternativeCategories.length > 0 && (
                          <div className="mt-2 text-xs text-purple-600">
                            {t('budget.smartFeedback.alternatives')}: {suggestion.alternativeCategories.join(', ')}
                          </div>
                        )}
                      </div>
                      
                      {/* Feedback Actions */}
                      <div className="flex gap-3">
                        {/* Accept Button */}
                        <Button
                          onClick={() => handleAcceptSuggestion(transaction)}
                          className="flex-1 bg-green-600 hover:bg-green-700 text-white"
                        >
                          <CheckCircle className="w-4 h-4 mr-2" />
                          {t('budget.smartFeedback.accept')}
                        </Button>
                        
                        {/* Reject with Alternative */}
                        <div className="flex-1 flex gap-2">
                          <Select
                            onValueChange={(value) => handleCategoryChange(transaction.id, value)}
                            value={feedbackState.userChoice || ''}
                          >
                            <SelectTrigger className="flex-1">
                              <SelectValue placeholder={t('budget.smartFeedback.selectAlternative')} />
                            </SelectTrigger>
                            <SelectContent>
                              {availableCategories.map((category) => (
                                <SelectItem key={category.id} value={category.name}>
                                  <div className="flex items-center gap-2">
                                    <div 
                                      className="w-3 h-3 rounded-full" 
                                      style={{ backgroundColor: category.colorCode || '#6366f1' }}
                                    />
                                    {category.name}
                                  </div>
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                          
                          <Button
                            onClick={() => {
                              if (feedbackState.userChoice) {
                                handleRejectSuggestion(transaction, feedbackState.userChoice)
                              }
                            }}
                            disabled={!feedbackState.userChoice}
                            variant="outline"
                            className="border-red-300 text-red-600 hover:bg-red-50"
                          >
                            <X className="w-4 h-4 mr-2" />
                            {t('budget.smartFeedback.reject')}
                          </Button>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                )
              })}
            </div>
          )}
          
          {/* Completed Feedback Summary */}
          {completedFeedbacks.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-medium text-gray-900 flex items-center gap-2">
                <CheckCircle className="w-5 h-5 text-green-600" />
                {t('budget.smartFeedback.completed')} ({completedFeedbacks.length})
              </h3>
              
              <div className="grid gap-2">
                {completedFeedbacks.map((transaction) => {
                  const suggestion = suggestions[transaction.id]
                  const feedbackState = feedbackStates[transaction.id]
                  const wasAccepted = feedbackState.userChoice === suggestion.suggestedCategory
                  
                  return (
                    <div 
                      key={transaction.id}
                      className={`p-3 rounded-lg border-l-4 ${
                        wasAccepted 
                          ? 'bg-green-50 border-l-green-500 border-green-200' 
                          : 'bg-orange-50 border-l-orange-500 border-orange-200'
                      }`}
                    >
                      <div className="flex justify-between items-center">
                        <div className="flex-1">
                          <div className="font-medium text-sm">
                            {transaction.description || transaction.merchantName}
                          </div>
                          <div className="text-xs text-gray-600 mt-1">
                            {wasAccepted ? (
                              <span className="text-green-700">
                                ✓ {t('budget.smartFeedback.acceptedSuggestion')}: {suggestion.suggestedCategory}
                              </span>
                            ) : (
                              <span className="text-orange-700">
                                ✗ {t('budget.smartFeedback.rejectedFor')}: {feedbackState.userChoice}
                              </span>
                            )}
                          </div>
                        </div>
                        <Badge 
                          variant={wasAccepted ? "default" : "secondary"}
                          className={wasAccepted ? "bg-green-100 text-green-700" : "bg-orange-100 text-orange-700"}
                        >
                          {wasAccepted ? t('budget.smartFeedback.accepted') : t('budget.smartFeedback.corrected')}
                        </Badge>
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
          
          {/* Empty State */}
          {pendingFeedbacks.length === 0 && completedFeedbacks.length === 0 && (
            <div className="text-center py-12">
              <Brain className="w-16 h-16 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                {t('budget.smartFeedback.noSuggestions')}
              </h3>
              <p className="text-gray-600">
                {t('budget.smartFeedback.noSuggestionsDescription')}
              </p>
            </div>
          )}
          
          {/* Close Button */}
          {pendingFeedbacks.length === 0 && (
            <div className="mt-6 text-center">
              <Button onClick={onClose} className="px-8">
                {t('common.close')}
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}