import { forwardRef, type LabelHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

/**
 * Preserves the codebase's implicit `<label>텍스트<input/></label>` wrapping
 * convention (no htmlFor/id pairing exists today) — this is a styled
 * drop-in replacement, not a structural change.
 */
export const Label = forwardRef<HTMLLabelElement, LabelHTMLAttributes<HTMLLabelElement>>(function Label(
  { className, ...props },
  ref,
) {
  return (
    <label
      ref={ref}
      className={cn(
        'flex flex-col gap-1.5 text-sm font-medium text-neutral-700 dark:text-neutral-300',
        className,
      )}
      {...props}
    />
  )
})
