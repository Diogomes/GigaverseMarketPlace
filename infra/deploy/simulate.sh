#!/usr/bin/env bash
# Simulação LOCAL do deploy de produção: imagens construídas aqui, domínio fictício .test, TLS da CA interna
# do Caddy e e-mails no Mailpit. Não publica nada (portas só em 127.0.0.1).
# Uso: infra/deploy/simulate.sh [up|down|destroy]
set -euo pipefail
cd "$(dirname "$0")/../.."
export ENV_FILE=infra/deploy/simulate.env STATE_DIR=.deploy-sim SKIP_PULL=1 BACKUP_DIR=./backups/sim
export SMOKE_CURL_OPTS="-k --resolve loja.gigaverse3d.test:443:127.0.0.1 --resolve loja.gigaverse3d.test:80:127.0.0.1"
compose() { docker compose -f docker-compose.prod.yml --env-file "$ENV_FILE" "$@"; }

case "${1:-up}" in
  up)
    compose build
    compose up -d mailpit
    bash infra/deploy/deploy.sh local
    ;;
  down) compose down ;;
  destroy)
    compose down -v
    rm -rf .deploy-sim
    ;;
  *)
    echo "uso: simulate.sh [up|down|destroy]" >&2
    exit 1
    ;;
esac
