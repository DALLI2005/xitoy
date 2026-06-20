import { useState, useEffect, useRef, useCallback } from 'react'
import { ImagePlus, DollarSign, Tag, Percent, AlignLeft, CheckCircle2, AlertCircle, Loader2, RefreshCw, TrendingUp, Clock, Megaphone } from 'lucide-react'
import { api } from '../api'
import PhotoUpload from '../components/PhotoUpload'
import { hapticSuccess, hapticError } from '../telegram'
import type { User } from '../types'

interface Props { user: User }

interface CategoryConfig {
  bufferPercent: number
  profitPercent: number
}

const CATEGORY_CONFIGS: Record<string, CategoryConfig> = {
  'Kiyim':       { bufferPercent: 25, profitPercent: 35 },
  'Elektronika': { bufferPercent: 15, profitPercent: 25 },
  'Poyabzal':    { bufferPercent: 20, profitPercent: 30 },
  'Aksessuar':   { bufferPercent: 25, profitPercent: 40 },
  'Sport':       { bufferPercent: 20, profitPercent: 30 },
  'Uy uchun':    { bufferPercent: 10, profitPercent: 25 },
  'Boshqa':      { bufferPercent: 25, profitPercent: 30 },
}

function getConfig(category: string): CategoryConfig {
  return CATEGORY_CONFIGS[category] ?? CATEGORY_CONFIGS['Boshqa']
}

function calcBreakdown(yuan: number, cnyRate: number, config: CategoryConfig) {
  const effectiveRate = cnyRate * (1 + config.bufferPercent / 100)
  const productCostSom = yuan * effectiveRate
  const totalWithSafety = productCostSom + 5000
  const profitAmount = totalWithSafety * config.profitPercent / 100
  const finalPrice = Math.ceil((totalWithSafety + profitAmount) / 1000) * 1000
  return { effectiveRate, productCostSom, profitAmount, finalPrice }
}

function fmt(n: number): string {
  return Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
}

type DiscountType = 'permanent' | 'temporary'

const DURATION_OPTIONS = [
  { label: '⚡ 15 daqiqa',                     minutes: 15    },
  { label: '⚡ 30 daqiqa',                     minutes: 30    },
  { label: '🔥 45 daqiqa',                     minutes: 45    },
  { label: '🔥 1 soat',                        minutes: 60    },
  { label: '🔥 1 soat 30 daqiqa',              minutes: 90    },
  { label: '🔥 2 soat',                        minutes: 120   },
  { label: '🔥 3 soat',                        minutes: 180   },
  { label: '📅 6 soat',                        minutes: 360   },
  { label: '📅 1 kun',                         minutes: 1440  },
  { label: '📅 3 kun',                         minutes: 4320  },
  { label: '📅 1 hafta',                       minutes: 10080 },
  { label: "✏️ Boshqa (5 ga bo'linadigan)",    minutes: -1    },
]

const EMPTY = { name: '', yuan: '', discount: '', category: '', description: '', rating: '4.5', sold_count: '10' }
const RATE_KEY = 'cny_rate_cache'

type ToastType = 'success' | 'error'
interface Toast { type: ToastType; msg: string }

export default function AddProduct({ user }: Props) {
  const [form, setForm]               = useState(EMPTY)
  const [photos, setPhotos]           = useState<File[]>([])
  const [loading, setLoading]         = useState(false)
  const [toast, setToast]             = useState<Toast | null>(null)
  const [cnyRate, setCnyRate]         = useState<number | null>(null)
  const [rateLabel, setRateLabel]     = useState('')
  const [rateLoading, setRateLoading] = useState(false)
  const [discountType, setDiscountType]     = useState<DiscountType>('permanent')
  const [selectedOption, setSelectedOption] = useState<number | null>(null)
  const [customMinutes, setCustomMinutes]   = useState('')
  const [autoDelete, setAutoDelete]         = useState(false)
  const [sendPush, setSendPush]             = useState(false)
  const [variantEnabled, setVariantEnabled]   = useState(false)
  const [variantNames,   setVariantNames]     = useState<string[]>([])
  const [variantPrices,  setVariantPrices]    = useState<string[]>([])
  const toastTimer                    = useRef<ReturnType<typeof setTimeout>>()

  const categories = user.categories

  function showToast(type: ToastType, msg: string) {
    clearTimeout(toastTimer.current)
    setToast({ type, msg })
    toastTimer.current = setTimeout(() => setToast(null), 3500)
  }

  useEffect(() => () => clearTimeout(toastTimer.current), [])

  const fetchRate = useCallback(async (force = false) => {
    if (!force) {
      try {
        const cached = localStorage.getItem(RATE_KEY)
        if (cached) {
          const { rate, today } = JSON.parse(cached)
          const todayStr = new Date().toISOString().slice(0, 10)
          if (today === todayStr && rate > 0) {
            setCnyRate(rate)
            setRateLabel(`1 ¥ = ${fmt(rate)} so'm`)
            return
          }
        }
      } catch {}
    }

    setRateLoading(true)
    try {
      const res = await fetch('https://cbu.uz/uz/arkhiv-kursov-valyut/json/CNY/')
      const data = await res.json()
      const rate = parseFloat(data[0].Rate)
      const dateStr: string = data[0].Date
      setCnyRate(rate)
      setRateLabel(`1 ¥ = ${fmt(rate)} so'm (${dateStr})`)
      localStorage.setItem(RATE_KEY, JSON.stringify({
        rate,
        today: new Date().toISOString().slice(0, 10),
      }))
    } catch {
      try {
        const cached = localStorage.getItem(RATE_KEY)
        if (cached) {
          const { rate } = JSON.parse(cached)
          setCnyRate(rate)
          setRateLabel(`1 ¥ = ${fmt(rate)} so'm (offline)`)
        } else {
          setRateLabel('Kurs yuklanmadi')
        }
      } catch {
        setRateLabel('Kurs yuklanmadi')
      }
    } finally {
      setRateLoading(false)
    }
  }, [])

  useEffect(() => { fetchRate() }, [fetchRate])

  function set(field: string, value: string) {
    setForm(f => ({ ...f, [field]: value }))
  }

  const yuanNum  = parseFloat(form.yuan) || 0
  const config   = getConfig(form.category)
  const breakdown = (yuanNum > 0 && cnyRate && form.category)
    ? calcBreakdown(yuanNum, cnyRate, config)
    : null

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.name.trim())         return showToast('error', 'Tovar nomini kiriting')
    if (!form.yuan || yuanNum <= 0) return showToast('error', 'Yuan narxini kiriting')
    if (!cnyRate)                  return showToast('error', 'Valyuta kursi yuklanmadi')
    if (!form.category)            return showToast('error', 'Kategoriyani tanlang')
    if (!photos.length)            return showToast('error', "Kamida 1 ta rasm qo'shing")

    const discountNum = parseInt(form.discount || '0') || 0
    if (discountNum > 0 && discountType === 'temporary') {
      if (selectedOption === null) return showToast('error', 'Chegirma muddatini tanlang')
      if (selectedOption === -1) {
        const cm = parseInt(customMinutes) || 0
        if (cm <= 0) return showToast('error', "Daqiqani kiriting")
        if (cm % 5 !== 0) return showToast('error', "Faqat 5 ga bo'linadigan daqiqalar: 5, 10, 15, 20...")
      }
    }

    const finalPrice = calcBreakdown(yuanNum, cnyRate, config).finalPrice

    const actualMinutes = selectedOption === -1 ? (parseInt(customMinutes) || 0) : (selectedOption || 0)
    const UZB_OFFSET    = 5 * 60 * 60 * 1000
    const discountExpiresAt = (discountNum > 0 && discountType === 'temporary' && actualMinutes > 0)
      ? new Date(Date.now() + UZB_OFFSET + actualMinutes * 60 * 1000).toISOString()
      : ''

    setLoading(true)
    try {
      const urls = await Promise.all(photos.map(f => api.uploadImage(f)))
      const res: any = await api.addProduct({
        name:             form.name.trim(),
        price:            finalPrice,
        discount:         discountNum,
        category:         form.category,
        description:      form.description.trim(),
        image_url:        urls[0],
        images:           urls,
        rating:           parseFloat(form.rating) || 4.5,
        sold_count:       parseInt(form.sold_count) || 10,
        discount_type:    discountNum > 0 ? (discountType === 'temporary' ? 'vaqtinchalik' : 'doimiy') : 'doimiy',
        discount_expires: discountExpiresAt,
        auto_delete:      discountType === 'temporary' && autoDelete,
        send_push:        sendPush && discountNum > 0,
        variantlar_yoqilgan: variantEnabled,
        variant_nomlari:     variantEnabled ? variantNames.slice(0, photos.length) : [],
        variant_narxlari:    variantEnabled ? variantPrices.slice(0, photos.length).map(p => parseInt(p) || 0) : [],
      })
      hapticSuccess()

      const toastMsg = res?.status === 'pending'
        ? `⏳ ${res.message}`
        : `"${form.name.trim()}" qo'shildi!`
      showToast('success', toastMsg)
      setForm({ ...EMPTY })
      setPhotos([])
      setDiscountType('permanent')
      setSelectedOption(null)
      setCustomMinutes('')
      setAutoDelete(false)
      setSendPush(false)
      setVariantEnabled(false)
      setVariantNames([])
      setVariantPrices([])
    } catch (e: any) {
      hapticError()
      showToast('error', e.message || 'Xatolik yuz berdi')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="px-4 pt-5 pb-6">
      {/* Toast */}
      {toast && (
        <div
          className="toast fixed top-4 left-4 right-4 z-50 flex items-center gap-3 px-4 py-3 rounded-2xl"
          style={{
            background: toast.type === 'success' ? 'rgba(34,197,94,0.12)' : 'rgba(239,68,68,0.12)',
            border: `1px solid ${toast.type === 'success' ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)'}`,
          }}
        >
          {toast.type === 'success'
            ? <CheckCircle2 size={18} style={{ color: 'var(--success)', flexShrink: 0 }} />
            : <AlertCircle  size={18} style={{ color: 'var(--error)',   flexShrink: 0 }} />
          }
          <span className="text-sm font-medium" style={{ color: 'var(--fg)' }}>{toast.msg}</span>
        </div>
      )}

      <h1 className="text-lg font-semibold mb-5" style={{ color: 'var(--fg)' }}>Tovar qo'shish</h1>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">

        {/* Photos */}
        <section className="glass p-4">
          <label className="flex items-center gap-2 text-xs font-medium mb-3" style={{ color: 'var(--fg-muted)' }}>
            <ImagePlus size={14} />
            RASMLAR <span style={{ color: 'var(--error)' }}>*</span>
          </label>
          <PhotoUpload files={photos} onChange={setPhotos} disabled={loading} />

          {/* Variant toggle */}
          {photos.length > 0 && (
            <div
              className="flex items-center justify-between mt-3 py-2.5 px-3 rounded-xl"
              style={{
                background: variantEnabled ? 'rgba(99,102,241,0.08)' : 'rgba(255,255,255,0.04)',
                border: `1px solid ${variantEnabled ? 'rgba(99,102,241,0.25)' : 'var(--border)'}`,
              }}
            >
              <div>
                <p className="text-xs font-medium" style={{ color: variantEnabled ? 'var(--accent)' : 'var(--fg)' }}>
                  Rasmlar narxi farq qiladimi?
                </p>
                <p className="text-xs" style={{ color: 'var(--fg-muted)' }}>
                  Har bir rasm uchun nom va alohida narx
                </p>
              </div>
              <button
                type="button"
                onClick={() => setVariantEnabled(v => !v)}
                disabled={loading}
                className="relative w-10 h-5 rounded-full transition-all duration-200 cursor-pointer disabled:opacity-50 flex-shrink-0"
                style={{ background: variantEnabled ? 'var(--accent)' : 'rgba(255,255,255,0.1)' }}
              >
                <span
                  className="absolute top-0.5 w-4 h-4 rounded-full transition-all duration-200"
                  style={{
                    background: 'white',
                    left: variantEnabled ? 'calc(100% - 1.125rem)' : '0.125rem',
                    boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
                  }}
                />
              </button>
            </div>
          )}

          {/* Per-photo variant inputs */}
          {variantEnabled && photos.length > 0 && (
            <div className="flex flex-col gap-2 mt-3">
              <p className="text-xs font-medium" style={{ color: 'var(--fg-muted)' }}>
                Har bir rasm uchun nom va narx kiriting:
              </p>
              {photos.map((_, i) => (
                <div key={i} className="flex gap-2 items-center">
                  <span
                    className="text-xs font-bold w-6 text-center flex-shrink-0"
                    style={{ color: 'var(--accent)' }}
                  >
                    {i + 1}
                  </span>
                  <input
                    className="field flex-1"
                    type="text"
                    placeholder={`Nomi (masalan: Qora)`}
                    value={variantNames[i] || ''}
                    onChange={e => {
                      const a = [...variantNames]
                      a[i] = e.target.value
                      setVariantNames(a)
                    }}
                    disabled={loading}
                    autoComplete="off"
                  />
                  <div className="flex items-center gap-1">
                    <input
                      className="field"
                      type="number"
                      inputMode="numeric"
                      placeholder="Narxi"
                      value={variantPrices[i] || ''}
                      onChange={e => {
                        const a = [...variantPrices]
                        a[i] = e.target.value
                        setVariantPrices(a)
                      }}
                      disabled={loading}
                      style={{ width: 110 }}
                    />
                    <span className="text-xs flex-shrink-0" style={{ color: 'var(--fg-muted)' }}>so'm</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Name + Price */}
        <section className="glass p-4 flex flex-col gap-3">
          <div>
            <label className="flex items-center gap-2 text-xs font-medium mb-2" style={{ color: 'var(--fg-muted)' }}>
              <Tag size={13} /> TOVAR NOMI <span style={{ color: 'var(--error)' }}>*</span>
            </label>
            <input
              className="field"
              type="text"
              value={form.name}
              onChange={e => set('name', e.target.value)}
              placeholder="Masalan: Adidas Ultraboost"
              disabled={loading}
              autoComplete="off"
            />
          </div>

          {/* Rate indicator */}
          <div className="flex items-center justify-between px-1">
            <span className="text-xs" style={{ color: 'var(--fg-muted)' }}>
              {rateLoading ? 'Kurs yuklanmoqda…' : rateLabel || 'Kurs yuklanmadi'}
            </span>
            <button
              type="button"
              onClick={() => fetchRate(true)}
              disabled={rateLoading || loading}
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--accent)', padding: 4, display: 'flex', alignItems: 'center' }}
            >
              <RefreshCw size={13} className={rateLoading ? 'animate-spin' : ''} />
            </button>
          </div>

          <div>
            <label className="flex items-center gap-2 text-xs font-medium mb-2" style={{ color: 'var(--fg-muted)' }}>
              <DollarSign size={13} /> XITOYDAGI NARXI (¥ YUAN) <span style={{ color: 'var(--error)' }}>*</span>
            </label>
            <input
              className="field"
              type="number"
              inputMode="decimal"
              value={form.yuan}
              onChange={e => set('yuan', e.target.value)}
              placeholder="0"
              disabled={loading}
              min={0}
              step="0.01"
            />
          </div>

          <div>
            <label className="flex items-center gap-2 text-xs font-medium mb-2" style={{ color: 'var(--fg-muted)' }}>
              <Percent size={13} /> CHEGIRMA (%) — ixtiyoriy
            </label>
            <input
              className="field"
              type="number"
              inputMode="numeric"
              value={form.discount}
              onChange={e => {
                set('discount', e.target.value)
                if (!parseInt(e.target.value || '0')) setSendPush(false)
              }}
              placeholder="0"
              min={0}
              max={90}
              disabled={loading}
              style={{ width: 120 }}
            />
          </div>

          {/* Chegirma turi — faqat chegirma > 0 da ko'rinadi */}
          {parseInt(form.discount || '0') > 0 && (
            <>
              {/* Push broadcast toggle */}
              {user.is_superadmin && (
                <div
                  className="flex items-center justify-between py-2.5 px-3 rounded-xl"
                  style={{
                    background: sendPush ? 'rgba(245,158,11,0.08)' : 'rgba(255,255,255,0.04)',
                    border: `1px solid ${sendPush ? 'rgba(245,158,11,0.25)' : 'var(--border)'}`,
                  }}
                >
                  <div className="flex items-center gap-2">
                    <Megaphone size={14} style={{ color: sendPush ? '#fbbf24' : 'var(--fg-muted)', flexShrink: 0 }} />
                    <div>
                      <p className="text-xs font-medium" style={{ color: sendPush ? '#fbbf24' : 'var(--fg)' }}>
                        Push yuborish
                      </p>
                      <p className="text-xs" style={{ color: 'var(--fg-muted)' }}>
                        Saqlanganda barcha foydalanuvchilarga
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setSendPush(v => !v)}
                    disabled={loading}
                    className="relative w-10 h-5 rounded-full transition-all duration-200 cursor-pointer disabled:opacity-50 flex-shrink-0"
                    style={{ background: sendPush ? '#f59e0b' : 'rgba(255,255,255,0.1)' }}
                  >
                    <span
                      className="absolute top-0.5 w-4 h-4 rounded-full transition-all duration-200"
                      style={{
                        background: 'white',
                        left: sendPush ? 'calc(100% - 1.125rem)' : '0.125rem',
                        boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
                      }}
                    />
                  </button>
                </div>
              )}

              <div>
                <label className="flex items-center gap-2 text-xs font-medium mb-2" style={{ color: 'var(--fg-muted)' }}>
                  <Clock size={13} /> CHEGIRMA TURI
                </label>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => { setDiscountType('permanent'); setSelectedOption(null); setCustomMinutes('') }}
                    disabled={loading}
                    className={`chip ${discountType === 'permanent' ? 'active' : ''}`}
                  >
                    Doimiy
                  </button>
                  <button
                    type="button"
                    onClick={() => setDiscountType('temporary')}
                    disabled={loading}
                    className={`chip ${discountType === 'temporary' ? 'active' : ''}`}
                  >
                    Vaqtinchalik
                  </button>
                </div>
              </div>

              {discountType === 'temporary' && (
                <div>
                  <label className="flex items-center gap-2 text-xs font-medium mb-2" style={{ color: 'var(--fg-muted)' }}>
                    <Clock size={13} /> CHEGIRMA MUDDATI <span style={{ color: 'var(--error)' }}>*</span>
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {DURATION_OPTIONS.map(opt => (
                      <button
                        key={opt.minutes}
                        type="button"
                        onClick={() => { setSelectedOption(opt.minutes); if (opt.minutes !== -1) setCustomMinutes('') }}
                        disabled={loading}
                        className={`chip ${selectedOption === opt.minutes ? 'active' : ''}`}
                      >
                        {opt.label}
                      </button>
                    ))}
                  </div>
                  {selectedOption === -1 && (
                    <input
                      className="field mt-3"
                      type="number"
                      inputMode="numeric"
                      value={customMinutes}
                      onChange={e => setCustomMinutes(e.target.value)}
                      placeholder="Masalan: 90 (faqat 5 ga bo'linadigan)"
                      min={1}
                      max={99999}
                      disabled={loading}
                      autoFocus
                    />
                  )}
                  {selectedOption !== null && (
                    <label
                      className="flex items-center gap-2 mt-3 cursor-pointer"
                      style={{ fontSize: 13, color: autoDelete ? 'var(--error)' : 'var(--fg-muted)' }}
                    >
                      <input
                        type="checkbox"
                        checked={autoDelete}
                        onChange={e => setAutoDelete(e.target.checked)}
                        disabled={loading}
                        style={{ accentColor: 'var(--error)', width: 15, height: 15 }}
                      />
                      🗑️ Chegirma tugagach tovarni avtomatik o'chir
                    </label>
                  )}
                  {autoDelete && selectedOption !== null && (
                    <p className="mt-1" style={{ fontSize: 12, color: 'var(--error)', opacity: 0.85 }}>
                      ⚠️ Chegirma tugagandan so'ng tovar ilovadan va kanaldan o'chiriladi
                    </p>
                  )}
                </div>
              )}
            </>
          )}
        </section>

        {/* Category */}
        <section className="glass p-4">
          <label className="flex items-center gap-2 text-xs font-medium mb-3" style={{ color: 'var(--fg-muted)' }}>
            <Tag size={13} /> KATEGORIYA <span style={{ color: 'var(--error)' }}>*</span>
          </label>
          <div className="flex flex-wrap gap-2">
            {categories.map(cat => (
              <button
                key={cat}
                type="button"
                onClick={() => set('category', cat === form.category ? '' : cat)}
                disabled={loading}
                className={`chip ${form.category === cat ? 'active' : ''}`}
              >
                {cat}
              </button>
            ))}
          </div>
        </section>

        {/* Price breakdown */}
        {breakdown && (
          <section className="glass p-4">
            <label className="flex items-center gap-2 text-xs font-medium mb-3" style={{ color: 'var(--fg-muted)' }}>
              <TrendingUp size={13} /> NARX HISOBI
            </label>
            <div className="flex flex-col gap-2">
              <Row
                label={`Kurs (+${config.bufferPercent}% bufer)`}
                value={`1¥ = ${fmt(breakdown.effectiveRate)} so'm`}
              />
              <Row
                label={`Tovar (${form.yuan}¥ × ${fmt(breakdown.effectiveRate)})`}
                value={`${fmt(breakdown.productCostSom)} so'm`}
              />
              <Row label="Xavfsizlik zaxira" value="+ 5 000 so'm" />
              <Row
                label={`Foyda (${config.profitPercent}%)`}
                value={`+ ${fmt(breakdown.profitAmount)} so'm`}
              />
              <div
                className="flex justify-between text-base font-semibold pt-2 mt-1"
                style={{ borderTop: '1px solid var(--border)', color: 'var(--fg)' }}
              >
                <span>JAMI NARX</span>
                <span style={{ color: 'var(--accent-hover)' }}>{fmt(breakdown.finalPrice)} so'm</span>
              </div>
            </div>
          </section>
        )}

        {/* Rating + Sold count */}
        <section className="glass p-4 flex flex-col gap-3">
          <div className="flex gap-3">
            <div className="flex-1">
              <label className="flex items-center gap-2 text-xs font-medium mb-2" style={{ color: 'var(--fg-muted)' }}>
                ⭐ REYTING (1–5)
              </label>
              <input
                className="field"
                type="number"
                inputMode="decimal"
                value={form.rating}
                onChange={e => set('rating', e.target.value)}
                min={1}
                max={5}
                step={0.1}
                disabled={loading}
              />
            </div>
            <div className="flex-1">
              <label className="flex items-center gap-2 text-xs font-medium mb-2" style={{ color: 'var(--fg-muted)' }}>
                📦 SOTILGAN (dona)
              </label>
              <input
                className="field"
                type="number"
                inputMode="numeric"
                value={form.sold_count}
                onChange={e => set('sold_count', e.target.value)}
                min={0}
                disabled={loading}
              />
            </div>
          </div>
        </section>

        {/* Description */}
        <section className="glass p-4">
          <label className="flex items-center gap-2 text-xs font-medium mb-2" style={{ color: 'var(--fg-muted)' }}>
            <AlignLeft size={13} /> TAVSIF — ixtiyoriy
          </label>
          <textarea
            className="field resize-none"
            value={form.description}
            onChange={e => set('description', e.target.value)}
            placeholder="Tovar haqida qo'shimcha ma'lumot…"
            rows={3}
            disabled={loading}
          />
        </section>

        {/* Submit */}
        <button type="submit" disabled={loading} className="btn-primary mt-1">
          {loading ? (
            <span className="flex items-center justify-center gap-2">
              <Loader2 size={18} className="animate-spin" />
              Saqlanmoqda…
            </span>
          ) : 'Saqlash'}
        </button>

      </form>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between text-sm" style={{ color: 'var(--fg-muted)' }}>
      <span>{label}</span>
      <span>{value}</span>
    </div>
  )
}
