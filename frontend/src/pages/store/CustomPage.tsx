import { ClipboardList, MessageCircle, Printer, Truck } from 'lucide-react'
import { InstagramLink } from '../../components/InstagramLink'
import { HERO_IMAGES } from '../../lib/images'

const STEPS = [
  { icon: MessageCircle, title: 'Conte sua ideia', text: 'Envie referências, medidas e a finalidade da peça.' },
  { icon: ClipboardList, title: 'Receba o orçamento', text: 'Você aprova valor e prazo antes de qualquer pagamento.' },
  { icon: Printer, title: 'Produção', text: 'Modelagem/ajustes, impressão e acabamento.' },
  { icon: Truck, title: 'Entrega', text: 'Envio ou retirada, conforme combinado.' },
]

export function CustomPage() {
  return (
    <>
      <section className="relative isolate overflow-hidden">
        <img
          src={HERO_IMAGES.alternate.src}
          width={HERO_IMAGES.alternate.width}
          height={HERO_IMAGES.alternate.height}
          alt={HERO_IMAGES.alternate.alt}
          fetchPriority="high"
          className="absolute inset-0 -z-10 h-full w-full object-cover object-[65%_center]"
        />
        <div aria-hidden className="absolute inset-0 -z-10 bg-gradient-to-r from-bg via-bg/85 to-bg/30" />
        <div className="mx-auto max-w-5xl px-4 py-20 sm:px-6">
          <p className="text-sm font-semibold uppercase tracking-widest text-accent">Personalizados</p>
          <h1 className="mt-2 text-4xl font-bold">Sua ideia em 3D</h1>
          <p className="mt-4 max-w-xl text-fg-muted">
            Fazemos peças sob medida a partir da sua referência, com pintura personalizada opcional. O formulário de
            pedido personalizado será publicado assim que as regras (envio de arquivos, sinal e prazos) forem definidas;
            por enquanto, fale com a gente pelo Instagram.
          </p>
        </div>
      </section>
      <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6">
        <ol className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {STEPS.map((s, i) => (
            <li key={s.title} className="rounded-card border border-border bg-surface p-5 shadow-card">
              <span className="text-xs font-semibold text-fg-muted">Passo {i + 1}</span>
              <s.icon aria-hidden className="mt-3 size-7 text-accent" />
              <h2 className="mt-3 font-semibold">{s.title}</h2>
              <p className="mt-1 text-sm text-fg-muted">{s.text}</p>
            </li>
          ))}
        </ol>
        <div className="mt-10">
          <InstagramLink />
        </div>
      </div>
    </>
  )
}
