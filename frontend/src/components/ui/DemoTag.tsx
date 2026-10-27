import { DEMO_MODE } from '../../data/store'

/** Selo "DEMO" sobre itens fictícios do seed de desenvolvimento. */
export function DemoTag({ className = '' }: { className?: string }) {
  if (!DEMO_MODE) return null
  return (
    <span
      className={`rounded bg-amber-400 px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-black ${className}`}
      title="Dados de demonstração"
    >
      Demo
    </span>
  )
}
