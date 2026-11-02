# Deploy

> **Estado em 2026-11-02:** nada foi publicado. Hospedagem (D-P03), domínio (D-P08), armazenamento de imagens (D-P04) e e-mail transacional (D-P05) **ainda não foram decididos**; nenhum serviço foi contratado ou provisionado. Este documento descreve o que está pronto e o que cada decisão destrava. A pilha foi validada numa **simulação local** (§7).

## 1. Arquitetura de produção

```
Internet ──HTTPS──▶ caddy (TLS Let's Encrypt, HSTS, HTTP→HTTPS)
                      │
                      ▼
                    web  (nginx sem privilégios: SPA estática + CSP; proxy /api, /media, /sitemap.xml)
                      │            rede "frontend"
                      ▼
                    api  (Spring Boot, perfil prod, usuário 10001, raiz somente leitura)
                      │            rede "backend"
                      ▼
                    postgres (sem porta publicada)        volume "media" (imagens, se APP_STORAGE_TYPE=local)
```

- **Mesma origem**: loja e API no mesmo domínio — cookie de sessão `Secure; HttpOnly; SameSite=Lax` e CSRF funcionam sem CORS entre domínios.
- **Uma instância da API** (sessões e limitador de tentativas em memória — R13). Escalar horizontalmente exige Spring Session JDBC/Redis antes.
- O atuador (`/actuator/*`) **não** é exposto: o nginx responde 404; os health checks usam a rede interna.
- Só o Caddy publica portas (80/443). Nunca publique a 8080 da API: ela confia em `X-Forwarded-*` porque só o proxy da própria pilha a alcança.

Arquivos: `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx/default.conf.template`, `docker-compose.prod.yml`, `infra/caddy/Caddyfile`, `infra/deploy/*.sh`, `.env.prod.example`.

## 2. Imagens

| Imagem | Base | Usuário | Tamanho (local) | Health check |
|---|---|---|---|---|
| `gigaverse3d-api` | `eclipse-temurin:21-jre-alpine` (camadas do Spring Boot extraídas) | `app` (10001) | ~450 MB | `GET /actuator/health/readiness` |
| `gigaverse3d-web` | `nginxinc/nginx-unprivileged:1.29-alpine` | `nginx` (101) | ~94 MB | `GET /healthz` |

- Build multi-stage: JDK, Maven, Node e fontes ficam no estágio de build.
- Os testes **não** rodam no `docker build` (precisam de Docker/Testcontainers); o CI os roda antes da publicação.
- Tags **imutáveis** `sha-<commit>`; `latest` nunca é usado nem aceito pelo `deploy.sh`.
- `release.yml` publica com SBOM e *provenance*.
- O build da loja remove os logos originais pesados (`gigaverse-logo-original.png`, `-512.png`) do artefato.

## 3. Configuração por ambiente

| Perfil | Onde | Banco | E-mail | Observações |
|---|---|---|---|---|
| `dev` | máquina local | docker compose (127.0.0.1) | Mailpit | seed de demonstração, Swagger, limites de tentativa elevados |
| `test` | `mvnw verify` / CI | Testcontainers | captura em memória | sem job de expiração |
| `prod` | servidor | rede interna | SMTP do provedor | logs JSON (ECS), sem Swagger, sem seed, **validação de configuração** |

O perfil `prod` **não sobe** com configuração insegura ou incompleta (`ProductionConfigValidator`, executado antes de abrir conexão com o banco — Flyway não roda): URL pública/CORS não HTTPS ou localhost, cookie sem `Secure`, sem SMTP (salvo `APP_MAIL_ALLOW_DISABLED=true`), remetente `.local`, S3 incompleto. Sem `DB_*`/`APP_CORS_ALLOWED_ORIGINS` também não sobe. No compose, variáveis obrigatórias ausentes falham antes de criar qualquer container (`${VAR:?mensagem}`). Lista completa: `ENVIRONMENT_VARIABLES.md`.

## 4. Pipelines (GitHub Actions)

| Workflow | Quando | O que faz |
|---|---|---|
| `ci.yml` | push em `master`, todo PR, manual | backend `mvnw verify` (+ JaCoCo); frontend lint, tipos, prettier, testes com cobertura, build; **E2E** Playwright num ambiente dev descartável do runner; segurança: `dependency-review` (PR), `npm audit` (prod, ≥ high), Trivy no repositório (dependências e segredos, ≥ high) e nas duas imagens (críticas com correção) |
| `release.yml` | **só manual** | confere que o CI do commit está verde → publica imagens no GHCR (`sha-<commit>`) → **se** marcado "deploy": job no ambiente `production` (exige aprovação de revisor) envia a pilha por SSH e roda `infra/deploy/deploy.sh` |

Nada é publicado automaticamente. Para habilitar o deploy (quando houver servidor):
1. GitHub → *Settings → Environments → New environment* `production` → **Required reviewers** (você) e, de preferência, *Deployment branches: master*.
2. Segredos do ambiente: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` (chave dedicada, sem senha, só para deploy), `DEPLOY_KNOWN_HOSTS` (`ssh-keyscan -H <host>`, conferido manualmente).
3. Variáveis do ambiente: `PUBLIC_BASE_URL`, `DEPLOY_PATH` (padrão `/opt/gigaverse3d`).
4. No servidor: `docker login ghcr.io` com um token **somente leitura de pacotes** (ou torne os pacotes públicos — não contêm segredos).

> **Não executado ainda:** os workflows foram validados com `actionlint` e shellcheck, mas **nunca rodaram no GitHub** (não houve push). A primeira execução pode exigir ajustes (ex.: tempo do E2E no runner, falsos positivos do Trivy).

## 5. Procedimento de deploy

No servidor, na pasta da pilha (`DEPLOY_PATH`), com `.env.prod` preenchido (modelo: `.env.prod.example`, `chmod 600`):

```bash
bash infra/deploy/deploy.sh sha-1a2b3c4
```

1. Baixa as imagens da tag.
2. **Backup pré-deploy** do banco (`infra/backup/backup.sh`, com checksum; cifrado se `BACKUP_AGE_RECIPIENT`).
3. Sobe a nova versão; o **Flyway aplica as migrations na inicialização** da API.
4. Espera os health checks (`docker compose up --wait`, padrão 240 s).
5. Roda o **smoke test** público somente leitura (`infra/deploy/smoke.sh`: loja, rota SPA, API, catálogo, CSRF, HSTS, CSP, nosniff, cookie `Secure`, atuador fechado, admin exige login, robots/sitemap, HTTP→HTTPS).
6. Registra a versão em `.deploy/current_tag` / `previous_tag` / `history.log`.

**Falhou nos passos 4–5** → volta sozinho para a versão anterior **se o esquema do banco não mudou**. Se a versão nova aplicou migrations, o script **para** (código 2) e mostra o comando de restauração — decisão humana (§6).

## 6. Migrations e rollback

- Migrations são **somente para frente** (`V<n>__*.sql`, nunca editar uma já aplicada; `ddl-auto: validate` confere o esquema na subida).
- Mudanças destrutivas seguem **expandir → migrar → contrair** em deploys separados (ex.: adicionar coluna nova e preencher; só remover a antiga num deploy seguinte). Assim a versão anterior continua compatível com o esquema e o rollback fica só de imagem.
- Rollback de imagem: `bash infra/deploy/rollback.sh` (volta para `previous_tag`) ou `rollback.sh <tag>`.
- Rollback com banco: `bash infra/deploy/rollback.sh <tag> backups/<arquivo>.dump` — para loja/API, faz **outro backup do estado atual**, restaura o arquivo indicado sobre o banco e sobe a tag. **Perde** o que foi gravado depois do backup (pedidos, cadastros): conferir antes com o responsável.

## 7. Simulação local (executada em 2026-11-02)

```bash
bash infra/deploy/simulate.sh up        # build + deploy da tag "local" + smoke test
bash infra/deploy/simulate.sh down      # para (mantém dados)
bash infra/deploy/simulate.sh destroy   # apaga volumes da simulação
```

Domínio fictício `loja.gigaverse3d.test` (TLS da CA interna do Caddy, portas só em 127.0.0.1), e-mails no Mailpit (perfil `sim`). Para abrir no navegador, adicione `127.0.0.1 loja.gigaverse3d.test` ao arquivo hosts e aceite o certificado local. Resultados em §9.

## 8. Decisões pendentes e o que cada uma destrava

### Hospedagem (D-P03)
Requisitos mínimos da pilha: **2 vCPU, 4 GB RAM, 40 GB SSD**, Docker + Compose, portas 80/443, IPv4 fixo, região no Brasil ou próxima (latência), snapshots/backup do disco.

| Opção | Encaixe com o que existe | Observações |
|---|---|---|
| **VPS com Docker** (ex.: Hetzner, DigitalOcean, Magalu Cloud, Locaweb, AWS Lightsail) | **Direto**: `docker-compose.prod.yml` + `release.yml` por SSH | Menor custo e menos peças; você administra SO, atualizações e backups do servidor. Recomendado para o MVP |
| **PaaS de containers** (ex.: Render, Railway, Fly.io) | Mesmas imagens; Caddy dispensável (TLS da plataforma); banco gerenciado | Menos operação; custo maior por recurso; disco efêmero → exige `APP_STORAGE_TYPE=s3` |
| **Cloud gerenciada** (AWS ECS/RDS, GCP Cloud Run/Cloud SQL) | Mesmas imagens; trocar o passo de deploy | Mais robusto e mais caro/complexo; faz sentido com tráfego maior |

Banco: no VPS, o Postgres do compose + backups cifrados fora do servidor; em PaaS/cloud, Postgres gerenciado com backup automático e PITR (`DB_URL` aponta para ele e o serviço `postgres` sai do compose).

### Domínio e DNS (D-P08)
- Registro `A` (e `AAAA` se houver IPv6) do domínio da loja → IP do servidor; TTL baixo (300 s) na semana do lançamento.
- `SITE_ADDRESS`/`PUBLIC_BASE_URL` com o domínio final; o Caddy emite e renova o certificado sozinho (portas 80/443 abertas).
- HSTS já é enviado (`max-age` 1 ano, `includeSubDomains`). **Só adicione `preload`** quando todos os subdomínios estiverem em HTTPS.
- CAA opcional: `0 issue "letsencrypt.org"`.

### E-mail transacional (D-P05)
Provedor SMTP (ex.: Amazon SES, Brevo, Resend, Postmark) com **domínio verificado**: SPF, DKIM e DMARC (`p=none` no início, depois `quarantine`). Preencher `SMTP_*` e `APP_MAIL_FROM`. Testar recuperação de senha e e-mail de pedido antes do lançamento.

### Armazenamento de imagens (D-P04)
- **VPS**: `APP_STORAGE_TYPE=local` serve (volume `media`), mas **incluir o volume no backup** (o `backup.sh` cobre só o banco — ver `OPERATIONS_RUNBOOK.md` §4).
- **Recomendado a médio prazo**: bucket S3-compatível (Cloudflare R2, Backblaze B2, AWS S3) com CDN; credenciais com permissão só no bucket. O adaptador S3 foi testado contra S3Mock, **nunca contra um provedor real**. Com S3, `img-src https:` da CSP já permite a URL pública.

### Pagamento (D-P01) e frete (D-P02)
- Hoje: `APP_PAYMENTS_PROVIDER=MANUAL` (PIX conferido e confirmado pelo admin). Frete: retirada + faixas de CEP por `APP_SHIPPING_ZONES_*` — **sem faixas em produção só aparece retirada**.
- Gateway: implementar o `PaymentProvider` do escolhido, testar no **sandbox**, configurar a URL do webhook `https://<domínio>/api/v1/payments/webhooks/<provedor>` (autenticado por HMAC, idempotente) e o segredo como variável de ambiente.

### Fiscal (D-P11)
Emissão de NF-e fora do escopo do sistema — decidir com o contador antes de vender.

## 9. Resultado da simulação (2026-11-02, Docker 29.9 / Compose v5.6)

| Verificação | Resultado |
|---|---|
| `docker compose ... config` com variáveis faltando | falha listando cada variável obrigatória |
| Build das duas imagens | ok |
| Deploy da tag `local` (primeira subida) | health checks ok; smoke test **14/14** |
| Usuários dos containers | api `uid=10001`, web `uid=101`; Caddy `uid=0` com todas as capabilities removidas exceto `NET_BIND_SERVICE` |
| Portas publicadas | só Caddy 80/443 (127.0.0.1 na simulação) |
| Logs | JSON ECS; avisos de produção: storage local, pagamento manual |
| Fluxo real pelo proxy (curl) | cadastro 201 → cookie de sessão `Secure` + `HttpOnly` → `/me` 200 → logout 204 → login 200 → pedidos 200; POST sem CSRF 403; "esqueci a senha" entregou e-mail no Mailpit com link `https://loja.gigaverse3d.test/redefinir-senha?token=…`; 0 linhas de erro no log |
| Segundo deploy (`v2`) | backup pré-deploy válido (88 KB + sha256), smoke 14/14, `previous_tag=local` |
| Deploy de versão quebrada (`v3-quebrada`, banco inacessível) | health check falhou → **rollback automático para `v2`** em ~25 s; tag atual permaneceu `v2` |
| Restauração do backup pré-deploy em banco de verificação | ok (versão do Flyway 8, contagens conferidas) |
| `X-Forwarded-For` forjado pelo cliente (7 IPs diferentes em "esqueci a senha", limite 5/IP) | 5 × 202 e depois **429** — o Caddy descarta o cabeçalho do cliente e o limitador usa o IP real |
| Scanner Trivy (repositório e imagens) e `npm audit --omit=dev` | 0 vulnerabilidades HIGH/CRITICAL com correção disponível (uma CVE crítica em `brotli-libs` da imagem base do nginx foi corrigida com `apk upgrade`) |
| API com configuração de prod inválida | não sobe; mensagem lista 4 variáveis; **nenhum valor/segredo no log; nenhuma conexão ao banco** |
