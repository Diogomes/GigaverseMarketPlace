#!/usr/bin/env bash
# Teste de fumaça SOMENTE LEITURA contra a URL pública (não cria contas nem pedidos).
# Uso: infra/deploy/smoke.sh https://loja.exemplo.com.br
#      SMOKE_CURL_OPTS="-k --resolve loja.gigaverse3d.test:443:127.0.0.1" infra/deploy/smoke.sh https://loja.gigaverse3d.test
set -uo pipefail
BASE="${1:?informe a URL pública}"
BASE="${BASE%/}"
HTTP_BASE="http://${BASE#https://}"
read -r -a OPTS <<<"${SMOKE_CURL_OPTS:-}"
fail=0

body() { curl -fsS --max-time 15 "${OPTS[@]}" "$1"; }
headers() { curl -sS -o /dev/null -D - --max-time 15 "${OPTS[@]}" "$1" | tr -d '\r'; }
code() { curl -s -o /dev/null -w '%{http_code}' --max-time 15 "${OPTS[@]}" "$1"; }

result() { # descrição, status (0 = ok)
  if [[ "$2" -eq 0 ]]; then echo "  ok     $1"; else echo "  FALHA  $1"; fail=1; fi
}
expect_body() { body "$2" | grep -qE "$3"; result "$1" $?; }
expect_header() { headers "$2" | grep -qiE "$3"; result "$1" $?; }
expect_code() { [[ "$(code "$2")" == "$3" ]]; result "$1" $?; }

echo "smoke test: $BASE"
expect_body "loja responde com o app" "$BASE/" 'id="root"'
expect_body "rota da SPA (/produtos) devolve o app" "$BASE/produtos" 'id="root"'
expect_body "API pública responde" "$BASE/api/v1/public/status" '"status"'
expect_body "catálogo público responde" "$BASE/api/v1/public/products?size=1" '"content"'
expect_body "token CSRF emitido" "$BASE/api/v1/auth/csrf" 'token'
expect_header "HSTS presente" "$BASE/" '^strict-transport-security:'
expect_header "CSP da loja presente" "$BASE/" '^content-security-policy:.*frame-ancestors'
expect_header "X-Content-Type-Options: nosniff" "$BASE/" '^x-content-type-options: nosniff'
expect_header "cookie CSRF com Secure" "$BASE/api/v1/auth/csrf" '^set-cookie:.*secure'
expect_code "atuador NÃO exposto publicamente" "$BASE/actuator/health" 404
expect_code "API administrativa exige login" "$BASE/api/v1/admin/products" 401
expect_body "robots.txt aponta sitemap absoluto" "$BASE/robots.txt" '^Sitemap: https://'
expect_body "sitemap.xml responde" "$BASE/sitemap.xml" '<urlset'
expect_header "HTTP redireciona para HTTPS" "$HTTP_BASE/" '^location: https://'

if [[ $fail -ne 0 ]]; then
  echo "smoke test: FALHOU"
  exit 1
fi
echo "smoke test: OK"
