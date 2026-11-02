#!/usr/bin/env bash
# Volta para uma versão anterior. Opcionalmente restaura o banco a partir de um backup — isso PERDE o que foi
# gravado depois dele (pedidos, cadastros). Use só se a versão nova corrompeu dados ou migrou o esquema de
# forma incompatível com a anterior.
#
# Uso:
#   infra/deploy/rollback.sh                       # volta para .deploy/previous_tag, sem tocar no banco
#   infra/deploy/rollback.sh sha-9f8e7d6           # volta para essa tag
#   infra/deploy/rollback.sh sha-9f8e7d6 backups/gigaverse3d-20261102T120000Z.dump   # e restaura o banco
source "$(dirname "$0")/lib.sh"

TAG="${1:-$(cat "$STATE_DIR/previous_tag" 2>/dev/null || true)}"
[[ -n "$TAG" ]] || { echo "ERRO: informe a tag (não existe $STATE_DIR/previous_tag)" >&2; exit 1; }
DUMP="${2:-}"

if [[ -n "$DUMP" ]]; then
  log "parando loja e API para restaurar o banco a partir de $DUMP"
  dc stop web api
  log "backup de segurança do estado atual antes de sobrescrever"
  BACKUP_DIR="${BACKUP_DIR:-./backups}" bash "$(dirname "$0")/../backup/backup.sh"
  DB="$(env_value POSTGRES_DB || true)"
  RESTORE_CONFIRM=sim bash "$(dirname "$0")/../backup/restore.sh" "$DUMP" "${DB:-gigaverse3d}"
fi

log "subindo $TAG"
up_tag "$TAG"
bash "$(dirname "$0")/smoke.sh" "$(env_value PUBLIC_BASE_URL)"

cur="$(cat "$STATE_DIR/current_tag" 2>/dev/null || true)"
if [[ -n "$cur" && "$cur" != "$TAG" ]]; then echo "$cur" >"$STATE_DIR/previous_tag"; fi
echo "$TAG" >"$STATE_DIR/current_tag"
printf '%s ROLLBACK %s restore=%s\n' "$(date -u +%FT%TZ)" "$TAG" "${DUMP:-nao}" >>"$STATE_DIR/history.log"
log "OK: rollback para $TAG concluído"
