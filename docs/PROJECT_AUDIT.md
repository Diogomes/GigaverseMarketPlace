# Auditoria do ambiente — Fase 01

Data: 2026-10-27 · Escopo: estado real do ambiente antes de iniciar a plataforma Gigaverse3D (e-commerce + CRM).

## 1. Repositório alvo

| Item | Evidência |
|---|---|
| Caminho | `D:\ProjetosDio\GigaverseMarketPlace` |
| Git | Repositório inicializado, branch `master`, **sem commits** |
| Remote | `origin` → `https://github.com/Diogomes/GigaverseMarketPlace.git` (remoto vazio: `git ls-remote` sem refs) |
| Conteúdo | Apenas `.git/` (antes desta fase) |
| Identidade Git | `user.name` e `user.email` configurados |

Conclusão: **projeto novo (greenfield)**. Não há stack, dependências, testes, Docker ou configuração a preservar no repositório alvo.

## 2. Pacote de prompts

`D:\ProjetosDio\Gigaverse3D_Prompts_Claude_Code\Gigaverse3D_Prompts_Claude_Code\` contém `00` (contexto mestre), `01`–`12` (fases), `13` (retomada), `README.md` e `PROMPTS-COMPLETOS.md` (concatenação dos demais). Serve como especificação; não é código.

**Versão atualizada (2026-10-27):** `...\Gigaverse3D_Prompts_Claude_Code_Atualizado\` — **é a versão vigente**. Muda `00`, `07`, `08`, `09` e `README` para exigir a referência visual `referencia-visual-gigaverse3d.png` + `REFERENCIA-VISUAL-E-ESPECIFICACAO.md` (8 telas). Ambos copiados para `docs/design/`.

## 3. Sistema existente relacionado: `sistema-Gigaverse3d`

`D:\ProjetosDio\sistema-Gigaverse3d` — repositório separado (`github.com/Diogomes/sistema-Gigaverse3d`), em uso.

- **O que é:** sistema desktop/local de gestão da operação de impressão 3D: insumos (compras), estoque de materiais, orçamentos com cálculo de custo, pedidos, relatórios mensais, auditoria, transferência de dados entre computadores.
- **Stack:** Python 3 (biblioteca padrão, servidor HTTP próprio em `gigaverse/api.py`), SQLite (`data/gigaverse_custos.db`, fora do Git), front-end HTML/JS puro, empacotamento Windows (PyInstaller + Inno Setup, workflow GitHub Actions).
- **Modelo:** `materials`, `item_components`, `purchases`, `stock_movements`, `printers`, `quotes`, `quote_items`, `audit_log`, `settings`, `cost_line_defs`, `quote_cost_lines`. Estoque derivado de movimentações; custo por média ponderada.
- **Tamanho:** ~5,7 mil linhas (Python + JS + HTML + CSS).

**Relação com a nova plataforma:** domínios diferentes. O sistema existente gere *insumos e custo de produção* (back-office); a plataforma nova gere *produtos à venda, clientes e pedidos online*. Recomendação: **não migrar nem modificar** o sistema existente; tratá-lo como sistema independente e avaliar integração (ex.: exportar custo de produção para sugerir preço) somente após a Fase 09, como decisão do proprietário.

## 4. Imagem de referência da marca

- Encontrada: `D:\ProjetosDio\sistema-Gigaverse3d\assets\gigaverse-logo.png` — PNG 1254×1254, RGB **sem canal alfa**, fundo preto.
- Conteúdo: hexágono prata com contorno azul neon, "G" com planeta/órbita azul, wordmark "GIGAVERSE**3D**" (prata + azul) e slogan "IMPRESSÃO 3D • TECNOLOGIA • UNIVERSO".
- Imagem **não foi alterada**. Será copiada (não editada) para `frontend/public/brand/` na Fase 02.
- Limitação: por ter fundo preto opaco, funciona bem no tema escuro; para o tema claro é necessária uma versão oficial com fundo transparente (ou SVG) — **solicitar ao proprietário**. Até lá, no tema claro o logo será exibido dentro de um contêiner escuro, sem distorção.
- Cores aproximadas observadas (a refinar em design tokens): azul neon ~`#2F7BFF`/`#3D8BFF`, azul profundo ~`#0B3FD9`, prata ~`#B8BEC6`, grafite ~`#0A0C10`.

## 5. Runtimes e ferramentas na máquina

| Ferramenta | Versão encontrada | Situação para a stack proposta |
|---|---|---|
| Git | 2.45.1 | OK |
| Node.js / npm | 24.16.0 / 11.13.0 | OK (pnpm ausente → usar npm) |
| Java no PATH | **OpenJDK 11.0.31** (Adoptium) | **Insuficiente** — stack exige Java 21 |
| Java 21 | `C:\Program Files\Java\jdk-21.0.11` instalado | OK, mas precisa ser o `JAVA_HOME` do projeto |
| Maven | 3.9.10 | OK (projeto usará Maven Wrapper) |
| Gradle | ausente | Não necessário |
| Docker / Compose | 29.5.2 / v5.1.4 | Instalado, **daemon parado** no momento da auditoria |
| PostgreSQL client (`psql`) | ausente | Não necessário (Postgres via Docker) |
| Python | 3.12.10 | Usado pelo sistema existente; não pela plataforma |

## 6. Segredos e arquivos sensíveis

- `GigaverseMarketPlace`: nenhum arquivo além de `.git/`. Nada versionado.
- `sistema-Gigaverse3d`: `git ls-files` sem `.env`, `.db`, `.sqlite` ou arquivos de segredo; `.gitignore` exclui `data/` e bancos. Nenhuma ação necessária. (Observação de higiene, fora do escopo: `__pycache__/` existe no disco, mas está ignorado.)
- Pacote de prompts: apenas Markdown, sem segredos.

## 7. Integrações existentes

Nenhuma (pagamento, frete, e-mail, armazenamento) — todas serão criadas atrás de interfaces/adapters (ver `DECISIONS.md`).

## 8. Proposta de arquitetura

```
GigaverseMarketPlace/
├── frontend/   React + Vite + TypeScript + Tailwind, React Router, TanStack Query, RHF + Zod
├── backend/    Java 21 + Spring Boot 3 (Maven Wrapper), Security, Data JPA, Validation, Flyway, springdoc-openapi
├── infra/      docker-compose (dev), Dockerfiles prod, scripts de backup
├── docs/       auditoria, roadmap, decisões, arquitetura, segurança, operação
└── .github/workflows/  CI
```

Backend organizado por módulo de domínio (`catalog`, `inventory`, `account`, `cart`, `order`, `payment`, `shipping`, `crm`, `admin`, `audit`, `common`), cada um com `api` (controllers + DTOs), `service`, `domain` (entidades) e `repository`. Detalhes e alternativas em `DECISIONS.md`.

## 9. Riscos

Ver `RISKS.md`.
