import { execFileSync } from 'node:child_process'
import { expect, type Page } from '@playwright/test'

/**
 * Apoio comum aos testes E2E. Tudo roda contra o ambiente LOCAL de desenvolvimento
 * (docker compose + backend perfil dev + Vite) com contas fictícias `e2e-…@exemplo.com`.
 */
export const PRODUCT = { slug: 'miniatura-para-pintar', name: 'Miniatura para Pintar' }
export const PASSWORD = 'Frase-Longa-De-Teste-E2E-7'

export const uniqueEmail = (prefix = 'e2e') => `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@exemplo.com`

export async function register(page: Page, email: string, name = 'Cliente E2E') {
  await page.goto('/cadastro')
  await page.getByLabel('Nome completo').fill(name)
  await page.getByLabel('E-mail', { exact: true }).fill(email)
  await page.getByLabel('Senha', { exact: true }).fill(PASSWORD)
  await page.getByLabel('Confirmar senha').fill(PASSWORD)
  await page.getByLabel(/Li e aceito/).check()
  await page.getByRole('button', { name: 'Criar conta' }).click()
  await expect(page).toHaveURL(/\/conta$/)
}

export async function login(page: Page, email: string) {
  await page.goto('/entrar')
  await page.getByLabel('E-mail', { exact: true }).fill(email)
  await page.getByLabel('Senha', { exact: true }).fill(PASSWORD)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page).not.toHaveURL(/\/entrar/)
}

export async function logout(page: Page) {
  await page.getByRole('button', { name: 'Sair' }).first().click()
  await expect(page.getByRole('link', { name: 'Entrar' }).first()).toBeVisible()
}

/** Adiciona o produto de demonstração e confirma um pedido com retirada + PIX. Devolve o número do pedido. */
export async function placeOrder(page: Page): Promise<string> {
  await page.goto(`/produtos/${PRODUCT.slug}`)
  await page.getByRole('button', { name: 'Adicionar ao carrinho' }).click()
  await expect(page.getByText('Adicionado ao carrinho.')).toBeVisible()
  await page.goto('/checkout')
  await page.getByRole('button', { name: 'Confirmar pedido' }).click()
  await expect(page.getByText('Pedido recebido!')).toBeVisible()
  const title = await page.getByRole('heading', { level: 1, name: /Pedido #\d+/ }).textContent()
  return title!.match(/#(\d+)/)![1]
}

const E2E_EMAIL = /^(e2e|e2e-admin)-\d+-\d+@exemplo\.com$/

/**
 * Promove uma conta de teste recém-criada a ADMIN direto no Postgres do docker compose.
 * Salvaguardas: só aceita e-mails `e2e-…@exemplo.com` e só roda quando a loja testada é localhost.
 */
export function promoteToAdmin(email: string) {
  const base = process.env.E2E_BASE_URL ?? 'http://localhost:5173'
  if (!/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?/.test(base)) {
    throw new Error('promoteToAdmin só pode ser usado contra o ambiente local')
  }
  if (!E2E_EMAIL.test(email)) throw new Error(`e-mail fora do padrão de teste: ${email}`)
  const container = process.env.E2E_DB_CONTAINER ?? 'gigaverse3d-postgres-1'
  const out = execFileSync(
    'docker',
    [
      'exec',
      container,
      'psql',
      '-U',
      process.env.POSTGRES_USER ?? 'gigaverse3d',
      '-d',
      process.env.POSTGRES_DB ?? 'gigaverse3d',
      '-tAc',
      // e-mail já validado pela regex acima (sem aspas nem caracteres especiais)
      `UPDATE users SET role = 'ADMIN' WHERE email = '${email}' RETURNING id`,
    ],
    { encoding: 'utf8' },
  )
  if (!/^\d+/.test(out.trim())) throw new Error(`conta ${email} não encontrada para promoção`)
}

/** Cria uma conta de teste, promove a ADMIN e entra no painel. */
export async function loginAsNewAdmin(page: Page) {
  const email = uniqueEmail('e2e-admin')
  await register(page, email, 'Admin E2E')
  await logout(page)
  promoteToAdmin(email)
  await login(page, email)
  await expect(page).toHaveURL(/\/admin/)
  return email
}
