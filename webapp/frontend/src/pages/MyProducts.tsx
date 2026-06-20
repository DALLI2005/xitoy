import { useEffect, useState } from 'react'
import { Package, RefreshCw, Pencil, Trash2, X, Check } from 'lucide-react'
import { api } from '../api'
import type { Product, User } from '../types'

interface Props { user: User }

function SkeletonCard() {
  return (
    <div className="glass p-3 flex gap-3">
      <div className="skeleton w-16 h-16 rounded-xl flex-shrink-0" />
      <div className="flex-1 flex flex-col gap-2 pt-1">
        <div className="skeleton h-4 w-3/4 rounded" />
        <div className="skeleton h-4 w-1/2 rounded" />
        <div className="flex gap-2 mt-1">
          <div className="skeleton h-5 w-16 rounded-full" />
          <div className="skeleton h-5 w-10 rounded-full" />
        </div>
      </div>
    </div>
  )
}

const CAT_COLORS: Record<string, string> = {
  'Kiyim':       'rgba(99,102,241,0.15)',
  'Elektronika': 'rgba(6,182,212,0.15)',
  'Poyabzal':    'rgba(34,197,94,0.15)',
  'Aksessuar':   'rgba(245,158,11,0.15)',
  'Sport':       'rgba(239,68,68,0.15)',
  'Uy uchun':    'rgba(168,85,247,0.15)',
  'Boshqa':      'rgba(255,255,255,0.06)',
}
const CAT_TEXT: Record<string, string> = {
  'Kiyim':       '#818cf8',
  'Elektronika': '#22d3ee',
  'Poyabzal':    '#4ade80',
  'Aksessuar':   '#fbbf24',
  'Sport':       '#f87171',
  'Uy uchun':    '#c084fc',
  'Boshqa':      '#71717a',
}

type Filter = 'all' | 'inactive' | 'outofstock'

interface EditForm {
  name: string; price: string; discount: string; category: string; description: string
}

export default function MyProducts({ user }: Props) {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState('')
  const [toggling, setToggling] = useState<string>('')  // "id:field"
  const [filter, setFilter]     = useState<Filter>('all')

  // Edit modal
  const [editProduct, setEditProduct] = useState<Product | null>(null)
  const [editForm, setEditForm]       = useState<EditForm>({ name: '', price: '', discount: '', category: '', description: '' })
  const [editSaving, setEditSaving]   = useState(false)
  const [editError, setEditError]     = useState('')

  // Delete confirm
  const [deleteId, setDeleteId]       = useState<number | string | null>(null)
  const [deleting, setDeleting]       = useState(false)

  function load() {
    setLoading(true)
    setError('')
    api.products()
      .then(setProducts)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  function openEdit(p: Product) {
    setEditProduct(p)
    setEditError('')
    setEditForm({
      name:        p.title || p.name || '',
      price:       String(p.price),
      discount:    String(p.discountPercent || p.discount || 0),
      category:    p.category || '',
      description: p.description || '',
    })
  }

  async function saveEdit() {
    if (!editProduct) return
    if (!editForm.name.trim()) { setEditError('Nom kiritilmagan'); return }
    if (!editForm.price)       { setEditError('Narx kiritilmagan'); return }
    setEditSaving(true)
    setEditError('')
    try {
      await api.updateProduct(editProduct.id!, {
        name:        editForm.name.trim(),
        price:       parseInt(editForm.price),
        discount:    parseInt(editForm.discount || '0') || 0,
        category:    editForm.category,
        description: editForm.description.trim(),
      })
      setProducts(prev => prev.map(p =>
        p.id == editProduct.id
          ? { ...p, title: editForm.name.trim(), name: editForm.name.trim(),
              price: parseInt(editForm.price),
              discountPercent: parseInt(editForm.discount || '0') || 0,
              category: editForm.category,
              description: editForm.description.trim() }
          : p
      ))
      setEditProduct(null)
    } catch (e: any) {
      setEditError(e.message)
    } finally {
      setEditSaving(false)
    }
  }

  async function confirmDelete() {
    if (deleteId == null) return
    setDeleting(true)
    try {
      await api.deleteProduct(deleteId)
      setProducts(prev => prev.filter(p => p.id != deleteId))
      setDeleteId(null)
    } catch (e: any) {
      setError(e.message)
      setDeleteId(null)
    } finally {
      setDeleting(false)
    }
  }

  async function toggleField(id: number | string, field: 'active' | 'in_stock', current: boolean) {
    const key = `${id}:${field}`
    setToggling(key)
    try {
      await api.patchProduct(id, { [field]: !current })
      setProducts(prev => prev.map(p => {
        if (p.id != id) return p
        if (field === 'active')   return { ...p, active:  !current }
        if (field === 'in_stock') return { ...p, inStock: !current }
        return p
      }))
    } catch (e: any) {
      setError(e.message)
    } finally {
      setToggling('')
    }
  }

  const fmt = (n: number) =>
    new Intl.NumberFormat('uz-UZ').format(n) + ' so\'m'

  const filtered = products.filter(p => {
    if (filter === 'inactive')   return p.active  === false
    if (filter === 'outofstock') return p.inStock === false
    return true
  })

  const filterLabels: { key: Filter; label: string; count?: number }[] = [
    { key: 'all',        label: 'Barcha',   count: products.length },
    { key: 'inactive',   label: '🔴 Nofaol', count: products.filter(p => p.active === false).length },
    { key: 'outofstock', label: '⚠️ Tugagan', count: products.filter(p => p.inStock === false).length },
  ]

  return (
    <div className="px-4 pt-5 pb-6">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-lg font-semibold" style={{ color: 'var(--fg)' }}>
            {user.is_superadmin ? 'Barcha tovarlar' : 'Tovarlarim'}
          </h1>
          {!loading && !error && (
            <p className="text-sm mt-0.5" style={{ color: 'var(--fg-muted)' }}>
              {filtered.length} ta tovar
            </p>
          )}
        </div>
        <button
          onClick={load}
          disabled={loading}
          className="w-9 h-9 rounded-xl flex items-center justify-center cursor-pointer transition-all duration-200 active:scale-90"
          style={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--fg-muted)' }}
          aria-label="Yangilash"
        >
          <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
        </button>
      </div>

      {/* Filter tabs — faqat superadmin uchun */}
      {user.is_superadmin && !loading && !error && (
        <div className="flex gap-2 mb-4 overflow-x-auto pb-1">
          {filterLabels.map(({ key, label, count }) => (
            <button
              key={key}
              onClick={() => setFilter(key)}
              className="flex-shrink-0 text-xs font-medium px-3 py-1.5 rounded-full cursor-pointer transition-all duration-150"
              style={{
                background: filter === key ? 'rgba(99,102,241,0.2)' : 'var(--surface)',
                color:      filter === key ? 'var(--accent-hover)' : 'var(--fg-muted)',
                border:     `1px solid ${filter === key ? 'rgba(99,102,241,0.35)' : 'var(--border)'}`,
              }}
            >
              {label} {count !== undefined && <span style={{ opacity: 0.7 }}>({count})</span>}
            </button>
          ))}
        </div>
      )}

      {/* Error */}
      {error && (
        <div
          className="glass p-4 mb-4 flex items-center gap-3"
          style={{ borderColor: 'rgba(239,68,68,0.3)' }}
        >
          <span className="text-sm" style={{ color: 'var(--error)' }}>{error}</span>
          <button
            onClick={load}
            className="ml-auto text-xs font-medium cursor-pointer"
            style={{ color: 'var(--accent)' }}
          >
            Qayta
          </button>
        </div>
      )}

      {/* Skeleton */}
      {loading && (
        <div className="flex flex-col gap-3">
          {[0,1,2].map(i => <SkeletonCard key={i} />)}
        </div>
      )}

      {/* Empty */}
      {!loading && !error && filtered.length === 0 && (
        <div className="glass flex flex-col items-center justify-center py-16 text-center">
          <div
            className="w-14 h-14 rounded-2xl flex items-center justify-center mb-4"
            style={{ background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.2)' }}
          >
            <Package size={24} style={{ color: 'var(--accent)' }} />
          </div>
          <p className="font-medium" style={{ color: 'var(--fg)' }}>
            {filter === 'all' ? 'Hozircha tovar yo\'q' : 'Bunday tovar topilmadi'}
          </p>
          <p className="text-sm mt-1" style={{ color: 'var(--fg-muted)' }}>
            {filter === 'all' ? 'Yangi tovar qo\'shing' : 'Filter o\'zgartiring'}
          </p>
        </div>
      )}

      {/* ── Delete confirm modal ── */}
      {deleteId != null && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center px-6"
          style={{ background: 'rgba(0,0,0,0.6)' }}
          onClick={() => !deleting && setDeleteId(null)}
        >
          <div
            className="w-full max-w-sm rounded-2xl p-6 flex flex-col gap-4"
            style={{ background: 'var(--surface)', border: '1px solid var(--border)' }}
            onClick={e => e.stopPropagation()}
          >
            <p className="font-semibold text-center text-base" style={{ color: 'var(--fg)' }}>
              Tovarni o'chirmoqchimisiz?
            </p>
            <p className="text-sm text-center" style={{ color: 'var(--fg-muted)' }}>
              Bu amalni qaytarib bo'lmaydi.
            </p>
            <div className="flex gap-3 mt-1">
              <button
                onClick={() => setDeleteId(null)}
                disabled={deleting}
                className="flex-1 py-2.5 rounded-xl text-sm font-medium cursor-pointer"
                style={{ background: 'var(--bg)', color: 'var(--fg-muted)', border: '1px solid var(--border)' }}
              >
                Bekor
              </button>
              <button
                onClick={confirmDelete}
                disabled={deleting}
                className="flex-1 py-2.5 rounded-xl text-sm font-medium cursor-pointer"
                style={{ background: 'rgba(239,68,68,0.15)', color: '#f87171', border: '1px solid rgba(239,68,68,0.3)' }}
              >
                {deleting ? '…' : 'O\'chirish'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Edit modal ── */}
      {editProduct && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center"
          style={{ background: 'rgba(0,0,0,0.55)' }}
          onClick={() => !editSaving && setEditProduct(null)}
        >
          <div
            className="w-full max-w-sm rounded-t-3xl p-5 flex flex-col gap-3 overflow-y-auto"
            style={{ background: 'var(--surface)', border: '1px solid var(--border)', maxHeight: '90vh', paddingBottom: 'calc(1.25rem + env(safe-area-inset-bottom, 72px))' }}
            onClick={e => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-1">
              <p className="font-semibold" style={{ color: 'var(--fg)' }}>Tovarni tahrirlash</p>
              <button onClick={() => setEditProduct(null)} style={{ color: 'var(--fg-muted)' }}>
                <X size={18} />
              </button>
            </div>

            {editError && (
              <p className="text-xs px-3 py-2 rounded-lg" style={{ background: 'rgba(239,68,68,0.1)', color: '#f87171' }}>
                {editError}
              </p>
            )}

            {/* Nom */}
            <div>
              <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--fg-muted)' }}>TOVAR NOMI</label>
              <input
                className="field"
                value={editForm.name}
                onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))}
                disabled={editSaving}
              />
            </div>

            {/* Narx */}
            <div>
              <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--fg-muted)' }}>NARX (SO'M)</label>
              <input
                className="field"
                type="number"
                inputMode="numeric"
                value={editForm.price}
                onChange={e => setEditForm(f => ({ ...f, price: e.target.value }))}
                disabled={editSaving}
              />
            </div>

            {/* Chegirma */}
            <div>
              <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--fg-muted)' }}>CHEGIRMA (%)</label>
              <input
                className="field"
                type="number"
                inputMode="numeric"
                value={editForm.discount}
                onChange={e => setEditForm(f => ({ ...f, discount: e.target.value }))}
                min={0} max={90}
                disabled={editSaving}
                style={{ width: 100 }}
              />
            </div>

            {/* Kategoriya */}
            <div>
              <label className="text-xs font-medium mb-2 block" style={{ color: 'var(--fg-muted)' }}>KATEGORIYA</label>
              <div className="flex flex-wrap gap-2">
                {['Kiyim','Elektronika','Poyabzal','Aksessuar','Sport','Uy uchun','Boshqa'].map(cat => (
                  <button
                    key={cat}
                    type="button"
                    disabled={editSaving}
                    onClick={() => setEditForm(f => ({ ...f, category: cat }))}
                    className="chip"
                    style={editForm.category === cat ? {
                      background: 'rgba(99,102,241,0.2)',
                      color: 'var(--accent-hover)',
                      border: '1px solid rgba(99,102,241,0.4)',
                    } : {}}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            </div>

            {/* Tavsif */}
            <div>
              <label className="text-xs font-medium mb-1 block" style={{ color: 'var(--fg-muted)' }}>TAVSIF</label>
              <textarea
                className="field resize-none"
                value={editForm.description}
                onChange={e => setEditForm(f => ({ ...f, description: e.target.value }))}
                rows={2}
                disabled={editSaving}
              />
            </div>

            <button
              onClick={saveEdit}
              disabled={editSaving}
              className="btn-primary mt-1 flex items-center justify-center gap-2"
            >
              {editSaving ? '…' : <><Check size={15} /> Saqlash</>}
            </button>
          </div>
        </div>
      )}

      {/* List */}
      {!loading && !error && filtered.length > 0 && (
        <div className="flex flex-col gap-3">
          {filtered.map((p, i) => {
            const id       = p.id ?? i
            const name     = p.title || p.name || '—'
            const imgUrl   = p.image_url || p.imageUrl
            const discount = p.discountPercent || p.discount || 0
            const cat      = p.category || 'Boshqa'
            const active   = p.active  !== false
            const inStock  = p.inStock !== false

            return (
              <div
                key={i}
                className="glass fade-up flex gap-3 p-3"
                style={{
                  animationDelay: `${Math.min(i * 40, 300)}ms`,
                  opacity: active ? 1 : 0.55,
                }}
              >
                {imgUrl ? (
                  <img
                    src={imgUrl}
                    alt={name}
                    loading="lazy"
                    className="w-16 h-16 rounded-xl object-cover flex-shrink-0"
                    style={{ border: '1px solid var(--border)' }}
                  />
                ) : (
                  <div
                    className="w-16 h-16 rounded-xl flex-shrink-0 flex items-center justify-center"
                    style={{ background: 'rgba(99,102,241,0.1)', border: '1px solid var(--border)' }}
                  >
                    <Package size={20} style={{ color: 'var(--accent)' }} />
                  </div>
                )}

                <div className="flex-1 min-w-0">
                  <p
                    className="font-medium text-sm truncate"
                    style={{ color: 'var(--fg)' }}
                  >
                    {name}
                  </p>
                  <p
                    className="text-sm font-semibold mt-0.5"
                    style={{ color: 'var(--accent-hover)', fontVariantNumeric: 'tabular-nums' }}
                  >
                    {fmt(p.price)}
                  </p>

                  <div className="flex flex-wrap items-center gap-1.5 mt-1.5">
                    <span
                      className="text-xs px-2 py-0.5 rounded-full font-medium"
                      style={{
                        background: CAT_COLORS[cat] || CAT_COLORS['Boshqa'],
                        color:      CAT_TEXT[cat]   || CAT_TEXT['Boshqa'],
                      }}
                    >
                      {cat}
                    </span>
                    {discount > 0 && (
                      <span
                        className="text-xs px-2 py-0.5 rounded-full font-medium"
                        style={{ background: 'rgba(239,68,68,0.12)', color: '#f87171' }}
                      >
                        -{discount}%
                      </span>
                    )}
                    {!active && (
                      <span
                        className="text-xs px-2 py-0.5 rounded-full font-medium"
                        style={{ background: 'rgba(239,68,68,0.12)', color: '#f87171' }}
                      >
                        Nofaol
                      </span>
                    )}
                    {!inStock && (
                      <span
                        className="text-xs px-2 py-0.5 rounded-full font-medium"
                        style={{ background: 'rgba(245,158,11,0.12)', color: '#fbbf24' }}
                      >
                        Tugagan
                      </span>
                    )}
                  </div>

                  {user.is_superadmin && p.added_by_name && (
                    <p className="text-xs mt-1" style={{ color: 'var(--fg-muted)' }}>
                      {p.added_by_name}
                    </p>
                  )}

                  {/* Superadmin toggle tugmalari */}
                  {user.is_superadmin && (
                    <div className="flex flex-wrap gap-2 mt-2.5">
                      {/* Faol / Nofaol */}
                      <button
                        disabled={toggling === `${id}:active`}
                        onClick={() => toggleField(id, 'active', active)}
                        className="text-xs px-2.5 py-1 rounded-lg font-medium cursor-pointer transition-all duration-150 active:scale-95 disabled:opacity-50"
                        style={{
                          background: active ? 'rgba(239,68,68,0.1)' : 'rgba(34,197,94,0.1)',
                          color: active ? '#f87171' : '#4ade80',
                          border: `1px solid ${active ? 'rgba(239,68,68,0.2)' : 'rgba(34,197,94,0.2)'}`,
                        }}
                      >
                        {toggling === `${id}:active` ? '…' : active ? '🔴 O\'chirish' : '🟢 Faollashtirish'}
                      </button>

                      {/* Bor / Tugadi */}
                      <button
                        disabled={toggling === `${id}:in_stock`}
                        onClick={() => toggleField(id, 'in_stock', inStock)}
                        className="text-xs px-2.5 py-1 rounded-lg font-medium cursor-pointer transition-all duration-150 active:scale-95 disabled:opacity-50"
                        style={{
                          background: inStock ? 'rgba(245,158,11,0.1)' : 'rgba(34,197,94,0.1)',
                          color: inStock ? '#fbbf24' : '#4ade80',
                          border: `1px solid ${inStock ? 'rgba(245,158,11,0.2)' : 'rgba(34,197,94,0.2)'}`,
                        }}
                      >
                        {toggling === `${id}:in_stock` ? '…' : inStock ? '⚠️ Tugadi' : '📦 Bor'}
                      </button>

                      {/* Tahrirlash */}
                      <button
                        onClick={() => openEdit(p)}
                        className="text-xs px-2.5 py-1 rounded-lg font-medium cursor-pointer transition-all duration-150 active:scale-95 flex items-center gap-1"
                        style={{ background: 'rgba(99,102,241,0.1)', color: '#818cf8', border: '1px solid rgba(99,102,241,0.2)' }}
                      >
                        <Pencil size={11} /> Tahrirlash
                      </button>

                                      {/* O'chirish */}
                      <button
                        onClick={() => setDeleteId(id)}
                        className="text-xs px-2.5 py-1 rounded-lg font-medium cursor-pointer transition-all duration-150 active:scale-95 flex items-center gap-1"
                        style={{ background: 'rgba(239,68,68,0.08)', color: '#f87171', border: '1px solid rgba(239,68,68,0.18)' }}
                      >
                        <Trash2 size={11} /> O'chirish
                      </button>

                    </div>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
