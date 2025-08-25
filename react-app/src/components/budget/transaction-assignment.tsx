import {useEffect, useState, useCallback} from "react";
import { useTranslation } from 'react-i18next'
import { apiClient } from '@/lib/api'
import type { Transaction, Budget } from '@/types/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { AlertTriangle, CheckCircle, Clock, Zap } from 'lucide-react'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/advanced-select'

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

export function TransactionAssignment({ budget, onAssignmentComplete }: TransactionAssignmentProps) {
  const { t } = useTranslation()
  const [unassignedTransactions, setUnassignedTransactions] = useState<Transaction[]>([])
  const [needsReviewTransactions, setNeedsReviewTransactions] = useState<Transaction[]>([])
  const [stats, setStats] = useState<AssignmentStats | null>(null)
  const [loading, setLoading] = useState(false)
  
  const loadTransactions = useCallback(async () => {
    try {
      const [unassigned, needsReview] = await Promise.all([
        apiClient.getUnassignedTransactions(budget.id),
        apiClient.getTransactionsNeedingReview(budget.id)
      ])
      
      setUnassignedTransactions(unassigned)
      setNeedsReviewTransactions(needsReview)
      
      // Calculate stats
      const total = unassigned.length + needsReview.length
      setStats({
        unassigned: unassigned.length,
        needsReview: needsReview.length,
        autoAssigned: 0, // These would be fetched separately if needed
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

  const handleManualAssign = async (transactionId: number, budgetCategoryId: number) => {
    try {
      await apiClient.manuallyAssignTransaction(transactionId, budgetCategoryId)
      await loadTransactions()
      onAssignmentComplete?.()
    } catch (error) {
      console.error('Manual assignment failed:', error)
    }
  }

  const handleOverrideAssignment = async (transactionId: number, budgetCategoryId: number) => {
    try {
      await apiClient.overrideAssignment(transactionId, budgetCategoryId)
      await loadTransactions()
      onAssignmentComplete?.()
    } catch (error) {
      console.error('Assignment override failed:', error)
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
            
            <Button 
              onClick={handleAutoAssign} 
              disabled={loading || stats.unassigned === 0}
              className="w-full"
            >
              <Zap className="w-4 h-4 mr-2" />
              {loading ? t('budget.assignment.processing') : t('budget.assignment.autoAssign')}
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
    </div>
  )
}