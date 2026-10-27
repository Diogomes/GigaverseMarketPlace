-- DADOS DE DEMONSTRAÇÃO — carregados SOMENTE no perfil `dev` (spring.flyway.locations em application-dev.yml).
-- Catálogo fictício para desenvolvimento. Não cria usuários nem senhas.
-- Migration repetível e idempotente: pode rodar novamente sem duplicar registros.

INSERT INTO categories (slug, name, description, sort_order) VALUES
    ('colecionaveis',  'Colecionáveis',  'Estátuas e displays com acabamento detalhado.', 1),
    ('articulados',    'Articulados',    'Peças com juntas móveis impressas em uma só etapa.', 2),
    ('decoracao',      'Decoração',      'Vasos, luminárias e objetos para o ambiente.', 3),
    ('acessorios',     'Acessórios',     'Suportes, organizadores e utilidades para o setup.', 4),
    ('personalizados', 'Personalizados', 'Projetos sob medida a partir da sua ideia.', 5)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO products (slug, sku, name, description, category_id, product_type, status, price, promo_price, production_days, featured)
SELECT p.slug, p.sku, p.name, p.description, c.id, p.product_type, p.status, p.price, p.promo_price, p.production_days, p.featured
FROM (VALUES
    ('dragao-articulado', 'GV-ART-001', 'Dragão Articulado', 'Dragão com mais de 30 articulações impressas no lugar, ideal para decoração e presente.', 'articulados', 'READY', 'ACTIVE', 189.90, NULL::numeric, NULL::integer, true),
    ('astronauta-colecionavel', 'GV-COL-002', 'Astronauta Colecionável', 'Estátua de astronauta em pose de exploração, com base lunar texturizada.', 'colecionaveis', 'READY', 'ACTIVE', 269.90, 249.90, NULL, true),
    ('robo-mecha-display', 'GV-COL-003', 'Robô Mecha Display', 'Display de robô mecha com peças encaixáveis e detalhes em alto relevo.', 'colecionaveis', 'MADE_TO_ORDER', 'ACTIVE', 299.90, NULL, 7, true),
    ('luminaria-lua-3d', 'GV-DEC-004', 'Luminária Lua 3D', 'Luminária com relevo lunar e luz LED de tom quente.', 'decoracao', 'READY', 'ACTIVE', 159.90, NULL, NULL, true),
    ('vaso-geometrico-voronoi', 'GV-DEC-005', 'Vaso Geométrico Voronoi', 'Vaso decorativo com padrão Voronoi vazado (uso com flores secas).', 'decoracao', 'READY', 'ACTIVE', 89.90, NULL, NULL, false),
    ('suporte-para-headset', 'GV-ACE-006', 'Suporte para Headset', 'Suporte de mesa com base pesada e passagem de cabo.', 'acessorios', 'READY', 'ACTIVE', 99.90, NULL, NULL, false),
    ('organizador-hexagonal', 'GV-ACE-007', 'Organizador de Mesa Hexagonal', 'Módulos hexagonais encaixáveis para canetas e pequenos objetos.', 'acessorios', 'READY', 'ACTIVE', 69.90, NULL, NULL, false),
    ('miniatura-personalizada', 'GV-PER-008', 'Miniatura Personalizada', 'Miniatura feita a partir da sua referência. Valor final após orçamento.', 'personalizados', 'CUSTOM', 'ACTIVE', 149.90, NULL, 15, false),
    ('chaveiro-hexagono', 'GV-ACE-009', 'Chaveiro Hexágono', 'Chaveiro leve e resistente.', 'acessorios', 'READY', 'DRAFT', 19.90, NULL, NULL, false)
) AS p (slug, sku, name, description, category_slug, product_type, status, price, promo_price, production_days, featured)
JOIN categories c ON c.slug = p.category_slug
ON CONFLICT (slug) DO NOTHING;

INSERT INTO inventory_items (product_id, quantity_on_hand, low_stock_threshold)
SELECT pr.id, s.qty, s.threshold
FROM (VALUES
    ('dragao-articulado', 20, 5), ('astronauta-colecionavel', 4, 5), ('robo-mecha-display', 0, 3),
    ('luminaria-lua-3d', 12, 4), ('vaso-geometrico-voronoi', 2, 4), ('suporte-para-headset', 25, 5),
    ('organizador-hexagonal', 3, 5), ('miniatura-personalizada', 0, 0), ('chaveiro-hexagono', 0, 10)
) AS s (slug, qty, threshold)
JOIN products pr ON pr.slug = s.slug
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO stock_movements (product_id, movement_type, quantity_delta, quantity_after, reason)
SELECT i.product_id, 'INITIAL', i.quantity_on_hand, i.quantity_on_hand, 'Saldo inicial (demonstração)'
FROM inventory_items i
WHERE i.quantity_on_hand > 0
  AND NOT EXISTS (SELECT 1 FROM stock_movements m WHERE m.product_id = i.product_id AND m.movement_type = 'INITIAL');

-- Opções: Tamanho (dragão, astronauta, miniatura) e Acabamento (astronauta, robô, miniatura).
INSERT INTO product_options (product_id, name, sort_order)
SELECT pr.id, o.name, o.sort_order
FROM (VALUES
    ('dragao-articulado', 'Tamanho', 1), ('astronauta-colecionavel', 'Tamanho', 1), ('astronauta-colecionavel', 'Acabamento', 2),
    ('robo-mecha-display', 'Acabamento', 1), ('miniatura-personalizada', 'Tamanho', 1), ('miniatura-personalizada', 'Acabamento', 2)
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
