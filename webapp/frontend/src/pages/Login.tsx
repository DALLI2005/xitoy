import { useState } from 'react'
import { KeyRound, ShieldCheck, UserCheck, ArrowRight, Lock } from 'lucide-react'
import { api } from '../api'
import type { User } from '../types'

interface LoginProps {
  onSuccess: (user: User) => void
}

export default function Login({ onSuccess }: LoginProps) {
  const [telegramId, setTelegramId] = useState('')
  const [passcode, setPasscode] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      if (!telegramId.trim()) {
        throw new Error('Iltimos, Telegram ID kiriting')
      }
      if (!passcode.trim()) {
        throw new Error('Iltimos, Parol kiriting')
      }
      const res = await api.adminLogin({ 
        telegram_id: telegramId.trim(),
        passcode: passcode.trim()
      })
      onSuccess(res.user)
    } catch (err: any) {
      setError(err.message || 'Kirishda xatolik yuz berdi')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-dvh flex items-center justify-center p-4 relative overflow-hidden" style={{ background: '#09090b' }}>
      {/* Background glow effects */}
      <div
        className="fixed top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 rounded-full pointer-events-none"
        style={{
          background: 'radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, transparent 70%)',
          filter: 'blur(60px)',
        }}
      />
      <div
        className="fixed bottom-1/4 right-1/4 w-80 h-80 rounded-full pointer-events-none"
        style={{
          background: 'radial-gradient(circle, rgba(168, 85, 247, 0.12) 0%, transparent 70%)',
          filter: 'blur(60px)',
        }}
      />

      <div className="w-full max-w-md relative z-10">
        {/* Logo / Header */}
        <div className="text-center mb-8">
          <div
            className="w-16 h-16 rounded-2xl mx-auto mb-4 flex items-center justify-center shadow-xl transition-transform duration-300 hover:scale-105"
            style={{
              background: 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)',
              boxShadow: '0 8px 32px rgba(99, 102, 241, 0.3)',
            }}
          >
            <ShieldCheck size={32} className="text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">
            Dalli Shop Admin Panel
          </h1>
          <p className="text-sm mt-1" style={{ color: 'var(--fg-muted, #a1a1aa)' }}>
            Tizimga kirish uchun login ma'lumotlarini kiriting
          </p>
        </div>

        {/* Card */}
        <div
          className="p-8 rounded-3xl backdrop-blur-xl border transition-all duration-300"
          style={{
            background: 'rgba(18, 18, 24, 0.75)',
            borderColor: 'rgba(255, 255, 255, 0.08)',
            boxShadow: '0 20px 50px rgba(0, 0, 0, 0.5)',
          }}
        >
          {error && (
            <div
              className="p-3.5 rounded-xl mb-5 text-xs font-medium border flex items-center gap-2"
              style={{
                background: 'rgba(239, 68, 68, 0.1)',
                borderColor: 'rgba(239, 68, 68, 0.2)',
                color: '#f87171',
              }}
            >
              <Lock size={14} />
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-xs font-medium mb-2 text-zinc-300">
                Telegram ID ingiz
              </label>
              <input
                type="text"
                value={telegramId}
                onChange={(e) => setTelegramId(e.target.value)}
                placeholder="Masalan: 5049583350"
                required
                className="w-full px-4 py-3 rounded-xl text-sm outline-none transition-all border text-white placeholder-zinc-500"
                style={{
                  background: 'rgba(255, 255, 255, 0.03)',
                  borderColor: 'rgba(255, 255, 255, 0.1)',
                }}
              />
            </div>
            
            <div>
              <label className="block text-xs font-medium mb-2 text-zinc-300">
                Parolingiz (Superadmin yoki bot orqali berilgan)
              </label>
              <div className="relative">
                <input
                  type="password"
                  value={passcode}
                  onChange={(e) => setPasscode(e.target.value)}
                  placeholder="Parol kiriting"
                  required
                  className="w-full px-4 py-3 rounded-xl text-sm outline-none transition-all border text-white placeholder-zinc-500"
                  style={{
                    background: 'rgba(255, 255, 255, 0.03)',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                  }}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 rounded-xl text-sm font-semibold text-white flex items-center justify-center gap-2 cursor-pointer transition-all duration-200 hover:opacity-90 active:scale-[0.99]"
              style={{
                background: 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)',
                boxShadow: '0 4px 20px rgba(99, 102, 241, 0.35)',
              }}
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <span>Tizimga Kirish</span>
                  <ArrowRight size={16} />
                </>
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
