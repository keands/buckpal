import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, ProtectedRoute } from '@/contexts/auth-context'
import { ProtectedLayout } from '@/components/layout/protected-layout'

// Pages
import LoginPage from '@/pages/login'
import RegisterPage from '@/pages/register'
import DashboardPage from '@/pages/dashboard'
import AccountsPage from '@/pages/accounts'
import CsvImportPage from '@/pages/csv-import'
import CalendarPage from '@/pages/calendar'
import BudgetPage from '@/pages/budget'
import DebugAuthPage from '@/pages/debug-auth'
import ResetAuthPage from '@/pages/reset-auth'
import { CategoryManagement } from '@/components/budget/category-management'

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="min-h-screen bg-gray-50">
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            
            {/* Protected routes */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <ProtectedLayout>
                    <DashboardPage />
                  </ProtectedLayout>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/accounts"
              element={
                <ProtectedRoute>
                  <ProtectedLayout>
                    <AccountsPage />
                  </ProtectedLayout>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/csv-import"
              element={
                <ProtectedRoute>
                  <ProtectedLayout>
                    <CsvImportPage />
                  </ProtectedLayout>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/calendar"
              element={
                <ProtectedRoute>
                  <ProtectedLayout>
                    <CalendarPage />
                  </ProtectedLayout>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/budget"
              element={
                <ProtectedRoute>
                  <ProtectedLayout>
                    <BudgetPage />
                  </ProtectedLayout>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/categories"
              element={
                <ProtectedRoute>
                  <ProtectedLayout>
                    <CategoryManagement />
                  </ProtectedLayout>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/settings"
              element={
                <ProtectedRoute>
                  <ProtectedLayout>
                    <div className="text-center py-12">
                      <h1 className="text-2xl font-bold">Paramètres</h1>
                      <p className="text-gray-600 mt-2">Cette page sera implémentée prochainement</p>
                    </div>
                  </ProtectedLayout>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/debug-auth"
              element={
                <ProtectedRoute>
                  <ProtectedLayout>
                    <DebugAuthPage />
                  </ProtectedLayout>
                </ProtectedRoute>
              }
            />
            
            {/* Public route for auth reset */}
            <Route path="/reset-auth" element={<ResetAuthPage />} />
          </Routes>
        </div>
      </Router>
    </AuthProvider>
  )
}

export default App