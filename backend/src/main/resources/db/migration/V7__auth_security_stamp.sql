-- "Carimbo de segurança": muda quando a senha é trocada/redefinida ou a conta é desativada.
-- Cada requisição autenticada compara o carimbo da sessão com o do banco; se divergir,
-- a sessão é encerrada (derruba sessões antigas após troca de senha, por exemplo).
ALTER TABLE users ADD COLUMN security_stamp varchar(36) NOT NULL DEFAULT gen_random_uuid()::text;
