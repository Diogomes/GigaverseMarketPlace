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
| R11 | Vazamento de segredos em commits | Baixa | Alto | `.gitignore` com `.env`, `.env.example` só com valores fictícios, varredura de segredos no CI (Fase 12) |
