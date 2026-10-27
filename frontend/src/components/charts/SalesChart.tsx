import { useEffect, useId, useRef, useState } from 'react'
import { formatBRL, formatDayMonth, formatDate } from '../../lib/format'

type Point = { day: string; cents: number }

const HEIGHT = 240
const M = { top: 12, right: 12, bottom: 28, left: 72 }

function niceMax(value: number): number {
  if (value <= 0) return 100_00
  const exp = Math.pow(10, Math.floor(Math.log10(value)))
  const n = value / exp
  const step = n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10
  return step * exp
}

/** Série única (vendas/dia): área + linha 2px, crosshair com tooltip e tabela alternativa. */
export function SalesChart({ data }: { data: Point[] }) {
  const wrapRef = useRef<HTMLDivElement>(null)
  const [width, setWidth] = useState(640)
  const [hover, setHover] = useState<number | null>(null)
  const [showTable, setShowTable] = useState(false)
  const gradientId = useId()

  useEffect(() => {
    const el = wrapRef.current
    if (!el || typeof ResizeObserver === 'undefined') return
    const ro = new ResizeObserver(([entry]) => setWidth(Math.max(280, entry.contentRect.width)))
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  const total = data.reduce((s, p) => s + p.cents, 0)
  const max = niceMax(Math.max(...data.map((p) => p.cents), 0))
  const innerW = width - M.left - M.right
  const innerH = HEIGHT - M.top - M.bottom
  const x = (i: number) => M.left + (data.length <= 1 ? innerW / 2 : (i / (data.length - 1)) * innerW)
  const y = (v: number) => M.top + innerH - (v / max) * innerH
  const line = data.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(p.cents).toFixed(1)}`).join(' ')
  const area = `${line} L${x(data.length - 1)},${y(0)} L${x(0)},${y(0)} Z`
  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((t) => t * max)
  const labelEvery = Math.max(1, Math.ceil(data.length / 6))

  function onMove(e: React.PointerEvent<SVGRectElement>) {
    const rect = e.currentTarget.getBoundingClientRect()
    const rel = (e.clientX - rect.left) / rect.width
    setHover(Math.round(Math.min(1, Math.max(0, rel)) * (data.length - 1)))
  }

  const hovered = hover !== null ? data[hover] : null

  return (
    <figure>
      <div ref={wrapRef} className="relative">
        <svg
          width={width}
          height={HEIGHT}
          role="img"
          aria-label={`Vendas diárias de ${formatDate(data[0]?.day ?? '')} a ${formatDate(data.at(-1)?.day ?? '')}, total ${formatBRL(total)}`}
          className="block max-w-full"
        >
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--viz-1)" stopOpacity="0.28" />
              <stop offset="100%" stopColor="var(--viz-1)" stopOpacity="0" />
            </linearGradient>
          </defs>
          {yTicks.map((t) => (
            <g key={t}>
              <line x1={M.left} x2={width - M.right} y1={y(t)} y2={y(t)} stroke="var(--gv-border)" strokeWidth={1} />
              <text x={M.left - 8} y={y(t)} dy="0.32em" textAnchor="end" className="fill-fg-muted text-[11px]">
                {formatBRL(t).replace(',00', '')}
              </text>
            </g>
          ))}
          {data.map((p, i) =>
            i % labelEvery === 0 || i === data.length - 1 ? (
              <text
                key={p.day}
                x={x(i)}
                y={HEIGHT - 8}
                textAnchor={i === 0 ? 'start' : i === data.length - 1 ? 'end' : 'middle'}
                className="fill-fg-muted text-[11px]"
              >
                {formatDayMonth(p.day)}
              </text>
            ) : null,
          )}
          <path d={area} fill={`url(#${gradientId})`} />
          <path
            d={line}
            fill="none"
            stroke="var(--viz-1)"
            strokeWidth={2}
            strokeLinejoin="round"
            strokeLinecap="round"
          />
          {hovered && hover !== null && (
            <g pointerEvents="none">
              <line
                x1={x(hover)}
                x2={x(hover)}
                y1={M.top}
                y2={M.top + innerH}
                stroke="var(--gv-text-muted)"
                strokeDasharray="3 3"
              />
              <circle
                cx={x(hover)}
                cy={y(hovered.cents)}
                r={5}
                fill="var(--viz-1)"
                stroke="var(--gv-surface)"
                strokeWidth={2}
              />
            </g>
          )}
          <rect
            x={M.left}
            y={M.top}
            width={innerW}
            height={innerH}
            fill="transparent"
            onPointerMove={onMove}
            onPointerLeave={() => setHover(null)}
          />
        </svg>
        {hovered && hover !== null && (
          <div
            className="pointer-events-none absolute top-2 z-10 -translate-x-1/2 rounded-lg border border-border bg-bg-elevated px-3 py-2 text-xs shadow-card"
            style={{ left: Math.min(Math.max(x(hover), 70), width - 70) }}
          >
            <p className="text-fg-muted">{formatDate(hovered.day)}</p>
            <p className="font-semibold text-fg">{formatBRL(hovered.cents)}</p>
          </div>
        )}
      </div>
      <figcaption className="mt-2 flex items-center justify-between text-xs text-fg-muted">
        <span>Total no período: {formatBRL(total)}</span>
        <button
          type="button"
          className="font-semibold text-accent hover:underline"
          onClick={() => setShowTable((s) => !s)}
        >
          {showTable ? 'Ocultar tabela' : 'Ver como tabela'}
        </button>
      </figcaption>
      {showTable && (
        <div className="mt-3 max-h-56 overflow-auto rounded-lg border border-border">
          <table className="w-full text-sm">
            <thead className="sticky top-0 bg-surface-muted text-left">
              <tr>
                <th className="px-3 py-2 font-medium">Dia</th>
                <th className="px-3 py-2 text-right font-medium">Vendas</th>
              </tr>
            </thead>
            <tbody>
              {data.map((p) => (
                <tr key={p.day} className="border-t border-border">
                  <td className="px-3 py-1.5">{formatDate(p.day)}</td>
                  <td className="px-3 py-1.5 text-right tabular-nums">{formatBRL(p.cents)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </figure>
  )
}
