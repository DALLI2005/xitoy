#!/usr/bin/env python3
"""
Bir martalik migratsiya skripti.

Eski bitta `admins.db` faylidagi 5 ta jadvalni (admins, channels, favorites,
app_users, products) 3 ta yangi faylga bo'lib ko'chiradi:

  - products.db  -> products
  - admins.db    -> admins, channels   (YANGI admins.db, eskisi zaxiralanadi)
  - users.db     -> app_users, favorites

Eski fayl o'CHIRILMAYDI — avval butun ma'lumot xotiraga o'qib olinadi,
so'ng eski fayl `admins.db.backup` nomiga ko'chiriladi (chunki yangi
admins.db xuddi shu nom bilan, xuddi shu papkada qayta yaratiladi).

Ishlatilishi:
    cd webapp/backend
    python3 migrate_split_db.py [--source admins.db] [--dest-dir .]

Xavfsizlik: dest papkada products.db yoki users.db allaqachon mavjud
bo'lsa, skript hech narsaga tegmasdan to'xtaydi. admins.db alohida
tekshiriladi — chunki u ham manba, ham (yangilangach) natija fayl nomi.
"""

import argparse
import shutil
import sqlite3
import sys
from pathlib import Path


TABLES_BY_DEST = {
    "products.db": ["products"],
    "admins.db":   ["admins", "channels"],
    "users.db":    ["app_users", "favorites"],
}


def table_exists(conn: sqlite3.Connection, name: str) -> bool:
    row = conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name = ?", (name,)
    ).fetchone()
    return row is not None


def read_table(src_conn: sqlite3.Connection, table: str):
    if not table_exists(src_conn, table):
        print(f"  [!] '{table}' jadvali source faylda topilmadi — o'tkazib yuborildi")
        return [], []
    src_conn.row_factory = sqlite3.Row
    cur = src_conn.execute(f"SELECT * FROM {table}")
    rows = cur.fetchall()
    columns = list(rows[0].keys()) if rows else [d[0] for d in cur.description]
    return columns, [tuple(r) for r in rows]


def write_table(dest_conn: sqlite3.Connection, table: str, columns: list, rows: list) -> None:
    if not rows:
        print(f"  '{table}': 0 ta qator (bo'sh)")
        return
    placeholders = ", ".join("?" for _ in columns)
    col_list = ", ".join(columns)
    dest_conn.executemany(
        f"INSERT OR IGNORE INTO {table} ({col_list}) VALUES ({placeholders})", rows
    )
    dest_conn.commit()
    (count,) = dest_conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()
    print(f"  '{table}': {len(rows)} ta qator ko'chirildi (dest jadvalda hozir jami {count} ta)")


def create_schema(dest_conn: sqlite3.Connection, dest_name: str) -> None:
    """Yangi fayldagi jadval sxemasini yaratadi (main.py dagi CREATE TABLE bilan bir xil)."""
    if dest_name == "products.db":
        dest_conn.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                name                 TEXT NOT NULL,
                price                REAL NOT NULL,
                discount             REAL DEFAULT 0,
                category             TEXT NOT NULL,
                description          TEXT DEFAULT '',
                image_url            TEXT DEFAULT '',
                images               TEXT DEFAULT '[]',
                rating               REAL DEFAULT 4.5,
                sold_count           INTEGER DEFAULT 0,
                discount_type        TEXT DEFAULT 'doimiy',
                discount_expires     TEXT DEFAULT '',
                auto_delete          INTEGER DEFAULT 0,
                active               INTEGER DEFAULT 1,
                in_stock             INTEGER DEFAULT 1,
                variantlar_yoqilgan  INTEGER DEFAULT 0,
                variant_nomlari      TEXT DEFAULT '[]',
                variant_narxlari     TEXT DEFAULT '[]',
                razmer_matritsa      TEXT DEFAULT '{}',
                added_by             TEXT DEFAULT '',
                telegram_message_id  INTEGER DEFAULT 0,
                subcategory          TEXT DEFAULT '',
                product_type         TEXT DEFAULT '',
                country              TEXT DEFAULT '',
                attributes           TEXT DEFAULT '{}',
                guarantee_months     INTEGER DEFAULT 0,
                created_at           TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)
    elif dest_name == "admins.db":
        dest_conn.execute("""
            CREATE TABLE IF NOT EXISTS admins (
                telegram_id  INTEGER PRIMARY KEY,
                name         TEXT    NOT NULL,
                categories   TEXT    NOT NULL DEFAULT '[]',
                active       INTEGER NOT NULL DEFAULT 1,
                password     TEXT    NOT NULL DEFAULT '',
                created_at   TEXT    DEFAULT CURRENT_TIMESTAMP
            )
        """)
        dest_conn.execute("""
            CREATE TABLE IF NOT EXISTS channels (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                channel_id TEXT    NOT NULL UNIQUE,
                label      TEXT    NOT NULL DEFAULT '',
                enabled    INTEGER NOT NULL DEFAULT 1,
                created_at TEXT    DEFAULT CURRENT_TIMESTAMP
            )
        """)
    elif dest_name == "users.db":
        dest_conn.execute("""
            CREATE TABLE IF NOT EXISTS favorites (
                telegram_id  INTEGER NOT NULL,
                product_id   TEXT    NOT NULL,
                created_at   INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                PRIMARY KEY (telegram_id, product_id)
            )
        """)
        dest_conn.execute("CREATE INDEX IF NOT EXISTS idx_favorites_user ON favorites(telegram_id)")
        dest_conn.execute("""
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
        dest_conn.execute("CREATE INDEX IF NOT EXISTS idx_app_users_phone ON app_users(phone)")
        dest_conn.execute("CREATE INDEX IF NOT EXISTS idx_app_users_token ON app_users(session_token)")
    dest_conn.commit()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default="admins.db", help="Eski umumiy DB fayl (default: admins.db)")
    parser.add_argument("--dest-dir", default=".", help="Yangi 3 ta faylning joylashadigan papkasi (default: joriy papka)")
    args = parser.parse_args()

    source_path = Path(args.source).resolve()
    dest_dir = Path(args.dest_dir).resolve()

    if not source_path.exists():
        print(f"XATO: source fayl topilmadi: {source_path}")
        sys.exit(1)

    # products.db / users.db uchun oldindan tekshirish — bular manbadan butunlay boshqa
    # nomli fayllar, shuning uchun mavjud bo'lsa darhol to'xtash xavfsiz.
    for dest_name in ("products.db", "users.db"):
        dest_path = dest_dir / dest_name
        if dest_path.exists() and dest_path != source_path:
            print(f"XATO: '{dest_path}' allaqachon mavjud. Migratsiyani qayta ishga tushirishdan oldin "
                  f"uni o'chiring yoki boshqa --dest-dir bering (mavjud ma'lumot ustidan yozilmasligi uchun).")
            sys.exit(1)

    backup_path = source_path.parent / f"{source_path.name}.backup"
    admins_dest_path = dest_dir / "admins.db"
    # Agar yangi admins.db manba fayldan farqli joyda bo'lsa (masalan boshqa --dest-dir),
    # uni ham oldindan tekshiramiz.
    if admins_dest_path != source_path and admins_dest_path.exists():
        print(f"XATO: '{admins_dest_path}' allaqachon mavjud va manba fayl emas. To'xtatildi.")
        sys.exit(1)
    if backup_path.exists():
        print(f"XATO: zaxira fayl allaqachon mavjud: {backup_path}. Avvalgi migratsiya allaqachon "
              f"bajarilgan bo'lishi mumkin — tekshirib ko'ring.")
        sys.exit(1)

    print(f"Source: {source_path}")
    print(f"Dest papka: {dest_dir}\n")

    # 1) Butun ma'lumotni xotiraga o'qib olamiz (manba faylga hali tegilmagan)
    src_conn = sqlite3.connect(str(source_path))
    data: dict[str, tuple[list, list]] = {}
    print("Manbadan o'qilmoqda...")
    for dest_name, tables in TABLES_BY_DEST.items():
        for table in tables:
            columns, rows = read_table(src_conn, table)
            data[table] = (columns, rows)
            print(f"  '{table}': {len(rows)} ta qator o'qildi")
    src_conn.close()
    print()

    # 2) Manba faylni zaxiraga ko'chiramiz — SHUNDAN KEYINGINA yangi admins.db
    #    xuddi shu nom bilan qayta yaratilishi mumkin (source == dest/admins.db bo'lgani uchun).
    shutil.move(str(source_path), str(backup_path))
    print(f"Eski fayl zaxiralandi: {backup_path}\n")

    # 3) Yangi 3 ta faylni yaratib, xotiradagi ma'lumotni yozamiz
    for dest_name, tables in TABLES_BY_DEST.items():
        dest_path = dest_dir / dest_name
        print(f"-> {dest_name} ({', '.join(tables)})")
        dest_conn = sqlite3.connect(str(dest_path))
        create_schema(dest_conn, dest_name)
        for table in tables:
            columns, rows = data[table]
            write_table(dest_conn, table, columns, rows)
        dest_conn.close()
        print()

    print("Migratsiya tugadi. Yangi fayllarni tekshiring:")
    for dest_name in TABLES_BY_DEST:
        print(f"  - {dest_dir / dest_name}")
    print(f"  - Eski fayl zaxirasi: {backup_path}")


if __name__ == "__main__":
    main()
