# Roadmap — Plataforma Gigaverse3D

Cada fase termina com resumo, comandos executados, resultados reais de testes e **aprovação do proprietário** antes da próxima. Commit ao fim de cada fase aprovada.

| # | Fase | Entregas principais | Depende de decisão do proprietário? | Status |
|---|---|---|---|---|
| 01 | Auditoria e plano | `PROJECT_AUDIT.md`, `ROADMAP.md`, `DECISIONS.md`, `RISKS.md` | — | ✅ Aprovada (2026-10-27) |
| 02 | Arquitetura e fundação | Monorepo, Vite/React, Spring Boot 4 + Java 21, docker-compose (Postgres), `.env.example`, health check, CORS, lint/format, `ARCHITECTURE.md`, `LOCAL_SETUP.md`, `ENVIRONMENT_VARIABLES.md` | Stack aprovada (D-T01, D-T09) | Concluída, aguardando aprovação |
| 03 | Banco e modelo de dados | Migrations Flyway, entidades, snapshots em pedido, estados, seed só em perfil `dev`, `DATA_MODEL.md` (Mermaid), testes Testcontainers | — | Pendente |
| 04 | Catálogo e estoque (backend) | APIs públicas/admin de produto e categoria, upload validado, `StorageService` (local/S3), movimentações de estoque com lock, OpenAPI | Armazenamento prod (D-P04) só no deploy | Pendente |
| 05 | Autenticação e contas | Registro/login/logout, sessão com cookie HttpOnly + CSRF, RBAC, reset de senha, endereços, rate limit, `EmailSender` (dev = log/Mailpit) | E-mail transacional (D-P05) só no deploy | Pendente |
| 06 | Carrinho, checkout e pedidos | Carrinho, totais no servidor, idempotência, máquina de estados, `PaymentProvider`/`ShippingProvider`, modo **pagamento manual/pendente** | Gateway (D-P01), frete (D-P02), política de troca (D-P06) | Pendente |
| 07 | Loja pública (frontend) | Design tokens, tema claro/escuro persistido, home, catálogo, produto, carrinho, checkout, páginas institucionais, SEO, Instagram | Logo transparente (D-P09), textos institucionais | Pendente |
| 08 | Área do cliente | Rotas protegidas, perfil, endereços, pedidos, consentimentos, sessão expirada, testes E2E | — | Pendente |
| 09 | Admin e CRM | Dashboard com métricas reais, produtos/estoque, pedidos, clientes, notas, etiquetas, auditoria, bootstrap seguro do admin | Definições de métricas (D-P10) | Pendente |
| 10 | Segurança, LGPD, observabilidade | `SECURITY.md`, `PRIVACY_DATA_MAP.md`, `INCIDENT_RESPONSE.md`, logs estruturados, backup/restauração | Revisão jurídica (D-P07) | Pendente |
| 11 | Testes e qualidade | Suíte completa, E2E Playwright, acessibilidade, performance, `TEST_REPORT.md` | — | Pendente |
| 12 | Deploy e lançamento | Dockerfiles prod, CI/CD com aprovação manual, runbooks, `DEPLOYMENT.md`, `GO_LIVE_CHECKLIST.md` | Hospedagem (D-P03), domínio (D-P08) | Pendente |

## Pré-requisitos de ambiente

1. Apontar `JAVA_HOME` para `C:\Program Files\Java\jdk-21.0.11` (o PATH atual usa Java 11) — ver `LOCAL_SETUP.md`.
2. Docker Desktop em execução (banco local e Testcontainers).

## Estrutura visual antecipada (2026-10-27)

A pedido do proprietário, a **interface** das telas 1–8 já existe com dados de demonstração (ver D-T13). As fases 07, 08 e 09 passam a focar em: ligar as telas às APIs reais, fluxos de checkout/sessão, testes E2E e ajustes finos — sem refazer o layout.

## Referência visual (telas → fases)

| Tela da referência | Fase |
|---|---|
| 1 Home escura · 2 Produto · 3 Home clara | 07 |
| 4 Área do cliente · 5 Login/cadastro | 08 |
| 6 Dashboard · 7 Produtos · 8 CRM | 09 |
