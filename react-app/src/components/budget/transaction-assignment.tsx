import {useEffect, useState, useCallback} from "react";
import { useTranslation } from 'react-i18next'
import { apiClient } from '@/lib/api'
import type { Transaction, Budget } from '@/types/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { AlertTriangle, CheckCircle, Clock, Zap, TrendingUp, Brain, Target, BarChart3, Sparkles } from 'lucide-react'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/advanced-select'
import { SmartFeedbackModal } from './smart-feedback-modal'

interface TransactionAssignmentProps {
  budget: Budget
  onAssignmentComplete?: () => void
}

interface AssignmentStats {
  unassigned: number
  needsReview: number
  autoAssigned: number
  manuallyAssigned: number
  total: number
}

interface EnhancedAssignmentResult {
  totalAssigned: number
  totalNeedsReview: number
  strategyBreakdown: Record<string, number>
  confidenceStats: {
    average: number
    high: number
    medium: number
    low: number
  }
}

interface SmartSuggestion {
  transactionId: number
  suggestedCategory: string
  confidence: number
  strategy: string
  alternativeCategories: string[]
  merchantText: string
}

export function TransactionAssignment({ budget, onAssignmentComplete }: TransactionAssignmentProps) {
  const { t } = useTranslation()
  const [unassignedTransactions, setUnassignedTransactions] = useState<Transaction[]>([])
  const [needsReviewTransactions, setNeedsReviewTransactions] = useState<Transaction[]>([])
  const [stats, setStats] = useState<AssignmentStats | null>(null)
  const [loading, setLoading] = useState(false)
  const [enhancedResult, setEnhancedResult] = useState<EnhancedAssignmentResult | null>(null)
  const [showEnhancedResults, setShowEnhancedResults] = useState(false)
  const [showSmartFeedback, setShowSmartFeedback] = useState(false)
  const [smartSuggestions, setSmartSuggestions] = useState<Record<number, SmartSuggestion>>({})
  const [feedbackTransactions, setFeedbackTransactions] = useState<Transaction[]>([])
  const [recentlyAssignedTransactions, setRecentlyAssignedTransactions] = useState<Transaction[]>([])
  const [revisionDetectionResult, setRevisionDetectionResult] = useState<{
    suspiciousTransactions: Transaction[]
    count: number
  } | null>(null)
  
  const loadTransactions = useCallback(async () => {
    try {
      const [unassigned, needsReview, recentlyAssigned] = await Promise.all([
        apiClient.getUnassignedTransactions(budget.id),
        apiClient.getTransactionsNeedingReview(budget.id),
        apiClient.getRecentlyAssignedTransactions()
      ])
      
      setUnassignedTransactions(unassigned)
      setNeedsReviewTransactions(needsReview)
      setRecentlyAssignedTransactions(recentlyAssigned)
      
      // Calculate stats
      const total = unassigned.length + needsReview.length
      setStats({
        unassigned: unassigned.length,
        needsReview: needsReview.length,
        autoAssigned: recentlyAssigned.length,
        manuallyAssigned: 0,
        total
      })
    } catch (error) {
      console.error('Failed to load transactions:', error)
    }
  }, [budget.id]) // Depend on budget.id
  
  useEffect(() => {
    loadTransactions()
  }, [loadTransactions]) // Reload when loadTransactions changes (which happens when budget.id changes)

  const handleAutoAssign = async () => {
    setLoading(true)
    try {
      await apiClient.autoAssignTransactions(budget.id)
      await loadTransactions()
      onAssignmentComplete?.()
    } catch (error) {
      console.error('Auto-assignment failed:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleEnhancedAutoAssign = async () => {
    setLoading(true)
    try {
      const result = await apiClient.enhancedAutoAssignTransactions(budget.id)
      setEnhancedResult({
        totalAssigned: result.totalAssigned,
        totalNeedsReview: result.totalNeedsReview,
        strategyBreakdown: result.strategyBreakdown,
        confidenceStats: result.confidenceStats
      })
      setShowEnhancedResults(true)
      await loadTransactions()
      onAssignmentComplete?.()
    } catch (error) {
      console.error('Enhanced auto-assignment failed:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleManualAssign = async (transactionId: number, budgetCategoryId: number) => {
    try {
      await apiClient.manuallyAssignTransaction(transactionId, budgetCategoryId)
      
      // Update local state without full reload - much better UX!
      updateTransactionLocally(transactionId)
      
      // Only notify parent for stats/dashboard update
      onAssignmentComplete?.()
    } catch (error) {
      console.error('Manual assignment failed:', error)
      // On error, fallback to full reload
      await loadTransactions()
    }
  }

  const handleOverrideAssignment = async (transactionId: number, budgetCategoryId: number) => {
    try {
      await apiClient.overrideAssignment(transactionId, budgetCategoryId)
      
      // Update local state without full reload
      updateTransactionLocally(transactionId)
      
      // Only notify parent for stats/dashboard update
      onAssignmentComplete?.()
    } catch (error) {
      console.error('Assignment override failed:', error)
      // On error, fallback to full reload
      await loadTransactions()
    }
  }
  
  const handleSmartFeedbackAssignment = async () => {
    setLoading(true)
    try {
      // Get unassigned transactions that we want to get suggestions for
      const transactionsToProcess = unassignedTransactions.slice(0, 10) // Limit to first 10 for UX
      
      if (transactionsToProcess.length === 0) {
        setLoading(false)
        return
      }
      
      // Get smart suggestions for these transactions
      const transactionIds = transactionsToProcess.map(t => t.id)
      const suggestionsResult = await apiClient.getSmartSuggestionsBatch(transactionIds)
      
      // Transform suggestions to include transaction and merchant info
      const enhancedSuggestions: Record<number, SmartSuggestion> = {}
      for (const [transactionIdStr, suggestion] of Object.entries(suggestionsResult.suggestions)) {
        const transactionId = parseInt(transactionIdStr)
        const transaction = transactionsToProcess.find(t => t.id === transactionId)
        if (transaction && suggestion.suggestedCategory) {
          enhancedSuggestions[transactionId] = {
            transactionId,
            suggestedCategory: suggestion.suggestedCategory,
            confidence: suggestion.confidence,
            strategy: suggestion.strategy,
            alternativeCategories: suggestion.alternativeCategories,
            merchantText: transaction.merchantName || transaction.description || ''
          }
        }
      }
      
      // Only show feedback modal if we have suggestions
      if (Object.keys(enhancedSuggestions).length > 0) {
        setSmartSuggestions(enhancedSuggestions)
        setFeedbackTransactions(transactionsToProcess.filter(t => enhancedSuggestions[t.id]))
        setShowSmartFeedback(true)
      }
      
    } catch (error) {
      console.error('Smart feedback assignment failed:', error)
    } finally {
      setLoading(false)
    }
  }
  
  const handleSmartFeedback = async (transactionId: number, feedback: {
    suggestedCategory: string
    userChosenCategory: string
    wasAccepted: boolean
    patternUsed?: string
  }) => {
    try {
      await apiClient.submitSmartFeedback(transactionId, feedback)
      
      // If user accepted or chose a category, also assign the transaction
      if (feedback.userChosenCategory) {
        const budgetCategory = budget.budgetCategories.find(cat => 
          cat.name === feedback.userChosenCategory
        )
        
        if (budgetCategory) {
          await apiClient.manuallyAssignTransaction(transactionId, budgetCategory.id)
          
          // Update local state without full reload - better UX!
          updateTransactionLocally(transactionId)
        }
      }
      
      // Only notify parent for stats/dashboard update - no full reload!
      onAssignmentComplete?.()
      
    } catch (error) {
      console.error('Smart feedback submission failed:', error)
      // On error, fallback to full reload
      await loadTransactions()
      throw error
    }
  }
  
  // Local state update function - avoids full page reload
  const updateTransactionLocally = (transactionId: number) => {
    // Remove from unassigned list
    setUnassignedTransactions(prev => prev.filter(t => t.id !== transactionId))
    
    // Remove from needs review list
    setNeedsReviewTransactions(prev => prev.filter(t => t.id !== transactionId))
    
    // Remove from recently assigned list if present
    setRecentlyAssignedTransactions(prev => prev.filter(t => t.id !== transactionId))
    
    // Update stats immediately
    setStats(prev => {
      if (!prev) return prev
      return {
        ...prev,
        unassigned: Math.max(0, prev.unassigned - 1),
        needsReview: Math.max(0, prev.needsReview - 1),
        manuallyAssigned: prev.manuallyAssigned + 1,
        total: Math.max(0, prev.total - 1)
      }
    })
  }
  
  const handleAutoDetectRevision = async () => {
    setLoading(true)
    try {
      const result = await apiClient.autoDetectAndMarkForRevision()
      setRevisionDetectionResult({
        suspiciousTransactions: result.revisedTransactions,
        count: result.markedForRevision
      })
      
      // Refresh transactions after marking for revision
      await loadTransactions()
      onAssignmentComplete?.()
      
    } catch (error) {
      console.error('Auto revision detection failed:', error)
    } finally {
      setLoading(false)
    }
  }
  
  const handleMarkForRevision = async (transactionId: number) => {
    try {
      await apiClient.markTransactionForRevision(transactionId)
      
      // Move transaction locally to needs review without full reload
      const transactionToMove = recentlyAssignedTransactions.find(t => t.id === transactionId)
      if (transactionToMove) {
        // Remove from recently assigned
        setRecentlyAssignedTransactions(prev => prev.filter(t => t.id !== transactionId))
        
        // Add to needs review with updated status
        const updatedTransaction = { ...transactionToMove, assignmentStatus: 'NEEDS_REVIEW' as const }
        setNeedsReviewTransactions(prev => [...prev, updatedTransaction])
        
        // Update stats
        setStats(prev => {
          if (!prev) return prev
          return {
            ...prev,
            autoAssigned: Math.max(0, prev.autoAssigned - 1),
            needsReview: prev.needsReview + 1
          }
        })
      }
      
      onAssignmentComplete?.()
    } catch (error) {
      console.error('Mark for revision failed:', error)
      // On error, fallback to full reload
      await loadTransactions()
    }
  }

  const getAssignmentStatusBadge = (status: string | undefined) => {
    switch (status) {
      case 'UNASSIGNED':
        return <Badge variant="secondary" className="text-gray-600"><Clock className="w-3 h-3 mr-1" />{t('budget.assignment.unassigned')}</Badge>
      case 'AUTO_ASSIGNED':
        return <Badge variant="default" className="bg-blue-100 text-blue-700"><Zap className="w-3 h-3 mr-1" />{t('budget.assignment.autoAssigned')}</Badge>
      case 'MANUALLY_ASSIGNED':
        return <Badge variant="default" className="bg-green-100 text-green-700"><CheckCircle className="w-3 h-3 mr-1" />{t('budget.assignment.manuallyAssigned')}</Badge>
      case 'NEEDS_REVIEW':
        return <Badge variant="destructive"><AlertTriangle className="w-3 h-3 mr-1" />{t('budget.assignment.needsReview')}</Badge>
      default:
        return null
    }
  }

  const TransactionCard = ({ transaction, showCategorySelector = false }: { transaction: Transaction, showCategorySelector?: boolean }) => (
    <Card key={transaction.id} className="mb-3">
      <CardContent className="p-4">
        <div className="flex justify-between items-start">
          <div className="flex-1">
            <div className="flex justify-between items-center mb-2">
              <h4 className="font-medium">{transaction.description || transaction.merchantName}</h4>
              <div className="text-right">
                <div className="font-semibold text-lg">
                  {transaction.transactionType === 'EXPENSE' ? '-' : ''}€{Math.abs(transaction.amount).toFixed(2)}
                </div>
                <div className="text-sm text-gray-500">{transaction.transactionDate}</div>
              </div>
            </div>
            
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                {transaction.categoryName && (
                  <Badge variant="outline" className="text-xs">{transaction.categoryName}</Badge>
                )}
                {getAssignmentStatusBadge(transaction.assignmentStatus)}
              </div>
              
              {showCategorySelector && (
                <div className="w-64">
                  <Select
                    onValueChange={(value) => {
                      if (value) {
                        const categoryId = parseInt(value)
                        if (transaction.assignmentStatus === 'NEEDS_REVIEW') {
                          handleOverrideAssignment(transaction.id, categoryId)
                        } else {
                          handleManualAssign(transaction.id, categoryId)
                        }
                      }
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={t('budget.assignment.selectCategory')} />
                    </SelectTrigger>
                    <SelectContent>
                      {budget.budgetCategories.map((category) => (
                        <SelectItem key={category.id} value={category.id.toString()}>
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
                </div>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )

  return (
    <div className="space-y-6">
      {/* Assignment Stats */}
      {stats && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Zap className="w-5 h-5" />
              {t('budget.assignment.title')}
            </CardTitle>
            <CardDescription>{t('budget.assignment.description')}</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
              <div className="text-center p-4 bg-gray-50 rounded-lg">
                <div className="text-2xl font-bold text-gray-600">{stats.unassigned}</div>
                <div className="text-sm text-gray-500">{t('budget.assignment.unassigned')}</div>
              </div>
              <div className="text-center p-4 bg-orange-50 rounded-lg">
                <div className="text-2xl font-bold text-orange-600">{stats.needsReview}</div>
                <div className="text-sm text-orange-500">{t('budget.assignment.needsReview')}</div>
              </div>
              <div className="text-center p-4 bg-blue-50 rounded-lg">
                <div className="text-2xl font-bold text-blue-600">{stats.autoAssigned}</div>
                <div className="text-sm text-blue-500">{t('budget.assignment.autoAssigned')}</div>
              </div>
              <div className="text-center p-4 bg-green-50 rounded-lg">
                <div className="text-2xl font-bold text-green-600">{stats.manuallyAssigned}</div>
                <div className="text-sm text-green-500">{t('budget.assignment.manuallyAssigned')}</div>
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-2">
              <Button 
                onClick={handleAutoAssign} 
                disabled={loading || stats.unassigned === 0}
                variant="outline"
              >
                <Zap className="w-4 h-4 mr-2" />
                {loading ? t('budget.assignment.processing') : t('budget.assignment.basicAutoAssign')}
              </Button>
              
              <Button 
                onClick={handleEnhancedAutoAssign} 
                disabled={loading || stats.unassigned === 0}
                variant="outline"
                className="border-purple-300 text-purple-700 hover:bg-purple-50"
              >
                <Brain className="w-4 h-4 mr-2" />
                {loading ? t('budget.assignment.processing') : t('budget.assignment.smartAutoAssign')}
              </Button>
              
              <Button 
                onClick={handleSmartFeedbackAssignment} 
                disabled={loading || stats.unassigned === 0}
                className="bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700"
              >
                <Sparkles className="w-4 h-4 mr-2" />
                {loading ? t('budget.assignment.processing') : t('budget.assignment.smartWithFeedback')}
              </Button>
              
              <Button 
                onClick={handleAutoDetectRevision} 
                disabled={loading}
                variant="outline"
                className="border-orange-300 text-orange-700 hover:bg-orange-50"
              >
                <AlertTriangle className="w-4 h-4 mr-2" />
                {loading ? t('budget.assignment.processing') : t('budget.assignment.reviseAssignments')}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Enhanced Assignment Results */}
      {showEnhancedResults && enhancedResult && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="w-5 h-5 text-purple-600" />
              {t('budget.assignment.enhancedResults')}
            </CardTitle>
            <CardDescription>
              {t('budget.assignment.enhancedResultsDescription')}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {/* Assignment Summary */}
            <div className="grid grid-cols-2 gap-4 mb-6">
              <div className="text-center p-4 bg-green-50 rounded-lg border border-green-200">
                <div className="text-3xl font-bold text-green-600">{enhancedResult.totalAssigned}</div>
                <div className="text-sm text-green-700">{t('budget.assignment.successfullyAssigned')}</div>
              </div>
              <div className="text-center p-4 bg-orange-50 rounded-lg border border-orange-200">
                <div className="text-3xl font-bold text-orange-600">{enhancedResult.totalNeedsReview}</div>
                <div className="text-sm text-orange-700">{t('budget.assignment.stillNeedsReview')}</div>
              </div>
            </div>

            {/* Confidence Statistics */}
            {enhancedResult.confidenceStats.average > 0 && (
              <div className="mb-6">
                <h4 className="text-sm font-medium text-gray-700 mb-3 flex items-center gap-2">
                  <Target className="w-4 h-4" />
                  {t('budget.assignment.confidenceStats')}
                </h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <div className="text-center p-3 bg-gray-50 rounded-lg">
                    <div className="text-lg font-semibold text-gray-600">{Math.round(enhancedResult.confidenceStats.average * 100)}%</div>
                    <div className="text-xs text-gray-500">{t('budget.assignment.avgConfidence')}</div>
                  </div>
                  <div className="text-center p-3 bg-green-50 rounded-lg">
                    <div className="text-lg font-semibold text-green-600">{enhancedResult.confidenceStats.high}</div>
                    <div className="text-xs text-green-500">{t('budget.assignment.highConfidence')} (&gt;80%)</div>
                  </div>
                  <div className="text-center p-3 bg-yellow-50 rounded-lg">
                    <div className="text-lg font-semibold text-yellow-600">{enhancedResult.confidenceStats.medium}</div>
                    <div className="text-xs text-yellow-500">{t('budget.assignment.mediumConfidence')} (60-80%)</div>
                  </div>
                  <div className="text-center p-3 bg-red-50 rounded-lg">
                    <div className="text-lg font-semibold text-red-600">{enhancedResult.confidenceStats.low}</div>
                    <div className="text-xs text-red-500">{t('budget.assignment.lowConfidence')} (&lt;60%)</div>
                  </div>
                </div>
              </div>
            )}

            {/* Strategy Breakdown */}
            {Object.keys(enhancedResult.strategyBreakdown).length > 0 && (
              <div className="mb-4">
                <h4 className="text-sm font-medium text-gray-700 mb-3 flex items-center gap-2">
                  <TrendingUp className="w-4 h-4" />
                  {t('budget.assignment.strategyBreakdown')}
                </h4>
                <div className="space-y-2">
                  {Object.entries(enhancedResult.strategyBreakdown).map(([strategy, count]) => (
                    <div key={strategy} className="flex justify-between items-center p-2 bg-blue-50 rounded">
                      <span className="text-sm font-medium capitalize">
                        {t(`budget.assignment.strategies.${strategy}`, strategy)}
                      </span>
                      <Badge variant="secondary">{count}</Badge>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowEnhancedResults(false)}
              className="w-full"
            >
              {t('common.close')}
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Recently Assigned Transactions - Available for Revision */}
      {recentlyAssignedTransactions.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Clock className="w-5 h-5 text-blue-500" />
              {t('budget.assignment.recentlyAssigned')} ({recentlyAssignedTransactions.length})
            </CardTitle>
            <CardDescription>
              {t('budget.assignment.recentlyAssignedDescription')}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {recentlyAssignedTransactions.map((transaction) => (
                <Card key={transaction.id} className="border-blue-200 bg-blue-50">
                  <CardContent className="p-4">
                    <div className="flex justify-between items-start">
                      <div className="flex-1">
                        <div className="flex justify-between items-center mb-2">
                          <h4 className="font-medium">{transaction.description || transaction.merchantName}</h4>
                          <div className="text-right">
                            <div className="font-semibold text-lg">
                              {transaction.transactionType === 'EXPENSE' ? '-' : ''}€{Math.abs(transaction.amount).toFixed(2)}
                            </div>
                            <div className="text-sm text-gray-500">{transaction.transactionDate}</div>
                          </div>
                        </div>
                        
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            {transaction.budgetCategoryName && (
                              <Badge variant="outline" className="text-xs bg-blue-100 text-blue-700">
                                {transaction.budgetCategoryName}
                              </Badge>
                            )}
                            <Badge variant="default" className="bg-blue-100 text-blue-700">
                              <Clock className="w-3 h-3 mr-1" />{t('budget.assignment.recentlyAssigned')}
                            </Badge>
                          </div>
                          
                          <Button
                            onClick={() => handleMarkForRevision(transaction.id)}
                            size="sm"
                            variant="outline"
                            className="border-orange-300 text-orange-700 hover:bg-orange-50"
                          >
                            <AlertTriangle className="w-4 h-4 mr-1" />
                            {t('budget.assignment.revise')}
                          </Button>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
      
      {/* Auto-Detection Results */}
      {revisionDetectionResult && revisionDetectionResult.count > 0 && (
        <Card className="border-orange-200 bg-orange-50">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-orange-600" />
              {t('budget.assignment.revisionDetected')}
            </CardTitle>
            <CardDescription>
              {t('budget.assignment.revisionDetectedDescription', { count: revisionDetectionResult.count })}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="bg-orange-100 border border-orange-200 rounded-lg p-4 text-center">
              <p className="text-orange-800 font-medium">
                {revisionDetectionResult.count} {t('budget.assignment.transactionsMarkedForRevision')}
              </p>
              <p className="text-orange-700 text-sm mt-1">
                {t('budget.assignment.checkNeedsReviewSection')}
              </p>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setRevisionDetectionResult(null)}
              className="w-full mt-3"
            >
              {t('common.dismiss')}
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Transactions Needing Review */}
      {needsReviewTransactions.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-orange-500" />
              {t('budget.assignment.reviewRequired')} ({needsReviewTransactions.length})
            </CardTitle>
            <CardDescription>
              {t('budget.assignment.reviewDescription')}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {needsReviewTransactions.map((transaction) => (
                <TransactionCard key={transaction.id} transaction={transaction} showCategorySelector={true} />
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Unassigned Transactions */}
      {unassignedTransactions.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Clock className="w-5 h-5 text-gray-500" />
              {t('budget.assignment.unassignedTransactions')} ({unassignedTransactions.length})
            </CardTitle>
            <CardDescription>
              {t('budget.assignment.unassignedDescription')}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {unassignedTransactions.map((transaction) => (
                <TransactionCard key={transaction.id} transaction={transaction} showCategorySelector={true} />
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Empty State */}
      {stats && stats.total === 0 && (
        <Card>
          <CardContent className="text-center py-12">
            <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-4" />
            <h3 className="text-xl font-semibold mb-2">{t('budget.assignment.allAssigned')}</h3>
            <p className="text-gray-500">{t('budget.assignment.allAssignedDescription')}</p>
          </CardContent>
        </Card>
      )}
      
      {/* Smart Feedback Modal */}
      {showSmartFeedback && (
        <SmartFeedbackModal
          transactions={feedbackTransactions}
          suggestions={smartSuggestions}
          onFeedback={handleSmartFeedback}
          onClose={() => {
            setShowSmartFeedback(false)
            setSmartSuggestions({})
            setFeedbackTransactions([])
          }}
          availableCategories={budget.budgetCategories.map(cat => ({
            id: cat.id,
            name: cat.name,
            colorCode: cat.colorCode
          }))}
        />
      )}
    </div>
  )
}