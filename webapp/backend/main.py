#!/usr/bin/env python3
"""
Xitoy WebApp Backend — FastAPI
"""

import asyncio
import base64
import hashlib
import hmac
import html as _html
import json
import logging
import urllib.parse
import urllib.request
from contextlib import asynccontextmanager
from datetime import datetime
from time import time
from typing import Annotated
from zoneinfo import ZoneInfo

import random

import firebase_admin
from firebase_admin import credentials as _fcm_creds
from firebase_admin import messaging as _fcm_messaging

_UZB_TZ          = ZoneInfo("Asia/Tashkent")
pending_products: list[dict] = []   # vaqtinchalik chegirmali tovarlar, hali e'lon qilinmagan

import aiosqlite
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from fastapi import Depends, FastAPI, File, HTTPException, Header, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, field_validator

# ── Config ─────────────────────────────────────────────────────────────────────
import os
import secrets

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

import login_state

BOT_TOKEN            = os.environ.get("BOT_TOKEN", "")
SUPERADMIN_ID        = int(os.environ.get("SUPERADMIN_ID", "0"))
CHANNEL_ID           = os.environ.get("CHANNEL_ID", "")
PAYMENT_CARD_NUMBER  = os.environ.get("PAYMENT_CARD_NUMBER", "")
PAYMENT_CARD_HOLDER  = os.environ.get("PAYMENT_CARD_HOLDER", "")
APPS_SCRIPT_URL      = os.environ.get("APPS_SCRIPT_URL", "")
IMGBB_API_KEY        = os.environ.get("IMGBB_API_KEY", "")
DB_PATH         = "admins.db"

# Login bot username (@ siz). Deep-link uchun.
LOGIN_BOT_USERNAME = os.environ.get("LOGIN_BOT_USERNAME", "dalli_login_robot")

CATEGORIES = ["Kiyim", "Elektronika", "Poyabzal", "Aksessuar", "Sport", "Uy uchun", "Boshqa"]

# Sarlavha qisqartirish uchun keraksiz marketing so'zlari ro'yxati
# (real tarjima natijalarini ko'rib, kengaytirish mumkin)
NOISE_TITLE_WORDS = [
    "yangi", "premium", "yuqori sifat", "yuqori sifatli",
    "tezkor yetkazib berish", "bepul yetkazib berish",
    "rasmiy", "original", "100%", "eng yaxshi", "maxsus",
    "chegirma", "aksiya", "tavsiya etiladi", "mashhur",
    "issiq sotuv", "ko'p sotiladigan", "trend",
    "kichik narx", "arzon", "iste'molchi", "ulgurji",
    "sifat kafolati", "tez yetkazish", "bepul",
]

log = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")


# ── FCM (Firebase Cloud Messaging) ─────────────────────────────────────────────
_FCM_CERT_PATH = "/opt/xitoy_webapp/backend/firebase-service-account.json"
_FCM_ENABLED   = False
if os.path.exists(_FCM_CERT_PATH):
    try:
        firebase_admin.initialize_app(_fcm_creds.Certificate(_FCM_CERT_PATH))
        _FCM_ENABLED = True
        log.info("Firebase Admin SDK tayyor")
    except Exception as _fcm_init_err:
        log.warning("Firebase init xato: %s", _fcm_init_err)
else:
    log.warning("Firebase sertifikat topilmadi: %s", _FCM_CERT_PATH)

_STATUS_PUSH: dict[str, tuple[str, str]] = {
    "Tasdiqlandi":      ("✅ Buyurtma tasdiqlandi",  "📋 {id} buyurtmangiz tasdiqlandi!"),
    "Rad_etildi":       ("❌ Buyurtma rad etildi",   "📋 {id} buyurtmangiz rad etildi."),
    "Yo'lda":           ("🚚 Buyurtmangiz yo'lda!",  "🚚 {id} — yo'lga chiqdi, tez yetib boradi!"),
    "Yetkazildi":       ("📦 Yetkazib berildi!",     "📦 {id} buyurtmangiz yetkazib berildi!"),
    "Tolov_kutilmoqda": ("💳 To'lov kutilmoqda",     "📋 {id} — to'lovingiz tasdiqlanmoqda."),
}


def _send_push_sync(fcm_token: str, title: str, body: str, data: dict | None = None):
    if not _FCM_ENABLED:
        log.warning("FCM o'chirilgan (sertifikat topilmadi) — push yuborilmadi: %s", body[:60])
        return
    if not fcm_token:
        return
    try:
        _fcm_messaging.send(_fcm_messaging.Message(
            notification=_fcm_messaging.Notification(title=title, body=body),
            data={k: str(v) for k, v in data.items()} if data else {},
            token=fcm_token,
        ))
    except Exception as e:
        log.warning("FCM xato: %s", e)


async def send_push_notification(fcm_token: str, title: str, body: str, data: dict | None = None):
    await asyncio.to_thread(_send_push_sync, fcm_token, title, body, data)


async def _fcm_order_notify(telegram_id: str, order_id: str, status: str):
    push = _STATUS_PUSH.get(status)
    if not push:
        return
    try:
        user_data = await sheets_post({"action": "getUser", "telegram_id": telegram_id})
        fcm_token = user_data.get("fcm_token", "") if isinstance(user_data, dict) else ""
        if fcm_token:
            await send_push_notification(fcm_token, push[0], push[1].format(id=order_id))
    except Exception as e:
        log.warning("FCM order notify xato: %s", e)


# ── Marketing push — cart reminder & broadcast ────────────────────────────────

CART_MESSAGES: dict[int, list[str]] = {
    1: [
        "{ism}, {mahsulot} hali savatingizda kutmoqda.",
        "{mahsulot} sizni savatda kutyapti, {ism}.",
        "Savatingizni unutmang — {mahsulot} hali turibdi.",
    ],
    2: [
        "{ism}, {mahsulot}ga boshqalar ham qiziqmoqda — tezroq buyurtma bering.",
        "Bir kun o'tdi, {mahsulot} hali sizni kutmoqda.",
        "{ism}, ulguring — {mahsulot} talab yuqori.",
    ],
    3: [
        "{ism}, {mahsulot} narxi istalgan payt o'zgarishi mumkin.",
        "3 kundan beri savatingizda — {mahsulot} sizni kutib charchadi.",
        "{ism}, fikringizni o'zgartirdingizmi? {mahsulot} hali turibdi.",
    ],
    4: [
        "{ism}, {mahsulot}ni hali ham xohlaysizmi?",
        "So'nggi eslatma: {mahsulot} savatingizda bir hafta turdi.",
        "{ism}, kerak bo'lmasa savatni tozalang. Aks holda — buyurtma bering!",
    ],
}
STAGE_DELAYS_HOURS: dict[int, int] = {1: 2, 2: 24, 3: 72, 4: 168}

DISCOUNT_MESSAGES: dict[tuple[int, int], str] = {
    (10, 30): "{ism}, {mahsulot} narxi pasaydi — {foiz}% chegirma sizni kutmoqda.",
    (30, 50): "Ajoyib taklif! {mahsulot} uchun {foiz}% chegirma boshlandi.",
    (50, 70): "Katta chegirma! {mahsulot} narxi {foiz}%ga tushdi — fursatni boy bermang.",
    (70, 91): "Bunday imkoniyat kam uchraydi: {mahsulot} {foiz}% arzonlashdi!",
}


def get_discount_message(foiz: int) -> str:
    for (low, high), template in DISCOUNT_MESSAGES.items():
        if low <= foiz < high:
            return template
    return "{ism}, {mahsulot} chegirmaga tushdi — {foiz}% arzonroq!"


def is_quiet_hours() -> bool:
    now = datetime.now(_UZB_TZ)
    return now.hour >= 23 or now.hour < 8


def _get_settings_sync() -> dict:
    query = urllib.parse.urlencode({"action": "getSettings"})
    with urllib.request.urlopen(f"{APPS_SCRIPT_URL}?{query}", timeout=15) as r:
        return json.loads(r.read())


async def get_marketing_notifications_enabled() -> bool:
    try:
        s = await asyncio.to_thread(_get_settings_sync)
        return s.get("marketing_notifications_enabled", True)
    except Exception as e:
        log.warning("Settings yuklanmadi, default=True: %s", e)
        return True


def _get_all_users_fcm_sync() -> list[dict]:
    query = urllib.parse.urlencode({"action": "getAllUsersWithFcmToken"})
    with urllib.request.urlopen(f"{APPS_SCRIPT_URL}?{query}", timeout=30) as r:
        d = json.loads(r.read())
    return d.get("users", []) if isinstance(d, dict) else []


async def get_all_users_with_fcm_token() -> list[dict]:
    return await asyncio.to_thread(_get_all_users_fcm_sync)


async def check_cart_reminders():
    if is_quiet_hours():
        return
    if not await get_marketing_notifications_enabled():
        return
    try:
        result = await asyncio.to_thread(
            _sheets_get_params, {"action": "getPendingCartReminders"}
        )
        reminders = result.get("reminders", []) if isinstance(result, dict) else []
    except Exception as e:
        log.warning("Cart reminders yuklanmadi: %s", e)
        return

    now = datetime.now(_UZB_TZ)
    for r in reminders:
        try:
            added_str = r.get("qoshilgan_vaqt", "")
            if not added_str:
                continue
            added_time = datetime.fromisoformat(added_str.replace("Z", "+00:00"))
            hours_passed = (now - added_time).total_seconds() / 3600
            current_stage = int(r.get("bosqich", 0))
            next_stage    = current_stage + 1
            if next_stage > 4 or hours_passed < STAGE_DELAYS_HOURS[next_stage]:
                continue

            tg_id     = r["telegram_id"]
            user_data = await sheets_post({"action": "getUser", "telegram_id": tg_id})
            if not isinstance(user_data, dict) or not user_data.get("fcm_token"):
                continue

            fullname = user_data.get("fullname", "") or ""
            ism      = fullname.split()[0] if fullname.strip() else "Mijoz"
            text     = random.choice(CART_MESSAGES[next_stage]).format(
                ism=ism, mahsulot=r.get("mahsulot_nomi", "mahsulot")
            )
            await send_push_notification(
                user_data["fcm_token"], "Dalli Shop", text, {"type": "cart"}
            )
            await sheets_post({
                "action":     "updateCartReminderStage",
                "telegram_id": tg_id,
                "new_stage":   next_stage,
            })
        except Exception as e:
            log.warning("Cart reminder xato (%s): %s", r.get("telegram_id"), e)


# ── Database ───────────────────────────────────────────────────────────────────
async def init_db():
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("""
            CREATE TABLE IF NOT EXISTS admins (
                telegram_id  INTEGER PRIMARY KEY,
                name         TEXT    NOT NULL,
                categories   TEXT    NOT NULL DEFAULT '[]',
                active       INTEGER NOT NULL DEFAULT 1,
                created_at   TEXT    DEFAULT CURRENT_TIMESTAMP
            )
        """)
        await db.execute("""
            CREATE TABLE IF NOT EXISTS channels (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                channel_id TEXT    NOT NULL UNIQUE,
                label      TEXT    NOT NULL DEFAULT '',
                enabled    INTEGER NOT NULL DEFAULT 1,
                created_at TEXT    DEFAULT CURRENT_TIMESTAMP
            )
        """)
        # Superadmin — barcha kategoriyalar
        await db.execute("""
            INSERT OR IGNORE INTO admins (telegram_id, name, categories)
            VALUES (?, ?, ?)
        """, (SUPERADMIN_ID, "Superadmin", json.dumps(CATEGORIES)))
        await db.commit()


# ── App lifecycle ──────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    scheduler = AsyncIOScheduler(timezone=str(_UZB_TZ))
    scheduler.add_job(check_cart_reminders, "interval", minutes=30, id="cart_reminder")
    scheduler.start()
    yield
    scheduler.shutdown(wait=False)


app = FastAPI(lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Telegram login (mijoz ilovasi uchun) ────────────────────────────────────────
@app.get("/auth/start")
async def auth_start():
    """Ilova login boshlaydi — login_token va bot havolasini qaytaradi."""
    login_token = secrets.token_urlsafe(16)
    deep_link = f"https://t.me/{LOGIN_BOT_USERNAME}?start={login_token}"
    return {"login_token": login_token, "telegram_url": deep_link}


class FcmTokenIn(BaseModel):
    telegram_id: str
    fcm_token:   str


class CartSyncIn(BaseModel):
    telegram_id:   str
    mahsulot_nomi: str


class CartCancelIn(BaseModel):
    telegram_id: str


class BroadcastDiscountIn(BaseModel):
    product_id:    int | str
    mahsulot_nomi: str
    foiz:          int


class NotificationToggleIn(BaseModel):
    enabled: bool


@app.post("/auth/register-fcm-token")
async def register_fcm_token(data: FcmTokenIn):
    """Android ilovadan FCM token qabul qilib, Users varag'iga saqlaydi."""
    await sheets_post({
        "action":      "saveFcmToken",
        "telegram_id": data.telegram_id,
        "fcm_token":   data.fcm_token,
    })
    return {"status": "success"}


@app.post("/cart/sync")
async def cart_sync(data: CartSyncIn):
    """Savatga mahsulot qo'shilganda chaqiriladi — eslatma timerini boshlaydi."""
    await sheets_post({
        "action":        "syncCart",
        "telegram_id":   data.telegram_id,
        "mahsulot_nomi": data.mahsulot_nomi,
    })
    return {"status": "success"}


@app.post("/cart/cancel-reminder")
async def cart_cancel_reminder(data: CartCancelIn):
    """Savat bo'shaganda yoki buyurtma berилganда eslatmani bekor qiladi."""
    await sheets_post({
        "action":      "cancelCartReminder",
        "telegram_id": data.telegram_id,
    })
    return {"status": "success"}


@app.get("/auth/check")
async def auth_check(login_token: str):
    """Ilova har 2 soniyada tekshiradi — tasdiqlandimi?"""
    status = login_state.get(login_token)
    if status and status.get("tasdiqlandi"):
        return {
            "status": "confirmed",
            "telegram_id": status["telegram_id"],
            "ism": status["ism"],
            "username": status["username"],
            "fullname": status.get("fullname", ""),
            "phone": status.get("phone", ""),
            "address": status.get("address", ""),
            "location_link": status.get("address", ""),
        }
    return {"status": "pending"}


# ── Telegram auth ──────────────────────────────────────────────────────────────
def _verify_init_data(init_data: str) -> dict:
    """Telegram WebApp initData ni tekshiradi, user dict qaytaradi."""
    if not init_data:
        raise HTTPException(401, "initData yo'q")

    try:
        parsed = dict(urllib.parse.parse_qsl(init_data, strict_parsing=True))
    except Exception:
        raise HTTPException(401, "initData format xato")

    received_hash = parsed.pop("hash", "")
    if not received_hash:
        raise HTTPException(401, "Hash topilmadi")

    auth_date = int(parsed.get("auth_date", 0))
    if time() - auth_date > 86400:
        raise HTTPException(401, "initData muddati o'tgan (>24h)")

    data_check = "\n".join(f"{k}={v}" for k, v in sorted(parsed.items()))
    secret_key = hmac.new(b"WebAppData", BOT_TOKEN.encode(), hashlib.sha256).digest()
    expected   = hmac.new(secret_key, data_check.encode(), hashlib.sha256).hexdigest()

    if not hmac.compare_digest(expected, received_hash):
        raise HTTPException(401, "Hash mos emas")

    user = json.loads(parsed.get("user", "{}"))
    if not user.get("id"):
        raise HTTPException(401, "User topilmadi")
    return user


async def get_current_user(x_init_data: Annotated[str, Header()] = "") -> dict:
    tg = _verify_init_data(x_init_data)
    tg_id = int(tg["id"])

    async with aiosqlite.connect(DB_PATH) as db:
        async with db.execute(
            "SELECT name, categories, active FROM admins WHERE telegram_id = ?", (tg_id,)
        ) as cur:
            row = await cur.fetchone()

    if not row or not row[2]:
        raise HTTPException(403, "Ruxsat yo'q. Superadmin sizni qo'shishi kerak.")

    is_super = tg_id == SUPERADMIN_ID
    return {
        "telegram_id":  tg_id,
        "name":         tg.get("first_name", row[0]),
        "username":     tg.get("username", ""),
        "categories":   CATEGORIES if is_super else json.loads(row[1]),
        "is_superadmin": is_super,
    }


def require_super(user: dict = Depends(get_current_user)):
    if not user["is_superadmin"]:
        raise HTTPException(403, "Faqat superadmin uchun")
    return user


def _shorten_title(text: str, max_words: int = 7, max_chars: int = 50) -> str:
    import re
    # 1) Birinchi vergul/nuqta/qavsgacha bo'lgan qismni olish
    first_part = re.split(r'[,，。()（）\[\]【】|/]', text)[0].strip()
    if not first_part:
        first_part = text.strip()

    # 2) NOISE_TITLE_WORDS ro'yxatidagi keraksiz so'zlarni olib tashlash
    cleaned = first_part
    for w in NOISE_TITLE_WORDS:
        cleaned = re.sub(re.escape(w), "", cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(r'\s+', ' ', cleaned).strip(" -–—,.")

    if not cleaned:
        cleaned = first_part

    # 3) So'z sonini cheklash
    words = cleaned.split()
    if len(words) > max_words:
        cleaned = " ".join(words[:max_words])

    # 4) Belgi sonini cheklash (so'z o'rtasida kesilmasligi uchun)
    if len(cleaned) > max_chars:
        cleaned = cleaned[:max_chars].rsplit(" ", 1)[0]

    cleaned = cleaned[0].upper() + cleaned[1:] if cleaned else cleaned
    return cleaned or text[:max_chars]


def _do_translate_sync(text: str) -> str:
    params = urllib.parse.urlencode(
        {"client": "gtx", "sl": "auto", "tl": "uz", "dt": "t", "q": text}
    )
    url = f"https://translate.googleapis.com/translate_a/single?{params}"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=10) as r:
        data = json.loads(r.read())
    return "".join(seg[0] for seg in data[0] if seg[0])


@app.post("/admin/translate")
async def translate_text(payload: dict, _: dict = Depends(get_current_user)):
    text = (payload.get("text") or "").strip()
    if not text:
        raise HTTPException(400, "text bo'sh bo'lishi mumkin emas")
    try:
        translated_full = await asyncio.to_thread(_do_translate_sync, text)
    except Exception as e:
        log.warning("Tarjima xatoligi: %s", e)
        raise HTTPException(502, f"Tarjima xizmati ishlamadi: {e}")
    translated_short = _shorten_title(translated_full)
    return {"translated_full": translated_full, "translated_short": translated_short}


@app.get("/admin/settings")
async def get_admin_settings(_: dict = Depends(require_super)):
    try:
        return await asyncio.to_thread(_get_settings_sync)
    except Exception:
        return {"marketing_notifications_enabled": True}


@app.patch("/admin/settings/notifications")
async def toggle_notifications(data: NotificationToggleIn, _: dict = Depends(require_super)):
    await sheets_post({
        "action": "updateSettings",
        "key":    "marketing_notifications_enabled",
        "value":  data.enabled,
    })
    return {"status": "success"}


@app.post("/admin/broadcast-discount")
async def broadcast_discount(data: BroadcastDiscountIn, _: dict = Depends(get_current_user)):
    if not await get_marketing_notifications_enabled():
        return {"status": "disabled", "message": "Bildirishnomalar o'chirilgan"}

    template = get_discount_message(data.foiz)
    users    = await get_all_users_with_fcm_token()
    sent     = 0
    for user in users:
        try:
            fullname = user.get("fullname", "") or ""
            ism  = fullname.split()[0] if fullname.strip() else "Mijoz"
            text = template.format(ism=ism, mahsulot=data.mahsulot_nomi, foiz=data.foiz)
            await send_push_notification(
                user["fcm_token"], "Dalli Shop", text,
                {"type": "product", "product_id": str(data.product_id)},
            )
            sent += 1
        except Exception as e:
            log.warning("Broadcast FCM xato (%s): %s", user.get("telegram_id"), e)

    return {"status": "success", "sent_count": sent}


# ── Pydantic models ────────────────────────────────────────────────────────────
class ProductIn(BaseModel):
    name:             str
    price:            int
    discount:         int       = 0
    category:         str
    description:      str       = ""
    image_url:        str
    images:           list[str] = []
    rating:           float     = 4.5
    sold_count:       int       = 10
    discount_type:    str       = "doimiy"
    discount_expires: str       = ""
    auto_delete:      bool      = False
    send_push:        bool      = False
    variantlar_yoqilgan: bool = False
    variant_nomlari: list[str] = []
    variant_narxlari: list[int] = []


class AdminIn(BaseModel):
    telegram_id: int
    name:        str
    categories:  list[str]


class AdminPatch(BaseModel):
    name:       str | None        = None
    categories: list[str] | None  = None
    active:     bool | None       = None


class ProductPatch(BaseModel):
    active:   bool | None = None
    in_stock: bool | None = None


class ProductUpdate(BaseModel):
    name:                str | None       = None
    price:               int | None       = None
    discount:            int | None       = None
    category:            str | None       = None
    description:         str | None       = None
    image_url:           str | None       = None
    images:              list[str] | None = None
    variantlar_yoqilgan: bool | None      = None
    variant_nomlari:     list[str] | None = None
    variant_narxlari:    list[int] | None = None


# ── Helpers — Google Sheets ────────────────────────────────────────────────────
def _sheets_post(data: dict) -> dict:
    payload = json.dumps(data).encode()
    req = urllib.request.Request(
        APPS_SCRIPT_URL, data=payload,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=20) as r:
        result = json.loads(r.read())
    if isinstance(result, dict) and result.get("ok") is False:
        raise RuntimeError(result.get("error", "Apps Script xatolik"))
    return result


def _sheets_get() -> list:
    with urllib.request.urlopen(APPS_SCRIPT_URL, timeout=20) as r:
        return json.loads(r.read())


async def sheets_post(data: dict) -> dict:
    return await asyncio.to_thread(_sheets_post, data)


async def sheets_get() -> list:
    return await asyncio.to_thread(_sheets_get)


# ── Helpers — imgbb ────────────────────────────────────────────────────────────
def _imgbb_upload(image_bytes: bytes) -> str:
    b64  = base64.b64encode(image_bytes).decode()
    data = urllib.parse.urlencode({"key": IMGBB_API_KEY, "image": b64}).encode()
    req  = urllib.request.Request("https://api.imgbb.com/1/upload", data=data)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read())["data"]["url"]


async def imgbb_upload(image_bytes: bytes) -> str:
    return await asyncio.to_thread(_imgbb_upload, image_bytes)


# ── Helpers — Telegram notification ───────────────────────────────────────────
def _tg_notify(text: str):
    payload = json.dumps({
        "chat_id": SUPERADMIN_ID, "text": text, "parse_mode": "HTML"
    }).encode()
    req = urllib.request.Request(
        f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage",
        data=payload, headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=10):
            pass
    except Exception as e:
        log.warning("Notification failed: %s", e)


async def notify(text: str):
    await asyncio.to_thread(_tg_notify, text)


def _tg_channel_post(
    channel_id: str,
    image_url: str, name: str, category: str, price: int,
    discount: int, discount_type: str, discount_expires: str, product_id
) -> int:
    """Bitta kanalga rasm bilan post yuboradi, message_id qaytaradi (0 = xatolik)."""
    if not channel_id:
        return 0

    if discount:
        if discount_type == "vaqtinchalik" and discount_expires:
            try:
                from datetime import datetime
                s = discount_expires.strip().replace("Z", "+00:00")
                dt = datetime.fromisoformat(s)
                expires_str = dt.strftime("%d.%m.%Y %H:%M")
                discount_line = f"🔥 Chegirma: -{discount}%\n⏰ Tugaydi: <b>{expires_str}</b> gacha"
            except Exception:
                discount_line = f"🔥 Chegirma: -{discount}%"
        else:
            discount_line = f"🔥 Chegirma: -{discount}% (doimiy)"
    else:
        discount_line = ""

    lines = [
        f"🛍 Yangi tovar!\n\n📌 {_html.escape(name)}",
        f"📦 Kategoriya: {_html.escape(category)}",
        f"💰 Narx: {price:,} so'm".replace(",", " "),
    ]
    if discount_line:
        lines.append(discount_line)
    lines.append("\n👇 Ilovada ko'rish uchun:")
    caption = "\n".join(lines)

    button_url = (
        f"https://admin.eliboyev.uz/open?product={product_id}"
        if product_id else "https://admin.eliboyev.uz"
    )
    payload = json.dumps({
        "chat_id":      channel_id,
        "photo":        image_url,
        "caption":      caption,
        "parse_mode":   "HTML",
        "reply_markup": {"inline_keyboard": [[{"text": "📱 Ilovada ochish", "url": button_url}]]},
    }).encode()
    req = urllib.request.Request(
        f"https://api.telegram.org/bot{BOT_TOKEN}/sendPhoto",
        data=payload, headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read())
            return int(data.get("result", {}).get("message_id", 0))
    except Exception as e:
        log.warning("Kanalga post yuborishda xatolik: %s", e)
        return 0


async def channel_post(
    image_url: str, name: str, category: str, price: int,
    discount: int, discount_type: str, discount_expires: str, product_id
) -> int:
    """Bazadagi barcha yoqilgan kanallarga post yuboradi."""
    async with aiosqlite.connect(DB_PATH) as db:
        db.row_factory = aiosqlite.Row
        cur = await db.execute("SELECT channel_id FROM channels WHERE enabled = 1")
        rows = await cur.fetchall()

    if not rows:
        log.warning("Faol kanal yo'q — post yuborilmadi")
        return 0

    last_msg_id = 0
    for row in rows:
        msg_id = await asyncio.to_thread(
            _tg_channel_post,
            row["channel_id"],
            image_url, name, category, price, discount, discount_type, discount_expires, product_id
        )
        if msg_id:
            last_msg_id = msg_id
            log.info("Kanalga post yuborildi: %s → msg_id=%d", row["channel_id"], msg_id)
    return last_msg_id


async def wait_until_next_5min():
    """Keyingi 5 ga bo'linadigan daqiqagacha kutadi: :00, :05, :10, :15..."""
    now            = datetime.now(_UZB_TZ)
    current_minute = now.minute
    current_second = now.second
    next_minute    = ((current_minute // 5) + 1) * 5
    if next_minute >= 60:
        wait_seconds = (60 - current_minute) * 60 - current_second
    else:
        wait_seconds = (next_minute - current_minute) * 60 - current_second
    if wait_seconds > 0:
        await asyncio.sleep(wait_seconds)


async def _broadcast_discount_task(product_id, name: str, discount: int):
    pid_str = str(product_id) if product_id else ""
    log.info("Broadcast boshlandi: mahsulot=%s product_id=%s", name, pid_str or "YO'Q")
    template = get_discount_message(discount)
    users    = await get_all_users_with_fcm_token()
    sent     = 0
    for user in users:
        try:
            fullname = user.get("fullname", "") or ""
            ism  = fullname.split()[0] if fullname.strip() else "Mijoz"
            text = template.format(ism=ism, mahsulot=name, foiz=discount)
            data = {"type": "product", "product_id": pid_str} if pid_str else {"type": "general"}
            await send_push_notification(user["fcm_token"], "Dalli Shop", text, data)
            sent += 1
        except Exception as e:
            log.warning("Broadcast FCM xato (%s): %s", user.get("telegram_id"), e)
    log.info("Broadcast tugadi: %d ta push yuborildi (%s)", sent, name)


async def _channel_post_task(
    product_id, image_url: str, name: str, category: str,
    price: int, discount: int, discount_type: str, discount_expires: str,
):
    """Background task: kanalga post yuboradi va message_id ni Sheets ga saqlaydi."""
    msg_id = await channel_post(
        image_url, name, category, price,
        discount, discount_type, discount_expires, product_id
    )
    if msg_id and product_id is not None:
        try:
            await sheets_post({
                "type":       "updateMessageId",
                "id":         product_id,
                "message_id": msg_id,
            })
        except Exception as e:
            log.warning("telegram_message_id saqlashda xatolik: %s", e)


def _seconds_until_next_5min() -> int:
    """Keyingi 5 ga bo'linadigan daqiqagacha soniyalar: :00, :05, :10..."""
    now            = datetime.now(_UZB_TZ)
    current_minute = now.minute
    current_second = now.second
    next_minute    = ((current_minute // 5) + 1) * 5
    if next_minute >= 60:
        return (60 - current_minute) * 60 - current_second
    return (next_minute - current_minute) * 60 - current_second


async def _publish_at_5min(sheets_payload: dict, image_url: str, name: str,
                           category: str, price: int, discount: int,
                           discount_type: str, discount_expires: str,
                           wait_seconds: int, send_push: bool = False):
    """Belgilangan vaqt kutib, ham Sheets ga (ilova), ham kanalga bir vaqtda joylashtiradi."""
    await asyncio.sleep(wait_seconds)

    result     = await sheets_post(sheets_payload)
    product_id = result.get("id") if isinstance(result, dict) else None

    await _channel_post_task(
        product_id, image_url, name, category,
        price, discount, discount_type, discount_expires,
    )

    pending_products[:] = [p for p in pending_products if p.get("name") != name]

    if send_push and discount > 0:
        log.info("Vaqtinchalik mahsulot chiqarildi, broadcast boshlanmoqda: %s (%s%%)", name, discount)
        template = get_discount_message(discount)
        users    = await get_all_users_with_fcm_token()
        sent     = 0
        for user in users:
            try:
                fullname = user.get("fullname", "") or ""
                ism  = fullname.split()[0] if fullname.strip() else "Mijoz"
                text = template.format(ism=ism, mahsulot=name, foiz=discount)
                pid_str = str(product_id) if product_id else ""
                data = {"type": "product", "product_id": pid_str} if pid_str else {"type": "general"}
                await send_push_notification(user["fcm_token"], "Dalli Shop", text, data)
                sent += 1
            except Exception as e:
                log.warning("Broadcast (pending) FCM xato (%s): %s", user.get("telegram_id"), e)
        log.info("Broadcast tugadi: %d ta push yuborildi (%s)", sent, name)


# ── /api/me ────────────────────────────────────────────────────────────────────
@app.get("/api/me")
async def get_me(user: dict = Depends(get_current_user)):
    return user


# ── /api/categories ────────────────────────────────────────────────────────────
@app.get("/api/categories")
async def get_categories():
    return CATEGORIES


# ── /api/products ──────────────────────────────────────────────────────────────
@app.get("/api/products")
async def list_products(user: dict = Depends(get_current_user)):
    products = await sheets_get()
    if user["is_superadmin"]:
        return products
    allowed = set(user["categories"])
    return [
        p for p in products
        if p.get("category") in allowed and p.get("active", True)
    ]


@app.patch("/api/products/{product_id}")
async def patch_product(
    product_id: int,
    patch: ProductPatch,
    _: dict = Depends(require_super),
):
    if patch.active is None and patch.in_stock is None:
        raise HTTPException(400, "Hech narsa o'zgartirilmadi")

    if patch.active is not None:
        await sheets_post({
            "type":  "updateProduct",
            "id":    product_id,
            "field": "active",
            "value": int(patch.active),
        })
    if patch.in_stock is not None:
        await sheets_post({
            "type":  "updateProduct",
            "id":    product_id,
            "field": "inStock",
            "value": int(patch.in_stock),
        })

    name = str(product_id)
    if patch.active is not None:
        status = "faollashtirildi" if patch.active else "o'chirildi"
        await notify(f"{'✅' if patch.active else '🔴'} Tovar #{product_id} <b>{status}</b>")
    if patch.in_stock is not None:
        status = "mavjud" if patch.in_stock else "tugagan"
        await notify(f"{'📦' if patch.in_stock else '⚠️'} Tovar #{product_id} — <b>{status}</b> deb belgilandi")

    return {"ok": True}


@app.put("/api/products/{product_id}")
async def update_product(
    product_id: int,
    data: ProductUpdate,
    user: dict = Depends(require_super),
):
    payload = {k: v for k, v in data.model_dump().items() if v is not None}
    if not payload:
        raise HTTPException(400, "Hech narsa o'zgartirilmadi")

    await sheets_post({"type": "edit_product", "id": product_id, **payload})
    await notify(f"✏️ Tovar #{product_id} <b>tahrirlandi</b> ({user['name']})")
    return {"ok": True}


@app.delete("/api/products/{product_id}")
async def delete_product(
    product_id: int,
    user: dict = Depends(require_super),
):
    await sheets_post({"type": "delete_product", "id": product_id})
    await notify(f"🗑 Tovar #{product_id} <b>o'chirildi</b> ({user['name']})")
    return {"ok": True}


@app.post("/api/products", status_code=201)
async def add_product(product: ProductIn, user: dict = Depends(get_current_user)):
    if product.category not in user["categories"]:
        raise HTTPException(403, f"'{product.category}' kategoriyasiga ruxsat yo'q")

    discount_txt = f" (-{product.discount}%)" if product.discount else ""
    await notify(
        f"📦 <b>Yangi tovar</b>\n\n"
        f"👤 {user['name']}\n"
        f"📌 {product.name}\n"
        f"💰 {product.price:,} so'm{discount_txt}\n"
        f"📂 {product.category}"
    )

    sheets_payload = {
        "name":             product.name,
        "description":      product.description,
        "price":            product.price,
        "image_url":        product.image_url,
        "images":           product.images,
        "category":         product.category,
        "discount":         product.discount,
        "rating":           product.rating,
        "sold_count":       product.sold_count,
        "added_by":         str(user["telegram_id"]),
        "added_by_name":    user["name"],
        "discount_type":    product.discount_type,
        "discount_expires": product.discount_expires,
        "auto_delete":      product.auto_delete,
        "variantlar_yoqilgan": product.variantlar_yoqilgan,
        "variant_nomlari":     product.variant_nomlari,
        "variant_narxlari":    product.variant_narxlari,
    }

    is_temporary = (product.discount > 0 and product.discount_type == "vaqtinchalik")

    if is_temporary:
        wait_seconds = _seconds_until_next_5min()
        pending_products.append({"name": product.name, "wait_seconds": wait_seconds})
        asyncio.create_task(_publish_at_5min(
            sheets_payload, product.image_url, product.name, product.category,
            product.price, product.discount, product.discount_type,
            product.discount_expires, wait_seconds, send_push=product.send_push,
        ))
        mins, secs = divmod(wait_seconds, 60)
        return {
            "status":       "pending",
            "message":      f"Tovar {mins} daqiqa {secs} soniyadan so'ng joylashadi",
            "wait_seconds": wait_seconds,
        }

    result     = await sheets_post(sheets_payload)
    product_id = result.get("id") if isinstance(result, dict) else None
    asyncio.create_task(_channel_post_task(
        product_id, product.image_url, product.name, product.category,
        product.price, product.discount, product.discount_type, product.discount_expires,
    ))

    if product.send_push and product.discount > 0:
        log.info("Doimiy mahsulot saqlandi, broadcast boshlanmoqda: %s (%s%%)", product.name, product.discount)
        asyncio.create_task(_broadcast_discount_task(
            product_id, product.name, product.discount
        ))

    return result


# ── /api/upload ────────────────────────────────────────────────────────────────
@app.post("/api/upload")
async def upload_image(
    file: UploadFile = File(...),
    user: dict = Depends(get_current_user),
):
    content = await file.read()
    if len(content) > 10 * 1024 * 1024:
        raise HTTPException(413, "Fayl 10MB dan katta bo'lmasligi kerak")
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(400, "Faqat rasm fayllari")
    url = await imgbb_upload(content)
    return {"url": url}


# ── /api/admins (superadmin) ───────────────────────────────────────────────────
@app.get("/api/admins")
async def list_admins(_: dict = Depends(require_super)):
    async with aiosqlite.connect(DB_PATH) as db:
        async with db.execute(
            "SELECT telegram_id, name, categories, active, created_at FROM admins ORDER BY created_at"
        ) as cur:
            rows = await cur.fetchall()
    return [
        {
            "telegram_id":  r[0],
            "name":         r[1],
            "categories":   json.loads(r[2]),
            "active":       bool(r[3]),
            "created_at":   r[4],
            "is_superadmin": r[0] == SUPERADMIN_ID,
        }
        for r in rows
    ]


@app.post("/api/admins", status_code=201)
async def create_admin(admin: AdminIn, _: dict = Depends(require_super)):
    async with aiosqlite.connect(DB_PATH) as db:
        try:
            await db.execute(
                "INSERT INTO admins (telegram_id, name, categories) VALUES (?, ?, ?)",
                (admin.telegram_id, admin.name, json.dumps(admin.categories)),
            )
            await db.commit()
        except aiosqlite.IntegrityError:
            raise HTTPException(409, "Bu admin allaqachon mavjud")
    return {"ok": True}


@app.put("/api/admins/{telegram_id}")
async def update_admin(telegram_id: int, patch: AdminPatch, _: dict = Depends(require_super)):
    if telegram_id == SUPERADMIN_ID:
        raise HTTPException(403, "Superadminni o'zgartirish mumkin emas")

    fields, values = [], []
    if patch.name is not None:
        fields.append("name = ?"); values.append(patch.name)
    if patch.categories is not None:
        fields.append("categories = ?"); values.append(json.dumps(patch.categories))
    if patch.active is not None:
        fields.append("active = ?"); values.append(int(patch.active))
    if not fields:
        raise HTTPException(400, "Hech narsa o'zgartirilmadi")

    values.append(telegram_id)
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute(f"UPDATE admins SET {', '.join(fields)} WHERE telegram_id = ?", values)
        await db.commit()
    return {"ok": True}


@app.delete("/api/admins/{telegram_id}")
async def delete_admin(telegram_id: int, _: dict = Depends(require_super)):
    if telegram_id == SUPERADMIN_ID:
        raise HTTPException(403, "Superadminni o'chirish mumkin emas")
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("DELETE FROM admins WHERE telegram_id = ?", (telegram_id,))
        await db.commit()
    return {"ok": True}


# ── /api/channels ──────────────────────────────────────────────────────────────

class ChannelIn(BaseModel):
    channel_id: str
    label: str = ""

@app.get("/api/channels")
async def get_channels(_: dict = Depends(require_super)):
    async with aiosqlite.connect(DB_PATH) as db:
        db.row_factory = aiosqlite.Row
        cur = await db.execute("SELECT * FROM channels ORDER BY created_at")
        rows = await cur.fetchall()
    return [dict(r) for r in rows]

@app.post("/api/channels", status_code=201)
async def add_channel(ch: ChannelIn, _: dict = Depends(require_super)):
    cid = ch.channel_id.strip()
    if not cid:
        raise HTTPException(400, "channel_id bo'sh bo'lmasligi kerak")
    # Bot shu kanalga yoza olishini tekshiramiz
    check_req = urllib.request.Request(
        f"https://api.telegram.org/bot{BOT_TOKEN}/getChat",
        data=json.dumps({"chat_id": cid}).encode(),
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(check_req, timeout=8) as resp:
            result = json.loads(resp.read())
            if not result.get("ok"):
                raise HTTPException(400, f"Kanal topilmadi yoki bot admin emas: {result.get('description')}")
            chat_title = result.get("result", {}).get("title", cid)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(400, f"Telegram xatosi: {e}")
    label = ch.label.strip() or chat_title
    try:
        async with aiosqlite.connect(DB_PATH) as db:
            await db.execute(
                "INSERT INTO channels (channel_id, label) VALUES (?, ?)", (cid, label)
            )
            await db.commit()
    except Exception:
        raise HTTPException(409, "Bu kanal allaqachon qo'shilgan")
    return {"ok": True, "label": label}

@app.patch("/api/channels/{ch_id}/toggle")
async def toggle_channel(ch_id: int, _: dict = Depends(require_super)):
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("UPDATE channels SET enabled = 1 - enabled WHERE id = ?", (ch_id,))
        await db.commit()
    return {"ok": True}

@app.delete("/api/channels/{ch_id}")
async def delete_channel(ch_id: int, _: dict = Depends(require_super)):
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("DELETE FROM channels WHERE id = ?", (ch_id,))
        await db.commit()
    return {"ok": True}


# ── /api/stats ─────────────────────────────────────────────────────────────────
@app.get("/api/stats")
async def get_stats(_: dict = Depends(require_super)):
    products = await sheets_get()

    by_category: dict[str, int] = {}
    by_admin: dict[str, int]    = {}

    for p in products:
        cat = p.get("category", "Boshqa")
        by_category[cat] = by_category.get(cat, 0) + 1

        key = p.get("added_by_name") or p.get("added_by") or "Noma'lum"
        by_admin[key] = by_admin.get(key, 0) + 1

    async with aiosqlite.connect(DB_PATH) as db:
        async with db.execute("SELECT COUNT(*) FROM admins WHERE active = 1") as cur:
            (active_admins,) = await cur.fetchone()

    return {
        "total_products": len(products),
        "active_admins":  active_admins,
        "by_category":    by_category,
        "by_admin":       sorted(by_admin.items(), key=lambda x: -x[1]),
    }


# ── Buyurtmalar (mijoz ilovasi) ────────────────────────────────────────────────
class OrderItemDetail(BaseModel):
    nomi:    str
    variant: str | None = None
    soni:    int        = 1
    narx:    int        = 0
    rasm:    str | None = None


class OrderCreate(BaseModel):
    telegram_id:         str
    fullname:            str = ""
    phone:               str = ""
    location_link:       str = ""
    mahsulotlar:         str
    jami_summa:          int = 0
    mahsulot_rasm:       str | None             = None
    mahsulotlar_royxati: list[OrderItemDetail]  = []

    @field_validator("mahsulotlar")
    @classmethod
    def mahsulotlar_not_empty(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("mahsulotlar bo'sh bo'lishi mumkin emas")
        return v


class OrderStatusPatch(BaseModel):
    status: str


def _sheets_get_params(params: dict) -> dict:
    """Apps Script ga GET so'rov (query parametrlar bilan)."""
    query = urllib.parse.urlencode(params)
    with urllib.request.urlopen(f"{APPS_SCRIPT_URL}?{query}", timeout=20) as r:
        return json.loads(r.read())


def _tg_order_notify(text: str, photo_url: str | None = None):
    """Adminga buyurtma haqida xabar: rasm bo’lsa sendPhoto, bo’lmasa sendMessage."""
    if photo_url:
        try:
            payload = json.dumps({
                "chat_id": SUPERADMIN_ID,
                "photo":   photo_url,
                "caption": text[:1024],
            }).encode()
            req = urllib.request.Request(
                f"https://api.telegram.org/bot{BOT_TOKEN}/sendPhoto",
                data=payload, headers={"Content-Type": "application/json"},
            )
            with urllib.request.urlopen(req, timeout=10):
                return
        except Exception as e:
            log.warning("sendPhoto xatoligi, sendMessage ga o’tildi: %s", e)

    payload = json.dumps({"chat_id": SUPERADMIN_ID, "text": text}).encode()
    req = urllib.request.Request(
        f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage",
        data=payload, headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=10):
            pass
    except Exception as e:
        log.warning("Buyurtma xabari yuborilmadi: %s", e)


async def send_order_to_admin(order_id: str, data: OrderCreate):
    dash     = "—"
    apostr   = "’"
    fullname = data.fullname  or dash
    phone    = data.phone     or dash
    location = data.location_link or f"Manzil yo{apostr}q"
    jami_fmt = f"{data.jami_summa:,}".replace(",", " ")

    if data.mahsulotlar_royxati:
        for item in data.mahsulotlar_royxati:
            narx_fmt = f"{item.narx:,}".replace(",", " ")
            lines = [
                f"\U0001f4cb {order_id}",
                f"\U0001f4e6 {item.nomi}",
            ]
            if item.variant:
                lines.append(f"\U0001f3a8 Rangi: {item.variant}")
            lines.append(f"\U0001f522 Soni: {item.soni} ta")
            lines.append(f"\U0001f4b5 Narxi: {narx_fmt} so{apostr}m")
            caption = "\n".join(lines)
            await asyncio.to_thread(_tg_order_notify, caption, item.rasm)
            await asyncio.sleep(0.4)

        summary = (
            f"\U0001f6d2 Yangi buyurtma!\n\n"
            f"\U0001f4cb {order_id}\n"
            f"\U0001f464 {fullname}\n"
            f"\U0001f4de {phone}\n"
            f"\U0001f4cd {location}\n\n"
            f"\U0001f4b0 Jami: {jami_fmt} so{apostr}m"
        )
        await asyncio.to_thread(_tg_order_notify, summary)
    else:
        text = (
            f"\U0001f6d2 Yangi buyurtma!\n\n"
            f"\U0001f4cb {order_id}\n"
            f"\U0001f464 {fullname}\n"
            f"\U0001f4de {phone}\n"
            f"\U0001f4cd {location}\n\n"
            f"\U0001f4e6 Mahsulotlar:\n{data.mahsulotlar}\n\n"
            f"\U0001f4b0 Jami: {jami_fmt} so{apostr}m"
        )
        await asyncio.to_thread(_tg_order_notify, text, data.mahsulot_rasm)


@app.post("/order/create")
async def create_order(order: OrderCreate):
    """Buyurtmani Google Sheets ga saqlaydi va adminga Telegram xabari yuboradi."""
    result = await sheets_post({
        "action":        "saveOrder",
        "telegram_id":   order.telegram_id,
        "fullname":      order.fullname,
        "phone":         order.phone,
        "location_link": order.location_link,
        "mahsulotlar":   order.mahsulotlar,
        "jami_summa":    order.jami_summa,
        "sana":          datetime.now(_UZB_TZ).strftime("%Y-%m-%d %H:%M"),
    })
    order_id = result.get("order_id", "DS-?") if isinstance(result, dict) else "DS-?"

    await send_order_to_admin(order_id, order)

    return {"status": "success", "order_id": order_id}


@app.get("/order/list")
async def list_orders(telegram_id: str):
    """Foydalanuvchining buyurtmalari (telegram_id bo'yicha)."""
    return await asyncio.to_thread(
        _sheets_get_params, {"action": "getUserOrders", "telegram_id": telegram_id}
    )


@app.get("/order/payment-card")
async def get_payment_card():
    """To'lov kartasi ma'lumoti (.env dan o'qiladi)."""
    return {
        "card_number": PAYMENT_CARD_NUMBER,
        "card_holder": PAYMENT_CARD_HOLDER,
    }


def _tg_send_receipt(order_id: str, telegram_id: str, photo_url: str):
    """Admin botga chek rasmini inline tugmalar bilan yuboradi."""
    keyboard = json.dumps({
        "inline_keyboard": [[
            {"text": "✅ Tasdiqlash", "callback_data": f"pay:approve:{order_id}:{telegram_id}"},
            {"text": "❌ Rad etish",  "callback_data": f"pay:reject:{order_id}:{telegram_id}"},
        ]]
    })
    payload = json.dumps({
        "chat_id":      SUPERADMIN_ID,
        "photo":        photo_url,
        "caption":      f"💳 To'lov tasdiqlash\n📋 {order_id}",
        "parse_mode":   "HTML",
        "reply_markup": json.loads(keyboard),
    }).encode()
    req = urllib.request.Request(
        f"https://api.telegram.org/bot{BOT_TOKEN}/sendPhoto",
        data=payload, headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=15):
            pass
    except Exception as e:
        log.warning("Receipt admin ga yuborilmadi: %s", e)


@app.post("/order/upload-receipt")
async def upload_receipt(
    order_id:    str,
    telegram_id: str,
    file:        UploadFile = File(...),
):
    """Chek rasmi qabul qilib, imgbb ga yuklaydi va admin botga yuboradi."""
    content = await file.read()
    if len(content) > 20 * 1024 * 1024:
        raise HTTPException(413, "Fayl 20MB dan katta bo'lmasligi kerak")

    photo_url = await imgbb_upload(content)

    await sheets_post({
        "action":   "updateOrderStatus",
        "order_id": order_id,
        "status":   "Tolov_kutilmoqda",
    })

    await asyncio.to_thread(_tg_send_receipt, order_id, telegram_id, photo_url)

    return {"status": "success"}


# ── /api/orders (admin panel) ─────────────────────────────────────────────────
@app.get("/api/orders")
async def admin_list_orders(_: dict = Depends(get_current_user)):
    """Admin panel uchun barcha buyurtmalar."""
    return await asyncio.to_thread(
        _sheets_get_params, {"action": "getAllOrders"}
    )


@app.patch("/api/orders/{order_id}/status")
async def admin_update_order_status(
    order_id: str,
    body: OrderStatusPatch,
    _: dict = Depends(get_current_user),
):
    """Admin buyurtma holatini yangilaydi va mijozga FCM bildirishnoma yuboradi."""
    valid = {"Yangi", "Tolov_kutilmoqda", "Tasdiqlandi", "Rad_etildi", "Yo'lda", "Yetkazildi"}
    if body.status not in valid:
        raise HTTPException(400, f"Noto'g'ri holat: {body.status}")
    result = await sheets_post({
        "action":   "updateOrderStatus",
        "order_id": order_id,
        "status":   body.status,
    })
    # FCM bildirishnoma (background — status yangilanishi bunga bog'liq emas)
    telegram_id = result.get("telegram_id", "") if isinstance(result, dict) else ""
    if telegram_id:
        asyncio.create_task(_fcm_order_notify(telegram_id, order_id, body.status))
    return result


# ── /open — deep link redirect ────────────────────────────────────────────────
@app.get("/open")
async def open_in_app(product: str = None, order: str = None):
    if product:
        deep_link = f"dalli://product/{product}"
    elif order:
        deep_link = f"dalli://order/{order}"
    else:
        deep_link = "dalli://"

    return HTMLResponse(f"""<!DOCTYPE html>
<html lang="uz">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta http-equiv="refresh" content="1;url={deep_link}">
<title>Dalli Shop</title>
<style>
  * {{ margin:0; padding:0; box-sizing:border-box; }}
  body {{
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
  }}
  .card {{
    text-align: center;
    padding: 48px 32px;
    max-width: 380px;
    width: 90%;
  }}
  .logo {{
    font-size: 64px;
    margin-bottom: 16px;
  }}
  h1 {{
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 8px;
    background: linear-gradient(90deg, #e94560, #f5a623);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }}
  p {{
    font-size: 15px;
    color: rgba(255,255,255,0.65);
    margin-bottom: 36px;
    line-height: 1.6;
  }}
  .btn {{
    display: inline-block;
    background: linear-gradient(135deg, #e94560, #c62a47);
    color: white;
    text-decoration: none;
    font-size: 16px;
    font-weight: 600;
    padding: 16px 40px;
    border-radius: 50px;
    box-shadow: 0 8px 24px rgba(233,69,96,0.4);
    transition: transform 0.15s, box-shadow 0.15s;
  }}
  .btn:active {{ transform: scale(0.97); }}
  .hint {{
    margin-top: 24px;
    font-size: 13px;
    color: rgba(255,255,255,0.4);
  }}
</style>
<script>
  window.onload = function() {{
    window.location.href = "{deep_link}";
  }};
</script>
</head>
<body>
<div class="card">
  <div class="logo">🛍️</div>
  <h1>Dalli Shop</h1>
  <p>Eng arzon narxlarda sifatli tovarlar.<br>Ilovani yuklab, chegirmalarni o'tkazib yubormang!</p>
  <a class="btn" href="/download">📥 Ilovani yuklab olish</a>
  <div class="hint">Agar ilova ochilmasa — yuklab oling</div>
</div>
</body>
</html>
""")


# ── /download — Play Store sahifasi ───────────────────────────────────────────
@app.get("/download")
async def download_page():
    return HTMLResponse("""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dalli Shop</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
            background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
        }
        .card {
            background: rgba(255,255,255,0.07);
            backdrop-filter: blur(20px);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 28px;
            padding: 48px 32px;
            max-width: 380px;
            width: 100%;
            text-align: center;
        }
        .logo {
            font-size: 72px;
            margin-bottom: 8px;
            display: block;
        }
        .brand {
            font-size: 32px;
            font-weight: 800;
            color: #fff;
            margin-bottom: 8px;
            letter-spacing: -0.5px;
        }
        .brand span { color: #f97316; }
        .tagline {
            font-size: 15px;
            color: rgba(255,255,255,0.6);
            margin-bottom: 40px;
            line-height: 1.6;
        }
        .features {
            display: flex;
            flex-direction: column;
            gap: 12px;
            margin-bottom: 40px;
        }
        .feature {
            background: rgba(255,255,255,0.05);
            border-radius: 14px;
            padding: 14px 18px;
            display: flex;
            align-items: center;
            gap: 12px;
            text-align: left;
        }
        .feature-icon { font-size: 24px; }
        .feature-text { color: rgba(255,255,255,0.85); font-size: 14px; font-weight: 500; }
        .btn {
            display: block;
            background: linear-gradient(135deg, #f97316, #ef4444);
            color: white;
            padding: 18px 32px;
            border-radius: 16px;
            text-decoration: none;
            font-weight: 700;
            font-size: 17px;
            margin-bottom: 16px;
            box-shadow: 0 8px 32px rgba(249,115,22,0.4);
            transition: transform 0.2s;
        }
        .btn:active { transform: scale(0.97); }
        .coming-soon {
            font-size: 13px;
            color: rgba(255,255,255,0.4);
        }
        .dots {
            display: flex;
            justify-content: center;
            gap: 6px;
            margin-top: 24px;
        }
        .dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: rgba(255,255,255,0.2);
        }
        .dot.active { background: #f97316; }
    </style>
</head>
<body>
    <div class="card">
        <span class="logo">🛍️</span>
        <div class="brand">Dalli <span>Shop</span></div>
        <p class="tagline">Xitoydan sifatli tovarlar.<br>Eng arzon narxlarda.</p>
        <div class="features">
            <div class="feature">
                <span class="feature-icon">🔥</span>
                <span class="feature-text">Har kuni yangi chegirmalar</span>
            </div>
            <div class="feature">
                <span class="feature-icon">🚚</span>
                <span class="feature-text">Toshkentga tez yetkazish</span>
            </div>
            <div class="feature">
                <span class="feature-icon">🔔</span>
                <span class="feature-text">Chegirma bildirishnomalari</span>
            </div>
            <div class="feature">
                <span class="feature-icon">✅</span>
                <span class="feature-text">Sifat kafolati</span>
            </div>
        </div>
        <a href="#" class="btn">📥 Ilovani yuklab olish</a>
        <p class="coming-soon">✨ Tez orada Play Store da!</p>
        <div class="dots">
            <div class="dot active"></div>
            <div class="dot"></div>
            <div class="dot"></div>
        </div>
    </div>
</body>
</html>
""")


# ── Static frontend ────────────────────────────────────────────────────────────
import os
frontend_dir = os.path.join(os.path.dirname(__file__), "..", "frontend", "dist")
if os.path.isdir(frontend_dir):
    from fastapi.responses import FileResponse

    @app.get("/{full_path:path}")
    async def serve_frontend(full_path: str):
        file_path = os.path.join(frontend_dir, full_path)
        if os.path.isfile(file_path):
            return FileResponse(file_path)
        return FileResponse(os.path.join(frontend_dir, "index.html"))
