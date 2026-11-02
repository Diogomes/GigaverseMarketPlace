# Relatório de testes e qualidade — Fase 11

Execução em **2026-11-02**, Windows 11, JDK 21.0.11, Node 24, Docker Desktop (Postgres 17 via Testcontainers e docker compose), Microsoft Edge (Playwright `channel: msedge`). Todos os números abaixo são de execuções reais nesta data.

## 1. Resumo

| Camada | Comando | Resultado |
|---|---|---|
| Backend — unitários + integração (Postgres real) | `cd backend && ./mvnw verify` | **204 testes, 0 falhas, 0 ignorados** (28 classes) |
| Backend — cobertura (JaCoCo) | idem → `target/site/jacoco/index.html` | **linhas 91,7 % · instruções 91,4 % · ramos 67,4 %** |
| Frontend — lint | `npm run lint` (oxlint `--deny-warnings`) | 0 avisos |
| Frontend — tipos | `npm run typecheck` | 0 erros |
| Frontend — formatação | `npm run format:check` | ok |
| Frontend — componentes | `npm test` | **70 testes, 17 arquivos, 0 falhas** |
| Frontend — cobertura (v8) | `npm run test:coverage` | linhas 68,4 % · comandos 65,8 % · ramos 61,1 % · funções 57,1 % |
| Frontend — build | `npm run build` | ok; JS inicial 357,9 kB (**110,8 kB gzip**) |
| E2E (Playwright, ambiente local completo) | `npm run e2e` | **13 testes, 0 falhas** (última execução completa, 1,2 min) |

Fase 10 tinha 193 testes de backend, 68 de frontend e 3 E2E.

## 2. O que foi adicionado nesta fase

### Backend
- **`CartApiTest`** (9 testes): preço sempre do servidor, preço promocional + acréscimo de opções, opção obrigatória / de outro produto / duplicada recusadas, quantidade acima do estoque sinalizada, produto esgotado / rascunho / personalizado recusado, isolamento entre clientes (404 ao tocar item alheio), junção idempotente do carrinho do navegador, CSRF obrigatório e **criação concorrente do carrinho** (6 requisições simultâneas → 1 carrinho).
- **`QueryCountTest`** (2 testes) — proteção contra **N+1**: mede os comandos SQL do Hibernate nas listagens pública e administrativa de produtos e nas listagens de pedidos (cliente e painel) com 2 e com 10–12 linhas e exige contagens iguais. Passou sem alterações no código (o `default_batch_fetch_size: 50` e as consultas atuais já evitam N+1).
- **JaCoCo** no `pom.xml` (relatório no `verify`, sem limite mínimo imposto).

### Frontend
- **`ProductFormPage.test.tsx`** (2 testes): sem formulários aninhados, categoria mantida ao carregar a edição, lançamento de estoque sem enviar o formulário do produto.
- **`@vitest/coverage-v8`** + script `test:coverage`.

### E2E (`frontend/e2e/`)
| Arquivo | Fluxo |
|---|---|
| `store.spec.ts` | catálogo → busca → produto → carrinho de visitante → cadastro (junção do carrinho) → checkout (retirada + PIX) → histórico → cancelamento; logout; cliente não vê pedido alheio nem o painel |
| `admin.spec.ts` | admin cria produto, lança entrada de estoque e o produto aparece comprável na loja; admin confirma pagamento e leva o pedido por *Em produção → Pronto para retirada → Entregue*, e o cliente vê "Entregue" |
| `quality.spec.ts` | axe (WCAG 2.1 A/AA) em 10 páginas públicas × tema claro/escuro, 4 páginas da área do cliente e 5 do painel; celular 390×844 sem rolagem horizontal + menu móvel; teclado (link "Pular para o conteúdo", ordem de foco, `aria-invalid` nos erros); queda de rede no catálogo com "Tentar novamente"; API respondendo 503; sessão expirada → login com aviso → volta à página pedida |
| `support.ts` | helpers; `promoteToAdmin` só aceita `e2e-…@exemplo.com` e só roda com a loja em localhost |

## 3. Bugs encontrados e corrigidos

| # | Severidade | Problema | Correção | Teste que protege |
|---|---|---|---|---|
| B1 | **Alta** | Na edição de produto, os formulários de **estoque** e de **imagem** estavam aninhados dentro do formulário do produto (HTML inválido). Clicar em **Lançar** fazia um envio nativo GET (`/admin/produtos/23?`), recarregava a página e **não registrava a movimentação**; tudo que estava sendo editado se perdia. | O formulário do produto passou a envolver só o cartão "Informações"; os campos da coluna lateral e o botão "Salvar produto" se ligam a ele pelo atributo `form="product-form"`. Layout inalterado. | `ProductFormPage.test.tsx`, `admin.spec.ts` |
| B2 | **Média** | Ao abrir/recarregar diretamente `/admin/produtos/:id`, o select **Categoria** aparecia como "Selecione…" (as opções chegavam depois do valor inicial). Salvar falhava com "Selecione uma categoria." ou induzia a escolher outra. | O formulário só é montado depois de as categorias carregarem. | `ProductFormPage.test.tsx` |
| B3 | **Média** | Corrida na criação do carrinho: requisições simultâneas logo após o login/cadastro (junção do carrinho do navegador + leitura) violavam `carts_user_id_key` → **409** para o cliente. Visto no log do backend durante o E2E. | `INSERT … ON CONFLICT (user_id) DO NOTHING` seguido de leitura (`CartRepository.insertIfAbsent`). | `CartApiTest.concurrentFirstAccessCreatesASingleCart` (falha com o código antigo, passa com o novo) |
| B4 | Baixa (a11y, WCAG 1.4.1) | Link "política de privacidade" no meio do texto em *Meus dados* só se distinguia pela cor (sublinhado apenas no hover) — apontado pelo axe como *serious*. | Sublinhado permanente. | `quality.spec.ts` (axe na área do cliente) |

Ajuste de ambiente (não é bug do produto): o perfil `dev` agora permite 100 cadastros / 200 logins por IP a cada 15 min, porque a suíte E2E cria ~15 contas a partir de 127.0.0.1 e esbarrava no limite de produção (10). **Produção mantém os padrões.**

## 4. Performance básica

| Item | Situação |
|---|---|
| Bundle | JS inicial 110,8 kB gzip; CSS 9,6 kB gzip. Área do cliente, checkout, autenticação e painel em *chunks* sob demanda (2–18 kB cada). O chunk `formErrors` (119 kB / 36 kB gzip — zod + react-hook-form) só carrega em páginas com formulário. |
| Paginação | Todas as listagens são paginadas no servidor, com teto `MAX_PAGE_SIZE = 48`. |
| N+1 | Coberto por `QueryCountTest` (ver §2). |
| Imagens do catálogo | `loading="lazy"` (exceto a imagem principal, `eager`) com variantes de tamanho (`CatalogImage`/`ProductMedia`); metadados EXIF removidos no upload (Fase 10). |
| Arquivos grandes em `public/` | `brand/gigaverse-logo-original.png` (1,8 MB) e `gigaverse-logo-512.png` (417 kB) **não são carregados por nenhuma página**, mas vão para o `dist/`. Fotos de demonstração ~300 kB (JPG 1200×1200). |

## 5. Falhas e limitações restantes

| Severidade | Item | Impacto | Encaminhamento |
|---|---|---|---|
| Média | Cobertura de **ramos** do backend 67 % — menor em `mail` (32 %, envio SMTP real só testado com captura), `StorageConfig` (seleção S3 por configuração) e `ApiExceptionHandler` (ramos de erros raros) | Caminhos de erro menos exercitados | S3 e SMTP reais são validados no deploy (Fase 12) com sandbox do provedor |
| Média | Cobertura do frontend 68 % de linhas; páginas administrativas secundárias (relatórios, conteúdo, configurações) sem teste de componente | Regressões visuais nessas telas dependem do E2E/axe | Aceitável para o MVP; ampliar quando as telas ganharem lógica |
| Média | E2E roda contra o **banco de desenvolvimento** (não isolado) e só no Edge/Chromium | Dados de teste acumulam (`e2e-…@exemplo.com`, `Peça E2E …`); Firefox/Safari não testados | Na Fase 12 o CI sobe banco descartável por execução; avaliar WebKit |
| Baixa | Axe só verifica o que é automatizável; **não houve teste com leitor de tela real** (NVDA/VoiceOver) | Problemas de anúncio/ordem de leitura podem passar | Teste manual com NVDA antes do lançamento (item do `GO_LIVE_CHECKLIST`) |
| Baixa | Violações axe de impacto *moderate/minor* não bloqueiam o teste | Ajustes finos de a11y podem existir | Revisar o relatório completo do axe antes do lançamento |
| Baixa | Logos originais pesados em `public/` | Só aumentam o artefato de deploy | Mover para fora de `public/` na Fase 12 |
| Baixa | Sem teste de carga | Capacidade desconhecida sob pico | Teste de carga simples (k6) no ambiente de homologação (Fase 12) |
| Info | Gateway de pagamento e frete reais ainda não integrados (D-P01/D-P02) | Testes cobrem só o modo manual e o webhook HMAC de teste | Quando escolhidos, testar no **sandbox** do provedor |

**Nenhum teste chama serviço externo real**: banco via Testcontainers/docker compose local, e-mail capturado (teste) ou Mailpit (dev), pagamento por provedor de teste HMAC, storage local.

## 6. Como reproduzir

```bash
docker compose up -d                                  # Postgres + Mailpit locais
cd backend && ./mvnw verify                           # 204 testes + JaCoCo (JAVA_HOME = JDK 21)
cd frontend && npm run lint && npm run typecheck && npm run format:check && npm run test:coverage && npm run build
# E2E: backend (./mvnw spring-boot:run) e frontend (npm run dev) rodando
cd frontend && npm run e2e
```

## 7. Atualização — Fase 12 (2026-11-02)

| Verificação | Resultado |
|---|---|
| Backend `mvnw verify` | **213 testes, 0 falhas** (+9 de `ProductionConfigValidatorTest`); linhas 91,5 % · ramos 68,0 % |
| Frontend | lint, tipos, prettier ok; 70 testes; build ok |
| E2E (dev, código final) | 13/13 |
| Workflows (`ci.yml`, `release.yml`) | `actionlint` 1.7.7 sem apontamentos; scripts com `shellcheck` 0.10 sem apontamentos. **Não executados no GitHub** |
| Segurança | `npm audit --omit=dev`: 0; Trivy (repo: dependências + segredos ≥ HIGH; imagens ≥ HIGH com correção): 0 após `apk upgrade` |
| Simulação da pilha de produção | ver `DEPLOYMENT.md` §9: smoke 14/14, fluxo de conta pelo proxy, e-mail entregue, rollback automático, restauração, fail-fast sem vazar segredos, `X-Forwarded-For` forjado não burla o limitador |

## 8. Ampliação da cobertura — compra e administração (2026-11-02)

Todos os cenários usam **dados fictícios** (contas `e2e-…@exemplo.com`, produtos "… Fictício" com preço e estoque definidos no próprio teste) e só rodam contra o ambiente local/CI.

| Camada | Antes | Agora |
|---|---|---|
| Backend (`mvnw verify`) | 213 testes · linhas 91,5 % · ramos 68,0 % | **263 testes, 0 falhas** · linhas **92,6 %** · ramos **71,5 %** (carrinho 97 %/91 %, frete 100 %/92 %, estoque 91 %/85 %, pedidos 94 %/73 %) |
| Frontend (Vitest) | 70 testes | **91 testes, 0 falhas** · linhas 69,4 % |
| E2E (Playwright) | 13 cenários | **25 cenários, 0 falhas** |

### Unitários novos — backend (domínio puro, sem banco)
| Arquivo | O que garante |
|---|---|
| `unit/MoneyAndTimeTest` | 2 casas e arredondamento bancário; dia comercial no fuso de Recife (UTC−3) |
| `unit/CatalogInventoryDomainTest` | produto nasce rascunho/invisível; preço promocional; ordem de imagens/opções; entrada de estoque; disponível = saldo − reservado; nunca abaixo do reservado nem negativo; estoque baixo |
| `cart/service/PricingTest` | preço = promoção + acréscimos das opções; exatamente uma opção por grupo (falta, duplicada, de outro produto); limite por estoque disponível, teto 99, sob encomenda até 10, personalizado bloqueado; produto rascunho ou categoria inativa indisponíveis; preço sempre do servidor |
| `unit/OrderCartShippingDomainTest` | totais (itens + frete); histórico completo até "Entregue"; cancelamento com motivo; valores congelados após pagamento; ciclo do pagamento; linha do carrinho igual independente da ordem das opções; limites de quantidade; faixas de CEP (primeira que casa, limites inclusivos, CEP inválido), retirada opcional |
| `unit/AccountDomainTest` | e-mail normalizado; bloqueio após 5 falhas e desbloqueio; troca de senha/papel/desativação renovam o carimbo de sessão; anonimização; token de redefinição de uso único e com prazo |

### Integração nova — backend
`order/PurchaseJourneyApiTest`: jornada completa só pela API — admin cadastra produto com opções e promoção e lança estoque → vitrine sem expor saldo → carrinho (2 × R$ 220) → endereço em PE → frete R$ 15 → checkout cartão R$ 455 → baixa de estoque com movimento `SALE` ligado ao pedido → admin confirma pagamento → produção → envio com rastreio → entrega → cliente vê o mesmo histórico → e-mail e auditoria. Segundo cenário: um cliente esgota o estoque, outro não consegue comprar, reposição volta a vender, arquivamento tira da loja.

### Unitários novos — frontend
| Arquivo | O que garante |
|---|---|
| `pages/store/CartPage.test.tsx` | itens, opções e subtotal do servidor; trocar quantidade (PUT) e remover (DELETE) atualizam o total; problema do item (sem estoque) visível; carrinho vazio; carrinho do visitante com "Entrar para finalizar" |
| `pages/auth/schemas.test.ts` | regras de cadastro, login, recuperação e redefinição de senha |
| `lib/api.test.ts` | cookies de sessão; erros `problem+json` com campos; mensagens para 403/429/502; aviso de sessão expirada só fora de `/auth`; CSRF buscado, enviado e renovado após 403; multipart; `Idempotency-Key` |

### E2E novos
| Arquivo | Cenário |
|---|---|
| `purchase.spec.ts` | entrega + cartão com 3 produtos, troca de quantidade, remoção, frete e total exatos (R$ 165,00) e baixa de estoque conferida na API · último item esgota, outro visitante vê "esgotado", cancelamento devolve · preço alterado pelo admin com item no carrinho → vale o novo · sob encomenda (sem estoque) · visitante é levado ao login no checkout e o carrinho sobrevive |
| `admin-catalog.spec.ts` | validação do formulário de produto (campos obrigatórios, SKU, promoção ≥ preço) · SKU duplicado recusado pelo servidor · rascunho invisível → ativar publica → arquivar tira do ar · upload de imagem com texto alternativo (painel e loja) · nova categoria disponível no cadastro · cancelamento de pedido **já pago** pelo admin com motivo obrigatório devolve o estoque · CRM: busca do cliente, nota interna e etiqueta |
| `api.ts` | apoio: cria produtos/estoque e altera preço pela API administrativa com a sessão e o CSRF do navegador |

A pasta `e2e/` passou a ser verificada pelo `npm run typecheck` (`tsconfig.e2e.json`).

**Observação:** os cenários criam produtos e contas no banco de **desenvolvimento**; o catálogo local fica com muitos "… Fictício". Para limpar: `docker compose down -v` (apaga o banco local; o seed de demonstração volta na próxima subida — será preciso recriar o admin de teste). No CI o banco é descartável a cada execução.
