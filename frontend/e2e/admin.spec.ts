import { expect, test } from '@playwright/test'
import { login, loginAsNewAdmin, logout, placeOrder, register, uniqueEmail } from './support'

/** Administração: cadastro de produto com estoque e avanço de status de pedido até a entrega. */

test('admin cria produto, lança estoque e o produto aparece na loja', async ({ page }) => {
  await loginAsNewAdmin(page)
  const name = `Peça E2E ${Date.now()}`

  await page.goto('/admin/produtos/novo')
  await page.getByLabel('Nome').fill(name)
  await page.getByLabel('SKU').fill(`GV-E2E-${Date.now() % 1e8}`)
  await page.getByLabel('Categoria').selectOption({ index: 1 })
  await page.getByLabel('Descrição').fill('Produto criado pelo teste automatizado de ponta a ponta.')
  await page.getByLabel('Preço (R$)', { exact: true }).fill('49,90')
  await page.getByLabel('Status').selectOption({ label: 'Ativo' })
  await page.getByRole('button', { name: 'Salvar produto' }).click()

  await expect(page).toHaveURL(/\/admin\/produtos\/\d+$/)
  await expect(page.getByText('Produto criado. Agora adicione imagens e estoque.')).toBeVisible()
  const slug = (await page.getByText(/^URL: \/produtos\//).textContent())!.replace('URL: /produtos/', '').trim()

  const stock = page.getByRole('form', { name: 'Lançar movimentação' })
  await expect(page.getByText('Nenhuma movimentação.')).toBeVisible()
  await stock.getByLabel('Tipo de movimentação').selectOption('PURCHASE')
  await stock.getByLabel('Quantidade').fill('5')
  await stock.getByRole('button', { name: 'Lançar' }).click()
  await expect(page.getByText('Movimentação registrada.')).toBeVisible()
  await expect(stock.getByLabel('Quantidade')).toHaveValue('')

  // vitrine pública
  await logout(page)
  await page.goto(`/produtos/${slug}`)
  await expect(page.getByRole('heading', { level: 1, name })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Adicionar ao carrinho' })).toBeEnabled()
  await expect(page.getByText('R$ 49,90').first()).toBeVisible()
})

test('admin confirma pagamento e leva o pedido até "Entregue"; cliente acompanha', async ({ page }) => {
  const customer = uniqueEmail()
  await register(page, customer)
  const number = await placeOrder(page)
  await logout(page)

  await loginAsNewAdmin(page)
  await page.goto(`/admin/pedidos/${number}`)
  await expect(page.getByRole('heading', { level: 1, name: `Pedido #${number}` })).toBeVisible()

  await page.getByRole('button', { name: 'Confirmar pagamento' }).click()
  await expect(page.getByText('Pagamento confirmado.')).toBeVisible()

  for (const status of ['Em produção', 'Pronto para retirada', 'Entregue']) {
    await page.getByLabel('Novo status').selectOption({ label: status })
    await page.getByRole('button', { name: 'Aplicar' }).click()
    await expect(page.getByText(`Status alterado para "${status}".`)).toBeVisible()
  }
  // estado final: não há mais transições
  await expect(page.getByLabel('Novo status')).toHaveCount(0)
  await logout(page)

  await login(page, customer)
  await page.goto(`/conta/pedidos/${number}`)
  await expect(page.getByText('Entregue', { exact: true }).first()).toBeVisible()
})
