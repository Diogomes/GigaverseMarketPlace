# Runbook de operação

Procedimentos do dia a dia em produção (pilha `docker-compose.prod.yml` num servidor). Todos os comandos rodam **no servidor**, na pasta da pilha (`/opt/gigaverse3d` por padrão), salvo indicação. Visão geral e decisões: `DEPLOYMENT.md`. Incidentes: `INCIDENT_RESPONSE.md`.

Atalho usado abaixo:
```bash
export COMPOSE_FILE=docker-compose.prod.yml COMPOSE_ENV_FILES=.env.prod
```

## 1. Preparar o servidor (uma vez)

1. SO atualizado com atualizações de segurança automáticas (ex.: `unattended-upgrades`), fuso UTC.
2. Usuário de deploy sem senha, só com chave SSH; login de root e senha por SSH **desativados**.
3. Firewall: entrada só **22** (restrita ao seu IP, se possível), **80** e **443**.
4. Docker Engine + Compose v2; usuário de deploy no grupo `docker`.
5. `mkdir -p /opt/gigaverse3d && cd /opt/gigaverse3d`; criar `.env.prod` a partir de `.env.prod.example` com `chmod 600`. Gerar senhas com `openssl rand -base64 32`.
6. `docker login ghcr.io` com token **somente leitura** de pacotes.
7. DNS do domínio apontando para o servidor **antes** da primeira subida (o Caddy emite o certificado na hora).

## 2. Primeira subida e primeiro administrador

1. Rodar o *Release* com "deploy" marcado (ou, manualmente: copiar `docker-compose.prod.yml` + `infra/` e rodar `bash infra/deploy/deploy.sh sha-<commit>`).
2. Criar o ADMIN — **nunca há usuário/senha padrão**:
   ```bash
   # adicione temporariamente ao .env.prod:
   APP_BOOTSTRAP_ADMIN_EMAIL=seu-email@dominio.com.br
   APP_BOOTSTRAP_ADMIN_PASSWORD=<senha longa gerada, guardada no gerenciador de senhas>
   APP_BOOTSTRAP_ADMIN_NAME=Seu Nome
   docker compose up -d api && docker compose logs -f api   # aguarde "Administrador inicial criado para …"
   ```
3. **Remova as três linhas** do `.env.prod` e rode `docker compose up -d api` de novo (a API avisa no log enquanto a senha estiver definida).
4. Entre em `https://<domínio>/entrar`, confira o painel e convide a equipe por **/admin/equipe**.

## 3. Deploy e rollback

| Situação | Comando |
|---|---|
| Publicar versão | GitHub → Actions → **Release** → *Run workflow* (marcar "deploy") → aprovar no ambiente `production` |
| Manual, no servidor | `bash infra/deploy/deploy.sh sha-1a2b3c4` |
| Voltar à versão anterior (mesmo esquema) | `bash infra/deploy/rollback.sh` |
| Voltar a uma tag específica | `bash infra/deploy/rollback.sh sha-9f8e7d6` |
| Voltar **e** restaurar o banco | `bash infra/deploy/rollback.sh sha-9f8e7d6 backups/gigaverse3d-<data>.dump` (perde dados gravados depois do backup — decidir com o responsável) |
| Ver versão no ar e histórico | `cat .deploy/current_tag .deploy/history.log` |

Regras: deploy fora do horário de pico; nunca `latest`; uma migration destrutiva por vez e só depois de a versão que deixou de usá-la estar estável (expandir → contrair, `DEPLOYMENT.md` §6).

## 4. Backups

| O quê | Como | Frequência proposta |
|---|---|---|
| Banco | `bash infra/backup/backup.sh` (pg_dump custom + sha256; cifrado com `BACKUP_AGE_RECIPIENT`) | diário + automático antes de todo deploy |
| Imagens dos produtos (storage `local`) | `bash infra/backup/backup-media.sh` | diário |
| Cópia fora do servidor | enviar `backups/*.age` + `.sha256` para outro provedor/conta (ex.: bucket com versionamento e *object lock*) | diário |
| Teste de restauração | `bash infra/backup/restore.sh backups/<último>.dump` (restaura em `<banco>_restore_check`, não toca no banco em uso) | **mensal**, registrar resultado |

Exemplo de `crontab -e` do usuário de deploy:
```cron
COMPOSE_FILE=docker-compose.prod.yml
COMPOSE_ENV_FILES=.env.prod
BACKUP_AGE_RECIPIENT=age1...chave-publica...
15 3 * * * cd /opt/gigaverse3d && bash infra/backup/backup.sh >> backups/backup.log 2>&1
30 3 * * * cd /opt/gigaverse3d && bash infra/backup/backup-media.sh >> backups/backup.log 2>&1
```
A chave **privada** do age fica fora do servidor (gerenciador de senhas + cópia offline). Sem ela os backups cifrados são irrecuperáveis. Política de retenção e restauração em incidente: `BACKUP_RESTORE.md`.

## 5. Monitoramento e alertas

| Sinal | Como obter | Alerta |
|---|---|---|
| Disponibilidade | monitor externo (ex.: UptimeRobot, Better Stack, Healthchecks.io) em `https://<domínio>/api/v1/public/status` e na home, a cada 1–5 min | 2 falhas seguidas → e-mail/celular |
| Saúde dos containers | `docker compose ps` (coluna STATUS `healthy`); `restart: unless-stopped` reinicia falhas | container `unhealthy` ou reiniciando em loop |
| Erros da aplicação | logs JSON (ECS) com `requestId`: `docker compose logs --since 1h api \| grep '"level":"ERROR"'` | qualquer ERROR repetido; picos de 5xx |
| Tentativas de login / abuso | logs da API (429, bloqueio de conta) e auditoria em **/admin/auditoria** | pico incomum |
| Certificado | o Caddy renova sozinho; monitor externo com checagem de validade TLS | < 14 dias para expirar |
| Disco | `df -h`, `docker system df` | > 80 % |
| Backup | `tail backups/backup.log`; idade do último arquivo | sem backup novo em 26 h |
| E-mails | logs `E-mail NÃO enviado` / exceções de SMTP; painel do provedor (bounces) | falhas de envio |

Logs ficam no driver `json-file` com rotação (5 × 20 MB por container). Para retenção maior ou busca, encaminhar a um agregador (ex.: Grafana Loki, Better Stack, Axiom); os logs já são JSON e **não contêm senhas, tokens nem corpo de e-mail**.

## 6. Rotação de segredos

Rotacione imediatamente em caso de suspeita de vazamento (`INCIDENT_RESPONSE.md`) e, de rotina, a cada 6–12 meses.

| Segredo | Procedimento |
|---|---|
| Senha do banco | `docker compose exec postgres psql -U gigaverse3d -c "ALTER USER gigaverse3d PASSWORD '<nova>'"` → atualizar `POSTGRES_PASSWORD` no `.env.prod` → `docker compose up -d api`. (A variável do container do Postgres só vale na criação do volume; a troca real é o `ALTER USER`.) |
| SMTP | gerar nova credencial no provedor → `SMTP_PASSWORD` → `docker compose up -d api` → testar "esqueci a senha" → revogar a antiga |
| Chaves S3 | criar nova chave com permissão só no bucket → `AWS_*` → `up -d api` → enviar uma imagem de teste → revogar a antiga |
| Chave SSH de deploy | nova chave → `authorized_keys` do servidor → segredo `DEPLOY_SSH_KEY` no GitHub → testar → remover a antiga |
| Token do GHCR no servidor | novo token só leitura → `docker login ghcr.io` → revogar o antigo |
| Contas administrativas | trocar senha em **Meus dados**; desativar quem saiu da equipe em **/admin/equipe** (encerra as sessões) |
| Todas as sessões de clientes | reiniciar a API (`docker compose restart api`) — sessões ficam em memória e todos precisam entrar de novo |
| Chave age dos backups | nova chave; backups antigos continuam legíveis só com a chave antiga — guarde-a até a retenção vencer |

## 7. Manutenção de rotina

- **Semanal:** conferir alertas, `docker compose ps`, último backup, espaço em disco.
- **Mensal:** teste de restauração; atualizar o SO; rodar o CI (o Trivy acusa CVEs novas) e publicar uma versão rebuild se houver correções nas imagens base; `docker image prune` das tags antigas (manter as 3 últimas).
- **Trimestral:** revisar contas da equipe e permissões; revisar textos legais e políticas (D-P06/D-P07); revisar este runbook.

## 8. Diagnóstico rápido

| Sintoma | Verificar |
|---|---|
| Site fora do ar | `docker compose ps`; `docker compose logs --tail=100 caddy web api`; DNS e firewall |
| 502 na loja | API não saudável: `docker compose logs --tail=200 api` (banco? configuração? — a API recusa subir e diz qual variável) |
| API não sobe após deploy | mensagem "Configuração de produção inválida" lista as variáveis; ou falha de migration (Flyway) → `rollback.sh` + restauração se necessário |
| Certificado inválido | `docker compose logs caddy` (DNS errado, porta 80 bloqueada, limite do Let's Encrypt) |
| E-mails não chegam | logs da API; credencial SMTP; SPF/DKIM do domínio; spam |
| Lentidão | `docker stats`; consultas lentas no Postgres (`pg_stat_activity`); disco cheio |
