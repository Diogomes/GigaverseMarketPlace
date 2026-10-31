import { useEffect } from 'react'

const SITE = 'Gigaverse3D'
const DEFAULT_DESCRIPTION = 'Gigaverse3D — impressão 3D, colecionáveis e peças personalizadas.'

function setMeta(name: string, content: string) {
  let tag = document.querySelector<HTMLMetaElement>(`meta[name="${name}"]`)
  if (!tag) {
    tag = document.createElement('meta')
    tag.name = name
    document.head.appendChild(tag)
  }
  tag.content = content
}

/** Título e descrição por página (SPA). */
export function usePageMeta(title: string | undefined, description?: string | null) {
  useEffect(() => {
    document.title = title ? `${title} | ${SITE}` : `${SITE} — Impressão 3D`
    setMeta('description', description?.slice(0, 160) || DEFAULT_DESCRIPTION)
  }, [title, description])
}
