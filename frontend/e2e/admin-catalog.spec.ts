import { expect, test, type Browser, type Page } from '@playwright/test'
import { createProduct, getProduct } from './api'
import { loginAsNewAdmin, placeOrder, register, uniqueEmail } from './support'

/** Administração do catálogo, pedidos e CRM com dados fictícios. */

// PNG 1×1 válido (o backend confere o conteúdo, não só a extensão)
const PNG_1PX = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64',
)

async function customerSession(browser: Browser): Promise<Page> {
  const ctx = await browser.newContext({ locale: 'pt-BR' })
  return ctx.newPage()
}

test('formulário de produto valida os campos antes de enviar e mostra o erro de preço promocional', async ({
  page,
}) => {
  await loginAsNewAdmin(page)
  await page.goto('/admin/produtos/novo')
  await page.getByRole('button', { name: 'Salvar produto' }).click()

  await expect(page.getByText('Nome deve ter ao menos 3 caracteres.')).toBeVisible()
  await expect(page.getByText('SKU: 3 a 32 caracteres, letras maiúsculas, números e hífen.')).toBeVisible()
  await expect(page.getByText('Selecione uma categoria.')).toBeVisible()
  await expect(page.getByText('Descreva o produto (mín. 10 caracteres).')).toBeVisible()
  await expect(page.getByLabel('Nome')).toHaveAttribute('aria-invalid', 'true')
  await expect(page).toHaveURL(/\/admin\/produtos\/novo$/)

  await page.getByLabel('Nome').fill('Produto Com Promo Errada')
  await page.getByLabel('SKU').fill('gv-minusculo')
  await page.getByLabel('Categoria').selectOption({ index: 1 })
  await page.getByLabel('Descrição').fill('Descrição fictícia longa o bastante.')
  await page.getByLabel('Preço (R$)', { exact: true }).fill('50,00')
  await page.getByLabel('Preço promocional (R$, opcional)').fill('60,00')
  await page.getByRole('button', { name: 'Salvar produto' }).click()
  await expect(page.getByText('Deve ser menor que o preço normal.')).toBeVisible()
  await expect(page.getByText('SKU: 3 a 32 caracteres, letras maiúsculas, números e hífen.')).toBeVisible()
  await expect(page).toHaveURL(/\/admin\/produtos\/novo$/)
})

test('SKU repetido é recusado pelo servidor e o erro aparece no campo', async ({ page }) => {
  await loginAsNewAdmin(page)
  const existing = await createProduct(page, { name: 'Original Fictício', price: 10, stock: 1 })
  const sku = (await page.request.get(`/api/v1/admin/products/${existing.id}`).then((r) => r.json())) as {
    sku: string
  }

  await page.goto('/admin/produtos/novo')
  await page.getByLabel('Nome').fill(`Cópia Fictícia ${Date.now()}`)
  await page.getByLabel('SKU').fill(sku.sku)
  await page.getByLabel('Categoria').selectOption({ index: 1 })
  await page.getByLabel('Descrição').fill('Tentativa de duplicar o SKU de outro produto.')
  await page.getByLabel('Preço (R$)', { exact: true }).fill('10,00')
  await page.getByRole('button', { name: 'Salvar produto' }).click()
  await expect(
    page
      .getByRole('alert')
      .filter({ hasText: /SKU|já existe|em uso/i })
      .first(),
  ).toBeVisible()
  await expect(page).toHaveURL(/\/admin\/produtos\/novo$/)
})

test('rascunho não aparece na loja; ativar publica; arquivar tira do ar', async ({ page, browser }) => {
  await loginAsNewAdmin(page)
  const draft = await createProduct(page, { name: 'Rascunho Fictício', price: 70, stock: 2, status: 'DRAFT' })
  const visitor = await customerSession(browser)

  await visitor.goto(`/produtos/${draft.slug}`)
  await expect(visitor.getByRole('heading', { name: 'Página não encontrada' })).toBeVisible()

  await page.goto(`/admin/produtos/${draft.id}`)
  await page.getByLabel('Status').selectOption({ label: 'Ativo' })
  await page.getByRole('button', { name: 'Salvar produto' }).click()
  await expect(page.getByText('Alterações salvas.')).toBeVisible()
  await visitor.goto(`/produtos/${draft.slug}`)
  await expect(visitor.getByRole('heading', { level: 1, name: draft.name })).toBeVisible()

  await page.goto(`/admin/produtos?q=${encodeURIComponent(draft.name)}`)
  await page.getByRole('button', { name: `Arquivar ${draft.name}` }).click()
  const dialog = page.getByRole('dialog')
  if (await dialog.isVisible().catch(() => false)) await dialog.getByRole('button', { name: /Arquivar/ }).click()
  await expect(page.getByRole('button', { name: `Reativar ${draft.name}` })).toBeVisible()
  await visitor.goto(`/produtos/${draft.slug}`)
  await expect(visitor.getByRole('heading', { name: 'Página não encontrada' })).toBeVisible()
  await visitor.context().close()
})

test('upload de imagem com texto alternativo aparece no painel e na página pública', async ({ page }) => {
  await loginAsNewAdmin(page)
  const p = await createProduct(page, { name: 'Com Foto Fictícia', price: 20, stock: 1 })
  await page.goto(`/admin/produtos/${p.id}`)

  // sem texto alternativo é recusado no cliente
  await page.locator('#image-file').setInputFiles({ name: 'foto.png', mimeType: 'image/png', buffer: PNG_1PX })
  await page.getByRole('button', { name: 'Enviar imagem' }).click()
  await expect(page.getByText('Descreva a imagem (texto alternativo, mín. 3 caracteres).')).toBeVisible()

  await page.getByLabel('Texto alternativo').fill('Foto fictícia de teste')
  await page.getByRole('button', { name: 'Enviar imagem' }).click()
  await expect(page.getByRole('button', { name: 'Remover imagem: Foto fictícia de teste' })).toBeVisible()

  await page.goto(`/produtos/${p.slug}`)
  const img = page.getByRole('img', { name: 'Foto fictícia de teste' }).first()
  await expect(img).toBeVisible()
  expect(await img.getAttribute('src')).toMatch(/\/media\//)
})

test('nova categoria fica disponível no cadastro de produto', async ({ page }) => {
  await loginAsNewAdmin(page)
  const name = `Categoria Fictícia ${Date.now() % 1e6}`
  await page.goto('/admin/categorias')
  await page.getByRole('button', { name: 'Nova categoria' }).click()
  const form = page.getByRole('form', { name: 'Nova categoria' })
  await form.getByLabel('Nome').fill(name)
  await form.getByLabel('Descrição (opcional)').fill('Criada pelo teste automatizado')
  await form.getByRole('button', { name: 'Salvar' }).click()
  await expect(page.getByText('Categoria criada.')).toBeVisible()
  await expect(page.getByRole('button', { name: `Editar ${name}` })).toBeVisible()

  await page.goto('/admin/produtos/novo')
  await expect(page.getByLabel('Categoria').locator('option', { hasText: name })).toHaveCount(1)
})

test('admin cancela pedido já pago: estoque volta e o cliente vê "Cancelado"', async ({ page, browser }) => {
  await loginAsNewAdmin(page)
  const p = await createProduct(page, { name: 'Pedido Cancelado Fictício', price: 45, stock: 2 })

  const customer = await customerSession(browser)
  await register(customer, uniqueEmail())
  await customer.goto(`/produtos/${p.slug}`)
  await customer.getByRole('button', { name: 'Adicionar ao carrinho' }).click()
  await expect(customer.getByText('Adicionado ao carrinho.')).toBeVisible()
  await customer.goto('/checkout')
  await customer.getByRole('button', { name: 'Confirmar pedido' }).click()
  await expect(customer.getByText('Pedido recebido!')).toBeVisible()
  const number = (await customer.getByRole('heading', { level: 1, name: /Pedido #\d+/ }).textContent())!.match(
    /#(\d+)/,
  )![1]
  expect((await getProduct(page, p.id)).stock!.available).toBe(1)

  await page.goto(`/admin/pedidos/${number}`)
  await page.getByRole('button', { name: 'Confirmar pagamento' }).click()
  await expect(page.getByText('Pagamento confirmado.')).toBeVisible()

  await page.getByLabel('Novo status').selectOption({ label: 'Cancelado' })
  await expect(page.getByText(/Pedido já pago: o estoque é devolvido/)).toBeVisible()
  await page.getByRole('button', { name: 'Aplicar' }).click()
  await expect(page.getByText('Informe o motivo do cancelamento.')).toBeVisible()
  await page.getByLabel('Motivo (obrigatório)').fill('Cliente pediu cancelamento (teste)')
  await page.getByRole('button', { name: 'Aplicar' }).click()
  await expect(page.getByText('Status alterado para "Cancelado".')).toBeVisible()
  expect((await getProduct(page, p.id)).stock!.available).toBe(2)

  await customer.reload()
  await expect(customer.getByText('Cancelado', { exact: true }).first()).toBeVisible()
  await customer.context().close()
})

test('CRM: admin encontra o cliente, adiciona nota interna e etiqueta', async ({ page, browser }) => {
  const customer = await customerSession(browser)
  const email = uniqueEmail()
  await register(customer, email, 'Cliente CRM Fictício')
  await placeOrder(customer)
  await customer.context().close()

  await loginAsNewAdmin(page)
  await page.goto('/admin/clientes')
  await page.getByLabel('Buscar cliente por nome, e-mail ou código').fill(email)
  await page
    .getByRole('link', { name: /Cliente CRM Fictício/ })
    .first()
    .click()
  await expect(page.getByText(email).first()).toBeVisible()

  await page.getByPlaceholder('Escreva uma nota interna…').fill('Prefere retirada aos sábados (nota fictícia).')
  await page.getByRole('button', { name: 'Adicionar nota' }).click()
  await expect(page.getByText('Prefere retirada aos sábados (nota fictícia).')).toBeVisible()

  const tag = `vip-teste-${Date.now() % 1e5}`
  await page.getByPlaceholder('Criar etiqueta').fill(tag)
  await page.getByRole('button', { name: 'Criar e adicionar etiqueta' }).click()
  await expect(page.getByRole('button', { name: `Remover etiqueta ${tag}` })).toBeVisible()

  // a nota fica gravada (persiste após recarregar)
  await page.reload()
  await expect(page.getByText('Prefere retirada aos sábados (nota fictícia).')).toBeVisible()
})
