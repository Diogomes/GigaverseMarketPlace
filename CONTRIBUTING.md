# Convenções

## Commits
[Conventional Commits](https://www.conventionalcommits.org/pt-br/), em português:
`feat(catalog): adiciona busca por slug`, `fix(auth): ...`, `docs: ...`, `test: ...`, `chore: ...`.
Escopos sugeridos: `frontend`, `backend`, `infra`, `docs`, ou o módulo de domínio (`catalog`, `order`, `crm`...).

Um commit por fase aprovada no mínimo; nunca commitar `.env` nem credenciais.

## Código
- **Backend:** pacotes por domínio (`br.com.gigaverse3d.<modulo>`), DTOs em toda API, `BigDecimal` para dinheiro, migrations Flyway versionadas (`V<n>__descricao.sql`) — nunca editar uma migration já aplicada.
- **Frontend:** TypeScript estrito, componentes reutilizáveis, cores apenas via tokens (`bg-surface`, `text-fg`, `bg-primary`…; ver `src/styles/index.css`), nada de cores soltas por página.
- Formatação: `.editorconfig` + Prettier no frontend (`npm run format`). Lint: oxlint (`npm run lint`).
- Toda tela deve funcionar em tema claro e escuro, no teclado e no celular.
