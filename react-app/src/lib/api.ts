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
}

// Export a singleton instance
export const apiClient = new ApiClient()

// Export the class for testing purposes
export { ApiClient }