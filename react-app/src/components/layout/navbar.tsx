import { useAuth } from '@/contexts/auth-context'
import { Button } from '@/components/ui/button'
import { Link, useLocation } from 'react-router-dom'
import { apiClient } from '@/lib/api'
import { 
  CreditCard, 
  BarChart3, 
  Upload, 
  Settings, 
  LogOut,
  User,
  Calendar
} from 'lucide-react'

export default function Navbar() {
  const { user, logout } = useAuth()
  const location = useLocation()

  const navigation = [
    { name: 'Dashboard', href: '/dashboard', icon: BarChart3 },
    { name: 'Comptes', href: '/accounts', icon: CreditCard },
    { name: 'Calendrier', href: '/calendar', icon: Calendar },
    { name: 'Import CSV', href: '/csv-import', icon: Upload },
    { name: 'Paramètres', href: '/settings', icon: Settings },
  ]

  return (
    <nav className="bg-white shadow-sm border-b">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center">
            <Link to="/dashboard" className="flex items-center">
              <span className="text-2xl font-bold text-primary">BuckPal</span>
            </Link>
            
            <div className="hidden sm:ml-8 sm:flex sm:space-x-8">
              {navigation.map((item) => {
                const Icon = item.icon
                const isActive = location.pathname === item.href
                
                return (
                  <Link
                    key={item.name}
                    to={item.href}
                    className={`inline-flex items-center px-1 pt-1 text-sm font-medium border-b-2 ${
                      isActive
                        ? 'border-primary text-primary'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                    }`}
                  >
                    <Icon className="w-4 h-4 mr-2" />
                    {item.name}
                  </Link>
                )
              })}
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {user && (
              <div className="flex items-center space-x-2">
                <User className="w-4 h-4 text-gray-500" />
                <span className="text-sm text-gray-700">
                  {user.firstName} {user.lastName}
                </span>
              </div>
            )}
            
            {/* Always show logout button when authenticated, even if user info is not available */}
            {(user || apiClient.getToken()) && (
              <Button 
                variant="outline" 
                size="sm" 
                onClick={logout}
                className="text-red-600 hover:text-red-700 hover:bg-red-50 border-red-200"
              >
                <LogOut className="w-4 h-4 mr-2" />
                Déconnexion
              </Button>
            )}
          </div>
        </div>
      </div>
    </nav>
  )
}