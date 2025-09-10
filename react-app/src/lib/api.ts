import axios, { AxiosInstance, AxiosResponse } from 'axios'
import type {
  LoginRequest,
  RegisterRequest,
  JwtResponse,
  Account,
  Category,
  Transaction,
  CalendarDay,
  CsvUploadResponse,
  CsvColumnMappingRequest,
  CsvPreviewResponse,
  CsvValidationRequest,
  CsvImportResult,
  CsvMappingTemplate,
  Budget,
  BudgetCategoryTemplate,
  IncomeCategory,
  HistoricalIncomeAnalysis,
  IncomeComparison,
  IncomePattern,
  IncomeStatistics,
  IncomeSuggestion,
  SmartBudgetTemplate,
  WizardInsights,
  OnboardingStatus,
  OnboardingProgress,
  OnboardingConfig,
  AiAvailabilityResponse,
  OnboardingTips,
} from '@/types/api'

class ApiClient {
  private client: AxiosInstance
  private token: string | null = null

  constructor() {
    this.client = axios.create({
      baseURL: '/api', // Proxy configured in Vite
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // Request interceptor to add JWT token
    this.client.interceptors.request.use((config) => {
      if (this.token) {
        config.headers.Authorization = `Bearer ${this.token}`
      }
      return config
    })

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        // Only redirect to login on actual authentication errors
        if (error.response?.status === 401) {
          this.setToken(null)
          window.location.href = '/login'
        }
        // Don't redirect on other errors (404, 500, etc.)
        return Promise.reject(error)
      }
    )

    // Load token from localStorage on initialization
    const savedToken = localStorage.getItem('jwt_token')
    if (savedToken) {
      this.setToken(savedToken)
    }
  }

  setToken(token: string | null) {
    this.token = token
    if (token) {
      localStorage.setItem('jwt_token', token)
    } else {
      localStorage.removeItem('jwt_token')
    }
  }

  getToken(): string | null {
    return this.token
  }

  // Authentication
  async login(credentials: LoginRequest): Promise<JwtResponse> {
    const response: AxiosResponse<JwtResponse> = await this.client.post('/auth/signin', credentials)
    this.setToken(response.data.accessToken)
    return response.data
  }

  async register(userData: RegisterRequest): Promise<JwtResponse> {
    const response: AxiosResponse<JwtResponse> = await this.client.post('/auth/signup', userData)
    this.setToken(response.data.accessToken)
    return response.data
  }

  logout() {
    this.setToken(null)
    window.location.href = '/login'
  }

  // Accounts
  async getAccounts(): Promise<Account[]> {
    const response: AxiosResponse<Account[]> = await this.client.get('/accounts')
    return response.data
  }

  async getAccount(id: number): Promise<Account> {
    const response: AxiosResponse<Account> = await this.client.get(`/accounts/${id}`)
    return response.data
  }

  async createAccount(account: Partial<Account>): Promise<Account> {
    const response: AxiosResponse<Account> = await this.client.post('/accounts', account)
    return response.data
  }

  async updateAccount(id: number, account: Partial<Account>): Promise<Account> {
    const response: AxiosResponse<Account> = await this.client.put(`/accounts/${id}`, account)
    return response.data
  }

  async deleteAccount(id: number): Promise<void> {
    await this.client.delete(`/accounts/${id}`)
  }

  async recalculateAccountBalance(id: number): Promise<Account> {
    const response: AxiosResponse<Account> = await this.client.post(`/accounts/${id}/recalculate-balance`)
    return response.data
  }

  async recalculateAllAccountBalances(): Promise<string> {
    const response: AxiosResponse<string> = await this.client.post('/accounts/recalculate-all-balances')
    return response.data
  }

  // Transactions
  async getTransactions(accountId?: number, page = 0, size = 20): Promise<{
    content: Transaction[]
    totalElements: number
    totalPages: number
    size: number
    number: number
  }> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })
    
    if (accountId) {
      params.append('accountId', accountId.toString())
    }

    const response = await this.client.get(`/transactions?${params}`)
    return response.data
  }

  async getTransaction(id: number): Promise<Transaction> {
    const response: AxiosResponse<Transaction> = await this.client.get(`/transactions/${id}`)
    return response.data
  }

  async createTransaction(transaction: Partial<Transaction>): Promise<Transaction> {
    const response: AxiosResponse<Transaction> = await this.client.post('/transactions', transaction)
    return response.data
  }

  async updateTransaction(id: number, transaction: Partial<Transaction>): Promise<Transaction> {
    const response: AxiosResponse<Transaction> = await this.client.put(`/transactions/${id}`, transaction)
    return response.data
  }

  async deleteTransaction(id: number): Promise<void> {
    await this.client.delete(`/transactions/${id}`)
  }

  // Categories
  async getCategories(): Promise<Category[]> {
    const response: AxiosResponse<Category[]> = await this.client.get('/categories')
    return response.data
  }

  // CSV Import
  async uploadCsv(file: File): Promise<CsvUploadResponse> {
    const formData = new FormData()
    formData.append('file', file)

    const response: AxiosResponse<CsvUploadResponse> = await this.client.post(
      '/csv-import/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    )
    return response.data
  }

  async processCsvMapping(request: CsvColumnMappingRequest): Promise<CsvPreviewResponse> {
    const response: AxiosResponse<CsvPreviewResponse> = await this.client.post(
      '/csv-import/mapping',
      request
    )
    return response.data
  }

  async validateAndImportCsv(request: CsvValidationRequest): Promise<CsvImportResult> {
    const response: AxiosResponse<CsvImportResult> = await this.client.post(
      '/csv-import/validate',
      request
    )
    return response.data
  }

  async getCsvMappingTemplates(): Promise<CsvMappingTemplate[]> {
    const response: AxiosResponse<CsvMappingTemplate[]> = await this.client.get('/csv-import/templates')
    return response.data
  }

  async applyCsvMappingTemplate(bankName: string, sessionId: string): Promise<CsvColumnMappingRequest> {
    const response: AxiosResponse<CsvColumnMappingRequest> = await this.client.post(
      `/csv-import/templates/${encodeURIComponent(bankName)}/apply`,
      null,
      {
        params: { sessionId },
      }
    )
    return response.data
  }

  async downloadCsvTemplate(): Promise<Blob> {
    const response: AxiosResponse<Blob> = await this.client.get('/csv-import/template', {
      responseType: 'blob',
    })
    return response.data
  }

  // Calendar
  async getCalendarData(startDate: string, endDate: string): Promise<CalendarDay[]> {
    const response: AxiosResponse<CalendarDay[]> = await this.client.get('/transactions/calendar', {
      params: { startDate, endDate }
    })
    return response.data
  }

  async getTransactionsByDate(date: string): Promise<Transaction[]> {
    const response: AxiosResponse<Transaction[]> = await this.client.get(`/transactions/date/${date}`)
    return response.data
  }

  // Transaction Management
  async getTransactionCountByAccount(accountId: number): Promise<{ count: number }> {
    const response: AxiosResponse<{ count: number }> = await this.client.get(`/transactions/account/${accountId}/count`)
    return response.data
  }

  async deleteAllTransactionsByAccount(accountId: number, forceDelete: boolean = false): Promise<{ message: string; deletedCount: number }> {
    const response: AxiosResponse<{ message: string; deletedCount: number }> = await this.client.delete(
      `/transactions/account/${accountId}/all`,
      {
        params: { forceDelete }
      }
    )
    return response.data
  }

  // Budget Management
  async getBudgets(): Promise<Budget[]> {
    const response: AxiosResponse<Budget[]> = await this.client.get('/budgets')
    return response.data
  }

  async getBudget(id: number): Promise<Budget> {
    const response: AxiosResponse<Budget> = await this.client.get(`/budgets/${id}`)
    return response.data
  }

  async createBudget(budget: Partial<Budget>): Promise<Budget> {
    const response: AxiosResponse<Budget> = await this.client.post('/budgets', budget)
    return response.data
  }

  async updateBudget(id: number, budget: Partial<Budget>): Promise<Budget> {
    const response: AxiosResponse<Budget> = await this.client.put(`/budgets/${id}`, budget)
    return response.data
  }

  async deleteBudget(id: number): Promise<void> {
    await this.client.delete(`/budgets/${id}`)
  }

  async getTransactionsByCategory(budgetId: number, categoryId: number): Promise<Transaction[]> {
    const response: AxiosResponse<Transaction[]> = await this.client.get(`/budgets/${budgetId}/categories/${categoryId}/transactions`)
    return response.data
  }

  async createBudgetFromWizard(wizardData: any): Promise<Budget> {
    const response: AxiosResponse<Budget> = await this.client.post('/budgets/setup-wizard', wizardData)
    return response.data
  }

  async getPreviousMonthBudget(): Promise<Budget> {
    const response: AxiosResponse<Budget> = await this.client.get('/budgets/previous')
    return response.data
  }

  async getCurrentMonthBudget(): Promise<Budget> {
    const response: AxiosResponse<Budget> = await this.client.get('/budgets/current')
    return response.data
  }

  // Transaction Assignment


  async manuallyAssignTransaction(transactionId: number, budgetCategoryId: number): Promise<{ message: string; status: string }> {
    const response: AxiosResponse<{ message: string; status: string }> = await this.client.post('/transaction-assignments/manual-assign', {
      transactionId,
      budgetCategoryId
    })
    return response.data
  }

  async overrideAssignment(transactionId: number, budgetCategoryId: number): Promise<{ message: string; status: string }> {
    const response: AxiosResponse<{ message: string; status: string }> = await this.client.put(`/transaction-assignments/override/${transactionId}`, {
      budgetCategoryId
    })
    return response.data
  }

  async getTransactionsNeedingReview(budgetId?: number): Promise<Transaction[]> {
    const endpoint = budgetId 
      ? `/transaction-assignments/needs-review/${budgetId}`
      : '/transaction-assignments/needs-review'
    const response: AxiosResponse<Transaction[]> = await this.client.get(endpoint)
    return response.data
  }

  async getUnassignedTransactions(budgetId?: number): Promise<Transaction[]> {
    const endpoint = budgetId 
      ? `/transaction-assignments/unassigned/${budgetId}`
      : '/transaction-assignments/unassigned'
    const response: AxiosResponse<Transaction[]> = await this.client.get(endpoint)
    return response.data
  }

  // Budget Category Templates
  async getBudgetCategoryTemplates(): Promise<BudgetCategoryTemplate[]> {
    const response: AxiosResponse<BudgetCategoryTemplate[]> = await this.client.get('/budgets/category-templates')
    return response.data
  }

  // Smart Transaction Assignment
  async getSmartCategorySuggestion(transactionId: number): Promise<{
    transactionId: number
    suggestedCategory: string
    confidence: number
    strategy: string
    alternativeCategories: string[]
    merchantText: string
  }> {
    const response = await this.client.post(`/transaction-assignments/smart-suggest/${transactionId}`)
    return response.data
  }

  async submitSmartFeedback(transactionId: number, feedback: {
    suggestedCategory: string
    userChosenCategory: string
    wasAccepted: boolean
    patternUsed?: string
  }): Promise<{ message: string; transactionId: number }> {
    const response = await this.client.post(`/transaction-assignments/smart-feedback/${transactionId}`, feedback)
    return response.data
  }

  async getSmartSuggestionsBatch(transactionIds: number[]): Promise<{
    suggestions: Record<number, {
      suggestedCategory: string
      confidence: number
      strategy: string
      alternativeCategories: string[]
    }>
  }> {
    const response = await this.client.post('/transaction-assignments/smart-suggest-batch', {
      transactionIds
    })
    return response.data
  }

  // Transaction Revision
  async getRecentlyAssignedTransactions(): Promise<Transaction[]> {
    const response: AxiosResponse<Transaction[]> = await this.client.get('/transaction-assignments/recently-assigned')
    return response.data
  }

  async detectTransactionsNeedingRevision(): Promise<{
    suspiciousTransactions: Transaction[]
    count: number
  }> {
    const response = await this.client.get('/transaction-assignments/detect-revision-needed')
    return response.data
  }

  async autoDetectAndMarkForRevision(): Promise<{
    message: string
    totalSuspicious: number
    markedForRevision: number
    revisedTransactions: Transaction[]
  }> {
    const response = await this.client.post('/transaction-assignments/auto-detect-revision')
    return response.data
  }

  async markTransactionsForRevision(transactionIds: number[]): Promise<{
    message: string
    count: number
  }> {
    const response = await this.client.post('/transaction-assignments/mark-for-revision', {
      transactionIds
    })
    return response.data
  }

  async markTransactionForRevision(transactionId: number): Promise<{
    message: string
    transactionId: number
  }> {
    const response = await this.client.post(`/transaction-assignments/mark-for-revision/${transactionId}`)
    return response.data
  }

  // ====== INCOME MANAGEMENT ======

  // Income Categories
  async getIncomeCategories(budgetId: number): Promise<IncomeCategory[]> {
    const response: AxiosResponse<IncomeCategory[]> = await this.client.get(`/income/budgets/${budgetId}/categories`)
    return response.data
  }

  async createIncomeCategory(budgetId: number, category: Partial<IncomeCategory>): Promise<IncomeCategory> {
    const response: AxiosResponse<IncomeCategory> = await this.client.post(`/income/budgets/${budgetId}/categories`, category)
    return response.data
  }

  async updateIncomeCategory(categoryId: number, category: Partial<IncomeCategory>): Promise<IncomeCategory> {
    const response: AxiosResponse<IncomeCategory> = await this.client.put(`/income/categories/${categoryId}`, category)
    return response.data
  }

  async deleteIncomeCategory(categoryId: number): Promise<void> {
    await this.client.delete(`/income/categories/${categoryId}`)
  }

  async getIncomeCategory(categoryId: number): Promise<IncomeCategory> {
    const response: AxiosResponse<IncomeCategory> = await this.client.get(`/income/categories/${categoryId}`)
    return response.data
  }

  // Income Transactions (now using Transaction entity directly)
  async getIncomeTransactionsForCategory(categoryId: number): Promise<Transaction[]> {
    const response: AxiosResponse<Transaction[]> = await this.client.get(`/income/categories/${categoryId}/transactions`)
    return response.data
  }

  async getTotalIncomeForMonth(year: number, month: number): Promise<{ totalIncome: number }> {
    const response = await this.client.get(`/income/transactions/month/${year}/${month}/total`)
    return response.data
  }

  // Income Smart Assignment
  async suggestIncomeCategory(description: string): Promise<IncomeCategory> {
    const response: AxiosResponse<IncomeCategory> = await this.client.post('/income/transactions/suggest-category', { description })
    return response.data
  }

  async submitIncomeFeedback(feedback: {
    description: string
    suggestedCategoryId: number
    chosenCategoryId: number
  }): Promise<{ message: string }> {
    const response = await this.client.post('/income/transactions/feedback', feedback)
    return response.data
  }

  // Income Statistics
  async getIncomeStatistics(budgetId: number): Promise<IncomeStatistics> {
    const response = await this.client.get(`/income/budgets/${budgetId}/statistics`)
    return response.data
  }

  // ====== HISTORICAL INCOME ======

  async analyzeHistoricalIncome(year: number, month: number): Promise<HistoricalIncomeAnalysis> {
    const response: AxiosResponse<HistoricalIncomeAnalysis> = await this.client.get(`/budgets/historical-income/${year}/${month}`)
    return response.data
  }

  async shouldDetectHistoricalIncome(budgetId: number): Promise<{ shouldDetect: boolean }> {
    const response = await this.client.get(`/budgets/${budgetId}/should-detect-historical-income`)
    return response.data
  }

  async applyHistoricalIncome(budgetId: number, data: {
    suggestions: any[]
    usePartialIncome: boolean
  }): Promise<Budget> {
    const response: AxiosResponse<Budget> = await this.client.post(`/budgets/${budgetId}/apply-historical-income`, data)
    return response.data
  }

  // ====== TRANSACTION LINKING ======

  async getAvailableIncomeTransactions(budgetId?: number): Promise<Transaction[]> {
    const params = new URLSearchParams()
    if (budgetId) {
      params.append('budgetId', budgetId.toString())
    }
    
    const url = `/income/available-transactions${params.toString() ? `?${params.toString()}` : ''}`
    const response: AxiosResponse<Transaction[]> = await this.client.get(url)
    return response.data
  }

  async getSuggestedTransactionsForCategory(categoryId: number): Promise<Transaction[]> {
    const response: AxiosResponse<Transaction[]> = await this.client.get(`/income/categories/${categoryId}/suggested-transactions`)
    return response.data
  }

  async linkHistoricalTransactions(categoryId: number, transactionIds: number[]): Promise<Transaction[]> {
    const response: AxiosResponse<Transaction[]> = await this.client.post(`/income/categories/${categoryId}/link-transactions`, {
      transactionIds
    })
    return response.data
  }

  async unlinkTransactionsFromCategories(transactionIds: number[]): Promise<Transaction[]> {
    const response: AxiosResponse<Transaction[]> = await this.client.post('/income/transactions/unlink', {
      transactionIds
    })
    return response.data
  }

  async getTransactionsForIncomeCategory(categoryId: number): Promise<Transaction[]> {
    const response: AxiosResponse<Transaction[]> = await this.client.get(`/income/categories/${categoryId}/transactions`)
    return response.data
  }

  async getIncomeComparison(year: number, month: number, monthsBack: number = 6): Promise<IncomeComparison> {
    const response: AxiosResponse<IncomeComparison> = await this.client.get(`/budgets/income-comparison/${year}/${month}?monthsBack=${monthsBack}`)
    return response.data
  }

  // ====== INTELLIGENT BUDGET FEATURES ======

  async getIncomePatterns(monthsBack: number = 12): Promise<IncomePattern[]> {
    const response: AxiosResponse<IncomePattern[]> = await this.client.get(`/intelligent-budget/income-patterns?monthsBack=${monthsBack}`)
    return response.data
  }

  async suggestIncomeCategoryFromDescription(description: string): Promise<IncomeSuggestion[]> {
    const response: AxiosResponse<IncomeSuggestion[]> = await this.client.post('/intelligent-budget/suggest-income-category', {
      description
    })
    return response.data
  }

  async getSmartBudgetTemplate(): Promise<SmartBudgetTemplate> {
    const response: AxiosResponse<SmartBudgetTemplate> = await this.client.get('/intelligent-budget/smart-budget-template')
    return response.data
  }

  async getWizardInsights(): Promise<WizardInsights> {
    const response: AxiosResponse<WizardInsights> = await this.client.get('/intelligent-budget/wizard-insights')
    return response.data
  }

  async getRecurringIncomePatterns(year: number, month: number): Promise<IncomePattern[]> {
    const response: AxiosResponse<IncomePattern[]> = await this.client.get(`/budgets/recurring-patterns/${year}/${month}`)
    return response.data
  }

  // Detailed Categories API
  async getDetailedCategories(): Promise<Category[]> {
    const response: AxiosResponse<Category[]> = await this.client.get('/budgets/detailed-categories')
    return response.data
  }

  async getCategoryMapping(): Promise<Record<string, string>> {
    const response: AxiosResponse<Record<string, string>> = await this.client.get('/budgets/category-mapping')
    return response.data
  }

  async getBudgetCategoryDetailedDistribution(budgetId: number, categoryId: number): Promise<{
    categoryId: number
    categoryName: string
    totalSpent: number
    detailedDistribution: Record<string, {
      amount: number
      percentage: number
      transactionCount: number
      colorCode?: string
      iconName?: string
    }>
    uncategorizedCount: number
    categorizedPercentage: string
  }> {
    const response = await this.client.get(`/budgets/${budgetId}/categories/${categoryId}/detailed-distribution`)
    return response.data
  }

  // Simplified: Only need detailed category, budget category is determined via mapping
  async assignTransactionToDetailedCategory(transactionId: number, detailedCategoryId: number): Promise<{
    message: string
    status: string
  }> {
    const response = await this.client.post(`/transaction-assignments/assign-detailed`, {
      transactionId,
      detailedCategoryId
    })
    return response.data
  }


  // ====== CATEGORY MAPPINGS ======
  
  // Get categories grouped by budget category
  async getCategoriesGroupedByBudgetCategory(): Promise<Record<string, Category[]>> {
    const response = await this.client.get('/category-mappings/grouped')
    return response.data
  }

  // Get budget category for detailed category
  async getBudgetCategoryForDetailed(detailedCategoryId: number): Promise<string> {
    const response = await this.client.get(`/category-mappings/budget-category/${detailedCategoryId}`)
    return response.data
  }

  // Update category mapping
  async updateCategoryMapping(detailedCategoryId: number, budgetCategoryKey: string): Promise<{success: string, message: string}> {
    const response = await this.client.put('/category-mappings/update-mapping', {
      detailedCategoryId,
      budgetCategoryKey
    })
    return response.data
  }

  // Create custom category with mapping
  async createCustomCategory(categoryData: {
    name: string
    description?: string
    budgetCategoryKey: string
    iconName?: string
    colorCode?: string
  }): Promise<Category> {
    const response = await this.client.post('/category-mappings/create-custom-category', categoryData)
    return response.data
  }

  // Get unmapped categories
  async getUnmappedCategories(): Promise<Category[]> {
    const response = await this.client.get('/category-mappings/unmapped')
    return response.data
  }

  // Initialize default mappings (admin)
  async initializeDefaultMappings(): Promise<{success: string, message: string}> {
    const response = await this.client.post('/category-mappings/initialize-defaults')
    return response.data
  }

  // Delete custom category
  async deleteCustomCategory(categoryId: number): Promise<{success: string, message: string}> {
    const response = await this.client.delete(`/category-mappings/delete-custom-category/${categoryId}`)
    return response.data
  }

  // Update custom category
  async updateCustomCategory(categoryId: number, categoryData: {
    name: string
    description?: string
    budgetCategoryKey: string
    iconName?: string
    colorCode?: string
  }): Promise<Category> {
    const response = await this.client.put(`/category-mappings/update-custom-category/${categoryId}`, categoryData)
    return response.data
  }

  // Category Management (Legacy)
  async createCategory(categoryData: {
    name: string
    description?: string
    iconName?: string
    colorCode?: string
  }): Promise<Category> {
    const response: AxiosResponse<Category> = await this.client.post('/categories', categoryData)
    return response.data
  }

  async updateCategory(id: number, categoryData: {
    name: string
    description?: string
    iconName?: string
    colorCode?: string
  }): Promise<Category> {
    const response: AxiosResponse<Category> = await this.client.put(`/categories/${id}`, categoryData)
    return response.data
  }

  async deleteCategory(id: number): Promise<void> {
    await this.client.delete(`/categories/${id}`)
  }

  async getAvailableIcons(): Promise<string[]> {
    const response: AxiosResponse<string[]> = await this.client.get('/categories/icons')
    return response.data
  }

  async getAvailableColors(): Promise<string[]> {
    const response: AxiosResponse<string[]> = await this.client.get('/categories/colors')
    return response.data
  }

  // Onboarding methods
  async getOnboardingStatus(): Promise<OnboardingStatus> {
    const response: AxiosResponse<OnboardingStatus> = await this.client.get('/onboarding/status')
    return response.data
  }

  async getOnboardingProgress(): Promise<OnboardingProgress> {
    const response: AxiosResponse<OnboardingProgress> = await this.client.get('/onboarding/progress')
    return response.data
  }

  async checkAiAvailability(): Promise<AiAvailabilityResponse> {
    const response: AxiosResponse<AiAvailabilityResponse> = await this.client.get('/onboarding/ai-availability')
    return response.data
  }

  async getOnboardingConfig(): Promise<OnboardingConfig> {
    const response: AxiosResponse<OnboardingConfig> = await this.client.get('/onboarding/config')
    return response.data
  }

  async acknowledgeAiFeature(accepted: boolean = false): Promise<{ acknowledged: boolean; aiAccepted: boolean; message: string }> {
    const response = await this.client.post(`/onboarding/acknowledge-ai?accepted=${accepted}`)
    return response.data
  }

  async getOnboardingTips(): Promise<OnboardingTips> {
    const response: AxiosResponse<OnboardingTips> = await this.client.get('/onboarding/tips')
    return response.data
  }

  // ====== RECURRING PAYMENTS ======

  async getRecurringPayments(): Promise<any[]> {
    const response = await this.client.get('/recurring-payments')
    return response.data
  }

  async getRecurringPaymentTypes(): Promise<Record<string, any>> {
    const response = await this.client.get('/recurring-payments/payment-types')
    return response.data
  }

  async getRecurringPaymentFrequencies(): Promise<Record<string, any>> {
    const response = await this.client.get('/recurring-payments/frequencies')
    return response.data
  }

  async getRecurringPaymentStatistics(): Promise<any> {
    const response = await this.client.get('/recurring-payments/statistics')
    return response.data
  }

  async createRecurringPayment(paymentData: any): Promise<any> {
    const response = await this.client.post('/recurring-payments', paymentData)
    return response.data
  }

  async updateRecurringPayment(id: number, paymentData: any): Promise<any> {
    const response = await this.client.put(`/recurring-payments/${id}`, paymentData)
    return response.data
  }

  async deleteRecurringPayment(id: number): Promise<void> {
    await this.client.delete(`/recurring-payments/${id}`)
  }

  async getBudgetMultiMonthProjection(startDate: string, monthsAhead: number): Promise<any> {
    const response = await this.client.get(`/recurring-payments/budget-projection?startDate=${startDate}&monthsAhead=${monthsAhead}`)
    return response.data
  }
}

// Export a singleton instance
export const apiClient = new ApiClient()

// Export the class for testing purposes
export { ApiClient }