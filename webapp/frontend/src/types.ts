export interface User {
  telegram_id: number
  name: string
  username: string
  categories: string[]
  is_superadmin: boolean
}

// Kategoriya daraxti: "Asosiy toifa" -> "Kichik toifa" -> ["Tovar turi", ...]
export type CategoryTree = Record<string, Record<string, string[]>>

export interface Product {
  id?: string | number
  title?: string
  name?: string
  price: number
  discountPercent?: number
  discount?: number
  category: string          // 1-daraja
  subcategory?: string      // 2-daraja
  product_type?: string     // 3-daraja
  productType?: string
  categoryPath?: string[]
  description?: string
  image_url?: string
  imageUrl?: string
  images?: string[]
  added_by?: string
  added_by_name?: string
  active?: boolean
  inStock?: boolean
  attributes?: Record<string, string[]>       // {"Rang": ["Qora","Oq"], ...}
  rangRasmlari?: Record<string, string>        // {"Qora": "https://...jpg", ...}
  variantlarYoqilgan?: boolean
  variantNomlari?: string[]
  variantNarxlari?: number[]
  razmerMatritsa?: Record<string, { nomi: string; narx: number }[]>
}

export interface Admin {
  telegram_id: number
  name: string
  categories: string[]
  active: boolean
  created_at: string
  is_superadmin: boolean
}

export interface AppUser {
  user_id:         number
  phone:           string
  fullname:        string
  created_at:      number
  favorites_count: number
}

export type AppUserSort = 'date_desc' | 'date_asc' | 'favorites_desc'

export interface AppUsersResponse {
  items:       AppUser[]
  total_count: number
  page:        number
  page_size:   number
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

export type Page = 'add' | 'list' | 'admins' | 'orders' | 'users'
