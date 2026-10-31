import { FilePenLine } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { useCategories } from '../../api/catalog'
import { InstagramLink } from '../../components/InstagramLink'
import { demoCategoryImage } from '../../data/store'
import { categoryImage } from '../../lib/images'
import { usePageMeta } from '../../lib/seo'

/** Aviso obrigatório: textos institucionais ainda não revisados pelo proprietário/jurídico. */
function DraftNotice() {
  return (
    <p className="flex items-start gap-2 rounded-lg border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm text-amber-800 dark:text-amber-200">
      <FilePenLine aria-hidden className="mt-0.5 size-4 shrink-0" />
      Rascunho — texto a ser revisado e aprovado pela Gigaverse3D antes do lançamento.
    </p>
  )
}

function Article({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  usePageMeta(title, description)
  return (
    <article className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold">{title}</h1>
      <div className="mt-4">
        <DraftNotice />
      </div>
      <div className="mt-8 space-y-6 leading-relaxed text-fg-muted [&_h2]:mt-8 [&_h2]:text-xl [&_h2]:font-semibold [&_h2]:text-fg [&_li]:ml-5 [&_li]:list-disc">
        {children}
      </div>
    </article>
  )
}

export function AboutPage() {
  return (
    <Article title="Sobre a Gigaverse3D" description="Impressão 3D, colecionáveis e peças personalizadas.">
      <p>
        A Gigaverse3D produz peças em impressão 3D: colecionáveis, itens articulados, miniaturas, decoração, acessórios
        e projetos sob medida.
      </p>
      <p>
        Cada peça é impressa, revisada e embalada pela nossa equipe. Itens sob encomenda informam o prazo de produção na
        página do produto.
      </p>
      <InstagramLink />
    </Article>
  )
}

export function ContactPage() {
  return (
    <Article title="Contato" description="Fale com a Gigaverse3D.">
      <p>
        Por enquanto, o atendimento é feito pelo Instagram. Os canais oficiais de e-mail e WhatsApp serão publicados
        aqui assim que definidos.
      </p>
      <InstagramLink />
    </Article>
  )
}

export function FaqPage() {
  return (
    <Article title="Perguntas frequentes" description="Pagamento, entrega, prazos e cancelamento.">
      <h2>Quais formas de pagamento são aceitas?</h2>
      <p>
        PIX e cartão. Depois do pedido, enviamos a chave PIX ou um link de pagamento seguro para o seu e-mail. Nenhum
        dado de cartão é digitado neste site.
      </p>
      <h2>Quando meu pedido é confirmado?</h2>
      <p>
        Quando a equipe confere o pagamento. Pedidos sem pagamento identificado em até 72 horas são cancelados
        automaticamente e os itens voltam ao estoque.
      </p>
      <h2>Como funciona a entrega?</h2>
      <p>
        Você pode escolher a retirada (local e horário combinados após a confirmação) ou a entrega, quando disponível
        para o seu CEP — o valor e o prazo aparecem no checkout.
      </p>
      <h2>Posso cancelar?</h2>
      <p>
        Sim, pela área “Meus pedidos”, enquanto o pedido estiver aguardando pagamento. Depois disso, fale conosco. As
        regras de troca e devolução estão na{' '}
        <Link to="/politica-comercial" className="text-accent underline">
          política comercial
        </Link>
        .
      </p>
      <h2>Vocês fazem peças personalizadas?</h2>
      <p>
        Sim. Veja{' '}
        <Link to="/personalizados" className="text-accent underline">
          como funciona
        </Link>{' '}
        e fale com a gente pelo Instagram para receber um orçamento.
      </p>
    </Article>
  )
}

export function CommercialPolicyPage() {
  return (
    <Article title="Política comercial" description="Pagamento, cancelamento, trocas e devoluções.">
      <h2>Pagamento e confirmação</h2>
      <p>O pedido é confirmado após a conferência do pagamento (PIX ou cartão, via link enviado por e-mail).</p>
      <h2>Cancelamento</h2>
      <p>
        O cliente pode cancelar pelo site enquanto o pedido aguarda pagamento. Pedidos não pagos em 72 horas são
        cancelados automaticamente.
      </p>
      <h2>Trocas e devoluções</h2>
      <p>
        <strong>Pendente de definição pela Gigaverse3D</strong> (prazos, condições para itens personalizados, quem arca
        com o frete). O texto final deve respeitar o Código de Defesa do Consumidor, incluindo o direito de
        arrependimento em compras on-line.
      </p>
    </Article>
  )
}

export function PrivacyPage() {
  return (
    <Article title="Política de privacidade" description="Como tratamos seus dados pessoais.">
      <p>
        Este texto resume o que o sistema faz hoje e <strong>precisa ser revisado por responsável jurídico</strong>{' '}
        antes da publicação (LGPD).
      </p>
      <h2>Dados que coletamos</h2>
      <ul>
        <li>Conta: nome, e-mail e senha (guardada apenas como hash).</li>
        <li>Opcional: telefone e endereços de entrega.</li>
        <li>Pedidos: itens, valores e o endereço usado na compra.</li>
        <li>Consentimentos: aceite dos termos e preferência de receber e-mails promocionais.</li>
      </ul>
      <h2>Para que usamos</h2>
      <p>
        Para criar sua conta, processar e entregar pedidos, enviar comunicações sobre eles e, se você permitir,
        novidades.
      </p>
      <h2>O que não fazemos</h2>
      <ul>
        <li>Não armazenamos dados de cartão.</li>
        <li>Não vendemos seus dados.</li>
      </ul>
      <h2>Seus direitos</h2>
      <p>Você pode atualizar seus dados e preferências em “Minha conta”. Canal para demais solicitações: a definir.</p>
    </Article>
  )
}

export function TermsPage() {
  return (
    <Article title="Termos de uso" description="Condições de uso da loja Gigaverse3D.">
      <p>
        Termos de uso <strong>pendentes de redação e revisão jurídica</strong>. Até lá, valem a{' '}
        <Link to="/politica-comercial" className="text-accent underline">
          política comercial
        </Link>{' '}
        e a{' '}
        <Link to="/privacidade" className="text-accent underline">
          política de privacidade
        </Link>{' '}
        em rascunho.
      </p>
    </Article>
  )
}

export function CollectionsPage() {
  usePageMeta('Coleções', 'Explore as categorias da Gigaverse3D.')
  const { data: categories = [], isPending } = useCategories()
  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold">Coleções</h1>
      {isPending ? (
        <p role="status" className="mt-6 text-sm text-fg-muted">
          Carregando…
        </p>
      ) : (
        <ul className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {categories.map((c) => {
            const base = demoCategoryImage(c.slug)
            const img = base ? categoryImage(base) : null
            return (
              <li key={c.slug}>
                <Link
                  to={`/produtos?categoria=${c.slug}`}
                  className="group relative flex aspect-[3/2] flex-col justify-end overflow-hidden rounded-card border border-border bg-gradient-to-br from-[#0b1f4d] via-[#123a8f] to-[#06101f] shadow-card"
                >
                  {img && (
                    <img
                      src={img.src}
                      width={img.width}
                      height={img.height}
                      alt=""
                      loading="lazy"
                      className="absolute inset-0 h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                    />
                  )}
                  <span
                    aria-hidden
                    className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/30 to-transparent"
                  />
                  <span className="relative p-5 text-white">
                    <span className="block text-xl font-semibold">{c.name}</span>
                    {c.description && <span className="text-sm text-white/80">{c.description}</span>}
                  </span>
                </Link>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
