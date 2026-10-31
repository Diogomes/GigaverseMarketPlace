-- Fase 09: índices para as métricas do dashboard e o CRM.
-- Vendas contam pela data de pagamento (paid_at); pedidos, pela data de criação (placed_at).
CREATE INDEX orders_paid_at_idx ON orders (paid_at) WHERE paid_at IS NOT NULL;
CREATE INDEX orders_placed_at_idx ON orders (placed_at);
-- Resumo por cliente (pedidos pagos, total gasto, última compra) na lista do CRM.
CREATE INDEX orders_user_paid_idx ON orders (user_id, paid_at) WHERE paid_at IS NOT NULL;
-- Linha do tempo de notas e auditoria por autor.
CREATE INDEX customer_notes_author_idx ON customer_notes (author_user_id);
