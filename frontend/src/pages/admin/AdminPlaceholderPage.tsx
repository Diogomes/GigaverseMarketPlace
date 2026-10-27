import { Construction } from 'lucide-react'
import { EmptyState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'

export function AdminPlaceholderPage({ title, phase }: { title: string; phase: string }) {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={title} />
      <EmptyState
        icon={Construction}
        title="Em desenvolvimento"
        description={`Esta seção será entregue na ${phase}.`}
      />
    </div>
  )
}
