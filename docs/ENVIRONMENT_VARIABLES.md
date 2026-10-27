# Variáveis de ambiente

Nunca versionar `.env`. Em produção, usar o secret manager da hospedagem (decisão D-P03).

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

### Autenticação e e-mail

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `APP_FRONTEND_BASE_URL` | sim em prod | `http://localhost:5173` | Base dos links enviados por e-mail (ex.: redefinição de senha) |
| `SESSION_TIMEOUT` | não | `2h` | Inatividade até a sessão expirar |
| `APP_MAIL_FROM` | sim em prod | `Gigaverse3D <nao-responda@gigaverse3d.local>` | Remetente |
| `SMTP_HOST` / `SMTP_PORT` | dev: não | `localhost` / `1025` (Mailpit) | SMTP do perfil `dev` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | prod: quando houver provedor (D-P05) | — | SMTP do provedor. **Senha é segredo.** Sem host configurado, os e-mails não são enviados (só registra destinatário mascarado e assunto) |
| `APP_BOOTSTRAP_ADMIN_EMAIL` / `_PASSWORD` / `_NAME` | só na 1ª subida | — | Cria o primeiro ADMIN se não existir nenhum. **Senha é segredo**; remova as variáveis depois |

### Armazenamento de imagens

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `APP_STORAGE_TYPE` | não | `local` | `local` (disco; só dev/test) ou `s3` (produção) |
| `APP_STORAGE_LOCAL_ROOT` | não | `./storage` | Pasta do modo local (ignorada pelo Git) |
| `APP_STORAGE_S3_BUCKET` | se `s3` | — | Bucket |
| `APP_STORAGE_S3_REGION` | se `s3` | — | Região (ex.: `sa-east-1`; R2 usa `auto`) |
| `APP_STORAGE_S3_ENDPOINT` | não | — | Endpoint de provedores S3-compatíveis (R2, B2, MinIO) |
| `APP_STORAGE_S3_PATH_STYLE` | não | `false` | `true` para provedores que exigem path-style |
| `APP_STORAGE_PUBLIC_BASE_URL` | se `s3` | — | URL pública/CDN usada nas respostas da API |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | se `s3` | — | **Segredos.** Lidos pela cadeia padrão do AWS SDK (ou papel da instância); nunca em arquivo versionado |

O backend falha na inicialização com mensagem clara se `APP_STORAGE_TYPE=s3` sem bucket, região ou URL pública.

Valores fixos em `application.yml` (alterar exige deploy): `app.store.currency=BRL`, `app.store.payment-methods=PIX,CARD`.

Variáveis futuras (registradas para não serem esquecidas): credenciais do gateway de pagamento e segredo de webhook (Fase 06, D-P01), provedor de frete (D-P02), e-mail transacional (Fase 05/12, D-P05), storage S3 (Fase 04/12, D-P04).

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
