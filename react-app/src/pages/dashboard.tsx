import { useState, useEffect } from 'react'
import { apiClient } from '@/lib/api'
import { formatCurrency } from '@/lib/utils'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Link } from 'react-router-dom'
import { 
  CreditCard, 
  TrendingUp, 
  TrendingDown, 
  Plus, 
  Upload,
  Calendar,
  ArrowUpRight,
  ArrowDownLeft
} from 'lucide-react'
import type { Account, Transaction } from '@/types/api'

export default function DashboardPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([])
  const [stats, setStats] = useState({
    totalBalance: 0,
    monthlyIncome: 0,
    monthlyExpenses: 0,
  })
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    loadDashboardData()
  }, [])

  const loadDashboardData = async () => {
    try {
      setIsLoading(true)
      
      // Load accounts
      const accountsData = await apiClient.getAccounts()
      setAccounts(accountsData)
      
      // Calculate total balance
      const totalBalance = accountsData.reduce((sum, account) => sum + account.balance, 0)
      
      // Load recent transactions
      const transactionsResponse = await apiClient.getTransactions(undefined, 0, 10)
      setRecentTransactions(transactionsResponse.content)
      
      // Calculate monthly stats (simplified - you might want to use actual date filtering)
      const currentMonth = new Date().getMonth()
      const currentYear = new Date().getFullYear()
      
      const monthlyTransactions = transactionsResponse.content.filter(t => {
        const transactionDate = new Date(t.transactionDate)
        return transactionDate.getMonth() === currentMonth && 
               transactionDate.getFullYear() === currentYear
      })
      
      const monthlyIncome = monthlyTransactions
        .filter(t => t.transactionType === 'INCOME')
        .reduce((sum, t) => sum + t.amount, 0)
      
      const monthlyExpenses = monthlyTransactions
        .filter(t => t.transactionType === 'EXPENSE')
        .reduce((sum, t) => sum + Math.abs(t.amount), 0)
      
      setStats({
        totalBalance,
        monthlyIncome,
        monthlyExpenses,
      })
      
    } catch (error) {
      console.error('Error loading dashboard data:', error)
    } finally {
      setIsLoading(false)
    }
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-lg">Chargement du tableau de bord...</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Tableau de bord</h1>
          <p className="text-gray-600">Aperçu de vos finances</p>
        </div>
        
        <div className="flex space-x-3">
          <Button asChild>
            <Link to="/csv-import">
              <Upload className="w-4 h-4 mr-2" />
              Importer CSV
            </Link>
          </Button>
          <Button variant="outline" asChild>
            <Link to="/accounts">
              <Plus className="w-4 h-4 mr-2" />
              Nouveau compte
            </Link>
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Solde total</CardTitle>
            <CreditCard className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatCurrency(stats.totalBalance)}</div>
            <p className="text-xs text-muted-foreground">
              {accounts.length} compte{accounts.length > 1 ? 's' : ''} actif{accounts.length > 1 ? 's' : ''}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Revenus du mois</CardTitle>
            <TrendingUp className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              +{formatCurrency(stats.monthlyIncome)}
            </div>
            <p className="text-xs text-muted-foreground">
              Ce mois-ci
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Dépenses du mois</CardTitle>
            <TrendingDown className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">
              -{formatCurrency(stats.monthlyExpenses)}
            </div>
            <p className="text-xs text-muted-foreground">
              Ce mois-ci
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Accounts and Recent Transactions */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Accounts */}
        <Card>
          <CardHeader>
            <CardTitle>Mes comptes</CardTitle>
            <CardDescription>
              Vue d'ensemble de vos comptes bancaires
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {accounts.length > 0 ? (
                accounts.map((account) => (
                  <div
                    key={account.id}
                    className="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50"
                  >
                    <div className="flex items-center space-x-3">
                      <div className="p-2 bg-primary/10 rounded-lg">
                        <CreditCard className="w-4 h-4 text-primary" />
                      </div>
                      <div>
                        <h3 className="font-medium">{account.name}</h3>
                        <p className="text-sm text-gray-500">
                          {account.bankName || account.accountType}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-semibold">
                        {formatCurrency(account.balance)}
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-4 text-gray-500">
                  <CreditCard className="w-8 h-8 mx-auto mb-2 text-gray-400" />
                  <p>Aucun compte configuré</p>
                  <Button className="mt-2" asChild>
                    <Link to="/accounts">Ajouter un compte</Link>
                  </Button>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Recent Transactions */}
        <Card>
          <CardHeader>
            <CardTitle>Transactions récentes</CardTitle>
            <CardDescription>
              Vos dernières transactions
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentTransactions.length > 0 ? (
                recentTransactions.map((transaction) => (
                  <div
                    key={transaction.id}
                    className="flex items-center justify-between p-3 border rounded-lg"
                  >
                    <div className="flex items-center space-x-3">
                      <div className={`p-2 rounded-lg ${
                        transaction.transactionType === 'INCOME' 
                          ? 'bg-green-50' 
                          : 'bg-red-50'
                      }`}>
                        {transaction.transactionType === 'INCOME' ? (
                          <ArrowUpRight className="w-4 h-4 text-green-600" />
                        ) : (
                          <ArrowDownLeft className="w-4 h-4 text-red-600" />
                        )}
                      </div>
                      <div>
                        <h3 className="font-medium">
                          {transaction.description || transaction.merchantName}
                        </h3>
                        <p className="text-sm text-gray-500">
                          {new Date(transaction.transactionDate).toLocaleDateString('fr-FR')}
                        </p>
                      </div>
                    </div>
                    <div className={`font-semibold ${
                      transaction.transactionType === 'INCOME' 
                        ? 'text-green-600' 
                        : 'text-red-600'
                    }`}>
                      {transaction.transactionType === 'INCOME' ? '+' : '-'}
                      {formatCurrency(Math.abs(transaction.amount))}
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-4 text-gray-500">
                  <Calendar className="w-8 h-8 mx-auto mb-2 text-gray-400" />
                  <p>Aucune transaction récente</p>
                  <Button className="mt-2" asChild>
                    <Link to="/csv-import">Importer des transactions</Link>
                  </Button>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}