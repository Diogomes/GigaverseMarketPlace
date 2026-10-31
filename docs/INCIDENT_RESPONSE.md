# Resposta a incidentes

Plano curto para uma operação pequena. Ajustar nomes e contatos quando a equipe e os fornecedores estiverem definidos. Prazos legais de comunicação devem ser confirmados com o jurídico (Resolução CD/ANPD nº 15/2024: comunicação à ANPD e aos titulares em até 3 dias úteis do conhecimento, quando houver risco ou dano relevante) **[validar]**.

## Papéis

| Papel | Responsável | Contato |
|---|---|---|
| Coordenação do incidente | Proprietário da Gigaverse3D | (preencher) |
| Técnico (aplicação/infra) | Responsável técnico | (preencher) |
| Encarregado de dados (DPO) | (a nomear — D-P07) | (preencher) |
| Jurídico | (a definir) | (preencher) |
| Fornecedores | Hospedagem, gateway, e-mail, storage | (preencher: canais de suporte e de segurança) |

## Severidade

| Nível | Exemplos | Ação inicial |
|---|---|---|
| **Crítico** | Vazamento de dados pessoais; acesso indevido a conta ADMIN; pedidos marcados como pagos sem pagamento; banco corrompido | Conter em minutos; acionar todos os papéis |
| **Alto** | Loja fora do ar; checkout falhando; webhook de pagamento parado; suspeita de credencial exposta | Conter em até 1 h |
| **Médio** | Lentidão; falha de e-mail transacional; job de expiração parado | Mesmo dia |
| **Baixo** | Erro visual; vulnerabilidade de baixo risco em dependência | Planejar correção |

## Passo a passo

1. **Detectar e registrar** — abrir um registro (data/hora, quem detectou, sintomas, `X-Request-Id` dos erros, prints). Não apagar logs.
2. **Conter** (conforme o caso):
   - Credencial exposta → rotacionar já (ver `SECURITY.md` §4) e revogar a antiga.
   - Conta da equipe comprometida → desativar em `/admin/equipe` (encerra as sessões na hora); se for a única ADMIN, rotacionar a senha pelo banco e reiniciar.
   - Sessões suspeitas em massa → reiniciar a aplicação (sessões ficam em memória: todos saem).
   - Ataque em andamento → bloquear IP/faixa no proxy/WAF; em último caso, colocar a loja em manutenção.
   - Pagamento suspeito → suspender a confirmação manual e conferir no painel do gateway antes de produzir/enviar.
3. **Investigar** — `audit_log` (`/admin/auditoria`, filtros por ação, data e ator), logs da aplicação pelo `requestId`, logs do proxy e do provedor. Definir: o quê, quando, quais dados, quantos titulares.
4. **Erradicar e recuperar** — corrigir a causa (deploy com teste de regressão), restaurar dados se preciso (`BACKUP_RESTORE.md`), validar integridade (contagens, pedidos, pagamentos).
5. **Comunicar** — se houver dados pessoais envolvidos: avaliar risco com o encarregado/jurídico; comunicar ANPD e titulares no prazo legal, com natureza dos dados, titulares afetados, medidas tomadas e riscos. Clientes afetados por falha de pedido/pagamento: contato direto.
6. **Aprender** — em até 5 dias úteis: relato sem culpados (linha do tempo, causa raiz, o que funcionou, ações com dono e prazo). Atualizar este plano, testes e alertas.

## Contatos úteis
- ANPD — comunicação de incidente de segurança: formulário no site da ANPD.
- Gateway de pagamento (D-P01), hospedagem (D-P03), e-mail (D-P05), storage (D-P04): canais de segurança a preencher.
