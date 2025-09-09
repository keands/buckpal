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
import SettingsPage from '@/pages/settings'
import RecurringPaymentsPage from '@/pages/RecurringPayments'
import DebugAuthPage from '@/pages/debug-auth'
import ResetAuthPage from '@/pages/reset-auth'

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
              path="/settings"
              element={
                <ProtectedRoute>
                  <ProtectedLayout>
                    <SettingsPage />
                  </ProtectedLayout>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/recurring-payments"
              element={
                <ProtectedRoute>
                  <ProtectedLayout>
                    <RecurringPaymentsPage />
                  </ProtectedLayout>
                </ProtectedRoute>
              }
            />
            
            {/* Redirect old categories route to settings */}
            <Route
              path="/categories"
              element={<Navigate to="/settings?tab=categories" replace />}
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