import { useEffect, useState } from 'react'
import { Users, Search, Trash2, Loader2, AlertCircle, Phone } from 'lucide-react'
import { api } from '../api'
import { hapticSuccess, hapticError } from '../telegram'
import type { AppUser } from '../types'

export default function AppUsers() {
  const [users, setUsers]     = useState<AppUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')
  const [search, setSearch]   = useState('')

  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const list = await api.appUsers()
      setUsers(list)
    } catch (e: any) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  async function confirmDelete() {
    if (deleteId == null) return
    setDeleting(true)
    try {
      await api.deleteAppUser(deleteId)
      setUsers(prev => prev.filter(u => u.user_id !== deleteId))
      hapticSuccess()
      setDeleteId(null)
    } catch (e: any) {
      hapticError()
      setError(e.message)
      setDeleteId(null)
    } finally {
      setDeleting(false)
    }
  }

  const fmtDate = (ts: number) => {
    if (!ts) return '—'
    return new Date(ts * 1000).toLocaleDateString('uz-UZ', { day: '2-digit', month: '2-digit', year: 'numeric' })
  }

  const filtered = users.filter(u => {
    if (!search) return true
    const q = search.toLowerCase()
    return u.fullname.toLowerCase().includes(q) || u.phone.toLowerCase().includes(q)
  })

  if (loading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Loader2 size={20} style={{ color: 'var(--accent)' }} className="animate-spin" />
      </div>
    )
  }

  return (
    <div className="px-4 pt-5 pb-6">
      <div className="flex items-center justify-between mb-5">
        <h1 className="text-lg font-semibold" style={{ color: 'var(--fg)' }}>Mijozlar</h1>
        {!error && (
          <span
            className="text-xs px-2.5 py-1 rounded-full font-semibold"
            style={{ background: 'rgba(99,102,241,0.12)', color: 'var(--accent-hover)' }}
          >
            {filtered.length} ta
          </span>
        )}
      </div>

      {error && (
        <div className="glass p-3 mb-4 flex items-center gap-2" style={{ borderColor: 'rgba(239,68,68,0.3)' }}>
          <AlertCircle size={15} style={{ color: 'var(--error)', flexShrink: 0 }} />
          <span className="text-sm" style={{ color: 'var(--error)' }}>{error}</span>
        </div>
      )}

      {/* Qidiruv */}
      <div className="relative mb-5">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2" style={{ color: 'var(--fg-muted)' }} />
        <input
          type="text"
          placeholder="Ism yoki telefon bo'yicha qidirish..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="field w-full pl-9"
        />
      </div>

      {/* Bo'sh holat */}
      {filtered.length === 0 && (
        <div className="glass p-6 text-center">
          <div
            className="w-12 h-12 rounded-2xl flex items-center justify-center mx-auto mb-3"
            style={{ background: 'rgba(99,102,241,0.1)' }}
          >
            <Users size={20} style={{ color: 'var(--accent)' }} />
          </div>
          <p className="text-sm font-medium" style={{ color: 'var(--fg)' }}>
            {search ? 'Hech narsa topilmadi' : "Hali mijozlar yo'q"}
          </p>
        </div>
      )}

      {/* Mijozlar ro'yxati */}
      <div className="flex flex-col gap-3">
        {filtered.map(u => (
          <div key={u.user_id} className="glass p-4 fade-up flex items-start justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div
                className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0"
                style={{ background: 'rgba(99,102,241,0.12)' }}
              >
                <Users size={16} style={{ color: 'var(--accent)' }} />
              </div>
              <div className="min-w-0">
                <p className="font-semibold text-sm truncate" style={{ color: 'var(--fg)' }}>{u.fullname}</p>
                <p className="text-xs mt-0.5 flex items-center gap-1" style={{ color: 'var(--fg-muted)' }}>
                  <Phone size={11} /> {u.phone}
                </p>
                <p className="text-xs mt-0.5" style={{ color: 'var(--fg-muted)', opacity: 0.7 }}>
                  Ro'yxatdan o'tgan: {fmtDate(u.created_at)}
                </p>
              </div>
            </div>
            <button
              onClick={() => setDeleteId(u.user_id)}
              className="w-8 h-8 rounded-xl flex items-center justify-center cursor-pointer transition-all active:scale-90 flex-shrink-0"
              style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.15)', color: '#f87171' }}
              aria-label="O'chirish"
            >
              <Trash2 size={13} />
            </button>
          </div>
        ))}
      </div>

      {/* O'chirishni tasdiqlash modali */}
      {deleteId != null && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center px-4"
          style={{ background: 'rgba(0,0,0,0.7)' }}
          onClick={() => !deleting && setDeleteId(null)}
        >
          <div
            className="w-full max-w-sm rounded-2xl p-6 flex flex-col gap-4 shadow-2xl"
            style={{ background: 'var(--surface)', border: '1px solid var(--border)' }}
            onClick={e => e.stopPropagation()}
          >
            <div
              className="w-12 h-12 rounded-full flex items-center justify-center mx-auto"
              style={{ background: 'rgba(239,68,68,0.1)', color: '#f87171' }}
            >
              <Trash2 size={24} />
            </div>
            <h3 className="font-bold text-center text-lg" style={{ color: 'var(--fg)' }}>Mijozni o'chirish</h3>
            <p className="text-sm text-center px-2" style={{ color: 'var(--fg-muted)' }}>
              Rostdan ham ushbu mijoz akkauntini o'chirib tashlamoqchimisiz? Bu amalni ortga qaytarib bo'lmaydi.
            </p>
            <div className="flex gap-3 mt-2">
              <button
                onClick={() => setDeleteId(null)}
                disabled={deleting}
                className="flex-1 py-2.5 rounded-xl text-sm font-bold cursor-pointer transition-colors hover:bg-white/5"
                style={{ background: 'var(--bg)', color: 'var(--fg)', border: '1px solid var(--border)' }}
              >
                Bekor qilish
              </button>
              <button
                onClick={confirmDelete}
                disabled={deleting}
                className="flex-1 py-2.5 rounded-xl text-sm font-bold cursor-pointer transition-opacity hover:opacity-80 flex justify-center"
                style={{ background: '#ef4444', color: '#fff' }}
              >
                {deleting ? <Loader2 size={18} className="animate-spin" /> : "O'chirish"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
