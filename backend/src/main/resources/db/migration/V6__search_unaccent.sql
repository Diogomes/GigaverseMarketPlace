-- Busca insensível a acentos: "dragao" encontra "Dragão", "luminarias" encontra "Luminária".
-- Requer a extensão unaccent (contrib do PostgreSQL; disponível nos serviços gerenciados comuns).
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TEXT SEARCH CONFIGURATION portuguese_unaccent (COPY = portuguese);
ALTER TEXT SEARCH CONFIGURATION portuguese_unaccent
    ALTER MAPPING FOR hword, hword_part, word WITH unaccent, portuguese_stem;

-- Recria a coluna gerada (e o índice GIN) com a nova configuração.
ALTER TABLE products DROP COLUMN search_vector;
ALTER TABLE products ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('portuguese_unaccent'::regconfig, coalesce(name, '')), 'A') ||
    setweight(to_tsvector('portuguese_unaccent'::regconfig, coalesce(description, '')), 'B')
) STORED;
CREATE INDEX products_search_idx ON products USING gin (search_vector);
