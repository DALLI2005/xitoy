"""
Kategoriya daraxti — YAGONA MANBA (single source of truth).

Bu fayl butun tizim uchun kategoriyalarning yagona ta'rifi:
  • backend    — tovar qo'shishda validatsiya va ruxsat tekshiruvi
  • admin sayt — /api/categories/tree orqali oladi
  • Android    — mahsulot javobidagi category/subcategory/product_type maydonlari

Struktura: 3 daraja
    "Asosiy toifa": { "Kichik toifa": ["Tovar turi", ...] }

Ruxsat (adminning categories ro'yxati) HAR DOIM 1-daraja (asosiy toifa)
nomlari bilan ishlaydi. Tovar esa uchala darajani ham saqlaydi.
"""

CATEGORY_TREE: dict[str, dict[str, list[str]]] = {
    "Texnika va elektronika": {
        "Smartfonlar va gadjetlar": ["Smartfonlar", "Smart soatlar", "Quloqchinlar"],
        "Noutbuklar va kompyuterlar": ["Noutbuklar", "Kompyuter qismlari", "Aksessuarlar"],
        "Televizorlar va audio": ["Televizorlar", "Uy kinoteatri", "Akustika"],
    },
    "Yozgi kolleksiya": {
        "Kiyimlar": ["Futbolkalar", "Shortilar", "Ko'ylaklar"],
        "Poyabzallar": ["Shlyopkalar", "Sandallar", "Krossovkalar"],
        "Aksessuarlar": ["Quyosh ko'zoynaklari", "Shlyapalar", "Sumkalar"],
    },
    "Mebel": {
        "Karavot va matraslar": ["Matraslar", "Karavotlar", "Yotoqxona uchun mebel to'plamlari"],
        "Oshxona mebellari": ["Oshxona burchaklari", "Oshxona garniturasi", "Oshxona modullari", "Oshxona xontaxtalari"],
        "Vannaxona uchun mebel": ["Rakovina uchun tumbalar", "Vannaxona uchun mebel to'plamlari", "Vannaxona uchun shkaflar", "Vannaxona uchun penallar", "Kir yuvish mashinasi uchun mebel"],
        "Mebel uchun furnitura va butlovchi qismlar": ["Mebel to'ldirgichlari", "Mebel uchun butlovchi qismlar", "Mebel uchun furnitura"],
        "Saqlash mebellari": ["Komodlar va tumbalar", "Tokcha va javonlar", "Qavatli ilgichlar", "Shkaflar va garideroblar"],
        "Yumshoq mebel": ["Pufiklar va banketkalar", "Divanlar", "Kreslolar", "Yumshoq mebel to'plamlari"],
        "Ofis va kompyuter mebellari": ["Ofis kreslolari", "Geymerlar uchun kreslolari", "Seyflar", "Partalar", "Ofis mebeli to'plamlari", "Ofis tumbalari", "Yozuv va kompyuter stollari"],
        "Stollar va stullar": ["Stullar", "Jurnal stollari", "Stollar", "Ovqatlanish guruhlari", "Pardoz stollari va konsollar", "Taburetkalar"],
    },
    "Turizm, baliq ovi va ovchilik": {
        "Kemping": ["Chodirlar", "Tentlar va chodirlar", "Aksessuarlar chodirlar va tentlar uchun"],
        "Taktik kiyim": ["Taktik kostyumlar", "Taktik aksessuarlar", "Taktik poyabzal", "Taktik qo'lqoplar", "Taktik kurtkalar", "Taktik ryukzaklar", "Taktik shim"],
        "Baliq ovi": ["G'altaklar baliq ovlash uchun", "Baliq ovlash asboblari va aksessuarlari", "Baliq ovi uchun primankalar va o'ljalar", "Qayiqlar va aksessuarlar", "Ozuqalar, nasadkalar, baliq ovlash uchun...", "Leskalar va simlar", "Ilgaklar, bog'ichlar va gruzilalar", "Exolotlar va navigatsiya"],
        "Turizm va ochiq havoda dam olish": ["Fonarlar va aksessuarlar", "Turizm va dam olish uchun aksessuarlar", "Sayohat uchun ryukzaklar va sumkalar", "Katlanadigan sayyohlik stullari", "Sayohat uchun idishlar", "Sayyohlik stollari va mebel to'plamlari", "Metall izlagichlar va aksessuarlar", "Uyqu qoplari", "Turistik gilamchalar", "Nasoslar", "Orientatsiya uchun yordamchi vositalar", "Osma qozonchalar va gulhan uchun uch..."],
        "Ov va otish mashg'ulotlari": ["Cho'zmalar", "Optika va diqqatga sazovor joylar", "Sport otish mashqlari", "Trofeylar uchun bezaklar"],
    },
    "Elektronika": {
        "Smartfonlar va telefonlar": ["Smartfonlar uchun aksessuarlar", "Knopkali telefonlar", "Smartfonlar", "Ehtiyot qismlar va ta'mirlash", "Statsionar telefonlar"],
        "Kompyuter texnikasi": ["Kompyuter aksessuarlari", "Kompyuter texnikalari uchun butlovchi qi...", "Kompyuterning tashqi qurilmalari", "Ma'lumotlarni saqlash", "Kompyuterlar", "Dasturiy ta'minot"],
        "Geymerlar uchun mahsulotlar": ["O'yin pristavkalari", "O'yin sichqonchalari", "O'yin klaviaturalari", "O'yin uchun quloqchinlar", "O'yinlar", "O'yin gilamchalari", "O'yin monitorlari", "O'yin noutbuklari", "O'yin g'ildiraklari va pedallari", "VR-garnituralar"],
        "Aqlli uy va xavfsizlik": ["Videokuzatuv", "Aqlli uy", "Uy xavfsizligi"],
        "Optik anjomlar": ["Teleskoplar", "Okulyarlar", "Mikroskoplar", "Teatr binokllari", "Mikroskoplar uchun aksessuarlar", "Smart ko'zoynak", "Teleskop aksessuarlari"],
        "Noutbuklar, planshetlar va elektron kitoblar": ["Noutbuklar", "Planshetlar va elektron kitoblar", "Noutbuk uchun aksessuarlar"],
        "Quloqchinlar va audio texnikalar": ["Quloqchinlar", "Audiotexnika", "Proigrivatel uchun aksessuarlar", "Periferiya va aksessuarlar"],
        "Ofis texnikasi": ["Ofis jihozlari uchun butlovchi qismlar", "Ofis jihozlari", "Aksessuarlar va ofis jihozlari parvarishi"],
        "Elektronikalar uchun aksessuarlar": ["USB hablar", "Akkumulyatorlar uchun zaryadlovchi quri...", "Tarmoq filtrlari", "Konnektorlar", "Kabel himoyasi", "Uzaytirgichlar", "Akkumulyator batareyalar", "Kabellar uchun tutqichlar", "Tarmoqlagichlar", "Elektr tarmoqiagichlar", "Kabellar uchun organayzerlar", "Dok-stansiyalar", "Quyosh panellari va batareyalari", "Kuchlanishni o'zgartirgichlar", "Simlar uchun qutilar"],
        "Navigatorlar": ["GPS-trekerlar va GPS-mayoqlar", "Sayyohlik navigatorlari", "Avtomobil navigatorlari", "Elektron kompaslar", "Navigatorlar uchun aksessuarlar"],
        "Televizorlar va videotexnikalar": ["Kabellar va adapterlar", "Televizorlar", "Tyuner va resiverlar", "Televizor aksessuarlari", "Televizor uchun stend va kronshteynlar", "TV-pristavkalar va media pleerlar", "Proyektorlar va aksessuarlar", "Raqamli va sun'iy yo'ldosh TV", "Ichki joylashadigan televizorlar"],
        "Aqlli soatlar va fitnes bilaguzuklar": ["Aqlli soatlar", "Qayishlar", "Fitnes bilaguzuklar", "Kabellar va zaryadlovchi qurilmalar", "Aqlli gadjetlar", "Soatlar va fitnes bilaguzuklar uchun him..."],
        "Foto va video texnika": ["Fotosuratchilar uchun uskunalar", "Foto va video kameralar", "Foto va video kameralar uchun aksessua..."],
        "Soatlar va elektron budilniklar": ["Elektron budilniklar", "Proyeksiya soatlari", "Radio budilniklar", "Aromabudilniklar va katrijlar"],
        "Kvadrokopterlar va aksessuarlar": ["Kvadrokopterlar uchun aksessuarlar", "Kvadrokopterlar"],
    },
    "Maishiy texnika": {
        "Katta maishiy texnika": ["Suv uchun kulerlar va aksessuarlar", "Oshxona dudburonlari", "Qaynatish panellari", "Katta maishiy texnika uchun aksessuarlar", "Sovutgichlar va muzlatgichlar", "Kir yuvish mashinalari", "Pechlar", "Pechkalar", "Idish yuvish mashinalari", "Quritgich mashinasi"],
        "Go'zallik uchun texnika": ["Soch turmaklash", "Soch kesish", "Elektr ustaralar", "Apparatli kosmetologiya", "Epilyatorlar va aksessuarlar", "Yerda turadigan tarozilar", "Mini solariya"],
        "Uy uchun texnika": ["Changyutgichlar va aksessuarlar", "Dazmollar va bug'lagichlar", "Tikuv mashinalari va aksessuarlari", "Sterilizatorlar", "Namlab tozalash apparatlari", "Qo'l uchun quritgichlar"],
        "Iqlim texnikasi": ["Ventilyatorlar", "Havoni tozalash va namlantirish", "Isitgichlar", "Konditsionerlar va split tizimlar", "Suv isitgichlari va isitish qozonlari", "Havo sovutgichlari", "Datchiklar", "Ob-havo stansiyalari"],
        "Oshxona texnikalari": ["Maydalash va aralashtirish", "Qovurish va pishirish uskunalari", "Elektr choynaklar va termopotlar", "Boshqa oshxona texnikalari", "Kofe tayyorlash", "Sharbat chiqargichlar", "Mikroto'lqinli pechlar va aksessuarlar"],
        "Maishiy texnika uchun boshqa aksessuarlar va ehtiyot qismlar": ["Aksessuarlar", "Ehtiyot qismlar"],
    },
    "Kiyim": {
        "Ayollar kiyimi": ["Ko'ylaklar", "Bluzkalar", "Shimlar va jinsilar"],
        "Erkaklar kiyimi": ["Futbolkalar", "Ko'ylaklar", "Shimlar"],
        "Bolalar kiyimi": ["Chaqaloqlar uchun", "Qizlar uchun", "O'g'il bolalar uchun"],
    },
    "Poyabzallar": {
        "Ayollar poyabzali": ["Tuflilar", "Krossovkalar", "Etiklar"],
        "Erkaklar poyabzali": ["Krossovkalar", "Klassik poyabzal", "Botinkalar"],
        "Bolalar poyabzali": ["Maktab uchun", "Sport uchun", "Qishki poyabzal"],
    },
    "Aksessuarlar": {
        "Sumkalar va hamyonlar": ["Ayollar sumkalari", "Erkaklar sumkalari", "Hamyonlar"],
        "Zargarlik buyumlari": ["Uzuklar", "Sirg'alar", "Zanjirlar"],
        "Soatlar": ["Erkaklar soatlari", "Ayollar soatlari", "Bolalar soatlari"],
    },
    "Go'zallik va parvarish": {
        "Makiyaj": ["Ko'z uchun", "Lab uchun", "Yuz uchun"],
        "Soch parvarishi": ["Shampunlar", "Konditsionerlar", "Soch uchun niqoblar"],
        "Parfyumeriya": ["Erkaklar atirlari", "Ayollar atirlari", "Uniseks atirlar"],
    },
    "Salomatlik": {
        "Vitaminlar va BADlar": ["Vitamin komplekslari", "Minerallar", "Sport ozuqalari"],
        "Tibbiy asboblar": ["Tonometrlar", "Termometrlar", "Inhalyatorlar"],
        "Ortopediya": ["Bandajlar", "Ortopedik yostiqlar", "Korsetlar"],
    },
    "Uy-ro'zg'or buyumlari": {
        "Idish-tovoq": ["Qozonlar va tavalar", "Pichoqlar", "Choy va qahva uchun"],
        "To'qimachilik": ["Choyshablar", "Sochiqlar", "Ko'rpalar va yostiqlar"],
        "Tozalash vositalari": ["Shvabralar", "Chelaklar", "Chiqindi qutilari"],
    },
    "Qurilish va ta'mirlash": {
        "Elektr asboblari": ["Drellar", "Perforatorlar", "Shurupovyortlar"],
        "Qo'l asboblari": ["Bolg'alar", "Otvyortkalar", "Kalitlar"],
        "Pardozlash materiallari": ["Gulqog'ozlar", "Bo'yoqlar", "Laminat"],
    },
    "Avtotovarlar": {
        "Avtokimyo va kosmetika": ["Shampunlar", "Polirovkalar", "Moylar"],
        "Avto ehtiyot qismlari": ["Filtrlar", "Svechalar", "Tormoz kolodkalari"],
        "Avto aksessuarlar": ["Gilamchalar", "Chexollar", "Tashkilotchilar"],
    },
    "Bolalar tovarlari": {
        "O'yinchoqlar": ["Konstruktorlar", "Qo'g'irchoqlar", "Yumshoq o'yinchoqlar"],
        "Chaqaloqlar parvarishi": ["Tagliklar", "Soskalar", "Bolalar kosmetikasi"],
        "Bolalar xonasi": ["Beshiklar", "Manajlar", "Bolalar mebellari"],
    },
    "Xobbi va ijod": {
        "Rasm chizish": ["Bo'yoqlar", "Mo'yqalamlar", "Molbertlar"],
        "Trikotaj va tikuvchilik": ["Iplar", "Ignalar", "Biserlar"],
        "Stol o'yinlari": ["Kattalar uchun", "Bolalar uchun", "Boshqotirmalar"],
    },
    "Sport va hordiq": {
        "Fitnes va trenajyorlar": ["Yugurish yo'lakchalari", "Gantellar", "Fitbollar"],
        "Velosport": ["Velosipedlar", "Samokatlar", "Roliklar"],
        "Sport kiyimlari va poyabzali": ["Sport kostyumlari", "Krossovkalar", "Futbolkalar"],
    },
    "Oziq-ovqat mahsulotlari": {
        "Choy, qahva, kakao": ["Qora choy", "Ko'k choy", "Qahva donalari"],
        "Shirinliklar va pishiriqlar": ["Shokolad", "Pechenye", "Marmelad"],
        "Baqqallik": ["Makaronlar", "Yormalar", "Ziravorlar"],
    },
    "Maishiy kimyoviy moddalar": {
        "Kir yuvish vositalari": ["Kir yuvish kukunlari", "Gellar", "Konditsionerlar"],
        "Idish yuvish vositalari": ["Gellar", "Tabletkalar", "Gubkalar"],
        "Uy tozalash": ["Pollar uchun", "Oynalar uchun", "Santexnika uchun"],
    },
    "Kanselyariya tovarlari": {
        "Yozuv asboblari": ["Ruchkalar", "Qalamlar", "Markerlar"],
        "Qog'oz mahsulotlari": ["Daftarlar", "Bloknotlar", "A4 qog'oz"],
        "Ofis jihozlari": ["Staplerlar", "Skrepkalar", "Papkalar"],
    },
    "Hayvonlar uchun tovarlar": {
        "Mushuklar uchun": ["Ozuqalar", "Tovoqlar", "O'yinchoqlar"],
        "Itlar uchun": ["Ozuqalar", "Bog'ichlar", "O'yinchoqlar"],
        "Qushlar va kemiruvchilar": ["Qafaslar", "Ozuqalar", "Aksessuarlar"],
    },
    "Kitoblar": {
        "Badiiy adabiyot": ["Romanlar", "Detektivlar", "Fantastika"],
        "Bolalar adabiyoti": ["Ertaklar", "She'rlar", "Ensiklopediyalar"],
        "O'quv adabiyoti": ["Darsliklar", "Qo'llanmalar", "Lug'atlar"],
    },
    "Dacha, bog' va tomorqa": {
        "Bog' asboblari": ["Kuraklar", "Xaskashlar", "Qaychilar"],
        "Sug'orish tizimlari": ["Shlanglar", "Purkagichlar", "Nasoslar"],
        "Bog' mebellari": ["Stollar", "Kreslolar", "Qavatoryerlar"],
    },
    "Reabilitatsiya uchun subsidiyalangan mahsulotlar": {
        "Harakatlanish vositalari": ["Nogironlar aravachalari", "Hassalar", "Xodunoklar"],
        "Parvarish vositalari": ["Tagliklar", "Gigiyena vositalari", "Maxsus to'shaklar"],
        "Ko'zi ojizlar uchun": ["Maxsus soatlar", "Tayoqlar", "Ovozli qurilmalar"],
    },
    "Boshqa": {
        "Turli xil": ["Boshqalar", "To'plamlar"],
    },
}

# 1-daraja — adminlarga ruxsat berish va Android filtrlari shu ro'yxat bilan ishlaydi
CATEGORIES: list[str] = list(CATEGORY_TREE.keys())

FALLBACK_ROOT = "Boshqa"


def _build_index() -> dict[str, tuple[str, str, str]]:
    """Har qanday nom -> (root, sub, leaf). Eski (faqat bitta nom saqlangan)
    tovarlarni normalizatsiya qilish uchun. Nom bir necha joyda uchrasa —
    birinchi topilgani olinadi."""
    idx: dict[str, tuple[str, str, str]] = {}
    for root, subs in CATEGORY_TREE.items():
        idx.setdefault(root.casefold(), (root, "", ""))
        for sub, leaves in subs.items():
            idx.setdefault(sub.casefold(), (root, sub, ""))
            for leaf in leaves:
                idx.setdefault(leaf.casefold(), (root, sub, leaf))
    return idx


_NAME_INDEX = _build_index()


def is_valid_path(root: str, sub: str = "", leaf: str = "") -> bool:
    """Berilgan yo'l daraxtda mavjudmi?"""
    if root not in CATEGORY_TREE:
        return False
    if not sub:
        # sub bo'lmasa, leaf ham bo'lmasligi kerak — aks holda leaf
        # tekshirilmay, tasdiqlanmagan qiymat sifatida o'tib ketadi.
        return not leaf
    if sub not in CATEGORY_TREE[root]:
        return False
    if not leaf:
        return True
    return leaf in CATEGORY_TREE[root][sub]


def resolve(root: str, sub: str = "", leaf: str = "") -> tuple[str, str, str]:
    """Kelgan kategoriya ma'lumotini to'liq (root, sub, leaf) yo'liga keltiradi.

    • To'liq va to'g'ri yo'l kelsa — o'zi qaytadi.
    • Faqat bitta nom kelsa (eski mijozlar 3-darajali nomni `category` da
      yuborardi) — indeks orqali asosiy toifasi topiladi.
    • Umuman topilmasa — ("Boshqa", "Turli xil", "Boshqalar").
    """
    root = (root or "").strip()
    sub = (sub or "").strip()
    leaf = (leaf or "").strip()

    if is_valid_path(root, sub, leaf):
        return root, sub, leaf

    # Eski format yoki noto'g'ri yo'l — nomlar bo'yicha qidiramiz
    for candidate in (leaf, sub, root):
        if not candidate:
            continue
        found = _NAME_INDEX.get(candidate.casefold())
        if found:
            return found

    return FALLBACK_ROOT, "Turli xil", "Boshqalar"


def root_of(name: str) -> str:
    """Istalgan darajadagi nomdan asosiy toifani qaytaradi."""
    return resolve(name)[0]


def roots_containing(name: str) -> list[str]:
    """`name` (root/sub/leaf darajasidan qat'i nazar) qaysi asosiy toifa(lar)da
    uchrashini qaytaradi. Bir nechta toifada bir xil nom bo'lishi mumkin
    (masalan "Futbolkalar" ham "Kiyim"da, ham "Yozgi kolleksiya"da bor) —
    shu holatlarni chaqiruvchi tomon noaniq deb bilishi uchun ishlatiladi."""
    key = (name or "").strip().casefold()
    if not key:
        return []
    roots: list[str] = []
    for root, subs in CATEGORY_TREE.items():
        if root.casefold() == key:
            roots.append(root)
            continue
        for sub, leaves in subs.items():
            if sub.casefold() == key or any(leaf.casefold() == key for leaf in leaves):
                roots.append(root)
                break
    return roots


def path_string(root: str, sub: str = "", leaf: str = "") -> str:
    return " > ".join([p for p in (root, sub, leaf) if p])
