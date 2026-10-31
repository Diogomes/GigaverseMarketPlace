import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Em desenvolvimento a API é acessada via proxy (mesma origem), o que simplifica
// cookies de sessão e CSRF. Em produção, use VITE_API_BASE_URL ou um proxy reverso.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_DEV_API_TARGET ?? 'http://localhost:8080', changeOrigin: false },
      '/media': { target: process.env.VITE_DEV_API_TARGET ?? 'http://localhost:8080', changeOrigin: false },
      '/sitemap.xml': { target: process.env.VITE_DEV_API_TARGET ?? 'http://localhost:8080', changeOrigin: false },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    css: false,
  },
})
