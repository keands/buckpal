import { useAuth } from '@/contexts/auth-context'
import { apiClient } from '@/lib/api'

export default function DebugAuthPage() {
  const { user, isAuthenticated, isLoading } = useAuth()
  const token = apiClient.getToken()

  const testAPICall = async () => {
    try {
      const response = await apiClient.getAccounts()
      console.log('API call successful:', response)
      alert('API call successful')
    } catch (error) {
      console.error('API call failed:', error)
      alert('API call failed: ' + error)
    }
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Debug Authentication</h1>
      
      <div className="space-y-4">
        <div>
          <strong>Is Loading:</strong> {isLoading ? 'true' : 'false'}
        </div>
        
        <div>
          <strong>Is Authenticated:</strong> {isAuthenticated ? 'true' : 'false'}
        </div>
        
        <div>
          <strong>User:</strong> {user ? JSON.stringify(user, null, 2) : 'null'}
        </div>
        
        <div>
          <strong>JWT Token:</strong> 
          <div className="bg-gray-100 p-2 rounded mt-2 text-sm break-all">
            {token || 'No token found'}
          </div>
        </div>
        
        <div>
          <strong>Local Storage JWT:</strong>
          <div className="bg-gray-100 p-2 rounded mt-2 text-sm break-all">
            {localStorage.getItem('jwt_token') || 'No token in localStorage'}
          </div>
        </div>
        
        <button 
          onClick={testAPICall}
          className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
        >
          Test API Call
        </button>
      </div>
    </div>
  )
}