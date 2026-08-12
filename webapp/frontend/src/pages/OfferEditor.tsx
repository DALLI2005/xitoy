import { useEffect, useState } from 'react'
import { FileText, Loader2, AlertCircle, Save, History } from 'lucide-react'
import { api } from '../api'
import { hapticSuccess, hapticError } from '../telegram'
import type { OfferDocument } from '../types'

export default function OfferEditor() {
  const [current, setCurrent] = useState<OfferDocument | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')

  const [version, setVersion] = useState('')
  const [title, setTitle]     = useState('')
  const [content, setContent] = useState('')

  const [showConfirm, setShowConfirm] = useState(false)
  const [saving, setSaving]   = useState(false)
  const [saveError, setSaveError] = useState('')
  const [saved, setSaved]     = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    api.getOffer()
      .then(res => {
        if (cancelled) return
        setCurrent(res)
        setVersion(res.version)
        setTitle(res.title)
        setContent(res.content)
      })
      .catch(e => { if (!cancelled) setError(e.message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [])

  const isDirty = current != null && (
    version.trim() !== current.version ||
    title.trim() !== current.title ||
    content.trim() !== current.content
  )

  async function confirmSave() {
    setSaveError('')
    if (!version.trim() || !title.trim() || !content.trim()) {
      setSaveError("Versiya, sarlavha va matn to'ldirilishi shart")
      return
    }
    setSaving(true)
    try {
      const res = await api.updateOffer({ version: version.trim(), title: title.trim(), content: content.trim() })
      setCurrent(res)
      hapticSuccess()
      setSaved(true)
      setShowConfirm(false)
      setTimeout(() => setSaved(false), 3000)
    } catch (e: any) {
      hapticError()
      setSaveError(e.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-32">
        <Loader2 size={20} style={{ color: 'var(--accent)' }} className="animate-spin" />
      </div>
    )
  }

  return (
    <div className="px-4 pt-5 pb-6">
      <div className="flex items-center justify-between mb-5">
        <h1 className="text-lg font-semibold flex items-center gap-2" style={{ color: 'var(--fg)' }}>
          <FileText size={18} /> Ommaviy oferta
        </h1>
        {current && (
          <span
            className="text-xs px-2.5 py-1 rounded-full font-semibold flex items-center gap-1"
            style={{ background: 'rgba(99,102,241,0.12)', color: 'var(--accent-hover)' }}
          >
            <History size={12} /> Joriy versiya: {current.version}
          </span>
        )}
      </div>

      {error && (
        <div className="glass p-3 mb-4 flex items-center gap-2" style={{ borderColor: 'rgba(239,68,68,0.3)' }}>
          <AlertCircle size={15} style={{ color: 'var(--error)', flexShrink: 0 }} />
          <span className="text-sm" style={{ color: 'var(--error)' }}>{error}</span>
        </div>
      )}

      {saved && (
        <div className="glass p-3 mb-4 flex items-center gap-2" style={{ borderColor: 'rgba(34,197,94,0.3)' }}>
          <span className="text-sm" style={{ color: '#4ade80' }}>Yangi versiya saqlandi.</span>
        </div>
      )}

      {!error && current && (
        <div className="glass p-4 flex flex-col gap-4">
          <div>
            <label className="text-xs font-semibold mb-1 block" style={{ color: 'var(--fg-muted)' }}>Versiya</label>
            <input
              type="text"
              value={version}
              onChange={e => setVersion(e.target.value)}
              className="field w-full"
              placeholder="masalan: 1.0"
              disabled={saving}
            />
          </div>

          <div>
            <label className="text-xs font-semibold mb-1 block" style={{ color: 'var(--fg-muted)' }}>Sarlavha</label>
            <input
              type="text"
              value={title}
              onChange={e => setTitle(e.target.value)}
              className="field w-full"
              disabled={saving}
            />
          </div>

          <div>
            <label className="text-xs font-semibold mb-1 block" style={{ color: 'var(--fg-muted)' }}>Matn</label>
            <textarea
              value={content}
              onChange={e => setContent(e.target.value)}
              className="field w-full font-mono text-sm"
              style={{ minHeight: 360, lineHeight: 1.6, resize: 'vertical' }}
              disabled={saving}
            />
          </div>

          <button
            onClick={() => setShowConfirm(true)}
            disabled={saving || !isDirty}
            className="btn-primary flex items-center justify-center gap-2 disabled:opacity-50"
          >
            <Save size={16} /> Saqlash
          </button>
        </div>
      )}

      {/* Tasdiqlash modali */}
      {showConfirm && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center px-4"
          style={{ background: 'rgba(0,0,0,0.7)' }}
          onClick={() => !saving && setShowConfirm(false)}
        >
          <div
            className="w-full max-w-sm rounded-2xl p-6 flex flex-col gap-4 shadow-2xl"
            style={{ background: 'var(--surface)', border: '1px solid var(--border)' }}
            onClick={e => e.stopPropagation()}
          >
            <div
              className="w-12 h-12 rounded-full flex items-center justify-center mx-auto"
              style={{ background: 'rgba(99,102,241,0.1)', color: 'var(--accent-hover)' }}
            >
              <History size={22} />
            </div>
            <h3 className="font-bold text-center text-lg" style={{ color: 'var(--fg)' }}>Yangi versiya yaratiladi</h3>
            <p className="text-sm text-center px-2" style={{ color: 'var(--fg-muted)' }}>
              Eski versiya ({current?.version}) o'chirilmaydi — tarixda saqlanib qoladi. Ilova va sayt darhol yangi
              matnni ko'rsata boshlaydi.
            </p>

            {saveError && (
              <p className="text-xs text-center" style={{ color: 'var(--error)' }}>{saveError}</p>
            )}

            <div className="flex gap-3 mt-2">
              <button
                onClick={() => setShowConfirm(false)}
                disabled={saving}
                className="flex-1 py-2.5 rounded-xl text-sm font-bold cursor-pointer transition-colors hover:bg-white/5"
                style={{ background: 'var(--bg)', color: 'var(--fg)', border: '1px solid var(--border)' }}
              >
                Bekor qilish
              </button>
              <button
                onClick={confirmSave}
                disabled={saving}
                className="flex-1 py-2.5 rounded-xl text-sm font-bold cursor-pointer transition-opacity hover:opacity-80 flex justify-center items-center"
                style={{ background: 'var(--accent)', color: '#fff' }}
              >
                {saving ? <Loader2 size={18} className="animate-spin" /> : 'Tasdiqlash'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
