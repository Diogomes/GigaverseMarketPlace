#!/usr/bin/env bash
# Backup lógico do PostgreSQL (pg_dump formato custom, compactado) + checksum.
#
# Uso:
#   infra/backup/backup.sh                 # banco do docker compose local (serviço "postgres")
#   PGHOST=... PGUSER=... PGPASSWORD=... PGDATABASE=... infra/backup/backup.sh   # conexão direta (produção)
#
# Variáveis opcionais:
#   BACKUP_DIR            pasta de destino (padrão: ./backups — ignorada pelo git)
#   BACKUP_RETENTION_DAYS apaga backups locais mais antigos (padrão: 14)
#   BACKUP_AGE_RECIPIENT  chave pública "age" (https://age-encryption.org): o arquivo é cifrado e o
#                         .dump em claro é apagado. Use SEMPRE em produção / cópias fora do servidor.
#
# Nunca grava senha em arquivo: a senha vem de PGPASSWORD/secret manager ou do container.
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"
umask 077 # backups contêm dados pessoais: só o dono lê

if [[ -n "${PGHOST:-}" ]]; then
  DB="${PGDATABASE:?defina PGDATABASE}"
  FILE="$BACKUP_DIR/${DB}-${STAMP}.dump"
  pg_dump --format=custom --no-owner --no-privileges --file="$FILE"
else
  DB="${POSTGRES_DB:-gigaverse3d}"
  FILE="$BACKUP_DIR/${DB}-${STAMP}.dump"
  docker compose exec -T postgres sh -c 'pg_dump --format=custom --no-owner --no-privileges -U "$POSTGRES_USER" "$POSTGRES_DB"' >"$FILE"
fi

# Um dump vazio ou truncado não pode passar como backup válido.
if [[ ! -s "$FILE" ]] || [[ "$(head -c 5 "$FILE")" != "PGDMP" ]]; then
  echo "ERRO: backup inválido ($FILE)" >&2
  rm -f "$FILE"
  exit 1
fi

if [[ -n "${BACKUP_AGE_RECIPIENT:-}" ]]; then
  age --recipient "$BACKUP_AGE_RECIPIENT" --output "$FILE.age" "$FILE"
  rm -f "$FILE"
  FILE="$FILE.age"
fi

sha256sum "$FILE" >"$FILE.sha256"
find "$BACKUP_DIR" -maxdepth 1 -type f \( -name '*.dump' -o -name '*.dump.age' -o -name '*.sha256' \) \
  -mtime +"$RETENTION_DAYS" -print -delete | sed 's/^/removido (retenção): /'

echo "OK: $FILE ($(du -h "$FILE" | cut -f1))"
