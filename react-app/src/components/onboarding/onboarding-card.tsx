import React from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { 
  FileText, 
  CreditCard, 
  BookOpen, 
  Brain, 
  Target, 
  CheckCircle,
  Sparkles
} from 'lucide-react'
import type { OnboardingStatus, OnboardingProgress } from '@/types/api'

interface OnboardingCardProps {
  status: OnboardingStatus
  progress: OnboardingProgress
  onActionClick?: (action: string) => void
  onAiAccept?: () => void
  onAiDecline?: () => void
}

const getPhaseIcon = (phase: string) => {
  switch (phase) {
    case 'WELCOME':
      return <Target className="w-6 h-6 text-blue-500" />
    case 'FIRST_STEPS':
      return <BookOpen className="w-6 h-6 text-green-500" />
    case 'LEARNING':
      return <Brain className="w-6 h-6 text-purple-500" />
    case 'AI_AVAILABLE':
      return <Sparkles className="w-6 h-6 text-yellow-500" />
    case 'MATURE':
      return <CheckCircle className="w-6 h-6 text-emerald-500" />
    default:
      return <Target className="w-6 h-6 text-gray-500" />
  }
}

const getPhaseColor = (phase: string) => {
  switch (phase) {
    case 'WELCOME':
      return 'border-l-blue-500'
    case 'FIRST_STEPS':
      return 'border-l-green-500'
    case 'LEARNING':
      return 'border-l-purple-500'
    case 'AI_AVAILABLE':
      return 'border-l-yellow-500'
    case 'MATURE':
      return 'border-l-emerald-500'
    default:
      return 'border-l-gray-500'
  }
}


export const OnboardingCard: React.FC<OnboardingCardProps> = ({
  status,
  progress,
  onActionClick,
  onAiAccept,
  onAiDecline
}) => {
  const { t } = useTranslation()
  const renderWelcomeActions = () => (
    <div className="flex flex-col sm:flex-row gap-3 mt-4">
      <Button 
        onClick={() => onActionClick?.('csv-import')}
        className="flex items-center gap-2"
      >
        <FileText className="w-4 h-4" />
        {t('onboarding.actions.importCsv')}
      </Button>
      <Button 
        variant="outline" 
        onClick={() => onActionClick?.('bank-connect')}
        className="flex items-center gap-2"
      >
        <CreditCard className="w-4 h-4" />
        {t('onboarding.actions.connectBank')}
      </Button>
    </div>
  )

  const renderLearningProgress = () => (
    <div className="mt-4">
      <div className="flex justify-between items-center mb-2">
        <span className="text-sm font-medium text-gray-600">{t('onboarding.progress.towardsAi')}</span>
        <span className="text-sm text-gray-500">
          {progress.transactionCount}/{progress.nextMilestone}
        </span>
      </div>
      <Progress 
        value={(progress.transactionCount / progress.nextMilestone) * 100} 
        className="h-2"
      />
      <p className="text-xs text-gray-500 mt-2">
        {progress.description}
      </p>
    </div>
  )

  const renderAiUnlockActions = () => (
    <div className="mt-6 p-4 bg-gradient-to-r from-yellow-50 to-orange-50 border border-yellow-200 rounded-lg">
      <div className="flex items-start gap-3">
        <Sparkles className="w-6 h-6 text-yellow-500 flex-shrink-0 mt-0.5" />
        <div className="flex-1">
          <h4 className="font-semibold text-gray-900 mb-2">
            {t('onboarding.aiUnlock.title')}
          </h4>
          <div className="space-y-2 mb-4">
            <div className="flex items-center gap-2 text-sm text-gray-600">
              <CheckCircle className="w-4 h-4 text-green-500" />
              <span>{t('onboarding.aiUnlock.benefits.suggestions')}</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-gray-600">
              <CheckCircle className="w-4 h-4 text-green-500" />
              <span>{t('onboarding.aiUnlock.benefits.timeSaving')}</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-gray-600">
              <CheckCircle className="w-4 h-4 text-green-500" />
              <span>{t('onboarding.aiUnlock.benefits.improves')}</span>
            </div>
          </div>
          <div className="flex flex-col sm:flex-row gap-2">
            <Button 
              onClick={onAiAccept}
              className="flex items-center gap-2 bg-gradient-to-r from-yellow-500 to-orange-500 hover:from-yellow-600 hover:to-orange-600"
            >
              <Sparkles className="w-4 h-4" />
              {t('onboarding.actions.activateAi')}
            </Button>
            <Button 
              variant="ghost" 
              onClick={onAiDecline}
              className="text-gray-600"
            >
              {t('onboarding.actions.later')}
            </Button>
          </div>
        </div>
      </div>
    </div>
  )

  const renderMatureStats = () => (
    <div className="mt-4 grid grid-cols-2 gap-4">
      <div className="text-center p-3 bg-gray-50 rounded-lg">
        <div className="text-2xl font-bold text-gray-900">
          {progress.transactionCount}
        </div>
        <div className="text-sm text-gray-600">{t('onboarding.stats.transactions')}</div>
      </div>
      <div className="text-center p-3 bg-gray-50 rounded-lg">
        <div className="text-2xl font-bold text-gray-900">
          {Math.round((progress.assignedCount / progress.transactionCount) * 100)}%
        </div>
        <div className="text-sm text-gray-600">{t('onboarding.stats.categorized')}</div>
      </div>
    </div>
  )

  const renderPhaseContent = () => {
    switch (status.phase) {
      case 'WELCOME':
        return renderWelcomeActions()
      case 'FIRST_STEPS':
      case 'LEARNING':
        return renderLearningProgress()
      case 'AI_AVAILABLE':
        return renderAiUnlockActions()
      case 'MATURE':
        return renderMatureStats()
      default:
        return null
    }
  }

  return (
    <Card className={`border-l-4 ${getPhaseColor(status.phase)} shadow-lg`}>
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            {getPhaseIcon(status.phase)}
            <div>
              <h3 className="text-lg font-bold">
                {status.phase === 'WELCOME' && t('onboarding.phases.welcome')}
                {status.phase === 'FIRST_STEPS' && t('onboarding.phases.firstSteps')}
                {status.phase === 'LEARNING' && t('onboarding.phases.learning')}
                {status.phase === 'AI_AVAILABLE' && t('onboarding.phases.aiAvailable')}
                {status.phase === 'MATURE' && t('onboarding.phases.mature')}
              </h3>
              {status.phase !== 'WELCOME' && (
                <p className="text-sm text-gray-500 font-normal">
                  Phase {status.phase.toLowerCase()}
                </p>
              )}
            </div>
          </div>
          <Badge variant="outline" className="ml-2">
            {status.transactionCount} transaction{status.transactionCount > 1 ? 's' : ''}
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-gray-700 leading-relaxed mb-4">
          {status.message}
        </p>
        
        {renderPhaseContent()}

        {status.phase === 'LEARNING' && (
          <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-sm text-blue-700">
              💡 <strong>{t('onboarding.tips.title')} :</strong> {t('onboarding.learningTip')}
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

export default OnboardingCard