import { expect, test, type Browser, type Page } from '@playwright/test'
import { changePrice, createProduct, getProduct } from './api'
import { loginAsNewAdmin, register, uniqueEmail } from './support'

/**
 * Cenários de compra com dados fictícios. Os produtos são criados pela API administrativa numa sessão
 * de ADMIN separada (outro contexto do navegador), com preço e estoque conhecidos — assim os totais
 * esperados são exatos e os testes não dependem do catálogo de demonstração.
 */

async function adminSession(browser: Browser): Promise<Page> {
  const ctx = await browser.newContext({ locale: 'pt-BR' })
  const page = await ctx.newPage()
  await loginAsNewAdmin(page)
  return page
}

async function addToCart(page: Page, slug: string, quantity = 1) {
  await page.goto(`/produtos/${slug}`)
  for (let i = 1; i < quantity; i++) await page.getByRole('button', { name: 'Aumentar quantidade' }).click()
  await page.getByRole('button', { name: 'Adicionar ao carrinho' }).click()
  await expect(page.getByText('Adicionado ao carrinho.')).toBeVisible()
}

const money = (text: string) => new RegExp(`R\\$\\s?${text.replace('.', '\\.')}`)

test('entrega com cartão: vários itens, troca de quantidade, remoção, frete e total corretos', async ({
  page,
  browser,
}) => {
  const admin = await adminSession(browser)
  const vase = await createProduct(admin, { name: 'Vaso Fictício', price: 40, stock: 10 })
  const lamp = await createProduct(admin, { name: 'Luminária Fictícia', price: 50, promoPrice: 30, stock: 5 })
  const extra = await createProduct(admin, { name: 'Chaveiro Fictício', price: 12.5, stock: 3 })

  await register(page, uniqueEmail())
  await addToCart(page, vase.slug)
  await addToCart(page, lamp.slug)
  await addToCart(page, extra.slug)

  // carrinho: 3 vasos, luminária em promoção, remove o chaveiro → 3×40 + 30 = 150,00
  await page.goto('/carrinho')
  await page.getByLabel(`Quantidade de ${vase.name}`).selectOption('3')
  await expect(page.getByText(money('120,00')).first()).toBeVisible()
  await page.getByRole('button', { name: `Remover ${extra.name}` }).click()
  await expect(page.getByRole('link', { name: extra.name })).toHaveCount(0)
  await expect(page.getByText(money('150,00')).first()).toBeVisible()

  // checkout: entrega em Recife (faixa "Pernambuco" do perfil dev = 15,00) + cartão
  await page.getByRole('link', { name: 'Finalizar compra' }).click()
  await expect(page.getByRole('heading', { name: 'Finalizar compra' })).toBeVisible()
  await page.getByText('Entrega no endereço').click()
  await page.getByRole('button', { name: 'Adicionar endereço' }).click()
  await page.getByLabel('Destinatário').fill('Cliente Fictício da Silva')
  await page.getByLabel('CEP').fill('50010000')
  await page.getByLabel('Rua').fill('Rua do Teste Automatizado')
  await page.getByLabel('Número').fill('123')
  await page.getByLabel('Bairro').fill('Centro')
  await page.getByLabel('Cidade').fill('Recife')
  await page.getByLabel('UF').selectOption('PE')
  await page.getByRole('button', { name: 'Salvar endereço' }).click()
  await expect(page.getByText(/Entrega — Pernambuco/)).toBeVisible()
  await page.getByText('Cartão', { exact: true }).click()

  const summary = page.getByRole('heading', { name: 'Resumo do pedido' }).locator('..')
  await expect(summary.getByText(money('150,00')).first()).toBeVisible()
  await expect(summary.getByText(money('15,00')).first()).toBeVisible()
  await expect(summary.getByText(money('165,00'))).toBeVisible()
  await page.getByRole('button', { name: 'Confirmar pedido' }).click()
  await expect(page.getByText('Pedido recebido!')).toBeVisible()

  // detalhe do pedido guarda o que foi comprado
  await expect(page.getByRole('heading', { name: /Pagamento — Cartão/ })).toBeVisible()
  await expect(page.getByText(money('165,00')).first()).toBeVisible()
  await expect(page.getByText('Rua do Teste Automatizado', { exact: false }).first()).toBeVisible()

  // estoque reservado/baixado no servidor
  expect((await getProduct(admin, vase.id)).stock!.available).toBe(7)
  expect((await getProduct(admin, lamp.id)).stock!.available).toBe(4)
  expect((await getProduct(admin, extra.id)).stock!.available).toBe(3)
  await admin.context().close()
})

test('último item: a compra esgota o produto, ninguém mais compra e o cancelamento devolve o estoque', async ({
  page,
  browser,
}) => {
  const admin = await adminSession(browser)
  const rare = await createProduct(admin, { name: 'Peça Única Fictícia', price: 99.9, stock: 1 })

  await register(page, uniqueEmail())
  await addToCart(page, rare.slug)
  await page.goto('/checkout')
  await page.getByRole('button', { name: 'Confirmar pedido' }).click()
  await expect(page.getByText('Pedido recebido!')).toBeVisible()
  expect((await getProduct(admin, rare.id)).stock!.available).toBe(0)

  // outra pessoa vê esgotado
  const other = await page.context().browser()!.newContext({ locale: 'pt-BR' })
  const visitor = await other.newPage()
  await visitor.goto(`/produtos/${rare.slug}`)
  await expect(visitor.getByText('Produto esgotado no momento.')).toBeVisible()
  await expect(visitor.getByRole('button', { name: 'Adicionar ao carrinho' })).toHaveCount(0)

  // cliente cancela → volta a ter estoque
  await page.getByRole('button', { name: 'Cancelar pedido' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar pedido' }).click()
  await expect(page.getByText('Pedido cancelado.')).toBeVisible()
  expect((await getProduct(admin, rare.id)).stock!.available).toBe(1)
  await visitor.reload()
  await expect(visitor.getByRole('button', { name: 'Adicionar ao carrinho' })).toBeEnabled()
  await other.close()
  await admin.context().close()
})

test('preço alterado pelo admin depois de o item estar no carrinho: vale o preço novo do servidor', async ({
  page,
  browser,
}) => {
  const admin = await adminSession(browser)
  const p = await createProduct(admin, { name: 'Estatueta Fictícia', price: 80, stock: 5 })

  await register(page, uniqueEmail())
  await addToCart(page, p.slug, 2)
  await page.goto('/carrinho')
  await expect(page.getByText(money('160,00')).first()).toBeVisible()

  await changePrice(admin, p.id, 95)
  await page.reload()
  await expect(page.getByText(money('190,00')).first()).toBeVisible()
  await page.getByRole('link', { name: 'Finalizar compra' }).click()
  const summary = page.getByRole('heading', { name: 'Resumo do pedido' }).locator('..')
  await expect(summary.getByText(money('190,00')).first()).toBeVisible()
  await page.getByRole('button', { name: 'Confirmar pedido' }).click()
  await expect(page.getByText('Pedido recebido!')).toBeVisible()
  await expect(page.getByText(money('190,00')).first()).toBeVisible()
  await admin.context().close()
})

test('sob encomenda: compra sem estoque físico respeita o limite por pedido', async ({ page, browser }) => {
  const admin = await adminSession(browser)
  const custom = await createProduct(admin, { name: 'Busto Sob Encomenda', price: 250, type: 'MADE_TO_ORDER' })

  await register(page, uniqueEmail())
  await page.goto(`/produtos/${custom.slug}`)
  await expect(page.getByText(/7 dias úteis|produção/i).first()).toBeVisible()
  await addToCart(page, custom.slug, 2)
  await page.goto('/checkout')
  await page.getByRole('button', { name: 'Confirmar pedido' }).click()
  await expect(page.getByText('Pedido recebido!')).toBeVisible()
  await expect(page.getByText(money('500,00')).first()).toBeVisible()
  await admin.context().close()
})

test('visitante não finaliza sem conta e o carrinho sobrevive ao login', async ({ page, browser }) => {
  const admin = await adminSession(browser)
  const p = await createProduct(admin, { name: 'Miniatura Fictícia', price: 35, stock: 4 })
  const email = uniqueEmail()

  // cria a conta em outra aba e sai, para depois entrar com o carrinho de visitante
  await register(page, email)
  await page.getByRole('button', { name: 'Sair' }).first().click()

  await addToCart(page, p.slug, 2)
  await page.goto('/checkout')
  await expect(page).toHaveURL(/\/entrar/)
  await page.getByLabel('E-mail', { exact: true }).fill(email)
  await page.getByLabel('Senha', { exact: true }).fill('Frase-Longa-De-Teste-E2E-7')
  await page.getByRole('button', { name: 'Entrar' }).click()
  // volta para onde estava (checkout) já com o carrinho do navegador enviado para a conta
  await expect(page).toHaveURL(/\/checkout$/)
  await expect(page.getByRole('heading', { name: 'Finalizar compra' })).toBeVisible()
  await page.goto('/carrinho')
  await expect(page.getByLabel(`Quantidade de ${p.name}`)).toHaveValue('2')
  await expect(page.getByText(money('70,00')).first()).toBeVisible()
  await admin.context().close()
})
