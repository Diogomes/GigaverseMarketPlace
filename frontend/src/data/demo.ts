/**
 * DADOS DE DEMONSTRAÇÃO — somente para visualizar os layouts em desenvolvimento.
 * Nomes, valores e pedidos são fictícios e NÃO representam a operação real.
 * Nunca são incluídos no comportamento de produção (ver DEMO_MODE em ./store.ts);
 * serão substituídos pelas APIs reais nas Fases 04–09.
 */
import { isoDay } from '../lib/format'
import type { Address, Category, Customer, Order, OrderStatus, Product } from './types'

export const demoCategories: Category[] = [
  { slug: 'colecionaveis', name: 'Colecionáveis', description: 'Estátuas e displays com acabamento detalhado.' },
  { slug: 'articulados', name: 'Articulados', description: 'Peças com juntas móveis impressas em uma só etapa.' },
  { slug: 'decoracao', name: 'Decoração', description: 'Vasos, luminárias e objetos para o ambiente.' },
  { slug: 'acessorios', name: 'Acessórios', description: 'Suportes, organizadores e utilidades para o setup.' },
  { slug: 'personalizados', name: 'Personalizados', description: 'Projetos sob medida a partir da sua ideia.' },
]

const sizes = {
  name: 'Tamanho',
  values: [{ label: '10 cm' }, { label: '15 cm', extraCents: 3000 }, { label: '20 cm', extraCents: 6000 }],
}
const finish = { name: 'Acabamento', values: [{ label: 'Padrão' }, { label: 'Pintado à mão', extraCents: 8000 }] }

export const demoProducts: Product[] = [
  {
    id: 'p1',
    slug: 'dragao-articulado',
    sku: 'GV-ART-001',
    name: 'Dragão Articulado',
    categorySlug: 'articulados',
    description: 'Dragão com mais de 30 articulações impressas no lugar, ideal para decoração e presente.',
    priceCents: 18990,
    stock: 20,
    lowStockThreshold: 5,
    status: 'ATIVO',
    type: 'PRONTO',
    featured: true,
    options: [sizes],
  },
  {
    id: 'p2',
    slug: 'astronauta-colecionavel',
    sku: 'GV-COL-002',
    name: 'Astronauta Colecionável',
    categorySlug: 'colecionaveis',
    description: 'Estátua de astronauta em pose de exploração, com base lunar texturizada.',
    priceCents: 26990,
    promoPriceCents: 24990,
    stock: 4,
    lowStockThreshold: 5,
    status: 'ATIVO',
    type: 'PRONTO',
    featured: true,
    options: [sizes, finish],
  },
  {
    id: 'p3',
    slug: 'robo-mecha-display',
    sku: 'GV-COL-003',
    name: 'Robô Mecha Display',
    categorySlug: 'colecionaveis',
    description: 'Display de robô mecha com peças encaixáveis e detalhes em alto relevo.',
    priceCents: 29990,
    stock: 0,
    lowStockThreshold: 3,
    status: 'ATIVO',
    type: 'SOB_ENCOMENDA',
    productionDays: 7,
    featured: true,
    options: [finish],
  },
  {
    id: 'p4',
    slug: 'luminaria-lua-3d',
    sku: 'GV-DEC-004',
    name: 'Luminária Lua 3D',
    categorySlug: 'decoracao',
    description: 'Luminária com relevo lunar e luz LED de tom quente.',
    priceCents: 15990,
    stock: 12,
    lowStockThreshold: 4,
    status: 'ATIVO',
    type: 'PRONTO',
    featured: true,
    options: [],
  },
  {
    id: 'p5',
    slug: 'vaso-geometrico-voronoi',
    sku: 'GV-DEC-005',
    name: 'Vaso Geométrico Voronoi',
    categorySlug: 'decoracao',
    description: 'Vaso decorativo com padrão Voronoi vazado (uso com flores secas).',
    priceCents: 8990,
    stock: 2,
    lowStockThreshold: 4,
    status: 'ATIVO',
    type: 'PRONTO',
    featured: false,
    options: [],
  },
  {
    id: 'p6',
    slug: 'suporte-para-headset',
    sku: 'GV-ACE-006',
    name: 'Suporte para Headset',
    categorySlug: 'acessorios',
    description: 'Suporte de mesa com base pesada e passagem de cabo.',
    priceCents: 9990,
    stock: 25,
    lowStockThreshold: 5,
    status: 'ATIVO',
    type: 'PRONTO',
    featured: false,
    options: [],
  },
  {
    id: 'p7',
    slug: 'organizador-hexagonal',
    sku: 'GV-ACE-007',
    name: 'Organizador de Mesa Hexagonal',
    categorySlug: 'acessorios',
    description: 'Módulos hexagonais encaixáveis para canetas e pequenos objetos.',
    priceCents: 6990,
    stock: 3,
    lowStockThreshold: 5,
    status: 'ATIVO',
    type: 'PRONTO',
    featured: false,
    options: [],
  },
  {
    id: 'p8',
    slug: 'miniatura-personalizada',
    sku: 'GV-PER-008',
    name: 'Miniatura Personalizada',
    categorySlug: 'personalizados',
    description: 'Miniatura feita a partir da sua referência. Valor final após orçamento.',
    priceCents: 14990,
    stock: 0,
    lowStockThreshold: 0,
    status: 'ATIVO',
    type: 'PERSONALIZADO',
    productionDays: 15,
    featured: false,
    options: [sizes, finish],
  },
  {
    id: 'p9',
    slug: 'chaveiro-hexagono',
    sku: 'GV-ACE-009',
    name: 'Chaveiro Hexágono',
    categorySlug: 'acessorios',
    description: 'Chaveiro leve e resistente.',
    priceCents: 1990,
    stock: 0,
    lowStockThreshold: 10,
    status: 'RASCUNHO',
    type: 'PRONTO',
    featured: false,
    options: [],
  },
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
