import i18n from '@/i18n'

export const formatCurrencyI18n = (amount: number): string => {
  const locale = i18n.language === 'fr' ? 'fr-FR' : 'en-US'
  const currency = i18n.language === 'fr' ? 'EUR' : 'USD'
  
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount)
}

export const formatDateI18n = (date: Date | string, options: Intl.DateTimeFormatOptions = {}): string => {
  const locale = i18n.language === 'fr' ? 'fr-FR' : 'en-US'
  const dateObj = typeof date === 'string' ? new Date(date) : date
  
  const defaultOptions: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    ...options
  }
  
  return new Intl.DateTimeFormat(locale, defaultOptions).format(dateObj)
}

export const formatDateShortI18n = (date: Date | string): string => {
  const locale = i18n.language === 'fr' ? 'fr-FR' : 'en-US'
  const dateObj = typeof date === 'string' ? new Date(date) : date
  
  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: '2-digit', 
    day: '2-digit'
  }).format(dateObj)
}

export const getDateInputFormat = (date: Date | string): string => {
  const dateObj = typeof date === 'string' ? new Date(date) : date
  return dateObj.toISOString().split('T')[0]
}