import { useEffect, useState } from 'react'
import { Users, Package, Plus, Pencil, Trash2, PauseCircle, PlayCircle, CheckCircle2, AlertCircle, Loader2, X, Bell, BellOff, Send } from 'lucide-react'
import { api } from '../api'
import { hapticSuccess, hapticError } from '../telegram'
import type { Admin } from '../types'

type Channel = { id: number; channel_id: string; label: string; enabled: number }

const ALL_CATS = ['Kiyim', 'Elektronika', 'Poyabzal', 'Aksessuar', 'Sport', 'Uy uchun', 'Boshqa']

export default function AdminPanel() {
  const [admins, setAdmins]   = useState<Admin[]>([])
  const [stats, setStats]     = useState<any>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')

  const [showForm, setShowForm]   = useState(false)
  const [newId, setNewId]         = useState('')
  const [newName, setNewName]     = useState('')
  const [newCats, setNewCats]     = useState<string[]>([])
  const [saving, setSaving]       = useState(false)
  const [formErr, setFormErr]     = useState('')

  const [editId, setEditId]     = useState<number | null>(null)
  const [editCats, setEditCats] = useState<string[]>([])

  const [notifEnabled, setNotifEnabled] = useState(true)
  const [notifLoading, setNotifLoading] = useState(false)

  // Channels
  const [channels, setChannels]         = useState<Channel[]>([])
  const [chInput, setChInput]           = useState('')
  const [chLabel, setChLabel]           = useState('')
  const [chAdding, setChAdding]         = useState(false)
  const [chErr, setChErr]               = useState('')
  const [showChForm, setShowChForm]     = useState(false)

  async function load() {
    setLoading(true)
    try {
      const [a, s, settings] = await Promise.all([
        api.admins(), api.stats(), api.getNotificationSettings()
      ])
      setAdmins(a); setStats(s)
      setNotifEnabled(settings.marketing_notifications_enabled)
    } catch (e: any) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
    // Kanallar alohida yuklanadi — boshqa xatolar ta'sir qilmasin
    try {
      const chs = await api.getChannels()
      setChannels(chs)
    } catch {
      // kanallar yuklanmasa UI bo'sh qoladi, lekin crash bo'lmaydi
    }
  }

  async function addChannel() {
    setChErr('')
    const cid = chInput.trim()
    if (!cid) return setChErr('Kanal ID yoki username kiriting')
    setChAdding(true)
    try {
      const res = await api.addChannel({ channel_id: cid, label: chLabel.trim() })
      hapticSuccess()
      setChannels(prev => [...prev, { id: Date.now(), channel_id: cid, label: res.label, enabled: 1 }])
      setChInput(''); setChLabel(''); setShowChForm(false)
      await load()
    } catch (e: any) {
      hapticError(); setChErr(e.message)
    } finally {
      setChAdding(false)
    }
  }

  async function toggleChannel(ch: Channel) {
    await api.toggleChannel(ch.id)
    setChannels(prev => prev.map(c => c.id === ch.id ? { ...c, enabled: c.enabled ? 0 : 1 } : c))
  }

  async function deleteChannel(ch: Channel) {
    await api.deleteChannel(ch.id)
    setChannels(prev => prev.filter(c => c.id !== ch.id))
    hapticSuccess()
  }

  async function toggleNotif() {
    setNotifLoading(true)
    try {
      await api.toggleNotifications(!notifEnabled)
      setNotifEnabled(v => !v)
      hapticSuccess()
    } catch (e: any) {
      hapticError()
    } finally {
      setNotifLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  async function addAdmin() {
    setFormErr('')
    if (!newId.trim() || isNaN(+newId)) return setFormErr('Telegram ID raqam bo\'lishi kerak')
    if (!newName.trim())                return setFormErr('Ism kiriting')
    if (!newCats.length)                return setFormErr('Kamida 1 ta kategoriya tanlang')
    setSaving(true)
    try {
      await api.createAdmin({ telegram_id: +newId, name: newName.trim(), categories: newCats })
      hapticSuccess()
      setShowForm(false); setNewId(''); setNewName(''); setNewCats([])
      await load()
    } catch (e: any) {
      hapticError(); setFormErr(e.message)
    } finally { setSaving(false) }
  }

  function toggleCat(cat: string, list: string[], set: (v: string[]) => void) {
    set(list.includes(cat) ? list.filter(c => c !== cat) : [...list, cat])
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Loader2 size={20} style={{ color: 'var(--accent)' }} className="animate-spin" />
      </div>
    )
  }

  return (
    <div className="px-4 pt-5 pb-6">
      <h1 className="text-lg font-semibold mb-5" style={{ color: 'var(--fg)' }}>Admin boshqaruvi</h1>

      {error && (
        <div className="glass p-3 mb-4 flex items-center gap-2" style={{ borderColor: 'rgba(239,68,68,0.3)' }}>
          <AlertCircle size={15} style={{ color: 'var(--error)', flexShrink: 0 }} />
          <span className="text-sm" style={{ color: 'var(--error)' }}>{error}</span>
        </div>
      )}

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 gap-3 mb-5">
          <div className="glass p-4">
            <div
              className="w-9 h-9 rounded-xl flex items-center justify-center mb-3"
              style={{ background: 'rgba(99,102,241,0.12)' }}
            >
              <Package size={18} style={{ color: 'var(--accent)' }} />
            </div>
            <p className="text-2xl font-bold" style={{ color: 'var(--fg)', fontVariantNumeric: 'tabular-nums' }}>
              {stats.total_products}
            </p>
            <p className="text-xs mt-0.5" style={{ color: 'var(--fg-muted)' }}>Jami tovar</p>
          </div>
          <div className="glass p-4">
            <div
              className="w-9 h-9 rounded-xl flex items-center justify-center mb-3"
              style={{ background: 'rgba(34,197,94,0.1)' }}
            >
              <Users size={18} style={{ color: 'var(--success)' }} />
            </div>
            <p className="text-2xl font-bold" style={{ color: 'var(--fg)', fontVariantNumeric: 'tabular-nums' }}>
              {stats.active_admins}
            </p>
            <p className="text-xs mt-0.5" style={{ color: 'var(--fg-muted)' }}>Faol admin</p>
          </div>

          {stats.by_admin?.length > 0 && (
            <div className="glass p-4 col-span-2">
              <p className="text-xs font-medium mb-3" style={{ color: 'var(--fg-muted)' }}>FAOLLIK</p>
              <div className="flex flex-col gap-2">
                {stats.by_admin.map(([name, count]: [string, number]) => (
                  <div key={name} className="flex items-center justify-between">
                    <span className="text-sm truncate" style={{ color: 'var(--fg)' }}>{name}</span>
                    <span
                      className="text-xs font-semibold px-2 py-0.5 rounded-full ml-3 flex-shrink-0"
                      style={{ background: 'rgba(99,102,241,0.12)', color: 'var(--accent-hover)', fontVariantNumeric: 'tabular-nums' }}
                    >
                      {count} ta
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Marketing bildirishnomalar toggle */}
      <div
        className="glass p-4 mb-5 flex items-center justify-between"
        style={{ borderColor: notifEnabled ? 'rgba(34,197,94,0.2)' : 'rgba(239,68,68,0.2)' }}
      >
        <div className="flex items-center gap-3">
          <div
            className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0"
            style={{ background: notifEnabled ? 'rgba(34,197,94,0.1)' : 'rgba(239,68,68,0.1)' }}
          >
            {notifEnabled
              ? <Bell size={17} style={{ color: '#4ade80' }} />
              : <BellOff size={17} style={{ color: '#f87171' }} />}
          </div>
          <div>
            <p className="text-sm font-semibold" style={{ color: 'var(--fg)' }}>
              Marketing bildirishnomalari
            </p>
            <p className="text-xs mt-0.5" style={{ color: 'var(--fg-muted)' }}>
              Savat eslatmasi & chegirma broadcast
            </p>
          </div>
        </div>
        <button
          onClick={toggleNotif}
          disabled={notifLoading}
          className="relative w-11 h-6 rounded-full transition-all duration-200 cursor-pointer disabled:opacity-50 flex-shrink-0"
          style={{ background: notifEnabled ? '#22c55e' : 'rgba(255,255,255,0.1)' }}
          aria-label="Toggle marketing notifications"
        >
          <span
            className="absolute top-0.5 w-5 h-5 rounded-full transition-all duration-200"
            style={{
              background: 'white',
              left: notifEnabled ? 'calc(100% - 1.375rem)' : '0.125rem',
              boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
            }}
          />
        </button>
      </div>

      {/* ── Telegram kanallar ── */}
      <div className="mb-5">
        <div className="flex items-center justify-between mb-3">
          <p className="text-sm font-semibold" style={{ color: 'var(--fg)' }}>
            Telegram kanallar
          </p>
          <button
            onClick={() => { setShowChForm(v => !v); setChErr('') }}
            className="flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-xl cursor-pointer transition-all active:scale-95"
            style={{ background: 'rgba(99,102,241,0.1)', color: 'var(--accent-hover)', border: '1px solid rgba(99,102,241,0.2)' }}
          >
            <Plus size={13} /> Kanal qo'shish
          </button>
        </div>

        {showChForm && (
          <div className="glass p-4 mb-3 fade-up" style={{ borderColor: 'rgba(99,102,241,0.2)' }}>
            <div className="flex items-center justify-between mb-3">
              <p className="text-sm font-semibold" style={{ color: 'var(--fg)' }}>Yangi kanal</p>
              <button onClick={() => setShowChForm(false)} style={{ color: 'var(--fg-muted)' }}><X size={16} /></button>
            </div>
            {chErr && <p className="text-xs mb-2" style={{ color: 'var(--error)' }}>{chErr}</p>}
            <div className="flex flex-col gap-2">
              <input
                className="field"
                placeholder="Kanal ID yoki @username"
                value={chInput}
                onChange={e => setChInput(e.target.value)}
              />
              <input
                className="field"
                placeholder="Nom (ixtiyoriy)"
                value={chLabel}
                onChange={e => setChLabel(e.target.value)}
              />
              <button
                onClick={addChannel}
                disabled={chAdding}
                className="btn-primary flex items-center justify-center gap-2"
              >
                {chAdding ? <><Loader2 size={14} className="animate-spin" /> Tekshirilmoqda…</> : <><Send size={14} /> Qo'shish</>}
              </button>
            </div>
          </div>
        )}

        {channels.length === 0 && !showChForm && (
          <div
            className="glass p-4 text-center"
            style={{ borderColor: 'rgba(255,255,255,0.06)' }}
          >
            <p className="text-sm" style={{ color: 'var(--fg-muted)' }}>
              Hali kanal qo'shilmagan
            </p>
            <p className="text-xs mt-1" style={{ color: 'var(--fg-muted)', opacity: 0.6 }}>
              Bot qo'shilgan kanalga tovarlar avtomatik yuboriladi
            </p>
          </div>
        )}

        <div className="flex flex-col gap-2">
          {channels.map(ch => (
            <div
              key={ch.id}
              className="glass p-3.5 flex items-center justify-between gap-3"
              style={{ opacity: ch.enabled ? 1 : 0.5, borderColor: ch.enabled ? 'rgba(34,197,94,0.15)' : 'var(--border)' }}
            >
              <div className="flex items-center gap-2.5 min-w-0">
                <div
                  className="w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0"
                  style={{ background: ch.enabled ? 'rgba(34,197,94,0.1)' : 'rgba(255,255,255,0.05)' }}
                >
                  <Send size={14} style={{ color: ch.enabled ? '#4ade80' : 'var(--fg-muted)' }} />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-medium truncate" style={{ color: 'var(--fg)' }}>{ch.label || ch.channel_id}</p>
                  <p className="text-xs truncate" style={{ color: 'var(--fg-muted)' }}>{ch.channel_id}</p>
                </div>
              </div>
              <div className="flex items-center gap-2 flex-shrink-0">
                <button
                  onClick={() => toggleChannel(ch)}
                  className="relative w-10 h-5 rounded-full transition-all duration-200 cursor-pointer flex-shrink-0"
                  style={{ background: ch.enabled ? '#22c55e' : 'rgba(255,255,255,0.1)' }}
                >
                  <span
                    className="absolute top-0.5 w-4 h-4 rounded-full transition-all duration-200"
                    style={{
                      background: 'white',
                      left: ch.enabled ? 'calc(100% - 1.125rem)' : '0.125rem',
                      boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
                    }}
                  />
                </button>
                <button
                  onClick={() => deleteChannel(ch)}
                  className="w-7 h-7 rounded-lg flex items-center justify-center cursor-pointer transition-all active:scale-90"
                  style={{ background: 'rgba(239,68,68,0.08)', color: '#f87171' }}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Add admin button */}
      <button
        onClick={() => { setShowForm(true); setFormErr('') }}
        className="w-full flex items-center justify-center gap-2 py-3.5 rounded-2xl cursor-pointer transition-all duration-200 active:scale-97 mb-4"
        style={{
          background: 'rgba(99,102,241,0.1)',
          border: '1px solid rgba(99,102,241,0.25)',
          color: 'var(--accent-hover)',
          fontWeight: 600,
          fontSize: 15,
        }}
      >
        <Plus size={18} />
        Admin qo'shish
      </button>

      {/* New admin form */}
      {showForm && (
        <div className="glass p-4 mb-4 fade-up" style={{ borderColor: 'rgba(99,102,241,0.2)' }}>
          <div className="flex items-center justify-between mb-4">
            <p className="font-semibold text-sm" style={{ color: 'var(--fg)' }}>Yangi admin</p>
            <button onClick={() => setShowForm(false)} className="cursor-pointer" style={{ color: 'var(--fg-muted)' }}>
              <X size={18} />
            </button>
          </div>

          {formErr && (
            <p className="text-xs mb-3" style={{ color: 'var(--error)' }}>{formErr}</p>
          )}

          <div className="flex flex-col gap-3">
            <input
              className="field"
              type="number"
              inputMode="numeric"
              value={newId}
              onChange={e => setNewId(e.target.value)}
              placeholder="Telegram ID (raqam)"
            />
            <input
              className="field"
              type="text"
              value={newName}
              onChange={e => setNewName(e.target.value)}
              placeholder="Ism (masalan: Sardor)"
            />

            <div>
              <p className="text-xs mb-2" style={{ color: 'var(--fg-muted)' }}>Kategoriyalar:</p>
              <div className="flex flex-wrap gap-1.5">
                {ALL_CATS.map(cat => (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => toggleCat(cat, newCats, setNewCats)}
                    className={`chip ${newCats.includes(cat) ? 'active' : ''}`}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            </div>

            <button
              onClick={addAdmin}
              disabled={saving}
              className="btn-primary"
              style={{ marginTop: 4 }}
            >
              {saving ? (
                <span className="flex items-center justify-center gap-2">
                  <Loader2 size={16} className="animate-spin" /> Saqlanmoqda…
                </span>
              ) : 'Saqlash'}
            </button>
          </div>
        </div>
      )}

      {/* Admin list */}
      <div className="flex flex-col gap-3">
        {admins.map(admin => (
          <div
            key={admin.telegram_id}
            className="glass p-4 fade-up"
            style={{ opacity: admin.active ? 1 : 0.5 }}
          >
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-semibold text-sm" style={{ color: 'var(--fg)' }}>{admin.name}</span>
                  {admin.is_superadmin && (
                    <span
                      className="text-xs px-2 py-0.5 rounded-full font-medium"
                      style={{ background: 'rgba(245,158,11,0.12)', color: '#fbbf24' }}
                    >
                      superadmin
                    </span>
                  )}
                  {!admin.active && (
                    <span
                      className="text-xs px-2 py-0.5 rounded-full font-medium"
                      style={{ background: 'rgba(239,68,68,0.1)', color: '#f87171' }}
                    >
                      nofaol
                    </span>
                  )}
                </div>
                <p className="text-xs mt-0.5" style={{ color: 'var(--fg-muted)' }}>
                  ID: {admin.telegram_id}
                </p>
              </div>

              {!admin.is_superadmin && (
                <div className="flex gap-1.5 flex-shrink-0">
                  <button
                    onClick={() => { setEditId(admin.telegram_id); setEditCats([...admin.categories]) }}
                    className="w-8 h-8 rounded-xl flex items-center justify-center cursor-pointer transition-all active:scale-90"
                    style={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--fg-muted)' }}
                    aria-label="Tahrirlash"
                  >
                    <Pencil size={13} />
                  </button>
                  <button
                    onClick={async () => {
                      await api.updateAdmin(admin.telegram_id, { active: !admin.active })
                      load()
                    }}
                    className="w-8 h-8 rounded-xl flex items-center justify-center cursor-pointer transition-all active:scale-90"
                    style={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--fg-muted)' }}
                    aria-label={admin.active ? 'To\'xtatish' : 'Yoqish'}
                  >
                    {admin.active ? <PauseCircle size={13} /> : <PlayCircle size={13} />}
                  </button>
                  <button
                    onClick={async () => {
                      if (confirm('O\'chirishni tasdiqlaysizmi?')) { await api.deleteAdmin(admin.telegram_id); load() }
                    }}
                    className="w-8 h-8 rounded-xl flex items-center justify-center cursor-pointer transition-all active:scale-90"
                    style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.15)', color: '#f87171' }}
                    aria-label="O'chirish"
                  >
                    <Trash2 size={13} />
                  </button>
                </div>
              )}
            </div>

            {/* Categories or edit */}
            {editId === admin.telegram_id ? (
              <div className="mt-3">
                <div className="flex flex-wrap gap-1.5 mb-3">
                  {ALL_CATS.map(cat => (
                    <button
                      key={cat}
                      type="button"
                      onClick={() => toggleCat(cat, editCats, setEditCats)}
                      className={`chip ${editCats.includes(cat) ? 'active' : ''}`}
                    >
                      {cat}
                    </button>
                  ))}
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={async () => {
                      await api.updateAdmin(admin.telegram_id, { categories: editCats })
                      hapticSuccess(); setEditId(null); load()
                    }}
                    className="flex items-center gap-1.5 px-3 py-2 rounded-xl cursor-pointer transition-all active:scale-95"
                    style={{ background: 'rgba(34,197,94,0.12)', color: 'var(--success)', fontSize: 13, fontWeight: 500 }}
                  >
                    <CheckCircle2 size={14} /> Saqlash
                  </button>
                  <button
                    onClick={() => setEditId(null)}
                    className="px-3 py-2 rounded-xl cursor-pointer transition-all active:scale-95"
                    style={{ background: 'var(--surface)', color: 'var(--fg-muted)', fontSize: 13 }}
                  >
                    Bekor
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex flex-wrap gap-1.5 mt-2.5">
                {admin.is_superadmin ? (
                  <span className="chip">Barcha kategoriyalar</span>
                ) : admin.categories.map(cat => (
                  <span key={cat} className="chip">{cat}</span>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
