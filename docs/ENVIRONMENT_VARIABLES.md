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
