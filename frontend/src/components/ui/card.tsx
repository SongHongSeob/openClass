import type { HTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

/**
 * Card is a plain styled `<div>` container. It intentionally does NOT
 * replace the `<section>` elements the pages already use for their
 * top-level landmark — pages nest `<Card>` *inside* their existing
 * `<section>` so no landmark/role is lost.
 */
export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        'rounded-lg border border-neutral-200 bg-white p-6 shadow-sm dark:border-neutral-800 dark:bg-neutral-900',
        className,
      )}
      {...props}
    />
  )
}
