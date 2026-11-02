#!/usr/bin/env bash
# Backup das imagens de produtos guardadas no volume "media" (só quando APP_STORAGE_TYPE=local).
# Com S3, use o versionamento/replicação do próprio bucket.
#
# Uso (no servidor, na pasta da pilha):  bash infra/backup/backup-media.sh
# Variáveis: BACKUP_DIR (./backups), MEDIA_VOLUME (gigaverse3d-prod_media), BACKUP_RETENTION_DAYS (14),
#            BACKUP_AGE_RECIPIENT (cifra o arquivo; recomendado para cópias fora do servidor).
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
VOLUME="${MEDIA_VOLUME:-gigaverse3d-prod_media}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"
umask 077
FILE="$BACKUP_DIR/media-${STAMP}.tar.gz"
# falhou no meio? não deixa arquivo parcial que pareça um backup
trap '[[ -f "$FILE.sha256" ]] || rm -f "$FILE"' EXIT

docker volume inspect "$VOLUME" >/dev/null
docker run --rm -v "$VOLUME:/data:ro" alpine:3.22 tar czf - -C /data . >"$FILE"
[[ -s "$FILE" ]] || { echo "ERRO: backup de mídia vazio ($FILE)" >&2; rm -f "$FILE"; exit 1; }
gzip -t "$FILE"

if [[ -n "${BACKUP_AGE_RECIPIENT:-}" ]]; then
  age --recipient "$BACKUP_AGE_RECIPIENT" --output "$FILE.age" "$FILE"
  rm -f "$FILE"
  FILE="$FILE.age"
fi
sha256sum "$FILE" >"$FILE.sha256"
find "$BACKUP_DIR" -maxdepth 1 -type f -name 'media-*' -mtime +"$RETENTION_DAYS" -print -delete | sed 's/^/removido (retenção): /'
echo "OK: $FILE ($(du -h "$FILE" | cut -f1))"
