import { lazy, Suspense, type ComponentType } from 'react'
import { Outlet, Route, Routes } from 'react-router'
import { PreviewGuard } from './components/PreviewGuard'
import { SiteFooter } from './components/SiteFooter'
import { SiteHeader } from './components/SiteHeader'
import { HomePage } from './pages/HomePage'
import { NotFoundPage, PlaceholderPage } from './pages/PlaceholderPage'
import { CartPage } from './pages/store/CartPage'
import { CatalogPage } from './pages/store/CatalogPage'
import { CustomPage } from './pages/store/CustomPage'
import { ProductPage } from './pages/store/ProductPage'

/** Carrega sob demanda áreas que a maioria dos visitantes da loja não acessa. */
function page<K extends string, P extends object>(loader: () => Promise<Record<K, ComponentType<P>>>, name: K) {
  return lazy(() => loader().then((m) => ({ default: m[name] })))
}

const accountPages = () => import('./pages/account/AccountPages')
const AccountLayout = page(() => import('./pages/account/AccountLayout'), 'AccountLayout')
const AccountOverviewPage = page(accountPages, 'AccountOverviewPage')
const AccountOrdersPage = page(accountPages, 'AccountOrdersPage')
const AccountOrderDetailPage = page(accountPages, 'AccountOrderDetailPage')
const AccountAddressesPage = page(accountPages, 'AccountAddressesPage')
const AccountProfilePage = page(accountPages, 'AccountProfilePage')
const AccountSupportPage = page(accountPages, 'AccountSupportPage')
const LoginPage = page(() => import('./pages/auth/LoginPage'), 'LoginPage')
const RegisterPage = page(() => import('./pages/auth/RegisterPage'), 'RegisterPage')
const ForgotPasswordPage = page(() => import('./pages/auth/ForgotPasswordPage'), 'ForgotPasswordPage')
const AdminLayout = page(() => import('./pages/admin/AdminLayout'), 'AdminLayout')
const AdminPlaceholderPage = page(() => import('./pages/admin/AdminPlaceholderPage'), 'AdminPlaceholderPage')
const DashboardPage = page(() => import('./pages/admin/DashboardPage'), 'DashboardPage')
const ProductsPage = page(() => import('./pages/admin/ProductsPage'), 'ProductsPage')
const ProductFormPage = page(() => import('./pages/admin/ProductFormPage'), 'ProductFormPage')
const CustomersPage = page(() => import('./pages/admin/CustomersPage'), 'CustomersPage')
const CustomerDetailPage = page(() => import('./pages/admin/CustomerDetailPage'), 'CustomerDetailPage')

function PageLoading() {
  return (
    <p role="status" className="px-4 py-24 text-center text-sm text-fg-muted">
      Carregando…
    </p>
  )
}

function StoreLayout() {
  return (
    <div className="flex min-h-dvh flex-col">
      <a
        href="#conteudo"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-on-primary"
      >
        Pular para o conteúdo
      </a>
      <SiteHeader />
      <main id="conteudo" className="flex-1">
        <Suspense fallback={<PageLoading />}>
          <Outlet />
        </Suspense>
      </main>
      <SiteFooter />
    </div>
  )
}

export function App() {
  return (
    <Suspense fallback={<PageLoading />}>
      <Routes>
        <Route element={<StoreLayout />}>
          <Route index element={<HomePage />} />
          <Route path="produtos" element={<CatalogPage />} />
          <Route path="produtos/:slug" element={<ProductPage />} />
          <Route path="colecoes" element={<PlaceholderPage title="Coleções" phase="Fase 07" />} />
          <Route path="personalizados" element={<CustomPage />} />
          <Route path="sobre" element={<PlaceholderPage title="Sobre" phase="Fase 07" />} />
          <Route path="contato" element={<PlaceholderPage title="Contato" phase="Fase 07" />} />
          <Route path="carrinho" element={<CartPage />} />
          <Route path="entrar" element={<LoginPage />} />
          <Route path="cadastro" element={<RegisterPage />} />
          <Route path="recuperar-senha" element={<ForgotPasswordPage />} />
          <Route
            path="conta"
            element={
              <PreviewGuard>
                <AccountLayout />
              </PreviewGuard>
            }
          >
            <Route index element={<AccountOverviewPage />} />
            <Route path="pedidos" element={<AccountOrdersPage />} />
            <Route path="pedidos/:number" element={<AccountOrderDetailPage />} />
            <Route path="enderecos" element={<AccountAddressesPage />} />
            <Route path="dados" element={<AccountProfilePage />} />
            <Route path="suporte" element={<AccountSupportPage />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>

        <Route
          path="admin"
          element={
            <PreviewGuard>
              <AdminLayout />
            </PreviewGuard>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="produtos" element={<ProductsPage />} />
          <Route path="produtos/novo" element={<ProductFormPage />} />
          <Route path="produtos/:id" element={<ProductFormPage />} />
          <Route path="clientes" element={<CustomersPage />} />
          <Route path="clientes/:id" element={<CustomerDetailPage />} />
          <Route path="pedidos" element={<AdminPlaceholderPage title="Pedidos" phase="Fase 09" />} />
          <Route path="estoque" element={<AdminPlaceholderPage title="Estoque" phase="Fase 09" />} />
          <Route path="categorias" element={<AdminPlaceholderPage title="Categorias" phase="Fase 09" />} />
          <Route path="conteudo" element={<AdminPlaceholderPage title="Conteúdo" phase="Fase 09" />} />
          <Route path="relatorios" element={<AdminPlaceholderPage title="Relatórios" phase="Fase 09" />} />
          <Route path="configuracoes" element={<AdminPlaceholderPage title="Configurações" phase="Fase 09" />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  )
}
