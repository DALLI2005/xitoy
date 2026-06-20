export const tg = (window as any).Telegram?.WebApp

export function setup() {
  if (!tg) return
  tg.ready()
  tg.expand()
}

export function getInitData(): string {
  return tg?.initData || ''
}

export function getUser() {
  return tg?.initDataUnsafe?.user || null
}

export function isTelegram(): boolean {
  return !!tg?.initData
}

export function haptic(type: 'light' | 'medium' | 'heavy' = 'light') {
  tg?.HapticFeedback?.impactOccurred(type)
}

export function hapticSuccess() {
  tg?.HapticFeedback?.notificationOccurred('success')
}

export function hapticError() {
  tg?.HapticFeedback?.notificationOccurred('error')
}

export function showAlert(msg: string) {
  if (tg) tg.showAlert(msg)
  else alert(msg)
}

export function themeColor(key: string, fallback: string): string {
  return tg?.themeParams?.[key] || fallback
}
