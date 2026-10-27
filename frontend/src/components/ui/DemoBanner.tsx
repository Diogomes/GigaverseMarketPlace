import { FlaskConical } from 'lucide-react'
import { DEMO_MODE } from '../../data/store'

/** Aviso obrigatório sempre que uma tela exibe dados de demonstração. */
export function DemoBanner({ children }: { children?: string }) {
  if (!DEMO_MODE) return null
  return (
    <div
      role="note"
      className="flex items-start gap-2 rounded-lg border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm text-amber-800 dark:text-amber-200"
    >
      <FlaskConical aria-hidden className="mt-0.5 size-4 shrink-0" />
      <span>
        <strong>Dados de demonstração</strong> — ambiente de desenvolvimento.{' '}
        {children ?? 'Nada aqui representa pedidos, clientes ou valores reais.'}
      </span>
    </div>
  )
}
