import { getInitData } from './telegram'

const BASE = '/api'

function headers(): HeadersInit {
  return {
    'Content-Type': 'application/json',
    'x-init-data': getInitData(),
  }
}

function authHeaders(): HeadersInit {
  return { 'x-init-data': getInitData() }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, init)
  if (!res.ok) {
    const err = await res.json().catch(() => ({ detail: res.statusText }))
    throw new Error(err.detail || 'Xatolik')
  }
  return res.json()
}

export const api = {
  me: () => request<import('./types').User>('/me', { headers: headers() }),

  categories: () => request<string[]>('/categories', { headers: headers() }),

  products: () => request<import('./types').Product[]>('/products', { headers: headers() }),

  addProduct: (data: {
    name: string
    price: number
    discount: number
    category: string
    description: string
    image_url: string
    images: string[]
    rating?: number
    sold_count?: number
    discount_type?: string
    discount_expires?: string
    auto_delete?: boolean
    send_push?: boolean
    variantlar_yoqilgan?: boolean
    variant_nomlari?: string[]
    variant_narxlari?: number[]
  }) =>
    request<any>('/products', {
      method: 'POST',
      headers: headers(),
      body: JSON.stringify(data),
    }),

  uploadImage: async (file: File): Promise<string> => {
    const form = new FormData()
    form.append('file', file)
    const res = await fetch(BASE + '/upload', {
      method: 'POST',
      headers: authHeaders(),
      body: form,
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({ detail: res.statusText }))
      throw new Error(err.detail || 'Yuklash xatoligi')
    }
    const data = await res.json()
    return data.url
  },

  admins: () => request<import('./types').Admin[]>('/admins', { headers: headers() }),

  createAdmin: (data: { telegram_id: number; name: string; categories: string[] }) =>
    request('/admins', {
      method: 'POST',
      headers: headers(),
      body: JSON.stringify(data),
    }),

  updateAdmin: (
    id: number,
    patch: { name?: string; categories?: string[]; active?: boolean }
  ) =>
    request(`/admins/${id}`, {
      method: 'PUT',
      headers: headers(),
      body: JSON.stringify(patch),
    }),

  deleteAdmin: (id: number) =>
    request(`/admins/${id}`, { method: 'DELETE', headers: headers() }),

  patchProduct: (id: number | string, patch: { active?: boolean; in_stock?: boolean }) =>
    request(`/products/${id}`, {
      method: 'PATCH',
      headers: headers(),
      body: JSON.stringify(patch),
    }),

  updateProduct: (id: number | string, data: {
    name?: string; price?: number; discount?: number
    category?: string; description?: string
  }) =>
    request(`/products/${id}`, {
      method: 'PUT',
      headers: headers(),
      body: JSON.stringify(data),
    }),

  deleteProduct: (id: number | string) =>
    request(`/products/${id}`, { method: 'DELETE', headers: headers() }),

  stats: () =>
    request<{
      total_products: number
      active_admins: number
      by_category: Record<string, number>
      by_admin: [string, number][]
    }>('/stats', { headers: headers() }),

  adminOrders: () =>
    request<{ orders: import('./types').Order[] }>('/orders', { headers: headers() }),

  updateOrderStatus: (orderId: string, status: string) =>
    request(`/orders/${encodeURIComponent(orderId)}/status`, {
      method: 'PATCH',
      headers: headers(),
      body: JSON.stringify({ status }),
    }),

  broadcastDiscount: (data: { product_id: number | string; mahsulot_nomi: string; foiz: number }) =>
    request<{ status: string; sent_count?: number; message?: string }>('/admin/broadcast-discount', {
      method: 'POST',
      headers: headers(),
      body: JSON.stringify(data),
    }),

  getNotificationSettings: () =>
    request<{ marketing_notifications_enabled: boolean }>('/admin/settings', { headers: headers() }),

  toggleNotifications: (enabled: boolean) =>
    request<{ status: string }>('/admin/settings/notifications', {
      method: 'PATCH',
      headers: headers(),
      body: JSON.stringify({ enabled }),
    }),

  getChannels: () =>
    request<{ id: number; channel_id: string; label: string; enabled: number }[]>(
      '/channels', { headers: headers() }
    ),

  addChannel: (data: { channel_id: string; label?: string }) =>
    request<{ ok: boolean; label: string }>('/channels', {
      method: 'POST',
      headers: headers(),
      body: JSON.stringify(data),
    }),

  toggleChannel: (id: number) =>
    request<{ ok: boolean }>(`/channels/${id}/toggle`, {
      method: 'PATCH',
      headers: headers(),
    }),

  deleteChannel: (id: number) =>
    request<{ ok: boolean }>(`/channels/${id}`, {
      method: 'DELETE',
      headers: headers(),
    }),
}
