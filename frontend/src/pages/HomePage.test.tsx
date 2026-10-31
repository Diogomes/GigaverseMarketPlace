import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { jsonResponse, mockApi, renderApp } from '../test/authTestUtils'
import { categories, page, summary } from '../test/catalogFixtures'
import { HomePage } from './HomePage'

describe('HomePage', () => {
  it('renders the hero, a safe Instagram link and featured products from the API', async () => {
    const fetchSpy = mockApi(null, (url) => {
      if (url.includes('/public/categories')) return jsonResponse(200, categories)
      if (url.includes('/public/products')) return jsonResponse(200, page([summary()]))
      return undefined
    })
    renderApp(<HomePage />)
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Seu universo em 3D')
    const instagram = screen.getByRole('link', { name: /instagram/i })
    expect(instagram).toHaveAttribute('href', 'https://www.instagram.com/giga_verse3d/')
    expect(instagram).toHaveAttribute('rel', 'noopener noreferrer')
    expect(await screen.findByText('Raptor Articulado')).toBeInTheDocument()
    expect(await screen.findByText('Decoração')).toBeInTheDocument()
    expect(fetchSpy.mock.calls.some(([u]) => String(u).includes('featured=true'))).toBe(true)
  })
})
