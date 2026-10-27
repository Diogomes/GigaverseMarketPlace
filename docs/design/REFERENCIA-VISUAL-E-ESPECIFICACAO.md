# ESPECIFICAÇÃO VISUAL OBRIGATÓRIA — REFERÊNCIA APROVADA

A imagem `referencia-visual-gigaverse3d.png`, incluída neste pacote, é a referência principal de layout e direção visual. Antes de desenvolver as telas, abra/inspecione a imagem. Não a trate apenas como inspiração genérica: reproduza a hierarquia, a composição e os padrões visuais, adaptando-os a componentes reais e responsivos. A imagem é um mockup visual, não uma fonte de dados nem uma especificação de valores comerciais. Não copiar preços, nomes de clientes, métricas ou pedidos fictícios como dados reais.

## Linguagem visual
- Marca: Gigaverse3D.
- Direção: e-commerce premium de impressão 3D, colecionáveis e personalizados, com estética tecnológica.
- Tema escuro: fundo grafite/preto azulado, superfícies em tons escuros, bordas discretas, texto branco/cinza claro e azul elétrico para ações e destaques.
- Tema claro: fundo branco/cinza muito claro, texto azul-marinho/grafite, superfícies limpas e o mesmo azul elétrico para ações.
- Prata/metálico deve aparecer em detalhes e identidade da marca, sem prejudicar contraste.
- Azul neon/glow somente em pequenos detalhes de destaque; evitar excesso de efeitos.
- Cards com cantos moderadamente arredondados, bordas finas, espaçamento consistente, tipografia legível e ícones coerentes.
- Layout responsivo, acessível, com foco visível, labels, navegação por teclado e respeitando redução de movimento.
- Criar tokens de design compartilhados para cores, tipografia, espaçamento, sombras, raios e estados.
- Botão de tema claro/escuro no cabeçalho da loja e na barra superior administrativa. Deve funcionar em todas as páginas e persistir a escolha do usuário.
- Usar o arquivo oficial do logo quando disponível. Não recriar o logo com texto nem deformar a imagem.

## Tela 1 — Home pública
- Cabeçalho horizontal com logo à esquerda, links de navegação, busca, carrinho e botão Entrar.
- Hero amplo em duas áreas: mensagem comercial à esquerda e imagem de produto/escultura 3D de alto impacto à direita.
- Título curto com destaque azul em “3D”, texto explicativo e CTAs “Ver produtos” e “Personalizados”.
- Faixa de benefícios perto do rodapé do hero: qualidade, envio e pagamento seguro, apenas se essas promessas forem verdadeiras e configuradas.
- Chamada para Instagram com link `https://www.instagram.com/giga_verse3d/`.
- Abaixo do hero: categorias, produtos em destaque, seção de personalizados e rodapé com links institucionais.
- Não usar personagens protegidos como imagens de produto real sem que o lojista tenha os direitos necessários; usar fotos próprias ou imagens autorizadas.

## Tela 2 — Detalhe de produto
- Mesmo cabeçalho da home e breadcrumb.
- Coluna de mídia com imagem principal grande e miniaturas.
- Coluna de compra com título, avaliação apenas quando existirem avaliações reais, preço BRL, disponibilidade, descrição, opções como tamanho/acabamento quando o produto oferecer essas variantes, quantidade e botão “Adicionar ao carrinho”.
- Recomendações de produtos relacionados.
- Informações de produção, envio e cuidados em seções legíveis.
- Não mostrar avaliação, estoque, prazo ou preço inventados.

## Tela 3 — Home em tema claro
- Mesma estrutura e hierarquia da home escura, com superfícies claras, texto escuro e azul elétrico para CTAs.
- Não basta inverter cores automaticamente; conferir contraste e estados de hover/foco individualmente.
- Persistir tema selecionado e permitir alternância a qualquer momento.

## Tela 4 — Área do cliente
- Layout de conta com navegação lateral no desktop e menu adaptado para celular.
- Itens sugeridos: Minha conta, Meus pedidos, Endereços, Meus dados, Suporte e Sair. Não exibir formas de pagamento salvas a menos que exista uma integração segura e real.
- Conteúdo principal com pedidos recentes, status visual, data, valor e ação para ver detalhes.
- Status com badges consistentes, sempre baseados nos dados reais do backend.

## Tela 5 — Login/cadastro
- Formulário central ou em coluna, logo da Gigaverse3D, e-mail e senha, ações de entrar, criar conta e recuperar senha.
- Painel visual complementar com imagem de impressão 3D/produto, sem comprometer usabilidade em telas pequenas.
- Estados de validação, carregamento e erro claros.
- Não adicionar login social fictício. Exibir provedores sociais somente quando estiverem realmente implementados.

## Tela 6 — Dashboard administrativo
- Sidebar escura/neutral com logo e navegação: Dashboard, Pedidos, Produtos, Clientes, Estoque, Categorias, Promoções (se implementadas), Conteúdo, Relatórios e Configurações.
- Barra superior com título da página, usuário autenticado, seletor de tema e filtro de período.
- Cards de KPI: vendas, pedidos, ticket médio e clientes. Cada indicador deve vir da API e ter definição documentada.
- Gráfico de vendas por período e visualização de pedidos por status.
- Seção para produtos mais vendidos, pedidos que precisam de ação e alertas de estoque baixo.
- Não preencher o dashboard com números estáticos em produção. Usar estados vazios e dados de demonstração apenas em ambiente de desenvolvimento claramente identificado.

## Tela 7 — Gestão de produtos
- Título e descrição da seção, botão destacado “Novo produto”.
- Busca, filtro por categoria e status.
- Tabela responsiva com miniatura, nome/SKU, categoria, preço, estoque, status e ações.
- Formulário de produto com nome, descrição, SKU, categoria, preço, preço promocional opcional, imagens, tipo de produto, estoque, prazo de produção e status.
- Confirmar ações destrutivas; preferir arquivar em vez de apagar quando houver pedidos históricos.

## Tela 8 — CRM de clientes
- Título “Clientes”, busca e filtros.
- Tabela com nome, contato necessário, quantidade de pedidos, total gasto, data da última compra e status, com acesso detalhado restrito a funcionários autorizados.
- Perfil de cliente com dados essenciais, histórico de pedidos, notas internas e etiquetas.
- Notas internas nunca devem ser visíveis ao cliente.
- Não inferir características sensíveis nem coletar dados desnecessários.
- Exportação, quando existir, deve exigir autorização e registrar auditoria.

## Regras de implementação visual
1. Inspecione a referência antes de implementar.
2. Construa componentes reutilizáveis; não faça cada página com estilos isolados.
3. Não implemente a imagem inteira como um fundo estático. As telas devem ser HTML/CSS/componentes reais, responsivos e funcionais.
4. Não use a imagem do mockup como substituto de produtos reais. Use placeholders claramente identificados até o lojista fornecer imagens.
5. Criar e validar cada tela em tema claro e escuro.
6. Validar visualmente em desktop e mobile, corrigindo overflow, espaçamento e contraste.
7. Não declarar fidelidade visual sem executar a aplicação e inspecionar as telas.
