import { expect, test } from '@playwright/test'
import { PRODUCT, register, logout, placeOrder, uniqueEmail } from './support'

/**
 * Fluxos críticos de ponta a ponta (backend real + banco de desenvolvimento com o catálogo de demonstração).
 * Produto usado: "Miniatura para Pintar" (estoque inicial 25 no seed).
 */
test('visitante monta o carrinho, cria conta e conclui um pedido que depois cancela', async ({ page }) => {
  // catálogo → produto
  await page.goto('/produtos')
  await page.getByRole('searchbox', { name: 'Buscar produtos' }).fill('miniatura pintar')
  await page.getByRole('link', { name: PRODUCT.name }).click()
  await expect(page.getByRole('heading', { level: 1, name: PRODUCT.name })).toBeVisible()

  // adiciona como visitante
  await page.getByRole('button', { name: 'Adicionar ao carrinho' }).click()
  await expect(page.getByText('Adicionado ao carrinho.')).toBeVisible()
  await page.goto('/carrinho')
  await expect(page.getByRole('link', { name: PRODUCT.name })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Entrar para finalizar' })).toBeVisible()

  // cadastro → o carrinho do navegador é enviado para a conta
  await register(page, uniqueEmail())
  await page.goto('/carrinho')
  await expect(page.getByRole('link', { name: PRODUCT.name })).toBeVisible()
  await page.getByRole('link', { name: 'Finalizar compra' }).click()

  // checkout: retirada + PIX
  await expect(page.getByRole('heading', { name: 'Finalizar compra' })).toBeVisible()
  await page.getByRole('button', { name: 'Confirmar pedido' }).click()
  await expect(page.getByText('Pedido recebido!')).toBeVisible()
  await expect(page.getByText(/chave PIX/)).toBeVisible()
  const orderTitle = await page.getByRole('heading', { level: 1, name: /Pedido #\d+/ }).textContent()

  // histórico e cancelamento
  await page.goto('/conta/pedidos')
  await expect(page.getByRole('heading', { name: orderTitle! })).toBeVisible()
  await page.getByRole('link', { name: 'Ver detalhes' }).first().click()
  await page.getByRole('button', { name: 'Cancelar pedido' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar pedido' }).click()
  await expect(page.getByText('Pedido cancelado.')).toBeVisible()
  await expect(page.getByText('Cancelado', { exact: true }).first()).toBeVisible()
})

test('logout encerra o acesso à área do cliente', async ({ page }) => {
  await register(page, uniqueEmail())
  await logout(page)
  await page.goto('/conta/pedidos')
  await expect(page).toHaveURL(/\/entrar\?next=%2Fconta%2Fpedidos/)
  await expect(page.getByRole('heading', { name: 'Bem-vindo de volta!' })).toBeVisible()
})

test('cliente não acessa pedido de outra pessoa nem o painel administrativo', async ({ page }) => {
  // cliente A faz um pedido
  await register(page, uniqueEmail())
  await placeOrder(page)
  const url = page.url().replace(/\?.*$/, '')
  await page.getByRole('button', { name: 'Cancelar pedido' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar pedido' }).click()
  await expect(page.getByText('Pedido cancelado.')).toBeVisible()
  await logout(page)

  // cliente B tenta abrir o pedido de A
  await register(page, uniqueEmail())
  await page.goto(url)
  await expect(page.getByRole('heading', { name: 'Página não encontrada' })).toBeVisible()

  // e o painel
  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: 'Acesso restrito' })).toBeVisible()
})
