import { useEffect, useState } from 'react'
import { Plus, Package, Settings, ShoppingBag, LogOut, ShieldCheck, User as UserIcon } from 'lucide-react'
import { api } from './api'
import Login from './pages/Login'
import AddProduct from './pages/AddProduct'
import MyProducts from './pages/MyProducts'
import AdminPanel from './pages/AdminPanel'
import OrdersPage from './pages/OrdersPage'
import type { User, Page } from './types'

export default function App() {
  const [user, setUser]       = useState<User | null>(null)
  const [page, setPage]       = useState<Page>('add')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.me()
      .then(u => { setUser(u); setLoading(false) })
      .catch(() => { setUser(null); setLoading(false) })
  }, [])

  const handleLogout = () => {
    api.logout()
    setUser(null)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-dvh" style={{ background: '#09090b' }}>
        <div className="flex flex-col items-center gap-4">
          <div
            className="w-10 h-10 rounded-full border-2 border-t-transparent animate-spin"
            style={{ borderColor: 'var(--accent, #6366f1)', borderTopColor: 'transparent' }}
          />
          <span className="text-sm text-zinc-400">Yuklanmoqda…</span>
        </div>
      </div>
    )
  }

  // Standalone Web Login sahifasi
  if (!user) {
    return <Login onSuccess={(u) => setUser(u)} />
  }

  const navItems: { id: Page; label: string; icon: typeof Plus }[] = [
    { id: 'add',    label: "Qo'shish",   icon: Plus        },
    { id: 'list',   label: 'Tovarlar',   icon: Package     },
    { id: 'orders', label: 'Buyurtmalar',icon: ShoppingBag },
    ...(user.is_superadmin ? [{ id: 'admins' as Page, label: 'Adminlar', icon: Settings }] : []),
  ]

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: 'var(--bg, #09090b)' }}>
      {/* Top Header Bar for Standalone Web App */}
      <header
        className="sticky top-0 z-40 px-4 py-3 flex items-center justify-between backdrop-blur-xl border-b"
        style={{
          background: 'rgba(9, 9, 11, 0.85)',
          borderColor: 'rgba(255, 255, 255, 0.08)',
        }}
      >
        <div className="flex items-center gap-3">
          <div
            className="w-9 h-9 rounded-xl flex items-center justify-center shadow-md"
            style={{ background: 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)' }}
          >
            <ShieldCheck size={20} className="text-white" />
          </div>
          <div>
            <h1 className="text-sm font-bold text-white leading-tight">Dalli Admin</h1>
            <div className="flex items-center gap-1.5 text-[11px] text-zinc-400">
              <UserIcon size={12} />
              <span>{user.name}</span>
              {user.is_superadmin && (
                <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
                  Superadmin
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2">
          <button
            onClick={handleLogout}
            title="Chiqish"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium text-red-400 bg-red-500/10 border border-red-500/20 hover:bg-red-500/20 transition-all cursor-pointer"
          >
            <LogOut size={14} />
            <span className="hidden sm:inline">Chiqish</span>
          </button>
        </div>
      </header>

      {/* Subtle top gradient blob */}
      <div
        className="fixed top-0 left-1/2 -translate-x-1/2 w-64 h-64 rounded-full pointer-events-none"
        style={{
          background: 'radial-gradient(circle, rgba(99,102,241,0.08) 0%, transparent 70%)',
          filter: 'blur(40px)',
          zIndex: 0,
        }}
      />

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto pb-24 relative z-10">
        {page === 'add'    && <AddProduct user={user} />}
        {page === 'list'   && <MyProducts user={user} />}
        {page === 'orders' && <OrdersPage />}
        {page === 'admins' && user.is_superadmin && <AdminPanel />}
      </main>

      {/* Navigation Bar */}
      <nav
        className="fixed bottom-0 left-0 right-0 safe-bottom z-50"
        style={{
          background: 'rgba(9,9,11,0.92)',
          backdropFilter: 'blur(20px)',
          borderTop: '1px solid rgba(255, 255, 255, 0.08)',
        }}
      >
        <div className="flex max-w-md mx-auto">
          {navItems.map(({ id, label, icon: Icon }) => {
            const active = page === id
            return (
              <button
                key={id}
                onClick={() => setPage(id)}
                className="flex-1 flex flex-col items-center justify-center py-2.5 gap-1 cursor-pointer transition-all duration-200"
                style={{
                  color: active ? 'var(--accent-hover, #818cf8)' : 'var(--fg-muted, #71717a)',
                  minHeight: 56,
                }}
              >
                <div
                  className="relative flex items-center justify-center w-8 h-8 rounded-xl transition-all duration-200"
                  style={{
                    background: active ? 'rgba(99,102,241,0.18)' : 'transparent',
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
