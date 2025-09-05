import { useState, useEffect } from 'react'
import { apiClient } from '@/lib/api'
import type { OnboardingStatus, OnboardingProgress } from '@/types/api'

export interface UseOnboardingReturn {
  status: OnboardingStatus | null
  progress: OnboardingProgress | null
  loading: boolean
  error: string | null
  refreshStatus: () => Promise<void>
  isAiAvailable: boolean
  shouldShowAiUnlock: boolean
  acknowledgeAi: (accepted: boolean) => Promise<void>
}

export const useOnboarding = (): UseOnboardingReturn => {
  const [status, setStatus] = useState<OnboardingStatus | null>(null)
  const [progress, setProgress] = useState<OnboardingProgress | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchOnboardingData = async () => {
    try {
      setLoading(true)
      setError(null)
      
      const [statusData, progressData] = await Promise.all([
        apiClient.getOnboardingStatus(),
        apiClient.getOnboardingProgress()
      ])
      
      setStatus(statusData)
      setProgress(progressData)
    } catch (err) {
      setError('Erreur lors du chargement des données d\'onboarding')
      console.error('Erreur onboarding:', err)
    } finally {
      setLoading(false)
    }
  }

  const refreshStatus = async () => {
    await fetchOnboardingData()
  }

  const acknowledgeAi = async (accepted: boolean) => {
    try {
      await apiClient.acknowledgeAiFeature(accepted)
      // Refresh status after acknowledgment
      await fetchOnboardingData()
    } catch (err) {
      setError('Erreur lors de l\'enregistrement de votre choix')
      console.error('Erreur acknowledge AI:', err)
    }
  }

  useEffect(() => {
    fetchOnboardingData()
  }, [])

  // Computed properties
  const isAiAvailable = status?.aiAvailable ?? false
  const shouldShowAiUnlock = status?.phase === 'AI_AVAILABLE'

  return {
    status,
    progress,
    loading,
    error,
    refreshStatus,
    isAiAvailable,
    shouldShowAiUnlock,
    acknowledgeAi
  }
}

export default useOnboarding