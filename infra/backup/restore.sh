#!/usr/bin/env bash
# Restaura um backup gerado por backup.sh e confere o resultado.
#
# Uso:
#   infra/backup/restore.sh backups/gigaverse3d-20261031T120000Z.dump [banco_destino]
#
# Por padrão restaura num banco NOVO de verificação ("<banco>_restore_check") no mesmo servidor —
# é o teste periódico de restauração e não toca no banco em uso. Restaurar por cima de um banco
# existente exige RESTORE_CONFIRM=sim (procedimento de incidente: ver docs/BACKUP_RESTORE.md).
#
# Arquivos .age são decifrados com a chave privada em AGE_IDENTITY_FILE.
set -euo pipefail

SRC="${1:?informe o arquivo de backup}"
[[ -f "$SRC" ]] || { echo "ERRO: arquivo não encontrado: $SRC" >&2; exit 1; }
if [[ -f "$SRC.sha256" ]]; then
  sha256sum --check --quiet "$SRC.sha256" || { echo "ERRO: checksum não confere" >&2; exit 1; }
fi

WORK="$SRC"
if [[ "$SRC" == *.age ]]; then
  WORK="$(mktemp)"
  trap 'rm -f "$WORK"' EXIT
  age --decrypt --identity "${AGE_IDENTITY_FILE:?defina AGE_IDENTITY_FILE}" --output "$WORK" "$SRC"
fi

if [[ -n "${PGHOST:-}" ]]; then
  BASE="${PGDATABASE:?defina PGDATABASE}"
  run_psql() { psql -v ON_ERROR_STOP=1 -X -q "$@"; }
  run_restore() { pg_restore --no-owner --no-privileges --exit-on-error "$@" <"$WORK"; }
else
  BASE="${POSTGRES_DB:-gigaverse3d}"
  run_psql() { docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -X -q -U "$POSTGRES_USER" "$@"' psql "$@"; }
  run_restore() { docker compose exec -T postgres sh -c 'pg_restore --no-owner --no-privileges --exit-on-error -U "$POSTGRES_USER" "$@"' pg_restore "$@" <"$WORK"; }
fi

TARGET="${2:-${BASE}_restore_check}"
if [[ "$TARGET" == "$BASE" && "${RESTORE_CONFIRM:-}" != "sim" ]]; then
  echo "ERRO: restaurar sobre o banco em uso ($TARGET) exige RESTORE_CONFIRM=sim" >&2
  exit 1
fi

echo "Restaurando em: $TARGET"
run_psql -d postgres -c "DROP DATABASE IF EXISTS \"$TARGET\" WITH (FORCE);"
run_psql -d postgres -c "CREATE DATABASE \"$TARGET\";"
run_restore -d "$TARGET"

echo "Conferência (migrations aplicadas e contagem de linhas):"
run_psql -d "$TARGET" -A -F ' | ' -c "
  SELECT 'flyway_versao_max', MAX(version)::text FROM flyway_schema_history WHERE success
  UNION ALL SELECT 'users', COUNT(*)::text FROM users
  UNION ALL SELECT 'products', COUNT(*)::text FROM products
  UNION ALL SELECT 'orders', COUNT(*)::text FROM orders
  UNION ALL SELECT 'audit_log', COUNT(*)::text FROM audit_log;"
echo "OK: restauração concluída em $TARGET"
