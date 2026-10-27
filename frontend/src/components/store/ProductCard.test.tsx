import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { listPublicProducts } from '../../data/store'
import { ProductCard } from './ProductCard'

function renderCard(slug: string) {
  const product = listPublicProducts().find((p) => p.slug === slug)!
  return render(
    <MemoryRouter>
      <ProductCard product={product} />
    </MemoryRouter>,
  )
}

describe('ProductCard images', () => {
  it('uses the 4:5 card asset with alt text, fixed dimensions and lazy loading', () => {
    renderCard('raptor-articulado')
    const img = screen.getByRole('img', { name: 'Raptor articulado azul e laranja sobre uma mesa' })
    expect(img).toHaveAttribute('src', '/images/products/articulated-raptor-toy-card-600x750.jpg')
    expect(img).toHaveAttribute('width', '600')
    expect(img).toHaveAttribute('height', '750')
    expect(img).toHaveAttribute('loading', 'lazy')
  })

  it('flags demo items and falls back to an identified placeholder without image', () => {
    renderCard('luminaria-lua-3d')
    expect(screen.getByText('Demo')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: /foto pendente/i })).toBeInTheDocument()
  })

  it('never references user-supplied images', () => {
    const { container } = renderCard('dragao-mecanico-azul')
    expect(container.innerHTML).not.toContain('references-user-supplied')
  })
})
