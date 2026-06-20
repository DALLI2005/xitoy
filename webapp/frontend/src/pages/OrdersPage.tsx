import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { RefreshCw, Search, ExternalLink } from 'lucide-react'
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
            background: '#18181b',
            border: '1px solid rgba(255,255,255,0.12)',
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
          padding: '4px 10px',
          fontSize: 12,
          fontWeight: 600,
          cursor: loading ? 'not-allowed' : 'pointer',
          opacity: loading ? 0.6 : 1,
          whiteSpace: 'nowrap',
          display: 'flex',
          alignItems: 'center',
          gap: 4,
        }}
      >
        {info.label}
        <span style={{ fontSize: 10, opacity: 0.7 }}>▾</span>
      </button>
      {dropdown}
    </>
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
        o.fullname.toLowerCase().includes(q)
      )
    }
    return true
  })

  const FILTERS = [
    { key: 'all', label: 'Hammasi', count: orders.length },
    ...STATUSES.map(s => ({ key: s, label: STATUS_INFO[s].label, count: counts[s] ?? 0 })),
  ]

  return (
    <div style={{ padding: '16px 12px 8px', maxWidth: 600, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <div>
          <h1 style={{ color: 'var(--fg)', fontSize: 18, fontWeight: 700, margin: 0 }}>Buyurtmalar</h1>
          {!loading && (
            <p style={{ color: 'var(--fg-muted)', fontSize: 12, margin: '2px 0 0' }}>
              Jami: {orders.length} ta
            </p>
          )}
        </div>
        <button
          onClick={loadOrders}
          disabled={loading}
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 10,
            padding: '8px 12px',
            color: loading ? 'var(--fg-muted)' : 'var(--accent)',
            cursor: loading ? 'not-allowed' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            fontSize: 13,
            fontWeight: 600,
          }}
        >
          <RefreshCw size={14} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
          Yangilash
        </button>
      </div>

      {/* Search */}
      <div style={{ position: 'relative', marginBottom: 12 }}>
        <Search
          size={14}
          style={{
            position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)',
            color: 'var(--fg-muted)',
          }}
        />
        <input
          className="field"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="ID, ism yoki telefon..."
          style={{ paddingLeft: 34, width: '100%', boxSizing: 'border-box' }}
        />
      </div>

      {/* Filter tabs */}
      <div
        style={{
          display: 'flex',
          gap: 6,
          overflowX: 'auto',
          marginBottom: 16,
          paddingBottom: 4,
          scrollbarWidth: 'none',
        }}
      >
        {FILTERS.map(f => {
          const active = filter === f.key
          const si = f.key === 'all' ? null : STATUS_INFO[f.key]
          return (
            <button
              key={f.key}
              onClick={() => setFilter(f.key)}
              style={{
                flexShrink: 0,
                padding: '5px 11px',
                borderRadius: 8,
                fontSize: 12,
                fontWeight: active ? 700 : 500,
                cursor: 'pointer',
                border: `1px solid ${active ? 'var(--accent)' : 'var(--border)'}`,
                background: active
                  ? 'rgba(99,102,241,0.15)'
                  : 'var(--surface)',
                color: active
                  ? 'var(--accent-hover)'
                  : si
                    ? si.color
                    : 'var(--fg-muted)',
                display: 'flex',
                alignItems: 'center',
                gap: 5,
              }}
            >
              {f.label}
              {f.count > 0 && (
                <span
                  style={{
                    background: active ? 'rgba(99,102,241,0.3)' : 'var(--border)',
                    borderRadius: 10,
                    padding: '1px 6px',
                    fontSize: 10,
                    fontWeight: 700,
                    color: active ? 'var(--accent-hover)' : 'var(--fg-muted)',
                  }}
                >
                  {f.count}
                </span>
              )}
            </button>
          )
        })}
      </div>

      {/* States */}
      {loading && (
        <div style={{ textAlign: 'center', padding: '48px 0' }}>
          <div
            style={{
              width: 32, height: 32, borderRadius: '50%',
              border: '2px solid var(--border)',
              borderTopColor: 'var(--accent)',
              animation: 'spin 0.8s linear infinite',
              margin: '0 auto 12px',
            }}
          />
          <p style={{ color: 'var(--fg-muted)', fontSize: 13 }}>Yuklanmoqda…</p>
        </div>
      )}

      {!loading && error && (
        <div className="glass" style={{ padding: 20, textAlign: 'center', borderColor: 'rgba(239,68,68,0.2)' }}>
          <p style={{ color: '#ef4444', fontSize: 13, marginBottom: 12 }}>{error}</p>
          <button
            onClick={loadOrders}
            style={{
              background: 'rgba(99,102,241,0.15)', color: 'var(--accent-hover)',
              border: '1px solid var(--accent)', borderRadius: 8,
              padding: '8px 16px', fontSize: 13, cursor: 'pointer',
            }}
          >
            Qayta urinish
          </button>
        </div>
      )}

      {!loading && !error && filtered.length === 0 && (
        <div style={{ textAlign: 'center', padding: '48px 0' }}>
          <p style={{ color: 'var(--fg-muted)', fontSize: 14 }}>
            {search ? 'Natija topilmadi' : "Buyurtmalar yo'q"}
          </p>
        </div>
      )}

      {/* Order cards */}
      {!loading && !error && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {filtered.map(order => (
            <div
              key={order.order_id}
              className="glass"
              style={{ padding: '14px 16px', borderRadius: 14 }}
            >
              {/* Top row */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                <div>
                  <span style={{ color: 'var(--fg)', fontWeight: 700, fontSize: 14 }}>
                    {order.order_id}
                  </span>
                  <p style={{ color: 'var(--fg-muted)', fontSize: 11, margin: '2px 0 0' }}>
                    {formatDate(order.sana)}
                  </p>
                </div>
                <StatusBadge
                  holat={order.holat}
                  orderId={order.order_id}
                  onSelect={s => changeStatus(order.order_id, s)}
                  loading={updating === order.order_id}
                />
              </div>

              {/* Details */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginBottom: 10 }}>
                <Row label="Mijoz" value={order.fullname || '—'} />
                <Row label="Tel" value={order.phone || '—'} />
                {order.location_link && order.location_link.startsWith('http') ? (
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ color: 'var(--fg-muted)', fontSize: 12 }}>Manzil</span>
                    <a
                      href={order.location_link}
                      target="_blank"
                      rel="noreferrer"
                      style={{ color: 'var(--accent-hover)', fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}
                    >
                      Xaritada ko'rish <ExternalLink size={11} />
                    </a>
                  </div>
                ) : order.location_link ? (
                  <Row label="Manzil" value={order.location_link} />
                ) : null}
              </div>

              {/* Products */}
              <div
                style={{
                  background: 'rgba(255,255,255,0.03)',
                  border: '1px solid var(--border)',
                  borderRadius: 8,
                  padding: '8px 10px',
                  marginBottom: 10,
                }}
              >
                <p style={{ color: 'var(--fg-muted)', fontSize: 11, marginBottom: 4, fontWeight: 600 }}>
                  MAHSULOTLAR
                </p>
                <p style={{ color: 'var(--fg)', fontSize: 12, lineHeight: 1.5, whiteSpace: 'pre-line' }}>
                  {order.mahsulotlar}
                </p>
              </div>

              {/* Total */}
              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <span style={{ color: 'var(--fg)', fontWeight: 700, fontSize: 14 }}>
                  {groupSom(order.jami_summa)} so'm
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div
          style={{
            position: 'fixed', bottom: 80, left: '50%', transform: 'translateX(-50%)',
            background: '#ef4444', color: '#fff',
            padding: '10px 20px', borderRadius: 10,
            fontSize: 13, fontWeight: 600,
            boxShadow: '0 4px 16px rgba(0,0,0,0.4)',
            zIndex: 999,
          }}
        >
          {toast}
        </div>
      )}

      <style>{`
        @keyframes spin { to { transform: rotate(360deg) } }
        input::-webkit-search-cancel-button { display: none }
      `}</style>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 8 }}>
      <span style={{ color: 'var(--fg-muted)', fontSize: 12, flexShrink: 0 }}>{label}</span>
      <span style={{ color: 'var(--fg)', fontSize: 12, textAlign: 'right', wordBreak: 'break-all' }}>{value}</span>
    </div>
  )
}
