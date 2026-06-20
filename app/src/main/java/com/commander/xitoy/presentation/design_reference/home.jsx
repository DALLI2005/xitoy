// home.jsx — three home-screen layout variations. Reads window.UI, window.SCREENS, window.DALLI, window.Icon.

const { FX: FXD, ORDERS: O3, STAGES: S3, PRODUCTS: P3, fmtSom: f3, catById: cb3 } = window.DALLI;
const { ExchangeTicker: Tick, OrderProgress: Pg, ProductCard: Card, ProductRow: Row, SectionHeader: Head, Badge: B3 } = window.UI;
const { CategoryGrid: Grid, CategoryChips: Chips } = window.SCREENS;

const hot = P3.filter(p => p.hot);
const topPicks = P3.slice(0, 6);

function SearchBar({ flat }) {
  return (
    <div style={{ display: "flex", gap: 9 }}>
      <div style={{ flex: 1, display: "flex", alignItems: "center", gap: 9, background: flat ? "var(--bg)" : "var(--card)",
        border: flat ? "none" : "1px solid var(--line)", borderRadius: 13, padding: "0 13px", height: 46 }}>
        <Icon name="search" size={19} color="var(--ink-3)" />
        <span style={{ fontSize: 14, color: "var(--ink-3)", fontWeight: 500 }}>Mahsulot yoki brend qidirish…</span>
      </div>
      <button style={{ width: 46, height: 46, borderRadius: 13, background: "var(--ink)", border: "none", display: "grid",
        placeItems: "center", cursor: "pointer", flexShrink: 0 }}>
        <Icon name="filter" size={20} color="#fff" />
      </button>
    </div>
  );
}

function Greeting({ pro }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 11 }}>
      <div style={{ width: 42, height: 42, borderRadius: 13, background: "var(--primary)", color: "#fff", display: "grid",
        placeItems: "center", fontWeight: 800, fontSize: 17, fontFamily: "var(--mono)" }}>SK</div>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 12.5, color: "var(--ink-3)", fontWeight: 600, display: "flex", alignItems: "center", gap: 4 }}>
          <Icon name="pin" size={13} color="var(--ink-3)" /> Toshkent · Chilonzor
        </div>
        <div style={{ fontSize: 16, fontWeight: 800, color: "var(--ink)" }}>Assalomu alaykum, Sardor</div>
      </div>
      <button style={{ position: "relative", width: 42, height: 42, borderRadius: 13, background: "var(--card)",
        border: "1px solid var(--line)", display: "grid", placeItems: "center", cursor: "pointer" }}>
        <Icon name="bell" size={20} color="var(--ink-2)" />
        <span style={{ position: "absolute", top: 9, right: 10, width: 8, height: 8, borderRadius: "50%", background: "var(--accent)", border: "2px solid var(--card)" }} />
      </button>
    </div>
  );
}

// compact active-order card
function ActiveOrderCard({ o, onNav, dark }) {
  return (
    <div onClick={() => onNav("orders")} style={{ background: dark ? "var(--ink)" : "var(--card)", color: dark ? "#fff" : "var(--ink)",
      borderRadius: 18, border: dark ? "none" : "1px solid var(--line)", padding: 15, cursor: "pointer" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 12 }}>
        <div style={{ width: 38, height: 38, borderRadius: 11, background: dark ? "rgba(255,255,255,0.12)" : "var(--primary-soft)",
          display: "grid", placeItems: "center", flexShrink: 0 }}>
          <Icon name="truck" size={19} color={dark ? "#fff" : "var(--primary)"} stroke={2.2} />
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontFamily: "var(--mono)", fontWeight: 800, fontSize: 13.5 }}>{o.id}</div>
          <div style={{ fontSize: 12, color: dark ? "rgba(255,255,255,0.6)" : "var(--ink-3)", fontWeight: 600 }}>{o.city}</div>
        </div>
        <span style={{ fontSize: 11.5, fontWeight: 800, padding: "5px 9px", borderRadius: 8,
          background: dark ? "rgba(94,226,154,0.18)" : "var(--accent-soft)", color: dark ? "#5ee29a" : "var(--accent-ink)" }}>{S3[o.stage].label}</span>
      </div>
      <Pg stage={o.stage} />
      <div style={{ display: "flex", justifyContent: "space-between", marginTop: 10, fontSize: 12 }}>
        {S3.map((s, i) => i % 1 === 0 && (i===0||i===2||i===4||i===5) ? (
          <span key={s.id} style={{ fontWeight: i===o.stage?800:600, fontSize: 10.5,
            color: i<=o.stage ? (dark?"#fff":"var(--primary)") : (dark?"rgba(255,255,255,0.4)":"var(--ink-3)") }}>{s.label}</span>
        ) : null)}
      </div>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginTop: 12,
        paddingTop: 12, borderTop: `1px solid ${dark ? "rgba(255,255,255,0.12)" : "var(--line)"}` }}>
        <span style={{ fontSize: 12.5, fontWeight: 700, display: "inline-flex", alignItems: "center", gap: 5,
          color: dark ? "#5ee29a" : "var(--primary)" }}><Icon name="clock" size={15} color={dark?"#5ee29a":"var(--primary)"} /> Yetkazish: {o.eta}</span>
        <span style={{ fontSize: 12.5, fontWeight: 700, display: "inline-flex", alignItems: "center", gap: 4,
          color: dark ? "rgba(255,255,255,0.7)" : "var(--ink-2)" }}>Batafsil <Icon name="chevR" size={15} color={dark?"#fff":"var(--ink-2)"} /></span>
      </div>
    </div>
  );
}

function PromoBanner() {
  return (
    <div style={{ position: "relative", borderRadius: 18, overflow: "hidden", padding: "18px 18px",
      background: "linear-gradient(120deg, var(--primary) 0%, oklch(0.5 0.2 280) 100%)", color: "#fff" }}>
      <div style={{ position: "absolute", right: -20, top: -20, width: 130, height: 130, borderRadius: "50%", background: "rgba(255,255,255,0.1)" }} />
      <div style={{ position: "absolute", right: 14, bottom: -28, width: 90, height: 90, borderRadius: "50%", background: "rgba(255,255,255,0.08)" }} />
      <B3 bg="rgba(255,255,255,0.2)" color="#fff" icon="gift">YANGI RESELLER</B3>
      <div style={{ fontSize: 21, fontWeight: 800, marginTop: 9, lineHeight: 1.18, letterSpacing: "-0.01em", maxWidth: "78%" }}>
        Birinchi yuk uchun 0% komissiya</div>
      <div style={{ fontSize: 13, color: "rgba(255,255,255,0.85)", marginTop: 5, maxWidth: "72%" }}>Xitoydan ulgurji import qiling — yetkazib berish bepul.</div>
      <button style={{ marginTop: 13, background: "#fff", color: "var(--primary-ink)", border: "none", borderRadius: 11,
        padding: "10px 16px", fontWeight: 800, fontSize: 13.5, cursor: "pointer", fontFamily: "var(--ui)" }}>Aksiyani ko'rish</button>
    </div>
  );
}

/* ============ HOME A — Classic marketplace ============ */
function HomeA({ onOpen, onAdd, onNav, onCat }) {
  return (
    <div style={{ padding: "12px 16px 24px", display: "flex", flexDirection: "column", gap: 16 }}>
      <Greeting />
      <SearchBar />
      <Tick variant="strip" />
      <PromoBanner />
      <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
        <Head title="Kategoriyalar" action="Barchasi" onAction={() => onNav("catalog")} />
        <Grid cols={3} onPick={onCat} />
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
        <Head title="Faol buyurtma" action="Buyurtmalar" onAction={() => onNav("orders")} />
        <ActiveOrderCard o={O3[0]} onNav={onNav} />
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
        <Head title="Ommabop mahsulotlar" action="Hammasi" onAction={() => onNav("catalog")} />
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 11 }}>
          {topPicks.map(p => <Card key={p.id} p={p} onOpen={onOpen} onAdd={onAdd} />)}
        </div>
      </div>
    </div>
  );
}

/* ============ HOME B — Discovery feed ============ */
function HomeB({ onOpen, onAdd, onNav, onCat, onTrack }) {
  return (
    <div style={{ display: "flex", flexDirection: "column" }}>
      {/* sticky-ish top */}
      <div style={{ padding: "12px 16px 12px", display: "flex", flexDirection: "column", gap: 12, background: "var(--card)", borderBottom: "1px solid var(--line)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ fontSize: 21, fontWeight: 800, letterSpacing: "-0.02em", color: "var(--ink)" }}>Dalli<span style={{ color: "var(--primary)" }}>Shop</span></span>
          <div style={{ marginLeft: "auto" }}><Tick variant="chip" /></div>
        </div>
        <SearchBar flat />
      </div>
      <div style={{ padding: "14px 16px 24px", display: "flex", flexDirection: "column", gap: 16 }}>
        {/* hero */}
        <div style={{ position: "relative", borderRadius: 20, overflow: "hidden", minHeight: 168,
          background: "linear-gradient(135deg, oklch(0.42 0.16 265) 0%, var(--primary) 60%, oklch(0.6 0.18 230) 100%)",
          padding: 20, color: "#fff", display: "flex", flexDirection: "column", justifyContent: "flex-end" }}>
          <div style={{ position: "absolute", inset: 0, opacity: 0.12,
            backgroundImage: "repeating-linear-gradient(60deg, #fff 0 1px, transparent 1px 16px)" }} />
          <B3 bg="rgba(255,255,255,0.2)" color="#fff" icon="flame">HAFTA HITI</B3>
          <div style={{ fontSize: 26, fontWeight: 800, marginTop: 10, lineHeight: 1.12, letterSpacing: "-0.02em" }}>Elektronika<br/>2x foyda bilan</div>
          <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 12 }}>
            <button onClick={() => onCat("elektronika")} style={{ background: "#fff", color: "var(--primary-ink)", border: "none",
              borderRadius: 11, padding: "11px 18px", fontWeight: 800, fontSize: 14, cursor: "pointer", fontFamily: "var(--ui)" }}>Ko'rish</button>
            <span style={{ fontSize: 12.5, color: "rgba(255,255,255,0.85)", fontWeight: 600 }}>120+ mahsulot · MOQ 10</span>
          </div>
        </div>
        <Chips active="all" onPick={onCat} />
        {/* slim order strip */}
        <button onClick={() => (onTrack ? onTrack(O3[0]) : onNav("orders"))} style={{ display: "flex", alignItems: "center", gap: 12, background: "var(--ink)",
          borderRadius: 15, padding: "13px 15px", border: "none", cursor: "pointer", width: "100%", fontFamily: "var(--ui)" }}>
          <Icon name="truck" size={20} color="#5ee29a" />
          <div style={{ flex: 1, textAlign: "left", color: "#fff" }}>
            <div style={{ fontSize: 13.5, fontWeight: 800 }}>{O3[0].id} · {S3[O3[0].stage].full}</div>
            <div style={{ fontSize: 11.5, color: "rgba(255,255,255,0.6)", fontWeight: 600 }}>{O3[0].city} · {O3[0].eta}</div>
          </div>
          <Icon name="chevR" size={18} color="rgba(255,255,255,0.7)" />
        </button>
        {/* flash deals horizontal */}
        <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
          <Head title="⚡ Tezkor chegirmalar" action="Hammasi" onAction={() => onNav("catalog")} />
          <div className="hscroll" style={{ display: "flex", gap: 11, overflowX: "auto", margin: "0 -16px", padding: "0 16px" }}>
            {hot.map(p => <div key={p.id} style={{ width: 158, flexShrink: 0 }}><Card p={p} onOpen={onOpen} onAdd={onAdd} thumbH={130} /></div>)}
          </div>
        </div>
        {/* feed */}
        <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
          <Head title="Siz uchun lenta" />
          {P3.slice(0, 6).map(p => <Row key={p.id} p={p} onOpen={onOpen} onAdd={onAdd} />)}
        </div>
      </div>
    </div>
  );
}

/* ============ HOME C — Reseller dashboard ============ */
function StatCard({ label, value, sub, icon, tone = "ink" }) {
  const tones = { ink: ["var(--card)", "var(--ink)", "var(--line)"], primary: ["var(--primary-soft)", "var(--primary-ink)", "transparent"],
    success: ["var(--success-soft)", "var(--success)", "transparent"] };
  const [bg, fg, bd] = tones[tone];
  return (
    <div style={{ background: bg, border: `1px solid ${bd}`, borderRadius: 15, padding: "13px 13px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 6, color: tone==="ink"?"var(--ink-3)":fg }}>
        <Icon name={icon} size={14} color={tone==="ink"?"var(--ink-3)":fg} /><span style={{ fontSize: 11, fontWeight: 700 }}>{label}</span>
      </div>
      <div style={{ fontFamily: "var(--mono)", fontWeight: 800, fontSize: 17, color: fg, marginTop: 6, letterSpacing: "-0.01em" }}>{value}</div>
      {sub && <div style={{ fontSize: 10.5, color: tone==="ink"?"var(--ink-3)":fg, fontWeight: 600, marginTop: 1, opacity: 0.85 }}>{sub}</div>}
    </div>
  );
}

function HomeC({ onOpen, onAdd, onNav, onCat }) {
  const o = O3[0];
  const actions = [["calc", "Kalkulyator"], ["truck", "Kuzatish"], ["refresh", "Qayta buyurtma"], ["qr", "Skanerlash"]];
  return (
    <div style={{ padding: "12px 16px 24px", display: "flex", flexDirection: "column", gap: 15 }}>
      <Greeting pro />
      {/* stat row */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 9 }}>
        <StatCard label="CNY kurs" value={f3(FXD.cny)} sub={`▲${FXD.cnyDelta}% bugun`} icon="globe" tone="primary" />
        <StatCard label="Oylik foyda" value="12.4M" sub="so'm · +18%" icon="rate" tone="success" />
        <StatCard label="Faol yuk" value="2 ta" sub="yo'lda" icon="truck" tone="ink" />
      </div>
      {/* prominent shipment tracking */}
      <div style={{ background: "var(--ink)", borderRadius: 20, padding: 17, color: "#fff", position: "relative", overflow: "hidden" }}>
        <div style={{ position: "absolute", right: -30, top: -30, width: 130, height: 130, borderRadius: "50%", background: "rgba(94,226,154,0.1)" }} />
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 4 }}>
          <span style={{ fontSize: 12.5, color: "rgba(255,255,255,0.6)", fontWeight: 700 }}>FAOL YETKAZIB BERISH</span>
          <span style={{ marginLeft: "auto", fontFamily: "var(--mono)", fontWeight: 800, fontSize: 13 }}>{o.id}</span>
        </div>
        <div style={{ display: "flex", alignItems: "baseline", gap: 9, marginBottom: 14 }}>
          <span style={{ fontSize: 22, fontWeight: 800, letterSpacing: "-0.01em" }}>{S3[o.stage].full}</span>
        </div>
        {/* route */}
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 12 }}>
          <Icon name="globe" size={16} color="#5ee29a" />
          <span style={{ fontFamily: "var(--mono)", fontSize: 12.5, fontWeight: 700, color: "rgba(255,255,255,0.85)" }}>Guangzhou</span>
          <div style={{ flex: 1, height: 2, background: "repeating-linear-gradient(90deg,#5ee29a 0 6px,transparent 6px 11px)" }} />
          <Icon name="truck" size={16} color="#5ee29a" />
          <div style={{ flex: 1, height: 2, background: "repeating-linear-gradient(90deg,rgba(255,255,255,0.3) 0 6px,transparent 6px 11px)" }} />
          <Icon name="pin" size={16} color="rgba(255,255,255,0.5)" />
          <span style={{ fontFamily: "var(--mono)", fontSize: 12.5, fontWeight: 700, color: "rgba(255,255,255,0.5)" }}>Toshkent</span>
        </div>
        <div style={{ background: "rgba(255,255,255,0.08)", borderRadius: 13, padding: "11px 13px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div><div style={{ fontSize: 10.5, color: "rgba(255,255,255,0.55)", fontWeight: 600 }}>Yetkazish</div>
            <div style={{ fontWeight: 800, fontSize: 14, color: "#5ee29a" }}>{o.eta}</div></div>
          <div style={{ textAlign: "right" }}><div style={{ fontSize: 10.5, color: "rgba(255,255,255,0.55)", fontWeight: 600 }}>{o.qty} dona</div>
            <div style={{ fontFamily: "var(--mono)", fontWeight: 800, fontSize: 14 }}>{f3(o.total)} so'm</div></div>
          <button onClick={() => onNav("orders")} style={{ background: "#fff", color: "var(--ink)", border: "none", borderRadius: 10,
            padding: "9px 13px", fontWeight: 800, fontSize: 12.5, cursor: "pointer", fontFamily: "var(--ui)" }}>Kuzatish</button>
        </div>
      </div>
      {/* quick actions */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr 1fr", gap: 8 }}>
        {actions.map(([ic, lbl]) => (
          <button key={lbl} style={{ background: "var(--card)", border: "1px solid var(--line)", borderRadius: 14, padding: "12px 4px",
            display: "flex", flexDirection: "column", alignItems: "center", gap: 7, cursor: "pointer", fontFamily: "var(--ui)" }}>
            <div style={{ width: 36, height: 36, borderRadius: 11, background: "var(--primary-soft)", display: "grid", placeItems: "center" }}>
              <Icon name={ic} size={18} color="var(--primary)" /></div>
            <span style={{ fontSize: 10.5, fontWeight: 700, color: "var(--ink-2)", textAlign: "center" }}>{lbl}</span>
          </button>
        ))}
      </div>
      <Chips active="all" onPick={onCat} />
      {/* recommendations with margin focus */}
      <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
        <Head title="Yuqori foydali tovarlar" action="Hammasi" onAction={() => onNav("catalog")} />
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 11 }}>
          {[...P3].sort((a,b) => window.DALLI.margin(b)-window.DALLI.margin(a)).slice(0,4).map(p => <Card key={p.id} p={p} onOpen={onOpen} onAdd={onAdd} />)}
        </div>
      </div>
    </div>
  );
}

window.HOMES = { HomeA, HomeB, HomeC };
