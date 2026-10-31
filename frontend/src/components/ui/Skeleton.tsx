/** Placeholders de carregamento (respeitam prefers-reduced-motion via CSS global). */
export function CardsSkeleton({ count, aspect }: { count: number; aspect: string }) {
  return (
    <div role="status" aria-label="Carregando" className="mt-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="animate-pulse overflow-hidden rounded-card border border-border bg-surface">
          <div className={`${aspect} w-full bg-surface-muted`} />
          <div className="space-y-2 p-4">
            <div className="h-4 w-3/4 rounded bg-surface-muted" />
            <div className="h-4 w-1/3 rounded bg-surface-muted" />
          </div>
        </div>
      ))}
    </div>
  )
}
