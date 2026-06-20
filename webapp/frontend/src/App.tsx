import { useEffect, useState } from 'react'
import { Plus, Package, Settings, ShoppingBag } from 'lucide-react'
import { api } from './api'
import { setup } from './telegram'
import AddProduct from './pages/AddProduct'
import MyProducts from './pages/MyProducts'
import AdminPanel from './pages/AdminPanel'
import OrdersPage from './pages/OrdersPage'
import type { User, Page } from './types'

export default function App() {
  const [user, setUser]       = useState<User | null>(null)
  const [page, setPage]       = useState<Page>('add')
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setup()
    api.me()
      .then(u  => { setUser(u); setLoading(false) })
      .catch(e => { setError(e.message); setLoading(false) })
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-dvh" style={{ background: 'var(--bg)' }}>
        <div className="flex flex-col items-center gap-4">
          <div
            className="w-10 h-10 rounded-full border-2 border-t-transparent animate-spin"
            style={{ borderColor: 'var(--accent)', borderTopColor: 'transparent' }}
          />
          <span className="text-sm" style={{ color: 'var(--fg-muted)' }}>Yuklanmoqda…</span>
        </div>
      </div>
    )
  }

  if (error || !user) {
    return (
      <div className="flex items-center justify-center min-h-dvh p-6" style={{ background: 'var(--bg)' }}>
        <div className="glass p-8 text-center max-w-xs w-full">
          <div
            className="w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-4"
            style={{ background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.2)' }}
          >
            <Settings size={24} style={{ color: 'var(--error)' }} />
          </div>
          <p className="font-semibold text-base mb-1" style={{ color: 'var(--fg)' }}>Kirish taqiqlangan</p>
          <p className="text-sm leading-relaxed" style={{ color: 'var(--fg-muted)' }}>
            {error || 'Superadmin sizni tizimga qo\'shishi kerak'}
          </p>
        </div>
      </div>
    )
  }

  const navItems: { id: Page; label: string; icon: typeof Plus }[] = [
    { id: 'add',    label: "Qo'shish",   icon: Plus        },
    { id: 'list',   label: 'Tovarlar',   icon: Package     },
    { id: 'orders', label: 'Buyurtmalar',icon: ShoppingBag },
    ...(user.is_superadmin ? [{ id: 'admins' as Page, label: 'Adminlar', icon: Settings }] : []),
  ]

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: 'var(--bg)' }}>
      {/* Subtle top gradient blob */}
      <div
        className="fixed top-0 left-1/2 -translate-x-1/2 w-64 h-64 rounded-full pointer-events-none"
        style={{
          background: 'radial-gradient(circle, rgba(99,102,241,0.08) 0%, transparent 70%)',
          filter: 'blur(40px)',
          zIndex: 0,
        }}
      />

      {/* Content */}
      <main className="flex-1 overflow-y-auto pb-24 relative z-10">
        {page === 'add'    && <AddProduct user={user} />}
        {page === 'list'   && <MyProducts user={user} />}
        {page === 'orders' && <OrdersPage />}
        {page === 'admins' && user.is_superadmin && <AdminPanel />}
      </main>

      {/* Bottom navigation */}
      <nav
        className="fixed bottom-0 left-0 right-0 safe-bottom z-50"
        style={{
          background: 'rgba(9,9,11,0.85)',
          backdropFilter: 'blur(20px)',
          borderTop: '1px solid var(--border)',
        }}
      >
        <div className="flex">
          {navItems.map(({ id, label, icon: Icon }) => {
            const active = page === id
            return (
              <button
                key={id}
                onClick={() => setPage(id)}
                className="flex-1 flex flex-col items-center justify-center py-3 gap-1 cursor-pointer transition-all duration-200"
                style={{
                  color: active ? 'var(--accent-hover)' : 'var(--fg-muted)',
                  minHeight: 60,
                }}
              >
                <div
                  className="relative flex items-center justify-center w-8 h-8 rounded-xl transition-all duration-200"
                  style={{
                    background: active ? 'rgba(99,102,241,0.15)' : 'transparent',
                  }}
                >
                  <Icon size={18} strokeWidth={active ? 2.5 : 2} />
                </div>
                <span className="text-xs font-medium" style={{ fontSize: 11 }}>{label}</span>
              </button>
            )
          })}
        </div>
      </nav>
    </div>
  )
}
