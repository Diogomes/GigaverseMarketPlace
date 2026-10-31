# Gigaverse3D — Loja virtual + CRM

Plataforma de e-commerce e CRM da Gigaverse3D (impressão 3D, colecionáveis e personalizados).

| Pasta | Conteúdo |
|---|---|
| `frontend/` | React + Vite + TypeScript + Tailwind CSS |
| `backend/` | Java 21 + Spring Boot 4 (Security, Data JPA, Flyway, OpenAPI) |
| `infra/` | Infraestrutura de deploy (a partir da Fase 12) |
| `docs/` | Auditoria, roadmap, decisões, arquitetura, setup e referência visual |
| `docker-compose.yml` | PostgreSQL local de desenvolvimento |

## Início rápido

```bash
docker compose up -d                       # PostgreSQL em 127.0.0.1:5432
cd backend && ./mvnw spring-boot:run       # API em http://localhost:8080 (exige JDK 21)
cd frontend && npm install && npm run dev  # Loja em http://localhost:5173
```

Passo a passo completo (incluindo Windows e `JAVA_HOME`): [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md).

## Verificações

| Onde | Comando |
|---|---|
| backend | `./mvnw verify` (testes unitários + integração com Testcontainers; requer Docker) |
| frontend | `npm run lint` · `npm run typecheck` · `npm test` · `npm run build` · `npm run format:check` |

## Documentação

- [API](docs/API.md) · [Modelo de dados](docs/DATA_MODEL.md) · [Métricas](docs/METRICS.md) · [Imagens](docs/IMAGE_ASSETS.md)
- [Arquitetura](docs/ARCHITECTURE.md) · [Variáveis de ambiente](docs/ENVIRONMENT_VARIABLES.md) · [Setup local](docs/LOCAL_SETUP.md)
- [Roadmap](docs/ROADMAP.md) · [Decisões](docs/DECISIONS.md) · [Riscos](docs/RISKS.md) · [Auditoria](docs/PROJECT_AUDIT.md)
- [Referência visual aprovada](docs/design/REFERENCIA-VISUAL-E-ESPECIFICACAO.md) · [Convenções](CONTRIBUTING.md)

Instagram: <https://www.instagram.com/giga_verse3d/>
