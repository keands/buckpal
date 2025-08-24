// Types based on Spring Boot DTOs

export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  createdAt: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
}

export interface JwtResponse {
  email: string
  firstName: string
  lastName: string
  accessToken: string
  tokenType: string
}

export interface Account {
  id: number
  name: string
  accountType: 'CHECKING' | 'SAVINGS' | 'CREDIT_CARD' | 'INVESTMENT' | 'LOAN' | 'OTHER'
  accountNumber?: string
  bankName?: string
  balance: number
  isActive: boolean
  createdAt: string
}

export interface Category {
  id: number
  name: string
  description?: string
  iconName?: string
  colorCode?: string
  isDefault: boolean
  parentCategory?: Category
  subCategories?: Category[]
}

export interface Transaction {
  id: number
  amount: number
  description?: string
  merchantName?: string
  transactionDate: string
  transactionType: 'INCOME' | 'EXPENSE' | 'TRANSFER'
  isPending: boolean
  accountId: number
  accountName: string
  categoryId?: number
  categoryName?: string
}

// CSV Import Types
export interface CsvUploadResponse {
  sessionId: string
  headers: string[]
  previewData: string[][]
  totalRows: number
}

export interface CsvColumnMappingRequest {
  sessionId: string
  accountId: number
  dateColumnIndex: number
  amountColumnIndex?: number
  debitColumnIndex?: number
  creditColumnIndex?: number
  descriptionColumnIndex?: number
  categoryColumnIndex?: number
  bankName?: string
  saveMapping: boolean
}

export interface TransactionPreview {
  rowIndex: number
  transactionDate: string
  amount: number
  description?: string
  category?: string
  transactionType: string
}

export interface ValidationError {
  rowIndex: number
  error: string
  rawData: string
  field: string
}

export interface DuplicateDetection {
  rowIndex: number
  transactionDate: string
  amount: number
  description?: string
  existingTransactionId: number
}

export interface CsvPreviewResponse {
  sessionId: string
  validTransactions: TransactionPreview[]
  validationErrors: ValidationError[]
  duplicateWarnings: DuplicateDetection[]
  totalProcessed: number
  validCount: number
  errorCount: number
  duplicateCount: number
}

export interface ManualCorrection {
  correctedDate?: string
  correctedAmount?: string
  correctedDescription?: string
  categoryId?: number
}

export interface CsvValidationRequest {
  sessionId: string
  approvedRows?: number[]
  rejectedRows?: number[]
  manualCorrections?: Record<number, ManualCorrection>
}

export interface CsvImportResult {
  sessionId: string
  totalProcessed: number
  successfulImports: number
  skippedRows: number
  failedImports: number
  errors: string[]
  importedTransactionIds: number[]
}

export interface CsvMappingTemplate {
  id: number
  bankName: string
  dateColumnIndex: number
  amountColumnIndex?: number
  debitColumnIndex?: number
  creditColumnIndex?: number
  descriptionColumnIndex?: number
  categoryColumnIndex?: number
  dateFormat?: string
}

// Calendar Types
export interface CalendarDay {
  date: string
  totalIncome: number
  totalExpense: number
  netAmount: number
  transactionCount: number
}

// Budget Types
export interface Budget {
  id: number
  budgetMonth: number
  budgetYear: number
  projectedIncome: number
  actualIncome: number
  totalAllocatedAmount: number
  totalSpentAmount: number
  budgetModel: string
  notes?: string
  isActive?: boolean
  createdAt?: string
  updatedAt?: string
  budgetCategories: BudgetCategory[]
  remainingAmount?: number
  usagePercentage?: number
  isOverBudget?: boolean
  
  // Computed properties for compatibility with frontend
  categories?: BudgetCategory[]
  name?: string
}

export interface BudgetCategory {
  id: number
  name: string
  allocatedAmount: number
  spentAmount: number
  categoryType: 'NEEDS' | 'WANTS' | 'SAVINGS' | 'EXPENSES' | 'HOUSING' | 'LIVING'
  colorCode?: string
  projectCategories?: ProjectCategory[]
  
  // Computed properties for compatibility
  remainingAmount?: number
  usagePercentage?: number
  isOverBudget?: boolean
}

export interface ProjectCategory {
  id: number
  name: string
  description?: string
  color?: string
  parentProjectCategory?: ProjectCategory
  childProjectCategories?: ProjectCategory[]
}