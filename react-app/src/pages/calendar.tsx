import { useState, useEffect } from 'react'
import { apiClient } from '@/lib/api'
import { formatCurrency, cn } from '@/lib/utils'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { 
  ChevronLeft, 
  ChevronRight, 
  Calendar as CalendarIcon,
  TrendingUp,
  TrendingDown,
  ArrowUpRight,
  ArrowDownLeft
} from 'lucide-react'
import type { CalendarDay } from '@/types/api'
import { startOfMonth, endOfMonth, format, addMonths, subMonths, eachDayOfInterval, getDay, isToday, parseISO } from 'date-fns'
import { fr } from 'date-fns/locale'

export default function CalendarPage() {
  const [calendarData, setCalendarData] = useState<CalendarDay[]>([])
  const [currentMonth, setCurrentMonth] = useState(new Date())
  const [isLoading, setIsLoading] = useState(true)
  const [selectedDay, setSelectedDay] = useState<CalendarDay | null>(null)

  useEffect(() => {
    loadCalendarData()
  }, [currentMonth])

  const loadCalendarData = async () => {
    try {
      setIsLoading(true)
      
      const start = startOfMonth(currentMonth)
      const end = endOfMonth(currentMonth)
      
      const startDate = format(start, 'yyyy-MM-dd')
      const endDate = format(end, 'yyyy-MM-dd')
      
      const data = await apiClient.getCalendarData(startDate, endDate)
      setCalendarData(data)
    } catch (error) {
      console.error('Error loading calendar data:', error)
      // Don't let API errors cause disconnection - just show empty calendar
      setCalendarData([])
    } finally {
      setIsLoading(false)
    }
  }

  const goToPreviousMonth = () => {
    setCurrentMonth(subMonths(currentMonth, 1))
  }

  const goToNextMonth = () => {
    setCurrentMonth(addMonths(currentMonth, 1))
  }

  const getCalendarDays = () => {
    const start = startOfMonth(currentMonth)
    const end = endOfMonth(currentMonth)
    
    // Get all days of the month
    const daysOfMonth = eachDayOfInterval({ start, end })
    
    // Add padding days from previous month
    const startDay = getDay(start)
    const paddingDays = startDay === 0 ? 6 : startDay - 1 // Monday = 0
    
    const calendarDays = []
    
    // Add padding from previous month
    for (let i = paddingDays - 1; i >= 0; i--) {
      const paddingDate = new Date(start)
      paddingDate.setDate(start.getDate() - i - 1)
      calendarDays.push({ date: paddingDate, isCurrentMonth: false })
    }
    
    // Add current month days
    daysOfMonth.forEach(date => {
      calendarDays.push({ date, isCurrentMonth: true })
    })
    
    // Add padding to complete the last week (42 days total for 6 weeks)
    while (calendarDays.length < 42) {
      const lastDate: Date = calendarDays[calendarDays.length - 1].date
      const nextDate: Date = new Date(lastDate)
      nextDate.setDate(lastDate.getDate() + 1)
      calendarDays.push({ date: nextDate, isCurrentMonth: false })
    }
    
    return calendarDays
  }

  const getDayData = (date: Date): CalendarDay | undefined => {
    const dateStr = format(date, 'yyyy-MM-dd')
    return calendarData.find(day => day.date === dateStr)
  }

  const getDayColor = (dayData: CalendarDay | undefined) => {
    if (!dayData || dayData.transactionCount === 0) return 'bg-gray-50'
    
    const { netAmount } = dayData
    if (netAmount > 0) return 'bg-green-100 border-green-200'
    if (netAmount < 0) return 'bg-red-100 border-red-200'
    return 'bg-gray-100 border-gray-200'
  }

  const monthlyStats = calendarData.reduce(
    (acc, day) => ({
      totalIncome: acc.totalIncome + day.totalIncome,
      totalExpense: acc.totalExpense + day.totalExpense,
      netAmount: acc.netAmount + day.netAmount,
      transactionCount: acc.transactionCount + day.transactionCount,
    }),
    { totalIncome: 0, totalExpense: 0, netAmount: 0, transactionCount: 0 }
  )

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="text-lg">Chargement du calendrier...</div>
      </div>
    )
  }

  const calendarDays = getCalendarDays()

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Calendrier</h1>
          <p className="text-gray-600">Vue mensuelle de vos transactions</p>
        </div>
        
        <div className="flex items-center space-x-3">
          <Button variant="outline" onClick={goToPreviousMonth}>
            <ChevronLeft className="w-4 h-4" />
          </Button>
          <h2 className="text-xl font-semibold text-gray-900 min-w-[200px] text-center">
            {format(currentMonth, 'MMMM yyyy', { locale: fr })}
          </h2>
          <Button variant="outline" onClick={goToNextMonth}>
            <ChevronRight className="w-4 h-4" />
          </Button>
        </div>
      </div>

      {/* Monthly Summary */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Revenus du mois</CardTitle>
            <TrendingUp className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              +{formatCurrency(monthlyStats.totalIncome)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Dépenses du mois</CardTitle>
            <TrendingDown className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">
              -{formatCurrency(monthlyStats.totalExpense)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Solde net</CardTitle>
            <CalendarIcon className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${
              monthlyStats.netAmount >= 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              {monthlyStats.netAmount >= 0 ? '+' : ''}
              {formatCurrency(monthlyStats.netAmount)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Transactions</CardTitle>
            <CalendarIcon className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {monthlyStats.transactionCount}
            </div>
            <p className="text-xs text-muted-foreground">
              Ce mois-ci
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Calendar Grid */}
      <Card>
        <CardContent className="p-6">
          {/* Day Headers */}
          <div className="grid grid-cols-7 gap-2 mb-4">
            {['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'].map(day => (
              <div key={day} className="text-center text-sm font-medium text-gray-500 py-2">
                {day}
              </div>
            ))}
          </div>

          {/* Calendar Days */}
          <div className="grid grid-cols-7 gap-2">
            {calendarDays.map(({ date, isCurrentMonth }, index) => {
              const dayData = getDayData(date)
              const dayNumber = date.getDate()
              
              return (
                <div
                  key={index}
                  className={cn(
                    'h-24 border rounded-lg p-2 cursor-pointer hover:shadow-md transition-shadow',
                    isCurrentMonth ? getDayColor(dayData) : 'bg-gray-50 text-gray-400',
                    isToday(date) && isCurrentMonth && 'ring-2 ring-blue-500',
                    selectedDay?.date === format(date, 'yyyy-MM-dd') && 'ring-2 ring-primary'
                  )}
                  onClick={() => dayData && setSelectedDay(dayData)}
                >
                  <div className="flex justify-between items-start h-full">
                    <span className={cn(
                      'text-sm font-medium',
                      !isCurrentMonth && 'text-gray-400'
                    )}>
                      {dayNumber}
                    </span>
                    
                    {dayData && dayData.transactionCount > 0 && isCurrentMonth && (
                      <div className="flex flex-col items-end space-y-1 text-xs">
                        {dayData.totalIncome > 0 && (
                          <div className="flex items-center text-green-600">
                            <ArrowUpRight className="w-3 h-3 mr-1" />
                            <span>{formatCurrency(dayData.totalIncome)}</span>
                          </div>
                        )}
                        {dayData.totalExpense > 0 && (
                          <div className="flex items-center text-red-600">
                            <ArrowDownLeft className="w-3 h-3 mr-1" />
                            <span>{formatCurrency(dayData.totalExpense)}</span>
                          </div>
                        )}
                        <div className="text-gray-500 text-xs">
                          {dayData.transactionCount} trans.
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </CardContent>
      </Card>

      {/* Selected Day Details */}
      {selectedDay && (
        <Card>
          <CardHeader>
            <CardTitle>
              {format(parseISO(selectedDay.date), 'EEEE d MMMM yyyy', { locale: fr })}
            </CardTitle>
            <CardDescription>
              Détail des transactions du jour
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="flex items-center justify-between p-3 bg-green-50 rounded-lg">
                <div className="flex items-center space-x-2">
                  <ArrowUpRight className="w-5 h-5 text-green-600" />
                  <span className="font-medium">Revenus</span>
                </div>
                <span className="text-green-600 font-bold">
                  +{formatCurrency(selectedDay.totalIncome)}
                </span>
              </div>
              
              <div className="flex items-center justify-between p-3 bg-red-50 rounded-lg">
                <div className="flex items-center space-x-2">
                  <ArrowDownLeft className="w-5 h-5 text-red-600" />
                  <span className="font-medium">Dépenses</span>
                </div>
                <span className="text-red-600 font-bold">
                  -{formatCurrency(selectedDay.totalExpense)}
                </span>
              </div>
              
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center space-x-2">
                  <CalendarIcon className="w-5 h-5 text-gray-600" />
                  <span className="font-medium">Solde net</span>
                </div>
                <span className={`font-bold ${
                  selectedDay.netAmount >= 0 ? 'text-green-600' : 'text-red-600'
                }`}>
                  {selectedDay.netAmount >= 0 ? '+' : ''}
                  {formatCurrency(selectedDay.netAmount)}
                </span>
              </div>
            </div>
            
            <div className="mt-4 text-center text-sm text-gray-600">
              {selectedDay.transactionCount} transaction{selectedDay.transactionCount > 1 ? 's' : ''} ce jour-là
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}