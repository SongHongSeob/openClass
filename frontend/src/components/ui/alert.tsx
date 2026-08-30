import type { HTMLAttributes } from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const alertVariants = cva('rounded-md border px-3 py-2 text-sm', {
  variants: {
    tone: {
      error: 'border-danger-600/30 bg-danger-50 text-danger-700 dark:bg-danger-600/10 dark:text-red-300',
      info: 'border-accent-500/30 bg-accent-50 text-accent-700 dark:bg-accent-500/10 dark:text-accent-400',
    },
  },
  defaultVariants: {
    tone: 'info',
  },
})

export interface AlertProps extends HTMLAttributes<HTMLParagraphElement>, VariantProps<typeof alertVariants> {
  /**
   * Callers pass the exact `role` value the page previously rendered
   * (`"alert"` or `"status"`) — this component never infers or defaults
   * the role, preserving every existing accessibility-tree assertion.
   */
  role: 'alert' | 'status'
}

export function Alert({ className, tone, role, ...props }: AlertProps) {
  return <p role={role} className={cn(alertVariants({ tone }), className)} {...props} />
}
