# Backup e restauração

## Política proposta

| Item | Proposta |
|---|---|
| O quê | Banco PostgreSQL completo (`pg_dump` formato custom). Imagens de produto: versionamento/replicação do bucket S3 (D-P04) |
| Frequência | Diário (madrugada, horário de Recife) + antes de cada deploy com migration |
| Retenção | 14 diários, 8 semanais, 12 mensais (o script local guarda `BACKUP_RETENTION_DAYS`, padrão 14; semanais/mensais no storage com regra de ciclo de vida) |
| Criptografia | `age` com chave pública no servidor e chave privada **offline** (cofre de senhas + cópia física). Nada sai do servidor sem cifrar |
| Local | Fora do provedor do banco (outra conta/região), com bloqueio contra exclusão (object lock) se disponível |
| Integridade | Checksum SHA-256 ao lado de cada arquivo, conferido antes de restaurar |
| Teste | **Mensal**: restaurar o último backup num banco de verificação e conferir contagens. Registrar data e resultado |
| Objetivos | RPO ≤ 24 h (perda máxima de dados); RTO ≤ 4 h (tempo para voltar) — revisar com o proprietário |

Backups contêm dados pessoais: acesso restrito e expiração conforme `PRIVACY_DATA_MAP.md`.

## Scripts

```bash
# Backup (local: banco do docker compose)
infra/backup/backup.sh
# Produção: conexão direta + cifragem
PGHOST=... PGUSER=... PGPASSWORD=... PGDATABASE=... BACKUP_AGE_RECIPIENT=age1... infra/backup/backup.sh

# Teste de restauração (cria/recria o banco "<banco>_restore_check" e confere contagens)
infra/backup/restore.sh backups/gigaverse3d-AAAAMMDDTHHMMSSZ.dump
AGE_IDENTITY_FILE=~/chave-backup.txt infra/backup/restore.sh backups/...dump.age
```

O backup recusa arquivo vazio ou sem a assinatura `PGDMP`. A restauração confere o checksum, para no primeiro erro e mostra a versão da migration e as contagens de `users`, `products`, `orders` e `audit_log`.

**Teste realizado em 31/10/2026 (ambiente de desenvolvimento):** backup de 96 KB → restaurado em `gigaverse3d_restore_check` → migration V8, `users` 23, `products` 10, `orders` 8, `audit_log` 67, idênticos à origem. Banco de verificação apagado depois.

## Restaurar em incidente (produção)

1. Colocar a loja em manutenção (evita pedidos novos durante a troca).
2. Fazer um backup do estado atual, mesmo corrompido (pode ser útil na investigação).
3. Restaurar num banco novo e conferir (`restore.sh arquivo banco_novo`).
4. Apontar `DB_URL` para o banco restaurado e reiniciar a aplicação (o Flyway confere as migrations na subida); ou restaurar sobre o banco original com `RESTORE_CONFIRM=sim`.
5. Conferir pedidos e pagamentos do intervalo perdido com o painel do gateway e os e-mails enviados; contatar clientes afetados.
6. Registrar no relato do incidente (`INCIDENT_RESPONSE.md`).
