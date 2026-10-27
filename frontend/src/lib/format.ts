const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const shortDate = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
const dayMonth = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' })

/** Valores monetários trafegam em centavos (inteiros) para evitar erro de ponto flutuante. */
export function formatBRL(cents: number): string {
  return brl.format(cents / 100)
}

/** Datas ISO `YYYY-MM-DD` são tratadas como data local (sem deslocamento de fuso). */
function parseDay(iso: string): Date {
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function formatDate(iso: string): string {
  return shortDate.format(parseDay(iso))
}

export function formatDayMonth(iso: string): string {
  return dayMonth.format(parseDay(iso))
}

/** Converte "1.234,56" / "189,90" / "189.90" em centavos; retorna null se inválido. */
export function parseBRLToCents(input: string): number | null {
  const raw = input.trim().replace(/^R\$\s?/, '')
  if (!/^\d{1,3}(\.\d{3})*(,\d{1,2})?$|^\d+([.,]\d{1,2})?$/.test(raw)) return null
  const normalized = raw.includes(',') ? raw.replace(/\./g, '').replace(',', '.') : raw
  return Math.round(Number(normalized) * 100)
}

export function centsToInput(cents: number): string {
  return (cents / 100).toFixed(2).replace('.', ',')
}

export function slugify(text: string): string {
  return text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase()
}

/** Data local no formato ISO `YYYY-MM-DD`. */
export function isoDay(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}
