import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { summary } from '../../test/catalogFixtures'
import { ProductCard } from './ProductCard'

function renderCard(product = summary()) {
  return render(
    <MemoryRouter>
      <ProductCard product={product} />
    </MemoryRouter>,
  )
}

describe('ProductCard', () => {
  it('prefers the image uploaded through the API', () => {
    renderCard(
      summary({ image: { id: 1, url: '/media/products/1/foto.jpg', alt: 'Foto real', width: 800, height: 1000 } }),
    )
    const img = screen.getByRole('img', { name: 'Foto real' })
    expect(img).toHaveAttribute('src', '/media/products/1/foto.jpg')
    expect(img).toHaveAttribute('loading', 'lazy')
  })

  it('falls back to the demo asset of the same slug in development, with fixed dimensions', () => {
    renderCard()
    const img = screen.getByRole('img', { name: 'Raptor articulado azul e laranja sobre uma mesa' })
    expect(img).toHaveAttribute('src', '/images/products/articulated-raptor-toy-card-600x750.jpg')
    expect(img).toHaveAttribute('width', '600')
    expect(img).toHaveAttribute('height', '750')
  })

  it('shows an identified placeholder when there is no image at all', () => {
    renderCard(summary({ slug: 'sem-foto', name: 'Sem foto' }))
    expect(screen.getByRole('img', { name: /foto pendente/i })).toBeInTheDocument()
  })

  it('shows availability, promotion and BRL prices from the API', () => {
    renderCard(summary({ availability: 'MADE_TO_ORDER', promoPrice: 99.9, effectivePrice: 99.9 }))
    expect(screen.getByText('Sob encomenda')).toBeInTheDocument()
    expect(screen.getByText('Promoção')).toBeInTheDocument()
    expect(screen.getByText(/99,90/)).toBeInTheDocument()
  })

  it('never references user-supplied images', () => {
    const { container } = renderCard()
    expect(container.innerHTML).not.toContain('references-user-supplied')
  })
})
