#!/usr/bin/env python3
"""
Xitoy Admin Bot — tez qo'shish rejimi

Bitta rasm:
  - Rasmga caption yozing → avtomatik saqlanadi

Ko'p rasm (album):
  - 2-10 rasmni birga tanlang → birinchi rasmga caption yozing → saqlanadi

Caption formati (har biri alohida satrda):
  Tovar nomi        ← majburiy
  Narx              ← majburiy (faqat raqam, masalan: 850000)
  Chegirma%         ← ixtiyoriy (masalan: 30)
  Kategoriya        ← ixtiyoriy (Kiyim / Elektronika / Poyabzal / Aksessuar / Sport / Uy uchun / Boshqa)
  Tavsif            ← ixtiyoriy (yo'q bo'lsa nom ishlatiladi)

Tahrirlash:
  /list → ✏️ Tahrirlash tugmasini bosing → caption formatida yangi ma'lumot yuboring
"""

import asyncio
import html as _html
import logging
import json
import base64
import os
import urllib.request
import urllib.parse
from datetime import datetime, timezone
from zoneinfo import ZoneInfo

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.error import BadRequest
from telegram.ext import (
    Application, CommandHandler, MessageHandler,
    CallbackQueryHandler, filters, ContextTypes
)

WEBAPP_URL = "https://admin.eliboyev.uz"

# ── Sozlamalar ────────────────────────────────────────────────────────────────

BOT_TOKEN       = "8843675619:AAEGFzuFrRIS-Cuv1E9_uTAQ_WtFSCGmO-s"
ADMIN_ID        = "5049583350"
APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwYNusH54O3kyMAVcdkzpOaBiejRLrvj6EcXtfgh1m37aG79ZiUYRG_OcOEUa3GSkFi8A/exec"
IMGBB_API_KEY   = "522bbbf3e6288c721c691d7ef773979e"
CHANNEL_ID      = os.environ.get("CHANNEL_ID", "")

CATEGORIES = ["Kiyim", "Elektronika", "Poyabzal", "Aksessuar", "Sport", "Uy uchun", "Boshqa"]

# Bot obyekti — post_init da o'rnatiladi, discount_checker da ishlatiladi
_bot = None

UZB_TZ = ZoneInfo("Asia/Tashkent")  # UTC+5
PAGE_SIZE  = 5

HELP_TEXT = (
    "📦 *Xitoy Admin Bot*\n\n"
    "Rasm yuboring \\+ caption:\n"
    "```\n"
    "Tovar nomi\n"
    "Narx\n"
    "Chegirma%   ← ixtiyoriy\n"
    "Kategoriya  ← ixtiyoriy\n"
    "Tavsif      ← ixtiyoriy\n"
    "```\n\n"
    "*Misol \\(bitta rasm\\):*\n"
    "```\nTapichka\n850000\n30\nPoyabzal\n```\n\n"
    "*Misol \\(album, ko'p rasm\\):*\n"
    "2\\-10 rasmni tanlang → birinchi rasmga caption yozing\n\n"
    "Kategoriyalar: Kiyim, Elektronika, Poyabzal, Aksessuar, Sport, Uy uchun, Boshqa\n\n"
    "/list — tovarlar ro'yxati \\(inline boshqaruv\\)\n"
    "/outofstock — faqat tugagan tovarlar\n"
    "/inactive — faqat nofaol tovarlar\n\n"
    "*Tahrirlash:* /list → ✏️ Tahrirlash tugmasi → yangi ma'lumot yuboring\n"
    "/bekor — joriy tahrirlashni bekor qilish"
)

logging.basicConfig(
    format="%(asctime)s — %(levelname)s — %(message)s",
    level=logging.INFO
)

# ── Media group kollektor ─────────────────────────────────────────────────────

_pending: dict = {}

# ── Yordamchi funksiyalar ─────────────────────────────────────────────────────

def is_admin(update: Update) -> bool:
    return str(update.effective_user.id) == ADMIN_ID


def parse_caption(text: str) -> dict | None:
    """Caption ni parse qiladi. Muvaffaqiyatsiz bo'lsa None qaytaradi."""
    lines = [ln.strip() for ln in text.strip().splitlines() if ln.strip()]
    if len(lines) < 2:
        return None

    name = lines[0]

    price_raw = lines[1].replace(" ", "").replace(",", "").replace(".", "")
    if not price_raw.isdigit():
        return None
    price = int(price_raw)

    discount = 0
    if len(lines) >= 3:
        d = lines[2].replace("%", "").strip()
        if d.isdigit() and 0 <= int(d) <= 90:
            discount = int(d)

    category = "Boshqa"
    if len(lines) >= 4:
        inp = lines[3].strip().lower()
        for cat in CATEGORIES:
            if inp in cat.lower() or cat.lower().startswith(inp[:3]):
                category = cat
                break

    description = name
    if len(lines) >= 5:
        description = " ".join(lines[4:])

    return {
        "name":        name,
        "price":       price,
        "discount":    discount,
        "category":    category,
        "description": description,
    }


def escape(text: str) -> str:
    """MarkdownV2 uchun maxsus belgilarni qochirish."""
    for ch in r"_*[]()~`>#+-=|{}.!":
        text = text.replace(ch, f"\\{ch}")
    return text


# ── Imgbb va Sheets ───────────────────────────────────────────────────────────

def _upload_sync(image_bytes: bytes) -> str:
    b64  = base64.b64encode(image_bytes).decode()
    data = urllib.parse.urlencode({"key": IMGBB_API_KEY, "image": b64}).encode()
    req  = urllib.request.Request("https://api.imgbb.com/1/upload", data=data)
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read())["data"]["url"]


def _post_sync(data: dict) -> dict:
    payload = json.dumps(data).encode()
    req = urllib.request.Request(
        APPS_SCRIPT_URL, data=payload,
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=15) as resp:
        result = json.loads(resp.read())
    if isinstance(result, dict) and result.get("ok") is False:
        raise RuntimeError(result.get("error", "Apps Script xatolik qaytardi"))
    return result


def _get_sync() -> list:
    with urllib.request.urlopen(APPS_SCRIPT_URL, timeout=15) as resp:
        return json.loads(resp.read())


async def upload_photo(image_bytes: bytes) -> str:
    return await asyncio.to_thread(_upload_sync, image_bytes)


async def post_to_sheets(data: dict) -> dict:
    return await asyncio.to_thread(_post_sync, data)


async def get_from_sheets() -> list:
    return await asyncio.to_thread(_get_sync)


async def patch_product(product_id: int, field: str, value: int) -> dict:
    """Tovar statusini yangilaydi: field = 'active' | 'inStock', value = 0 | 1"""
    return await asyncio.to_thread(_post_sync, {
        "type":  "updateProduct",
        "id":    product_id,
        "field": field,
        "value": value,
    })


async def update_message_id_in_sheets(product_id: int, message_id: int):
    await asyncio.to_thread(_post_sync, {
        "type":       "updateMessageId",
        "id":         product_id,
        "message_id": message_id,
    })


async def update_discount_in_sheets(product_id: int, discount: int):
    await asyncio.to_thread(_post_sync, {
        "type":     "updateDiscount",
        "id":       product_id,
        "discount": discount,
    })


async def delete_product(product_id: int) -> dict:
    """Tovarni Sheets dan butunlay o'chiradi."""
    return await asyncio.to_thread(_post_sync, {
        "type": "deleteProduct",
        "id":   product_id,
    })


async def edit_product(product_id: int, parsed: dict) -> dict:
    """Tovar nom, narx, chegirma, kategoriya va tavsifini yangilaydi."""
    return await asyncio.to_thread(_post_sync, {
        "type":        "editProduct",
        "id":          product_id,
        "name":        parsed["name"],
        "description": parsed["description"],
        "price":       parsed["price"],
        "discount":    parsed["discount"],
        "category":    parsed["category"],
    })


# ── Kanalga post ─────────────────────────────────────────────────────────────

def _parse_iso_dt(s: str) -> datetime:
    """ISO stringni Toshkent vaqtida naïve datetime ga aylantiradi.
    Frontend UZB_OFFSET (+5h) qo'shib saqlaydi, shuning uchun timezone strippi
    Toshkent devor soatini to'g'ri beradi."""
    s = s.strip().replace("Z", "").split("+")[0]
    try:
        return datetime.fromisoformat(s)
    except ValueError:
        return datetime.strptime(s[:19], "%Y-%m-%dT%H:%M:%S")


def format_remaining_time(expires_dt: datetime) -> str:
    """Qolgan vaqtni hissiyotli matn sifatida qaytaradi (Toshkent vaqti)."""
    now       = datetime.now(UZB_TZ).replace(tzinfo=None)
    remaining = expires_dt - now
    total_sec = int(remaining.total_seconds())

    if total_sec <= 0:
        return "✅ Chegirma tugadi"

    hours   = total_sec // 3600
    minutes = (total_sec % 3600) // 60

    if total_sec < 10 * 60:           # 10 daqiqadan kam
        return f"🚨 SHOSHILING! Faqat {minutes} daqiqa qoldi!"
    elif total_sec < 30 * 60:         # 30 daqiqadan kam
        return f"⚡ Tezlaning! {minutes} daqiqa qoldi!"
    elif total_sec < 3 * 3600:        # 3 soatdan kam
        return f"🔥 {hours} soat {minutes} daqiqa qoldi!"
    else:
        expires_str = expires_dt.strftime("%d.%m.%Y %H:%M")
        return f"⏰ Tugaydi: <b>{expires_str}</b> gacha"


def _build_channel_text(
    name: str, category: str, price: int,
    discount: int, discount_type: str, expires_at: str,
    is_new: bool = True
) -> str:
    header = "🛍 Yangi tovar!\n\n📌" if is_new else "🛍"
    if discount and int(discount) > 0:
        if discount_type == "vaqtinchalik" and expires_at:
            try:
                expires_dt  = _parse_iso_dt(expires_at)
                expires_str = expires_dt.strftime("%d.%m.%Y %H:%M")
                discount_text = f"🔥 Chegirma: -{discount}%\n⏰ Tugaydi: <b>{expires_str}</b> gacha"
            except Exception:
                discount_text = f"🔥 Chegirma: -{discount}%"
        else:
            discount_text = f"🔥 Chegirma: -{discount}% (doimiy)"
    else:
        discount_text = ""

    lines = [
        f"{header} {_html.escape(name)}",
        f"📦 Kategoriya: {_html.escape(category)}",
        f"💰 Narx: {price:,} so'm".replace(",", " "),
    ]
    if discount_text:
        lines.append(discount_text)
    lines.append("\n👇 Ilovada ko'rish uchun:")
    return "\n".join(lines)


async def wait_until_next_5min():
    """Keyingi 5 ga bo'linadigan daqiqagacha kutadi: :00, :05, :10, :15..."""
    now            = datetime.now(UZB_TZ)
    current_minute = now.minute
    current_second = now.second
    next_minute    = ((current_minute // 5) + 1) * 5
    if next_minute >= 60:
        wait_seconds = (60 - current_minute) * 60 - current_second
    else:
        wait_seconds = (next_minute - current_minute) * 60 - current_second
    if wait_seconds > 0:
        await asyncio.sleep(wait_seconds)


async def send_product_to_channel(product: dict, is_temporary_discount: bool = False) -> int:
    """Kanalga yangi tovar postini (rasm bilan) yuboradi. message_id qaytaradi (0 = xatolik)."""
    if not CHANNEL_ID or not _bot:
        return 0

    if is_temporary_discount:
        await wait_until_next_5min()

    image_url     = str(product.get("image_url") or product.get("image") or "")
    name          = str(product.get("title") or product.get("name") or "")
    category      = str(product.get("category") or "")
    price         = int(product.get("price") or 0)
    discount      = int(product.get("discountPercent") or product.get("discount") or 0)
    discount_type = str(product.get("discountType") or "doimiy")
    expires_at    = str(product.get("discountExpires") or "")
    product_id    = product.get("id")

    text = _build_channel_text(name, category, price, discount, discount_type, expires_at, is_new=True)
    button_url = (
        f"https://admin.eliboyev.uz/open?product={product_id}"
        if product_id else "https://admin.eliboyev.uz"
    )
    markup = InlineKeyboardMarkup([[
        InlineKeyboardButton("📱 Ilovada ochish", url=button_url)
    ]])

    try:
        msg = await _bot.send_photo(
            chat_id=CHANNEL_ID,
            photo=image_url,
            caption=text,
            parse_mode="HTML",
            reply_markup=markup,
        )
        return msg.message_id
    except Exception as e:
        logging.warning("Kanalga post yuborishda xatolik: %s", e)
        return 0


async def update_telegram_post(product: dict, discount: int, expired: bool = False):
    """Mavjud kanal postini yangilaydi (qolgan vaqt yoki 'tugadi' yozuvi)."""
    if not _bot:
        return
    message_id = product.get("telegramMessageId") or product.get("telegram_message_id")
    if not message_id:
        return

    product_id = product.get("id")
    name       = str(product.get("title") or product.get("name") or "")
    category   = str(product.get("category") or "")
    price      = int(product.get("price") or 0)

    if expired or discount == 0:
        original_discount = int(product.get("originalDiscount") or 0) or discount
        discount_section = (
            f"✅ -{original_discount}% chegirma tugadi\n"
            f"🔔 Keyingi katta chegirmalarni o'tkazib yubormang!\n"
            f"👇 Ilovani o'rnating va bildirishnomalarni yoqing"
        )
    else:
        expires_at = str(product.get("discountExpires") or "")
        try:
            expires_dt       = _parse_iso_dt(expires_at)
            discount_section = f"🔥 Chegirma: -{discount}%\n{format_remaining_time(expires_dt)}"
        except Exception:
            discount_section = f"🔥 Chegirma: -{discount}%"

    price_fmt = f"{price:,}".replace(",", " ")
    text = (
        f"🛍 {_html.escape(name)}\n\n"
        f"📦 {_html.escape(category)}\n"
        f"💰 Narx: {price_fmt} so'm\n"
        f"{discount_section}\n\n"
        f"👇 Ilovada ko'rish uchun:"
    )
    button_url = (
        f"https://admin.eliboyev.uz/open?product={product_id}"
        if product_id else "https://admin.eliboyev.uz"
    )
    markup = InlineKeyboardMarkup([[
        InlineKeyboardButton("📱 Ilovada ochish", url=button_url)
    ]])

    try:
        await _bot.edit_message_caption(
            chat_id=CHANNEL_ID,
            message_id=int(message_id),
            caption=text,
            parse_mode="HTML",
            reply_markup=markup,
        )
    except BadRequest as e:
        if "message is not modified" not in str(e).lower():
            logging.warning("Post yangilash xatolik: %s", e)
    except Exception as e:
        logging.warning("Post yangilash xatolik: %s", e)


async def discount_checker():
    """Har 5 daqiqada vaqtinchalik chegirmalarni tekshiradi."""
    while True:
        await asyncio.sleep(5 * 60)
        try:
            products = await get_from_sheets()
            now      = datetime.now(UZB_TZ).replace(tzinfo=None)
            logging.info("discount_checker: %d tovar tekshirilmoqda, now=%s", len(products), now)

            for product in products:
                if product.get("discountType") != "vaqtinchalik":
                    continue
                expires_str = str(product.get("discountExpires") or "")
                if not expires_str:
                    continue

                product_id = product.get("id")
                name       = str(product.get("title") or "")
                discount   = int(product.get("discountPercent") or 0)

                # auto_delete ni turli formatdan o'qish (bool, int, string)
                raw = product.get("autoDelete")
                if isinstance(raw, bool):
                    auto_delete = raw
                elif isinstance(raw, (int, float)):
                    auto_delete = bool(raw)
                else:
                    auto_delete = str(raw).strip().upper() in ("TRUE", "1", "YES")

                logging.info(
                    "[checker] id=%s name=%r discount=%s autoDelete_raw=%r autoDelete=%s expires=%r",
                    product_id, name, discount, raw, auto_delete, expires_str,
                )

                if discount == 0 and not auto_delete:
                    logging.info("[checker] id=%s — chegirma=0 va auto_delete=False, o'tkazib yuborildi", product_id)
                    continue

                try:
                    expires_dt = _parse_iso_dt(expires_str)
                except Exception as e:
                    logging.warning("[checker] id=%s — sanani parse qilib bo'lmadi: %s", product_id, e)
                    continue

                expired    = now > expires_dt
                message_id = product.get("telegramMessageId") or product.get("telegram_message_id")

                logging.info("[checker] id=%s expired=%s message_id=%s", product_id, expired, message_id)

                if expired:
                    if auto_delete:
                        logging.info("[checker] id=%s — AUTO-DELETE boshlandi", product_id)
                        # 1. Kanal postini o'chir
                        if message_id and _bot and CHANNEL_ID:
                            try:
                                await _bot.delete_message(
                                    chat_id=CHANNEL_ID,
                                    message_id=int(message_id),
                                )
                                logging.info("[checker] id=%s — kanal posti o'chirildi", product_id)
                            except Exception as e:
                                logging.warning("[checker] id=%s — post o'chirishda xatolik: %s", product_id, e)
                        # 2. Sheets dan o'chir
                        try:
                            result = await delete_product(product_id)
                            logging.info("[checker] id=%s — Sheets dan o'chirildi: %s", product_id, result)
                        except Exception as e:
                            logging.warning("[checker] id=%s — Sheets o'chirishda xatolik: %s", product_id, e)
                        # 3. Admin xabarnomasi
                        if _bot and ADMIN_ID:
                            try:
                                await _bot.send_message(
                                    chat_id=int(ADMIN_ID),
                                    text=f"🗑️ <b>{_html.escape(name)}</b> avtomatik o'chirildi (chegirma tugadi)",
                                    parse_mode="HTML",
                                )
                            except Exception as e:
                                logging.warning("[checker] admin xabar yuborishda xatolik: %s", e)
                    else:
                        if discount == 0:
                            # Allaqachon yangilangan, qayta yuborish shart emas
                            logging.info("[checker] id=%s — chegirma allaqachon 0, o'tkazildi", product_id)
                            continue
                        try:
                            await update_discount_in_sheets(product_id, 0)
                            await update_telegram_post(product, discount=0, expired=True)
                            logging.info("[checker] id=%s — chegirma tugadi, post yangilandi", product_id)
                        except Exception as e:
                            logging.warning("[checker] id=%s — chegirma yakunlashda xatolik: %s", product_id, e)
                else:
                    if discount == 0:
                        continue
                    try:
                        await update_telegram_post(product, discount=discount)
                        logging.info("[checker] id=%s — aktiv, post yangilandi", product_id)
                    except Exception as e:
                        logging.warning("[checker] id=%s — post yangilashda xatolik: %s", product_id, e)

        except Exception as e:
            logging.warning("discount_checker xatolik: %s", e)


# ── Asosiy saqlash logikasi ───────────────────────────────────────────────────

async def save_product(
    chat_id: int,
    photo_ids: list[str],
    parsed: dict,
    context: ContextTypes.DEFAULT_TYPE
):
    """Rasmlarni yuklaydi va Sheets ga saqlaydi."""
    status = await context.bot.send_message(
        chat_id,
        f"⏳ {len(photo_ids)} ta rasm yuklanmoqda…"
    )

    async def upload_one(file_id: str) -> str:
        file        = await context.bot.get_file(file_id)
        image_bytes = bytes(await file.download_as_bytearray())
        return await upload_photo(image_bytes)

    urls = await asyncio.gather(*[upload_one(fid) for fid in photo_ids])

    result = await post_to_sheets({
        "name":             parsed["name"],
        "description":      parsed["description"],
        "price":            parsed["price"],
        "image_url":        urls[0],
        "images":           list(urls),
        "category":         parsed["category"],
        "discount":         parsed["discount"],
        "sold_count":       0,
        "discount_type":    "doimiy",
        "discount_expires": "",
    })
    product_id = result.get("id") if isinstance(result, dict) else None

    price_fmt    = f"{parsed['price']:,}".replace(",", " ")
    discount_txt = f"  •  \\-{parsed['discount']}%" if parsed["discount"] else ""
    img_txt      = f"{len(urls)} ta rasm" if len(urls) > 1 else "1 ta rasm"

    await context.bot.edit_message_text(
        chat_id=chat_id,
        message_id=status.message_id,
        text=(
            f"✅ *Saqlandi\\!*\n\n"
            f"📦 {escape(parsed['name'])}\n"
            f"💰 {price_fmt} so'm{discount_txt}\n"
            f"📂 {parsed['category']}\n"
            f"🖼 {img_txt}\n\n"
            f"Keyingi mahsulotni yuboring yoki /list"
        ),
        parse_mode="MarkdownV2"
    )

    msg_id = await send_product_to_channel({
        "id":             product_id,
        "image_url":      urls[0],
        "title":          parsed["name"],
        "category":       parsed["category"],
        "price":          parsed["price"],
        "discountPercent": parsed["discount"],
        "discountType":   "doimiy",
        "discountExpires": "",
    }, is_temporary_discount=False)
    if msg_id and product_id is not None:
        try:
            await update_message_id_in_sheets(product_id, msg_id)
        except Exception as e:
            logging.warning("message_id saqlashda xatolik: %s", e)


# ── Ro'yxat va boshqaruv ──────────────────────────────────────────────────────

def _filter_and_title(mode: str) -> tuple:
    """Mode ga qarab filter funksiyasi va sarlavha qaytaradi."""
    if mode == "oos":
        return lambda p: not p.get("inStock", True), "Tugagan tovarlar"
    if mode == "off":
        return lambda p: not p.get("active", True), "Nofaol tovarlar"
    return None, "Barcha tovarlar"


def _build_list_message(
    products: list,
    page: int,
    filter_fn=None,
    title: str = "Barcha tovarlar",
    mode: str = "all"
) -> tuple[str, InlineKeyboardMarkup]:
    """
    products  — to'liq ro'yxat
    filter_fn — None = hammasi, aks holda True qaytargan tovarlar
    mode      — sahifa o'tishda filtr holatini saqlab qoladi: "all" | "oos" | "off"
    """
    filtered    = [p for p in products if filter_fn is None or filter_fn(p)]
    total       = len(filtered)
    total_pages = max(1, -(-total // PAGE_SIZE))
    start       = page * PAGE_SIZE
    chunk       = filtered[start: start + PAGE_SIZE]

    esc_title = escape(title)
    lines = [f"📦 *{esc_title}* — jami *{total}* ta \\({page + 1}/{total_pages}\\):\n"]
    keyboard: list[list[InlineKeyboardButton]] = []

    for idx, p in enumerate(chunk, start=1):
        pid      = p.get("id", start + idx - 1)
        name     = escape(str(p.get("title") or p.get("name") or "?")[:24])
        price    = int(p.get("price", 0))
        active   = p.get("active", True)
        in_stock = p.get("inStock", True)

        s_icon = "🟢" if active else "🔴"
        k_icon = "📦" if in_stock else "⚠️"
        p_fmt  = f"{price:,}".replace(",", " ")

        lines.append(f"{idx}\\. {s_icon}{k_icon} {name} — {p_fmt} so'm")

        act_label   = f"{idx}. 🔴 Deaktiv"   if active   else f"{idx}. 🟢 Aktiv"
        stock_label = f"{idx}. ⚠️ Tugadi"    if in_stock else f"{idx}. 📦 Mavjud"

        keyboard.append([
            InlineKeyboardButton(act_label,   callback_data=f"ta:{pid}:{page}:{mode}"),
            InlineKeyboardButton(stock_label, callback_data=f"ts:{pid}:{page}:{mode}"),
        ])
        keyboard.append([
            InlineKeyboardButton(f"{idx}. ✏️ Tahrirlash", callback_data=f"ed:{pid}:{page}:{mode}"),
            InlineKeyboardButton(f"{idx}. 🗑 O'chirish",  callback_data=f"del:{pid}:{page}:{mode}"),
        ])

    nav: list[InlineKeyboardButton] = []
    if page > 0:
        nav.append(InlineKeyboardButton("◀️ Oldingi", callback_data=f"pg:{page - 1}:{mode}"))
    if start + PAGE_SIZE < total:
        nav.append(InlineKeyboardButton("▶️ Keyingi", callback_data=f"pg:{page + 1}:{mode}"))
    if nav:
        keyboard.append(nav)

    return "\n".join(lines), InlineKeyboardMarkup(keyboard)


# ── Komandalar ────────────────────────────────────────────────────────────────

async def cmd_start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not is_admin(update):
        return
    await update.message.reply_text(
        "👋 Xush kelibsiz\\!\n\n"
        "📱 *Admin panelni ochish uchun* pastdagi tugmani bosing\\.",
        parse_mode="MarkdownV2",
    )


async def cmd_help(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not is_admin(update):
        return
    await update.message.reply_text(HELP_TEXT, parse_mode="MarkdownV2")


async def cmd_list(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not is_admin(update):
        return
    try:
        products = await get_from_sheets()
        if not products:
            await update.message.reply_text("Hozircha tovar yo'q.")
            return
        text, markup = _build_list_message(products, 0, mode="all")
        await update.message.reply_text(text, parse_mode="MarkdownV2", reply_markup=markup)
    except Exception as e:
        await update.message.reply_text(f"❌ Xatolik: {e}")


async def cmd_outofstock(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Faqat tugagan tovarlarni ko'rsatadi."""
    if not is_admin(update):
        return
    try:
        products = await get_from_sheets()
        oos = [p for p in products if not p.get("inStock", True)]
        if not oos:
            await update.message.reply_text("✅ Hozircha barcha tovarlar mavjud\\!", parse_mode="MarkdownV2")
            return
        text, markup = _build_list_message(
            products, page=0,
            filter_fn=lambda p: not p.get("inStock", True),
            title="Tugagan tovarlar", mode="oos"
        )
        await update.message.reply_text(text, parse_mode="MarkdownV2", reply_markup=markup)
    except Exception as e:
        await update.message.reply_text(f"❌ Xatolik: {e}")


async def cmd_inactive(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Faqat nofaol tovarlarni ko'rsatadi."""
    if not is_admin(update):
        return
    try:
        products = await get_from_sheets()
        if not any(not p.get("active", True) for p in products):
            await update.message.reply_text("✅ Hozircha nofaol tovar yo'q\\!", parse_mode="MarkdownV2")
            return
        text, markup = _build_list_message(
            products, page=0,
            filter_fn=lambda p: not p.get("active", True),
            title="Nofaol tovarlar", mode="off"
        )
        await update.message.reply_text(text, parse_mode="MarkdownV2", reply_markup=markup)
    except Exception as e:
        await update.message.reply_text(f"❌ Xatolik: {e}")


async def cmd_bekor(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Joriy tahrirlash jarayonini bekor qiladi."""
    if not is_admin(update):
        return
    if context.user_data.pop("edit", None):
        await update.message.reply_text("❌ Tahrirlash bekor qilindi.")
    else:
        await update.message.reply_text("Hozir hech qanday faol jarayon yo'q.")


# ── Callback handler (inline tugmalar) ───────────────────────────────────────

async def handle_callback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not is_admin(update):
        await update.callback_query.answer("Ruxsat yo'q", show_alert=True)
        return

    query  = update.callback_query
    await query.answer()

    parts  = query.data.split(":")
    action = parts[0]

    try:
        # ── Sahifa o'tish (filtr holati saqlanadi) ──
        if action == "pg":
            page = int(parts[1])
            mode = parts[2] if len(parts) > 2 else "all"
            products = await get_from_sheets()
            if not products:
                await query.edit_message_text("Hozircha tovar yo'q.")
                return
            filter_fn, title = _filter_and_title(mode)
            text, markup = _build_list_message(products, page, filter_fn=filter_fn, title=title, mode=mode)
            await query.edit_message_text(text, parse_mode="MarkdownV2", reply_markup=markup)

        # ── Aktiv/Deaktiv va Mavjud/Tugadi toggle ──
        elif action in ("ta", "ts"):
            pid   = int(parts[1])
            page  = int(parts[2])
            mode  = parts[3] if len(parts) > 3 else "all"
            field = "active" if action == "ta" else "inStock"

            products = await get_from_sheets()
            product  = next((p for p in products if p.get("id") == pid), None)
            if product is None:
                await query.answer("Tovar topilmadi!", show_alert=True)
                return

            current = product.get("active", True) if field == "active" else product.get("inStock", True)
            new_val = 0 if current else 1

            status_msg = await context.bot.send_message(query.message.chat_id, "⏳ Yangilanmoqda…")
            await patch_product(pid, field, new_val)
            await context.bot.delete_message(query.message.chat_id, status_msg.message_id)

            products = await get_from_sheets()
            filter_fn, title = _filter_and_title(mode)
            text, markup = _build_list_message(products, page, filter_fn=filter_fn, title=title, mode=mode)
            await query.edit_message_text(text, parse_mode="MarkdownV2", reply_markup=markup)

        # ── O'chirish — tasdiqlash so'rash ──
        elif action == "del":
            pid  = int(parts[1])
            page = int(parts[2])
            mode = parts[3] if len(parts) > 3 else "all"

            products = await get_from_sheets()
            product  = next((p for p in products if p.get("id") == pid), None)
            name = str(product.get("title") or product.get("name") or pid) if product else str(pid)

            confirm_markup = InlineKeyboardMarkup([[
                InlineKeyboardButton("✅ Ha, o'chir",  callback_data=f"delok:{pid}:{page}:{mode}"),
                InlineKeyboardButton("❌ Bekor",        callback_data=f"delno:{pid}:{page}:{mode}"),
            ]])
            await context.bot.send_message(
                query.message.chat_id,
                f"⚠️ *{escape(name[:40])}* tovarni butunlay o'chirasizmi?",
                parse_mode="MarkdownV2",
                reply_markup=confirm_markup
            )

        # ── O'chirishni tasdiqladi ──
        elif action == "delok":
            pid  = int(parts[1])
            page = int(parts[2])
            mode = parts[3] if len(parts) > 3 else "all"

            await context.bot.delete_message(query.message.chat_id, query.message.message_id)

            status_msg = await context.bot.send_message(query.message.chat_id, "⏳ O'chirilmoqda…")
            await delete_product(pid)
            await context.bot.delete_message(query.message.chat_id, status_msg.message_id)

            products = await get_from_sheets()
            filter_fn, title = _filter_and_title(mode)

            if not products:
                await context.bot.send_message(query.message.chat_id, "✅ O'chirildi\\. Hozircha tovar yo'q\\.", parse_mode="MarkdownV2")
                return

            filtered_count = len([p for p in products if filter_fn is None or filter_fn(p)])
            actual_page    = min(page, max(0, -(-filtered_count // PAGE_SIZE) - 1))

            text, markup = _build_list_message(products, actual_page, filter_fn=filter_fn, title=title, mode=mode)
            await context.bot.send_message(
                query.message.chat_id,
                "✅ O'chirildi\\!\n\n" + text,
                parse_mode="MarkdownV2",
                reply_markup=markup
            )

        # ── To'lov tasdiqlash / rad etish ──
        elif action == "pay":
            sub_action = parts[1] if len(parts) > 1 else ""
            order_id   = parts[2] if len(parts) > 2 else ""
            cust_tg    = parts[3] if len(parts) > 3 else ""

            new_status  = "Tasdiqlandi" if sub_action == "approve" else "Rad_etildi"
            status_text = "✅ To'lov tasdiqlandi!" if sub_action == "approve" else "❌ To'lov rad etildi."

            await asyncio.to_thread(_post_sync, {
                "action":   "updateOrderStatus",
                "order_id": order_id,
                "status":   new_status,
            })

            old_caption = query.message.caption or ""
            await query.edit_message_caption(
                caption=f"{old_caption}\n\n{status_text}",
                parse_mode="HTML",
            )

        # ── O'chirishni bekor qildi ──
        elif action == "delno":
            await context.bot.delete_message(query.message.chat_id, query.message.message_id)

        # ── Tahrirlash boshlash ──
        elif action == "ed":
            pid  = int(parts[1])
            page = int(parts[2])
            mode = parts[3] if len(parts) > 3 else "all"

            products = await get_from_sheets()
            product  = next((p for p in products if p.get("id") == pid), None)
            if product is None:
                await query.answer("Tovar topilmadi!", show_alert=True)
                return

            name     = str(product.get("title") or product.get("name") or "")
            price    = int(product.get("price", 0))
            discount = int(product.get("discountPercent", 0))
            category = str(product.get("category", ""))
            desc     = str(product.get("description", ""))

            context.user_data["edit"] = {"pid": pid, "page": page, "mode": mode}

            await context.bot.send_message(
                query.message.chat_id,
                f"✏️ *{escape(name[:40])}* tahrirlash\\:\n\n"
                f"Joriy: *{price:,}* so'm, chegirma *{discount}%*, kategoriya *{escape(category)}*\n"
                f"Tavsif: _{escape(desc[:60])}_\n\n"
                f"Yangi ma'lumotlarni yuboring:\n"
                f"```\nNom\nNarx\nChegirma%\nKategoriya\nTavsif\n```\n\n"
                f"/bekor — bekor qilish",
                parse_mode="MarkdownV2"
            )

    except Exception as e:
        logging.error("Callback xatolik: %s", e)
        try:
            await query.answer(f"Xatolik: {e}", show_alert=True)
        except Exception:
            pass


# ── Matn handler (tahrirlash uchun) ──────────────────────────────────────────

async def handle_text(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not is_admin(update):
        return

    edit_state = context.user_data.get("edit")
    if not edit_state:
        return

    text   = (update.message.text or "").strip()
    parsed = parse_caption(text)

    if not parsed:
        await update.message.reply_text(
            "❌ Noto'g'ri format\\.\n\n"
            "```\nNom\nNarx\nChegirma%\nKategoriya\nTavsif\n```\n\n"
            "/bekor — bekor qilish",
            parse_mode="MarkdownV2"
        )
        return

    pid  = edit_state["pid"]
    page = edit_state["page"]
    mode = edit_state["mode"]

    status = await update.message.reply_text("⏳ Saqlanmoqda…")

    try:
        await edit_product(pid, parsed)
        context.user_data.pop("edit", None)

        price_fmt    = f"{parsed['price']:,}".replace(",", " ")
        discount_txt = f" \\(\\-{parsed['discount']}%\\)" if parsed["discount"] else ""

        await context.bot.edit_message_text(
            chat_id=status.chat_id,
            message_id=status.message_id,
            text=(
                f"✅ *Yangilandi\\!*\n\n"
                f"📦 {escape(parsed['name'])}\n"
                f"💰 {price_fmt} so'm{discount_txt}\n"
                f"📂 {escape(parsed['category'])}"
            ),
            parse_mode="MarkdownV2"
        )

        products = await get_from_sheets()
        filter_fn, title = _filter_and_title(mode)
        text_list, markup = _build_list_message(products, page, filter_fn=filter_fn, title=title, mode=mode)
        await context.bot.send_message(
            update.message.chat_id,
            text_list,
            parse_mode="MarkdownV2",
            reply_markup=markup
        )

    except Exception as e:
        logging.error("edit_product xatolik: %s", e)
        await context.bot.edit_message_text(
            chat_id=status.chat_id,
            message_id=status.message_id,
            text=f"❌ Xatolik: {e}"
        )


# ── Album kollektor ───────────────────────────────────────────────────────────

async def _flush_group(group_id: str, context: ContextTypes.DEFAULT_TYPE):
    """Albomning barcha rasmlari kelishini 1.5s kutib qayta ishlaydi."""
    await asyncio.sleep(1.5)

    group = _pending.pop(group_id, None)
    if not group:
        return

    caption = group["caption"]
    if not caption:
        await context.bot.send_message(
            group["chat_id"],
            "⚠️ Caption yozing\\! Albomning birinchi rasmiga caption qo'shing\\.\n\n"
            "/help — format ko'rsatish",
            parse_mode="MarkdownV2"
        )
        return

    parsed = parse_caption(caption)
    if not parsed:
        await context.bot.send_message(
            group["chat_id"],
            "❌ Noto'g'ri format\\.\n\n/help — to'g'ri formatni ko'rsatish",
            parse_mode="MarkdownV2"
        )
        return

    try:
        await save_product(group["chat_id"], group["photos"], parsed, context)
    except Exception as e:
        logging.error("save_product xatolik: %s", e)
        await context.bot.send_message(group["chat_id"], f"❌ Xatolik: {e}")


# ── Foto handler ──────────────────────────────────────────────────────────────

async def handle_photo(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not is_admin(update):
        return

    msg      = update.message
    caption  = (msg.caption or "").strip()
    photo_id = msg.photo[-1].file_id
    group_id = msg.media_group_id

    if group_id:
        if group_id not in _pending:
            _pending[group_id] = {
                "photos":  [],
                "caption": "",
                "chat_id": msg.chat_id,
                "task":    None,
            }

        grp = _pending[group_id]
        grp["photos"].append(photo_id)

        if caption:
            grp["caption"] = caption

        if grp["task"] and not grp["task"].done():
            grp["task"].cancel()
        grp["task"] = asyncio.create_task(_flush_group(group_id, context))

    else:
        if not caption:
            await msg.reply_text(
                "⚠️ Rasmga caption yozing\\!\n\n"
                "/help — format ko'rsatish",
                parse_mode="MarkdownV2"
            )
            return

        parsed = parse_caption(caption)
        if not parsed:
            await msg.reply_text(
                "❌ Noto'g'ri format\\.\n\n/help — to'g'ri formatni ko'rsatish",
                parse_mode="MarkdownV2"
            )
            return

        try:
            await save_product(msg.chat_id, [photo_id], parsed, context)
        except Exception as e:
            logging.error("save_product xatolik: %s", e)
            await msg.reply_text(f"❌ Xatolik: {e}")


# ── Main ──────────────────────────────────────────────────────────────────────

async def post_init(application):
    global _bot
    _bot = application.bot

    await application.bot.set_chat_menu_button(
        menu_button={
            "type": "web_app",
            "text": "🛍 Admin Panel",
            "web_app": {"url": WEBAPP_URL},
        }
    )
    logging.info("Menu button o'rnatildi: %s", WEBAPP_URL)

    asyncio.create_task(discount_checker())
    logging.info("Discount checker ishga tushdi")


def main():
    app = (
        Application.builder()
        .token(BOT_TOKEN)
        .post_init(post_init)
        .build()
    )

    app.add_handler(CommandHandler("start",      cmd_start))
    app.add_handler(CommandHandler("help",       cmd_help))
    app.add_handler(CommandHandler("webapp",     cmd_start))
    app.add_handler(CommandHandler("list",       cmd_list))
    app.add_handler(CommandHandler("outofstock", cmd_outofstock))
    app.add_handler(CommandHandler("inactive",   cmd_inactive))
    app.add_handler(CommandHandler("bekor",      cmd_bekor))
    app.add_handler(CallbackQueryHandler(handle_callback))
    app.add_handler(MessageHandler(filters.PHOTO, handle_photo))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_text))

    logging.info("Admin bot ishga tushdi!")
    app.run_polling(allowed_updates=Update.ALL_TYPES)


if __name__ == "__main__":
    main()
