#!/usr/bin/env bash
# Deploy de uma versão (tag imutável de imagem) com backup prévio, verificação e rollback automático.
#
# Uso (no servidor, na pasta com docker-compose.prod.yml, infra/ e .env.prod):
#   infra/deploy/deploy.sh sha-1a2b3c4
#
# Passos: baixa as imagens → backup do banco → sobe a nova versão (o Flyway aplica as migrations na subida)
#         → espera os health checks → smoke test público → registra a versão.
# Se falhar depois de subir, volta para a versão anterior. Se a nova versão aplicou migrations, o banco NÃO é
# revertido automaticamente: o script para e mostra o comando de restauração (decisão humana; ver runbook).
source "$(dirname "$0")/lib.sh"

TAG="${1:?uso: deploy.sh <tag-da-imagem>}"
if [[ "$TAG" == "latest" ]]; then
  echo "ERRO: use uma tag imutável (ex.: sha-1a2b3c4), não latest" >&2
  exit 1
fi
PREV="$(cat "$STATE_DIR/current_tag" 2>/dev/null || true)"
PUBLIC_BASE_URL="$(env_value PUBLIC_BASE_URL)"

log "versão atual: ${PREV:-nenhuma} -> nova: $TAG"
if [[ "${SKIP_PULL:-}" != "1" ]]; then
  log "baixando imagens"
  IMAGE_TAG="$TAG" dc pull api web
fi

BACKUP_FILE=""
before=0
if dc ps --status running --services 2>/dev/null | grep -qx postgres; then
  log "backup pré-deploy"
  out="$(BACKUP_DIR="${BACKUP_DIR:-./backups}" bash "$(dirname "$0")/../backup/backup.sh")"
  echo "$out"
  BACKUP_FILE="$(sed -n 's/^OK: \([^ ]*\) .*/\1/p' <<<"$out")"
  before="$(migration_count)"
else
  log "primeira subida: banco ainda não está no ar, sem backup"
fi

rollback() {
  log "FALHA: $1"
  local after
  after="$(migration_count || echo '?')"
  if [[ -z "$PREV" ]]; then
    log "não há versão anterior para voltar. Diagnóstico: docker compose logs --tail=200 api"
    exit 1
  fi
  if [[ "$after" != "$before" ]]; then
    log "ATENÇÃO: $TAG aplicou migrations ($before -> $after); $PREV pode não funcionar com o esquema novo."
    log "Rollback do banco é manual: infra/deploy/rollback.sh $PREV ${BACKUP_FILE:-<arquivo-de-backup>}"
    exit 2
  fi
  log "voltando para $PREV (esquema inalterado)"
  if up_tag "$PREV"; then
    log "rollback concluído: $PREV no ar"
  else
    log "rollback também falhou: siga docs/OPERATIONS_RUNBOOK.md"
  fi
  exit 1
}

log "subindo $TAG"
up_tag "$TAG" || rollback "health checks não ficaram saudáveis em ${WAIT_TIMEOUT:-240}s"

log "smoke test em $PUBLIC_BASE_URL"
bash "$(dirname "$0")/smoke.sh" "$PUBLIC_BASE_URL" || rollback "smoke test falhou"

if [[ -n "$PREV" && "$PREV" != "$TAG" ]]; then echo "$PREV" >"$STATE_DIR/previous_tag"; fi
echo "$TAG" >"$STATE_DIR/current_tag"
printf '%s DEPLOY %s backup=%s\n' "$(date -u +%FT%TZ)" "$TAG" "${BACKUP_FILE:-nenhum}" >>"$STATE_DIR/history.log"
log "OK: $TAG no ar"
