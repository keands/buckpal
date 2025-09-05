import React from 'react'
import { useTranslation } from 'react-i18next'
import { Progress } from '@/components/ui/progress'
import { Badge } from '@/components/ui/badge'
import { Brain, Sparkles } from 'lucide-react'
import type { OnboardingStatus, OnboardingProgress } from '@/types/api'

interface OnboardingProgressBarProps {
  status: OnboardingStatus
  progress: OnboardingProgress
  compact?: boolean
}

export const OnboardingProgressBar: React.FC<OnboardingProgressBarProps> = ({
  status,
  progress,
  compact = false
}) => {
  const { t } = useTranslation()
  if (status.phase === 'MATURE') {
    return null // Don't show for mature users
  }

  const isAiPhase = status.phase === 'AI_AVAILABLE'
  const progressPercentage = Math.min((progress.transactionCount / progress.nextMilestone) * 100, 100)

  if (compact) {
    return (
      <div className="flex items-center gap-3 px-3 py-2 bg-gray-50 rounded-lg border">
        {isAiPhase ? (
          <Sparkles className="w-4 h-4 text-yellow-500" />
        ) : (
          <Brain className="w-4 h-4 text-purple-500" />
        )}
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-medium text-gray-600 truncate">
              {isAiPhase ? t('onboarding.progress.aiUnlocked') : t('onboarding.progress.towardsAi')}
            </span>
            <span className="text-xs text-gray-500">
              {progress.transactionCount}/{progress.nextMilestone}
            </span>
          </div>
          {!isAiPhase && (
            <Progress value={progressPercentage} className="h-1.5" />
          )}
          {isAiPhase && (
            <div className="h-1.5 bg-gradient-to-r from-yellow-400 to-orange-400 rounded-full" />
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="p-4 bg-gradient-to-r from-purple-50 to-blue-50 border border-purple-200 rounded-lg">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          {isAiPhase ? (
            <Sparkles className="w-5 h-5 text-yellow-500" />
          ) : (
            <Brain className="w-5 h-5 text-purple-500" />
          )}
          <h4 className="font-semibold text-gray-900">
            {isAiPhase ? t('onboarding.progress.description.aiAvailable') : t('onboarding.progress.towardsAi')}
          </h4>
        </div>
        <Badge variant={isAiPhase ? 'default' : 'secondary'}>
          {progress.transactionCount}/{progress.nextMilestone}
        </Badge>
      </div>
      
      {!isAiPhase && (
        <>
          <Progress value={progressPercentage} className="h-2 mb-2" />
          <p className="text-sm text-gray-600">
            {progress.description}
          </p>
        </>
      )}
      
      {isAiPhase && (
        <div className="space-y-2">
          <div className="h-2 bg-gradient-to-r from-yellow-400 to-orange-400 rounded-full" />
          <p className="text-sm text-gray-600">
            Cliquez sur la carte ci-dessus pour activer l'attribution intelligente !
          </p>
        </div>
      )}
    </div>
  )
}

export default OnboardingProgressBar