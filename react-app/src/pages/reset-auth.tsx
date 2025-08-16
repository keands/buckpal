import { useAuth } from '@/contexts/auth-context'
import { apiClient } from '@/lib/api'
import { Button } from '@/components/ui/button'

export default function ResetAuthPage() {
  const { logout } = useAuth()

  const clearAllAuth = () => {
    // Clear localStorage
    localStorage.removeItem('jwt_token')
    
    // Clear API client token
    apiClient.setToken(null)
    
    // Logout from context
    logout()
    
    alert('Authentication cleared. You will be redirected to login.')
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Reset Authentication</h1>
      
      <div className="space-y-4">
        <p>If you're having authentication issues, click the button below to clear all authentication data and force a fresh login.</p>
        
        <Button 
          onClick={clearAllAuth}
          className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
        >
          Clear All Authentication Data
        </Button>
      </div>
    </div>
  )
}