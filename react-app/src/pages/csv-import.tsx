import { useState, useEffect } from 'react'
import { apiClient } from '@/lib/api'
import { formatCurrency } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { 
  Upload, 
  FileText, 
  Settings, 
  CheckCircle, 
  AlertCircle,
  Download,
  ArrowLeft,
  ArrowRight
} from 'lucide-react'
import type { 
  Account, 
  CsvUploadResponse, 
  CsvColumnMappingRequest, 
  CsvPreviewResponse,
  CsvValidationRequest,
  CsvImportResult,
  CsvMappingTemplate
} from '@/types/api'

type Step = 'upload' | 'mapping' | 'preview' | 'validation' | 'result'

export default function CsvImportPage() {
  const [currentStep, setCurrentStep] = useState<Step>('upload')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  
  // Data states
  const [accounts, setAccounts] = useState<Account[]>([])
  const [templates, setTemplates] = useState<CsvMappingTemplate[]>([])
  const [uploadResponse, setUploadResponse] = useState<CsvUploadResponse | null>(null)
  const [previewResponse, setPreviewResponse] = useState<CsvPreviewResponse | null>(null)
  const [importResult, setImportResult] = useState<CsvImportResult | null>(null)
  
  // Form states
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [mapping, setMapping] = useState<CsvColumnMappingRequest>({
    sessionId: '',
    accountId: 0,
    dateColumnIndex: 0,
    amountColumnIndex: undefined,
    debitColumnIndex: undefined,
    creditColumnIndex: undefined,
    descriptionColumnIndex: undefined,
    categoryColumnIndex: undefined,
    bankName: '',
    saveMapping: false,
  })
  const [validationChoices, setValidationChoices] = useState<{
    approvedRows: number[]
    rejectedRows: number[]
    manualCorrections: Record<number, any>
  }>({
    approvedRows: [],
    rejectedRows: [],
    manualCorrections: {},
  })

  useEffect(() => {
    loadInitialData()
  }, [])

  const loadInitialData = async () => {
    try {
      console.log('Loading initial data for CSV import...')
      const [accountsData, templatesData] = await Promise.all([
        apiClient.getAccounts(),
        apiClient.getCsvMappingTemplates(),
      ])
      console.log('Accounts loaded:', accountsData)
      console.log('Templates loaded:', templatesData)
      setAccounts(accountsData)
      setTemplates(templatesData)
    } catch (error) {
      console.error('Error loading initial data:', error)
      setError('Erreur lors du chargement des données: ' + (error as any)?.message)
    }
  }

  // Step 1: File Upload
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file && file.type === 'text/csv') {
      setSelectedFile(file)
      setError('')
    } else {
      setError('Veuillez sélectionner un fichier CSV valide')
    }
  }

  const handleUpload = async () => {
    if (!selectedFile) return

    setIsLoading(true)
    setError('')
    
    try {
      const response = await apiClient.uploadCsv(selectedFile)
      setUploadResponse(response)
      setMapping(prev => ({
        ...prev,
        sessionId: response.sessionId,
      }))
      setCurrentStep('mapping')
    } catch (error: any) {
      setError(error.response?.data?.message || 'Erreur lors de l\'upload du fichier')
    } finally {
      setIsLoading(false)
    }
  }

  // Step 2: Column Mapping
  const handleMappingChange = (field: keyof CsvColumnMappingRequest, value: any) => {
    setMapping(prev => ({
      ...prev,
      [field]: value,
    }))
  }

  const applyTemplate = async (template: CsvMappingTemplate) => {
    if (!uploadResponse) return

    try {
      const appliedMapping = await apiClient.applyCsvMappingTemplate(
        template.bankName,
        uploadResponse.sessionId
      )
      setMapping({
        ...appliedMapping,
        accountId: mapping.accountId, // Keep selected account
      })
    } catch (error: any) {
      setError(error.response?.data?.message || 'Erreur lors de l\'application du template')
    }
  }

  const handleProcessMapping = async () => {
    if (!mapping.accountId) {
      setError('Veuillez sélectionner un compte')
      return
    }

    setIsLoading(true)
    setError('')

    try {
      const response = await apiClient.processCsvMapping(mapping)
      setPreviewResponse(response)
      
      // Initialize validation choices with ALL valid transactions approved by default
      // Note: We need to approve all valid transactions, not just the preview ones
      // Since we don't have all row indices from preview, we'll approve all by setting approvedRows to null
      // The backend will import all valid transactions when approvedRows is null
      setValidationChoices({
        approvedRows: [], // We'll modify backend to import all when this is empty and no rejections
        rejectedRows: [],
        manualCorrections: {},
      })
      
      setCurrentStep('preview')
    } catch (error: any) {
      setError(error.response?.data?.message || 'Erreur lors du traitement du mapping')
    } finally {
      setIsLoading(false)
    }
  }

  // Step 3 & 4: Preview and Validation
  const toggleRowApproval = (rowIndex: number) => {
    setValidationChoices(prev => {
      const isApproved = prev.approvedRows.includes(rowIndex)
      const isRejected = prev.rejectedRows.includes(rowIndex)

      if (isApproved) {
        // Move from approved to rejected
        return {
          ...prev,
          approvedRows: prev.approvedRows.filter(r => r !== rowIndex),
          rejectedRows: [...prev.rejectedRows, rowIndex],
        }
      } else if (isRejected) {
        // Move from rejected to approved
        return {
          ...prev,
          rejectedRows: prev.rejectedRows.filter(r => r !== rowIndex),
          approvedRows: [...prev.approvedRows, rowIndex],
        }
      } else {
        // Add to approved
        return {
          ...prev,
          approvedRows: [...prev.approvedRows, rowIndex],
        }
      }
    })
  }

  // Step 5: Final Import
  const handleFinalImport = async () => {
    if (!previewResponse) return

    setIsLoading(true)
    setError('')

    try {
      const validationRequest: CsvValidationRequest = {
        sessionId: previewResponse.sessionId,
        approvedRows: validationChoices.approvedRows,
        rejectedRows: validationChoices.rejectedRows,
        manualCorrections: validationChoices.manualCorrections,
      }

      const result = await apiClient.validateAndImportCsv(validationRequest)
      setImportResult(result)
      setCurrentStep('result')
    } catch (error: any) {
      setError(error.response?.data?.message || 'Erreur lors de l\'import final')
    } finally {
      setIsLoading(false)
    }
  }

  const resetWizard = () => {
    setCurrentStep('upload')
    setSelectedFile(null)
    setUploadResponse(null)
    setPreviewResponse(null)
    setImportResult(null)
    setError('')
    setMapping({
      sessionId: '',
      accountId: 0,
      dateColumnIndex: 0,
      amountColumnIndex: undefined,
      debitColumnIndex: undefined,
      creditColumnIndex: undefined,
      descriptionColumnIndex: undefined,
      categoryColumnIndex: undefined,
      bankName: '',
      saveMapping: false,
    })
  }

  const stepNames = {
    upload: 'Upload du fichier',
    mapping: 'Mapping des colonnes',
    preview: 'Prévisualisation',
    validation: 'Validation',
    result: 'Résultats'
  }

  const stepIndex = {
    upload: 0,
    mapping: 1,
    preview: 2,
    validation: 3,
    result: 4
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Import CSV</h1>
        <p className="text-gray-600">Importez vos transactions depuis un fichier CSV</p>
      </div>

      {/* Progress Bar */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center justify-between mb-4">
            {Object.entries(stepNames).map(([key, name], index) => (
              <div key={key} className="flex items-center">
                <div className={`flex items-center justify-center w-8 h-8 rounded-full ${
                  stepIndex[currentStep] >= index 
                    ? 'bg-primary text-white' 
                    : 'bg-gray-200 text-gray-600'
                }`}>
                  {stepIndex[currentStep] > index ? (
                    <CheckCircle className="w-4 h-4" />
                  ) : (
                    <span className="text-sm font-medium">{index + 1}</span>
                  )}
                </div>
                {index < Object.keys(stepNames).length - 1 && (
                  <div className={`w-12 h-0.5 ml-2 ${
                    stepIndex[currentStep] > index ? 'bg-primary' : 'bg-gray-200'
                  }`} />
                )}
              </div>
            ))}
          </div>
          <Progress value={(stepIndex[currentStep] + 1) * 20} className="h-2" />
          <p className="text-center mt-2 text-sm font-medium">
            {stepNames[currentStep]}
          </p>
        </CardContent>
      </Card>

      {/* Error Display */}
      {error && (
        <Card className="border-red-200">
          <CardContent className="pt-6">
            <div className="flex items-center space-x-2 text-red-600">
              <AlertCircle className="w-5 h-5" />
              <span>{error}</span>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Step Content */}
      {currentStep === 'upload' && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Upload className="w-5 h-5" />
              <span>Sélectionner un fichier CSV</span>
            </CardTitle>
            <CardDescription>
              Choisissez le fichier CSV contenant vos transactions bancaires
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Input
                type="file"
                accept=".csv"
                onChange={handleFileSelect}
                className="cursor-pointer"
              />
            </div>

            {selectedFile && (
              <div className="p-4 bg-gray-50 rounded-lg">
                <div className="flex items-center space-x-2">
                  <FileText className="w-4 h-4 text-gray-600" />
                  <span className="text-sm font-medium">{selectedFile.name}</span>
                  <span className="text-sm text-gray-500">
                    ({(selectedFile.size / 1024).toFixed(1)} KB)
                  </span>
                </div>
              </div>
            )}

            <div className="flex justify-between">
              <Button 
                variant="outline" 
                onClick={() => apiClient.downloadCsvTemplate()}
              >
                <Download className="w-4 h-4 mr-2" />
                Télécharger un exemple CSV
              </Button>

              <Button 
                onClick={handleUpload} 
                disabled={!selectedFile || isLoading}
              >
                {isLoading ? 'Upload...' : 'Continuer'}
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {currentStep === 'mapping' && uploadResponse && (
        <div className="space-y-6">
          {/* CSV Preview */}
          <Card>
            <CardHeader>
              <CardTitle>Aperçu du fichier CSV</CardTitle>
              <CardDescription>
                {uploadResponse.totalRows} lignes détectées
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="min-w-full text-sm">
                  <thead>
                    <tr className="border-b">
                      {uploadResponse.headers.map((header, index) => (
                        <th key={index} className="text-left p-2 font-medium">
                          Colonne {index}: {header}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {uploadResponse.previewData.slice(0, 5).map((row, rowIndex) => (
                      <tr key={rowIndex} className="border-b">
                        {row.map((cell, cellIndex) => (
                          <td key={cellIndex} className="p-2 text-gray-700">
                            {cell || '(vide)'}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>

          {/* Mapping Configuration */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Settings className="w-5 h-5" />
                <span>Configuration du mapping</span>
              </CardTitle>
              <CardDescription>
                Indiquez quelle colonne correspond à chaque type de donnée
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Template Selection */}
              {templates.length > 0 && (
                <div>
                  <label className="block text-sm font-medium mb-2">
                    Utiliser un template existant
                  </label>
                  <div className="flex space-x-2">
                    {templates.map((template) => (
                      <Button
                        key={template.id}
                        variant="outline"
                        size="sm"
                        onClick={() => applyTemplate(template)}
                      >
                        {template.bankName}
                      </Button>
                    ))}
                  </div>
                </div>
              )}

              {/* Account Selection */}
              <div>
                <label className="block text-sm font-medium mb-2">
                  Compte de destination *
                </label>
                <div className="space-y-2">
                  <Select
                    value={mapping.accountId || ''}
                    onChange={(e) => handleMappingChange('accountId', parseInt(e.target.value))}
                  >
                    <option value="">
                      {accounts.length === 0 ? 'Aucun compte disponible' : 'Sélectionner un compte'}
                    </option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.name} - {account.bankName || account.accountType}
                      </option>
                    ))}
                  </Select>
                  {accounts.length === 0 && (
                    <div className="flex items-center space-x-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={loadInitialData}
                        disabled={isLoading}
                      >
                        Recharger les comptes
                      </Button>
                      <span className="text-sm text-gray-500">
                        Aucun compte trouvé
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Column Mapping */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2">
                    Colonne Date *
                  </label>
                  <Select
                    value={mapping.dateColumnIndex}
                    onChange={(e) => handleMappingChange('dateColumnIndex', parseInt(e.target.value))}
                  >
                    {uploadResponse.headers.map((header, index) => (
                      <option key={index} value={index}>
                        Colonne {index}: {header}
                      </option>
                    ))}
                  </Select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">
                    Colonne Description
                  </label>
                  <Select
                    value={mapping.descriptionColumnIndex || ''}
                    onChange={(e) => handleMappingChange('descriptionColumnIndex', e.target.value ? parseInt(e.target.value) : undefined)}
                  >
                    <option value="">Aucune</option>
                    {uploadResponse.headers.map((header, index) => (
                      <option key={index} value={index}>
                        Colonne {index}: {header}
                      </option>
                    ))}
                  </Select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">
                    Colonne Montant unique
                  </label>
                  <Select
                    value={mapping.amountColumnIndex || ''}
                    onChange={(e) => handleMappingChange('amountColumnIndex', e.target.value ? parseInt(e.target.value) : undefined)}
                  >
                    <option value="">Aucune</option>
                    {uploadResponse.headers.map((header, index) => (
                      <option key={index} value={index}>
                        Colonne {index}: {header}
                      </option>
                    ))}
                  </Select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">
                    Colonne Catégorie
                  </label>
                  <Select
                    value={mapping.categoryColumnIndex || ''}
                    onChange={(e) => handleMappingChange('categoryColumnIndex', e.target.value ? parseInt(e.target.value) : undefined)}
                  >
                    <option value="">Aucune</option>
                    {uploadResponse.headers.map((header, index) => (
                      <option key={index} value={index}>
                        Colonne {index}: {header}
                      </option>
                    ))}
                  </Select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">
                    Colonne Débit
                  </label>
                  <Select
                    value={mapping.debitColumnIndex || ''}
                    onChange={(e) => handleMappingChange('debitColumnIndex', e.target.value ? parseInt(e.target.value) : undefined)}
                  >
                    <option value="">Aucune</option>
                    {uploadResponse.headers.map((header, index) => (
                      <option key={index} value={index}>
                        Colonne {index}: {header}
                      </option>
                    ))}
                  </Select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">
                    Colonne Crédit
                  </label>
                  <Select
                    value={mapping.creditColumnIndex || ''}
                    onChange={(e) => handleMappingChange('creditColumnIndex', e.target.value ? parseInt(e.target.value) : undefined)}
                  >
                    <option value="">Aucune</option>
                    {uploadResponse.headers.map((header, index) => (
                      <option key={index} value={index}>
                        Colonne {index}: {header}
                      </option>
                    ))}
                  </Select>
                </div>
              </div>

              {/* Save Template */}
              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="saveMapping"
                  checked={mapping.saveMapping}
                  onChange={(e) => handleMappingChange('saveMapping', e.target.checked)}
                  className="rounded"
                />
                <label htmlFor="saveMapping" className="text-sm">
                  Sauvegarder ce mapping pour réutilisation future
                </label>
              </div>

              {mapping.saveMapping && (
                <div>
                  <label className="block text-sm font-medium mb-2">
                    Nom de la banque
                  </label>
                  <Input
                    value={mapping.bankName || ''}
                    onChange={(e) => handleMappingChange('bankName', e.target.value)}
                    placeholder="Ex: BNP Paribas"
                  />
                </div>
              )}

              <div className="flex justify-between pt-4">
                <Button variant="outline" onClick={() => setCurrentStep('upload')}>
                  <ArrowLeft className="w-4 h-4 mr-2" />
                  Retour
                </Button>

                <Button onClick={handleProcessMapping} disabled={isLoading || !mapping.accountId}>
                  {isLoading ? 'Traitement...' : 'Prévisualiser'}
                  <ArrowRight className="w-4 h-4 ml-2" />
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Continue with Preview and Result steps... */}
      {currentStep === 'preview' && previewResponse && (
        <Card>
          <CardHeader>
            <CardTitle>Prévisualisation des transactions</CardTitle>
            <CardDescription>
              Aperçu de {previewResponse.validTransactions.length} transactions sur {previewResponse.validCount} valides trouvées • {previewResponse.errorCount} erreurs • {previewResponse.duplicateCount} doublons détectés
            </CardDescription>
          </CardHeader>
          <CardContent>
            {/* Preview Information */}
            {previewResponse.validCount > previewResponse.validTransactions.length && (
              <div className="mb-4 p-3 bg-blue-50 rounded-lg border border-blue-200">
                <div className="text-sm text-blue-800">
                  <strong>Aperçu équilibré :</strong> Cette prévisualisation montre {previewResponse.validTransactions.length} transactions représentatives 
                  (incluant revenus et dépenses) pour vérifier le format. Par défaut, toutes les {previewResponse.validCount} transactions valides 
                  seront importées. Vous pouvez approuver/rejeter individuellement les transactions de l'aperçu si nécessaire.
                </div>
              </div>
            )}
            
            <div className="space-y-4 max-h-96 overflow-y-auto">
              {previewResponse.validTransactions.map((transaction) => (
                <div
                  key={transaction.rowIndex}
                  className={`p-3 border rounded-lg ${
                    validationChoices.approvedRows.includes(transaction.rowIndex)
                      ? 'border-green-200 bg-green-50'
                      : validationChoices.rejectedRows.includes(transaction.rowIndex)
                      ? 'border-red-200 bg-red-50'
                      : 'border-gray-200'
                  }`}
                >
                  <div className="flex justify-between items-center">
                    <div>
                      <div className="font-medium">{transaction.description}</div>
                      <div className="text-sm text-gray-500">
                        {new Date(transaction.transactionDate).toLocaleDateString('fr-FR')} - 
                        Ligne {transaction.rowIndex}
                      </div>
                    </div>
                    <div className="flex items-center space-x-4">
                      <span className={`font-semibold ${
                        transaction.transactionType === 'INCOME' ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {formatCurrency(transaction.amount)}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => toggleRowApproval(transaction.rowIndex)}
                      >
                        {validationChoices.approvedRows.includes(transaction.rowIndex)
                          ? 'Approuvé ✓'
                          : validationChoices.rejectedRows.includes(transaction.rowIndex)
                          ? 'Rejeté ✗'
                          : 'Sélectionner'
                        }
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className="flex justify-between pt-4">
              <Button variant="outline" onClick={() => setCurrentStep('mapping')}>
                <ArrowLeft className="w-4 h-4 mr-2" />
                Retour
              </Button>

              <Button onClick={handleFinalImport} disabled={isLoading}>
                {isLoading ? 'Import...' : 
                 validationChoices.approvedRows.length > 0 
                   ? `Importer ${validationChoices.approvedRows.length} transactions sélectionnées`
                   : `Importer toutes les transactions valides (${previewResponse.validCount})`
                }
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {currentStep === 'result' && importResult && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <CheckCircle className="w-5 h-5 text-green-600" />
              <span>Import terminé</span>
            </CardTitle>
            <CardDescription>
              Résultats de l'importation CSV
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="text-center p-4 bg-green-50 rounded-lg">
                <div className="text-2xl font-bold text-green-600">
                  {importResult.successfulImports}
                </div>
                <div className="text-sm text-gray-600">Importées</div>
              </div>
              <div className="text-center p-4 bg-gray-50 rounded-lg">
                <div className="text-2xl font-bold text-gray-600">
                  {importResult.skippedRows}
                </div>
                <div className="text-sm text-gray-600">Ignorées</div>
              </div>
              <div className="text-center p-4 bg-red-50 rounded-lg">
                <div className="text-2xl font-bold text-red-600">
                  {importResult.failedImports}
                </div>
                <div className="text-sm text-gray-600">Échecs</div>
              </div>
              <div className="text-center p-4 bg-blue-50 rounded-lg">
                <div className="text-2xl font-bold text-blue-600">
                  {importResult.totalProcessed}
                </div>
                <div className="text-sm text-gray-600">Total traité</div>
              </div>
            </div>

            {importResult.errors.length > 0 && (
              <div className="p-4 bg-red-50 rounded-lg">
                <h4 className="font-medium text-red-800 mb-2">Erreurs rencontrées :</h4>
                <ul className="text-sm text-red-700 space-y-1">
                  {importResult.errors.map((error, index) => (
                    <li key={index}>• {error}</li>
                  ))}
                </ul>
              </div>
            )}

            <div className="flex justify-between">
              <Button variant="outline" onClick={resetWizard}>
                Nouveau import
              </Button>
              <Button asChild>
                <a href="/dashboard">Voir le dashboard</a>
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}