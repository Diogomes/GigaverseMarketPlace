# Registro de decisões

Duas seções separadas: **decisões técnicas** (propostas pela engenharia, sujeitas a aprovação) e **decisões pendentes do proprietário** (comerciais/operacionais — não serão inventadas; o código usará abstrações seguras até a definição).

## A. Decisões técnicas propostas

### D-T01 — Stack: React/Vite/TS + Spring Boot 3/Java 21 + PostgreSQL
- **Status:** proposta (aguarda aprovação na Fase 01).
- **Motivo:** é a stack do contexto mestre; repositório alvo vazio, sem custo de migração. JDK 21, Node 24, Maven e Docker já estão instalados.
- **Alternativas consideradas:**
  - *Reaproveitar o Python do `sistema-Gigaverse3d`*: servidor HTTP próprio sem framework, SQLite e sem autenticação — inadequado para loja pública multiusuário. Rejeitada.
  - *Next.js full-stack (TypeScript único)*: menos peças, SSR nativo para SEO; porém foge da stack especificada e mistura regras de negócio sensíveis (estoque, pedidos) num runtime menos maduro para transações. Não recomendada.
- **Consequência:** SEO da SPA depende de metadados por rota + sitemap gerado no backend; reavaliar pré-renderização na Fase 07 se necessário.

### D-T02 — Monorepo com `frontend/`, `backend/`, `infra/`, `docs/`
Um único repositório (`GigaverseMarketPlace`), CI por caminho. Build backend com **Maven Wrapper** (Maven já instalado; Gradle não). Frontend com **npm** (pnpm ausente).

### D-T03 — Autenticação por sessão server-side com cookie
- Spring Session (JDBC/PostgreSQL), cookie `HttpOnly`, `Secure` (prod), `SameSite=Lax`, proteção CSRF (token em cookie legível + header `X-XSRF-TOKEN`).
- **Alternativa:** JWT de acesso curto + refresh token em cookie HttpOnly. Rejeitada por ora: mais complexidade (rotação, revogação) sem benefício para um único front same-site; sessão permite logout/revogação imediatos.
- Nenhum token em `localStorage`.

### D-T04 — Backend modular por domínio
Pacotes `catalog`, `inventory`, `account`, `cart`, `order`, `payment`, `shipping`, `crm`, `admin`, `audit`, `common`. DTOs em todas as APIs; entidades JPA nunca expostas. Monólito modular — microsserviços não se justificam no volume esperado.

### D-T05 — Dados e consistência
PostgreSQL + Flyway; `ddl-auto=validate`. Dinheiro em `NUMERIC(12,2)` / `BigDecimal`, moeda BRL explícita. Estoque com movimentações auditáveis e lock pessimista/`UPDATE ... WHERE qty >= n` na baixa. Snapshots de produto e endereço no pedido.

### D-T06 — Integrações via adapters
`PaymentProvider`, `ShippingProvider`, `EmailSender`, `StorageService`. Implementações iniciais: pagamento **manual/pendente** (sem coleta de cartão, sem confirmação simulada), frete **tabela fixa configurável / retirada**, e-mail **log local / Mailpit** em dev, storage **disco local** em dev.

### D-T07 — Relação com `sistema-Gigaverse3d`
Sistemas independentes; nenhuma alteração no sistema existente. Integração (ex.: custo de produção → preço sugerido) fica como possível evolução pós-lançamento.

### D-T08 — Testes
JUnit 5 + Testcontainers (Postgres) no backend; Vitest + React Testing Library e Playwright no frontend. Testcontainers exige Docker em execução.

### D-T09 — Spring Boot 4.1 em vez de 3.x (Fase 02)
- **Motivo:** em 2026-10 o Spring Initializr só oferece 4.0.x/4.1.x; a linha 3.5 encerrou o suporte OSS em 2026-06, o que deixaria a plataforma sem correções de segurança antes do lançamento. O contexto mestre aceita "alternativas justificadas".
- **Impacto:** mesma arquitetura (Security, Data JPA, Validation, Flyway, OpenAPI via springdoc 3.x). Java 21 mantido.
- **Alternativa:** Spring Boot 3.5.x — rejeitada por estar fora do suporte gratuito.

### D-T10 — Ferramentas do frontend (Fase 02)
Template atual do Vite: React 19, TypeScript 7, Vite 8, **oxlint** (lint) + Prettier (formatação), Tailwind CSS 4, React Router, TanStack Query, lucide-react (ícones; glifo do Instagram próprio, pois a biblioteca removeu ícones de marcas). React Hook Form + Zod entram com os formulários (Fases 05/07).

### D-T11 — Referência visual aprovada (pacote atualizado)
`docs/design/referencia-visual-gigaverse3d.png` + `REFERENCIA-VISUAL-E-ESPECIFICACAO.md` (8 telas) são a referência obrigatória de layout. Dados do mockup (preços, clientes, métricas, personagens) **não** são usados como dados reais. Tokens de design em `frontend/src/styles/index.css`; cores de ação ajustadas para contraste AA com texto branco.

### D-T13 — Estrutura visual das 8 telas antecipada (2026-10-27, a pedido do proprietário)
- As telas 1–8 da referência foram construídas como componentes reais (loja, produto, área do cliente, login/cadastro, dashboard, produtos, CRM) **antes** das APIs. Só a camada de interface: nenhuma regra de negócio foi movida para o frontend.
- **Dados de demonstração** (`frontend/src/data/demo.ts`): fictícios, gerados de forma determinística, exibidos **somente em desenvolvimento** e sempre com o aviso "Dados de demonstração". O build de produção não contém esses dados (verificado no bundle) e mostra estados vazios.
- `/conta` e `/admin` abrem sem login apenas em desenvolvimento (`PreviewGuard`); em produção redirecionam para `/entrar`. Isso é navegação, não segurança — autorização real no backend (Fases 05/09).
- Ações que dependem de backend (salvar, arquivar, exportar, login, carrinho) validam os dados e mostram aviso da fase em que serão habilitadas; nada é simulado como concluído.
- Gráficos seguem o método dataviz: paleta categórica validada em claro/escuro, "status dos pedidos" como barra empilhada + legenda com valores (em vez da rosca do mockup), tabela alternativa para o gráfico de vendas.
- Personagens protegidos do mockup não foram usados; imagens de produto são placeholders "Foto pendente".

### D-T12 — Logo no tema claro
O logo oficial tem fundo preto opaco. Até existir versão transparente (D-P09), é exibido sobre um "chip" escuro em ambos os temas; recortes (emblema/wordmark) foram gerados por redimensionamento, sem distorção.

## B. Decisões pendentes do proprietário

| ID | Pergunta | Necessária até | Padrão seguro enquanto pendente |
|---|---|---|---|
| D-P01 | **Gateway de pagamento**: Mercado Pago, PagSeguro, Stripe, Asaas, Pagar.me? — ✅ *Meios decididos em 2026-10-27: **PIX e cartão**.* Gateway ainda pendente | Fase 06 (adapter real) / 12 | Pedido "Aguardando pagamento", confirmado só por admin; nenhum dado de cartão coletado até haver gateway |
| D-P02 | **Frete**: Melhor Envio, Correios direto, transportadora, retirada local, entrega própria? — ✅ *CEP de origem decidido em 2026-10-27: **53625-816**.* Provedor pendente | Fase 06 | Frete por tabela fixa configurável + retirada |
| D-P03 | **Hospedagem**: VPS (ex.: Hetzner, DigitalOcean), PaaS (Render, Railway, Fly.io), cloud (AWS/GCP)? Orçamento mensal? | Fase 12 | Imagens Docker portáveis |
| D-P04 | **Armazenamento de imagens** em produção (S3, Cloudflare R2, Backblaze B2…)? | Fase 12 | Adapter local em dev |
| D-P05 | **E-mail transacional**: Resend, Amazon SES, Brevo, SendGrid? Remetente/domínio? | Fase 12 | E-mails apenas logados/capturados localmente |
| D-P06 | **Política de troca, devolução e cancelamento** (prazos, quem paga frete, exceções) | Fase 06/07 | Regras configuráveis; cancelamento só antes de "Em produção"; texto marcado como rascunho |
| D-P07 | **Política de privacidade e termos** (revisão jurídica, LGPD, encarregado/DPO) | Fase 10 | Textos rascunho claramente identificados |
| D-P08 | **Domínio próprio** (já registrado? qual?) | Fase 12 | — |
| D-P09 | **Logo** em fundo transparente/SVG e versão horizontal; favicon | Fase 07 | Logo atual em contêiner escuro |
| D-P10 | **Produtos personalizados**: cliente envia arquivo STL/imagem? Orçamento antes do pagamento? Prazo de produção? Sinal/adiantamento? | Fase 04/06 | Tipo "personalizado" exige contato/orçamento; sem upload de arquivos de cliente até definição |
| D-P11 | **Dados fiscais**: emissão de NF-e, CNPJ/MEI, regime? | Fase 12 | Fora do escopo; registrar como pendência de lançamento |
| D-P12 | **Métricas do dashboard**: venda conta na criação, no pagamento ou na entrega? Vendas líquidas descontam frete/cancelamentos? | Fase 09 | Proposta: venda = pedido pago; líquida = pago − reembolsos − frete |
