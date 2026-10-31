import { Download, FileText, History, Mail, ShieldCheck, UserPlus } from 'lucide-react'
import { useState } from 'react'
import {
  exportCustomers,
  exportOrders,
  useAudit,
  useSettings,
  useStaff,
  useStaffActions,
  type StaffRole,
  type StaffUser,
} from '../../api/admin'
import { useAuth } from '../../auth/AuthProvider'
import {
  ActionError,
  errorMessage,
  FilterSelect,
  Loading,
  Pagination,
  QueryError,
  rowClass,
  tableWrapClass,
  theadClass,
} from '../../components/admin/AdminUi'
import { CUSTOMER_STATUS_UI, ROLE_UI } from '../../components/admin/labels'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { EmptyState } from '../../components/ui/EmptyState'
import { controlClass } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { formatDateTime, formatPrice } from '../../lib/format'

/** Páginas exclusivas de ADMIN mostram este aviso para operadores (o servidor também recusa: 403). */
function AdminOnly() {
  return (
    <EmptyState
      icon={ShieldCheck}
      title="Área restrita a administradores"
      description="Peça a um administrador caso precise desta informação."
    />
  )
}

// ------------------------------------------------------------------ equipe

export function StaffPage() {
  const { user } = useAuth()
  if (user?.role !== 'ADMIN') return <AdminOnly />
  return <Staff myId={user.id} />
}

function Staff({ myId }: { myId: number }) {
  const { data, error, isPending, refetch } = useStaff()
  const actions = useStaffActions()
  const [email, setEmail] = useState('')
  const [fullName, setFullName] = useState('')
  const [role, setRole] = useState<StaffRole>('OPERATOR')
  const [notice, setNotice] = useState<string | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const [confirm, setConfirm] = useState<{ user: StaffUser; kind: 'disable' | 'role' } | null>(null)

  async function run(action: () => Promise<unknown>, message: string) {
    setFailure(null)
    setNotice(null)
    try {
      await action()
      setNotice(message)
    } catch (e) {
      setFailure(errorMessage(e))
    }
  }

  async function invite(e: React.FormEvent) {
    e.preventDefault()
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.trim())) return setFailure('Informe um e-mail válido.')
    if (fullName.trim().length < 2) return setFailure('Informe o nome.')
    await run(async () => {
      await actions.create({ email: email.trim(), fullName: fullName.trim(), role })
      setEmail('')
      setFullName('')
    }, `Convite enviado para ${email.trim()}. A pessoa define a própria senha pelo link (válido por 48 h).`)
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Equipe" description="Contas internas e papéis. Dê a cada pessoa só o acesso necessário." />
      <Card>
        <h2 className="flex items-center gap-2 font-semibold">
          <UserPlus aria-hidden className="size-4" /> Convidar pessoa
        </h2>
        <form onSubmit={invite} className="mt-3 grid gap-3 md:grid-cols-[1fr_1fr_auto_auto]">
          <label className="text-sm">
            <span className="sr-only">Nome</span>
            <input
              value={fullName}
              maxLength={120}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Nome"
              className={`${controlClass} h-10`}
            />
          </label>
          <label className="text-sm">
            <span className="sr-only">E-mail</span>
            <input
              type="email"
              value={email}
              maxLength={254}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="email@dominio.com"
              className={`${controlClass} h-10`}
            />
          </label>
          <FilterSelect label="Papel" value={role} onChange={(v) => setRole(v as StaffRole)}>
            <option value="OPERATOR">Operador</option>
            <option value="ADMIN">Administrador</option>
          </FilterSelect>
          <button type="submit" className={buttonClass('primary')}>
            Enviar convite
          </button>
        </form>
        <p className="mt-2 text-xs text-fg-muted">
          Operador: pedidos, produtos, estoque e CRM. Administrador: tudo isso, mais confirmar pagamentos, excluir,
          exportar dados, ver a auditoria e gerenciar a equipe. Use um e-mail diferente da conta de cliente.
        </p>
      </Card>
      {notice && <Notice>{notice}</Notice>}
      <ActionError message={failure} />
      {error ? (
        <QueryError error={error} onRetry={() => void refetch()} />
      ) : isPending ? (
        <Loading />
      ) : (
        <div className={tableWrapClass}>
          <table className="w-full min-w-[44rem] text-sm">
            <caption className="sr-only">Equipe</caption>
            <thead className={theadClass}>
              <tr>
                <th scope="col" className="px-4 py-3 font-medium">
                  Pessoa
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Papel
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Último acesso
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Situação
                </th>
                <th scope="col" className="px-4 py-3 text-right font-medium">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody>
              {data.map((u) => {
                const self = u.id === myId
                return (
                  <tr key={u.id} className={rowClass}>
                    <td className="px-4 py-3">
                      <span className="block font-medium">
                        {u.name}
                        {self && <span className="font-normal text-fg-muted"> (você)</span>}
                      </span>
                      <span className="text-xs text-fg-muted">{u.email}</span>
                    </td>
                    <td className="px-4 py-3">{ROLE_UI[u.role]}</td>
                    <td className="px-4 py-3 text-fg-muted">
                      {u.lastLoginAt ? formatDateTime(u.lastLoginAt) : 'Ainda não acessou'}
                    </td>
                    <td className="px-4 py-3">
                      <Badge tone={CUSTOMER_STATUS_UI[u.status].tone}>{CUSTOMER_STATUS_UI[u.status].label}</Badge>
                    </td>
                    <td className="px-4 py-3">
                      {!self && (
                        <div className="flex flex-wrap justify-end gap-1">
                          <button
                            type="button"
                            className={buttonClass('ghost', 'sm')}
                            onClick={() => setConfirm({ user: u, kind: 'role' })}
                          >
                            Tornar {u.role === 'ADMIN' ? 'operador' : 'administrador'}
                          </button>
                          {u.status === 'ACTIVE' ? (
                            <>
                              {!u.lastLoginAt && (
                                <button
                                  type="button"
                                  className={buttonClass('ghost', 'sm')}
                                  aria-label={`Reenviar convite para ${u.name}`}
                                  onClick={() =>
                                    void run(() => actions.invite(u.id), `Novo convite enviado para ${u.email}.`)
                                  }
                                >
                                  <Mail aria-hidden className="size-4" />
                                </button>
                              )}
                              <button
                                type="button"
                                className={buttonClass('danger', 'sm')}
                                onClick={() => setConfirm({ user: u, kind: 'disable' })}
                              >
                                Desativar
                              </button>
                            </>
                          ) : (
                            <button
                              type="button"
                              className={buttonClass('outline', 'sm')}
                              onClick={() =>
                                void run(() => actions.setEnabled(u.id, true), `${u.name} foi reativado(a).`)
                              }
                            >
                              Reativar
                            </button>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
      <ConfirmDialog
        open={confirm !== null}
        title={confirm?.kind === 'disable' ? 'Desativar conta?' : 'Alterar papel?'}
        description={
          confirm?.kind === 'disable'
            ? `${confirm.user.name} perde o acesso imediatamente (as sessões abertas são encerradas). Você pode reativar depois.`
            : `${confirm?.user.name ?? ''} passará a ser ${confirm?.user.role === 'ADMIN' ? 'operador' : 'administrador'}. As sessões abertas são encerradas.`
        }
        confirmLabel={confirm?.kind === 'disable' ? 'Desativar' : 'Alterar papel'}
        onCancel={() => setConfirm(null)}
        onConfirm={() => {
          const c = confirm
          setConfirm(null)
          if (!c) return
          if (c.kind === 'disable')
            void run(() => actions.setEnabled(c.user.id, false), `${c.user.name} foi desativado(a).`)
          else {
            const next: StaffRole = c.user.role === 'ADMIN' ? 'OPERATOR' : 'ADMIN'
            void run(
              () => actions.changeRole(c.user.id, next),
              `${c.user.name} agora é ${ROLE_UI[next].toLowerCase()}.`,
            )
          }
        }}
      />
    </div>
  )
}

// ------------------------------------------------------------------ auditoria

const AUDIT_ACTIONS = [
  'PAYMENT_CONFIRMED',
  'ORDER_STATUS_CHANGED',
  'ORDER_CANCELLED',
  'PRODUCT_CREATED',
  'PRODUCT_UPDATED',
  'PRODUCT_STATUS_CHANGED',
  'PRODUCT_DELETED',
  'STOCK_PURCHASE',
  'STOCK_ADJUSTMENT',
  'CUSTOMER_NOTE_ADDED',
  'CUSTOMER_NOTE_DELETED',
  'CUSTOMER_TAGS_CHANGED',
  'CUSTOMERS_EXPORTED',
  'ORDERS_EXPORTED',
  'STAFF_USER_CREATED',
  'STAFF_ROLE_CHANGED',
  'STAFF_USER_DISABLED',
  'STAFF_USER_ENABLED',
  'LOGIN_FAILED',
]

export function AuditPage() {
  const { user } = useAuth()
  if (user?.role !== 'ADMIN') return <AdminOnly />
  return <Audit />
}

function Audit() {
  const [action, setAction] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)
  const { data, error, isPending, refetch } = useAudit({
    action: action || undefined,
    from: from || undefined,
    to: to || undefined,
    page,
  })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Auditoria" description="Registro das ações relevantes: quem fez, o quê e quando." />
      <div className="grid gap-3 sm:grid-cols-[1fr_auto_auto]">
        <FilterSelect
          label="Ação"
          value={action}
          onChange={(v) => {
            setAction(v)
            setPage(0)
          }}
        >
          <option value="">Todas as ações</option>
          {AUDIT_ACTIONS.map((a) => (
            <option key={a} value={a}>
              {a}
            </option>
          ))}
        </FilterSelect>
        <label>
          <span className="sr-only">De</span>
          <input
            type="date"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value)
              setPage(0)
            }}
            className={`${controlClass} h-10`}
          />
        </label>
        <label>
          <span className="sr-only">Até</span>
          <input
            type="date"
            value={to}
            onChange={(e) => {
              setTo(e.target.value)
              setPage(0)
            }}
            className={`${controlClass} h-10`}
          />
        </label>
      </div>
      {error ? (
        <QueryError error={error} onRetry={() => void refetch()} />
      ) : isPending ? (
        <Loading />
      ) : data.content.length === 0 ? (
        <EmptyState icon={History} title="Nenhum registro" />
      ) : (
        <>
          <div className={tableWrapClass}>
            <table className="w-full min-w-[48rem] text-sm">
              <caption className="sr-only">Registros de auditoria</caption>
              <thead className={theadClass}>
                <tr>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Quando
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Quem
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Ação
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Alvo
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Detalhes
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((e) => (
                  <tr key={e.id} className={`${rowClass} align-top`}>
                    <td className="px-4 py-3 whitespace-nowrap text-fg-muted">{formatDateTime(e.createdAt)}</td>
                    <td className="px-4 py-3">
                      {e.actorName ?? (e.actorId ? `#${e.actorId}` : 'Sistema')}
                      {e.actorEmail && <span className="block text-xs text-fg-muted">{e.actorEmail}</span>}
                    </td>
                    <td className="px-4 py-3 font-mono text-xs">{e.action}</td>
                    <td className="px-4 py-3 text-fg-muted">
                      {e.entityType}
                      {e.entityId && ` #${e.entityId}`}
                    </td>
                    <td className="max-w-xs px-4 py-3">
                      <code className="break-all text-xs text-fg-muted">
                        {e.details ? JSON.stringify(e.details) : '—'}
                      </code>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPage={setPage}
          />
        </>
      )}
    </div>
  )
}

// ------------------------------------------------------------------ configurações

export function SettingsPage() {
  const { data, error, isPending, refetch } = useSettings()
  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Configurações"
        description="Parâmetros operacionais em vigor. São definidos por variáveis de ambiente no deploy (mudança rastreável), não por esta tela."
      />
      {error ? (
        <QueryError error={error} onRetry={() => void refetch()} />
      ) : isPending ? (
        <Loading />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          <Card>
            <h2 className="font-semibold">Pagamentos</h2>
            <dl className="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
              <dt className="text-fg-muted">Provedor</dt>
              <dd>
                {data.paymentProvider === 'MANUAL'
                  ? 'Manual — pedido aguarda pagamento e um administrador confirma (gateway ainda não escolhido)'
                  : data.paymentProvider}
              </dd>
              <dt className="text-fg-muted">Meios</dt>
              <dd>{data.paymentMethods.map((m) => (m === 'CARD' ? 'Cartão' : m)).join(', ')}</dd>
              <dt className="text-fg-muted">Expiração</dt>
              <dd>Pedidos não pagos são cancelados após {data.pendingPaymentExpirationHours} h</dd>
            </dl>
          </Card>
          <Card>
            <h2 className="font-semibold">Entrega</h2>
            <dl className="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
              <dt className="text-fg-muted">CEP de origem</dt>
              <dd>{data.shippingOriginPostalCode.replace(/^(\d{5})(\d{3})$/, '$1-$2')}</dd>
              <dt className="text-fg-muted">Retirada</dt>
              <dd>{data.pickupEnabled ? 'Habilitada' : 'Desabilitada'}</dd>
            </dl>
            {data.shippingDemoValues && (
              <p className="mt-3 text-xs text-amber-700 dark:text-amber-300">
                Valores de frete fictícios (ambiente de desenvolvimento).
              </p>
            )}
            {data.shippingZones.length === 0 ? (
              <p className="mt-3 text-sm text-fg-muted">Nenhuma faixa de entrega: só retirada.</p>
            ) : (
              <ul className="mt-3 flex flex-col divide-y divide-border text-sm">
                {data.shippingZones.map((z) => (
                  <li key={z.name} className="flex justify-between gap-2 py-1.5">
                    <span>
                      {z.name}
                      <span className="block text-xs text-fg-muted">
                        CEP {z.fromPostalCode}–{z.toPostalCode} · {z.minDays}–{z.maxDays} dias úteis
                      </span>
                    </span>
                    <span className="tabular-nums">{formatPrice(z.price)}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>
          <Card>
            <h2 className="font-semibold">Sistema</h2>
            <dl className="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
              <dt className="text-fg-muted">Imagens</dt>
              <dd>{data.storageType === 's3' ? 'Armazenamento S3' : 'Disco local (desenvolvimento)'}</dd>
              <dt className="text-fg-muted">Fuso dos relatórios</dt>
              <dd>{data.timezone}</dd>
            </dl>
          </Card>
        </div>
      )}
    </div>
  )
}

// ------------------------------------------------------------------ relatórios e conteúdo

export function ReportsPage() {
  const { user } = useAuth()
  const [failure, setFailure] = useState<string | null>(null)
  const isAdmin = user?.role === 'ADMIN'
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Relatórios" description="Exportações e definições das métricas do dashboard." />
      <ActionError message={failure} />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <h2 className="font-semibold">Exportar dados</h2>
          {isAdmin ? (
            <>
              <p className="mt-2 text-sm text-fg-muted">
                CSV para planilha (separador “;”). Até 5.000 linhas por arquivo; cada exportação fica registrada na
                auditoria. Para filtrar, use os botões de exportar nas telas de Pedidos e Clientes.
              </p>
              <div className="mt-4 flex flex-wrap gap-2">
                <button
                  type="button"
                  className={buttonClass('outline')}
                  onClick={() => exportOrders({}).catch((e) => setFailure(errorMessage(e)))}
                >
                  <Download aria-hidden className="size-4" /> Todos os pedidos
                </button>
                <button
                  type="button"
                  className={buttonClass('outline')}
                  onClick={() => exportCustomers({}).catch((e) => setFailure(errorMessage(e)))}
                >
                  <Download aria-hidden className="size-4" /> Todos os clientes
                </button>
              </div>
              <p className="mt-3 text-xs text-fg-muted">
                Os arquivos contêm dados pessoais: guarde-os com cuidado e apague quando não precisar mais.
              </p>
            </>
          ) : (
            <p className="mt-2 text-sm text-fg-muted">Exportações são restritas a administradores.</p>
          )}
        </Card>
        <Card>
          <h2 className="font-semibold">Como as métricas são calculadas</h2>
          <ul className="mt-3 flex list-disc flex-col gap-2 pl-5 text-sm text-fg-muted">
            <li>Venda conta na data em que o pagamento foi confirmado, no horário de Recife.</li>
            <li>Vendas brutas: total (produtos + frete) dos pedidos pagos, inclusive os cancelados depois.</li>
            <li>Vendas líquidas: brutas − pedidos cancelados após pagamento − frete.</li>
            <li>Ticket médio: total dos pedidos pagos válidos ÷ quantidade desses pedidos.</li>
            <li>Pedidos por etapa e “pedidos criados” usam a data de criação do pedido.</li>
            <li>Comprador recorrente: já tinha compra paga antes do período.</li>
          </ul>
          <p className="mt-3 text-xs text-fg-muted">
            Proposta técnica (decisão D-P12) — confirme com o proprietário. Documento completo: docs/METRICS.md.
          </p>
        </Card>
      </div>
    </div>
  )
}

export function ContentPage() {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Conteúdo" />
      <EmptyState
        icon={FileText}
        title="Edição de conteúdo ainda não disponível"
        description="Banners e textos institucionais são publicados com o código nesta versão (os textos estão em rascunho até a revisão jurídica). Um editor de conteúdo pode entrar numa fase futura, se for prioridade."
      />
    </div>
  )
}
