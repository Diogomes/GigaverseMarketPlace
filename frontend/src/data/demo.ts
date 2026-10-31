/**
 * DADOS DE DEMONSTRAÇÃO — somente para visualizar os layouts em desenvolvimento.
 * Nomes, valores e pedidos são fictícios e NÃO representam a operação real.
 * Nunca são incluídos no comportamento de produção (ver DEMO_MODE em ./store.ts);
 * Pedidos, clientes e métricas de demonstração foram removidos na Fase 09 (o painel usa a API).
 */
import type { Category, Product } from './types'

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
