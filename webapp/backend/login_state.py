#!/usr/bin/env python3
"""
Login holati — login_bot.py va main.py o'rtasida umumiy.

Eng oddiy va ishonchli yechim: kichik JSON fayl (login_state.json).
- Faqat login_bot.py YOZADI (yangi so'rov va tasdiqlash).
- main.py faqat O'QIYDI (/auth/check).
Shu sababli process'lar orasida yozish-yozish poygasi yo'q,
o'qish esa atomik os.replace tufayli har doim butun faylni ko'radi.

Ikkala fayl ham bitta serverda, bitta papkada ishlashi kerak
(yoki LOGIN_STATE_FILE env orqali umumiy yo'l ko'rsatilsin).
"""

import json
import os
import tempfile
import time
from threading import Lock

# Standart: shu fayl yonidagi login_state.json
LOGIN_STATE_FILE = os.getenv(
    "LOGIN_STATE_FILE",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "login_state.json"),
)

# Login tokeni qancha vaqt amal qiladi (soniya)
TTL_SECONDS = 600  # 10 daqiqa

_lock = Lock()


def _read() -> dict:
    try:
        with open(LOGIN_STATE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return {}


def _write(state: dict) -> None:
    """Atomik yozish — yarim yozilgan fayl hech qachon o'qilmaydi."""
    directory = os.path.dirname(LOGIN_STATE_FILE) or "."
    fd, tmp = tempfile.mkstemp(dir=directory, suffix=".tmp")
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            json.dump(state, f, ensure_ascii=False)
        os.replace(tmp, LOGIN_STATE_FILE)
    except Exception:
        try:
            os.remove(tmp)
        except OSError:
            pass
        raise


def _cleanup(state: dict) -> dict:
    """Eskirgan tokenlarni tashlab yuboradi."""
    now = time.time()
    return {
        token: v
        for token, v in state.items()
        if now - float(v.get("created_at", now)) <= TTL_SECONDS
    }


def add_pending(login_token: str, telegram_id: int, ism: str, username: str) -> None:
    """Ilovadan kelgan yangi login so'rovi (hali tasdiqlanmagan)."""
    with _lock:
        state = _cleanup(_read())
        state[login_token] = {
            "telegram_id": telegram_id,
            "ism": ism,
            "username": username,
            "tasdiqlandi": False,
            "created_at": time.time(),
        }
        _write(state)


def confirm(login_token: str) -> dict | None:
    """Tasdiqlash tugmasi bosilganda. Topilmasa None qaytadi.

    Eslatma: bu yerda tasdiqlandi=True QILINMAYDI. Foydalanuvchi avval
    telefon, to'liq ism va manzilni kiritishi kerak — yakuniy tasdiq
    complete_registration() orqali beriladi.
    """
    with _lock:
        state = _cleanup(_read())
        return state.get(login_token)


def complete_registration(
    login_token: str,
    fullname: str,
    phone: str,
    address: str,
) -> dict | None:
    """Ro'yxatdan o'tish yakunlandi — qo'shimcha maydonlarni yozadi va
    tasdiqlandi=True qiladi. Shundan keyingina /auth/check 'confirmed' beradi.
    Token topilmasa None qaytadi."""
    with _lock:
        state = _cleanup(_read())
        if login_token not in state:
            return None
        state[login_token]["fullname"] = fullname
        state[login_token]["phone"] = phone
        state[login_token]["address"] = address
        state[login_token]["tasdiqlandi"] = True
        _write(state)
        return state[login_token]


def get(login_token: str) -> dict | None:
    """main.py /auth/check uchun — joriy holatni qaytaradi."""
    with _lock:
        return _cleanup(_read()).get(login_token)
