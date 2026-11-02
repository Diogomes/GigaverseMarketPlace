# Riscos

| # | Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|---|
| R1 | Java 11 é o padrão no PATH; build Spring Boot 3 falharia | Alta | Médio | Maven Wrapper + `JAVA_HOME` para JDK 21 documentado em `LOCAL_SETUP.md`; toolchain/enforcer exigindo Java 21 |
| R2 | Docker Desktop parado → Postgres local e Testcontainers indisponíveis | Média | Médio | Iniciar Docker antes da Fase 02; registrar testes não executados quando indisponível |
| R3 | Gateway e frete indefinidos atrasam checkout real | Alta | Alto | Adapters + modo de pagamento manual; nunca simular cobrança |
| R4 | Logo só existe com fundo preto opaco | Certa | Baixo | Contêiner escuro no tema claro; solicitar versão transparente/SVG (D-P09) |
| R5 | Escopo grande (12 fases) para equipe de uma pessoa | Alta | Alto | Entregas por fase com critérios de aceite; MVP vendável ao fim da Fase 08 com pagamento manual |
| R6 | SEO limitado em SPA | Média | Médio | Metadados por rota, sitemap no backend, avaliar pré-renderização na Fase 07 |
| R7 | Conformidade LGPD e textos legais exigem revisão jurídica | Certa | Alto | Rascunhos identificados; não alegar conformidade (D-P07) |
| R8 | Produtos personalizados com upload de arquivos de cliente ampliam superfície de ataque | Média | Alto | Não aceitar uploads de cliente até D-P10; quando aceitar, storage privado, validação e limites |
| R9 | Duplicidade/divergência entre estoque do `sistema-Gigaverse3d` (insumos) e da loja (produtos) | Média | Médio | Domínios separados (insumo ≠ produto acabado); integração futura explícita |
| R10 | Emissão fiscal (NF-e) não contemplada | Média | Alto | Pendência de lançamento (D-P11) |
| R11 | Vazamento de segredos em commits | Baixa | Alto | `.gitignore` com `.env`, `.env.example` só com valores fictícios; varredura manual limpa em 31/10/2026 (inclusive histórico); varredura automática no CI (Fase 12) |
| R12 | Retenção de dados após anonimização (pedidos com nome/endereço) divergir da orientação jurídica | Média | Médio | Proposta documentada em `PRIVACY_DATA_MAP.md`; ajuste é uma query no `PrivacyService` (D-P07) |
| R13 | Sessões e rate limit em memória com mais de uma instância | Média (ao escalar) | Médio | A pilha de produção roda uma instância; ao escalar, Spring Session JDBC/Redis |
| R14 | Sem teste de intrusão independente | Certa | Alto | Testes automatizados de autorização/cabeçalhos/limites; contratar pentest antes de campanhas de tráfego |
| R15 | E2E roda no banco de desenvolvimento compartilhado, só em Chromium/Edge, sem leitor de tela real | Certa | Baixo | Banco descartável por execução no CI (Fase 12); teste manual com NVDA no `GO_LIVE_CHECKLIST` (ver `TEST_REPORT.md` §5) |
| R16 | Caddy roda como root no container (só com `NET_BIND_SERVICE`) | Baixa | Médio | `cap_drop: ALL`, `no-new-privileges`; avaliar imagem/usuário sem privilégios com `ip_unprivileged_port_start` |
| R17 | Workflows de CI/CD nunca executados no GitHub | Certa | Médio | Validados com actionlint/shellcheck e a simulação local; primeira execução acompanhada antes do lançamento |
| R18 | Imagens em volume local (storage `local`) dependem do backup do servidor | Média | Alto | `backup-media.sh` diário + cópia fora do servidor; migrar para S3 (D-P04) |
