// screens.jsx — shared screens + modals. Reads window.UI, window.Icon, window.DALLI.

const { fmtSom: fmt, CATEGORIES: CATS, PRODUCTS: ALL, ORDERS: ORD, STAGES: STG,
        catById: catOf, productsByCat: byCat, margin: mg, profit: pf } = window.DALLI;
const { Money: M, Stars: St, Badge: Bd, Thumb: Th, ExchangeTicker: FX2, OrderProgress: Prog,
        ProductCard: PCard, ProductRow: PRow, SectionHeader: SH, BottomNav: Nav } = window.UI;

/* ---------- Category grid + chips (shared by homes & catalog) ---------- */

function CategoryGrid({ cols = 3, onPick }) {
  return (
    <div style={{ display: "grid", gridTemplateColumns: `repeat(${cols}, 1fr)`, gap: 9 }}>
      {CATS.map(c => (
        <button key={c.id} onClick={() => onPick?.(c.id)} style={{ background: "var(--card)", border: "1px solid var(--line)",
          borderRadius: 15, padding: "13px 8px", cursor: "pointer", display: "flex", flexDirection: "column",
          alignItems: "center", gap: 8, fontFamily: "var(--ui)" }}>
          <div style={{ width: 46, height: 46, borderRadius: 13, background: c.tint, display: "grid", placeItems: "center" }}>
            <Icon name={c.glyph} size={24} color={c.ink} stroke={1.8} />
          </div>
          <span style={{ fontSize: 12, fontWeight: 700, color: "var(--ink)" }}>{c.label}</span>
        </button>
      ))}
    </div>
  );
}

function CategoryChips({ active, onPick, includeAll = true }) {
  const list = includeAll ? [{ id: "all", label: "Hammasi" }, ...CATS] : CATS;
  return (
    <div className="hscroll" style={{ display: "flex", gap: 8, overflowX: "auto", padding: "2px 0" }}>
      {list.map(c => {
        const on = active === c.id;
        return (
          <button key={c.id} onClick={() => onPick?.(c.id)} style={{ flexShrink: 0, padding: "9px 15px", borderRadius: 999,
            border: on ? "none" : "1px solid var(--line)", background: on ? "var(--ink)" : "var(--card)",
            color: on ? "#fff" : "var(--ink-2)", fontWeight: 700, fontSize: 13, cursor: "pointer", fontFamily: "var(--ui)",
            display: "inline-flex", alignItems: "center", gap: 6 }}>
            {c.glyph && <Icon name={c.glyph} size={15} color={on ? "#fff" : catOf(c.id).ink} stroke={2} />}
            {c.label}
          </button>
        );
      })}
    </div>
  );
}

/* ---------- Catalog screen ---------- */

function CatalogScreen({ initialCat = "all", onOpen, onAdd }) {
  const [cat, setCat] = React.useState(initialCat);
  const [sort, setSort] = React.useState("hot");
  React.useEffect(() => setCat(initialCat), [initialCat]);
  let items = cat === "all" ? ALL : byCat(cat);
  items = [...items].sort((a, b) =>
    sort === "hot" ? (b.hot === a.hot ? b.sold - a.sold : (b.hot ? 1 : 0) - (a.hot ? 1 : 0))
    : sort === "margin" ? mg(b) - mg(a)
    : sort === "cheap" ? a.cost - b.cost : 0);
  return (
    <div style={{ padding: "12px 16px 24px", display: "flex", flexDirection: "column", gap: 14 }}>
      <h2 style={{ margin: 0, fontSize: 24, fontWeight: 800, letterSpacing: "-0.02em" }}>Katalog</h2>
      <CategoryChips active={cat} onPick={setCat} />
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <span style={{ fontSize: 13, color: "var(--ink-3)", fontWeight: 600 }}>{items.length} ta mahsulot</span>
        <div style={{ display: "flex", gap: 6 }}>
          {[["hot","Ommabop"],["margin","Foyda %"],["cheap","Arzon"]].map(([k, lbl]) => (
            <button key={k} onClick={() => setSort(k)} style={{ padding: "6px 11px", borderRadius: 9, fontSize: 12, fontWeight: 700,
              cursor: "pointer", fontFamily: "var(--ui)", border: sort===k ? "none" : "1px solid var(--line)",
              background: sort===k ? "var(--primary-soft)" : "var(--card)", color: sort===k ? "var(--primary-ink)" : "var(--ink-3)" }}>{lbl}</button>
          ))}
        </div>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 11 }}>
        {items.map(p => <PCard key={p.id} p={p} onOpen={onOpen} onAdd={onAdd} />)}
      </div>
    </div>
  );
}

/* ---------- Cart screen (with profit projection) ---------- */

function CartScreen({ cart, setQty, remove, onOpen, onNav }) {
  const lines = Object.values(cart);
  const cost = lines.reduce((s, l) => s + l.p.cost * l.qty, 0);
  const rev = lines.reduce((s, l) => s + l.p.sell * l.qty, 0);
  const units = lines.reduce((s, l) => s + l.qty, 0);
  if (!lines.length) {
    return (
      <div style={{ padding: "60px 28px", textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center", gap: 14 }}>
        <div style={{ width: 88, height: 88, borderRadius: 26, background: "var(--primary-soft)", display: "grid", placeItems: "center" }}>
          <Icon name="cart" size={40} color="var(--primary)" stroke={1.6} />
        </div>
        <h2 style={{ margin: 0, fontSize: 21, fontWeight: 800 }}>Savatcha bo'sh</h2>
        <p style={{ margin: 0, fontSize: 14, color: "var(--ink-3)", lineHeight: 1.5 }}>Katalogdan ulgurji mahsulot tanlang va foydangizni hisoblang.</p>
        <button onClick={() => onNav("catalog")} style={{ marginTop: 6, padding: "13px 22px", borderRadius: 13, border: "none",
          background: "var(--primary)", color: "#fff", fontWeight: 800, fontSize: 15, cursor: "pointer", fontFamily: "var(--ui)" }}>Katalogga o'tish</button>
      </div>
    );
  }
  return (
    <div style={{ padding: "12px 16px 20px", display: "flex", flexDirection: "column", gap: 12 }}>
      <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between" }}>
        <h2 style={{ margin: 0, fontSize: 24, fontWeight: 800, letterSpacing: "-0.02em" }}>Savatcha</h2>
        <span style={{ fontSize: 13, color: "var(--ink-3)", fontWeight: 600 }}>{units} dona · {lines.length} pozitsiya</span>
      </div>
      {lines.map(l => (
        <div key={l.p.id} style={{ background: "var(--card)", borderRadius: 16, border: "1px solid var(--line)", padding: 10, display: "flex", gap: 11 }}>
          <div onClick={() => onOpen(l.p)} style={{ width: 72, flexShrink: 0, cursor: "pointer" }}><Th cat={l.p.cat} h={72} radius={11} /></div>
          <div style={{ flex: 1, minWidth: 0, display: "flex", flexDirection: "column", gap: 5 }}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
              <span style={{ fontSize: 13.5, fontWeight: 700, lineHeight: 1.25 }}>{l.p.name}</span>
              <button onClick={() => remove(l.p.id)} style={{ background: "none", border: "none", cursor: "pointer", padding: 0, height: 18, color: "var(--ink-3)" }}>
                <Icon name="close" size={16} /></button>
            </div>
            <Bd bg="var(--success-soft)" color="var(--success)" icon="rate">+{fmt(pf(l.p)*l.qty)} so'm foyda</Bd>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginTop: "auto" }}>
              <M value={l.p.cost * l.qty} size={16} />
              <div style={{ display: "flex", alignItems: "center", gap: 0, border: "1px solid var(--line)", borderRadius: 10, overflow: "hidden" }}>
                <button onClick={() => setQty(l.p.id, l.qty - 1)} style={qtyBtn}><Icon name="minus" size={15} color="var(--ink-2)" stroke={2.6} /></button>
                <span style={{ minWidth: 34, textAlign: "center", fontFamily: "var(--mono)", fontWeight: 700, fontSize: 13 }}>{l.qty}</span>
                <button onClick={() => setQty(l.p.id, l.qty + 1)} style={qtyBtn}><Icon name="plus" size={15} color="var(--ink-2)" stroke={2.6} /></button>
              </div>
            </div>
          </div>
        </div>
      ))}
      {/* profit summary */}
      <div style={{ background: "var(--ink)", borderRadius: 18, padding: 16, color: "#fff", marginTop: 4 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 7, marginBottom: 12 }}>
          <Icon name="calc" size={17} color="#5ee29a" /><span style={{ fontWeight: 800, fontSize: 14 }}>Foyda kalkulyatori</span>
        </div>
        {[["Ulgurji tan narx", cost, "rgba(255,255,255,0.7)"], ["Tavsiya etilgan savdo", rev, "rgba(255,255,255,0.7)"]].map(([k,v,c]) => (
          <div key={k} style={{ display: "flex", justifyContent: "space-between", padding: "5px 0", fontSize: 13 }}>
            <span style={{ color: c }}>{k}</span><span style={{ fontFamily: "var(--mono)", fontWeight: 700 }}>{fmt(v)} so'm</span>
          </div>
        ))}
        <div style={{ height: 1, background: "rgba(255,255,255,0.14)", margin: "8px 0" }} />
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <span style={{ fontWeight: 700, fontSize: 14, color: "#5ee29a" }}>Sof foyda (taxminiy)</span>
          <span style={{ fontFamily: "var(--mono)", fontWeight: 800, fontSize: 19, color: "#5ee29a" }}>+{fmt(rev-cost)} so'm</span>
        </div>
      </div>
      {/* sticky checkout */}
      <div style={{ position: "sticky", bottom: 0, margin: "8px -16px -20px", background: "var(--card)", borderTop: "1px solid var(--line)",
        padding: "13px 16px 16px", display: "flex", alignItems: "center", gap: 12 }}>
        <div>
          <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 600 }}>To'lov · ulgurji</div>
          <M value={cost} size={20} />
        </div>
        <button style={{ marginLeft: "auto", flex: 1, maxWidth: 200, height: 50, borderRadius: 14, border: "none",
          background: "var(--primary)", color: "#fff", fontWeight: 800, fontSize: 15, cursor: "pointer", fontFamily: "var(--ui)",
          display: "inline-flex", alignItems: "center", justifyContent: "center", gap: 7, boxShadow: "0 8px 20px -6px var(--primary)" }}>
          Buyurtma berish <Icon name="arrowR" size={18} color="#fff" /></button>
      </div>
    </div>
  );
}
const qtyBtn = { width: 32, height: 32, background: "var(--bg)", border: "none", display: "grid", placeItems: "center", cursor: "pointer" };

/* ---------- Orders screen ---------- */

function OrdersScreen({ onTrack }) {
  const [open, setOpen] = React.useState(ORD[0].id);
  return (
    <div style={{ padding: "12px 16px 24px", display: "flex", flexDirection: "column", gap: 12 }}>
      <h2 style={{ margin: 0, fontSize: 24, fontWeight: 800, letterSpacing: "-0.02em" }}>Buyurtmalarim</h2>
      {ORD.map(o => {
        const isOpen = open === o.id, done = o.stage === STG.length - 1;
        return (
          <div key={o.id} style={{ background: "var(--card)", borderRadius: 18, border: "1px solid var(--line)", overflow: "hidden" }}>
            <button onClick={() => (onTrack ? onTrack(o) : setOpen(isOpen ? null : o.id))} style={{ width: "100%", textAlign: "left", background: "none",
              border: "none", cursor: "pointer", padding: 15, fontFamily: "var(--ui)" }}>
              <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                <div style={{ width: 40, height: 40, borderRadius: 12, background: done ? "var(--success-soft)" : "var(--primary-soft)",
                  display: "grid", placeItems: "center", flexShrink: 0 }}>
                  <Icon name={done ? "check" : "truck"} size={20} color={done ? "var(--success)" : "var(--primary)"} stroke={2.2} />
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontFamily: "var(--mono)", fontWeight: 800, fontSize: 13.5, color: "var(--ink)" }}>{o.id}</div>
                  <div style={{ fontSize: 12, color: "var(--ink-3)", fontWeight: 600 }}>{o.items} pozitsiya · {o.qty} dona · {o.date}</div>
                </div>
                <Bd bg={done ? "var(--success-soft)" : "var(--accent-soft)"} color={done ? "var(--success)" : "var(--accent-ink)"}>
                  {STG[o.stage].label}</Bd>
              </div>
              <div style={{ marginTop: 12 }}><Prog stage={o.stage} /></div>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginTop: 11 }}>
                <span style={{ fontSize: 12.5, color: "var(--ink-3)", fontWeight: 600, display: "inline-flex", alignItems: "center", gap: 5 }}>
                  <Icon name="pin" size={14} color="var(--ink-3)" /> {o.city}</span>
                <span style={{ fontSize: 12.5, fontWeight: 700, color: done ? "var(--success)" : "var(--primary)",
                  display: "inline-flex", alignItems: "center", gap: 5 }}>
                  <Icon name="clock" size={14} color={done ? "var(--success)" : "var(--primary)"} /> {o.eta}{onTrack && <Icon name="chevR" size={15} color={done ? "var(--success)" : "var(--primary)"} />}</span>
              </div>
            </button>
            {!onTrack && isOpen && (
              <div style={{ borderTop: "1px solid var(--line)", padding: "16px 15px 16px", background: "var(--bg)" }}>
                <Prog stage={o.stage} variant="full" />
                <div style={{ display: "flex", justifyContent: "space-between", marginTop: 6, padding: "10px 0 0", borderTop: "1px dashed var(--line-2)" }}>
                  <span style={{ fontSize: 13, color: "var(--ink-2)", fontWeight: 600 }}>Buyurtma summasi</span>
                  <M value={o.total} size={16} />
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

/* ---------- Account screen ---------- */

function AccountScreen() {
  const totalImported = ORD.reduce((s, o) => s + o.total, 0);
  const stats = [["Buyurtmalar", ORD.length], ["Import qilingan", (totalImported/1e6).toFixed(1)+"M"], ["O'rtacha foyda", "48%"]];
  const menu = [
    ["pin", "Yetkazib berish manzillari"], ["calc", "Foyda kalkulyatori"], ["bell", "Kurs o'zgarishi xabarnomasi"],
    ["globe", "Til · O'zbekcha"], ["box", "Bojxona hujjatlari"], ["user", "Yordam markazi"],
  ];
  return (
    <div style={{ padding: "12px 16px 24px", display: "flex", flexDirection: "column", gap: 14 }}>
      <h2 style={{ margin: 0, fontSize: 24, fontWeight: 800, letterSpacing: "-0.02em" }}>Profil</h2>
      <div style={{ background: "var(--card)", borderRadius: 18, border: "1px solid var(--line)", padding: 16, display: "flex", alignItems: "center", gap: 13 }}>
        <div style={{ width: 56, height: 56, borderRadius: 16, background: "var(--primary)", color: "#fff", display: "grid",
          placeItems: "center", fontWeight: 800, fontSize: 22, fontFamily: "var(--mono)" }}>SK</div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 17, fontWeight: 800 }}>Sardor Karimov</div>
          <div style={{ fontSize: 12.5, color: "var(--ink-3)", fontWeight: 600 }}>Reseller · Chilonzor bozori</div>
        </div>
        <Bd bg="var(--accent-soft)" color="var(--accent-ink)" icon="spark">PRO</Bd>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 9 }}>
        {stats.map(([k, v]) => (
          <div key={k} style={{ background: "var(--card)", border: "1px solid var(--line)", borderRadius: 14, padding: "13px 10px", textAlign: "center" }}>
            <div style={{ fontFamily: "var(--mono)", fontWeight: 800, fontSize: 18, color: "var(--ink)" }}>{v}</div>
            <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 600, marginTop: 2 }}>{k}</div>
          </div>
        ))}
      </div>
      <div style={{ background: "var(--card)", border: "1px solid var(--line)", borderRadius: 16, overflow: "hidden" }}>
        {menu.map(([ic, lbl], i) => (
          <div key={lbl} style={{ display: "flex", alignItems: "center", gap: 13, padding: "14px 15px",
            borderTop: i ? "1px solid var(--line)" : "none", cursor: "pointer" }}>
            <Icon name={ic} size={19} color="var(--ink-2)" />
            <span style={{ flex: 1, fontSize: 14.5, fontWeight: 600, color: "var(--ink)" }}>{lbl}</span>
            <Icon name="chevR" size={17} color="var(--ink-3)" />
          </div>
        ))}
      </div>
    </div>
  );
}

/* ---------- Product bottom sheet (detail + markup calc) ---------- */

function ProductSheet({ p, onClose, onAdd }) {
  const [qty, setQty] = React.useState(p ? p.moq : 1);
  const [sell, setSell] = React.useState(p ? p.sell : 0);
  React.useEffect(() => { if (p) { setQty(p.moq); setSell(p.sell); } }, [p]);
  if (!p) return null;
  const unitProfit = sell - p.cost, totalCost = p.cost * qty, totalProfit = unitProfit * qty;
  const m = sell > 0 ? Math.round((unitProfit / sell) * 100) : 0;
  return (
    <div style={{ position: "absolute", inset: 0, zIndex: 40, display: "flex", flexDirection: "column", justifyContent: "flex-end" }}>
      <div onClick={onClose} style={{ position: "absolute", inset: 0, background: "rgba(15,23,41,0.45)", animation: "fade .2s" }} />
      <div style={{ position: "relative", background: "var(--card)", borderRadius: "24px 24px 0 0", padding: "10px 18px 20px",
        maxHeight: "92%", overflowY: "auto", animation: "sheet .26s cubic-bezier(.2,.8,.2,1)" }}>
        <div style={{ width: 38, height: 4, borderRadius: 2, background: "var(--line-2)", margin: "0 auto 14px" }} />
        <Th cat={p.cat} h={150} radius={16} />
        <div style={{ display: "flex", gap: 7, marginTop: 12 }}>
          {p.hot && <Bd icon="flame" bg="var(--accent)" color="#fff">HIT mahsulot</Bd>}
          <Bd bg="var(--primary-soft)" color="var(--primary-ink)">MOQ {p.moq} dona</Bd>
          <Bd bg="var(--success-soft)" color="var(--success)" icon="rate">{mg(p)}% foyda</Bd>
        </div>
        <h2 style={{ margin: "10px 0 4px", fontSize: 20, fontWeight: 800, letterSpacing: "-0.01em", lineHeight: 1.2 }}>{p.name}</h2>
        <St rating={p.rating} sold={p.sold} />

        {/* markup calculator */}
        <div style={{ background: "var(--bg)", border: "1px solid var(--line)", borderRadius: 16, padding: 15, marginTop: 16 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 7, marginBottom: 12 }}>
            <Icon name="calc" size={17} color="var(--primary)" /><span style={{ fontWeight: 800, fontSize: 14 }}>Foyda kalkulyatori</span>
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 11 }}>
            <span style={{ fontSize: 13, color: "var(--ink-2)", fontWeight: 600 }}>Ulgurji tan narx</span><M value={p.cost} size={15} />
          </div>
          <label style={{ fontSize: 13, color: "var(--ink-2)", fontWeight: 600 }}>Sotuv narxingiz (dona)</label>
          <div style={{ display: "flex", alignItems: "center", gap: 10, background: "var(--card)", border: "1px solid var(--line)",
            borderRadius: 11, padding: "0 12px", marginTop: 6 }}>
            <input type="number" value={sell} onChange={e => setSell(Math.max(0, +e.target.value || 0))}
              style={{ flex: 1, border: "none", outline: "none", background: "none", fontFamily: "var(--mono)", fontWeight: 800,
                fontSize: 18, color: "var(--ink)", padding: "12px 0", width: "100%" }} />
            <span style={{ fontSize: 13, color: "var(--ink-3)", fontWeight: 600 }}>so'm</span>
          </div>
          <input type="range" min={p.cost} max={Math.round(p.cost*3)} step={1000} value={Math.min(sell, p.cost*3)}
            onChange={e => setSell(+e.target.value)} style={{ width: "100%", marginTop: 12, accentColor: "var(--primary)" }} />
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 9, marginTop: 12 }}>
            <div style={{ background: "var(--card)", borderRadius: 12, padding: "10px 12px", border: "1px solid var(--line)" }}>
              <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 600 }}>Dona foyda</div>
              <div style={{ fontFamily: "var(--mono)", fontWeight: 800, fontSize: 16, color: "var(--success)" }}>+{fmt(unitProfit)}</div>
            </div>
            <div style={{ background: "var(--card)", borderRadius: 12, padding: "10px 12px", border: "1px solid var(--line)" }}>
              <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 600 }}>Margin</div>
              <div style={{ fontFamily: "var(--mono)", fontWeight: 800, fontSize: 16, color: "var(--primary)" }}>{m}%</div>
            </div>
          </div>
        </div>

        {/* qty + total */}
        <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 16 }}>
          <span style={{ fontSize: 13.5, fontWeight: 700, color: "var(--ink-2)" }}>Miqdor</span>
          <div style={{ display: "flex", alignItems: "center", border: "1px solid var(--line)", borderRadius: 11, overflow: "hidden" }}>
            <button onClick={() => setQty(Math.max(p.moq, qty - p.moq))} style={qtyBtn}><Icon name="minus" size={16} color="var(--ink-2)" stroke={2.6} /></button>
            <span style={{ minWidth: 44, textAlign: "center", fontFamily: "var(--mono)", fontWeight: 700, fontSize: 15 }}>{qty}</span>
            <button onClick={() => setQty(qty + p.moq)} style={qtyBtn}><Icon name="plus" size={16} color="var(--ink-2)" stroke={2.6} /></button>
          </div>
          <div style={{ marginLeft: "auto", textAlign: "right" }}>
            <div style={{ fontSize: 11, color: "var(--ink-3)", fontWeight: 600 }}>Jami foyda</div>
            <span style={{ fontFamily: "var(--mono)", fontWeight: 800, fontSize: 16, color: "var(--success)" }}>+{fmt(totalProfit)} so'm</span>
          </div>
        </div>
        <button onClick={() => { onAdd(p, qty); onClose(); }} style={{ width: "100%", height: 54, borderRadius: 15, border: "none",
          background: "var(--primary)", color: "#fff", fontWeight: 800, fontSize: 16, cursor: "pointer", fontFamily: "var(--ui)", marginTop: 16,
          display: "inline-flex", alignItems: "center", justifyContent: "center", gap: 9, boxShadow: "0 10px 24px -8px var(--primary)" }}>
          <Icon name="cart" size={19} color="#fff" /> Savatga · {fmt(totalCost)} so'm
        </button>
      </div>
    </div>
  );
}

window.SCREENS = { CategoryGrid, CategoryChips, CatalogScreen, CartScreen, OrdersScreen, AccountScreen, ProductSheet };
