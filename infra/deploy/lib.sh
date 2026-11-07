# shellcheck shell=bash
# Funções comuns aos scripts de deploy (carregado com "source"). Roda NO SERVIDOR, na pasta da pilha.
# backup.sh/restore.sh usam "docker compose" puro: COMPOSE_FILE/COMPOSE_ENV_FILES apontam a pilha de produção.
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.prod}"
[[ -f "$ENV_FILE" ]] || { echo "ERRO: arquivo de ambiente não encontrado: $ENV_FILE" >&2; exit 1; }
export COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
export COMPOSE_ENV_FILES="$ENV_FILE"
STATE_DIR="${STATE_DIR:-.deploy}"
mkdir -p "$STATE_DIR"

# lê uma variável do arquivo de ambiente sem executá-lo (nunca "source" de arquivo com segredos)
env_value() { grep -E "^$1=" "$ENV_FILE" | tail -1 | cut -d= -f2-; }

dc() { docker compose "$@"; }

log() { printf '%s %s\n' "$(date -u +%H:%M:%SZ)" "$*"; }

# número de migrations aplicadas (0 se o banco ainda não tem o histórico do Flyway)
migration_count() {
  dc exec -T postgres sh -c \
    'psql -X -tA -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) FROM flyway_schema_history WHERE success" 2>/dev/null || echo 0' |
    tr -d '[:space:]'
}

# sobe a pilha numa tag e espera os health checks (api: readiness; web: /healthz)
up_tag() {
  IMAGE_TAG="$1" dc up -d --no-build --wait --wait-timeout "${WAIT_TIMEOUT:-240}" postgres api web caddy
}
