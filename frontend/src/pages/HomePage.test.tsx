import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { HomePage } from './HomePage'

describe('HomePage', () => {
  it('renders the hero and a safe Instagram link', () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Seu universo em 3D')
    const instagram = screen.getByRole('link', { name: /instagram/i })
    expect(instagram).toHaveAttribute('href', 'https://www.instagram.com/giga_verse3d/')
    expect(instagram).toHaveAttribute('rel', 'noopener noreferrer')
    expect(instagram).toHaveAttribute('target', '_blank')
  })
})
