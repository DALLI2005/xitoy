#!/usr/bin/env python3
"""
Bir martalik dev skript — LOKAL sinov uchun soxta mijozlar (app_users) va
sevimlilar (favorites) yaratadi. Maqsad: Mijozlar sahifasidagi filter/
saralash/sahifalash funksiyalarini real hajmdagi ma'lumot bilan sinash.

FAQAT LOKAL fayllarga yozadi (products.db/users.db joriy papkada yoki
PRODUCTS_DB_PATH/USERS_DB_PATH environment variable orqali ko'rsatilgan
joyda). Production yo'liga (/opt/xitoy_webapp/...) yozishdan qat'iy
saqlanadi — pastdagi tekshiruvga qarang.

Qayta ishga tushirish xavfsiz: oldingi seed yozuvlari (user_id 9900000000
oralig'ida, pastga qarang) avval o'chiriladi, so'ng qaytadan yaratiladi —
dublikat qolmaydi.

Ishlatilishi:
    cd webapp/backend
    python3 seed_fake_users.py [--count 100]
"""

import argparse
import hashlib
import os
import random
import sqlite3
import sys
import time
from pathlib import Path

# ── Xavfsizlik: production papkasiga yozishni butunlay taqiqlaymiz ─────────────
_PRODUCTION_PREFIX = "/opt/xitoy_webapp"

PRODUCTS_DB_PATH = os.environ.get("PRODUCTS_DB_PATH", "products.db")
USERS_DB_PATH    = os.environ.get("USERS_DB_PATH", "users.db")

for _label, _path in (("PRODUCTS_DB_PATH", PRODUCTS_DB_PATH), ("USERS_DB_PATH", USERS_DB_PATH)):
    if str(Path(_path).resolve()).startswith(_PRODUCTION_PREFIX):
        print(f"XATO: {_label} production yo'liga ishora qilmoqda ({_path}). "
              f"Bu skript FAQAT lokal sinov uchun — to'xtatildi.")
        sys.exit(1)

# ── main.py dagi _hash_password bilan bir xil (mustaqil nusxa — main.py ni
#    import qilish FastAPI ilovasini yon-ta'sirlar bilan ishga tushirib
#    yuboradi, shuning uchun faqat kerakli funksiya qayta yozilgan) ─────────────
def _hash_password(password: str, salt: str) -> str:
    return hashlib.scrypt(password.encode(), salt=salt.encode(), n=16384, r=8, p=1).hex()


# Seed yozuvlari shu diapazonda — qayta ishga tushirishda avval shu
# oraliqdagilar tozalanadi, real (tasodifiy generatsiya qilingan) user_id lar
# bilan deyarli hech qachon to'qnashmaydi.
SEED_ID_START = 9_900_000_000
SEED_ID_END   = 9_900_000_999  # 1000 tagacha joy — 100 tadan ancha katta zahira

MALE_FIRST_NAMES = [
    "Aziz", "Sardor", "Bekzod", "Jasur", "Otabek", "Ravshan", "Ulugbek", "Farrux",
    "Islom", "Anvar", "Bahodir", "Dostonbek", "Sherzod", "Davron", "Rustam",
    "Shavkat", "Akmal", "Nodirbek", "Sarvar", "Ilhom", "Jahongir", "Murodjon",
    "Elyor", "Xurshid", "Botir",
]
FEMALE_FIRST_NAMES = [
    "Malika", "Dilnoza", "Gulnora", "Nodira", "Feruza", "Zarina", "Shahnoza",
    "Madina", "Sevinch", "Nigora", "Kamola", "Lola", "Nilufar", "Zebo",
    "Munisa", "Gulbahor", "Diyora", "Mohira", "Yulduz", "Sabina", "Ozoda",
    "Xosiyat", "Muattar", "Iroda", "Gulchehra",
]
LAST_NAMES_BASE = [
    "Karimov", "Yusupov", "Toshpulatov", "Rashidov", "Nazarov", "Ismoilov",
    "Ergashev", "Xolmatov", "Yuldashev", "Saidov", "Abdullayev", "Mirzayev",
    "Qodirov", "Tursunov", "Norqulov", "Bekov", "Ahmedov", "Ganiyev",
    "Umarov", "Sultonov", "Rustamov", "Xudoyberdiyev", "Aliyev", "Rahimov",
    "Nurmatov", "Shukurov", "Ibragimov", "Xasanov", "Yoqubov", "Mamatov",
]

OPERATOR_CODES = ["90", "91", "93", "94", "95", "97", "98", "99"]


def feminize(surname: str) -> str:
    """Erkak familiyasini ayol formasiga o'giradi (Karimov -> Karimova)."""
    return surname + "a"


def gen_fullname(used: set) -> tuple[str, bool]:
    """Takrorlanmaydigan ism-familiya generatsiya qiladi. (fullname, is_female) qaytaradi."""
    for _ in range(200):
        is_female = random.random() < 0.45
        first = random.choice(FEMALE_FIRST_NAMES if is_female else MALE_FIRST_NAMES)
        surname = random.choice(LAST_NAMES_BASE)
        if is_female:
            surname = feminize(surname)
        full = f"{first} {surname}"
        if full not in used:
            used.add(full)
            return full, is_female
    # 200 urinishdan keyin ham topilmasa (deyarli imkonsiz) — raqam qo'shib majburlaymiz
    full = f"{full} {random.randint(2, 99)}"
    used.add(full)
    return full, is_female


def gen_phone(used: set) -> str:
    while True:
        phone = f"+998{random.choice(OPERATOR_CODES)}{random.randint(0, 9_999_999):07d}"
        if phone not in used:
            used.add(phone)
            return phone


def gen_created_at() -> int:
    """So'nggi 6 oy (180 kun) ichida tasodifiy vaqt — tarqoq bo'lishi uchun uniform."""
    now = int(time.time())
    seconds_ago = random.randint(0, 180 * 86400)
    return now - seconds_ago


def gen_favorites_count() -> int:
    """Notekis taqsimot: ko'pchilik kam, ozchilik ko'p sevimliga ega."""
    bucket = random.choices(
        ["zero", "few", "some", "many"],
        weights=[40, 35, 15, 10],
        k=1,
    )[0]
    return {
        "zero": 0,
        "few":  random.randint(1, 3),
        "some": random.randint(4, 9),
        "many": random.randint(10, 15),
    }[bucket]


def get_product_ids(products_db_path: str) -> list[str]:
    """Mahalliy products.db dan haqiqiy tovar id'larini o'qiydi. Bo'sh/mavjud
    bo'lmasa — soxta id'lar bilan fallback qiladi (foydalanuvchi ruxsat bergan)."""
    if os.path.exists(products_db_path):
        try:
            conn = sqlite3.connect(products_db_path)
            rows = conn.execute("SELECT id FROM products").fetchall()
            conn.close()
            if rows:
                ids = [str(r[0]) for r in rows]
                print(f"products.db dan {len(ids)} ta haqiqiy tovar id topildi, ulardan foydalaniladi.")
                return ids
        except sqlite3.OperationalError:
            pass
    print("products.db bo'sh yoki topilmadi — soxta tovar id'lari (1..10) ishlatiladi.")
    return [str(i) for i in range(1, 11)]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--count", type=int, default=100, help="Nechta soxta mijoz yaratish (default: 100)")
    args = parser.parse_args()

    print(f"Users DB:    {Path(USERS_DB_PATH).resolve()}")
    print(f"Products DB: {Path(PRODUCTS_DB_PATH).resolve()}")
    print()

    product_ids = get_product_ids(PRODUCTS_DB_PATH)

    conn = sqlite3.connect(USERS_DB_PATH)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS app_users (
            user_id       INTEGER PRIMARY KEY,
            phone         TEXT UNIQUE NOT NULL,
            fullname      TEXT NOT NULL,
            password_hash TEXT NOT NULL,
            salt          TEXT NOT NULL,
            session_token TEXT,
            created_at    INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS favorites (
            telegram_id  INTEGER NOT NULL,
            product_id   TEXT    NOT NULL,
            created_at   INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
            PRIMARY KEY (telegram_id, product_id)
        )
    """)

    # Oldingi seed yozuvlarini tozalaymiz — qayta ishga tushirish dublikat yaratmaydi
    deleted_favs = conn.execute(
        "DELETE FROM favorites WHERE telegram_id BETWEEN ? AND ?", (SEED_ID_START, SEED_ID_END)
    ).rowcount
    deleted_users = conn.execute(
        "DELETE FROM app_users WHERE user_id BETWEEN ? AND ?", (SEED_ID_START, SEED_ID_END)
    ).rowcount
    if deleted_users:
        print(f"Oldingi seed: {deleted_users} ta mijoz, {deleted_favs} ta sevimli tozalandi.\n")

    # Real jadvaldagi telefon raqamlar bilan to'qnashmasligi uchun mavjudlarini ham hisobga olamiz
    existing_phones = {r[0] for r in conn.execute("SELECT phone FROM app_users").fetchall()}
    used_names: set = set()
    used_phones: set = set(existing_phones)

    pwd_hash_cache: dict[str, str] = {}

    def hashed_password(salt: str) -> str:
        if salt not in pwd_hash_cache:
            pwd_hash_cache[salt] = _hash_password("test123456", salt)
        return pwd_hash_cache[salt]

    total_favorites = 0
    for i in range(args.count):
        user_id = SEED_ID_START + i
        fullname, _ = gen_fullname(used_names)
        phone = gen_phone(used_phones)
        salt = f"seedsalt{i}"
        pwd_hash = hashed_password(salt)
        created_at = gen_created_at()

        conn.execute(
            "INSERT INTO app_users (user_id, phone, fullname, password_hash, salt, session_token, created_at) "
            "VALUES (?,?,?,?,?,?,?)",
            (user_id, phone, fullname, pwd_hash, salt, None, created_at),
        )

        fav_count = min(gen_favorites_count(), len(product_ids))
        fav_products = random.sample(product_ids, fav_count) if fav_count else []
        for pid in fav_products:
            conn.execute(
                "INSERT OR IGNORE INTO favorites (telegram_id, product_id, created_at) VALUES (?,?,?)",
                (user_id, pid, created_at),
            )
        total_favorites += len(fav_products)

    conn.commit()

    total_users = conn.execute("SELECT COUNT(*) FROM app_users").fetchone()[0]
    conn.close()

    print(f"{args.count} ta soxta mijoz yaratildi ({total_favorites} ta sevimli yozuv bilan).")
    print(f"Jami app_users jadvalida: {total_users} ta yozuv.")
    print("\nTest parol (barcha soxta mijozlar uchun bir xil): test123456")
    print(f"Tozalash uchun: DELETE FROM app_users/favorites WHERE user_id BETWEEN {SEED_ID_START} AND {SEED_ID_END}")


if __name__ == "__main__":
    main()
