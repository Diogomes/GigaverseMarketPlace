/**
 * DADOS DE DEMONSTRAÇÃO — somente para visualizar os layouts em desenvolvimento.
 * Nomes, valores e pedidos são fictícios e NÃO representam a operação real.
 * Nunca são incluídos no comportamento de produção (ver DEMO_MODE em ./store.ts);
 * serão substituídos pelas APIs reais nas Fases 04–09.
 */
import { isoDay } from '../lib/format'
import type { Address, Category, Customer, Order, OrderStatus, Product } from './types'

export const demoCategories: Category[] = [
  {
    slug: 'colecionaveis',
    name: 'Colecionáveis',
    description: 'Estátuas e displays com acabamento detalhado.',
    image: 'sci-fi-knight',
  },
  {
    slug: 'articulados',
    name: 'Articulados',
    description: 'Peças com juntas móveis impressas em uma só etapa.',
    image: 'articulated-raptor-toy',
  },
  {
    slug: 'miniaturas',
    name: 'Miniaturas',
    description: 'Miniaturas em resina prontas para pintar.',
    image: 'unpainted-miniature-workbench',
  },
  {
    slug: 'personalizados',
    name: 'Personalizados',
    description: 'Projetos sob medida e pintura personalizada.',
    image: 'fantasy-warrior',
  },
  { slug: 'decoracao', name: 'Decoração', description: 'Luminárias e objetos para o ambiente.' },
  { slug: 'acessorios', name: 'Acessórios', description: 'Suportes e utilidades para o setup.' },
]

const sizes = {
  name: 'Tamanho',
  values: [{ label: '10 cm' }, { label: '15 cm', extraCents: 3000 }, { label: '20 cm', extraCents: 6000 }],
}
const finish = { name: 'Acabamento', values: [{ label: 'Padrão' }, { label: 'Pintado à mão', extraCents: 8000 }] }

type Seed = Omit<Product, 'lowStockThreshold' | 'featured' | 'options' | 'status'> &
  Partial<Pick<Product, 'lowStockThreshold' | 'featured' | 'options' | 'status'>>

function product(p: Seed): Product {
  return { lowStockThreshold: 5, featured: false, options: [], status: 'ATIVO', ...p }
}

/** Os 6 primeiros usam os conceitos de imagem do pacote de assets (docs/IMAGE_ASSETS.md). */
export const demoProducts: Product[] = [
  product({
    id: 'p1',
    slug: 'dragao-mecanico-azul',
    sku: 'GV-COL-001',
    name: 'Dragão Mecânico Azul',
    categorySlug: 'colecionaveis',
    description: 'Escultura de dragão mecânico com placas sobrepostas e detalhes em azul.',
    priceCents: 34990,
    promoPriceCents: 31990,
    stock: 4,
    type: 'PRONTO',
    featured: true,
    options: [sizes, finish],
    image: { base: 'blue-mechanical-dragon', alt: 'Escultura de dragão mecânico branco e azul' },
  }),
  product({
    id: 'p2',
    slug: 'raptor-articulado',
    sku: 'GV-ART-002',
    name: 'Raptor Articulado',
    categorySlug: 'articulados',
    description: 'Dinossauro com juntas móveis impressas no lugar, em azul e laranja.',
    priceCents: 12990,
    stock: 20,
    type: 'PRONTO',
    featured: true,
    options: [sizes],
    image: { base: 'articulated-raptor-toy', alt: 'Raptor articulado azul e laranja sobre uma mesa' },
  }),
  product({
    id: 'p3',
    slug: 'cavaleiro-sci-fi',
    sku: 'GV-COL-003',
    name: 'Cavaleiro Sci-Fi',
    categorySlug: 'colecionaveis',
    description: 'Cavaleiro de armadura futurista com espada e escudo luminosos, sobre base de ruínas.',
    priceCents: 29990,
    stock: 0,
    lowStockThreshold: 3,
    type: 'SOB_ENCOMENDA',
    productionDays: 7,
    featured: true,
    options: [finish],
    image: { base: 'sci-fi-knight', alt: 'Estátua de cavaleiro de armadura prateada com espada azul' },
  }),
  product({
    id: 'p4',
    slug: 'pantera-mecanica',
    sku: 'GV-COL-004',
    name: 'Pantera Mecânica',
    categorySlug: 'colecionaveis',
    description: 'Pantera robótica com placas escuras e detalhes iluminados em laranja.',
    priceCents: 27990,
    stock: 6,
    type: 'PRONTO',
    featured: true,
    options: [finish],
    image: { base: 'mechanical-panther', alt: 'Escultura de pantera mecânica preta com luzes laranja' },
  }),
  product({
    id: 'p5',
    slug: 'guerreira-de-fantasia',
    sku: 'GV-COL-005',
    name: 'Guerreira de Fantasia',
    categorySlug: 'colecionaveis',
    description: 'Guerreira com escudo e martelo, pintada à mão com acabamento envelhecido.',
    priceCents: 25990,
    stock: 2,
    lowStockThreshold: 4,
    type: 'PRONTO',
    options: [sizes, finish],
    image: { base: 'fantasy-warrior', alt: 'Miniatura pintada de guerreira com escudo redondo e martelo' },
  }),
  product({
    id: 'p6',
    slug: 'miniatura-para-pintar',
    sku: 'GV-MIN-006',
    name: 'Miniatura para Pintar',
    categorySlug: 'miniaturas',
    description: 'Miniatura em resina cinza, sem pintura, pronta para o seu acabamento.',
    priceCents: 8990,
    stock: 25,
    type: 'PRONTO',
    options: [sizes],
    image: {
      base: 'unpainted-miniature-workbench',
      alt: 'Miniatura cinza sem pintura sobre bancada com tintas e pincéis',
    },
  }),
  product({
    id: 'p7',
    slug: 'luminaria-lua-3d',
    sku: 'GV-DEC-007',
    name: 'Luminária Lua 3D',
    categorySlug: 'decoracao',
    description: 'Luminária com relevo lunar e luz LED de tom quente.',
    priceCents: 15990,
    stock: 12,
    lowStockThreshold: 4,
    type: 'PRONTO',
  }),
  product({
    id: 'p8',
    slug: 'suporte-para-headset',
    sku: 'GV-ACE-008',
    name: 'Suporte para Headset',
    categorySlug: 'acessorios',
    description: 'Suporte de mesa com base pesada e passagem de cabo.',
    priceCents: 9990,
    stock: 3,
    type: 'PRONTO',
  }),
  product({
    id: 'p9',
    slug: 'miniatura-personalizada',
    sku: 'GV-PER-009',
    name: 'Miniatura Personalizada',
    categorySlug: 'personalizados',
    description: 'Miniatura feita a partir da sua referência. Valor final após orçamento.',
    priceCents: 14990,
    stock: 0,
    lowStockThreshold: 0,
    type: 'PERSONALIZADO',
    productionDays: 15,
    options: [sizes, finish],
  }),
  product({
    id: 'p10',
    slug: 'chaveiro-hexagono',
    sku: 'GV-ACE-010',
    name: 'Chaveiro Hexágono',
    categorySlug: 'acessorios',
    description: 'Chaveiro leve e resistente.',
    priceCents: 1990,
    stock: 0,
    lowStockThreshold: 10,
    status: 'RASCUNHO',
    type: 'PRONTO',
  }),
]

export const demoCustomers: Customer[] = [
  {
    id: 'c1',
    name: 'Ana Demonstração',
    email: 'ana@exemplo.com',
    createdAt: '',
    status: 'ATIVO',
    tags: ['Recorrente', 'Colecionáveis'],
    notes: [
      { id: 'n1', author: 'Equipe', at: '', text: 'Prefere retirar pedidos pessoalmente (exemplo de nota interna).' },
    ],
  },
  {
    id: 'c2',
    name: 'Bruno Exemplo',
    email: 'bruno@exemplo.com',
    createdAt: '',
    status: 'ATIVO',
    tags: ['Personalizados'],
    notes: [],
  },
  { id: 'c3', name: 'Carla Fictícia', email: 'carla@exemplo.com', createdAt: '', status: 'ATIVO', tags: [], notes: [] },
  {
    id: 'c4',
    name: 'Diego Teste',
    email: 'diego@exemplo.com',
    createdAt: '',
    status: 'ATIVO',
    tags: ['Atacado'],
    notes: [],
  },
  { id: 'c5', name: 'Elisa Modelo', email: 'elisa@exemplo.com', createdAt: '', status: 'INATIVO', tags: [], notes: [] },
  { id: 'c6', name: 'Fábio Amostra', email: 'fabio@exemplo.com', createdAt: '', status: 'ATIVO', tags: [], notes: [] },
  {
    id: 'c7',
    name: 'Gabi Simulada',
    email: 'gabi@exemplo.com',
    createdAt: '',
    status: 'ATIVO',
    tags: ['Recorrente'],
    notes: [],
  },
  { id: 'c8', name: 'Hugo Protótipo', email: 'hugo@exemplo.com', createdAt: '', status: 'ATIVO', tags: [], notes: [] },
]

export const demoAddresses: Address[] = [
  {
    id: 'a1',
    label: 'Casa',
    recipient: 'Ana Demonstração',
    line: 'Rua Exemplo, 123',
    district: 'Centro',
    city: 'Cidade Exemplo',
    state: 'PE',
    postalCode: '00000-000',
    isDefault: true,
  },
  {
    id: 'a2',
    label: 'Trabalho',
    recipient: 'Ana Demonstração',
    line: 'Av. Fictícia, 456, sala 7',
    district: 'Bairro Modelo',
    city: 'Cidade Exemplo',
    state: 'PE',
    postalCode: '00000-001',
    isDefault: false,
  },
]

// --- geração determinística de pedidos (mesmo resultado a cada carregamento) ---

function lcg(seed: number) {
  let s = seed
  return () => {
    s = (s * 1664525 + 1013904223) % 4294967296
    return s / 4294967296
  }
}

function addDays(date: Date, days: number): Date {
  const d = new Date(date)
  d.setDate(d.getDate() + days)
  return d
}

function buildOrders(today: Date): Order[] {
  const rand = lcg(42)
  const sellable = demoProducts.filter((p) => p.status === 'ATIVO')
  const orders: Order[] = []
  let seq = 1000
  for (let ago = 179; ago >= 0; ago--) {
    const day = addDays(today, -ago)
    const count = rand() < 0.35 ? 0 : rand() < 0.75 ? 1 : 2
    for (let k = 0; k < count; k++) {
      const customer = demoCustomers[Math.floor(rand() * demoCustomers.length)]
      const itemCount = rand() < 0.7 ? 1 : 2
      const items = Array.from({ length: itemCount }, () => {
        const p = sellable[Math.floor(rand() * sellable.length)]
        const variant = p.options.length ? p.options.map((g) => g.values[0].label).join(' · ') : undefined
        return { productSlug: p.slug, name: p.name, variant, quantity: 1, unitCents: p.promoPriceCents ?? p.priceCents }
      })
      const shippingCents = 2490
      const subtotal = items.reduce((s, i) => s + i.unitCents * i.quantity, 0)
      let status: OrderStatus
      const r = rand()
      if (ago <= 2) status = r < 0.5 ? 'AGUARDANDO_PAGAMENTO' : 'EM_PRODUCAO'
      else if (ago <= 7) status = r < 0.5 ? 'EM_PRODUCAO' : 'ENVIADO'
      else status = r < 0.9 ? 'ENTREGUE' : 'CANCELADO'
      const created = isoDay(day)
      const flow: OrderStatus[] =
        status === 'CANCELADO'
          ? ['AGUARDANDO_PAGAMENTO', 'CANCELADO']
          : ORDER_FLOW.slice(0, ORDER_FLOW.indexOf(status) + 1)
      orders.push({
        number: String(seq++),
        customerId: customer.id,
        createdAt: created,
        status,
        items,
        shippingCents,
        totalCents: subtotal + shippingCents,
        history: flow.map((s, i) => ({ status: s, at: isoDay(addDays(day, i * 2)) })),
      })
    }
  }
  return orders.reverse()
}

const ORDER_FLOW: OrderStatus[] = ['AGUARDANDO_PAGAMENTO', 'EM_PRODUCAO', 'ENVIADO', 'ENTREGUE']

export const DEMO_TODAY = new Date()
export const demoOrders: Order[] = buildOrders(DEMO_TODAY)

// datas de cadastro coerentes com o primeiro pedido de cada cliente
for (const c of demoCustomers) {
  const first = demoOrders.filter((o) => o.customerId === c.id).at(-1)
  c.createdAt = first?.createdAt ?? isoDay(addDays(DEMO_TODAY, -200))
  for (const n of c.notes) n.at = c.createdAt
}
