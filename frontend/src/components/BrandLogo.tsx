/**
 * Logo oficial (recortes redimensionados de public/brand/gigaverse-logo-original.png,
 * sem distorção). A arte tem fundo escuro opaco, então fica sobre um "chip" escuro
 * também no tema claro até existir uma versão transparente (decisão D-P09).
 */
export function BrandLogo({ size = 'md' }: { size?: 'md' | 'lg' }) {
  const emblem = size === 'lg' ? 'size-16' : 'size-9'
  const wordmark = size === 'lg' ? 'h-9' : 'h-5'

  return (
    <span className="inline-flex items-center gap-2 rounded-lg bg-logo-chip px-2 py-1">
      <img src="/brand/gigaverse-emblem.png" alt="" className={`${emblem} shrink-0`} />
      <img src="/brand/gigaverse-wordmark.png" alt="Gigaverse3D" className={`${wordmark} w-auto`} />
    </span>
  )
}
