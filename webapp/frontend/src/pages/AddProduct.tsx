import { useState, useEffect, useRef } from 'react'
import {
  ChevronDown, CheckCircle2, AlertCircle, Loader2,
  ArrowRight, ArrowLeft, ImagePlus, X, Languages,
  Tag, Percent, Clock, Ruler, Plus, Trash2, Search, Check
} from 'lucide-react'
import { api } from '../api'
import { hapticSuccess, hapticError } from '../telegram'
import type { User, CategoryTree } from '../types'

interface Props { user: User }

// ─── Kategoriya daraxti ──────────────────────────────────────────────────────
// Daraxt backenddan (`categories.py` — yagona manba) olinadi, shuning uchun
// sayt / backend / ilova bir xil kategoriyalar bilan ishlaydi.

// ─── Yordamchi funksiyalar ────────────────────────────────────────────────────
function randInt(min: number, max: number) {
  return Math.floor(Math.random() * (max - min + 1)) + min
}
function randomSold() { return randInt(10, 100) }
function randomRating() { return (randInt(0, 10) / 10 + 3.5).toFixed(1) }

function makeEmpty() {
  return {
    name: '', price: '', discount: '', description: '',
    rating: randomRating(), sold_count: String(randomSold()),
  }
}

const DESCRIPTION_MAX = 390

// select ning qiymati -> ilovada ko'rsatiladigan nom
const COUNTRY_LABELS: Record<string, string> = {
  uzbekistan: "O'zbekiston",
  russia: 'Rossiya',
  turkey: 'Turkiya',
  china: 'Xitoy',
}

type DiscountType = 'permanent' | 'temporary'
type ToastType = 'success' | 'error'
interface Toast { type: ToastType; msg: string }

const DURATION_OPTIONS = [
  { label: '15 daqiqa', minutes: 15 },
  { label: '30 daqiqa', minutes: 30 },
  { label: '1 soat', minutes: 60 },
  { label: '2 soat', minutes: 120 },
  { label: '6 soat', minutes: 360 },
  { label: '1 kun', minutes: 1440 },
  { label: '3 kun', minutes: 4320 },
  { label: '1 hafta', minutes: 10080 },
  { label: "Boshqa", minutes: -1 },
]

// ─── Komponent ────────────────────────────────────────────────────────────────
export default function AddProduct({ user }: Props) {
  // Step management
  const [step, setStep] = useState(1)  // 1 = Kartochka, 2 = Narx, 3 = Razmer/Xususiyat

  // Category state (3 daraja)
  const [categoryTree, setCategoryTree] = useState<CategoryTree>({})
  const [treeLoading, setTreeLoading] = useState(true)
  const [treeError, setTreeError] = useState('')
  const [cat1, setCat1] = useState('')  // Asosiy kategoriya (ruxsat shu bo'yicha)
  const [cat2, setCat2] = useState('')  // 2-daraja
  const [cat3, setCat3] = useState('')  // 3-daraja
  const [catConfirmed, setCatConfirmed] = useState(false)

  // Form
  const [form, setForm] = useState(makeEmpty)
  const [photos, setPhotos] = useState<File[]>([])
  const [previews, setPreviews] = useState<string[]>([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState<Toast | null>(null)
  const [translating, setTranslating] = useState(false)

  // Step 3 specific
  const [showAttrModal, setShowAttrModal] = useState(false)
  const [attrDropdownOpen, setAttrDropdownOpen] = useState(false)
  const [colorSearch, setColorSearch] = useState('')
  const [activeAttrType, setActiveAttrType] = useState<string>('')
  const [selectedAttributes, setSelectedAttributes] = useState<Record<string, string[]>>({})
  const [activeAttributes, setActiveAttributes] = useState<string[]>([])
  
  const AVAILABLE_COLORS = [
    { name: 'Alvon', hex: '#E03C31' },
    { name: 'Ametist', hex: '#9966CC' },
    { name: 'To\'q qizil', hex: '#8B0000' },
    { name: 'Sarg\'ish', hex: '#F5DEB3' },
    { name: 'Sarg\'ish melanj', hex: '#D2B48C' },
    { name: 'Oq', hex: '#FFFFFF' },
    { name: 'Moviy', hex: '#4169E1' },
    { name: 'Xantal', hex: '#FFDB58' },
    { name: 'Sariq', hex: '#FFFF00' },
    { name: 'Yashil', hex: '#008000' },
    { name: 'Yashil xaki', hex: '#556B2F' },
    { name: 'Tilla xaki', hex: '#BDB76B' },
    { name: 'Tilla', hex: '#FFD700' },
    { name: 'Indigo', hex: '#4B0082' },
    { name: 'Qora', hex: '#000000' },
    { name: 'Kulrang', hex: '#808080' },
    { name: 'Qizil', hex: '#FF0000' },
    { name: 'Pushti', hex: '#FFC0CB' },
    { name: 'Jigarrang', hex: '#A52A2A' },
    { name: 'Binafsha', hex: '#800080' },
    { name: 'Havorang', hex: '#87CEEB' },
  ]

  const ATTR_TYPES = [
    { id: 'Rang', label: 'Rang', type: 'color' },
    { id: 'O\'lcham', label: 'Kiyim o\'lchami', type: 'text', options: ['XXS', 'XS', 'S', 'M', 'L', 'XL', 'XXL', '3XL', '4XL', '5XL'] },
    { id: 'Poyabzal o\'lchami', label: 'Poyabzal o\'lchami', type: 'text', options: ['35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45'] },
    { id: 'Xotira hajmi', label: 'Xotira hajmi', type: 'text', options: ['16 GB', '32 GB', '64 GB', '128 GB', '256 GB', '512 GB', '1 TB'] },
    { id: 'Operativ xotira', label: 'Operativ xotira', type: 'text', options: ['2 GB', '4 GB', '6 GB', '8 GB', '12 GB', '16 GB', '32 GB'] },
    { id: 'Uzuk o\'lchamlari', label: 'Uzuk o\'lchamlari', type: 'text', options: ['15', '15.5', '16', '16.5', '17', '17.5', '18', '18.5', '19', '19.5', '20', '20.5', '21'] },
  ]


  // New states for Uzum UI
  const [country, setCountry] = useState('')
  const [noCountry, setNoCountry] = useState(false)
  const [isChina, setIsChina] = useState(true)
  const [guarantee, setGuarantee] = useState('')
  
  // Handlers for country
  const handleCountryChange = (val: string) => {
    setCountry(val)
    if (val) {
      setNoCountry(false)
      setIsChina(false)
    }
  }
  const handleNoCountryChange = (checked: boolean) => {
    setNoCountry(checked)
    if (checked) {
      setCountry('')
      setIsChina(false)
    }
  }
  const handleIsChinaChange = (checked: boolean) => {
    setIsChina(checked)
    if (checked) {
      setCountry('')
      setNoCountry(false)
    }
  }


  // Narx (Step 2)
  const [discountType, setDiscountType] = useState<DiscountType>('permanent')
  const [selectedDuration, setSelectedDuration] = useState<number | null>(null)
  const [customMinutes, setCustomMinutes] = useState('')
  const [autoDelete, setAutoDelete] = useState(false)
  const [sendPush, setSendPush] = useState(false)

  // Variantlar (Step 3)
  const [variantEnabled, setVariantEnabled] = useState(false)
  const [variantNames, setVariantNames] = useState<string[]>([])
  const [variantPrices, setVariantPrices] = useState<string[]>([])
  const [sizesByColor, setSizesByColor] = useState<Record<number, { nomi: string; narx: string }[]>>({})

  const toastTimer = useRef<ReturnType<typeof setTimeout>>()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const descTooLong = form.description.length >= DESCRIPTION_MAX

  // Kategoriya daraxtini backenddan olamiz (yagona manba)
  useEffect(() => {
    let cancelled = false
    api.categoryTree()
      .then(res => {
        if (cancelled) return
        setCategoryTree(res.tree || {})
        setTreeError('')
      })
      .catch(() => {
        if (!cancelled) setTreeError("Kategoriyalarni yuklab bo'lmadi")
      })
      .finally(() => { if (!cancelled) setTreeLoading(false) })
    return () => { cancelled = true }
  }, [])

  // Kategoriya optionlari — admin faqat o'ziga ruxsat berilgan asosiy toifalarni ko'radi
  const allowedRoots = user.is_superadmin
    ? Object.keys(categoryTree)
    : Object.keys(categoryTree).filter(r => user.categories.includes(r))
  const cat1Options = allowedRoots
  const cat2Options = cat1 ? Object.keys(categoryTree[cat1] || {}) : []
  const cat3Options = cat1 && cat2 ? (categoryTree[cat1]?.[cat2] || []) : []

  // Kategoriya o'zgarganda quyi darajalarni tozalash
  function handleCat1Change(val: string) {
    setCat1(val); setCat2(''); setCat3(''); setCatConfirmed(false)
  }
  function handleCat2Change(val: string) {
    setCat2(val); setCat3(''); setCatConfirmed(false)
  }
  function handleCat3Change(val: string) {
    setCat3(val); setCatConfirmed(false)
  }
  function confirmCategory() {
    if (!cat1 || !cat2 || !cat3) return
    setCatConfirmed(true)
  }

  function showToast(type: ToastType, msg: string) {
    clearTimeout(toastTimer.current)
    setToast({ type, msg })
    toastTimer.current = setTimeout(() => setToast(null), 3500)
  }

  useEffect(() => () => clearTimeout(toastTimer.current), [])

  function set(field: string, value: string) {
    setForm(f => ({ ...f, [field]: value }))
  }

  // Rasm tanlash
  function handlePhotoSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files || [])
    if (files.length + photos.length > 10) return showToast('error', "Maksimum 10 ta rasm")
    const newPreviews = files.map(f => URL.createObjectURL(f))
    setPhotos(p => [...p, ...files])
    setPreviews(p => [...p, ...newPreviews])
    e.target.value = ''
  }

  function removePhoto(i: number) {
    URL.revokeObjectURL(previews[i])
    setPhotos(p => p.filter((_, j) => j !== i))
    setPreviews(p => p.filter((_, j) => j !== i))
  }

  // Variant funksiyalar
  function addVariant() { setVariantNames(n => [...n, '']); setVariantPrices(p => [...p, '']) }
  function removeVariant(i: number) {
    setVariantNames(n => n.filter((_, j) => j !== i))
    setVariantPrices(p => p.filter((_, j) => j !== i))
    setSizesByColor(s => { const c = { ...s }; delete c[i]; return c })
  }
  function addSize(ci: number) {
    setSizesByColor(s => ({ ...s, [ci]: [...(s[ci] || []), { nomi: '', narx: '' }] }))
  }
  function removeSize(ci: number, si: number) {
    setSizesByColor(s => ({ ...s, [ci]: (s[ci] || []).filter((_, j) => j !== si) }))
  }

  // Tarjima
  async function handleTranslate() {
    if (!form.name.trim()) return showToast('error', 'Avval nom kiriting')
    setTranslating(true)
    try {
      const res = await api.translateAndShorten(form.name.trim())
      set('name', res.translated_short)
      showToast('success', 'Tarjima bajarildi')
    } catch { showToast('error', 'Tarjima ishlamadi') }
    finally { setTranslating(false) }
  }

  // Step validatsiya
  function canGoStep2() {
    return catConfirmed && form.name.trim().length > 0 && photos.length > 0
  }
  function canGoStep3() {
    return (parseInt(form.price) || 0) > 0
  }

  // Yuborish
  async function handleSubmit() {
    if (!canGoStep2()) return showToast('error', 'Kategoriya, nom va rasm talab qilinadi')
    if (!canGoStep3()) return showToast('error', 'Narx talab qilinadi')

    setLoading(true)
    try {
      // Variantlarni JSON ga o'girish
      let razmerMatritsa: Record<string, { nomi: string; narx: number }[]> = {}
      if (variantEnabled && variantNames.length > 0) {
        variantNames.forEach((vn, ci) => {
          if (vn.trim()) {
            razmerMatritsa[vn] = (sizesByColor[ci] || [])
              .filter(s => s.nomi.trim())
              .map(s => ({ nomi: s.nomi, narx: parseInt(s.narx) || 0 }))
          }
        })
      }

      // Muddatni hisoblash
      let discountExpires: string | null = null
      let minutesFinal = 0
      if (discountType === 'temporary' && selectedDuration !== null) {
        minutesFinal = selectedDuration === -1 ? (parseInt(customMinutes) || 0) : selectedDuration
        if (minutesFinal > 0) {
          const d = new Date(Date.now() + minutesFinal * 60000)
          discountExpires = d.toISOString()
        }
      }

      const priceNum = parseInt(form.price) || 0
      const discountNum = parseFloat(form.discount) || 0
      const finalPrice = discountType === 'permanent'
        ? Math.round(priceNum * (1 - discountNum / 100))
        : priceNum

      // Rasmlarni yuklab olish
      const imageUrls: string[] = []
      for (const photo of photos) {
        imageUrls.push(await api.uploadImage(photo))
      }

      // Ishlab chiqarilgan mamlakat — uchta variantdan bittasi
      const countryFinal = isChina
        ? 'Xitoy'
        : noCountry
          ? ''
          : (COUNTRY_LABELS[country] || '')

      // Xususiyatlar: faqat qiymati tanlanganlari yuboriladi
      const attributesFinal: Record<string, string[]> = {}
      activeAttributes.forEach(attr => {
        const vals = (selectedAttributes[attr] || []).filter(Boolean)
        if (vals.length > 0) attributesFinal[attr] = vals
      })

      await api.addProduct({
        name: form.name.trim(),
        price: priceNum,
        discount: discountNum,
        discount_type: discountType,
        discount_expires: discountExpires || undefined,
        auto_delete: autoDelete,
        // Uchala daraja alohida yuboriladi: ruxsat cat1 (asosiy toifa) bo'yicha
        // tekshiriladi, cat2/cat3 esa tovar bilan birga saqlanadi.
        category: cat1,
        subcategory: cat2,
        product_type: cat3,
        description: form.description.trim(),
        // Ilovadagi tovar kartochkasida ko'rsatiladigan qo'shimcha ma'lumotlar
        country: countryFinal,
        guarantee_months: parseInt(guarantee) || 0,
        attributes: attributesFinal,
        image_url: imageUrls[0] || '',
        images: imageUrls,
        rating: parseFloat(form.rating) || 4.2,
        sold_count: parseInt(form.sold_count) || 10,
        variantlar_yoqilgan: variantEnabled,
        variant_nomlari: variantEnabled ? variantNames : [],
        variant_narxlari: variantEnabled ? variantPrices.map(p => parseInt(p) || 0) : [],
        razmer_matritsa: razmerMatritsa,
        send_push: sendPush,
      })

      hapticSuccess?.()
      showToast('success', "Tovar muvaffaqiyatli qo'shildi!")
      // Formani tozalash
      setForm(makeEmpty())
      setPhotos([]); setPreviews([])
      setCat1(''); setCat2(''); setCat3(''); setCatConfirmed(false)
      setStep(1)
      setVariantEnabled(false); setVariantNames([]); setVariantPrices([])
      setSizesByColor({}); setDiscountType('permanent')
      setSelectedDuration(null); setCustomMinutes('')
      setCountry(''); setNoCountry(false); setIsChina(true); setGuarantee('')
      setActiveAttributes([]); setSelectedAttributes({})
    } catch (err: any) {
      hapticError?.()
      showToast('error', err?.message || "Xato yuz berdi")
    } finally {
      setLoading(false)
    }
  }

  const priceNum = parseInt(form.price) || 0
  const discountNum = parseFloat(form.discount) || 0
  const finalPrice = discountNum > 0 ? Math.round(priceNum * (1 - discountNum / 100)) : priceNum

  // ─── RENDER ────────────────────────────────────────────────────────────────
  return (
    <div className="add-product-container">
      {/* Toast */}
      {toast && (
        <div className={`ap-toast ap-toast--${toast.type}`}>
          {toast.type === 'success'
            ? <CheckCircle2 size={16} />
            : <AlertCircle size={16} />}
          <span>{toast.msg}</span>
        </div>
      )}

      {/* Sahifa sarlavhasi */}
      <div className="ap-header">
        <div className="ap-breadcrumb">
          <span className="ap-breadcrumb__parent">Mening tovarlarim</span>
          <span className="ap-breadcrumb__sep">/</span>
          <span className="ap-breadcrumb__current">Tovarni yaratish</span>
        </div>
        <h1 className="ap-title">Tovarni yaratish</h1>
      </div>

      {/* ── BOSQICH 1: TOVAR KARTOCHKASI ── */}
      <div className="ap-card fade-up" style={{ marginBottom: "24px" }}>
          {/* Kategoriya bo'limi */}
          <div className="ap-section">
            <div className="ap-section__header">
              <h2 className="ap-section__title">Tovar toifasi <span className="ap-required">*</span></h2>
              <a className="ap-link" href="#">Yo'riqnomada batafsil</a>
            </div>

            {/* Kategoriyalar yuklanmasa / ruxsat bo'lmasa — aniq xabar */}
            {treeError && (
              <div className="ap-info-box" style={{ color: '#B91C1C', background: '#FEE2E2' }}>
                {treeError}. Sahifani yangilab ko'ring.
              </div>
            )}
            {!treeLoading && !treeError && cat1Options.length === 0 && (
              <div className="ap-info-box" style={{ color: '#B91C1C', background: '#FEE2E2' }}>
                Sizga hech qanday toifaga ruxsat berilmagan. Superadmin bilan bog'laning.
              </div>
            )}

            <div className="ap-category-selects">
              {/* 1-daraja */}
              <div className="ap-select-wrap">
                <select
                  className="ap-select"
                  value={cat1}
                  disabled={treeLoading || cat1Options.length === 0}
                  onChange={e => handleCat1Change(e.target.value)}
                >
                  <option value="">
                    {treeLoading ? 'Yuklanmoqda…' : 'Toifani tanlang'}
                  </option>
                  {cat1Options.map(o => <option key={o} value={o}>{o}</option>)}
                </select>
                <ChevronDown size={16} className="ap-select-icon" />
              </div>

              {/* 2-daraja */}
              {cat1 && (
                <div className="ap-select-wrap fade-up">
                  <select
                    className="ap-select"
                    value={cat2}
                    onChange={e => handleCat2Change(e.target.value)}
                  >
                    <option value="">Kichik toifani tanlang</option>
                    {cat2Options.map(o => <option key={o} value={o}>{o}</option>)}
                  </select>
                  <ChevronDown size={16} className="ap-select-icon" />
                </div>
              )}

              {/* 3-daraja */}
              {cat2 && (
                <div className="ap-select-wrap fade-up">
                  <select
                    className="ap-select"
                    value={cat3}
                    onChange={e => handleCat3Change(e.target.value)}
                  >
                    <option value="">Tovar turini tanlang</option>
                    {cat3Options.map(o => <option key={o} value={o}>{o}</option>)}
                  </select>
                  <ChevronDown size={16} className="ap-select-icon" />
                </div>
              )}
            </div>

            {/* Kategoriya o'zgartirilsa xabar */}
            {catConfirmed && (
              <div className="ap-info-box">
                Kategoriyani o'zgartirsangiz — avvalgi xususiyatlar saqlanmaydi
              </div>
            )}

            <button
              className={`ap-btn-confirm ${!cat1 || !cat2 || !cat3 ? 'ap-btn-confirm--disabled' : ''}`}
              onClick={confirmCategory}
              disabled={!cat1 || !cat2 || !cat3}
            >
              {catConfirmed ? <><CheckCircle2 size={16} /> Tasdiqlangan</> : 'Qabul qilish'}
            </button>
          </div>

          {/* Tovar nomi bo'limi */}
          <div className="ap-section">
            <div className="ap-section__header">
              <h2 className="ap-section__title">Tovar nomi <span className="ap-required">*</span></h2>
            </div>

            {/* Nomlash bo'yicha maslahat */}
            <div className="ap-name-tips">
              <div className="ap-name-tip">
                <span className="ap-name-tip__title">Nomlash sxemasi</span>
                <span className="ap-name-tip__desc">Tovarning turi + brend + model muhim tavsif</span>
              </div>
              <div className="ap-name-tip">
                <span className="ap-name-tip__title">Hajmi 90 ta belgi</span>
                <span className="ap-name-tip__desc">Qisqaroq nomlar raqobatchilar bilan mos kelishi mumkin — bu Yandex va Google izlashlaridagi ko'rsatkichlarni kamaytiradi</span>
              </div>
              <div className="ap-name-tip">
                <span className="ap-name-tip__title">Bitta nomdagi variantlar</span>
                <span className="ap-name-tip__desc">Yo'l qo'yiladi, lekin 90 ta belgidan ko'p emas, masalan: Simsiz quloqchinlar AKG N 700</span>
              </div>
            </div>

            <div className="ap-field-wrap">
              <label className="ap-label">Tovar nomi <span className="ap-required">*</span></label>
              <div className="ap-input-row">
                <input
                  className="ap-input"
                  placeholder="Tovarning aniq nomi"
                  value={form.name}
                  maxLength={90}
                  onChange={e => set('name', e.target.value)}
                />
              </div>
              <div className="ap-char-count">{form.name.length}/90</div>
            </div>

            {/* Ishlab chiqarilgan mamlakat */}
            <div className="ap-field-wrap" style={{ marginTop: '32px' }}>
              <label className="ap-label" style={{ fontSize: '16px', fontWeight: '600' }}>Ishlab chiqarilgan mamlakat</label>
              <div className="ap-select-wrap" style={{ marginTop: '12px' }}>
                <select className="ap-select" value={country} onChange={e => handleCountryChange(e.target.value)}>
                  <option value="">Mamlakatni tanlang</option>
                  <option value="uzbekistan">O'zbekiston</option>
                  <option value="russia">Rossiya</option>
                  <option value="turkey">Turkiya</option>
                  <option value="china">Xitoy</option>
                </select>
                <ChevronDown size={16} className="ap-select-icon" />
              </div>
              <label className="ap-checkbox">
                <input type="checkbox" checked={noCountry} onChange={e => handleNoCountryChange(e.target.checked)} />
                <span style={{ fontSize: '14px', color: '#475569' }}>Mavjud emas ishlab chiqarilgan mamlakat</span>
              </label>
              <label className="ap-checkbox">
                <input type="checkbox" checked={isChina} onChange={e => handleIsChinaChange(e.target.checked)} />
                <span style={{ fontSize: '14px', color: '#475569' }}>Xitoyda ishlab chiqarilgan</span>
              </label>
            </div>

            {/* Kafolat */}
            <div className="ap-field-wrap" style={{ marginTop: '32px' }}>
              <label className="ap-label" style={{ fontSize: '16px', fontWeight: '600' }}>Kafolat <span style={{ color: '#94A3B8', fontWeight: '400' }}>oylarda</span></label>
              <div style={{ display: 'flex', gap: '16px', marginTop: '12px', alignItems: 'flex-start' }}>
                <div style={{ flex: 1, padding: '16px', border: '1px solid #E2E8F0', borderRadius: '8px', background: '#F8FAFC' }}>
                  <div style={{ fontWeight: '600', fontSize: '14px', marginBottom: '4px' }}>Agar muddat ko'rsatilmasa</div>
                  <div style={{ fontSize: '13px', color: '#64748B' }}><span style={{ color: '#8B5CF6' }}>Qonun bo'yicha</span> kafolat avtomatik ravishda 6 oy bo'ladi</div>
                </div>
                <div style={{ flex: 1 }}>
                  <input
                    className="ap-input"
                    placeholder="Masalan, 24 yoki 12"
                    value={guarantee}
                    onChange={e => setGuarantee(e.target.value)}
                  />
                </div>
              </div>
            </div>

            {/* Tavsif — oddiy matn maydoni. Ilova tavsifni oddiy matn sifatida
                ko'rsatadi, shuning uchun bu yerda ham HTML formatlash yo'q. */}
            <div className="ap-field-wrap" style={{ marginTop: '32px' }}>
              <label className="ap-label" htmlFor="ap-description">
                Tovar qisqacha tavsifi <span className="ap-required">*</span>
              </label>
              <div className="ap-input-row">
                <textarea
                  id="ap-description"
                  className="ap-textarea"
                  rows={3}
                  maxLength={DESCRIPTION_MAX}
                  placeholder="Tovarning qisqacha tavsifi"
                  value={form.description}
                  onChange={e => set('description', e.target.value)}
                />
              </div>
              <div className="ap-char-count" style={{ color: descTooLong ? '#DC2626' : undefined }}>
                {form.description.length}/{DESCRIPTION_MAX}
              </div>
            </div>
          </div>

          {/* Rasmlar bo'limi */}
          <div className="ap-section">
            <div className="ap-section__header">
              <h2 className="ap-section__title">Rasmlar <span className="ap-required">*</span></h2>
              <span className="ap-muted">Maksimum 10 ta</span>
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              onChange={handlePhotoSelect}
              style={{ display: 'none' }}
            />

            <div className="ap-photos">
              {previews.map((src, i) => (
                <div className="ap-photo" key={i}>
                  <img src={src} alt="" />
                  <button className="ap-photo__remove" onClick={() => removePhoto(i)}>
                    <X size={12} />
                  </button>
                </div>
              ))}
              {photos.length < 10 && (
                <button className="ap-photo-add" onClick={() => fileInputRef.current?.click()}>
                  <ImagePlus size={32} className="ap-photo-add__icon" />
                </button>
              )}
            </div>
          </div>

        </div>

      {/* ── BOSQICH 2: NARX ── */}
      <div className="ap-card fade-up" style={{ marginBottom: "24px" }}>
          <div className="ap-section">
            <div className="ap-section__header">
              <h2 className="ap-section__title">Narx <span className="ap-required">*</span></h2>
            </div>

            <div className="ap-price-grid">
              <div className="ap-field-wrap">
                <label className="ap-label">Asosiy narx (so'm) <span className="ap-required">*</span></label>
                <div className="ap-input-icon-wrap">
                  <input
                    className="ap-input"
                    type="number"
                    placeholder="0"
                    value={form.price}
                    onChange={e => set('price', e.target.value)}
                  />
                  <span className="ap-input-suffix">so'm</span>
                </div>
              </div>
            </div>

            {/* Chegirma turi */}
            <div className="ap-field-wrap">
              <label className="ap-label">Chegirma turi</label>
              <div className="ap-radio-group">
                <label className={`ap-radio ${discountType === 'permanent' ? 'ap-radio--active' : ''}`}>
                  <input
                    type="radio"
                    checked={discountType === 'permanent'}
                    onChange={() => setDiscountType('permanent')}
                  />
                  <Tag size={15} />
                  <span>Doimiy chegirma</span>
                </label>
                <label className={`ap-radio ${discountType === 'temporary' ? 'ap-radio--active' : ''}`}>
                  <input
                    type="radio"
                    checked={discountType === 'temporary'}
                    onChange={() => setDiscountType('temporary')}
                  />
                  <Clock size={15} />
                  <span>Vaqtinchalik chegirma</span>
                </label>
              </div>
            </div>

            {/* Chegirma foizi */}
            <div className="ap-field-wrap">
              <label className="ap-label">Chegirma foizi (%)</label>
              <div className="ap-input-icon-wrap">
                <Percent size={16} className="ap-input-prefix-icon" />
                <input
                  className="ap-input ap-input--icon"
                  type="number"
                  placeholder="0"
                  min={0}
                  max={99}
                  value={form.discount}
                  onChange={e => set('discount', e.target.value)}
                />
              </div>
            </div>

            {/* Vaqtinchalik muddat */}
            {discountType === 'temporary' && (
              <div className="ap-field-wrap fade-up">
                <label className="ap-label">Chegirma muddati</label>
                <div className="ap-duration-grid">
                  {DURATION_OPTIONS.map(opt => (
                    <button
                      key={opt.minutes}
                      className={`ap-duration-btn ${selectedDuration === opt.minutes ? 'ap-duration-btn--active' : ''}`}
                      onClick={() => setSelectedDuration(opt.minutes)}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
                {selectedDuration === -1 && (
                  <input
                    className="ap-input"
                    type="number"
                    placeholder="Daqiqa kiriting (5 ga bo'linishi kerak)"
                    value={customMinutes}
                    onChange={e => setCustomMinutes(e.target.value)}
                  />
                )}
                <label className="ap-checkbox">
                  <input
                    type="checkbox"
                    checked={autoDelete}
                    onChange={e => setAutoDelete(e.target.checked)}
                  />
                  <span>Muddat tugagandan so'ng tovarni o'chirish</span>
                </label>
              </div>
            )}

            {/* Narx ko'rinishi */}
            {priceNum > 0 && (
              <div className="ap-price-preview">
                <div className="ap-price-preview__row">
                  <span>Asl narx:</span>
                  <span className={discountNum > 0 ? 'ap-price-old' : ''}>{priceNum.toLocaleString()} so'm</span>
                </div>
                {discountNum > 0 && (
                  <>
                    <div className="ap-price-preview__row">
                      <span>Chegirma:</span>
                      <span className="ap-price-discount">-{discountNum}%</span>
                    </div>
                    <div className="ap-price-preview__row ap-price-preview__row--final">
                      <span>Yakuniy narx:</span>
                      <span className="ap-price-final">{finalPrice.toLocaleString()} so'm</span>
                    </div>
                  </>
                )}
              </div>
            )}

            {/* Push xabarnoma */}
            <label className="ap-checkbox">
              <input
                type="checkbox"
                checked={sendPush}
                onChange={e => setSendPush(e.target.checked)}
              />
              <span>Foydalanuvchilarga push-xabarnoma yuborish</span>
            </label>
          </div>

        </div>

      {/* ── BOSQICH 3: RAZMER VA XUSUSIYATLAR ── */}
      <div className="ap-card fade-up">
          <div className="ap-section">
            <div className="ap-section__header">
              <h2 className="ap-section__title">Xususiyatlarni tanlash <span className="ap-muted" style={{fontWeight: 400}}>(Majburiy emas, maksimal 5 ta)</span></h2>
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px', padding: '16px', background: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '8px', marginBottom: '8px' }}>
              <div>
                <div style={{ fontWeight: '600', fontSize: '14px', marginBottom: '4px' }}>Tovarlar qanday guruhlanadi</div>
                <div style={{ fontSize: '13px', color: '#64748B' }}>Masalan, O'lcham tavsifli futbolka — XS, S, M, L, XL, 2XL</div>
              </div>
              <div style={{ borderLeft: '1px solid #E2E8F0', paddingLeft: '16px' }}>
                <div style={{ fontWeight: '600', fontSize: '14px', marginBottom: '4px' }}>Har xil turdagi tovarlar bo'lsa</div>
                <div style={{ fontSize: '13px', color: '#64748B' }}>Masalan, telefon uchun g'ilof va telefonni guruhlash mumkin emas</div>
              </div>
              <div style={{ borderLeft: '1px solid #E2E8F0', paddingLeft: '16px' }}>
                <div style={{ fontWeight: '600', fontSize: '14px', marginBottom: '4px' }}>Kerakli tavsif mavjud bo'lmasa</div>
                <div style={{ fontSize: '13px', color: '#64748B' }}>O'zining noyob tavsifini yaratish mumkin</div>
              </div>
            </div>
            <a href="#" style={{ color: '#8B5CF6', fontSize: '13px', textDecoration: 'none', display: 'block', marginBottom: '24px' }}>Yo'riqnomada batafsil</a>

            <div style={{ position: 'relative', marginBottom: '16px' }}>
              <button 
                type="button"
                style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 16px', background: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '8px', cursor: 'pointer', fontSize: '15px', color: '#94A3B8' }}
                onClick={() => setAttrDropdownOpen(!attrDropdownOpen)}
              >
                Xususiyat qo'shish <ChevronDown size={20} />
              </button>
              {attrDropdownOpen && (
                <div style={{ position: 'absolute', top: '100%', left: 0, right: 0, background: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '8px', marginTop: '4px', zIndex: 10, boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}>
                  {ATTR_TYPES.map(attr => (
                    <div 
                      key={attr.id}
                      style={{ padding: '12px 16px', cursor: 'pointer', borderBottom: '1px solid #F1F5F9', background: '#F8FAFC', color: '#334155' }}
                      onMouseEnter={e => e.currentTarget.style.background = '#F1F5F9'}
                      onMouseLeave={e => e.currentTarget.style.background = '#F8FAFC'}
                      onClick={() => { 
                        setAttrDropdownOpen(false); 
                        if (!activeAttributes.includes(attr.id)) setActiveAttributes(prev => [...prev, attr.id]); 
                      }}
                    >
                      {attr.label}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {activeAttributes.map(attr => (
              <div key={attr} style={{ marginBottom: '16px' }}>
                <button 
                  type="button"
                  onClick={() => { setActiveAttrType(attr); setShowAttrModal(true); }}
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '8px 16px', background: '#F1F5F9', border: 'none', borderRadius: '8px', fontSize: '14px', fontWeight: '500', cursor: 'pointer' }}
                >
                  Qo'shish: {attr.toLowerCase()}
                </button>
                <div style={{ marginTop: '8px' }}>
                  <button 
                    type="button"
                    onClick={() => {
                      setActiveAttributes(prev => prev.filter(a => a !== attr))
                      setSelectedAttributes(prev => {
                        const copy = { ...prev }
                        delete copy[attr]
                        return copy
                      })
                    }}
                    style={{ color: '#EF4444', background: '#FEE2E2', border: 'none', borderRadius: '4px', padding: '4px 8px', fontSize: '12px', cursor: 'pointer' }}
                  >
                    O'chirish
                  </button>
                </div>
                
                {(selectedAttributes[attr] || []).length > 0 && (
                  <div style={{ marginTop: '16px', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                    {(selectedAttributes[attr] || []).map(val => {
                      const isColor = attr === 'Rang'
                      const colorInfo = isColor ? AVAILABLE_COLORS.find(a => a.name === val) : null
                      return (
                        <div key={val} style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 12px', background: '#F8FAFC', border: '1px solid #E2E8F0', borderRadius: '8px' }}>
                          {isColor && <div style={{ width: '16px', height: '16px', borderRadius: '50%', background: colorInfo?.hex, border: colorInfo?.hex === '#FFFFFF' ? '1px solid #E2E8F0' : 'none' }}></div>}
                          <span style={{ fontSize: '14px' }}>{val}</span>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            ))}
            
            {showAttrModal && activeAttrType && (
              <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>
                <div style={{ background: '#FFFFFF', width: '100%', maxWidth: '500px', borderRadius: '12px', display: 'flex', flexDirection: 'column', maxHeight: '100%', overflow: 'hidden', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)' }}>
                  {/* Header */}
                  <div style={{ padding: '16px 24px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexShrink: 0 }}>
                    <h3 style={{ fontSize: '18px', fontWeight: '600', margin: 0 }}>Xususiyatlarni tanlash</h3>
                    <button type="button" onClick={() => setShowAttrModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#94A3B8' }}><X size={20}/></button>
                  </div>
                  
                  {/* Search */}
                  <div style={{ padding: '16px 24px', flexShrink: 0, borderBottom: '1px solid #E2E8F0' }}>
                    <div style={{ position: 'relative' }}>
                      <input 
                        type="text" 
                        placeholder="Qidirish" 
                        value={colorSearch}
                        onChange={e => setColorSearch(e.target.value)}
                        style={{ width: '100%', padding: '10px 12px 10px 36px', border: '1px solid #E2E8F0', borderRadius: '8px', outline: 'none', fontSize: '14px' }} 
                      />
                      <Search size={16} color="#94A3B8" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                    </div>
                  </div>

                  {/* List */}
                  <div style={{ overflowY: 'auto', flex: 1, padding: '8px 0', minHeight: 0 }}>
                    {(() => {
                      const isColor = activeAttrType === 'Rang'
                      const attrDef = ATTR_TYPES.find(a => a.id === activeAttrType)
                      const allOptions = isColor ? AVAILABLE_COLORS.map(c => c.name) : (attrDef?.options || [])
                      const filteredOptions = allOptions.filter(o => o.toLowerCase().includes(colorSearch.toLowerCase()))
                      
                      return filteredOptions.map(opt => {
                        const isSelected = (selectedAttributes[activeAttrType] || []).includes(opt)
                        const colorInfo = isColor ? AVAILABLE_COLORS.find(a => a.name === opt) : null
                        
                        return (
                          <label key={opt} style={{ display: 'flex', alignItems: 'center', padding: '10px 24px', cursor: 'pointer', gap: '16px', transition: 'background 0.2s' }} onMouseEnter={e => e.currentTarget.style.background = '#F8FAFC'} onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
                            <div style={{ position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                              <input 
                                type="checkbox" 
                                style={{ width: '20px', height: '20px', cursor: 'pointer', margin: 0, opacity: 0, position: 'absolute' }}
                                checked={isSelected}
                                onChange={e => {
                                  const checked = e.target.checked
                                  setSelectedAttributes(prev => {
                                    const current = prev[activeAttrType] || []
                                    const updated = checked ? [...current, opt] : current.filter(x => x !== opt)
                                    return { ...prev, [activeAttrType]: updated }
                                  })
                                }}
                              />
                              <div style={{ 
                                width: '20px', height: '20px', borderRadius: '6px', 
                                border: isSelected ? 'none' : '2px solid #CBD5E1', 
                                background: isSelected ? '#EF4444' : '#FFFFFF',
                                display: 'flex', alignItems: 'center', justifyContent: 'center'
                              }}>
                                {isSelected && <Check size={14} color="#FFFFFF" strokeWidth={3} />}
                              </div>
                            </div>
                            {isColor && <div style={{ width: '28px', height: '28px', borderRadius: '50%', background: colorInfo?.hex, border: colorInfo?.hex === '#FFFFFF' ? '1px solid #E2E8F0' : 'none', flexShrink: 0 }}></div>}
                            <span style={{ fontSize: '15px', color: '#334155' }}>{opt}</span>
                          </label>
                        )
                      })
                    })()}
                  </div>

                  {/* Footer */}
                  <div style={{ padding: '16px 24px', borderTop: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexShrink: 0 }}>
                    <button type="button" onClick={() => setShowAttrModal(false)} style={{ background: 'none', border: 'none', color: '#64748B', fontWeight: '500', cursor: 'pointer', fontSize: '15px' }}>Bekor qilish</button>
                    <button type="button" onClick={() => setShowAttrModal(false)} style={{ background: '#EF4444', color: '#FFFFFF', border: 'none', padding: '10px 24px', borderRadius: '8px', fontWeight: '600', cursor: 'pointer', fontSize: '15px' }}>Saqlash</button>
                  </div>
                </div>
              </div>
            )}
          </div>

      {/* Yakuniy ko'rinish */}
          <div className="ap-summary">
            <h3 className="ap-summary__title">Tovar xulasasi</h3>
            <div className="ap-summary__row">
              <span>Kategoriya:</span>
              <span>{[cat1, cat2, cat3].filter(Boolean).join(' › ')}</span>
            </div>
            <div className="ap-summary__row">
              <span>Nomi:</span>
              <span>{form.name || '—'}</span>
            </div>
            <div className="ap-summary__row">
              <span>Narx:</span>
              <span>{priceNum > 0 ? `${finalPrice.toLocaleString()} so'm` : '—'}</span>
            </div>
            <div className="ap-summary__row">
              <span>Rasmlar:</span>
              <span>{photos.length} ta</span>
            </div>
          </div>

          <div className="ap-actions ap-actions--row" style={{ justifyContent: 'flex-end' }}>
            <button
              className="ap-btn-submit"
              onClick={handleSubmit}
              disabled={loading}
            >
              {loading
                ? <><Loader2 size={18} className="spin" /> Saqlanmoqda...</>
                : <><CheckCircle2 size={18} /> Saqlash va davom etish</>}
            </button>
          </div>
        </div>
    </div>
  )
}
