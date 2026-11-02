# Checklist de lançamento

Marque cada item **com evidência** (data, quem, link/print). Itens ⛔ bloqueiam o lançamento. Situação em **2026-11-02**: nada publicado; o sistema está pronto para deploy, mas as decisões de negócio abaixo estão abertas.

Legenda: ✅ pronto no sistema · 🟡 depende de decisão/ação do proprietário · ⬜ fazer no lançamento

## 1. Decisões que precisam existir antes

| # | Item | Situação |
|---|---|---|
| ⛔ | Hospedagem escolhida e contratada (D-P03) | 🟡 opções e requisitos em `DEPLOYMENT.md` §8 |
| ⛔ | Domínio registrado (D-P08) | 🟡 |
| ⛔ | Provedor de e-mail transacional com domínio verificado — SPF, DKIM, DMARC (D-P05) | 🟡 |
| ⛔ | Forma de cobrança: manual (PIX conferido pelo admin) **ou** gateway testado em sandbox (D-P01) | 🟡 manual pronto ✅; gateway pendente |
| ⛔ | Frete: faixas de CEP e prazos reais configurados, ou só retirada (D-P02) | 🟡 sistema pronto ✅; valores reais pendentes |
| ⛔ | Política de troca, devolução e cancelamento revisada (D-P06) | 🟡 texto em rascunho |
| ⛔ | Termos e política de privacidade revisados por jurídico; encarregado (DPO) definido (D-P07) | 🟡 rascunhos; itens em `PRIVACY_DATA_MAP.md` §3 |
| ⛔ | Emissão fiscal (NF-e / MEI / CNPJ) resolvida com o contador (D-P11) | 🟡 fora do sistema |
| | Armazenamento de imagens: volume local com backup **ou** bucket S3 (D-P04) | 🟡 ambos suportados ✅ |
| | Logo em fundo transparente / SVG (D-P09) | 🟡 |
| | Métricas do dashboard confirmadas (D-P12) | 🟡 proposta em `METRICS.md` |

## 2. Infraestrutura

| # | Item | Situação |
|---|---|---|
| ⛔ | Servidor preparado: SSH só por chave, firewall 22/80/443, atualizações automáticas (`OPERATIONS_RUNBOOK.md` §1) | ⬜ |
| ⛔ | DNS apontando; **HTTPS válido**; HTTP redireciona para HTTPS; HSTS presente | ✅ pronto (Caddy) · ⬜ verificar no domínio real |
| ⛔ | `.env.prod` preenchido no servidor (`chmod 600`), sem valores de exemplo; nenhum segredo no Git | ⬜ — a API recusa subir com configuração insegura ✅ |
| ⛔ | Ambiente `production` no GitHub com **revisor obrigatório**; segredos de deploy configurados | ⬜ (`DEPLOYMENT.md` §4) |
| ⛔ | CI verde no commit a publicar (backend, frontend, E2E, segurança) | ⬜ workflows prontos ✅, **nunca executados no GitHub** |
| | Imagens publicadas com tag `sha-…` (não `latest`), SBOM e *provenance* | ✅ `release.yml` |

## 3. Dados, backup e recuperação

| # | Item | Situação |
|---|---|---|
| ⛔ | Backup diário do banco **cifrado** e cópia **fora do servidor** | ✅ scripts · ⬜ agendar (cron) e configurar destino |
| ⛔ | Backup das imagens (se storage local) | ✅ `backup-media.sh` · ⬜ agendar |
| ⛔ | **Restauração testada** no ambiente real, com registro | ✅ testada na simulação · ⬜ repetir em produção |
| ⛔ | Chave privada do age guardada fora do servidor (2 locais) | ⬜ |
| | Rollback ensaiado (`rollback.sh`) | ✅ simulação: rollback automático em ~25 s |
| | Banco de produção **sem** dados de demonstração (seed só em `dev`) | ✅ |

## 4. Contas e acesso

| # | Item | Situação |
|---|---|---|
| ⛔ | Primeiro ADMIN criado pelo bootstrap e variáveis `APP_BOOTSTRAP_ADMIN_*` **removidas** | ⬜ (`OPERATIONS_RUNBOOK.md` §2) |
| ⛔ | Senha do admin longa e única, no gerenciador de senhas | ⬜ |
| | Equipe convidada por /admin/equipe com o menor papel necessário (OPERATOR × ADMIN) | ⬜ |

## 5. Funcionalidade (no domínio real, antes de divulgar)

| # | Item | Situação |
|---|---|---|
| ⛔ | `bash infra/deploy/smoke.sh https://<domínio>` → 14/14 | ⬜ |
| ⛔ | Cadastro de cliente real (e-mail de verdade) e login/logout | ⬜ |
| ⛔ | "Esqueci minha senha": e-mail chega (caixa de entrada, não spam) e o link abre o domínio certo | ⬜ |
| ⛔ | Catálogo real cadastrado: produtos, preços, imagens, estoque, categorias | ⬜ |
| ⛔ | **Compra de ponta a ponta**: carrinho → checkout → pedido → confirmação de pagamento pelo admin → status até a entrega → cliente acompanha | ⬜ (coberto pelo E2E em dev ✅) |
| ⛔ | Pagamento: valor e chave PIX / instruções conferidos (manual) **ou** pagamento real de baixo valor no gateway, com estorno | ⬜ |
| | Frete: cotação de 3 CEPs (local, região, distante) confere com a tabela | ⬜ |
| | Cancelamento pelo cliente e pelo admin devolvem o estoque | ✅ testado |
| | Páginas institucionais revisadas: sobre, contato, FAQ, política comercial, privacidade, termos | 🟡 rascunhos |
| | Sitemap e robots com o domínio final; Google Search Console | ✅ gerados · ⬜ enviar |
| | Teste rápido em celular real (Android e iPhone) | ⬜ |
| | Teste com leitor de tela (NVDA ou VoiceOver) no fluxo de compra | ⬜ (`TEST_REPORT.md` §5) |

## 6. Operação

| # | Item | Situação |
|---|---|---|
| ⛔ | Monitor de disponibilidade externo com alerta no celular | ⬜ (`OPERATIONS_RUNBOOK.md` §5) |
| ⛔ | Plano de incidentes conhecido por quem opera; contatos preenchidos | 🟡 `INCIDENT_RESPONSE.md` |
| | Canal de suporte ao cliente divulgado (e-mail/WhatsApp) e prazo de resposta | 🟡 |
| | Logs acessíveis e alerta para erros repetidos | ⬜ |
| | Teste de carga simples (k6) num ambiente de homologação | ⬜ opcional antes de campanhas |
| | Pentest independente antes de campanhas de tráfego pago | ⬜ (R14) |

## 7. Depois do lançamento (primeira semana)

- Acompanhar logs e alertas diariamente; conferir que o backup das 03:15 rodou.
- Conferir os primeiros pedidos manualmente (valores, frete, e-mails).
- Reduzir o TTL do DNS de volta ao normal; avaliar `preload` do HSTS só depois.
