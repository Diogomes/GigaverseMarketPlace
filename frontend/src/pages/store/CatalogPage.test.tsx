import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { CatalogPage } from './CatalogPage'

function renderCatalog(url = '/produtos') {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <Routes>
        <Route path="/produtos" element={<CatalogPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('CatalogPage', () => {
  it('identifies demo data and filters by search', async () => {
    renderCatalog()
    expect(screen.getByRole('note')).toHaveTextContent('Dados de demonstração')
    await userEvent.type(screen.getByRole('searchbox'), 'luminária')
    const headings = screen.getAllByRole('heading', { level: 3 })
    expect(headings.map((h) => h.textContent)).toEqual(['Luminária Lua 3D'])
  })

  it('reads the category from the URL', () => {
    renderCatalog('/produtos?categoria=acessorios')
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Acessórios')
  })

  it('shows an empty state with a reset action', async () => {
    renderCatalog('/produtos?q=inexistente')
    const empty = screen.getByText('Nenhum produto encontrado').parentElement!
    await userEvent.click(within(empty).getByRole('button', { name: 'Limpar filtros' }))
    expect(screen.getAllByRole('heading', { level: 3 }).length).toBeGreaterThan(1)
  })
})
