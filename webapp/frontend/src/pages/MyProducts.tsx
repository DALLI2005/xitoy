import { useEffect, useState } from 'react'
import { Package, RefreshCw, Pencil, Trash2, X, Check, Search } from 'lucide-react'
import { api } from '../api'
import type { Product, User, CategoryTree } from '../types'

interface Props { user: User }

function SkeletonRow() {
  return (
    <tr className="border-b" style={{ borderColor: 'var(--border)' }}>
      <td className="p-3"><div className="skeleton w-12 h-12 rounded-lg" /></td>
      <td className="p-3"><div className="skeleton h-4 w-3/4 rounded mb-2" /><div className="skeleton h-3 w-1/2 rounded" /></td>
      <td className="p-3"><div className="skeleton h-5 w-20 rounded-full" /></td>
      <td className="p-3"><div className="skeleton h-4 w-16 rounded" /></td>
      <td className="p-3"><div className="skeleton h-6 w-16 rounded-full" /></td>
      <td className="p-3"><div className="skeleton h-6 w-16 rounded-full" /></td>
      <td className="p-3"><div className="skeleton h-8 w-20 rounded-lg" /></td>
    </tr>
  )
}

// Kategoriyalar backenddan keladi va ro'yxati uzun — rangni nom asosida
// barqaror hisoblaymiz (bir xil toifa doim bir xil rangda ko'rinadi).
const CAT_PALETTE = [
  { bg: 'rgba(99,102,241,0.15)',  fg: '#818cf8' },
  { bg: 'rgba(6,182,212,0.15)',   fg: '#22d3ee' },
  { bg: 'rgba(34,197,94,0.15)',   fg: '#4ade80' },
  { bg: 'rgba(245,158,11,0.15)',  fg: '#fbbf24' },
  { bg: 'rgba(239,68,68,0.15)',   fg: '#f87171' },
  { bg: 'rgba(168,85,247,0.15)',  fg: '#c084fc' },
  { bg: 'rgba(236,72,153,0.15)',  fg: '#f472b6' },
  { bg: 'rgba(20,184,166,0.15)',  fg: '#2dd4bf' },
]
function catColor(cat: string) {
  if (!cat || cat === 'Boshqa') return { bg: 'rgba(255,255,255,0.06)', fg: '#71717a' }
  let hash = 0
  for (let i = 0; i < cat.length; i++) hash = (hash * 31 + cat.charCodeAt(i)) >>> 0
  return CAT_PALETTE[hash % CAT_PALETTE.length]
}

type Filter = 'all' | 'inactive' | 'outofstock'

interface EditForm {
  name: string; price: string; discount: string; description: string
  category: string; subcategory: string; product_type: string
  variantlar_yoqilgan: boolean
  variant_nomlari: string[]
  variant_narxlari: string[]
}

export default function MyProducts({ user }: Props) {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState('')
  const [toggling, setToggling] = useState<string>('')  // "id:field"
  const [filter, setFilter]     = useState<Filter>('all')
  const [search, setSearch]     = useState('')

  // Edit modal
  const [editProduct, setEditProduct] = useState<Product | null>(null)
  const [editForm, setEditForm]       = useState<EditForm>({ name: '', price: '', discount: '', description: '', category: '', subcategory: '', product_type: '', variantlar_yoqilgan: false, variant_nomlari: [], variant_narxlari: [] })
  const [categoryTree, setCategoryTree] = useState<CategoryTree>({})
  const [editSaving, setEditSaving]   = useState(false)
  const [editError, setEditError]     = useState('')
  const [sizesByColor, setSizesByColor] = useState<Record<number, {nomi: string, narx: string}[]>>({})

  function addSize(colorIndex: number) {
    setSizesByColor(prev => ({
      ...prev,
      [colorIndex]: [...(prev[colorIndex] || []), { nomi: '', narx: '' }]
    }))
  }
  function updateSize(colorIndex: number, sizeIndex: number, field: 'nomi' | 'narx', value: string) {
    setSizesByColor(prev => {
      const updated = [...(prev[colorIndex] || [])]
      updated[sizeIndex] = { ...updated[sizeIndex], [field]: value }
      return { ...prev, [colorIndex]: updated }
    })
  }
  function removeSize(colorIndex: number, sizeIndex: number) {
    setSizesByColor(prev => ({
      ...prev,
      [colorIndex]: (prev[colorIndex] || []).filter((_, i) => i !== sizeIndex)
    }))
  }

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

  // Kategoriya daraxti — backend bilan bir xil manba
  useEffect(() => {
    api.categoryTree()
      .then(res => setCategoryTree(res.tree || {}))
      .catch(() => {})
  }, [])

  // Tahrirda admin faqat o'ziga ruxsat berilgan asosiy toifalarni ko'radi
  const allowedRoots = user.is_superadmin
    ? Object.keys(categoryTree)
    : Object.keys(categoryTree).filter(r => user.categories.includes(r))
  const editSubs  = editForm.category ? Object.keys(categoryTree[editForm.category] || {}) : []
  const editTypes = editForm.category && editForm.subcategory
    ? (categoryTree[editForm.category]?.[editForm.subcategory] || [])
    : []

  function openEdit(p: Product) {
    setEditProduct(p)
    setEditError('')
    setEditForm({
      name:                p.title || p.name || '',
      price:               String(p.price),
      discount:            String(p.discountPercent || p.discount || 0),
      category:            p.category || '',
      subcategory:         p.subcategory || '',
      product_type:        p.product_type || p.productType || '',
      description:         p.description || '',
      variantlar_yoqilgan: p.variantlarYoqilgan ?? false,
      variant_nomlari:     p.variantNomlari ?? [],
      variant_narxlari:    (p.variantNarxlari ?? []).map(String),
    })
    const loadedSizes: Record<number, {nomi: string, narx: string}[]> = {}
    Object.entries(p.razmerMatritsa || {}).forEach(([idx, sizes]) => {
      loadedSizes[parseInt(idx)] = (sizes as any[]).map((s: any) => ({
        nomi: s.nomi,
        narx: String(s.narx)
      }))
    })
    setSizesByColor(loadedSizes)
  }

  async function saveEdit() {
    if (!editProduct) return
    if (!editForm.name.trim()) { setEditError('Nom kiritilmagan'); return }
    if (!editForm.price)       { setEditError('Narx kiritilmagan'); return }
    setEditSaving(true)
    setEditError('')

    const basePrice = parseInt(editForm.price) || 0
    const razmerMatritsa: Record<string, {nomi: string, narx: number}[]> = {}
    Object.entries(sizesByColor).forEach(([colorIdx, sizes]) => {
      const validSizes = sizes.filter(s => s.nomi.trim())
      if (validSizes.length > 0) {
        razmerMatritsa[colorIdx] = validSizes.map(s => ({
          nomi: s.nomi.trim(),
          narx: parseInt(s.narx) || basePrice
        }))
      }
    })

    try {
      await api.updateProduct(editProduct.id!, {
        name:                editForm.name.trim(),
        price:               basePrice,
        discount:            parseInt(editForm.discount || '0') || 0,
        category:            editForm.category,
        subcategory:         editForm.subcategory,
        product_type:        editForm.product_type,
        description:         editForm.description.trim(),
        variantlar_yoqilgan: editForm.variantlar_yoqilgan,
        variant_nomlari:     editForm.variant_nomlari,
        variant_narxlari:    editForm.variant_narxlari.map(s => parseInt(s) || 0),
        razmer_matritsa:     editForm.variantlar_yoqilgan ? razmerMatritsa : {},
      })
      setProducts(prev => prev.map(p =>
        p.id == editProduct.id
          ? { ...p, title: editForm.name.trim(), name: editForm.name.trim(),
              price: basePrice,
              discountPercent: parseInt(editForm.discount || '0') || 0,
              category: editForm.category,
              subcategory: editForm.subcategory,
              product_type: editForm.product_type,
              description: editForm.description.trim(),
              variantlarYoqilgan: editForm.variantlar_yoqilgan,
              variantNomlari: editForm.variant_nomlari,
              variantNarxlari: editForm.variant_narxlari.map(s => parseInt(s) || 0),
              razmerMatritsa: editForm.variantlar_yoqilgan ? razmerMatritsa : {} }
          : p
      ))
      setEditProduct(null)
      setSizesByColor({})
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
    if (filter === 'inactive' && p.active !== false) return false
    if (filter === 'outofstock' && p.inStock !== false) return false
    
    if (search) {
      const q = search.toLowerCase()
      const title = (p.title || p.name || '').toLowerCase()
      if (!title.includes(q)) return false
    }
    return true
  })

  const filterLabels: { key: Filter; label: string; count?: number }[] = [
    { key: 'all',        label: 'Barcha',   count: products.length },
    { key: 'inactive',   label: 'Nofaol', count: products.filter(p => p.active === false).length },
    { key: 'outofstock', label: 'Tugagan', count: products.filter(p => p.inStock === false).length },
  ]

  return (
    <div className="px-4 md:px-8 pt-6 pb-12 w-full max-w-[1400px] mx-auto">
      {/* Header */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold" style={{ color: 'var(--fg)' }}>
            {user.is_superadmin ? 'Barcha tovarlar' : 'Tovarlarim'}
          </h1>
          {!loading && !error && (
            <p className="text-sm mt-1" style={{ color: 'var(--fg-muted)' }}>
              Jami {filtered.length} ta tovar
            </p>
          )}
        </div>
        <div className="flex gap-3 w-full md:w-auto">
          <div className="relative flex-1 md:w-64">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
            <input 
              type="text" 
              placeholder="Tovar nomi bo'yicha qidiruv..." 
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="w-full pl-9 pr-4 py-2 rounded-xl text-sm"
              style={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--fg)' }}
            />
          </div>
          <button
            onClick={load}
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
      {user.is_superadmin && !loading && !error && (
        <div className="flex gap-2 mb-6 overflow-x-auto pb-2 scrollbar-hide">
          {filterLabels.map(({ key, label, count }) => (
            <button
              key={key}
              onClick={() => setFilter(key)}
              className="flex-shrink-0 text-sm font-semibold px-4 py-2 rounded-xl cursor-pointer transition-all duration-200"
              style={{
                background: filter === key ? 'rgba(99,102,241,0.15)' : 'var(--surface)',
                color:      filter === key ? '#818cf8' : 'var(--fg-muted)',
                border:     `1px solid ${filter === key ? 'rgba(99,102,241,0.3)' : 'var(--border)'}`,
              }}
            >
              {label} {count !== undefined && <span className="ml-1.5 px-2 py-0.5 rounded-full text-xs" style={{ background: filter === key ? 'rgba(99,102,241,0.2)' : 'var(--border)', color: filter === key ? '#818cf8' : 'var(--fg-muted)' }}>{count}</span>}
            </button>
          ))}
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="p-4 rounded-xl mb-6 flex items-center gap-3" style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.2)' }}>
          <span className="text-sm text-red-400 font-medium">{error}</span>
          <button onClick={load} className="ml-auto text-sm text-red-400 font-bold hover:underline">Qayta urinish</button>
        </div>
      )}

      {/* Empty */}
      {!loading && !error && filtered.length === 0 && (
        <div className="glass flex flex-col items-center justify-center py-20 text-center rounded-2xl">
          <div className="w-16 h-16 rounded-2xl flex items-center justify-center mb-4" style={{ background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.2)' }}>
            <Package size={28} style={{ color: 'var(--accent)' }} />
          </div>
          <p className="text-lg font-semibold" style={{ color: 'var(--fg)' }}>{filter === 'all' && !search ? "Tovarlar yo'q" : "Hech narsa topilmadi"}</p>
          <p className="text-sm mt-1" style={{ color: 'var(--fg-muted)' }}>{filter === 'all' && !search ? "Yangi tovar qo'shing" : "Qidiruv yoki filterni o'zgartirib ko'ring"}</p>
        </div>
      )}

      {/* Table */}
      {(loading || (!error && filtered.length > 0)) && (
        <div className="w-full overflow-x-auto rounded-2xl border" style={{ borderColor: 'var(--border)', background: 'var(--surface)' }}>
          <table className="w-full text-left text-sm whitespace-nowrap" style={{ color: 'var(--fg)' }}>
            <thead>
              <tr className="border-b bg-black/20" style={{ borderColor: 'var(--border)' }}>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Rasm</th>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Tovar nomi & ID</th>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Kategoriya</th>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Narx</th>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider text-center" style={{ color: 'var(--fg-muted)' }}>Zaxira</th>
                <th className="p-4 font-semibold text-xs uppercase tracking-wider text-center" style={{ color: 'var(--fg-muted)' }}>Status</th>
                {user.is_superadmin && <th className="p-4 font-semibold text-xs uppercase tracking-wider text-right" style={{ color: 'var(--fg-muted)' }}>Amallar</th>}
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
                filtered.map((p, i) => {
                  const id       = p.id ?? i
                  const name     = p.title || p.name || '—'
                  const imgUrl   = p.image_url || p.imageUrl
                  const discount = p.discountPercent || p.discount || 0
                  const cat      = p.category || 'Boshqa'
                  const catPath  = (p.categoryPath && p.categoryPath.length
                    ? p.categoryPath
                    : [cat, p.subcategory, p.product_type || p.productType].filter(Boolean)
                  ).join(' › ')
                  const active   = p.active  !== false
                  const inStock  = p.inStock !== false

                  return (
                    <tr 
                      key={id} 
                      className="border-b transition-colors hover:bg-white/5" 
                      style={{ borderColor: 'var(--border)', opacity: active ? 1 : 0.6 }}
                    >
                      <td className="p-4">
                        {imgUrl ? (
                          <img src={imgUrl} alt={name} loading="lazy" className="w-12 h-12 rounded-lg object-cover border" style={{ borderColor: 'var(--border)' }} />
                        ) : (
                          <div className="w-12 h-12 rounded-lg flex items-center justify-center bg-indigo-500/10 border" style={{ borderColor: 'var(--border)' }}>
                            <Package size={18} className="text-indigo-400" />
                          </div>
                        )}
                      </td>
                      <td className="p-4 max-w-[200px] md:max-w-[300px]">
                        <p className="font-semibold truncate text-sm" style={{ color: 'var(--fg)' }} title={name}>{name}</p>
                        <p className="text-xs mt-1 font-mono" style={{ color: 'var(--fg-muted)' }}>#{id}</p>
                      </td>
                      <td className="p-4">
                        <span
                          className="text-xs px-2.5 py-1 rounded-md font-semibold"
                          style={{ background: catColor(cat).bg, color: catColor(cat).fg }}
                          title={catPath}
                        >
                          {cat}
                        </span>
                        {catPath !== cat && (
                          <p className="text-[11px] mt-1 truncate max-w-[160px]" style={{ color: 'var(--fg-muted)' }} title={catPath}>
                            {[p.subcategory, p.product_type || p.productType].filter(Boolean).join(' › ')}
                          </p>
                        )}
                      </td>
                      <td className="p-4">
                        <div className="flex flex-col gap-1">
                          <span className="font-semibold" style={{ color: 'var(--accent-hover)' }}>{fmt(p.price)}</span>
                          {discount > 0 && <span className="text-xs font-medium text-red-400 bg-red-400/10 px-2 py-0.5 rounded-full w-fit">-{discount}% chegirma</span>}
                        </div>
                      </td>
                      <td className="p-4 text-center align-middle">
                        {user.is_superadmin ? (
                          <button
                            disabled={toggling === `${id}:in_stock`}
                            onClick={() => toggleField(id, 'in_stock', inStock)}
                            className="inline-flex items-center justify-center text-xs px-3 py-1.5 rounded-full font-bold cursor-pointer transition-all active:scale-95 disabled:opacity-50"
                            style={{
                              background: inStock ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)',
                              color: inStock ? '#4ade80' : '#f87171',
                              border: `1px solid ${inStock ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)'}`
                            }}
                          >
                            {toggling === `${id}:in_stock` ? '...' : inStock ? 'Bor' : 'Tugagan'}
                          </button>
                        ) : (
                          <span className="text-xs px-3 py-1.5 rounded-full font-bold inline-block" style={{ background: inStock ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)', color: inStock ? '#4ade80' : '#f87171' }}>
                            {inStock ? 'Bor' : 'Tugagan'}
                          </span>
                        )}
                      </td>
                      <td className="p-4 text-center align-middle">
                        {user.is_superadmin ? (
                          <button
                            disabled={toggling === `${id}:active`}
                            onClick={() => toggleField(id, 'active', active)}
                            className="inline-flex items-center justify-center text-xs px-3 py-1.5 rounded-full font-bold cursor-pointer transition-all active:scale-95 disabled:opacity-50"
                            style={{
                              background: active ? 'rgba(99,102,241,0.15)' : 'var(--bg)',
                              color: active ? '#818cf8' : 'var(--fg-muted)',
                              border: `1px solid ${active ? 'rgba(99,102,241,0.3)' : 'var(--border)'}`
                            }}
                          >
                            {toggling === `${id}:active` ? '...' : active ? 'Faol' : 'Nofaol'}
                          </button>
                        ) : (
                          <span className="text-xs px-3 py-1.5 rounded-full font-bold inline-block" style={{ background: active ? 'rgba(99,102,241,0.15)' : 'var(--bg)', color: active ? '#818cf8' : 'var(--fg-muted)' }}>
                            {active ? 'Faol' : 'Nofaol'}
                          </span>
                        )}
                      </td>
                      {user.is_superadmin && (
                        <td className="p-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => openEdit(p)}
                              className="p-2 rounded-lg cursor-pointer transition-colors hover:bg-indigo-500/20 text-indigo-400"
                              title="Tahrirlash"
                            >
                              <Pencil size={16} />
                            </button>
                            <button
                              onClick={() => setDeleteId(id)}
                              className="p-2 rounded-lg cursor-pointer transition-colors hover:bg-red-500/20 text-red-400"
                              title="O'chirish"
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* ── Delete confirm modal ── */}
      {deleteId != null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={{ background: 'rgba(0,0,0,0.7)' }} onClick={() => !deleting && setDeleteId(null)}>
          <div className="w-full max-w-sm rounded-2xl p-6 flex flex-col gap-4 shadow-2xl scale-100" style={{ background: 'var(--surface)', border: '1px solid var(--border)' }} onClick={e => e.stopPropagation()}>
            <div className="w-12 h-12 rounded-full flex items-center justify-center mx-auto" style={{ background: 'rgba(239,68,68,0.1)', color: '#f87171' }}>
              <Trash2 size={24} />
            </div>
            <h3 className="font-bold text-center text-lg" style={{ color: 'var(--fg)' }}>Tovarni o'chirish</h3>
            <p className="text-sm text-center px-2" style={{ color: 'var(--fg-muted)' }}>Rostdan ham ushbu tovarni o'chirib tashlamoqchimisiz? Bu amalni ortga qaytarib bo'lmaydi.</p>
            <div className="flex gap-3 mt-2">
              <button onClick={() => setDeleteId(null)} disabled={deleting} className="flex-1 py-2.5 rounded-xl text-sm font-bold cursor-pointer transition-colors hover:bg-white/5" style={{ background: 'var(--bg)', color: 'var(--fg)', border: '1px solid var(--border)' }}>Bekor qilish</button>
              <button onClick={confirmDelete} disabled={deleting} className="flex-1 py-2.5 rounded-xl text-sm font-bold cursor-pointer transition-opacity hover:opacity-80 flex justify-center" style={{ background: '#ef4444', color: '#fff' }}>
                {deleting ? <RefreshCw size={18} className="animate-spin" /> : "O'chirish"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Edit modal ── */}
      {editProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4 py-8" style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)' }} onClick={() => { if (!editSaving) { setEditProduct(null); setSizesByColor({}) } }}>
          <div className="w-full max-w-lg rounded-2xl p-6 flex flex-col gap-5 overflow-y-auto max-h-full shadow-2xl relative" style={{ background: 'var(--surface)', border: '1px solid var(--border)' }} onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between pb-2 border-b" style={{ borderColor: 'var(--border)' }}>
              <h2 className="text-lg font-bold" style={{ color: 'var(--fg)' }}>Tovarni tahrirlash</h2>
              <button onClick={() => { setEditProduct(null); setSizesByColor({}) }} className="p-2 rounded-full hover:bg-white/10 transition-colors" style={{ color: 'var(--fg-muted)' }}>
                <X size={20} />
              </button>
            </div>

            {editError && (
              <div className="px-4 py-3 rounded-xl flex items-center gap-2" style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.2)', color: '#f87171' }}>
                <span className="text-sm font-semibold">{editError}</span>
              </div>
            )}

            <div className="flex flex-col gap-4">
              {/* Nom */}
              <div>
                <label className="text-xs font-bold mb-1.5 block uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Tovar nomi</label>
                <input className="field w-full" value={editForm.name} onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))} disabled={editSaving} />
              </div>

              <div className="flex gap-4">
                {/* Narx */}
                <div className="flex-1">
                  <label className="text-xs font-bold mb-1.5 block uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Narx (so'm)</label>
                  <input className="field w-full" type="number" inputMode="numeric" value={editForm.price} onChange={e => setEditForm(f => ({ ...f, price: e.target.value }))} disabled={editSaving} />
                </div>
                {/* Chegirma */}
                <div className="flex-1">
                  <label className="text-xs font-bold mb-1.5 block uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Chegirma (%)</label>
                  <input className="field w-full" type="number" inputMode="numeric" value={editForm.discount} onChange={e => setEditForm(f => ({ ...f, discount: e.target.value }))} min={0} max={90} disabled={editSaving} />
                </div>
              </div>

              {/* Kategoriya — 3 daraja, backenddagi daraxt bilan bir xil */}
              <div>
                <label className="text-xs font-bold mb-2 block uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Kategoriya</label>
                <div className="flex flex-col gap-2">
                  <select
                    className="field w-full" disabled={editSaving || allowedRoots.length === 0}
                    value={editForm.category}
                    onChange={e => setEditForm(f => ({ ...f, category: e.target.value, subcategory: '', product_type: '' }))}
                  >
                    <option value="">Asosiy toifani tanlang</option>
                    {allowedRoots.map(c => <option key={c} value={c}>{c}</option>)}
                  </select>

                  {editForm.category && (
                    <select
                      className="field w-full" disabled={editSaving}
                      value={editForm.subcategory}
                      onChange={e => setEditForm(f => ({ ...f, subcategory: e.target.value, product_type: '' }))}
                    >
                      <option value="">Kichik toifani tanlang</option>
                      {editSubs.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  )}

                  {editForm.subcategory && (
                    <select
                      className="field w-full" disabled={editSaving}
                      value={editForm.product_type}
                      onChange={e => setEditForm(f => ({ ...f, product_type: e.target.value }))}
                    >
                      <option value="">Tovar turini tanlang</option>
                      {editTypes.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  )}
                </div>
              </div>

              {/* Tavsif */}
              <div>
                <label className="text-xs font-bold mb-1.5 block uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Tavsif</label>
                <textarea className="field w-full resize-none" value={editForm.description} onChange={e => setEditForm(f => ({ ...f, description: e.target.value }))} rows={3} disabled={editSaving} />
              </div>

              <div className="border-t my-2" style={{ borderColor: 'var(--border)' }}></div>

              {/* Variant toggle */}
              <div className="flex items-center justify-between p-4 rounded-xl" style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid var(--border)' }}>
                <div>
                  <h4 className="font-semibold text-sm" style={{ color: 'var(--fg)' }}>Ranglar va razmerlar</h4>
                  <p className="text-xs mt-1" style={{ color: 'var(--fg-muted)' }}>Mahsulotning narxi va rasmlari farq qiladimi?</p>
                </div>
                <button
                  type="button" disabled={editSaving}
                  onClick={() => setEditForm(f => ({
                    ...f, variantlar_yoqilgan: !f.variantlar_yoqilgan,
                    variant_nomlari:  !f.variantlar_yoqilgan && f.variant_nomlari.length === 0 ? [''] : f.variant_nomlari,
                    variant_narxlari: !f.variantlar_yoqilgan && f.variant_narxlari.length === 0 ? [''] : f.variant_narxlari,
                  }))}
                  className="px-4 py-2 rounded-xl text-sm font-bold transition-colors cursor-pointer"
                  style={{
                    background: editForm.variantlar_yoqilgan ? '#818cf8' : 'var(--bg)',
                    color: editForm.variantlar_yoqilgan ? '#fff' : 'var(--fg)',
                    border: `1px solid ${editForm.variantlar_yoqilgan ? '#818cf8' : 'var(--border)'}`
                  }}
                >
                  {editForm.variantlar_yoqilgan ? 'Yoqilgan' : "O'chirilgan"}
                </button>
              </div>

              {/* Variant qatorlari */}
              {editForm.variantlar_yoqilgan && (
                <div className="flex flex-col gap-4 p-4 rounded-xl mt-2" style={{ background: 'var(--bg)', border: '1px dashed var(--border)' }}>
                  <label className="text-xs font-bold uppercase tracking-wider" style={{ color: 'var(--fg-muted)' }}>Variantlar (Rang va Narx)</label>
                  {editForm.variant_nomlari.map((nom, idx) => (
                    <div key={idx} className="flex flex-col gap-2 p-3 rounded-lg border" style={{ borderColor: 'var(--border)', background: 'var(--surface)' }}>
                      <div className="flex gap-2 items-center">
                        <input className="field flex-1" placeholder="Rang nomi" value={nom} disabled={editSaving} onChange={e => setEditForm(f => { const n = [...f.variant_nomlari]; n[idx] = e.target.value; return { ...f, variant_nomlari: n } })} />
                        <input className="field w-32" type="number" inputMode="numeric" placeholder="Narx" value={editForm.variant_narxlari[idx] ?? ''} disabled={editSaving} onChange={e => setEditForm(f => { const n = [...f.variant_narxlari]; n[idx] = e.target.value; return { ...f, variant_narxlari: n } })} />
                        <button type="button" disabled={editSaving} onClick={() => setEditForm(f => ({ ...f, variant_nomlari: f.variant_nomlari.filter((_, i) => i !== idx), variant_narxlari: f.variant_narxlari.filter((_, i) => i !== idx) }))} className="w-10 h-10 flex items-center justify-center rounded-xl transition-colors hover:bg-red-500/20" style={{ color: '#f87171' }}><X size={16} /></button>
                      </div>
                      
                      {/* Razmerlar shu rang uchun */}
                      <div className="mt-2 pl-4 border-l-2" style={{ borderColor: 'var(--border)' }}>
                        <p className="text-xs mb-2 font-medium" style={{ color: 'var(--fg-muted)' }}>Razmerlar (S, M, L...)</p>
                        {(sizesByColor[idx] || []).map((size, sizeIndex) => (
                          <div key={sizeIndex} className="flex gap-2 mb-2 items-center">
                            <input className="field flex-1 text-sm py-1.5" placeholder="Razmer" value={size.nomi} onChange={e => updateSize(idx, sizeIndex, 'nomi', e.target.value)} disabled={editSaving} />
                            <input className="field w-28 text-sm py-1.5" type="number" placeholder="Narxi" value={size.narx} onChange={e => updateSize(idx, sizeIndex, 'narx', e.target.value)} disabled={editSaving} />
                            <button type="button" onClick={() => removeSize(idx, sizeIndex)} disabled={editSaving} className="p-2 text-red-400 hover:bg-red-400/10 rounded-lg"><X size={14}/></button>
                          </div>
                        ))}
                        <button type="button" onClick={() => addSize(idx)} disabled={editSaving} className="text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors" style={{ color: '#818cf8', background: 'rgba(99,102,241,0.1)' }}>+ Razmer qo'shish</button>
                      </div>
                    </div>
                  ))}
                  <button type="button" disabled={editSaving} onClick={() => setEditForm(f => ({ ...f, variant_nomlari: [...f.variant_nomlari, ''], variant_narxlari: [...f.variant_narxlari, ''] }))} className="w-full py-2.5 rounded-xl text-sm font-bold border-2 border-dashed transition-colors" style={{ borderColor: 'var(--border)', color: 'var(--fg-muted)' }}>
                    + Yangi rang qo'shish
                  </button>
                </div>
              )}
            </div>

            <div className="pt-4 border-t mt-2 flex justify-end gap-3" style={{ borderColor: 'var(--border)' }}>
              <button onClick={() => { setEditProduct(null); setSizesByColor({}) }} disabled={editSaving} className="px-5 py-2.5 rounded-xl font-bold transition-colors hover:bg-white/5" style={{ color: 'var(--fg)' }}>Bekor qilish</button>
              <button onClick={saveEdit} disabled={editSaving} className="px-6 py-2.5 rounded-xl font-bold flex items-center gap-2 transition-opacity hover:opacity-90 shadow-lg shadow-indigo-500/20" style={{ background: '#6366f1', color: '#fff' }}>
                {editSaving ? <RefreshCw size={18} className="animate-spin" /> : <><Check size={18} /> Saqlash</>}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
