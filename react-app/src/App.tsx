import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, ProtectedRoute } from '@/contexts/auth-context'
import Navbar from '@/components/layout/navbar'

// Pages
import LoginPage from '@/pages/login'
import RegisterPage from '@/pages/register'
import DashboardPage from '@/pages/dashboard'
import AccountsPage from '@/pages/accounts'
import CsvImportPage from '@/pages/csv-import'
import CalendarPage from '@/pages/calendar'
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
                  <div>
                    <Navbar />
                    <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                      <DashboardPage />
                    </main>
                  </div>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/accounts"
              element={
                <ProtectedRoute>
                  <div>
                    <Navbar />
                    <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                      <AccountsPage />
                    </main>
                  </div>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/csv-import"
              element={
                <ProtectedRoute>
                  <div>
                    <Navbar />
                    <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                      <CsvImportPage />
                    </main>
                  </div>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/calendar"
              element={
                <ProtectedRoute>
                  <div>
                    <Navbar />
                    <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                      <CalendarPage />
                    </main>
                  </div>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/settings"
              element={
                <ProtectedRoute>
                  <div>
                    <Navbar />
                    <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                      <div className="text-center py-12">
                        <h1 className="text-2xl font-bold">Paramètres</h1>
                        <p className="text-gray-600 mt-2">Cette page sera implémentée prochainement</p>
                      </div>
                    </main>
                  </div>
                </ProtectedRoute>
              }
            />
            
            <Route
              path="/debug-auth"
              element={
                <ProtectedRoute>
                  <div>
                    <Navbar />
                    <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                      <DebugAuthPage />
                    </main>
                  </div>
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