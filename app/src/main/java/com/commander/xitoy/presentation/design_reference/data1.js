// data.jsx — Dalli Shop catalog, orders, FX rates, formatters
// All copy in Uzbek (Latin). Prices in UZS (so'm). Wholesale = import cost.

const fmtSom = (n) => {
  const s = Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ");
  return s; // grouped with spaces, e.g. 1 290 000
};

const FX = {
  cny: 1752,        // 1 CNY -> UZS
  cnyDelta: +0.42,  // % change today
  usd: 12640,       // 1 USD -> UZS
  updated: "10:42",
};

// category id, Uzbek label, English hint, accent tint (low-chroma oklch)
const CATEGORIES = [
  { id: "kiyim",       label: "Kiyim",        en: "Clothing",    glyph: "shirt",  tint: "oklch(0.95 0.03 250)", ink: "oklch(0.45 0.13 255)", count: 4120 },
  { id: "elektronika", label: "Elektronika",  en: "Electronics", glyph: "chip",   tint: "oklch(0.95 0.025 230)", ink: "oklch(0.45 0.11 235)", count: 2870 },
  { id: "kosmetika",   label: "Kosmetika",    en: "Cosmetics",   glyph: "drop",   tint: "oklch(0.95 0.03 350)", ink: "oklch(0.5 0.14 355)", count: 1960 },
  { id: "uy",          label: "Uy-ro'zg'or",  en: "Home goods",  glyph: "home",   tint: "oklch(0.95 0.035 75)", ink: "oklch(0.52 0.13 60)", count: 3340 },
  { id: "oyinchoq",    label: "O'yinchoqlar", en: "Toys",        glyph: "block",  tint: "oklch(0.95 0.04 150)", ink: "oklch(0.48 0.12 155)", count: 1510 },
  { id: "aksessuar",   label: "Aksessuar",    en: "Accessories", glyph: "bag",    tint: "oklch(0.95 0.035 300)", ink: "oklch(0.48 0.13 305)", count: 2230 },
];

// products: cost = wholesale import price/unit, sell = suggested resale price/unit
const PRODUCTS = [
  { id: "p1",  cat: "elektronika", name: "Simsiz quloqchin TWS Pro",      cost: 78000,  sell: 145000, moq: 10, rating: 4.8, sold: 1240, hot: true },
  { id: "p2",  cat: "kiyim",       name: "Ayollar kuzgi kurtka",          cost: 165000, sell: 320000, moq: 5,  rating: 4.6, sold: 890 },
  { id: "p3",  cat: "kosmetika",   name: "Matte lab gloss to'plami 12x",  cost: 32000,  sell: 69000,  moq: 20, rating: 4.9, sold: 3410, hot: true },
  { id: "p4",  cat: "uy",          name: "Aqlli LED tungi chiroq",        cost: 54000,  sell: 110000, moq: 12, rating: 4.5, sold: 540 },
  { id: "p5",  cat: "oyinchoq",    name: "Yumshoq ayiq o'yinchoq 40sm",   cost: 41000,  sell: 89000,  moq: 15, rating: 4.7, sold: 760 },
  { id: "p6",  cat: "aksessuar",   name: "Erkaklar charm hamyon",         cost: 58000,  sell: 129000, moq: 10, rating: 4.4, sold: 320 },
  { id: "p7",  cat: "kiyim",       name: "Oversize futbolka unisex",      cost: 39000,  sell: 79000,  moq: 24, rating: 4.6, sold: 2100, hot: true },
  { id: "p8",  cat: "elektronika", name: "Tez zaryadlovchi blok 65W",     cost: 62000,  sell: 119000, moq: 10, rating: 4.8, sold: 1530 },
  { id: "p9",  cat: "kosmetika",   name: "Kollagen yuz niqobi 50x",       cost: 88000,  sell: 169000, moq: 6,  rating: 4.7, sold: 980 },
  { id: "p10", cat: "uy",          name: "Oshxona tashkilotchi to'plam",  cost: 72000,  sell: 139000, moq: 8,  rating: 4.5, sold: 410 },
  { id: "p11", cat: "oyinchoq",    name: "Konstruktor bloklar 1000 dona", cost: 95000,  sell: 189000, moq: 6,  rating: 4.8, sold: 670 },
  { id: "p12", cat: "aksessuar",   name: "Quyosh ko'zoynagi UV400",       cost: 28000,  sell: 65000,  moq: 30, rating: 4.3, sold: 1840, hot: true },
];

// 6-stage shipment pipeline
const STAGES = [
  { id: "ordered",  label: "Buyurtma",   full: "Buyurtma berildi" },
  { id: "paid",     label: "To'landi",   full: "To'lov qabul qilindi" },
  { id: "warehouse",label: "Xitoy ombor",full: "Xitoy omborida" },
  { id: "transit",  label: "Yo'lda",     full: "Yo'lda (transport)" },
  { id: "customs",  label: "Bojxona",    full: "Bojxonada rasmiylashtirilmoqda" },
  { id: "delivered",label: "Yetkazildi", full: "Yetkazib berildi" },
];

const ORDERS = [
  { id: "DS-24817", stage: 3, items: 3, qty: 45, total: 2310000, eta: "3-5 kun", city: "Guangzhou \u2192 Toshkent", date: "11-iyun" },
  { id: "DS-24790", stage: 2, items: 2, qty: 28, total: 1640000, eta: "8-10 kun", city: "Yiwu ombori",            date: "08-iyun" },
  { id: "DS-24705", stage: 5, items: 5, qty: 80, total: 3920000, eta: "Yetkazildi", city: "Toshkent, Chilonzor",  date: "02-iyun" },
];

const catById = (id) => CATEGORIES.find(c => c.id === id);
const productsByCat = (id) => PRODUCTS.filter(p => p.cat === id);
const margin = (p) => Math.round(((p.sell - p.cost) / p.sell) * 100);
const profit = (p) => p.sell - p.cost;

window.DALLI = { fmtSom, FX, CATEGORIES, PRODUCTS, STAGES, ORDERS, catById, productsByCat, margin, profit };
