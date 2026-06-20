export interface User {
  telegram_id: number
  name: string
  username: string
  categories: string[]
  is_superadmin: boolean
}

export interface Product {
  id?: string | number
  title?: string
  name?: string
  price: number
  discountPercent?: number
  discount?: number
  category: string
  description?: string
  image_url?: string
  imageUrl?: string
  images?: string[]
  added_by?: string
  added_by_name?: string
  active?: boolean
  inStock?: boolean
  variantlarYoqilgan?: boolean
  variantNomlari?: string[]
  variantNarxlari?: number[]
}

export interface Admin {
  telegram_id: number
  name: string
  categories: string[]
  active: boolean
  created_at: string
  is_superadmin: boolean
}

export interface Order {
  order_id:      string
  telegram_id:   string
  fullname:      string
  phone:         string
  location_link: string
  mahsulotlar:   string
  jami_summa:    number
  holat:         string
  sana:          string
}

export type Page = 'add' | 'list' | 'admins' | 'orders'
