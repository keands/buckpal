import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { apiClient } from '@/lib/api'

export default function CalendarSimplePage() {
  const [message, setMessage] = useState('Calendar page loaded successfully!')

  const testBasicApiCall = async () => {
    try {
      setMessage('Testing basic API call (accounts)...')
      const accounts = await apiClient.getAccounts()
      setMessage(`✅ Basic API works! Found ${accounts.length} accounts`)
    } catch (error: any) {
      setMessage(`❌ Basic API failed: ${error.message || error}`)
    }
  }

  const testCalendarApiCall = async () => {
    try {
      setMessage('Testing calendar API call...')
      const currentYear = new Date().getFullYear()
      const startDate = `${currentYear}-01-01`
      const endDate = `${currentYear}-12-31`
      const calendarData = await apiClient.getCalendarData(startDate, endDate)
      setMessage(`✅ Calendar API works! Found ${calendarData.length} days with data`)
      if (calendarData.length > 0) {
        console.log('First few calendar entries:', calendarData.slice(0, 3))
      }
    } catch (error: any) {
      setMessage(`❌ Calendar API failed: ${error.message || error}. Status: ${error.response?.status || 'unknown'}`)
      console.error('Calendar API error:', error)
    }
  }

  const checkTransactions = async () => {
    try {
      setMessage('Checking recent transactions...')
      const response = await apiClient.getTransactions(undefined, 0, 10)
      setMessage(`✅ Found ${response.totalElements} total transactions. Recent: ${response.content.length}`)
      if (response.content.length > 0) {
        const sample = response.content[0]
        console.log('Sample transaction:', sample)
        setMessage(prev => prev + `\nSample: ${sample.description} on ${sample.transactionDate} = ${sample.amount} (${sample.transactionType})`)
      }
    } catch (error: any) {
      setMessage(`❌ Transaction check failed: ${error.message || error}`)
    }
  }

  const testToken = () => {
    const token = apiClient.getToken()
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        setMessage(`✅ Token exists and is valid. User: ${payload.sub || 'unknown'}`)
      } catch (e) {
        setMessage(`❌ Token exists but is malformed`)
      }
    } else {
      setMessage(`❌ No token found`)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Calendrier (Test)</h1>
        <p className="text-gray-600">Page de test du calendrier</p>
      </div>
      
      <Card>
        <CardHeader>
          <CardTitle>Test de connexion</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="mb-4 p-3 bg-gray-50 rounded">{message}</p>
          <div className="space-x-2 space-y-2">
            <Button onClick={testToken} variant="outline">
              1. Check Token
            </Button>
            <Button onClick={testBasicApiCall} variant="outline">
              2. Test Basic API
            </Button>
            <Button onClick={checkTransactions} variant="outline">
              3. Check Transactions
            </Button>
            <Button onClick={testCalendarApiCall}>
              4. Test Calendar API
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}