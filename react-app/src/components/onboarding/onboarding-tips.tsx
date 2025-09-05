import React, { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { 
  Lightbulb, 
  ArrowRight, 
  X,
  ChevronLeft,
  ChevronRight
} from 'lucide-react'
import { apiClient } from '@/lib/api'
import type { OnboardingTips } from '@/types/api'

interface OnboardingTipsProps {
  onDismiss?: () => void
  className?: string
}

export const OnboardingTipsComponent: React.FC<OnboardingTipsProps> = ({
  onDismiss,
  className = ''
}) => {
  const { t } = useTranslation()
  const [tips, setTips] = useState<OnboardingTips | null>(null)
  const [currentTipIndex, setCurrentTipIndex] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchTips = async () => {
      try {
        const tipsData = await apiClient.getOnboardingTips()
        setTips(tipsData)
      } catch (error) {
        console.error('Erreur lors du chargement des tips:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchTips()
  }, [])

  if (loading || !tips || tips.tips.length === 0) {
    return null
  }

  const currentTip = tips.tips[currentTipIndex]
  const hasMultipleTips = tips.tips.length > 1

  const nextTip = () => {
    setCurrentTipIndex((prev) => (prev + 1) % tips.tips.length)
  }

  const prevTip = () => {
    setCurrentTipIndex((prev) => (prev - 1 + tips.tips.length) % tips.tips.length)
  }

  const getPhaseColor = (phase: string) => {
    switch (phase) {
      case 'WELCOME':
        return 'border-l-blue-500 bg-blue-50'
      case 'FIRST_STEPS':
        return 'border-l-green-500 bg-green-50'
      case 'LEARNING':
        return 'border-l-purple-500 bg-purple-50'
      case 'AI_AVAILABLE':
        return 'border-l-yellow-500 bg-yellow-50'
      case 'MATURE':
        return 'border-l-emerald-500 bg-emerald-50'
      default:
        return 'border-l-gray-500 bg-gray-50'
    }
  }

  return (
    <Card className={`border-l-4 ${getPhaseColor(tips.phase)} ${className}`}>
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Lightbulb className="w-5 h-5 text-yellow-500" />
            <span className="text-base font-semibold">{t('onboarding.tips.title')}</span>
            {hasMultipleTips && (
              <span className="text-xs text-gray-500 bg-gray-200 px-2 py-1 rounded-full">
                {currentTipIndex + 1}/{tips.tips.length}
              </span>
            )}
          </div>
          {onDismiss && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onDismiss}
              className="h-6 w-6 p-0"
            >
              <X className="w-4 h-4" />
            </Button>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          <p className="text-sm text-gray-700 leading-relaxed">
            {currentTip}
          </p>
          
          {tips.nextSteps && tips.nextSteps.length > 0 && (
            <div className="pt-2 border-t border-gray-200">
              <h4 className="text-xs font-medium text-gray-600 mb-2 uppercase tracking-wide">
                {t('common.next')} étapes
              </h4>
              <div className="space-y-1">
                {tips.nextSteps.map((step, index) => (
                  <div key={index} className="flex items-center gap-2 text-xs text-gray-600">
                    <ArrowRight className="w-3 h-3 text-gray-400" />
                    <span>{step}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {hasMultipleTips && (
            <div className="flex justify-between items-center pt-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={prevTip}
                className="h-7 px-2"
              >
                <ChevronLeft className="w-4 h-4 mr-1" />
                {t('common.previous')}
              </Button>
              
              <div className="flex gap-1">
                {tips.tips.map((_, index) => (
                  <button
                    key={index}
                    onClick={() => setCurrentTipIndex(index)}
                    className={`w-2 h-2 rounded-full transition-colors ${
                      index === currentTipIndex
                        ? 'bg-blue-500'
                        : 'bg-gray-300 hover:bg-gray-400'
                    }`}
                  />
                ))}
              </div>

              <Button
                variant="ghost"
                size="sm"
                onClick={nextTip}
                className="h-7 px-2"
              >
                {t('common.next')}
                <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

export default OnboardingTipsComponent