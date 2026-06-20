#!/usr/bin/env python3
"""
Dalli Shop — Telegram login bot (@dalli_login_robot)

Oqim:
  1. Ilova /auth/start chaqiradi → login_token va t.me havolasini oladi.
  2. Foydalanuvchi havolani ochadi → /start <login_token> bilan bu botga keladi.
  3. Bot "Kirishni tasdiqlash" tugmasini ko'rsatadi.
  4. Tasdiqlagandan keyin bot ketma-ket so'raydi:
       a) 📱 telefon raqami  (KeyboardButton request_contact)
       b) 👤 to'liq ism
       c) 📍 yetkazib berish manzili
  5. Hammasi Google Sheets "Users" varag'iga saqlanadi va login_state.json ga
     yoziladi (main.py /auth/check o'qiydi). Faqat shundan keyin "confirmed".

Serverda ishga tushirish:  python login_bot.py
Token .env dagi LOGIN_BOT_TOKEN dan o'qiladi (kod ichiga yozilmaydi).
"""

import asyncio
import os
from datetime import datetime
from zoneinfo import ZoneInfo

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

import requests
from telegram import (
    InlineKeyboardButton,
    InlineKeyboardMarkup,
    KeyboardButton,
    ReplyKeyboardMarkup,
    ReplyKeyboardRemove,
    Update,
)
from telegram.ext import (
    Application,
    CallbackQueryHandler,
    CommandHandler,
    ContextTypes,
    ConversationHandler,
    MessageHandler,
    filters,
)

import login_state

LOGIN_BOT_TOKEN = os.getenv("LOGIN_BOT_TOKEN")
APPS_SCRIPT_URL = os.getenv(
    "APPS_SCRIPT_URL",
    "https://script.google.com/macros/s/AKfycbwYNusH54O3kyMAVcdkzpOaBiejRLrvj6EcXtfgh1m37aG79ZiUYRG_OcOEUa3GSkFi8A/exec",
)
UZB_TZ = ZoneInfo("Asia/Tashkent")

# ConversationHandler holatlari
PHONE, FULLNAME, ADDRESS = range(3)


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Ilovadan kelgan login so'rovi: /start <login_token>"""
    args = context.args
    user = update.effective_user

    if not args:
        await update.message.reply_text(
            "👋 Salom! Dalli Shop ilovasiga kirish uchun "
            "ilovadagi 'Telegram orqali kirish' tugmasini bosing."
        )
        return

    login_token = args[0]

    # Login so'rovini umumiy holatga yozish (hali tasdiqlanmagan)
    login_state.add_pending(
        login_token=login_token,
        telegram_id=user.id,
        ism=user.full_name,
        username=user.username or "",
    )

    keyboard = InlineKeyboardMarkup([[
        InlineKeyboardButton("✅ Kirishni tasdiqlash", callback_data=f"confirm:{login_token}")
    ]])

    await update.message.reply_text(
        f"🔐 Dalli Shop ilovasiga kirmoqchimisiz?\n\n"
        f"👤 {user.full_name}\n\n"
        f"Tasdiqlash uchun tugmani bosing:",
        reply_markup=keyboard,
    )


def _get_user_from_sheets(telegram_id: int) -> dict:
    """Google Sheets 'Users' varag'idan foydalanuvchini qidiradi (bloklovchi — thread'da chaqiriladi).
    Mavjud bo'lsa {'found': True, ...}, bo'lmasa {'found': False}."""
    resp = requests.post(
        APPS_SCRIPT_URL,
        json={"action": "getUser", "telegram_id": str(telegram_id)},
        timeout=10,
    )
    return resp.json()


def _save_user_to_sheets(user_data: dict) -> None:
    """Google Sheets 'Users' varag'iga saqlash (bloklovchi — thread'da chaqiriladi)."""
    requests.post(
        APPS_SCRIPT_URL,
        json={
            "action": "saveUser",
            "telegram_id": str(user_data["telegram_id"]),
            "username": user_data.get("username", ""),
            "fullname": user_data.get("fullname", ""),
            "phone": user_data.get("phone", ""),
            "latitude": user_data.get("latitude", ""),
            "longitude": user_data.get("longitude", ""),
            "location_link": user_data.get("location_link", ""),
            "ro_yxat_sana": datetime.now(UZB_TZ).strftime("%Y-%m-%d %H:%M"),
        },
        timeout=10,
    )


async def confirm_login(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Tasdiqlash tugmasi bosilganda → telefon raqamini so'rash (ConversationHandler boshlanishi)."""
    query = update.callback_query
    await query.answer()

    login_token = query.data.split(":", 1)[1]

    pending = login_state.get(login_token)
    if pending is None:
        await query.edit_message_text("⚠️ Bu so'rov eskirgan. Qaytadan urinib ko'ring.")
        return ConversationHandler.END

    # Joriy ro'yxatdan o'tish jarayonini saqlash
    context.user_data["login_token"] = login_token

    # Mavjud foydalanuvchini tekshirish — bo'lsa, qayta ro'yxatdan o'tkazmasdan to'g'ridan-to'g'ri login
    try:
        existing = await asyncio.to_thread(_get_user_from_sheets, query.from_user.id)
    except Exception as e:  # noqa: BLE001
        print(f"getUser xatosi: {e}")
        existing = {"found": False}

    if existing.get("found"):
        fullname = str(existing.get("fullname") or pending.get("ism") or "")
        phone = str(existing.get("phone") or "")
        location_link = str(existing.get("location_link") or "")

        record = login_state.complete_registration(
            login_token=login_token,
            fullname=fullname,
            phone=phone,
            address=location_link,
        )
        if record is None:
            await query.edit_message_text("⚠️ Bu so'rov eskirgan. Qaytadan urinib ko'ring.")
            return ConversationHandler.END

        await query.edit_message_text(
            f"✅ Xush kelibsiz, {fullname}!\n\nEndi ilovaga qaytishingiz mumkin."
        )
        context.user_data.clear()
        return ConversationHandler.END

    # Yangi foydalanuvchi — to'liq ro'yxatdan o'tish oqimi (telefon → ism → manzil)
    await query.edit_message_text("✅ Tasdiqlandi!")

    keyboard = ReplyKeyboardMarkup(
        [[KeyboardButton("📱 Raqamni ulashish", request_contact=True)]],
        resize_keyboard=True,
        one_time_keyboard=True,
    )
    await context.bot.send_message(
        chat_id=query.from_user.id,
        text="📱 Davom etish uchun telefon raqamingizni ulashing",
        reply_markup=keyboard,
    )
    return PHONE


async def get_phone(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Telefon raqami kelganda — faqat foydalanuvchining o'z O'zbekiston raqami."""
    user_id = update.effective_user.id
    contact = update.message.contact

    # 1. Faqat kontakt tugmasi orqali (qo'lda yozishni rad et)
    if not contact:
        await update.message.reply_text(
            "⚠️ Iltimos, pastdagi '📱 Raqamni ulashish' tugmasini bosing. "
            "Raqamni qo'lda yozish mumkin emas."
        )
        return PHONE

    # 2. Faqat o'zining raqami (boshqaning kontaktini rad et)
    if contact.user_id != user_id:
        await update.message.reply_text(
            "⚠️ Faqat o'zingizning raqamingizni ulashing. "
            "Boshqa kishining kontakti qabul qilinmaydi."
        )
        return PHONE

    # 3. Faqat O'zbekiston raqami (+998)
    phone_clean = contact.phone_number.replace(" ", "").replace("-", "")
    if not phone_clean.startswith("+"):
        phone_clean = "+" + phone_clean

    if not phone_clean.startswith("+998"):
        await update.message.reply_text(
            "⚠️ Faqat O'zbekiston (+998) raqami qabul qilinadi."
        )
        return PHONE

    context.user_data["phone"] = phone_clean

    await update.message.reply_text(
        "👤 Ism Familiyangizni yozing",
        reply_markup=ReplyKeyboardRemove(),
    )
    return FULLNAME


async def get_fullname(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Ism familiya kelganda → joylashuvni so'rash (xarita nuqtasi)."""
    context.user_data["fullname"] = update.message.text.strip()

    keyboard = ReplyKeyboardMarkup(
        [[KeyboardButton("📍 Joylashuvni yuborish", request_location=True)]],
        resize_keyboard=True,
        one_time_keyboard=True,
    )
    await update.message.reply_text(
        "📍 Qayerdan buyurtma bermoqchisiz?\n"
        "Pastdagi tugmani bosib joylashuvingizni yuboring:",
        reply_markup=keyboard,
    )
    return ADDRESS


async def get_address(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Joylashuv kelganda — yakuniy saqlash va loginni tasdiqlash."""
    location = update.message.location

    # Faqat location qabul qilinadi
    if not location:
        keyboard = ReplyKeyboardMarkup(
            [[KeyboardButton("📍 Joylashuvni yuborish", request_location=True)]],
            resize_keyboard=True,
            one_time_keyboard=True,
        )
        await update.message.reply_text(
            "⚠️ Iltimos, '📍 Joylashuvni yuborish' tugmasini bosing.",
            reply_markup=keyboard,
        )
        return ADDRESS

    lat = location.latitude
    lon = location.longitude
    location_link = f"https://maps.google.com/?q={lat},{lon}"
    context.user_data["latitude"] = lat
    context.user_data["longitude"] = lon
    context.user_data["location_link"] = location_link

    login_token = context.user_data.get("login_token")
    if not login_token:
        await update.message.reply_text(
            "⚠️ So'rov eskirgan. Ilovadan qaytadan kiring.",
            reply_markup=ReplyKeyboardRemove(),
        )
        return ConversationHandler.END

    fullname = context.user_data.get("fullname", "")
    phone = context.user_data.get("phone", "")

    # Login holatini yangilash (tasdiqlandi=True bo'ladi) — main.py /auth/check shu yerdan o'qiydi.
    # Manzil sifatida Google Maps havolasi saqlanadi.
    record = login_state.complete_registration(
        login_token=login_token,
        fullname=fullname,
        phone=phone,
        address=location_link,
    )
    if record is None:
        await update.message.reply_text(
            "⚠️ So'rov eskirgan. Ilovadan qaytadan kiring.",
            reply_markup=ReplyKeyboardRemove(),
        )
        return ConversationHandler.END

    # Google Sheets 'Users' varag'iga saqlash (event loop'ni bloklamaslik uchun thread'da)
    try:
        await asyncio.to_thread(
            _save_user_to_sheets,
            {
                "telegram_id": record["telegram_id"],
                "username": record.get("username", ""),
                "fullname": fullname,
                "phone": phone,
                "latitude": lat,
                "longitude": lon,
                "location_link": location_link,
            },
        )
    except Exception as e:  # noqa: BLE001
        print(f"User saqlashda xato: {e}")

    await update.message.reply_text(
        "✅ Ro'yxatdan o'tdingiz!\n\n"
        f"👤 {fullname}\n"
        f"📱 {phone}\n"
        f"📍 Joylashuv saqlandi\n\n"
        "Endi ilovaga qaytishingiz mumkin",
        reply_markup=ReplyKeyboardRemove(),
    )
    context.user_data.clear()
    return ConversationHandler.END


async def cancel(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Ro'yxatdan o'tishni bekor qilish."""
    context.user_data.clear()
    await update.message.reply_text(
        "❌ Bekor qilindi. Ilovadan qaytadan kirishingiz mumkin.",
        reply_markup=ReplyKeyboardRemove(),
    )
    return ConversationHandler.END


def main():
    if not LOGIN_BOT_TOKEN:
        raise SystemExit("❌ LOGIN_BOT_TOKEN topilmadi — .env faylga qo'shing.")

    app = Application.builder().token(LOGIN_BOT_TOKEN).build()

    conv_handler = ConversationHandler(
        entry_points=[CallbackQueryHandler(confirm_login, pattern="^confirm:")],
        states={
            PHONE:    [MessageHandler(filters.CONTACT | (filters.TEXT & ~filters.COMMAND), get_phone)],
            FULLNAME: [MessageHandler(filters.TEXT & ~filters.COMMAND, get_fullname)],
            ADDRESS:  [MessageHandler(filters.LOCATION | (filters.TEXT & ~filters.COMMAND), get_address)],
        },
        fallbacks=[CommandHandler("cancel", cancel)],
        per_message=False,
    )

    app.add_handler(CommandHandler("start", start))
    app.add_handler(conv_handler)

    print("🔐 Login bot ishga tushdi")
    app.run_polling(allowed_updates=Update.ALL_TYPES)


if __name__ == "__main__":
    main()
