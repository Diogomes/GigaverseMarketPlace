import { defineConfig } from '@playwright/test'

/**
 * Testes E2E contra o ambiente local completo (docker compose + backend dev + `npm run dev`).
 * Usa o Microsoft Edge instalado (channel "msedge"), sem baixar navegadores.
 * Cria contas de teste e-mail `e2e-…@exemplo.com` no banco de DESENVOLVIMENTO — nunca rode contra produção.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  workers: 1,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    // Local: Edge instalado. CI: E2E_CHANNEL=chromium usa o Chromium baixado pelo Playwright.
    channel: process.env.E2E_CHANNEL === 'chromium' ? undefined : (process.env.E2E_CHANNEL ?? 'msedge'),
    headless: true,
    locale: 'pt-BR',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
})
