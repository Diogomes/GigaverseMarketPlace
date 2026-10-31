# Mapa de dados pessoais (LGPD)

> **Rascunho técnico para validação jurídica (D-P07).** Bases legais, prazos de retenção e textos ao titular abaixo são *propostas* da engenharia. Não representam parecer jurídico nem garantia de conformidade. Itens marcados **[validar]** precisam de decisão do responsável legal/encarregado (DPO).

## 1. Inventário

| Dado | Onde | Finalidade | Base legal proposta | Quem acessa | Retenção proposta | Exclusão/anonimização |
|---|---|---|---|---|---|---|
| Nome, e-mail | `users` | Conta, login, comunicação sobre pedidos | Execução de contrato (art. 7º, V) | Cliente; equipe (CRM) | Enquanto a conta existir | Anonimização a pedido (automática, na conta) |
| Hash da senha (Argon2id) | `users.password_hash` | Autenticação | Execução de contrato | Ninguém (só verificação) | Enquanto a conta existir | Substituído por valor aleatório na anonimização |
| Telefone (opcional) | `customer_profiles` | Contato sobre pedidos | Execução de contrato | Cliente; equipe | Enquanto a conta existir | Apagado na anonimização |
| Endereços | `addresses` | Entrega | Execução de contrato | Cliente; equipe (via pedido) | Enquanto a conta existir | Apagados na anonimização |
| Snapshot do pedido (nome, e-mail, endereço de entrega, itens, valores, observação) | `orders`, `order_items` | Cumprir o contrato, obrigações fiscais e de consumo, defesa em processos | Obrigação legal / exercício de direitos (art. 7º, II e VI; art. 16, I) | Cliente; equipe | **[validar]** proposta: 5 anos após a entrega (CDC art. 27; obrigações fiscais) | Na anonimização: e-mail substituído e observação apagada; **nome e endereço mantidos** pelo prazo legal **[validar]** |
| Pagamento (método, status, referência do provedor) | `payments` | Conciliação | Obrigação legal / contrato | Equipe (ADMIN) | Mesmo prazo do pedido | Nunca há dados de cartão |
| Consentimentos (termos, privacidade, marketing) com versão e data | `user_consents` | Prova de consentimento | Obrigação legal (art. 8º, §2º) | Equipe | **[validar]** enquanto a conta existir + 5 anos | Mantidos como prova; na anonimização é registrada a revogação de marketing |
| Notas internas e etiquetas | `customer_notes`, `customer_tag_assignments` | Atendimento | Legítimo interesse (art. 7º, IX) **[validar teste de balanceamento]** | Equipe (OPERATOR/ADMIN) | Enquanto a conta existir | Notas apagadas (texto substituído) e etiquetas removidas na anonimização |
| Último login, falhas de login, bloqueio | `users` | Segurança da conta | Legítimo interesse (segurança) | Equipe (ADMIN) | Enquanto a conta existir | Zerados na anonimização |
| Trilha de auditoria (ator, ação, alvo, requestId — sem conteúdo pessoal desnecessário) | `audit_log` | Segurança, prestação de contas | Legítimo interesse / obrigação de segurança (art. 46) | ADMIN | **[validar]** proposta: 5 anos | Mantida; referências ao usuário passam a apontar para a conta anonimizada |
| Tokens de redefinição/convite (só hash) | `password_reset_tokens` | Recuperar acesso | Execução de contrato | Ninguém | 30 min de validade (convite: 48 h) | **Apagados automaticamente** 30 dias após vencer (`DataRetentionJob`) |
| Carrinho de visitante | `carts` (sem dado pessoal) e `localStorage` do navegador | Manter a seleção | Legítimo interesse | — | Até expirar | Carrinhos expirados apagados diariamente |
| Sessão | memória do servidor + cookie | Manter o login | Execução de contrato | — | 2 h de inatividade | Encerrada no logout/troca de senha |
| Logs de aplicação | stdout → agregador (Fase 12) | Operação e segurança | Legítimo interesse | Equipe técnica | **[validar]** proposta: 30 dias | Rotação automática do agregador |
| Backups | armazenamento cifrado | Recuperação de desastre | Legítimo interesse / segurança | Equipe técnica | 14 diários + 8 semanais + 12 mensais | Expiram pela rotação. Uma conta anonimizada continua nos backups antigos até eles expirarem **[validar redação no aviso de privacidade]** |

Dados **não coletados:** CPF, data de nascimento, gênero, dados de cartão, documentos, localização, origem do cadastro. O CRM não infere atributos pessoais e orienta a equipe a não registrar dados sensíveis (art. 11).

**Operadores (terceiros):** hospedagem (D-P03), e-mail transacional (D-P05), storage (D-P04), gateway de pagamento (D-P01). Para cada um: contrato com cláusulas de proteção de dados, local de armazenamento (transferência internacional, art. 33) **[validar]**.

## 2. Direitos do titular (art. 18)

| Direito | Como é atendido hoje |
|---|---|
| Confirmação e acesso | **Automático:** "Minha conta → Meus dados → Baixar meus dados" (JSON com conta, endereços, consentimentos e pedidos; até 5 por hora; auditado). Notas internas e etiquetas não entram na cópia automática; pedido formal ao encarregado avalia a inclusão **[validar]** |
| Correção | Automático para nome, telefone e endereços; e-mail por solicitação ao suporte |
| Eliminação / anonimização | **Automático:** "Excluir minha conta", com senha e confirmação. Recusado com pedidos em andamento. Encerra todas as sessões; o e-mail fica livre para novo cadastro |
| Revogação de consentimento (marketing) | Automático na conta (preferência de e-mails), com histórico |
| Portabilidade | O JSON da cópia é legível por máquina; formato e alcance definitivos **[validar]** |
| Informação sobre compartilhamento | Aviso de privacidade (rascunho, D-P07) |
| Solicitações por outro canal | E-mail do encarregado (a definir) → equipe executa no painel; prazo de resposta proposto: 15 dias **[validar]** |

**Descadastro de marketing:** hoje não existe envio de marketing. Quando houver, cada e-mail deve ter link de descadastro **sem login** (token assinado, uso idempotente) e envio só para quem tem `MARKETING_EMAIL` concedido (exibido no CRM como "Aceita receber").

## 3. Pendências legais e operacionais

1. Nomear o encarregado (DPO) e publicar o canal de contato.
2. Revisão jurídica do aviso de privacidade, termos e política comercial (textos atuais são rascunho, versão `rascunho-2026-10`).
3. Validar prazos de retenção e campos mantidos após a anonimização (pedidos, auditoria, consentimentos, logs, backups).
4. Teste de balanceamento do legítimo interesse para notas internas e auditoria.
5. Contratos com operadores e avaliação de transferência internacional.
6. Procedimento e modelo de resposta para solicitações recebidas fora do sistema.
7. Plano de comunicação de incidentes à ANPD e aos titulares (ver `INCIDENT_RESPONSE.md`).
