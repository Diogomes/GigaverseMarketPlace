import { describe, expect, it } from 'vitest'
import { formatBRL, formatDate, parseBRLToCents, slugify } from './format'

describe('format', () => {
  it('formats cents as BRL', () => {
    expect(formatBRL(18990).replace(/\s/g, ' ')).toBe('R$ 189,90')
  })

  it('formats ISO dates without timezone shift', () => {
    expect(formatDate('2026-01-01')).toBe('01/01/2026')
  })

  it.each([
    ['189,90', 18990],
    ['1.234,56', 123456],
    ['R$ 10', 1000],
    ['99.9', 9990],
    ['0,5', 50],
  ])('parses %s to %i cents', (input, cents) => {
    expect(parseBRLToCents(input)).toBe(cents)
  })

  it.each(['', 'abc', '1,234,56', '-5', '10,999'])('rejects invalid money %s', (input) => {
    expect(parseBRLToCents(input)).toBeNull()
  })

  it('slugifies names with accents', () => {
    expect(slugify('Luminária Lua 3D!')).toBe('luminaria-lua-3d')
  })
})
