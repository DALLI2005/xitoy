import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { RefreshCw, Search, ExternalLink, Package } from 'lucide-react'
import { api } from '../api'
import type { Order } from '../types'

const STATUSES = [
  'Yangi',
  'Tolov_kutilmoqda',
  'Tasdiqlandi',
  "Yo'lda",
  'Yetkazildi',
  'Rad_etildi',
] as const

const STATUS_INFO: Record<string, { label: string; color: string; bg: string }> = {
  'Yangi':            { label: 'Yangi',             color: '#a1a1aa', bg: 'rgba(113,113,122,0.15)' },
  'Tolov_kutilmoqda': { label: "To'lov kutilmoqda", color: '#eab308', bg: 'rgba(234,179,8,0.15)'   },
  'Tasdiqlandi':      { label: 'Tasdiqlandi',        color: '#818cf8', bg: 'rgba(99,102,241,0.15)'  },
  "Yo'lda":           { label: "Yo'lda",             color: '#f59e0b', bg: 'rgba(245,158,11,0.15)'  },
  'Yetkazildi':       { label: 'Yetkazildi',         color: '#22c55e', bg: 'rgba(34,197,94,0.15)'   },
  'Rad_etildi':       { label: 'Rad etildi',         color: '#ef4444', bg: 'rgba(239,68,68,0.15)'   },
}

const EN_MON = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
const UZ_MON = ['yanvar','fevral','mart','aprel','may','iyun','iyul','avgust','sentabr','oktabr','noyabr','dekabr']

function formatDate(raw: string): string {
  try {
    if (raw.includes('GMT')) {
      const parts = raw.split(' GMT')[0].trim().split(/\s+/)
      const mi = EN_MON.indexOf(parts[1])
      if (mi === -1) return raw
      return `${parts[2].replace(/^0/, '')}-${UZ_MON[mi]}, ${parts[4].slice(0, 5)}`
    }
    if (raw.includes('-') && raw.includes(':')) {
      const [d, t] = raw.trim().split(' ')
      const [, m, day] = d.split('-')
      return `${parseInt(day)}-${UZ_MON[parseInt(m) - 1]}, ${t.slice(0, 5)}`
    }
    return raw
  } catch { return raw }
}

function groupSom(n: number): string {
  return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
}

function StatusBadge({
  holat,
  orderId,
  onSelect,
  loading,
}: {
  holat: string
  orderId: string
  onSelect: (status: string) => void
  loading: boolean
}) {
  const [open, setOpen] = useState(false)
  const [pos, setPos] = useState({ top: 0, right: 0 })
  const btnRef = useRef<HTMLButtonElement>(null)
  const info = STATUS_INFO[holat] ?? STATUS_INFO['Yangi']

  useEffect(() => {
    if (!open) return
    function close(e: MouseEvent) {
      const target = e.target as Node
      if (btnRef.current && !btnRef.current.contains(target)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])

  function handleOpen() {
    if (loading) return
    if (btnRef.current) {
      const r = btnRef.current.getBoundingClientRect()
      setPos({ top: r.bottom + 6, right: window.innerWidth - r.right })
    }
    setOpen(o => !o)
  }

  const dropdown = open
    ? createPortal(
        <div
          style={{
            position: 'fixed',
            top: pos.top,
            right: pos.right,
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 12,
            overflow: 'hidden',
            zIndex: 9999,
            minWidth: 180,
            boxShadow: '0 12px 40px rgba(0,0,0,0.7)',
          }}
        >
          {STATUSES.map(s => {
            const si = STATUS_INFO[s]
            return (
              <button
                key={s}
                onMouseDown={e => { e.preventDefault(); onSelect(s); setOpen(false) }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  width: '100%',
                  padding: '10px 16px',
                  background: s === holat ? 'rgba(99,102,241,0.12)' : 'transparent',
                  color: si.color,
                  fontSize: 13,
                  fontWeight: s === holat ? 700 : 500,
                  cursor: 'pointer',
                  border: 'none',
                  textAlign: 'left',
                }}
                className="hover:bg-white/5 transition-colors"
              >
                <span
                  style={{
                    width: 8, height: 8, borderRadius: '50%',
                    background: si.color, flexShrink: 0,
                  }}
                />
                {si.label}
              </button>
            )
          })}
        </div>,
        document.body
      )
    : null

  return (
    <>
      <button
        ref={btnRef}
        onClick={handleOpen}
        style={{
          background: info.bg,
          color: info.color,
          border: `1px solid ${info.color}55`,
          borderRadius: 8,
          padding: '6px 12px',
          fontSize: 12,
          fontWeight: 600,
          cursor: loading ? 'not-allowed' : 'pointer',
          opacity: loading ? 0.6 : 1,
          whiteSpace: 'nowrap',
          display: 'flex',
          alignItems: 'center',
          gap: 6,
        }}
        className="transition-transform active:scale-95"
      >
        {loading ? <RefreshCw size={14} className="animate-spin" /> : (
          <span style={{ width: 8, height: 8, borderRadius: '50%', background: info.color }} />
        )}
        {info.label}
        <span style={{ fontSize: 10, opacity: 0.7, marginLeft: 4 }}>▾</span>
      </button>
      {dropdown}
    </>
  )
}

function SkeletonRow() {
  return (
    <tr className="border-b" style={{ borderColor: 'var(--border)' }}>
      <td className="p-4"><div className="skeleton h-5 w-24 rounded mb-2" /><div className="skeleton h-4 w-16 rounded" /></td>
      <td className="p-4"><div className="skeleton h-5 w-32 rounded mb-2" /><div className="skeleton h-4 w-24 rounded" /></td>
      <td className="p-4"><div className="skeleton h-10 w-full rounded-lg" /></td>
      <td className="p-4"><div className="skeleton h-6 w-24 rounded font-bold" /></td>
      <td className="p-4"><div className="skeleton h-8 w-28 rounded-lg" /></td>
    </tr>
  )
}

export default function OrdersPage() {
  const [orders, setOrders]       = useState<Order[]>([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState('')
  const [filter, setFilter]       = useState<string>('all')
  const [search, setSearch]       = useState('')
  const [updating, setUpdating]   = useState<string | null>(null)
  const [toast, setToast]         = useState('')

  useEffect(() => { loadOrders() }, [])

  async function loadOrders() {
    setLoading(true)
    setError('')
    try {
      const data = await api.adminOrders()
      setOrders(data.orders)
    } catch (e: any) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  async function changeStatus(orderId: string, newStatus: string) {
    const prev = orders.find(o => o.order_id === orderId)?.holat ?? ''
    setOrders(prev => prev.map(o => o.order_id === orderId ? { ...o, holat: newStatus } : o))
    setUpdating(orderId)
    try {
      await api.updateOrderStatus(orderId, newStatus)
    } catch (e: any) {
      setOrders(list => list.map(o => o.order_id === orderId ? { ...o, holat: prev } : o))
      showToast(e.message || 'Xatolik yuz berdi')
    } finally {
      setUpdating(null)
    }
  }

  function showToast(msg: string) {
    setToast(msg)
    setTimeout(() => setToast(''), 3000)
  }

  const counts = orders.reduce<Record<string, number>>((acc, o) => {
    acc[o.holat] = (acc[o.holat] ?? 0) + 1
    return acc
  }, {})

  const filtered = orders.filter(o => {
    if (filter !== 'all' && o.holat !== filter) return false
    if (search) {
      const q = search.toLowerCase()
      return (
        o.order_id.toLowerCase().includes(q) ||
        o.phone.includes(q) ||
        (o.fullname && o.fullname.toLowerCase().includes(q))
      )
    }
    return true
  })

  const FILTERS = [
    { key: 'all', label: 'Hammasi', count: orders.length },
    ...STATUSES.map(s => ({ key: s, label: STATUS_INFO[s].label, count: counts[s] ?? 0 })),
  ]

  return (
    <div className="px-4 md:px-8 pt-6 pb-12 w-full max-w-[1400px] mx-auto">
      {/* Header */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold" style={{ color: 'var(--fg)' }}>Buyurtmalar</h1>
          {!loading && !error && (
            <p className="text-sm mt-1" style={{ color: 'var(--fg-muted)' }}>
              Jami {orders.length} ta buyurtma
            </p>
          )}
        </div>
        <div className="flex gap-3 w-full md:w-auto">
          <div className="relative flex-1 md:w-64">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
            <input 
              type="text" 
              placeholder="ID, ism yoki telefon..." 
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="w-full pl-9 pr-4 py-2 rounded-xl text-sm"
              style={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--fg)' }}
            />
          </div>
          <button
            onClick={loadOrders}
            disabled={loading}
            className="w-10 h-10 rounded-xl flex items-center justify-center cursor-pointer transition-all hover:brightness-110 active:scale-95 flex-shrink-0"
            style={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--fg)' }}
            aria-label="Yangilash"
          >
            <RefreshCw size={18} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Filter tabs */}
      {!loading && !error && (
        <div className="flex gap-2 mb-6 overflow-x-auto pb-2 scrollbar-hide">
          {FILTERS.map(f => {
            const active = filter === f.key
            const si = f.key === 'all' ? null : STATUS_INFO[f.key]
            return (
              <button
                key={f.key}
                onClick={() => setFilter(f.key)}
                className="flex-shrink-0 text-sm font-semibold px-4 py-2 rounded-xl cursor-pointer transition-all duration-200 flex items-center gap-2"
                style={{
                  background: active ? 'rgba(99,102,241,0.15)' : 'var(--surface)',
                  color: active ? '#818cf8' : si ? si.color : 'var(--fg-muted)',
                  border: `1px solid ${active ? 'rgba(99,102,241,0.3)' : 'var(--border)'}`,
                }}
              >
                {f.label}
                {f.count > 0 && (
                  <span
                    className="px-2 py-0.5 rounded-full text-xs font-bold"
                    style={{
                      background: active ? 'rgba(99,102,241,0.3)' : 'var(--border)',
                      color: active ? '#818cf8' : 'var(--fg-muted)',
                    }}
                  >
                    {f.count}
                  </span>
                )}
              </button>
            )
          })}
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="p-4 rounded-xl mb-6 flex items-center gap-3" style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.2)' }}>
          <span className="text-sm text-red-400 font-medium">{error}</span>
          <button onClick={loadOrders} className="ml-auto text-sm text-red-400 font-bold hover:underline">Qayta urinish</button>
        </div>
      )}

      {/* Empty */}
      {!loading && !error && filtered.length === 0 && (
        <div className="glass flex flex-col items-center justify-center py-20 text-center rounded-2xl">
          <div className="w-16 h-16 rounded-2xl flex items-center justify-center mb-4" style={{ background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.2)' }}>
            <Package size={28} style={{ color: 'var(--accent)' }} />
          </div>
          <p className="text-lg font-semibold" style={{ color: 'var(--fg)' }}>{search ? "Natija topilmadi" : "Buyurtmalar yo'q"}</p>
        </div>
      )}

      {/* Table */}
      {(loading || (!error && filtered.length > 0)) && (
        <div className="w-full overflow-x-auto rounded-2xl border" style={{ borderColor: 'var(--border)', background: 'var(--surface)' }}>
          <table className="w-full text-left text-sm whitespace-nowrap" style={{ color: 'var(--fg)' }}>
            <thead>
              <tr className="border-b bg-black/20" style={{ borderColor: 'var(--border)' }}>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>ID & Sana</th>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Mijoz & Manzil</th>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Mahsulotlar</th>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider text-right" style={{ color: 'var(--fg-muted)' }}>Jami Summa</th>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider text-center" style={{ color: 'var(--fg-muted)' }}>Holat</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <>
                  <SkeletonRow />
                  <SkeletonRow />
                  <SkeletonRow />
                  <SkeletonRow />
                </>
              ) : (
                filtered.map(order => (
                  <tr 
                    key={order.order_id} 
                    className="border-b transition-colors hover:bg-white/5" 
                    style={{ borderColor: 'var(--border)' }}
                  >
                    <td className="p-4 align-top">
                      <p className="font-bold text-sm" style={{ color: 'var(--fg)' }}>{order.order_id}</p>
                      <p className="text-xs mt-1" style={{ color: 'var(--fg-muted)' }}>{formatDate(order.sana)}</p>
                    </td>
                    <td className="p-4 align-top">
                      <p className="font-semibold text-sm" style={{ color: 'var(--fg)' }}>{order.fullname || 'Ismi yo\'q'}</p>
                      <p className="text-xs mt-1 font-mono" style={{ color: 'var(--fg-muted)' }}>{order.phone || '—'}</p>
                      {order.location_link && (
                        <div className="mt-2">
                          {order.location_link.startsWith('http') ? (
                            <a
                              href={order.location_link}
                              target="_blank"
                              rel="noreferrer"
                              className="inline-flex items-center gap-1.5 text-xs font-medium hover:underline transition-colors"
                              style={{ color: '#818cf8', background: 'rgba(99,102,241,0.1)', padding: '4px 8px', borderRadius: '6px' }}
                            >
                              Xaritada ko'rish <ExternalLink size={12} />
                            </a>
                          ) : (
                            <p className="text-xs" style={{ color: 'var(--fg-muted)', whiteSpace: 'normal', maxWidth: '200px' }}>
                              {order.location_link}
                            </p>
                          )}
                        </div>
                      )}
                    </td>
                    <td className="p-4 align-top max-w-[300px]">
                      <div className="rounded-lg p-2.5 text-xs whitespace-pre-wrap leading-relaxed" style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid var(--border)', color: 'var(--fg)' }}>
                        {order.mahsulotlar}
                      </div>
                    </td>
                    <td className="p-4 align-top text-right">
                      <span className="font-bold text-sm" style={{ color: 'var(--accent-hover)' }}>
                        {groupSom(order.jami_summa)} so'm
                      </span>
                    </td>
                    <td className="p-4 align-top text-center">
                      <div className="flex justify-center">
                        <StatusBadge
                          holat={order.holat}
                          orderId={order.order_id}
                          onSelect={s => changeStatus(order.order_id, s)}
                          loading={updating === order.order_id}
                        />
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div
          className="fixed bottom-6 left-1/2 -translate-x-1/2 px-5 py-2.5 rounded-xl text-sm font-bold shadow-2xl z-[9999]"
          style={{ background: '#ef4444', color: '#fff', boxShadow: '0 8px 32px rgba(239,68,68,0.4)' }}
        >
          {toast}
        </div>
      )}
    </div>
  )
}
