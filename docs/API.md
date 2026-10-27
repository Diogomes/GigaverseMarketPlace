# API — visão geral

Documentação interativa (perfil `dev`): <http://localhost:8080/swagger-ui.html> · especificação: `/v3/api-docs`.

Convenções:
- Prefixo `/api/v1`. JSON em UTF-8. Dinheiro como número decimal com 2 casas, moeda `BRL`.
- Erros em **RFC 9457** (`application/problem+json`): `status`, `title`, `detail` e, em validação, `errors: [{ field, message }]`. Nunca há stack trace.
- Paginação: `page` (0…), `size` (1…48). Resposta `{ content, page, size, totalElements, totalPages }`.
- Rotas `admin` exigem sessão com papel **ADMIN** ou **OPERATOR** (DELETE: só **ADMIN**) e o token CSRF (`X-XSRF-TOKEN`, lido do cookie `XSRF-TOKEN`). O login chega na Fase 05; até lá essas rotas respondem 401.

## Público

| Método | Rota | Descrição |
|---|---|---|
| GET | `/public/status` | Status da API |
| GET | `/public/categories` | Categorias ativas |
| GET | `/public/products?q&category&type&sort&page&size` | Produtos **ativos**; busca em português, insensível a acentos; `sort` = `relevance` \| `price_asc` \| `price_desc` \| `name` \| `newest`. Retorna `availability` (`IN_STOCK`, `OUT_OF_STOCK`, `MADE_TO_ORDER`, `ON_REQUEST`), nunca a quantidade em estoque |
| GET | `/public/products/{slug}` | Detalhe (opções, imagens). Rascunho/arquivado → 404 |
| GET | `/media/products/{id}/{arquivo}` | Imagem (modo storage local) com `nosniff` e CSP restritiva |

## Autenticação (`/auth`) e conta (`/account`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/auth/csrf` | público | Emite o token CSRF (cookie `XSRF-TOKEN`) |
| POST | `/auth/register` | público | Cria conta CUSTOMER (ignora `role`) e inicia sessão; e-mail repetido → 409 |
| POST | `/auth/login` | público | Sessão por cookie; falha → 401 genérico; bloqueio após 5 falhas; 429 por excesso |
| POST | `/auth/logout` | logado | Encerra a sessão |
| GET | `/auth/me` | logado | `{ id, email, fullName, role }` |
| POST | `/auth/password/forgot` | público | Sempre 202 com a mesma mensagem |
| POST | `/auth/password/reset` | público | Token de uso único; encerra todas as sessões |
| GET/PUT | `/account/profile` | logado | Nome, telefone, consentimento de marketing (e-mail/papel não editáveis) |
| PUT | `/account/password` | logado | Exige senha atual; derruba as outras sessões |
| GET/POST | `/account/addresses` | logado | Lista/cria (máx. 10; o 1º vira principal) |
| PUT/DELETE | `/account/addresses/{id}` | dono | Endereço de outra pessoa → 404 |
| POST | `/account/addresses/{id}/default` | dono | Define o principal |

## Administração

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/admin/categories` | ADMIN, OPERATOR | Todas as categorias |
| POST | `/admin/categories` | ADMIN, OPERATOR | Cria (slug único → 409 se repetido) |
| PUT | `/admin/categories/{id}` | ADMIN, OPERATOR | Atualiza |
| DELETE | `/admin/categories/{id}` | ADMIN | Só sem produtos/subcategorias (senão 409) |
| GET | `/admin/products?q&categoryId&status&page&size&sort&direction` | ADMIN, OPERATOR | Lista todos os status; `sort` ∈ name, sku, price, createdAt, updatedAt |
| GET | `/admin/products/{id}` | ADMIN, OPERATOR | Detalhe com estoque e `version` |
| POST | `/admin/products` | ADMIN, OPERATOR | Cria; slug gerado do nome se omitido; estoque inicia em 0 |
| PUT | `/admin/products/{id}` | ADMIN, OPERATOR | Atualiza; **exige `version`** (409 se outra pessoa alterou) |
| DELETE | `/admin/products/{id}` | ADMIN | Só sem pedidos; com pedidos → 409 (arquive com `status: ARCHIVED`) |
| POST | `/admin/products/{id}/images` (multipart `file`, `altText`) | ADMIN, OPERATOR | JPG/PNG/WebP ≤ 5 MB, ≤ 8000 px, máx. 8 por produto |
| DELETE | `/admin/products/{id}/images/{imageId}` | ADMIN | Remove registro e arquivo (após commit) |
| GET | `/admin/products/{id}/stock-movements` | ADMIN, OPERATOR | Histórico paginado |
| POST | `/admin/products/{id}/stock-movements` | ADMIN, OPERATOR | `PURCHASE`/`RETURN` (> 0) ou `ADJUSTMENT` (± com motivo); saldo nunca negativo (409) |

Toda escrita administrativa grava `audit_log` (ação, entidade, ator, resumo — sem dados sensíveis).

## Códigos de resposta testados

| Código | Quando |
|---|---|
| 400 | Validação de campos, regra entre campos (ex.: promoção ≥ preço), parâmetros inválidos, upload inválido, URL malformada |
| 401 | Sem sessão em rota protegida |
| 403 | Papel insuficiente (cliente no admin, operador excluindo) ou sem token CSRF |
| 404 | Recurso inexistente ou não público |
| 409 | SKU/slug duplicado, versão desatualizada, exclusão com dependências, estoque insuficiente, limite de imagens |
| 413 | Arquivo acima de 5 MB |
