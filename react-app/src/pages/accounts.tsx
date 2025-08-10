import { useState, useEffect } from 'react'
import { apiClient } from '@/lib/api'
import { formatCurrency } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { ConfirmationDialog } from '@/components/ui/confirmation-dialog'
import { 
  Plus, 
  Edit, 
  Trash2, 
  CreditCard, 
  Building, 
  Eye,
  EyeOff,
  CheckCircle,
  AlertCircle,
  FileX
} from 'lucide-react'
import type { Account } from '@/types/api'

type AccountFormData = {
  name: string
  accountType: Account['accountType']
  bankName: string
  accountNumber: string
  routingNumber: string
  balance: string
  isActive: boolean
}

const ACCOUNT_TYPES = [
  { value: 'CHECKING', label: 'Compte Courant' },
  { value: 'SAVINGS', label: 'Compte Épargne' },
  { value: 'CREDIT_CARD', label: 'Carte de Crédit' },
  { value: 'INVESTMENT', label: 'Compte d\'Investissement' },
  { value: 'LOAN', label: 'Prêt' },
  { value: 'OTHER', label: 'Autre' },
] as const

export default function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  
  // Dialog states
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [isDeleteTransactionsDialogOpen, setIsDeleteTransactionsDialogOpen] = useState(false)
  const [editingAccount, setEditingAccount] = useState<Account | null>(null)
  const [deletingAccount, setDeletingAccount] = useState<Account | null>(null)
  const [deletingTransactionsAccount, setDeletingTransactionsAccount] = useState<Account | null>(null)
  const [transactionCount, setTransactionCount] = useState<number>(0)
  const [isDeletingTransactions, setIsDeletingTransactions] = useState(false)
  
  // Form state
  const [formData, setFormData] = useState<AccountFormData>({
    name: '',
    accountType: 'CHECKING',
    bankName: '',
    accountNumber: '',
    routingNumber: '',
    balance: '0',
    isActive: true,
  })
  
  // UI state
  const [showAccountNumbers, setShowAccountNumbers] = useState<Record<number, boolean>>({})

  useEffect(() => {
    loadAccounts()
  }, [])

  const loadAccounts = async () => {
    try {
      setIsLoading(true)
      setError('')
      const accountsData = await apiClient.getAccounts()
      setAccounts(accountsData)
    } catch (error: any) {
      setError(error.response?.data?.message || 'Erreur lors du chargement des comptes')
    } finally {
      setIsLoading(false)
    }
  }

  const resetForm = () => {
    setFormData({
      name: '',
      accountType: 'CHECKING',
      bankName: '',
      accountNumber: '',
      routingNumber: '',
      balance: '0',
      isActive: true,
    })
  }

  const handleFormChange = (field: keyof AccountFormData, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value,
    }))
  }

  const validateForm = (): string | null => {
    if (!formData.name.trim()) return 'Le nom du compte est requis'
    if (!formData.bankName.trim()) return 'Le nom de la banque est requis'
    if (isNaN(parseFloat(formData.balance))) return 'Le solde doit être un nombre valide'
    return null
  }

  // Create account
  const handleCreateAccount = async () => {
    const validationError = validateForm()
    if (validationError) {
      setError(validationError)
      return
    }

    try {
      setError('')
      const newAccount = await apiClient.createAccount({
        name: formData.name.trim(),
        accountType: formData.accountType,
        bankName: formData.bankName.trim(),
        accountNumber: formData.accountNumber.trim() || undefined,
        routingNumber: formData.routingNumber.trim() || undefined,
        balance: parseFloat(formData.balance),
        isActive: formData.isActive,
      })
      
      setAccounts(prev => [...prev, newAccount])
      setIsCreateDialogOpen(false)
      resetForm()
      setSuccess('Compte créé avec succès')
      setTimeout(() => setSuccess(''), 3000)
    } catch (error: any) {
      setError(error.response?.data?.message || 'Erreur lors de la création du compte')
    }
  }

  // Edit account
  const openEditDialog = (account: Account) => {
    setEditingAccount(account)
    setFormData({
      name: account.name,
      accountType: account.accountType,
      bankName: account.bankName || '',
      accountNumber: account.accountNumber || '',
      routingNumber: account.routingNumber || '',
      balance: account.balance.toString(),
      isActive: account.isActive,
    })
    setIsEditDialogOpen(true)
  }

  const handleUpdateAccount = async () => {
    if (!editingAccount) return
    
    const validationError = validateForm()
    if (validationError) {
      setError(validationError)
      return
    }

    try {
      setError('')
      const updatedAccount = await apiClient.updateAccount(editingAccount.id, {
        name: formData.name.trim(),
        accountType: formData.accountType,
        bankName: formData.bankName.trim(),
        accountNumber: formData.accountNumber.trim() || undefined,
        routingNumber: formData.routingNumber.trim() || undefined,
        balance: parseFloat(formData.balance),
        isActive: formData.isActive,
      })
      
      setAccounts(prev => prev.map(acc => 
        acc.id === editingAccount.id ? updatedAccount : acc
      ))
      setIsEditDialogOpen(false)
      setEditingAccount(null)
      resetForm()
      setSuccess('Compte mis à jour avec succès')
      setTimeout(() => setSuccess(''), 3000)
    } catch (error: any) {
      setError(error.response?.data?.message || 'Erreur lors de la mise à jour du compte')
    }
  }

  // Delete account
  const openDeleteDialog = (account: Account) => {
    setDeletingAccount(account)
    setIsDeleteDialogOpen(true)
  }

  const handleDeleteAccount = async () => {
    if (!deletingAccount) return

    try {
      setError('')
      await apiClient.deleteAccount(deletingAccount.id)
      setAccounts(prev => prev.filter(acc => acc.id !== deletingAccount.id))
      setIsDeleteDialogOpen(false)
      setDeletingAccount(null)
      setSuccess('Compte supprimé avec succès')
      setTimeout(() => setSuccess(''), 3000)
    } catch (error: any) {
      setError(error.response?.data?.message || 'Erreur lors de la suppression du compte')
    }
  }

  // Delete all transactions
  const openDeleteTransactionsDialog = async (account: Account) => {
    try {
      setError('')
      // Get transaction count first
      const countResponse = await apiClient.getTransactionCountByAccount(account.id)
      setTransactionCount(countResponse.count)
      setDeletingTransactionsAccount(account)
      setIsDeleteTransactionsDialogOpen(true)
    } catch (error: any) {
      setError('Erreur lors de la récupération du nombre de transactions')
    }
  }

  const handleDeleteAllTransactions = async (forceDelete: boolean) => {
    if (!deletingTransactionsAccount) return

    try {
      setIsDeletingTransactions(true)
      setError('')
      
      const result = await apiClient.deleteAllTransactionsByAccount(
        deletingTransactionsAccount.id, 
        forceDelete
      )
      
      setIsDeleteTransactionsDialogOpen(false)
      setDeletingTransactionsAccount(null)
      setTransactionCount(0)
      setSuccess(`${result.deletedCount} transactions supprimées définitivement`)
      setTimeout(() => setSuccess(''), 5000)
      
    } catch (error: any) {
      setError(error.response?.data?.message || 'Erreur lors de la suppression des transactions')
    } finally {
      setIsDeletingTransactions(false)
    }
  }

  const toggleAccountNumberVisibility = (accountId: number) => {
    setShowAccountNumbers(prev => ({
      ...prev,
      [accountId]: !prev[accountId],
    }))
  }

  const maskAccountNumber = (accountNumber: string | undefined): string => {
    if (!accountNumber) return 'Non spécifié'
    if (accountNumber.length <= 4) return accountNumber
    return '*'.repeat(accountNumber.length - 4) + accountNumber.slice(-4)
  }

  const getAccountTypeIcon = (type: Account['accountType']) => {
    switch (type) {
      case 'CHECKING':
      case 'SAVINGS':
        return <CreditCard className="w-5 h-5" />
      case 'CREDIT_CARD':
        return <CreditCard className="w-5 h-5 text-orange-600" />
      case 'INVESTMENT':
        return <Building className="w-5 h-5 text-green-600" />
      case 'LOAN':
        return <Building className="w-5 h-5 text-red-600" />
      default:
        return <CreditCard className="w-5 h-5" />
    }
  }

  const getAccountTypeLabel = (type: Account['accountType']): string => {
    return ACCOUNT_TYPES.find(t => t.value === type)?.label || type
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-lg">Chargement des comptes...</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Gestion des Comptes</h1>
          <p className="text-gray-600">Gérez vos comptes bancaires et leurs informations</p>
        </div>
        
        <Button onClick={() => setIsCreateDialogOpen(true)}>
          <Plus className="w-4 h-4 mr-2" />
          Nouveau Compte
        </Button>
      </div>

      {/* Success/Error Messages */}
      {success && (
        <Alert variant="success">
          <CheckCircle className="w-4 h-4" />
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}
      
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="w-4 h-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Accounts List */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {accounts.map((account) => (
          <Card key={account.id} className={`${!account.isActive ? 'opacity-60' : ''}`}>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className={`p-2 rounded-lg ${
                    account.balance >= 0 ? 'bg-green-50' : 'bg-red-50'
                  }`}>
                    {getAccountTypeIcon(account.accountType)}
                  </div>
                  <div>
                    <CardTitle className="text-lg">{account.name}</CardTitle>
                    <CardDescription>
                      {getAccountTypeLabel(account.accountType)}
                      {!account.isActive && ' (Inactif)'}
                    </CardDescription>
                  </div>
                </div>
              </div>
            </CardHeader>
            
            <CardContent className="space-y-4">
              {/* Balance */}
              <div className="text-center py-4 border rounded-lg bg-gray-50">
                <div className={`text-2xl font-bold ${
                  account.balance >= 0 ? 'text-green-600' : 'text-red-600'
                }`}>
                  {formatCurrency(account.balance)}
                </div>
                <p className="text-sm text-gray-500">Solde actuel</p>
              </div>

              {/* Account Details */}
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500">Banque :</span>
                  <span className="font-medium">{account.bankName || 'Non spécifiée'}</span>
                </div>
                
                {account.accountNumber && (
                  <div className="flex justify-between items-center">
                    <span className="text-gray-500">N° de compte :</span>
                    <div className="flex items-center space-x-2">
                      <span className="font-mono text-xs">
                        {showAccountNumbers[account.id] 
                          ? account.accountNumber 
                          : maskAccountNumber(account.accountNumber)
                        }
                      </span>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => toggleAccountNumberVisibility(account.id)}
                        className="p-1 h-6 w-6"
                      >
                        {showAccountNumbers[account.id] ? (
                          <EyeOff className="w-3 h-3" />
                        ) : (
                          <Eye className="w-3 h-3" />
                        )}
                      </Button>
                    </div>
                  </div>
                )}

                <div className="flex justify-between">
                  <span className="text-gray-500">Créé le :</span>
                  <span>{new Date(account.createdAt).toLocaleDateString('fr-FR')}</span>
                </div>
              </div>

              {/* Actions */}
              <div className="space-y-2 pt-2">
                <div className="flex space-x-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => openEditDialog(account)}
                    className="flex-1"
                  >
                    <Edit className="w-4 h-4 mr-2" />
                    Modifier
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => openDeleteDialog(account)}
                    className="text-red-600 hover:text-red-700"
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => openDeleteTransactionsDialog(account)}
                  className="w-full text-orange-600 hover:text-orange-700 border-orange-200 hover:border-orange-300"
                >
                  <FileX className="w-4 h-4 mr-2" />
                  Supprimer toutes les transactions
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}

        {accounts.length === 0 && (
          <div className="col-span-full text-center py-12">
            <CreditCard className="w-16 h-16 mx-auto text-gray-400 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">Aucun compte</h3>
            <p className="text-gray-600 mb-4">Créez votre premier compte pour commencer</p>
            <Button onClick={() => setIsCreateDialogOpen(true)}>
              <Plus className="w-4 h-4 mr-2" />
              Créer un compte
            </Button>
          </div>
        )}
      </div>

      {/* Create Account Dialog */}
      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Créer un nouveau compte</DialogTitle>
            <DialogDescription>
              Ajoutez un nouveau compte bancaire à votre profil
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2">Nom du compte *</label>
              <Input
                value={formData.name}
                onChange={(e) => handleFormChange('name', e.target.value)}
                placeholder="Ex: Mon Compte Courant"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Type de compte *</label>
              <Select
                value={formData.accountType}
                onChange={(e) => handleFormChange('accountType', e.target.value)}
              >
                {ACCOUNT_TYPES.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </Select>
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Nom de la banque *</label>
              <Input
                value={formData.bankName}
                onChange={(e) => handleFormChange('bankName', e.target.value)}
                placeholder="Ex: BNP Paribas"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-2">Numéro de compte</label>
                <Input
                  value={formData.accountNumber}
                  onChange={(e) => handleFormChange('accountNumber', e.target.value)}
                  placeholder="123456789"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">Code banque</label>
                <Input
                  value={formData.routingNumber}
                  onChange={(e) => handleFormChange('routingNumber', e.target.value)}
                  placeholder="12345"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Solde initial</label>
              <Input
                type="number"
                step="0.01"
                value={formData.balance}
                onChange={(e) => handleFormChange('balance', e.target.value)}
                placeholder="0.00"
              />
            </div>

            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="isActive"
                checked={formData.isActive}
                onChange={(e) => handleFormChange('isActive', e.target.checked)}
                className="rounded"
              />
              <label htmlFor="isActive" className="text-sm">Compte actif</label>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
              Annuler
            </Button>
            <Button onClick={handleCreateAccount}>
              Créer le compte
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Account Dialog */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Modifier le compte</DialogTitle>
            <DialogDescription>
              Modifiez les informations du compte sélectionné
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2">Nom du compte *</label>
              <Input
                value={formData.name}
                onChange={(e) => handleFormChange('name', e.target.value)}
                placeholder="Ex: Mon Compte Courant"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Type de compte *</label>
              <Select
                value={formData.accountType}
                onChange={(e) => handleFormChange('accountType', e.target.value)}
              >
                {ACCOUNT_TYPES.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </Select>
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Nom de la banque *</label>
              <Input
                value={formData.bankName}
                onChange={(e) => handleFormChange('bankName', e.target.value)}
                placeholder="Ex: BNP Paribas"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-2">Numéro de compte</label>
                <Input
                  value={formData.accountNumber}
                  onChange={(e) => handleFormChange('accountNumber', e.target.value)}
                  placeholder="123456789"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">Code banque</label>
                <Input
                  value={formData.routingNumber}
                  onChange={(e) => handleFormChange('routingNumber', e.target.value)}
                  placeholder="12345"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Solde actuel</label>
              <Input
                type="number"
                step="0.01"
                value={formData.balance}
                onChange={(e) => handleFormChange('balance', e.target.value)}
                placeholder="0.00"
              />
            </div>

            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="isActiveEdit"
                checked={formData.isActive}
                onChange={(e) => handleFormChange('isActive', e.target.checked)}
                className="rounded"
              />
              <label htmlFor="isActiveEdit" className="text-sm">Compte actif</label>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>
              Annuler
            </Button>
            <Button onClick={handleUpdateAccount}>
              Mettre à jour
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Account Dialog */}
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Supprimer le compte</DialogTitle>
            <DialogDescription>
              Êtes-vous sûr de vouloir supprimer ce compte ? Cette action est irréversible.
            </DialogDescription>
          </DialogHeader>

          {deletingAccount && (
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-3">
                {getAccountTypeIcon(deletingAccount.accountType)}
                <div>
                  <div className="font-medium">{deletingAccount.name}</div>
                  <div className="text-sm text-gray-600">
                    {getAccountTypeLabel(deletingAccount.accountType)} - {deletingAccount.bankName}
                  </div>
                  <div className="text-sm font-semibold text-red-600">
                    Solde : {formatCurrency(deletingAccount.balance)}
                  </div>
                </div>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>
              Annuler
            </Button>
            <Button variant="destructive" onClick={handleDeleteAccount}>
              Supprimer définitivement
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete All Transactions Confirmation Dialog */}
      <ConfirmationDialog
        isOpen={isDeleteTransactionsDialogOpen}
        onClose={() => {
          setIsDeleteTransactionsDialogOpen(false)
          setDeletingTransactionsAccount(null)
          setTransactionCount(0)
        }}
        onConfirm={handleDeleteAllTransactions}
        title="Supprimer toutes les transactions"
        message="Vous êtes sur le point de supprimer définitivement toutes les transactions de ce compte."
        confirmText="Supprimer définitivement"
        cancelText="Annuler"
        isDestructive={true}
        isLoading={isDeletingTransactions}
        showForceOption={true}
        warningDetails={
          deletingTransactionsAccount
            ? {
                count: transactionCount,
                accountName: deletingTransactionsAccount.name,
              }
            : undefined
        }
      />
    </div>
  )
}