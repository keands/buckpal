import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { apiClient } from '@/lib/api'
import type { User, LoginRequest, RegisterRequest } from '@/types/api'

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (credentials: LoginRequest) => Promise<void>
  register: (userData: RegisterRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    // Check if user is already logged in on app start
    const token = apiClient.getToken()
    if (token) {
      // Try to fetch user info to validate token
      // For now, we'll assume token is valid if it exists
      // In a real app, you might want to validate the token with the backend
      
      // Try to decode JWT to get user info
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        if (payload.sub && payload.firstName && payload.lastName) {
          const user: User = {
            id: parseInt(payload.sub) || 0,
            firstName: payload.firstName || '',
            lastName: payload.lastName || '',
            email: payload.sub || '',
            createdAt: new Date().toISOString()
          }
          setUser(user)
        }
      } catch (error) {
        console.warn('Could not decode JWT token:', error)
        // Token might be invalid, clear it
        apiClient.setToken(null)
      }
      setIsLoading(false)
    } else {
      setIsLoading(false)
    }
  }, [])

  const login = async (credentials: LoginRequest) => {
    setIsLoading(true)
    try {
      const response = await apiClient.login(credentials)
      // Convert response to User object
      const user: User = {
        id: 0, // We don't have the ID in the response, will be set later
        firstName: response.firstName,
        lastName: response.lastName,
        email: response.email,
        createdAt: new Date().toISOString()
      }
      setUser(user)
    } catch (error) {
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  const register = async (userData: RegisterRequest) => {
    setIsLoading(true)
    try {
      const response = await apiClient.register(userData)
      // Convert response to User object
      const user: User = {
        id: 0, // We don't have the ID in the response, will be set later
        firstName: response.firstName,
        lastName: response.lastName,
        email: response.email,
        createdAt: new Date().toISOString()
      }
      setUser(user)
    } catch (error) {
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  const logout = () => {
    apiClient.logout()
    setUser(null)
  }

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user || !!apiClient.getToken(),
    isLoading,
    login,
    register,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// HOC for protecting routes
interface ProtectedRouteProps {
  children: ReactNode
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">Chargement...</div>
      </div>
    )
  }

  if (!isAuthenticated) {
    window.location.href = '/login'
    return null
  }

  return <>{children}</>
}