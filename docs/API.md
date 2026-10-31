# API — visão geral

Documentação interativa (perfil `dev`): <http://localhost:8080/swagger-ui.html> · especificação: `/v3/api-docs`.

Convenções:
- Prefixo `/api/v1`. JSON em UTF-8. Dinheiro como número decimal com 2 casas, moeda `BRL`.
- Erros em **RFC 9457** (`application/problem+json`): `status`, `title`, `detail` e, em validação, `errors: [{ field, message }]`. Nunca há stack trace.
- Paginação: `page` (0…), `size` (1…48). Resposta `{ content, page, size, totalElements, totalPages }`.
- Rotas `admin` exigem sessão com papel **ADMIN** ou **OPERATOR** (DELETE: só **ADMIN**) e o token CSRF (`X-XSRF-TOKEN`, lido do cookie `XSRF-TOKEN`). Sem sessão → 401; papel insuficiente → 403.

## Público

| Método | Rota | Descrição |
|---|---|---|
| GET | `/public/status` | Status da API |
| GET | `/public/categories` | Categorias ativas |
| GET | `/public/products?q&category&type&sort&page&size` | Produtos **ativos**; busca em português, insensível a acentos; `sort` = `relevance` \| `price_asc` \| `price_desc` \| `name` \| `newest`. Retorna `availability` (`IN_STOCK`, `OUT_OF_STOCK`, `MADE_TO_ORDER`, `ON_REQUEST`), nunca a quantidade em estoque |
| GET | `/public/products/{slug}` | Detalhe (opções, imagens). Rascunho/arquivado → 404 |
| GET | `/public/products?featured=true` | Vitrine da home (produtos marcados como destaque) |
| GET | `/sitemap.xml` | Páginas públicas + produtos ativos |
| GET | `/media/products/{id}/{arquivo}` | Imagem (modo storage local) com `nosniff` e CSP restritiva |

## Autenticação (`/auth`) e conta (`/account`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/auth/csrf` | público | Emite o token CSRF (cookie `XSRF-TOKEN`) |
| POST | `/auth/register` | público | Cria conta CUSTOMER (ignora `role`) e inicia sessão; e-mail repetido → 409 |
| POST | `/auth/login` | público | Sessão por cookie; falha → 401 genérico; bloqueio após 5 falhas; 429 por excesso |
| POST | `/auth/logout` | logado | Encerra a sessão |
| GET | `/auth/me` | público | `{ id, email, fullName, role }`; **204** sem sessão (evita erro 401 no console do visitante) |
| POST | `/auth/password/forgot` | público | Sempre 202 com a mesma mensagem |
| POST | `/auth/password/reset` | público | Token de uso único; encerra todas as sessões |
| GET/PUT | `/account/profile` | logado | Nome, telefone, consentimento de marketing (e-mail/papel não editáveis) |
| PUT | `/account/password` | logado | Exige senha atual; derruba as outras sessões |
| GET/POST | `/account/addresses` | logado | Lista/cria (máx. 10; o 1º vira principal) |
| PUT/DELETE | `/account/addresses/{id}` | dono | Endereço de outra pessoa → 404 |
| POST | `/account/addresses/{id}/default` | dono | Define o principal |

## Carrinho, checkout e pedidos do cliente

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/cart` | logado | Carrinho com preços e disponibilidade **recalculados** a cada leitura (`problem` por linha; `hasProblems`) |
| POST | `/cart/items` | logado | `{ productSlug, optionValueIds, quantity }` — soma à linha existente (máx. 99 por linha, 30 linhas) |
| PUT/DELETE | `/cart/items/{itemId}` | dono | Altera quantidade / remove |
| POST | `/cart/merge` | logado | Junta o carrinho do visitante após o login. **Idempotente**: fica a maior quantidade (não soma); itens inválidos são descartados |
| GET | `/checkout/shipping-options?addressId` | logado | Retirada (se habilitada) e faixas de entrega cujo intervalo de CEP contém o endereço (a 1ª que casar) |
| POST | `/checkout` | logado | Header **`Idempotency-Key`** obrigatório (repetição devolve o mesmo pedido). Corpo `{ shippingMethod: PICKUP\|DELIVERY, addressId, paymentMethod: PIX\|CARD, customerNote, expectedTotal }`. Total diferente de `expectedTotal` → 409; estoque insuficiente → 409. Reserva estoque e cria o pedido em `PENDING_PAYMENT` |
| GET | `/account/orders?group&page&size` | logado | Só os próprios pedidos; `group` = `ACTIVE` \| `DELIVERED` \| `CANCELLED` |
| GET | `/account/orders/{number}` | dono | Detalhe com itens (snapshot), histórico e instruções de pagamento. Pedido de outra pessoa → **404** |
| POST | `/account/orders/{number}/cancel` | dono | Só enquanto `PENDING_PAYMENT` (depois, via atendimento — política D-P06); devolve o estoque |

## Pagamentos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/payments/webhooks/{provider}` | público, sem CSRF | Corpo bruto + assinatura obrigatória (HMAC no adapter). Assinatura inválida → 401; evento repetido → 200 sem efeito (`payment_webhook_events`); valor divergente não confirma. O provedor atual `MANUAL` não tem webhook (404) |

O navegador **nunca** marca pedido como pago e nenhum dado de cartão é coletado (gateway: D-P01).

## Administração

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/admin/categories` | ADMIN, OPERATOR | Todas as categorias |
| POST | `/admin/categories` | ADMIN, OPERATOR | Cria (slug único → 409 se repetido) |
| PUT | `/admin/categories/{id}` | ADMIN, OPERATOR | Atualiza |
| DELETE | `/admin/categories/{id}` | ADMIN | Só sem produtos/subcategorias (senão 409) |
| GET | `/admin/products?q&categoryId&status&type&lowStock&page&size&sort&direction` | ADMIN, OPERATOR | Lista todos os status; `sort` ∈ name, sku, price, createdAt, updatedAt; `lowStock=true` = pronta entrega com disponível ≤ mínimo |
| GET | `/admin/products/{id}` | ADMIN, OPERATOR | Detalhe com estoque e `version` |
| POST | `/admin/products` | ADMIN, OPERATOR | Cria; slug gerado do nome se omitido; estoque inicia em 0 |
| PUT | `/admin/products/{id}` | ADMIN, OPERATOR | Atualiza; **exige `version`** (409 se outra pessoa alterou) |
| POST | `/admin/products/{id}/status` | ADMIN, OPERATOR | `{ status, version }` — arquivar/ativar/rascunho sem reenviar o produto (409 se a versão mudou; auditado) |
| DELETE | `/admin/products/{id}` | ADMIN | Só sem pedidos; com pedidos → 409 (arquive) |
| POST | `/admin/products/{id}/images` (multipart `file`, `altText`) | ADMIN, OPERATOR | JPG/PNG/WebP ≤ 5 MB, ≤ 8000 px, máx. 8 por produto |
| DELETE | `/admin/products/{id}/images/{imageId}` | ADMIN | Remove registro e arquivo (após commit) |
| GET | `/admin/products/{id}/stock-movements` | ADMIN, OPERATOR | Histórico paginado |
| POST | `/admin/products/{id}/stock-movements` | ADMIN, OPERATOR | `PURCHASE`/`RETURN` (> 0) ou `ADJUSTMENT` (± com motivo); saldo nunca negativo (409) |

| GET | `/admin/orders?status&q&customerId&from&to&page&size` | ADMIN, OPERATOR | Lista com filtros (q = número, nome ou e-mail; período pela data de criação) |
| GET | `/admin/orders/export?…mesmos filtros` | ADMIN | CSV (máx. 5000 linhas, sem endereço; acima disso 409); auditado (`ORDERS_EXPORTED`) |
| GET | `/admin/orders/{number}` | ADMIN, OPERATOR | Detalhe + cliente, `paidAt`, histórico com autor (`actorName`), `allowedNext` e `canConfirmPayment` |
| POST | `/admin/orders/{number}/status` | ADMIN, OPERATOR | `{ status, reason, trackingCode }` — só transições válidas (409); cancelar exige motivo; cancelar pedido pago só ADMIN; cancelamento devolve estoque |
| POST | `/admin/orders/{number}/confirm-payment` | ADMIN | Confirmação manual (modo sem gateway); registra auditoria |

Máquina de estados: `PENDING_PAYMENT → PAID | CANCELLED`; `PAID → IN_PRODUCTION | READY_FOR_PICKUP | SHIPPED | CANCELLED`; `IN_PRODUCTION → READY_FOR_PICKUP | SHIPPED | CANCELLED`; `READY_FOR_PICKUP → DELIVERED | CANCELLED`; `SHIPPED → DELIVERED`. Pedidos não pagos expiram após `APP_ORDERS_PENDING_EXPIRATION` (padrão 72 h) e o estoque volta.

### Dashboard, CRM, equipe e auditoria (Fase 09)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/admin/dashboard?from&to` | ADMIN, OPERATOR | Métricas do período (padrão 30 dias; máx. 366; fuso America/Recife). Definições em [METRICS.md](METRICS.md) |
| GET | `/admin/customers?q&status&tagId&segment&sort&page&size` | ADMIN, OPERATOR | Clientes (só papel CUSTOMER); q = nome, e-mail ou id; `segment` ∈ NO_PURCHASE, REPEAT, LAPSED; `sort` ∈ LAST_PURCHASE, TOTAL_SPENT, CREATED_AT, NAME |
| GET | `/admin/customers/export?q&status&tagId&segment` | ADMIN | CSV com dados mínimos (máx. 5000; 409 acima); proteção contra fórmulas; auditado sem o texto da busca |
| GET | `/admin/customers/{id}` | ADMIN, OPERATOR | Perfil mínimo, resumo de compras, aceite de marketing, etiquetas (equipe → 404) |
| PUT | `/admin/customers/{id}/tags` | ADMIN, OPERATOR | `{ tagIds: [...] }` — conjunto completo (máx. 20); auditado com adicionadas/removidas |
| GET / POST | `/admin/customers/{id}/notes` | ADMIN, OPERATOR | Notas internas (paginadas / `{ body }` até 2000); auditoria guarda só o tamanho |
| DELETE | `/admin/customers/{id}/notes/{noteId}` | ADMIN | Exclusão lógica, auditada |
| GET / POST | `/admin/customer-tags` | ADMIN, OPERATOR | Etiquetas com contagem / cria (`{ name }`, 2–30; duplicada sem diferenciar maiúsculas → 409) |
| DELETE | `/admin/customer-tags/{id}` | ADMIN | Remove de todos os clientes; auditado |
| GET / POST | `/admin/users` | ADMIN | Equipe (OPERATOR/ADMIN) / cria e envia **convite** (link de 48 h para a pessoa definir a senha) |
| POST | `/admin/users/{id}/invite` | ADMIN | Reenvia o convite |
| PUT | `/admin/users/{id}/role` | ADMIN | `{ role }`; não pode o próprio; sempre resta 1 ADMIN ativo; encerra as sessões da pessoa |
| POST | `/admin/users/{id}/disable` · `/enable` | ADMIN | Desativar encerra as sessões; não pode a própria conta |
| GET | `/admin/audit?action&entityType&entityId&actorId&from&to&page&size` | ADMIN | Trilha de auditoria com autor (nome/e-mail) e detalhes |
| GET | `/admin/settings` | ADMIN, OPERATOR | Configuração operacional em vigor, somente leitura e sem segredos |

Toda escrita administrativa grava `audit_log` (ação, entidade, ator, resumo — sem senhas, tokens, conteúdo de notas nem termos de busca).

## Códigos de resposta testados

| Código | Quando |
|---|---|
| 400 | Validação de campos, regra entre campos (ex.: promoção ≥ preço), parâmetros inválidos, upload inválido, URL malformada |
| 401 | Sem sessão em rota protegida |
| 403 | Papel insuficiente (cliente no admin, operador excluindo) ou sem token CSRF |
| 404 | Recurso inexistente ou não público |
| 409 | Total do checkout divergente, transição de status inválida, SKU/slug duplicado, versão desatualizada, exclusão com dependências, estoque insuficiente, limite de imagens |
| 413 | Arquivo acima de 5 MB |
| 429 | Excesso de tentativas (login, cadastro, recuperação) — com `Retry-After` |
