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

### D-T14 — Assets de mockup integrados (2026-10-27)
- Imagens do pacote `assets-mockup` (conceitos originais gerados por IA) copiadas para `frontend/public/images/` e ligadas às telas existentes: hero, categorias, cards, galeria, banners, login, miniaturas de pedidos/admin. Detalhes em `IMAGE_ASSETS.md`.
- `references-user-supplied/` **não** foi copiada (possível propriedade intelectual de terceiros).
- O catálogo de demonstração do frontend foi refeito em torno dos 6 conceitos (selo **DEMO**). **Pendência:** o seed do backend (`R__demo_catalog.sql`) ainda tem os nomes anteriores; alinhar na Fase 04, quando o catálogo passar a vir da API com imagens.
- Hero e banners são arte provisória presente também no build de produção; devem ser trocados antes do lançamento (checklist em `IMAGE_ASSETS.md`).

### D-T16 — Fase 05: autenticação e contas
- **Sessão no servidor** (cookie `GV3DSESSION`, HttpOnly, SameSite=Lax, Secure em prod, 2 h de inatividade, só por cookie — sem id na URL) + **CSRF** (cookie `XSRF-TOKEN` → header `X-XSRF-TOKEN`) em todas as escritas, inclusive login. Id de sessão trocado no login (session fixation). Nenhum token no `localStorage`.
- **Senhas:** Argon2id (`{argon2}`; BCrypt aceito para migração), mínimo 10 caracteres, bloqueio de senhas óbvias/iguais ao e-mail (sem regras de composição — NIST 800-63B).
- **Força bruta:** bloqueio de 15 min após 5 falhas seguidas + limite por IP e por conta (429 com `Retry-After`). Mensagem única "E-mail ou senha inválidos." para todos os casos. Trade-off: um atacante pode bloquear temporariamente uma conta; aceito por ser temporário.
- **Carimbo de segurança** (`users.security_stamp`, V7): troca de senha/redefinição, desativação ou mudança de papel encerram as sessões antigas na próxima requisição (testado).
- **Recuperação:** token de 256 bits, só o SHA-256 no banco, 30 min, uso único, apenas o mais recente vale, resposta idêntica para e-mail existente ou não, e-mail de aviso após a troca.
- **Cadastro:** sempre `CUSTOMER`; campos extras (`role`, `status`) ignorados (testado). Trade-off: e-mail já cadastrado retorna 409 (revela existência no cadastro, não na recuperação); limitado por IP.
- **Primeiro administrador:** variáveis `APP_BOOTSTRAP_ADMIN_*` na primeira subida; nenhum usuário/senha no código.
- **Limitações registradas:** sessões e limitador ficam em memória — adequados a **uma instância**; com várias instâncias ou reinícios frequentes, migrar para Spring Session JDBC/Redis (Fase 12). Rotação do token CSRF no login não é feita (o token não é vinculado à sessão; mitigado por SameSite + troca de sessão). E-mail transacional real depende do provedor (D-P05); hoje só Mailpit em dev.
- **Consentimentos:** versão dos documentos legais registrada como `rascunho-2026-10` até a revisão jurídica (D-P07).

### D-T15 — Fase 04: catálogo, mídia e estoque
- **Autorização já aplicada no servidor** antes do login existir: `/api/v1/admin/**` exige ADMIN/OPERATOR, `DELETE` só ADMIN, e CSRF em escritas. Testado com usuários simulados; a autenticação real (Fase 05) só precisa fornecer a sessão.
- **Concorrência:** edição de produto exige `version` (409 em conflito); estoque usa `SELECT … FOR UPDATE` + CHECK no banco — teste com 20 baixas simultâneas sobre saldo 5 termina com exatamente 5 aceitas e saldo 0.
- **Uploads:** tipo detectado pela assinatura dos bytes (não pelo nome nem pelo Content-Type do cliente), extensão coerente, ≤ 5 MB, ≤ 8000 px (dimensões lidas só do cabeçalho), nome gerado pelo servidor (UUID), servidos com `nosniff` + CSP `sandbox`. Pendência: remover metadados EXIF (ex.: GPS) reprocessando a imagem — avaliar na Fase 10.
- **Storage S3:** adaptador implementado e testado contra **S3Mock (Adobe)** em container — as imagens oficiais do MinIO deixaram de estar disponíveis no Docker Hub/quay.io sem login. Não foi testado contra nenhum provedor de nuvem (D-P04 pendente).
- **Busca:** configuração `portuguese_unaccent` (V6) — insensível a acentos e maiúsculas. Limitação conhecida: plurais irregulares (“dragões” × “dragão”) não casam; avaliar busca por trigramas se necessário.
- **Seed `dev`** alinhado ao catálogo de demonstração do frontend (pendência da D-T14 resolvida). As imagens de demonstração continuam servidas pelo frontend; o seed não cria `product_images`.
- **Bug corrigido:** URL com UTF-8 inválido retornava 401 (despacho `/error` passava pela segurança); agora 400 — teste com servidor real.

### D-T17 — Fases 06 a 08: carrinho, checkout, pedidos, loja e área do cliente
- **Preço só no servidor:** `Pricing` é a única fonte de preço (produto + opções + promoção). O cliente envia `expectedTotal`; se o servidor calcular outro valor, o checkout responde 409 e a tela mostra o novo total.
- **Checkout idempotente:** header `Idempotency-Key` por tentativa; repetir a requisição devolve o mesmo pedido.
- **Estoque:** reservado no checkout com lock pessimista em ordem fixa de produto; devolvido no cancelamento e na expiração (72 h). Teste com 8 compradores simultâneos pela última unidade → exatamente 1 pedido.
- **Pagamento manual** (`MANUAL`) enquanto D-P01 estiver pendente: pedido nasce "Aguardando pagamento" e só ADMIN confirma; nenhum dado de cartão é coletado. O contrato `PaymentProvider` + webhook (HMAC, idempotência por evento, conferência de valor) está pronto e testado com um provedor de teste.
- **Frete:** retirada + faixas por intervalo de CEP configuráveis; sem faixas em produção só há retirada. Valores do perfil `dev` são fictícios (D-P02).
- **Carrinho do visitante** no `localStorage` (só a seleção, sem dados pessoais), enviado no login. A junção é **idempotente no servidor** (maior quantidade, não soma) — corrigiu um bug real em que recarregar a página durante o envio duplicava as quantidades.
- **`/auth/me` responde 204 sem sessão** (antes 401), eliminando erro no console de todo visitante. Em rotas protegidas, um 401 inesperado mostra "sua sessão expirou" e volta ao login preservando o destino (`next` validado contra open redirect).
- **Loja pública:** home, catálogo (busca na URL), produto (opções, JSON-LD), carrinho, checkout e páginas institucionais (**rascunho**) consomem a API. Imagens de demonstração por slug só no modo DEV; o bundle de produção não contém dados de demonstração (verificado no build).
- **Área do cliente:** pedidos com abas e paginação, detalhe com histórico, instruções e cancelamento, endereços, perfil e troca de senha. Pedido de outra pessoa → 404 (testado no backend, no frontend e no E2E).
- **E2E:** Playwright com o Edge instalado (sem baixar navegadores). 3 fluxos: compra completa + cancelamento, logout, isolamento entre clientes e admin.
- **Ainda com dados de demonstração:** telas de admin e CRM (Fase 09).

### D-T18 — Fase 09: painel administrativo, CRM e auditoria
- **Métricas no banco, não no navegador:** `DashboardService` agrega com SQL (índices V8 em `paid_at`/`placed_at`). Venda = pagamento confirmado (`paid_at`), fuso America/Recife; brutas incluem pedidos cancelados após pagamento, líquidas excluem esses e o frete. Definições e limitações em `METRICS.md`; testes com valores conhecidos, inclusive na virada do dia (UTC × Recife). **Proposta para D-P12 — aguarda confirmação.**
- **Gráfico de etapas:** os 7 status da API foram agrupados em 5 etapas para usar só as 5 cores já validadas da paleta (sem gerar cores novas).
- **Privilégio mínimo:** OPERATOR opera pedidos, catálogo, estoque e CRM. Só ADMIN confirma pagamento, exclui (notas, etiquetas, imagens, categorias, produtos), **exporta**, vê a **auditoria** e gerencia a **equipe** — regras em `SecurityConfig`, testadas por papel em `AdminAccessTest` (cliente → 403 em todas as rotas; anônimo → 401).
- **Equipe por convite:** o ADMIN informa nome/e-mail/papel; a senha nunca passa pelo administrador (link de 48 h com o mesmo token de uso único da recuperação). Ninguém altera o próprio papel/status; mudança de papel ou desativação encerra as sessões (carimbo de segurança). Clientes não viram equipe pela API (contas separadas). A regra "manter ao menos 1 ADMIN ativo" trava as linhas (`FOR UPDATE`); com o bloqueio de auto-alteração ela é defesa em profundidade.
- **CRM sem dados excessivos:** lista sem telefone/endereço; perfil mostra só o necessário (inclui o aceite de marketing, para saber se pode ofertar). Segmentos derivados apenas dos pedidos (sem compra, recorrentes, 90+ dias sem comprar). Origem do cadastro não é exibida (não é coletada). A tela orienta a não registrar dados sensíveis em notas/etiquetas.
- **Auditoria sem conteúdo pessoal:** notas registram só id e tamanho; exportações registram filtros e quantidade de linhas, não o texto buscado.
- **Exportações:** CSV pt-BR (`;`, decimal com vírgula, BOM), anexo `no-store`, máx. 5000 linhas — acima disso a API **recusa** (409) em vez de cortar em silêncio. Células de texto iniciadas por `= + - @` recebem apóstrofo (injeção de fórmula).
- **Concorrência:** arquivar/ativar exige a `version` lida (409 se outra pessoa alterou).
- **Configurações:** tela somente leitura; mudanças operacionais continuam por variável de ambiente + deploy (rastreável). Editor de conteúdo/banners ficou fora do escopo (tela informa).
- **Dados de demonstração:** pedidos, clientes e métricas fictícios do frontend foram removidos; o painel só mostra dados da API. Permanece apenas o catálogo de demonstração usado para imagens por slug em desenvolvimento (D-T14).
- **Pendências:** E2E do fluxo administrativo (precisa de conta ADMIN no banco de desenvolvimento) fica para a Fase 11; sessões/limitador seguem em memória (uma instância — D-T16).

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
