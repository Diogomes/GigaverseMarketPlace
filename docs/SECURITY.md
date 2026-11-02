# Segurança

Revisão da Fase 10 (2026-10-31). Este documento registra **controles implementados e testados**, riscos residuais e o que depende de configuração no deploy. Não é uma certificação: o sistema não é "100% seguro" e deve passar por teste de intrusão independente antes de crescer.

## 1. Revisão por tema

| Tema | Controle | Onde | Evidência (teste) |
|---|---|---|---|
| **Autenticação** | Sessão no servidor, cookie `GV3DSESSION` HttpOnly + SameSite=Lax + Secure (prod), troca de id no login, 2 h de inatividade; Argon2id; bloqueio após 5 falhas; mensagem única de erro | `account/security`, `SecurityConfig` | `AuthApiTest`, `AccountApiTest` |
| **Sessões revogáveis** | Carimbo de segurança: troca de senha, desativação, mudança de papel e anonimização derrubam todas as sessões | `SecurityStampFilter` | `StaffUsersApiTest`, `PrivacyApiTest` |
| **Autorização (RBAC)** | Regras no servidor por rota; ADMIN/OPERATOR/CUSTOMER; esconder botões é só conveniência | `SecurityConfig` | **`SecurityHardeningTest`** percorre *todas* as rotas registradas: nenhuma rota privada responde sem sessão (401) e nenhuma rota `/admin` responde a cliente (403); `AdminAccessTest` por papel |
| **IDOR** | Consultas da área do cliente sempre filtradas pelo dono (pedido alheio → 404) | `OrderRepository.findByNumberAndUserId`, `AddressService` | `CheckoutApiTest`, `AccountApiTest`, E2E |
| **CSRF** | Token em cookie `XSRF-TOKEN` → header `X-XSRF-TOKEN` em toda escrita (inclusive login); webhooks isentos e autenticados por HMAC | `SecurityConfig` | `AdminAccessTest.writesWithoutCsrfAreRejected` |
| **CORS** | Origens explícitas por ambiente (sem curinga; validado na subida) | `AppProperties.Cors` | `StatusControllerTest` |
| **XSS** | React escapa por padrão; nenhum `dangerouslySetInnerHTML` com dado de usuário; JSON-LD serializado com escape; API com CSP `default-src 'none'` | frontend, `SecurityConfig` | `SecurityHardeningTest` (cabeçalhos) |
| **SQL injection** | JPA/Criteria e `NamedParameterJdbcTemplate` com parâmetros ligados; ordenação só por lista branca; `LIKE` com escape | serviços | buscas com `%_\` em `CustomerCrmApiTest` |
| **Uploads** | Tipo pela assinatura dos bytes, ≤ 5 MB, ≤ 8000 px, máx. 8 por produto, nome gerado (UUID), servidos com `nosniff` + CSP `sandbox`; **metadados EXIF/XMP/GPS removidos** sem recomprimir | `ImageValidator`, `ImageMetadataStripper`, `MediaController` | `ProductImageApiTest`, `ImageMetadataStripperTest` |
| **Documentos privados** | Não existem uploads privados nem de clientes (D-P10). Só imagens de produto, públicas por natureza. Quando houver arquivos de cliente: bucket privado + URL assinada de curta duração | — | — |
| **Limites e abuso** | Corpo JSON ≤ 256 KB (413), multipart ≤ 5 MB; paginação ≤ 48 itens; período do dashboard ≤ 366 dias; exportações ≤ 5000 linhas; rate limit em login, cadastro, recuperação de senha, exportação de dados (5/h) e exclusão de conta | `BodySizeLimitFilter`, `PageResponse`, `RateLimiter` | `SecurityHardeningTest`, `AuthApiTest` |
| **Erros** | Respostas RFC 9457 sem stack trace nem mensagem interna; erro inesperado devolve só o código da requisição | `ApiExceptionHandler`, `server.error.*` | `HttpErrorDispatchTest` |
| **Logs** | Sem senha, token, conteúdo de nota, termos de busca nem mensagens do banco com valores; e-mail mascarado no log de e-mail; `X-Request-Id` validado antes de entrar no log | ver §3 | `SecurityHardeningTest.credentialsNeverReachTheLogs` |
| **Pagamentos** | Nenhum dado de cartão trafega ou é guardado; pedido nunca é marcado pago pelo navegador; webhook com HMAC, idempotência e conferência de valor | `payment/*` | `PaymentWebhookTest` |
| **Dependências** | `npm audit`: 0 vulnerabilidades. Backend: 152 dependências de runtime conferidas na base OSV: 0 conhecidas (31/10/2026). OWASP dependency-check exige chave da NVD — configurar no CI (Fase 12) | — | ver §5 |

## 2. Cabeçalhos HTTP

**API (Spring Security):** `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()`, `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; base-uri 'none'` (rotas `/api/**`), `Strict-Transport-Security` em HTTPS e `X-Request-Id` em toda resposta.

**Frontend (servidor web / CDN — configurar na Fase 12):**

```
Content-Security-Policy: default-src 'self'; img-src 'self' data: <domínio-das-imagens>; style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()
```

`style-src 'unsafe-inline'` é necessário enquanto houver estilos inline (larguras de gráficos); revisar depois. Validar a CSP do frontend em modo *report-only* antes de aplicar.

## 3. Logs e observabilidade

- **Correlação:** `RequestIdFilter` aceita `X-Request-Id` do proxy (só `[A-Za-z0-9-]{8,64}`) ou gera um UUID; o id vai para o MDC (toda linha de log), para `audit_log.request_id` e para a resposta. Em erro 500 o cliente recebe o código para informar ao suporte.
- **Formato:** em produção, JSON no padrão ECS (`logging.structured.format.console=ecs`), pronto para Loki/ELK/CloudWatch; em desenvolvimento, texto com `[requestId]`.
- **Nunca registrar:** senhas, hashes, tokens (sessão, CSRF, redefinição, convite), corpo de e-mails, dados de cartão, conteúdo de notas internas, texto livre de buscas, valores de constraints do banco.
- **Saúde:** `/actuator/health` (+ `/liveness`, `/readiness`), sem detalhes. Outros endpoints do Actuator não são expostos. Métricas (Prometheus/OpenTelemetry) ficam para a Fase 12, junto da escolha da hospedagem (D-P03).
- **Auditoria:** `audit_log` registra ações relevantes (login, pagamentos, status, catálogo, estoque, CRM, exportações, equipe, privacidade) com ator e requestId; consulta em `/admin/auditoria` (ADMIN).

## 4. Gestão de segredos e rotação

Checklist (repetir a cada deploy de produção e a cada 6 meses):

- [ ] Nenhum `.env` real no repositório (`.gitignore` cobre `.env*`, exceto `.env.example` com valores fictícios). Varredura de 31/10/2026: limpa, inclusive no histórico.
- [ ] Segredos de produção só no secret manager da hospedagem (variáveis de ambiente injetadas), nunca em imagem Docker, log ou ticket.
- [ ] `APP_BOOTSTRAP_ADMIN_*` removidas do ambiente logo após criar o primeiro ADMIN.
- [ ] Senha do banco exclusiva por ambiente; usuário da aplicação sem permissão de superusuário.
- [ ] Varredura de segredos automática no CI (gitleaks ou equivalente) — Fase 12.
- [ ] Acesso ao secret manager e ao banco de produção restrito e com MFA.

| Segredo | Rotação | Como rotacionar | Efeito colateral |
|---|---|---|---|
| Senha do PostgreSQL (`DB_PASSWORD`) | 12 meses ou suspeita | Criar nova senha no banco → atualizar secret → reiniciar app → revogar a antiga | Reinício rápido |
| Segredo do webhook do gateway | 12 meses ou suspeita | Gerar no painel do gateway → atualizar secret → validar evento de teste | Webhooks com assinatura antiga falham durante a troca |
| Credenciais SMTP/e-mail transacional | 12 meses ou suspeita | Nova chave no provedor → secret → reiniciar → revogar antiga | — |
| Chaves do storage S3 | 12 meses ou suspeita | Nova chave IAM com mesma política → secret → reiniciar → desativar antiga | — |
| Chave `age` dos backups | Ao sair alguém com acesso | Gerar novo par; backups novos com a nova chave; guardar a chave antiga (offline) enquanto houver backups cifrados com ela | — |
| Sessões de usuários | Em incidente | Reiniciar a aplicação encerra todas as sessões (ficam em memória — D-T16) | Todos precisam entrar de novo |
| Senhas de contas da equipe | Em incidente / saída de pessoa | Desativar a conta em `/admin/equipe` (encerra sessões na hora) | — |

## 5. Pendências e riscos residuais

- **Teste de intrusão independente** antes de campanhas de tráfego.
- Sessões e limitador de tentativas em memória: adequados a **uma instância** (a pilha de produção roda uma); com mais instâncias, Spring Session JDBC/Redis.
- Sem rate limit dedicado no checkout e nas rotas administrativas (mitigado por sessão, CSRF, idempotência e papéis). Avaliar WAF/limite no proxy (Caddy/CDN) quando houver tráfego.
- ✅ CI com `dependency-review` (PR), `npm audit` e Trivy (dependências, segredos e imagens). Pendente: Dependabot/Renovate para PRs automáticos de atualização.
- ✅ CSP da loja no nginx (`script-src 'self'`, sem inline — o script de tema virou `public/theme-init.js`; `frame-ancestors 'none'`, `object-src 'none'`). `style-src` ainda permite `'unsafe-inline'` (estilos inline do React).
- E-mails de marketing ainda não existem; quando existirem, cada mensagem deve ter link de descadastro sem login (ver `PRIVACY_DATA_MAP.md`).

## 6. Hardening da produção (Fase 12)

- **Containers**: API como `uid 10001` com raiz somente leitura e `/tmp` em tmpfs; loja em nginx sem privilégios (`uid 101`); todos com `cap_drop: ALL` e `no-new-privileges`. O Caddy roda como `uid 0` com apenas `NET_BIND_SERVICE` (risco residual baixo, R16).
- **Rede**: só o Caddy publica portas (80/443); banco e API apenas em redes internas; `/actuator/*` responde 404 para fora.
- **Proxy**: o Caddy descarta `X-Forwarded-*` vindos do cliente e envia o IP real — o limitador de tentativas depende disso. A API nunca deve ser exposta diretamente (confia em `X-Forwarded-*`).
- **TLS**: Let's Encrypt automático, HSTS de 1 ano, HTTP→HTTPS.
- **Configuração**: o perfil `prod` recusa subir com URL pública/CORS não-HTTPS, cookie sem `Secure`, sem SMTP ou com S3 incompleto, **antes** de conectar ao banco; a mensagem nunca contém valores.
- **Imagens**: `apk upgrade` no build (correções do Alpine posteriores à base); Trivy no CI bloqueia CVEs críticas com correção disponível.
- **Publicação**: só manual, com aprovação no ambiente `production`; tags imutáveis com SBOM.
