import * as React from "react"
import { cn } from "@/lib/utils"

export interface ProgressProps extends React.HTMLAttributes<HTMLDivElement> {
  value?: number
  max?: number
}

const Progress = React.forwardRef<HTMLDivElement, ProgressProps>(
  ({ className, value = 0, max = 100, ...props }, ref) => {
    const percentage = Math.min(Math.max((value / max) * 100, 0), 100)
    
    return (
      <div
        ref={ref}
        className={cn(
          "relative h-4 w-full overflow-hidden rounded-full bg-secondary",
          className
        )}
        {...props}
      >
        <div
          className={cn(
            "h-full w-full flex-1 transition-all",
            percentage <= 50 ? "bg-green-500" :
            percentage <= 75 ? "bg-yellow-500" :
            percentage <= 90 ? "bg-orange-500" :
            "bg-red-500"
          )}
          style={{
            transform: `translateX(-${100 - percentage}%)`
          }}
        />
      </div>
    )
  }
)
Progress.displayName = "Progress"

export { Progress }