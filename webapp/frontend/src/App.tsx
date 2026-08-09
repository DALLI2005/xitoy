import { useEffect, useState } from 'react'
import { Plus, Package, Settings, ShoppingBag, LogOut, ShieldCheck, User as UserIcon, Menu, X, Users } from 'lucide-react'
import { api } from './api'
import Login from './pages/Login'
import AddProduct from './pages/AddProduct'
import MyProducts from './pages/MyProducts'
import AdminPanel from './pages/AdminPanel'
import OrdersPage from './pages/OrdersPage'
import AppUsers from './pages/AppUsers'
import type { User, Page } from './types'

export default function App() {
  const [user, setUser]       = useState<User | null>(null)
  const [page, setPage]       = useState<Page>('add')
  const [loading, setLoading] = useState(true)
  const [sidebarOpen, setSidebarOpen] = useState(false)

  useEffect(() => {
    // 1. TWA initData tekshiramiz
    const tg = (window as any).Telegram?.WebApp
    if (tg && tg.initData) {
      api.webappLogin(tg.initData)
        .then(res => {
          setUser(res.user)
          setPage('admins') // TWA dan kirganida Admin Panel ochiladi
          setLoading(false)
        })
        .catch(err => {
          console.error("TWA Login Error:", err)
          // Fallback to normal me() check
          checkMe()
        })
    } else {
      checkMe()
    }

    function checkMe() {
      api.me()
        .then(u => { setUser(u); setLoading(false) })
        .catch(() => { setUser(null); setLoading(false) })
    }
  }, [])

  const handleLogout = () => {
    api.logout()
    setUser(null)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-dvh" style={{ background: 'var(--bg)' }}>
        <div className="flex flex-col items-center gap-4">
          <div
            className="w-10 h-10 rounded-full border-2 border-t-transparent animate-spin"
            style={{ borderColor: 'var(--primary)', borderTopColor: 'transparent' }}
          />
          <span className="text-sm text-slate-500">Yuklanmoqda…</span>
        </div>
      </div>
    )
  }

  if (!user) {
    return <Login onSuccess={(u) => setUser(u)} />
  }

  const navItems: { id: Page; label: string; icon: typeof Plus }[] = [
    { id: 'list',   label: 'Mening tovarlarim',   icon: Package     },
    { id: 'add',    label: "Tovar qo'shish",   icon: Plus        },
    { id: 'orders', label: 'Buyurtmalar',icon: ShoppingBag },
    ...(user.is_superadmin ? [{ id: 'admins' as Page, label: 'Xodimlar (Adminlar)', icon: Settings }] : []),
    ...(user.is_superadmin ? [{ id: 'users' as Page, label: 'Mijozlar', icon: Users }] : []),
  ]

  return (
    <div className="flex h-dvh overflow-hidden bg-slate-50 font-sans text-slate-800">
      
      {/* Mobile Sidebar Overlay */}
      {sidebarOpen && (
        <div 
          className="fixed inset-0 bg-slate-900/50 z-40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside 
        className={`fixed lg:static inset-y-0 left-0 z-50 w-64 bg-slate-900 text-slate-300 flex flex-col transition-transform duration-300 ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}
      >
        <div className="flex items-center justify-between h-16 px-6 bg-slate-950/50 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center">
              <ShieldCheck size={18} className="text-white" />
            </div>
            <span className="text-white font-bold tracking-wide">DalliShop</span>
          </div>
          <button className="lg:hidden text-slate-400 hover:text-white" onClick={() => setSidebarOpen(false)}>
            <X size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto py-6 px-4">
          <div className="mb-6 px-2">
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">Asosiy Menu</div>
            <div className="space-y-1">
              {navItems.map(({ id, label, icon: Icon }) => (
                <button
                  key={id}
                  onClick={() => { setPage(id); setSidebarOpen(false); }}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    page === id 
                      ? 'bg-blue-600 text-white' 
                      : 'hover:bg-slate-800 hover:text-white'
                  }`}
                >
                  <Icon size={18} />
                  {label}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="p-4 border-t border-slate-800 bg-slate-900">
          <div className="flex items-center justify-between p-3 rounded-lg bg-slate-800/50">
            <div className="flex items-center gap-3 truncate">
              <div className="w-8 h-8 rounded-full bg-slate-700 flex items-center justify-center flex-shrink-0">
                <UserIcon size={16} className="text-slate-400" />
              </div>
              <div className="truncate">
                <div className="text-sm font-medium text-white truncate">{user.name}</div>
                <div className="text-xs text-slate-500">{user.is_superadmin ? 'Superadmin' : 'Sotuvchi'}</div>
              </div>
            </div>
            <button 
              onClick={handleLogout}
              className="p-1.5 text-slate-400 hover:text-red-400 hover:bg-slate-700 rounded-md transition-colors"
              title="Chiqish"
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 h-full overflow-hidden">
        
        {/* Top Header (Mobile mainly, but visible on desktop too) */}
        <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-4 lg:px-8 z-10 shrink-0">
          <div className="flex items-center gap-3">
            <button 
              className="lg:hidden p-2 -ml-2 text-slate-600 hover:bg-slate-100 rounded-lg"
              onClick={() => setSidebarOpen(true)}
            >
              <Menu size={20} />
            </button>
            <h2 className="text-lg font-semibold text-slate-800 hidden sm:block">
              {navItems.find(i => i.id === page)?.label}
            </h2>
          </div>
          <div className="flex items-center gap-4">
            <a href="http://localhost:5173" target="_blank" rel="noreferrer" className="text-sm font-medium text-blue-600 hover:underline">
              Do'konga o'tish
            </a>
          </div>
        </header>

        {/* Scrollable Main */}
        <main className="flex-1 overflow-y-auto p-4 lg:p-8 bg-slate-50">
          <div className="mx-auto max-w-5xl">
            {page === 'add'    && <AddProduct user={user} />}
            {page === 'list'   && <MyProducts user={user} />}
            {page === 'orders' && <OrdersPage />}
            {page === 'admins' && user.is_superadmin && <AdminPanel />}
            {page === 'users'  && user.is_superadmin && <AppUsers />}
          </div>
        </main>
      </div>
    </div>
  )
}
