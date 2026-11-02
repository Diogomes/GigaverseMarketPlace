import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'
import { PRODUCT, loginAsNewAdmin, register, uniqueEmail } from './support'

/**
 * Qualidade transversal: acessibilidade automatizada (axe, WCAG 2.1 A/AA), responsividade,
 * navegação por teclado e comportamento em falhas de rede / sessão expirada / API fora do ar.
 */

async function expectNoSeriousA11yViolations(page: Page, label: string) {
  const { violations } = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze()
  const relevant = violations.filter((v) => v.impact === 'serious' || v.impact === 'critical')
  const summary = relevant.map((v) => `${v.id} (${v.impact}): ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)
  expect(summary, `violações de acessibilidade em ${label}`).toEqual([])
}

async function settle(page: Page) {
  await page.waitForLoadState('networkidle')
  await expect(page.getByText('Carregando…')).toHaveCount(0)
}

const PUBLIC_PAGES = [
  ['início', '/'],
  ['catálogo', '/produtos'],
  ['produto', `/produtos/${PRODUCT.slug}`],
  ['carrinho', '/carrinho'],
  ['entrar', '/entrar'],
  ['cadastro', '/cadastro'],
  ['sobre', '/sobre'],
  ['FAQ', '/perguntas-frequentes'],
  ['contato', '/contato'],
  ['personalizados', '/personalizados'],
] as const

for (const theme of ['light', 'dark'] as const) {
  test(`páginas públicas sem violações graves de acessibilidade (tema ${theme})`, async ({ page }) => {
    await page.addInitScript((t) => {
      try {
        localStorage.setItem('gv3d-theme', t)
      } catch {
        /* sem armazenamento */
      }
    }, theme)
    await page.emulateMedia({ colorScheme: theme })
    for (const [label, path] of PUBLIC_PAGES) {
      await page.goto(path)
      await settle(page)
      await expectNoSeriousA11yViolations(page, `${label} (${theme})`)
    }
  })
}

test('área do cliente e painel sem violações graves de acessibilidade', async ({ page }) => {
  await register(page, uniqueEmail())
  for (const path of ['/conta', '/conta/pedidos', '/conta/enderecos', '/conta/dados']) {
    await page.goto(path)
    await settle(page)
    await expectNoSeriousA11yViolations(page, path)
  }
  await page.getByRole('button', { name: 'Sair' }).first().click()
  await loginAsNewAdmin(page)
  for (const path of ['/admin', '/admin/produtos', '/admin/produtos/novo', '/admin/pedidos', '/admin/clientes']) {
    await page.goto(path)
    await settle(page)
    await expectNoSeriousA11yViolations(page, path)
  }
})

test.describe('celular (390×844)', () => {
  test.use({ viewport: { width: 390, height: 844 }, hasTouch: true })

  test('sem rolagem horizontal e com menu acessível', async ({ page }) => {
    for (const path of ['/', '/produtos', `/produtos/${PRODUCT.slug}`, '/carrinho', '/entrar', '/cadastro']) {
      await page.goto(path)
      await settle(page)
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
      expect(overflow, `rolagem horizontal em ${path}`).toBeLessThanOrEqual(1)
    }
    await page.goto('/')
    const toggle = page.getByRole('button', { name: 'Abrir menu' })
    await toggle.click()
    const nav = page.getByRole('navigation', { name: 'Principal (celular)' })
    await expect(nav).toBeVisible()
    await nav
      .getByRole('link', { name: /Produtos|Loja|Catálogo/ })
      .first()
      .click()
    await expect(page).toHaveURL(/\/produtos/)
  })
})

test('teclado: link "Pular para o conteúdo" e foco visível no cadastro', async ({ page }) => {
  await page.goto('/')
  await settle(page)
  await page.keyboard.press('Tab')
  const skip = page.getByRole('link', { name: 'Pular para o conteúdo' })
  await expect(skip).toBeFocused()
  await page.keyboard.press('Enter')
  await expect(page).toHaveURL(/#conteudo$/)

  // formulário inteiro preenchível só com teclado; erros anunciados e associados aos campos
  await page.goto('/cadastro')
  await page.getByLabel('Nome completo').focus()
  await page.keyboard.press('Tab')
  await expect(page.getByLabel('E-mail', { exact: true })).toBeFocused()
  await page.getByRole('button', { name: 'Criar conta' }).focus()
  await page.keyboard.press('Enter')
  const email = page.getByLabel('E-mail', { exact: true })
  await expect(email).toHaveAttribute('aria-invalid', 'true')
})

test('falha de rede no catálogo mostra erro e permite tentar de novo', async ({ page }) => {
  let block = true
  await page.route(/\/api\/v1\/public\/products(\?|$)/, (route) =>
    block ? route.abort('internetdisconnected') : route.fallback(),
  )
  await page.goto('/produtos')
  await expect(page.getByText('Não foi possível carregar os produtos')).toBeVisible({
    timeout: 20_000,
  })
  block = false
  await page.getByRole('button', { name: 'Tentar novamente' }).click()
  // não depende de um produto específico: o banco de dev acumula produtos criados por outros testes
  await expect(page.getByText(/\d+ produtos? encontrados?/)).toBeVisible()
  await expect(page.getByText('Não foi possível carregar os produtos')).toHaveCount(0)
})

test('API indisponível (503) não quebra a página de produto', async ({ page }) => {
  await page.route('**/api/v1/public/**', (route) =>
    route.fulfill({ status: 503, contentType: 'application/problem+json', body: '{"title":"Service Unavailable"}' }),
  )
  await page.goto(`/produtos/${PRODUCT.slug}`)
  // a casca da loja continua navegável e o usuário recebe uma mensagem, não uma tela em branco
  await expect(page.getByRole('navigation', { name: /Principal/ }).first()).toBeAttached()
  await expect(page.locator('main')).not.toBeEmpty({ timeout: 20_000 })
  await expect(page.getByText(/não foi possível|tente novamente|indispon/i).first()).toBeVisible({ timeout: 20_000 })
})

test('sessão expirada leva ao login com aviso e retorna à página após entrar', async ({ page, context }) => {
  const email = uniqueEmail()
  await register(page, email)
  await page.goto('/conta/pedidos')
  await settle(page)
  // simula expiração: o servidor deixa de reconhecer o cookie de sessão
  await context.clearCookies()
  await page.getByRole('navigation', { name: 'Minha conta' }).getByRole('link', { name: 'Endereços' }).click()
  await expect(page).toHaveURL(/\/entrar\?next=.*expirada=1/)
  await expect(page.getByText('Sua sessão expirou ou foi encerrada. Entre novamente para continuar.')).toBeVisible()

  await page.getByLabel('E-mail', { exact: true }).fill(email)
  await page.getByLabel('Senha', { exact: true }).fill('Frase-Longa-De-Teste-E2E-7')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page).toHaveURL(/\/conta\/enderecos$/)
})
