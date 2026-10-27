-- DADOS DE DEMONSTRAÇÃO — carregados SOMENTE no perfil `dev` (spring.flyway.locations em application-dev.yml).
-- Catálogo fictício alinhado ao frontend (frontend/src/data/demo.ts). Não cria usuários nem senhas.
-- Migration repetível e idempotente: reexecuta quando o arquivo muda e converge para o mesmo estado.

-- Produtos de uma versão anterior deste seed (removidos só se não tiverem pedidos).
DELETE FROM products p
WHERE p.slug IN ('dragao-articulado', 'astronauta-colecionavel', 'robo-mecha-display', 'vaso-geometrico-voronoi',
                 'organizador-hexagonal')
  AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.product_id = p.id);

INSERT INTO categories (slug, name, description, sort_order) VALUES
    ('colecionaveis',  'Colecionáveis',  'Estátuas e displays com acabamento detalhado.', 1),
    ('articulados',    'Articulados',    'Peças com juntas móveis impressas em uma só etapa.', 2),
    ('miniaturas',     'Miniaturas',     'Miniaturas em resina prontas para pintar.', 3),
    ('personalizados', 'Personalizados', 'Projetos sob medida e pintura personalizada.', 4),
    ('decoracao',      'Decoração',      'Luminárias e objetos para o ambiente.', 5),
    ('acessorios',     'Acessórios',     'Suportes e utilidades para o setup.', 6)
ON CONFLICT (slug) DO UPDATE
    SET name = EXCLUDED.name, description = EXCLUDED.description, sort_order = EXCLUDED.sort_order;

INSERT INTO products (slug, sku, name, description, category_id, product_type, status, price, promo_price, production_days, featured)
SELECT p.slug, p.sku, p.name, p.description, c.id, p.product_type, p.status, p.price, p.promo_price, p.production_days, p.featured
FROM (VALUES
    ('dragao-mecanico-azul', 'GV-COL-001', 'Dragão Mecânico Azul', 'Escultura de dragão mecânico com placas sobrepostas e detalhes em azul.', 'colecionaveis', 'READY', 'ACTIVE', 349.90, 319.90::numeric, NULL::integer, true),
    ('raptor-articulado', 'GV-ART-002', 'Raptor Articulado', 'Dinossauro com juntas móveis impressas no lugar, em azul e laranja.', 'articulados', 'READY', 'ACTIVE', 129.90, NULL, NULL, true),
    ('cavaleiro-sci-fi', 'GV-COL-003', 'Cavaleiro Sci-Fi', 'Cavaleiro de armadura futurista com espada e escudo luminosos, sobre base de ruínas.', 'colecionaveis', 'MADE_TO_ORDER', 'ACTIVE', 299.90, NULL, 7, true),
    ('pantera-mecanica', 'GV-COL-004', 'Pantera Mecânica', 'Pantera robótica com placas escuras e detalhes iluminados em laranja.', 'colecionaveis', 'READY', 'ACTIVE', 279.90, NULL, NULL, true),
    ('guerreira-de-fantasia', 'GV-COL-005', 'Guerreira de Fantasia', 'Guerreira com escudo e martelo, pintada à mão com acabamento envelhecido.', 'colecionaveis', 'READY', 'ACTIVE', 259.90, NULL, NULL, false),
    ('miniatura-para-pintar', 'GV-MIN-006', 'Miniatura para Pintar', 'Miniatura em resina cinza, sem pintura, pronta para o seu acabamento.', 'miniaturas', 'READY', 'ACTIVE', 89.90, NULL, NULL, false),
    ('luminaria-lua-3d', 'GV-DEC-007', 'Luminária Lua 3D', 'Luminária com relevo lunar e luz LED de tom quente.', 'decoracao', 'READY', 'ACTIVE', 159.90, NULL, NULL, false),
    ('suporte-para-headset', 'GV-ACE-008', 'Suporte para Headset', 'Suporte de mesa com base pesada e passagem de cabo.', 'acessorios', 'READY', 'ACTIVE', 99.90, NULL, NULL, false),
    ('miniatura-personalizada', 'GV-PER-009', 'Miniatura Personalizada', 'Miniatura feita a partir da sua referência. Valor final após orçamento.', 'personalizados', 'CUSTOM', 'ACTIVE', 149.90, NULL, 15, false),
    ('chaveiro-hexagono', 'GV-ACE-010', 'Chaveiro Hexágono', 'Chaveiro leve e resistente.', 'acessorios', 'READY', 'DRAFT', 19.90, NULL, NULL, false)
) AS p (slug, sku, name, description, category_slug, product_type, status, price, promo_price, production_days, featured)
JOIN categories c ON c.slug = p.category_slug
ON CONFLICT (slug) DO UPDATE
    SET sku = EXCLUDED.sku, name = EXCLUDED.name, description = EXCLUDED.description,
        category_id = EXCLUDED.category_id, product_type = EXCLUDED.product_type, status = EXCLUDED.status,
        price = EXCLUDED.price, promo_price = EXCLUDED.promo_price, production_days = EXCLUDED.production_days,
        featured = EXCLUDED.featured, updated_at = now();

-- Estoque só é criado na primeira vez (não sobrescreve movimentações feitas em desenvolvimento).
INSERT INTO inventory_items (product_id, quantity_on_hand, low_stock_threshold)
SELECT pr.id, s.qty, s.threshold
FROM (VALUES
    ('dragao-mecanico-azul', 4, 5), ('raptor-articulado', 20, 5), ('cavaleiro-sci-fi', 0, 3),
    ('pantera-mecanica', 6, 5), ('guerreira-de-fantasia', 2, 4), ('miniatura-para-pintar', 25, 5),
    ('luminaria-lua-3d', 12, 4), ('suporte-para-headset', 3, 5), ('miniatura-personalizada', 0, 0),
    ('chaveiro-hexagono', 0, 10)
) AS s (slug, qty, threshold)
JOIN products pr ON pr.slug = s.slug
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO stock_movements (product_id, movement_type, quantity_delta, quantity_after, reason)
SELECT i.product_id, 'INITIAL', i.quantity_on_hand, i.quantity_on_hand, 'Saldo inicial (demonstração)'
FROM inventory_items i
WHERE i.quantity_on_hand > 0
  AND NOT EXISTS (SELECT 1 FROM stock_movements m WHERE m.product_id = i.product_id AND m.movement_type = 'INITIAL');

INSERT INTO product_options (product_id, name, sort_order)
SELECT pr.id, o.name, o.sort_order
FROM (VALUES
    ('dragao-mecanico-azul', 'Tamanho', 1), ('dragao-mecanico-azul', 'Acabamento', 2),
    ('raptor-articulado', 'Tamanho', 1), ('cavaleiro-sci-fi', 'Acabamento', 1), ('pantera-mecanica', 'Acabamento', 1),
    ('guerreira-de-fantasia', 'Tamanho', 1), ('guerreira-de-fantasia', 'Acabamento', 2),
    ('miniatura-para-pintar', 'Tamanho', 1),
    ('miniatura-personalizada', 'Tamanho', 1), ('miniatura-personalizada', 'Acabamento', 2)
) AS o (slug, name, sort_order)
JOIN products pr ON pr.slug = o.slug
ON CONFLICT (product_id, name) DO NOTHING;

INSERT INTO product_option_values (option_id, label, price_delta, sort_order)
SELECT po.id, v.label, v.delta, v.sort_order
FROM product_options po
JOIN (VALUES
    ('Tamanho', '10 cm', 0.00, 1), ('Tamanho', '15 cm', 30.00, 2), ('Tamanho', '20 cm', 60.00, 3),
    ('Acabamento', 'Padrão', 0.00, 1), ('Acabamento', 'Pintado à mão', 80.00, 2)
) AS v (option_name, label, delta, sort_order) ON v.option_name = po.name
ON CONFLICT (option_id, label) DO NOTHING;
