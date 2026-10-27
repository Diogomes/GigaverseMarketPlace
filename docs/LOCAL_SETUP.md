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

## 6. Testes e verificações

```bash
cd backend && ./mvnw verify                 # requer Docker (Testcontainers)
cd frontend && npm run lint && npm run typecheck && npm test && npm run build
```

## Problemas comuns

| Sintoma | Causa / solução |
|---|---|
| `Java 21+ é obrigatório` | Ajuste `JAVA_HOME` (seção 1) |
| `Connection refused :5432` | `docker compose up -d` e aguarde `healthy` |
| Testcontainers: `Could not find a valid Docker environment` | Inicie o Docker Desktop |
| Rodapé "API indisponível" | Backend não está rodando na porta 8080 |
| Porta 5432 ocupada | Defina `POSTGRES_PORT=5433` no `.env` e `DB_URL` correspondente |

## Parar / limpar

```bash
docker compose down        # mantém dados
docker compose down -v     # APAGA o volume do banco local
```
