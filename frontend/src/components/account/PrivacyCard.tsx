import { useQueryClient } from '@tanstack/react-query'
import { Download, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError, apiDownload, apiSend } from '../../lib/api'
import { buttonClass } from '../ui/button'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { InputField } from '../ui/Field'
import { Card } from '../ui/PageHeader'

/** Direitos do titular na própria conta: cópia dos dados e exclusão (anonimização). */
export function PrivacyCard() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function download() {
    setError(null)
    try {
      await apiDownload('/api/v1/account/privacy/export', 'meus-dados-gigaverse3d.json')
      setMessage('Arquivo gerado. Guarde-o em local seguro: ele contém seus dados pessoais.')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Não foi possível gerar o arquivo.')
    }
  }

  async function remove() {
    setConfirming(false)
    setBusy(true)
    setError(null)
    try {
      await apiSend('POST', '/api/v1/account/privacy/delete-account', { password })
      queryClient.clear()
      navigate('/', { replace: true, state: { notice: 'Sua conta foi excluída.' } })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Não foi possível excluir a conta.')
    } finally {
      setBusy(false)
      setPassword('')
    }
  }

  return (
    <Card>
      <h2 className="font-semibold">Privacidade</h2>
      <p className="mt-1 text-sm text-fg-muted">
        Veja como tratamos seus dados na{' '}
        <Link to="/privacidade" className="text-accent hover:underline">
          política de privacidade
        </Link>
        . A preferência de e-mails de ofertas fica em “Dados pessoais”, acima.
      </p>
      {message && (
        <p role="status" className="mt-3 text-sm text-success">
          {message}
        </p>
      )}
      {error && (
        <p role="alert" className="mt-3 text-sm text-danger">
          {error}
        </p>
      )}
      <div className="mt-4 flex flex-col gap-6">
        <div>
          <h3 className="text-sm font-semibold">Cópia dos seus dados</h3>
          <p className="mt-1 text-sm text-fg-muted">Conta, endereços, consentimentos e pedidos em um arquivo JSON.</p>
          <button type="button" className={buttonClass('outline', 'sm', 'mt-2')} onClick={() => void download()}>
            <Download aria-hidden className="size-4" /> Baixar meus dados
          </button>
        </div>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            if (!password) return setError('Confirme com sua senha.')
            setConfirming(true)
          }}
          className="flex flex-col gap-2"
        >
          <h3 className="text-sm font-semibold text-danger">Excluir conta</h3>
          <p className="text-sm text-fg-muted">
            Seu nome, e-mail, telefone e endereços são removidos e você não poderá mais entrar com esta conta. Pedidos
            já feitos ficam guardados pelo prazo legal, sem seus dados de contato. Não é possível excluir com pedidos em
            andamento.
          </p>
          <div className="max-w-sm">
            <InputField
              id="delete-password"
              label="Sua senha"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit" className={buttonClass('danger', 'sm', 'self-start')} disabled={busy}>
            <Trash2 aria-hidden className="size-4" /> {busy ? 'Excluindo…' : 'Excluir minha conta'}
          </button>
        </form>
      </div>
      <ConfirmDialog
        open={confirming}
        title="Excluir sua conta?"
        description="Esta ação não pode ser desfeita. Você sairá da conta imediatamente."
        confirmLabel="Excluir definitivamente"
        onCancel={() => setConfirming(false)}
        onConfirm={() => void remove()}
      />
    </Card>
  )
}
