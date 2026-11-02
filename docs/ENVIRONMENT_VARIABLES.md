# Variáveis de ambiente

Nunca versionar `.env`/`.env.prod`. Em produção, usar o secret manager da hospedagem ou um `.env.prod` só no servidor (`chmod 600`) — modelo em [`.env.prod.example`](../.env.prod.example), pilha em `docker-compose.prod.yml` (ver `DEPLOYMENT.md`).

**Validação na inicialização (perfil `prod`)**: `ProductionConfigValidator` roda antes de qualquer conexão com o banco e recusa subir se `APP_FRONTEND_BASE_URL` ou `APP_CORS_ALLOWED_ORIGINS` não forem HTTPS públicos, se `SESSION_COOKIE_SECURE=false`, se faltar `SMTP_HOST` (salvo `APP_MAIL_ALLOW_DISABLED=true`), se `APP_MAIL_FROM` usar domínio `.local` ou se `s3` estiver incompleto. A mensagem lista os **nomes** das variáveis, nunca os valores.

## Backend

| Variável | Obrigatória em prod | Padrão em `dev` | Descrição |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | sim (`prod`) | `dev` | Perfil ativo |
| `SERVER_PORT` | não | `8080` | Porta HTTP |
| `DB_URL` | **sim** | `jdbc:postgresql://localhost:5432/gigaverse3d` | JDBC do PostgreSQL |
| `DB_USERNAME` | **sim** | `gigaverse3d` | Usuário do banco |
| `DB_PASSWORD` | **sim (segredo)** | `gigaverse3d_dev` | Senha do banco (valor dev só vale para o compose local) |
| `APP_CORS_ALLOWED_ORIGINS` | **sim** | `http://localhost:5173` | Origens separadas por vírgula; curinga `*` é rejeitado na inicialização |
| `SESSION_COOKIE_SECURE` | não (padrão `true`) | `false` | Cookie de sessão só via HTTPS |
| `APP_SHIPPING_ORIGIN_POSTAL_CODE` | não | `53625816` | CEP de origem dos envios (8 dígitos, validado) |

### Pedidos, pagamento e frete

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `APP_PAYMENTS_PROVIDER` | não | `MANUAL` | Provedor de pagamento. Só `MANUAL` existe (pedido aguarda confirmação do admin); gateway real depende de D-P01 |
| `APP_ORDERS_PENDING_EXPIRATION` | não | `PT72H` | Prazo (ISO-8601) para pedido não pago expirar e devolver o estoque |
| `APP_SHIPPING_PICKUP_ENABLED` | não | `true` | Oferece retirada no checkout |
| `APP_SHIPPING_ZONES_<n>_*` (`NAME`, `FROM_POSTAL_CODE`, `TO_POSTAL_CODE`, `PRICE`, `MIN_DAYS`, `MAX_DAYS`) | não | nenhuma | Faixas de entrega por intervalo de CEP (vale a primeira que casar). **Sem faixas, só a retirada é oferecida.** O perfil `dev` traz valores **fictícios** (PE 15,00 · Nordeste 25,00 · demais 39,00) que não valem em produção (D-P02) |

### Autenticação e e-mail

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `APP_FRONTEND_BASE_URL` | **sim em prod** (HTTPS público) | `http://localhost:5173` | Base dos links enviados por e-mail e do sitemap. No `docker-compose.prod.yml` vem de `PUBLIC_BASE_URL` |
| `SESSION_TIMEOUT` | não | `2h` | Inatividade até a sessão expirar |
| `APP_MAIL_FROM` | **sim em prod** | `Gigaverse3D <nao-responda@gigaverse3d.local>` | Remetente (domínio verificado no provedor, com SPF/DKIM/DMARC) |
| `SMTP_HOST` / `SMTP_PORT` | **sim em prod** (D-P05) | dev: `localhost` / `1025` (Mailpit); prod: — / `587` | Servidor SMTP do provedor |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | prod: conforme provedor | — | Credenciais SMTP. **Senha é segredo** |
| `SMTP_AUTH` / `SMTP_STARTTLS` | não | `true` / `true` (prod) | Autenticação e STARTTLS **obrigatório** |
| `APP_MAIL_ALLOW_DISABLED` | não | `false` | `true` permite subir em prod **sem** e-mail (só registra destinatário mascarado e assunto). Decisão consciente: recuperação de senha deixa de funcionar |
| `APP_BOOTSTRAP_ADMIN_EMAIL` / `_PASSWORD` / `_NAME` | só na 1ª subida | — | Cria o primeiro ADMIN se não existir nenhum. **Senha é segredo**; remova as variáveis depois |

### Armazenamento de imagens

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `APP_STORAGE_TYPE` | não | `local` | `local` (disco — em prod só com volume persistente com backup e **uma** instância; a API avisa no log) ou `s3` |
| `APP_STORAGE_LOCAL_ROOT` | não | `./storage` | Pasta do modo local (ignorada pelo Git) |
| `APP_STORAGE_S3_BUCKET` | se `s3` | — | Bucket |
| `APP_STORAGE_S3_REGION` | se `s3` | — | Região (ex.: `sa-east-1`; R2 usa `auto`) |
| `APP_STORAGE_S3_ENDPOINT` | não | — | Endpoint de provedores S3-compatíveis (R2, B2, MinIO) |
| `APP_STORAGE_S3_PATH_STYLE` | não | `false` | `true` para provedores que exigem path-style |
| `APP_STORAGE_PUBLIC_BASE_URL` | se `s3` | — | URL pública/CDN usada nas respostas da API |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | se `s3` | — | **Segredos.** Lidos pela cadeia padrão do AWS SDK (ou papel da instância); nunca em arquivo versionado |

O backend falha na inicialização com mensagem clara se `APP_STORAGE_TYPE=s3` sem bucket, região ou URL pública.

Valores fixos em `application.yml` (alterar exige deploy): `app.store.currency=BRL`, `app.store.payment-methods=PIX,CARD`.

Variáveis futuras (registradas para não serem esquecidas): credenciais do gateway de pagamento e segredo de webhook (D-P01), provedor de frete (D-P02).

## Pilha de produção (`docker-compose.prod.yml`)

| Variável | Obrigatória | Descrição |
|---|---|---|
| `SITE_ADDRESS` | **sim** | Domínio atendido pelo Caddy (ex.: `loja.seudominio.com.br`); certificado Let's Encrypt automático |
| `PUBLIC_BASE_URL` | **sim** | `https://` + domínio; vira `APP_FRONTEND_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS` e o sitemap do `robots.txt` |
| `IMAGE_REGISTRY` / `IMAGE_TAG` | sim | Ex.: `ghcr.io/diogomes` / `sha-1a2b3c4` (tag imutável do CI; `latest` é recusado pelo `deploy.sh`) |
| `POSTGRES_DB` / `POSTGRES_USER` | não | `gigaverse3d` / `gigaverse3d` |
| `POSTGRES_PASSWORD` | **sim (segredo)** | Senha gerada (`openssl rand -base64 32`); o banco não publica porta |
| `HTTP_BIND` | não | Interface das portas 80/443 (padrão `0.0.0.0`; a simulação usa `127.0.0.1`) |
| `CADDYFILE` | não | `./infra/caddy/Caddyfile` (prod) ou `Caddyfile.local` (simulação, CA interna) |
| `JAVA_TOOL_OPTIONS` | não | Padrão `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` |
| `ENV_FILE`, `STATE_DIR`, `WAIT_TIMEOUT`, `SKIP_PULL`, `SMOKE_CURL_OPTS` | não | Ajustes de `infra/deploy/*.sh` (padrões `.env.prod`, `.deploy`, `240`s) |

Imagem da loja (`web`, nginx): `API_UPSTREAM` (padrão `http://api:8080`) e `PUBLIC_BASE_URL`, lidas na inicialização do container.

## Docker Compose (dev)

| Variável | Padrão |
|---|---|
| `POSTGRES_DB` | `gigaverse3d` |
| `POSTGRES_USER` | `gigaverse3d` |
| `POSTGRES_PASSWORD` | `gigaverse3d_dev` |
| `POSTGRES_PORT` | `5432` (publicada só em 127.0.0.1) |

## Frontend (build-time, públicas)

| Variável | Padrão | Descrição |
|---|---|---|
| `VITE_API_BASE_URL` | vazio (mesma origem) | Base da API. **Nunca** coloque segredos em `VITE_*`: vão para o bundle público |
| `VITE_DEV_API_TARGET` | `http://localhost:8080` | Alvo do proxy `/api` do Vite em desenvolvimento |

## Privacidade e observabilidade (Fase 10)

| Variável / propriedade | Padrão | Descrição |
|---|---|---|
| `APP_PRIVACY_RETENTION_CRON` (`app.privacy.retention-cron`) | `0 30 3 * * *` (Recife) | Quando o job de retenção apaga tokens vencidos e carrinhos expirados |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | `ecs` no perfil `prod` | Formato JSON dos logs; deixe vazio para texto |
| `BACKUP_DIR`, `BACKUP_RETENTION_DAYS`, `BACKUP_AGE_RECIPIENT` | `./backups`, `14`, — | Usadas por `infra/backup/backup.sh` (ver `BACKUP_RESTORE.md`) |
| `AGE_IDENTITY_FILE`, `RESTORE_CONFIRM` | —, — | Usadas por `infra/backup/restore.sh` |
