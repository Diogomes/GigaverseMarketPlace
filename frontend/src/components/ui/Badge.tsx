import type { ReactNode } from 'react'
import type { Tone } from '../../data/labels'
import { cn } from '../../lib/cn'

const tones: Record<Tone, string> = {
  neutral: 'bg-surface-muted text-fg-muted border-border',
  info: 'bg-sky-500/10 text-sky-700 border-sky-500/30 dark:text-sky-300',
  success: 'bg-emerald-500/10 text-emerald-700 border-emerald-500/30 dark:text-emerald-300',
  warning: 'bg-amber-500/10 text-amber-800 border-amber-500/30 dark:text-amber-300',
  danger: 'bg-red-500/10 text-red-700 border-red-500/30 dark:text-red-300',
  accent: 'bg-primary/10 text-primary border-primary/30 dark:text-accent',
}

export function Badge({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span
      className={cn('inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold', tones[tone])}
    >
      {children}
    </span>
  )
}
