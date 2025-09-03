import { ReactNode } from 'react'
import { CategoriesProvider } from '@/contexts/categories-context'
import Navbar from '@/components/layout/navbar'

interface ProtectedLayoutProps {
  children: ReactNode
}

export function ProtectedLayout({ children }: ProtectedLayoutProps) {
  return (
    <CategoriesProvider>
      <div>
        <Navbar />
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {children}
        </main>
      </div>
    </CategoriesProvider>
  )
}