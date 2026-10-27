import { ShoppingCart } from 'lucide-react'
import { Link } from 'react-router'
import { buttonClass } from '../../components/ui/button'
import { EmptyState } from '../../components/ui/EmptyState'

export function CartPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold">Carrinho</h1>
      <div className="mt-8">
        <EmptyState
          icon={ShoppingCart}
          title="Seu carrinho está vazio"
          description="O carrinho e o checkout (PIX e cartão) serão habilitados na Fase 06."
          action={
            <Link to="/produtos" className={buttonClass('primary')}>
              Ver produtos
            </Link>
          }
        />
      </div>
    </div>
  )
}
