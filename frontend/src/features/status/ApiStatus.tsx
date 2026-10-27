import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../../lib/api'

type StatusResponse = { service: string; status: string; timestamp: string }

/** Indicador de conectividade com o backend (exibido apenas em desenvolvimento). */
export function ApiStatus() {
  const { data, isPending, isError } = useQuery({
    queryKey: ['public-status'],
    queryFn: () => apiGet<StatusResponse>('/api/v1/public/status'),
    retry: false,
  })

  let dot = 'bg-silver'
  let label = 'Verificando API…'
  if (isError) {
    dot = 'bg-danger'
    label = 'API indisponível'
  } else if (!isPending && data?.status === 'ok') {
    dot = 'bg-success'
    label = 'API conectada'
  }

  return (
    <p role="status" className="inline-flex items-center gap-2 text-sm text-fg-muted">
      <span aria-hidden className={`size-2 rounded-full ${dot}`} />
      {label}
    </p>
  )
}
