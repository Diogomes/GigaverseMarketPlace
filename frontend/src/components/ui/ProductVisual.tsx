import { Box } from 'lucide-react'
import { cn } from '../../lib/cn'

/**
 * Placeholder identificado para imagens de produto até o lojista enviar fotos próprias
 * (upload na Fase 04). Não usa o mockup nem imagens de terceiros.
 */
export function ProductVisual({
  name,
  variant = 0,
  size = 'md',
  className,
}: {
  name: string
  variant?: number
  size?: 'xs' | 'sm' | 'md' | 'lg'
  className?: string
}) {
  const gradients = [
    'from-[#0b1f4d] via-[#123a8f] to-[#06101f]',
    'from-[#101a2e] via-[#1f4fb8] to-[#070b14]',
    'from-[#0a1430] via-[#2a63d6] to-[#0b1222]',
    'from-[#141c2c] via-[#3b4a66] to-[#0a0f1a]',
  ]
  const icon = { xs: 'size-4', sm: 'size-6', md: 'size-12', lg: 'size-20' }[size]
  return (
    <div
      role="img"
      aria-label={`${name} — imagem ilustrativa (foto pendente)`}
      className={cn(
        'relative flex items-center justify-center overflow-hidden bg-gradient-to-br text-white/80',
        gradients[variant % gradients.length],
        className,
      )}
    >
      <Box aria-hidden className={icon} strokeWidth={1.25} />
      {(size === 'md' || size === 'lg') && (
        <span className="absolute bottom-2 left-2 rounded bg-black/50 px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-white/80">
          Foto pendente
        </span>
      )}
    </div>
  )
}
