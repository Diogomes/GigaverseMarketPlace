# Métricas do dashboard e do CRM

Fonte única de verdade: `backend/…/admin/DashboardService.java` e `crm/CustomerCrmService.java` (SQL no banco). O frontend só exibe. Testes com valores conhecidos: `DashboardMetricsTest` e `CustomerCrmApiTest`.

> **Status:** proposta técnica para a decisão **D-P12** (como contar vendas). Até o proprietário confirmar, valem as regras abaixo. Mudar uma regra = mudar o SQL, este documento e os testes juntos.

## Convenções

- **Fuso:** `America/Recife` (UTC−3, sem horário de verão). Um "dia" vai de 00:00 a 23:59:59 nesse horário. Um pagamento às 23:30 de 31/03 em Recife (02:30 UTC de 01/04) conta em março.
- **Período:** `from` e `to` inclusivos, máximo 366 dias. Padrão: últimos 30 dias até hoje.
- **Período anterior:** mesmo número de dias, imediatamente antes de `from` (ex.: 01–31/03 compara com 30/01–29/02).
- **Venda = pagamento confirmado** (`orders.paid_at`): por webhook verificado do gateway ou baixa manual de um ADMIN. Pedido aguardando pagamento **não** é venda.
- **Pedido pago válido:** `paid_at` preenchido e status diferente de `CANCELLED`.
- **Dinheiro:** `NUMERIC(12,2)`/`BigDecimal`, arredondamento bancário (HALF_EVEN) só no ticket médio.

## Dashboard

| Métrica | Definição | Data usada |
|---|---|---|
| **Vendas brutas** | Σ `total` (produtos + frete − desconto) de todos os pedidos com `paid_at` no período, **inclusive** os cancelados depois de pagos | `paid_at` |
| **Cancelados após pagamento** | Σ `total` dos pedidos pagos no período que hoje estão `CANCELLED` (estorno manual ao cliente) | `paid_at` |
| **Frete** | Σ `shipping_total` dos pedidos pagos válidos do período | `paid_at` |
| **Vendas líquidas** | Vendas brutas − cancelados após pagamento − frete = Σ (`total` − `shipping_total`) dos pedidos pagos válidos | `paid_at` |
| **Pedidos pagos** | Quantidade de pedidos pagos válidos | `paid_at` |
| **Ticket médio** | Σ `total` dos pedidos pagos válidos ÷ pedidos pagos (0 se não houver) | `paid_at` |
| **Pedidos criados** | Quantidade de pedidos criados, em qualquer status | `placed_at` |
| **Pedidos por etapa** | Pedidos criados no período agrupados pelo status **atual**. O gráfico junta os 7 status em 5 etapas (Aguardando pagamento · Pago/em produção · Pronto ou enviado · Entregue · Cancelado) | `placed_at` |
| **Vendas por dia** | Vendas brutas (e líquidas, na API) por dia; dias sem venda aparecem com zero | `paid_at` |
| **Mais vendidos** | Top 5 por unidades em pedidos pagos válidos; receita = Σ `line_total` (sem frete). Nome atual do produto, ou o do pedido se o produto foi excluído | `paid_at` |
| **Compradores** | Clientes distintos com pedido pago válido no período | `paid_at` |
| **1ª compra** | Compradores cujo primeiro pedido pago válido (de todos os tempos) caiu no período | `paid_at` |
| **Recorrentes** | Compradores − 1ª compra (já tinham compra paga antes do período) | `paid_at` |
| **Novos cadastros** | Contas de cliente criadas no período | `users.created_at` |
| **Estoque baixo** | Produtos `ACTIVE` de pronta entrega com disponível (`quantity_on_hand − quantity_reserved`) ≤ mínimo. Situação **atual**, não depende do período. Lista os 10 mais críticos + total | — |
| **Aguardando ação** | Contagem atual de pedidos em `PENDING_PAYMENT`, `PAID`, `IN_PRODUCTION`, `READY_FOR_PICKUP` + os 5 mais antigos. Não depende do período | — |

Variação "vs. período anterior": `(atual − anterior) ÷ anterior`; sem base (anterior = 0) a tela mostra "Sem base de comparação".

### Limitações conhecidas

- Estorno é manual (sem gateway): "cancelados após pagamento" assume que todo pedido pago e cancelado foi reembolsado integralmente. Reembolso parcial não é modelado.
- Pedidos por etapa refletem o status de hoje, não o status no fim do período.
- Ainda não há metas, custo de produção nem margem (possível integração futura com o `sistema-Gigaverse3d`, D-T07).

## CRM

| Campo | Definição |
|---|---|
| **Compras** | Pedidos pagos válidos do cliente |
| **Total gasto** | Σ `total` dos pedidos pagos válidos (inclui frete) |
| **Ticket médio** | Total gasto ÷ compras |
| **Primeira / última compra** | Menor / maior `paid_at` entre os pedidos pagos válidos |
| **Pedidos (todos)** | Todos os pedidos, em qualquer status |

Segmentos (filtro da lista), calculados dos pedidos — sem perfilamento nem atributos inferidos:

| Segmento | Regra |
|---|---|
| Sem compra | 0 pedidos pagos válidos |
| Recorrentes | 2 ou mais pedidos pagos válidos |
| Sem comprar há 90+ dias | Última compra há mais de 90 dias |

Origem do cadastro não é exibida porque não é coletada.
