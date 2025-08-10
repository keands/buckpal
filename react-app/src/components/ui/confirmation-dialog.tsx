import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { AlertTriangle, Trash2 } from 'lucide-react'

interface ConfirmationDialogProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: (forceDelete: boolean) => void
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  isDestructive?: boolean
  isLoading?: boolean
  showForceOption?: boolean
  warningDetails?: {
    count: number
    accountName: string
  }
}

export function ConfirmationDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = "Confirm",
  cancelText = "Cancel",
  isDestructive = false,
  isLoading = false,
  showForceOption = false,
  warningDetails
}: ConfirmationDialogProps) {
  const [forceDelete, setForceDelete] = useState(false)

  if (!isOpen) return null

  const handleConfirm = () => {
    onConfirm(forceDelete)
  }

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose()
    }
  }

  return (
    <div 
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      onClick={handleOverlayClick}
    >
      <Card className="w-full max-w-md mx-4">
        <CardHeader>
          <div className="flex items-center space-x-3">
            {isDestructive && (
              <div className="flex-shrink-0">
                <AlertTriangle className="h-6 w-6 text-red-600" />
              </div>
            )}
            <div>
              <CardTitle className={isDestructive ? "text-red-900" : ""}>
                {title}
              </CardTitle>
              <CardDescription className="mt-1">
                {message}
              </CardDescription>
            </div>
          </div>
        </CardHeader>
        
        {warningDetails && (
          <CardContent className="pt-0">
            <div className="bg-red-50 border border-red-200 rounded-md p-3">
              <div className="flex items-start">
                <Trash2 className="h-5 w-5 text-red-400 mt-0.5 flex-shrink-0" />
                <div className="ml-3">
                  <h4 className="text-sm font-medium text-red-800">
                    Permanent Deletion Warning
                  </h4>
                  <div className="mt-2 text-sm text-red-700">
                    <p>You are about to permanently delete:</p>
                    <ul className="mt-1 space-y-1">
                      <li>• <strong>{warningDetails.count} transactions</strong></li>
                      <li>• From account: <strong>"{warningDetails.accountName}"</strong></li>
                    </ul>
                    <p className="mt-2 font-medium">
                      ⚠️ This action cannot be undone!
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {showForceOption && (
              <div className="mt-4">
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={forceDelete}
                    onChange={(e) => setForceDelete(e.target.checked)}
                    className="rounded border-gray-300 text-red-600 focus:ring-red-500"
                  />
                  <span className="text-sm text-gray-700">
                    I understand this is permanent and cannot be undone
                  </span>
                </label>
              </div>
            )}
          </CardContent>
        )}
        
        <CardContent className={warningDetails ? "pt-0" : ""}>
          <div className="flex justify-end space-x-3">
            <Button 
              variant="outline" 
              onClick={onClose}
              disabled={isLoading}
            >
              {cancelText}
            </Button>
            <Button 
              variant={isDestructive ? "destructive" : "default"}
              onClick={handleConfirm}
              disabled={isLoading || (showForceOption && !forceDelete)}
            >
              {isLoading ? (
                <div className="flex items-center space-x-2">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                  <span>Processing...</span>
                </div>
              ) : (
                confirmText
              )}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}