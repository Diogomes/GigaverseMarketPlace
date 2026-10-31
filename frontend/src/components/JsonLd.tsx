/**
 * Dados estruturados (schema.org). Escapa "<" para impedir que conteúdo do catálogo feche a tag
 * script (XSS). Só use dados reais — nunca avaliações ou preços inventados.
 */
export function JsonLd({ data }: { data: Record<string, unknown> }) {
  const safe = JSON.stringify(data).replace(/</g, '\\u003c')
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: safe }} />
}
