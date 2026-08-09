import { useEffect, useState } from 'react'
import { Users, Search, Trash2, Loader2, AlertCircle, Phone, KeyRound, Wand2, Copy, CheckCircle2, X } from 'lucide-react'
import { api } from '../api'
import { hapticSuccess, hapticError } from '../telegram'
import type { AppUser } from '../types'

// Chalkash bo'lishi mumkin bo'lgan belgilar (0/O, 1/I/l) chiqarib tashlangan
const PASSWORD_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789'
function generatePassword(length = 10): string {
  const arr = new Uint32Array(length)
  crypto.getRandomValues(arr)
  return Array.from(arr, n => PASSWORD_CHARS[n % PASSWORD_CHARS.length]).join('')
}

export default function AppUsers() {
  const [users, setUsers]     = useState<AppUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')
  const [search, setSearch]   = useState('')

  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)

  // Parolni tiklash
  const [resetUser, setResetUser]     = useState<AppUser | null>(null)
  const [resetPassword, setResetPassword] = useState('')
  const [resetSaving, setResetSaving] = useState(false)
  const [resetError, setResetError]   = useState('')
  const [resetDone, setResetDone]     = useState(false)  // true bo'lsa — parol o'rnatilgan, bir martalik ko'rsatilmoqda
  const [copied, setCopied]           = useState(false)

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

  function openReset(u: AppUser) {
    setResetUser(u)
    setResetPassword('')
    setResetError('')
    setResetDone(false)
    setCopied(false)
  }

  function closeReset() {
    setResetUser(null)
    setResetPassword('')  // parol matn ko'rinishida hech qayerda saqlanmaydi — modal yopilganda tozalanadi
    setResetError('')
    setResetDone(false)
    setCopied(false)
  }

  async function saveResetPassword() {
    if (!resetUser) return
    setResetError('')
    if (resetPassword.length < 6) return setResetError('Parol kamida 6 belgidan iborat bo\'lishi kerak')
    setResetSaving(true)
    try {
      await api.resetAppUserPassword(resetUser.user_id, resetPassword)
      hapticSuccess()
      setResetDone(true)
    } catch (e: any) {
      hapticError()
      setResetError(e.message)
    } finally {
      setResetSaving(false)
    }
  }

  async function copyPassword() {
    try {
      await navigator.clipboard.writeText(resetPassword)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // clipboard ruxsati yo'q bo'lishi mumkin — jim o'tkazamiz, parol ekranda ko'rinib turibdi
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
            <div className="flex gap-1.5 flex-shrink-0">
              <button
                onClick={() => openReset(u)}
                className="w-8 h-8 rounded-xl flex items-center justify-center cursor-pointer transition-all active:scale-90"
                style={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--fg-muted)' }}
                aria-label="Parolni tiklash"
                title="Parolni tiklash"
              >
                <KeyRound size={13} />
              </button>
              <button
                onClick={() => setDeleteId(u.user_id)}
                className="w-8 h-8 rounded-xl flex items-center justify-center cursor-pointer transition-all active:scale-90"
                style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.15)', color: '#f87171' }}
                aria-label="O'chirish"
                title="O'chirish"
              >
                <Trash2 size={13} />
              </button>
            </div>
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

      {/* Parolni tiklash modali */}
      {resetUser && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center px-4"
          style={{ background: 'rgba(0,0,0,0.7)' }}
          onClick={() => !resetSaving && closeReset()}
        >
          <div
            className="w-full max-w-sm rounded-2xl p-6 flex flex-col gap-4 shadow-2xl"
            style={{ background: 'var(--surface)', border: '1px solid var(--border)' }}
            onClick={e => e.stopPropagation()}
          >
            {!resetDone ? (
              <>
                <div className="flex items-center justify-between">
                  <h3 className="font-bold text-lg" style={{ color: 'var(--fg)' }}>Parolni tiklash</h3>
                  <button onClick={closeReset} disabled={resetSaving} className="cursor-pointer" style={{ color: 'var(--fg-muted)' }}>
                    <X size={18} />
                  </button>
                </div>
                <p className="text-sm" style={{ color: 'var(--fg-muted)' }}>
                  <span style={{ color: 'var(--fg)', fontWeight: 600 }}>{resetUser.fullname}</span> ({resetUser.phone}) uchun yangi parol o'rnatiladi. Eski parol talab qilinmaydi.
                </p>

                {resetError && (
                  <p className="text-xs" style={{ color: 'var(--error)' }}>{resetError}</p>
                )}

                <div className="flex gap-2">
                  <input
                    className="field flex-1"
                    type="text"
                    value={resetPassword}
                    onChange={e => setResetPassword(e.target.value)}
                    placeholder="Yangi parol (kamida 6 belgi)"
                    disabled={resetSaving}
                  />
                  <button
                    onClick={() => setResetPassword(generatePassword())}
                    disabled={resetSaving}
                    className="px-3 rounded-xl cursor-pointer transition-all active:scale-95 flex items-center gap-1.5 flex-shrink-0"
                    style={{ background: 'rgba(99,102,241,0.1)', color: 'var(--accent-hover)', border: '1px solid rgba(99,102,241,0.2)', fontSize: 13, fontWeight: 500 }}
                    title="Tasodifiy parol yaratish"
                  >
                    <Wand2 size={14} /> Yaratish
                  </button>
                </div>

                <button
                  onClick={saveResetPassword}
                  disabled={resetSaving || resetPassword.length < 6}
                  className="btn-primary flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {resetSaving ? <><Loader2 size={16} className="animate-spin" /> Saqlanmoqda…</> : "Saqlash"}
                </button>
              </>
            ) : (
              <>
                <div
                  className="w-12 h-12 rounded-full flex items-center justify-center mx-auto"
                  style={{ background: 'rgba(34,197,94,0.1)', color: '#4ade80' }}
                >
                  <CheckCircle2 size={24} />
                </div>
                <h3 className="font-bold text-center text-lg" style={{ color: 'var(--fg)' }}>
                  Parol muvaffaqiyatli o'zgartirildi
                </h3>
                <p className="text-xs text-center px-2" style={{ color: 'var(--fg-muted)' }}>
                  Bu parol faqat hozir ko'rsatiladi va hech qayerda saqlanmaydi — mijozga nusxalab yuboring.
                </p>
                <div className="flex items-center gap-2">
                  <div
                    className="flex-1 px-3 py-2.5 rounded-xl font-mono text-sm text-center select-all"
                    style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--fg)', letterSpacing: 1 }}
                  >
                    {resetPassword}
                  </div>
                  <button
                    onClick={copyPassword}
                    className="w-10 h-10 rounded-xl flex items-center justify-center cursor-pointer transition-all active:scale-90 flex-shrink-0"
                    style={{
                      background: copied ? 'rgba(34,197,94,0.12)' : 'rgba(99,102,241,0.1)',
                      color: copied ? '#4ade80' : 'var(--accent-hover)',
                      border: `1px solid ${copied ? 'rgba(34,197,94,0.25)' : 'rgba(99,102,241,0.2)'}`,
                    }}
                    aria-label="Nusxalash"
                    title="Nusxalash"
                  >
                    {copied ? <CheckCircle2 size={16} /> : <Copy size={16} />}
                  </button>
                </div>
                <button
                  onClick={closeReset}
                  className="btn-primary"
                  style={{ marginTop: 4 }}
                >
                  Yopish
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
