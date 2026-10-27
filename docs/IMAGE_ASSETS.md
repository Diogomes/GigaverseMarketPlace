# Imagens e assets

## Origem

| Pasta | Origem | Uso permitido |
|---|---|---|
| `frontend/public/brand/` | Logo oficial (`sistema-Gigaverse3d/assets/gigaverse-logo.png`) + recortes redimensionados sem distorção | Identidade da marca |
| `frontend/public/images/` | Pacote `Gigaverse3D_Pacote_Completo_Prompts_Layout_Assets/assets-mockup` — **conceitos originais gerados por IA para mockup** (sem imagens externas/bancos de imagem) | **Somente demonstração/desenvolvimento.** Substituir por fotos reais antes de vender (ver checklist abaixo) |
| *(não copiada)* `references-user-supplied/` | Imagens enviadas pelo proprietário como referência | **Não usadas** no site: podem representar propriedade intelectual de terceiros. Ficam no pacote original até confirmação de direitos |

Os arquivos `*-original-concept.jpg` também não foram copiados (são as fontes dos recortes).

## Arquivos e onde são usados

| Arquivo(s) | Dimensão | Proporção | Onde | Tratamento |
|---|---|---|---|---|
| `hero/hero-blue-dragon-1920x720.jpg` | 1920×720 | 8:3 | Hero da home | `object-cover`, `object-position 70%`, gradiente do tema por CSS para legibilidade; `fetchpriority=high` |
| `hero/hero-fantasy-warrior-1920x720.jpg` | 1920×720 | 8:3 | Cabeçalho de `/personalizados` (alternativo) | Idem |
| `products/<conceito>-card-600x750.jpg` | 600×750 | 4:5 | Cards de produto (home, catálogo) | `aspect-[4/5]`, `object-cover`, `loading=lazy` |
| `products/<conceito>-detail-1200x1200.jpg` | 1200×1200 | 1:1 | Imagem principal da página de produto; painel do login (`sci-fi-knight`) | `aspect-square`, carregamento prioritário |
| `products/<conceito>-square-800x800.jpg` | 800×800 | 1:1 | Galeria, relacionados, miniaturas de pedidos e da tabela admin | `object-cover`, `lazy` |
| `categories/<conceito>-category-900x600.jpg` | 900×600 | 3:2 | Cards de categoria da home | Gradiente escuro inferior + texto em HTML |
| `banners/banner-collectibles-1600x600.jpg`, `banner-articulated-raptor-1600x600.jpg` | 1600×600 | 8:3 | Banners "Coleções" da home | `aspect-[16/7]`, gradiente lateral |
| `banners/banner-custom-painting-1600x600.jpg` | 1600×600 | 8:3 | Seção "Personalizados" da home | Gradiente da superfície do tema |

Conceitos: `blue-mechanical-dragon`, `articulated-raptor-toy`, `sci-fi-knight`, `mechanical-panther`, `fantasy-warrior`, `unpainted-miniature-workbench`.

Regras aplicadas em todas as imagens: `width`/`height` declarados (sem layout shift), `alt` descritivo (decorativas com `alt=""`), `loading="lazy"` abaixo da dobra, texto sempre em HTML/CSS (nunca gravado na imagem), legibilidade conferida nos temas claro e escuro, desktop e celular.

## Como o código resolve as imagens

- `frontend/src/lib/images.ts` — caminhos/dimensões de hero, banners, categorias e variantes de produto (`card`, `square`, `detail`).
- `frontend/src/components/ui/ProductMedia.tsx` — `<img>` com dimensões e lazy loading; sem imagem, mostra o placeholder identificado "Foto pendente".
- Dados de demonstração (`frontend/src/data/demo.ts`) apontam cada produto/categoria para um conceito. Esses dados só existem em desenvolvimento e aparecem com o selo **DEMO**.

## Como substituir por fotos reais

1. **Produtos:** a partir da Fase 04, as fotos serão enviadas pelo painel (upload validado: JPG/PNG/WebP, até 5 MB) e servidas pelo storage (local em dev, S3 em produção). A API devolverá as URLs; `ProductMedia` passará a usá-las no lugar de `lib/images.ts`.
2. **Hero, banners e categorias:** troque os arquivos em `frontend/public/images/` mantendo nome e proporção (ou atualize `lib/images.ts`). Recomenda-se WebP/AVIF com largura máxima de 1920 px para o hero e ~1600 px para banners, com o assunto à direita (o texto ocupa a esquerda).
3. Rode `npm run build` e confira o tamanho total; hoje as imagens somam ~5,2 MB no diretório público, mas cada página carrega só as que usa.

## Checklist antes do lançamento

- [ ] Substituir todas as imagens de `public/images/` por fotos reais dos produtos vendidos ou por arte com direitos confirmados.
- [ ] Remover/atualizar os conceitos de demonstração do hero e dos banners.
- [ ] Confirmar direitos de qualquer imagem de `references-user-supplied` antes de qualquer uso público.
- [ ] Converter para WebP/AVIF e revisar pesos (Fase 11 — performance).
