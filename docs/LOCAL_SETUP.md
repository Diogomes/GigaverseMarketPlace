# Execução local do zero

## 1. Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| Git | 2.4x+ | |
| JDK | **21** | Maven Wrapper incluso; não precisa instalar Maven |
| Node.js | 22+ (testado com 24.16) | npm 10+ |
| Docker Desktop | com Compose v2+ | Necessário para o banco e para os testes de integração |

### Windows — JDK 21
Nesta máquina o `java` do PATH é o 11. Antes de usar o backend, aponte o `JAVA_HOME` para o JDK 21:

```powershell
# PowerShell (sessão atual)
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
# Permanente (usuário):
[Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Java\jdk-21.0.11', 'User')
```
```bash
# Git Bash
export JAVA_HOME="/c/Program Files/Java/jdk-21.0.11"
```
O build falha com mensagem clara (maven-enforcer) se a versão for menor que 21.

## 2. Configuração

```bash
cp .env.example .env   # opcional: valores padrão do perfil dev já batem com o docker-compose
```

## 3. Banco de dados

```bash
docker compose up -d
docker compose ps        # postgres deve ficar "healthy"
```

## 4. Backend

```bash
cd backend
./mvnw spring-boot:run           # Windows CMD/PowerShell: mvnw.cmd spring-boot:run
```
- Saúde: <http://localhost:8080/actuator/health> → `{"status":"UP",...}`
- Status público: <http://localhost:8080/api/v1/public/status>
- OpenAPI (só dev): <http://localhost:8080/swagger-ui.html>

## 5. Frontend

```bash
cd frontend
npm install
npm run dev
```
Abra <http://localhost:5173>. O rodapé mostra **"API conectada"** quando o backend responde (indicador só em dev).

## 6. Criar o primeiro administrador (uma única vez)

Não existe usuário nem senha padrão. Você define o e-mail e a senha por variáveis de ambiente **só na primeira subida**; o backend cria o ADMIN se ainda não houver nenhum e nunca grava a senha em log.

```powershell
# PowerShell — na pasta backend, com o JDK 21 configurado
$env:APP_BOOTSTRAP_ADMIN_EMAIL = 'seu-email@dominio.com'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = Read-Host 'Senha do admin (mín. 10 caracteres)' -MaskInput
$env:APP_BOOTSTRAP_ADMIN_NAME = 'Seu Nome'
.\mvnw.cmd spring-boot:run
```

Quando aparecer no log `Administrador inicial criado para …`, pare o backend, **remova as variáveis** e suba de novo:

```powershell
Remove-Item Env:APP_BOOTSTRAP_ADMIN_EMAIL, Env:APP_BOOTSTRAP_ADMIN_PASSWORD, Env:APP_BOOTSTRAP_ADMIN_NAME
.\mvnw.cmd spring-boot:run
```

Depois entre em <http://localhost:5173/entrar> — administradores e operadores vão para `/admin`. Outras pessoas da equipe são convidadas em **/admin/equipe** (o convite chega por e-mail — em dev, no Mailpit — e a pessoa define a própria senha). Se as variáveis ficarem definidas, nada acontece (já existe ADMIN), mas mantenha-as fora do ambiente.

## 7. E-mails em desenvolvimento

`docker compose up -d` também sobe o **Mailpit**: todos os e-mails (ex.: recuperação de senha) aparecem em <http://localhost:8025>. Nada é enviado para a internet.

## 8. Testes e verificações

```bash
cd backend && ./mvnw verify                 # requer Docker (Testcontainers)
cd frontend && npm run lint && npm run typecheck && npm test && npm run build
```

### Testes ponta a ponta (Playwright)

Com Postgres, backend (8080) e frontend (`npm run dev`, 5173) rodando:

```bash
cd frontend && npm run e2e      # usa o Microsoft Edge instalado (channel msedge)
```

Os testes criam contas `e2e-…@exemplo.com` com pedidos **cancelados** no banco de desenvolvimento (o estoque volta). Elas não podem ser apagadas enquanto tiverem pedidos; para zerar tudo use `docker compose down -v` (apaga o banco local). Nunca rode o E2E contra produção.

Se aparecer 429 durante o E2E, o limite de cadastros por IP foi atingido — reinicie o backend (o limitador fica em memória).

## Problemas comuns

| Sintoma | Causa / solução |
|---|---|
| Login responde 429 | Muitas tentativas na janela de 15 min — aguarde |
| Login sempre "E-mail ou senha inválidos" após várias tentativas | Conta bloqueada por 15 min após 5 falhas seguidas |
| Link de redefinição "inválido ou expirado" | Vale 30 min e uma vez; só o link mais recente funciona |
| `Java 21+ é obrigatório` | Ajuste `JAVA_HOME` (seção 1) |
| `Connection refused :5432` | `docker compose up -d` e aguarde `healthy` |
| Testcontainers: `Could not find a valid Docker environment` | Inicie o Docker Desktop |
| Checkout sem opção de entrega | Sem faixas de frete configuradas só há retirada — esperado até D-P02 |
| Rodapé "API indisponível" | Backend não está rodando na porta 8080 |
| Porta 5432 ocupada | Defina `POSTGRES_PORT=5433` no `.env` e `DB_URL` correspondente |

## Parar / limpar

```bash
docker compose down        # mantém dados
docker compose down -v     # APAGA o volume do banco local
```
