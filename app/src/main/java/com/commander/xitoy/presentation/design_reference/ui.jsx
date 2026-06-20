// ui.jsx — shared primitives for Dalli Shop. Reads window.DALLI + window.Icon.
// Exports a bag of components to window at the end.

const { fmtSom, FX, catById, margin: marginOf, profit: profitOf, STAGES } = window.DALLI;

/* ---------- atoms ---------- */

function Money({ value, size = 16, weight = 800, color = "var(--ink)", suffix = true }) {
  return (
    <span style={{ fontFamily: "var(--mono)", fontWeight: weight, fontSize: size, color, letterSpacing: "-0.01em", whiteSpace: "nowrap" }}>
      {fmtSom(value)}{suffix && <span style={{ fontSize: size * 0.62, fontWeight: 600, color: "var(--ink-3)", marginLeft: 2 }}>so'm</span>}
    </span>
  );
}

function Stars({ rating, sold }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 4, fontSize: 12, color: "var(--ink-3)", fontWeight: 600 }}>
      <Icon name="star" size={13} fill="var(--warn)" color="var(--warn)" stroke={0} />
      <span style={{ color: "var(--ink)" }}>{rating.toFixed(1)}</span>
      {sold != null && <span>· {sold > 999 ? (sold/1000).toFixed(1).replace(".0","")+"k" : sold} sotildi</span>}
    </span>
  );
}

function Badge({ children, bg = "var(--accent-soft)", color = "var(--accent-ink)", icon }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 3, background: bg, color,
      fontSize: 11, fontWeight: 800, padding: "3px 7px", borderRadius: 7, letterSpacing: "0.01em", lineHeight: 1 }}>
      {icon && <Icon name={icon} size={12} stroke={2.4} />}{children}
    </span>
  );
}

// striped/tinted product placeholder (no real image asset)
function Thumb({ cat, h = 124, radius = 14, label = "rasm" }) {
  const c = catById(cat);
  return (
    <div style={{ position: "relative", height: h, borderRadius: radius, overflow: "hidden",
      background: c.tint, display: "flex", alignItems: "center", justifyContent: "center" }}>
      <div style={{ position: "absolute", inset: 0, opacity: 0.5,
        backgroundImage: `repeating-linear-gradient(45deg, ${c.ink}14 0 1px, transparent 1px 9px)` }} />
      <Icon name={c.glyph} size={Math.min(h*0.34, 46)} color={c.ink} stroke={1.6} style={{ opacity: 0.55 }} />
      <span style={{ position: "absolute", left: 8, bottom: 7, fontFamily: "var(--mono)", fontSize: 9.5,
        color: c.ink, opacity: 0.6, letterSpacing: "0.04em", textTransform: "uppercase" }}>{label}</span>
    </div>
  );
}

/* ---------- exchange ticker ---------- */

function ExchangeTicker({ variant = "strip", onCalc }) {
  const up = FX.cnyDelta >= 0;
  if (variant === "chip") {
    return (
      <div style={{ display: "inline-flex", alignItems: "center", gap: 7, background: "var(--primary-soft)",
        padding: "7px 11px", borderRadius: 11 }}>
        <Icon name="globe" size={15} color="var(--primary)" />
        <span style={{ fontFamily: "var(--mono)", fontSize: 12.5, fontWeight: 700, color: "var(--primary-ink)" }}>
          ¥1 = {fmtSom(FX.cny)}
        </span>
        <span style={{ fontSize: 11, fontWeight: 800, color: up ? "var(--success)" : "var(--danger)" }}>
          {up ? "▲" : "▼"}{Math.abs(FX.cnyDelta)}%
        </span>
      </div>
    );
  }
  // strip
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 12, background: "var(--ink)", borderRadius: 16,
      padding: "12px 14px", color: "#fff" }}>
      <div style={{ width: 34, height: 34, borderRadius: 10, background: "rgba(255,255,255,0.12)",
        display: "grid", placeItems: "center", flexShrink: 0 }}>
        <Icon name="rate" size={18} color="#fff" />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 11, color: "rgba(255,255,255,0.6)", fontWeight: 600, letterSpacing: "0.02em" }}>Bugungi kurs · {FX.updated}</div>
        <div style={{ display: "flex", alignItems: "baseline", gap: 8 }}>
          <span style={{ fontFamily: "var(--mono)", fontSize: 16, fontWeight: 800 }}>¥1 = {fmtSom(FX.cny)} so'm</span>
          <span style={{ fontSize: 12, fontWeight: 800, color: up ? "#5ee29a" : "#ff8a8a" }}>{up ? "▲" : "▼"}{Math.abs(FX.cnyDelta)}%</span>
        </div>
      </div>
      <div style={{ textAlign: "right", paddingLeft: 10, borderLeft: "1px solid rgba(255,255,255,0.14)" }}>
        <div style={{ fontSize: 11, color: "rgba(255,255,255,0.6)", fontWeight: 600 }}>$1</div>
        <div style={{ fontFamily: "var(--mono)", fontSize: 13, fontWeight: 700 }}>{fmtSom(FX.usd)}</div>
      </div>
    </div>
  );
}

/* ---------- order pipeline ---------- */

function OrderProgress({ stage, variant = "bar" }) {
  if (variant === "full") {
    return (
      <div style={{ display: "flex", flexDirection: "column", gap: 0 }}>
        {STAGES.map((s, i) => {
          const done = i < stage, cur = i === stage;
          const color = done ? "var(--success)" : cur ? "var(--primary)" : "var(--line-2)";
          return (
            <div key={s.id} style={{ display: "flex", gap: 12, minHeight: i === STAGES.length-1 ? 0 : 38 }}>
              <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                <div style={{ width: 20, height: 20, borderRadius: "50%", flexShrink: 0,
                  background: done ? "var(--success)" : cur ? "var(--primary)" : "#fff",
                  border: `2px solid ${done || cur ? "transparent" : "var(--line-2)"}`,
                  display: "grid", placeItems: "center" }}>
                  {done && <Icon name="check" size={11} color="#fff" stroke={3} />}
                  {cur && <div style={{ width: 7, height: 7, borderRadius: "50%", background: "#fff" }} />}
                </div>
                {i < STAGES.length-1 && <div style={{ width: 2, flex: 1, background: i < stage ? "var(--success)" : "var(--line-2)" }} />}
              </div>
              <div style={{ paddingBottom: 14, paddingTop: 0 }}>
                <div style={{ fontSize: 13.5, fontWeight: cur ? 800 : 600, color: cur ? "var(--ink)" : done ? "var(--ink-2)" : "var(--ink-3)" }}>{s.full}</div>
                {cur && <div style={{ fontSize: 11.5, color: "var(--primary)", fontWeight: 700, marginTop: 1 }}>Hozirgi bosqich</div>}
              </div>
            </div>
          );
        })}
      </div>
    );
  }
  // compact bar with dots
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
      {STAGES.map((s, i) => (
        <div key={s.id} style={{ flex: 1, height: 5, borderRadius: 3,
          background: i <= stage ? (stage === STAGES.length-1 ? "var(--success)" : "var(--primary)") : "var(--line-2)" }} />
      ))}
    </div>
  );
}

/* ---------- product cards ---------- */

function ProductCard({ p, onOpen, onAdd, thumbH = 118 }) {
  return (
    <div onClick={() => onOpen?.(p)} style={{ background: "var(--card)", borderRadius: 16, padding: 9,
      border: "1px solid var(--line)", cursor: "pointer", display: "flex", flexDirection: "column", gap: 8 }}>
      <div style={{ position: "relative" }}>
        <Thumb cat={p.cat} h={thumbH} />
        {p.hot && <div style={{ position: "absolute", top: 7, left: 7 }}><Badge icon="flame" bg="var(--accent)" color="#fff">HIT</Badge></div>}
        <div style={{ position: "absolute", top: 7, right: 7 }}><Badge bg="rgba(15,23,41,0.78)" color="#fff" icon="rate">+{marginOf(p)}%</Badge></div>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 6, padding: "0 2px 2px" }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: "var(--ink)", lineHeight: 1.25, minHeight: 32,
          display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>{p.name}</div>
        <Stars rating={p.rating} sold={p.sold} />
        <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", marginTop: 2 }}>
          <div>
            <div style={{ fontSize: 10.5, color: "var(--ink-3)", fontWeight: 600 }}>Tan narx · {p.moq}+ dona</div>
            <Money value={p.cost} size={16} />
          </div>
          <button onClick={(e) => { e.stopPropagation(); onAdd?.(p); }} style={{ width: 34, height: 34, borderRadius: 10,
            background: "var(--primary)", border: "none", display: "grid", placeItems: "center", cursor: "pointer", flexShrink: 0,
            boxShadow: "0 4px 10px -3px var(--primary)" }}>
            <Icon name="plus" size={18} color="#fff" stroke={2.6} />
          </button>
        </div>
      </div>
    </div>
  );
}

function ProductRow({ p, onOpen, onAdd }) {
  return (
    <div onClick={() => onOpen?.(p)} style={{ background: "var(--card)", borderRadius: 16, padding: 10, border: "1px solid var(--line)",
      display: "flex", gap: 12, cursor: "pointer" }}>
      <div style={{ width: 96, flexShrink: 0, position: "relative" }}>
        <Thumb cat={p.cat} h={96} radius={12} />
        {p.hot && <div style={{ position: "absolute", top: 6, left: 6 }}><Badge icon="flame" bg="var(--accent)" color="#fff">HIT</Badge></div>}
      </div>
      <div style={{ flex: 1, minWidth: 0, display: "flex", flexDirection: "column", gap: 5 }}>
        <div style={{ fontSize: 14, fontWeight: 700, color: "var(--ink)", lineHeight: 1.25 }}>{p.name}</div>
        <Stars rating={p.rating} sold={p.sold} />
        <div style={{ display: "flex", gap: 6, marginTop: 1 }}>
          <Badge bg="var(--success-soft)" color="var(--success)" icon="rate">{marginOf(p)}% foyda</Badge>
          <Badge bg="var(--primary-soft)" color="var(--primary-ink)">MOQ {p.moq}</Badge>
        </div>
        <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", marginTop: "auto" }}>
          <div>
            <div style={{ fontSize: 10.5, color: "var(--ink-3)", fontWeight: 600 }}>Tan narx</div>
            <div style={{ display: "flex", alignItems: "baseline", gap: 7 }}>
              <Money value={p.cost} size={17} />
              <span style={{ fontFamily: "var(--mono)", fontSize: 11.5, color: "var(--ink-3)", textDecoration: "line-through" }}>{fmtSom(p.sell)}</span>
            </div>
          </div>
          <button onClick={(e) => { e.stopPropagation(); onAdd?.(p); }} style={{ height: 34, padding: "0 14px", borderRadius: 10,
            background: "var(--primary)", border: "none", color: "#fff", fontWeight: 800, fontSize: 13, cursor: "pointer",
            display: "inline-flex", alignItems: "center", gap: 5, fontFamily: "var(--ui)" }}>
            <Icon name="cart" size={15} color="#fff" /> Savatga
          </button>
        </div>
      </div>
    </div>
  );
}

/* ---------- section header ---------- */

function SectionHeader({ title, action, onAction }) {
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "0 2px" }}>
      <h3 style={{ margin: 0, fontSize: 17, fontWeight: 800, color: "var(--ink)", letterSpacing: "-0.01em" }}>{title}</h3>
      {action && <button onClick={onAction} style={{ background: "none", border: "none", color: "var(--primary)", fontWeight: 700,
        fontSize: 13, cursor: "pointer", display: "inline-flex", alignItems: "center", gap: 2, fontFamily: "var(--ui)" }}>
        {action} <Icon name="chevR" size={15} color="var(--primary)" /></button>}
    </div>
  );
}

/* ---------- bottom nav ---------- */

function BottomNav({ active, onNav, cartCount }) {
  const tabs = [
    { id: "home", icon: "home", label: "Asosiy" },
    { id: "catalog", icon: "grid", label: "Katalog" },
    { id: "cart", icon: "cart", label: "Savatcha" },
    { id: "orders", icon: "box", label: "Buyurtma" },
    { id: "account", icon: "user", label: "Profil" },
  ];
  return (
    <div style={{ display: "flex", background: "var(--card)", borderTop: "1px solid var(--line)", padding: "8px 6px 6px", flexShrink: 0 }}>
      {tabs.map(t => {
        const on = active === t.id;
        return (
          <button key={t.id} onClick={() => onNav(t.id)} style={{ flex: 1, background: "none", border: "none", cursor: "pointer",
            display: "flex", flexDirection: "column", alignItems: "center", gap: 4, padding: "4px 0", fontFamily: "var(--ui)" }}>
            <div style={{ position: "relative", width: 56, height: 30, borderRadius: 16, display: "grid", placeItems: "center",
              background: on ? "var(--primary-soft)" : "transparent", transition: "background 0.15s" }}>
              <Icon name={t.icon} size={21} color={on ? "var(--primary)" : "var(--ink-3)"} stroke={on ? 2.4 : 2} />
              {t.id === "cart" && cartCount > 0 && (
                <span style={{ position: "absolute", top: -3, right: 8, minWidth: 16, height: 16, padding: "0 4px", borderRadius: 8,
                  background: "var(--accent)", color: "#fff", fontSize: 10, fontWeight: 800, display: "grid", placeItems: "center",
                  border: "2px solid var(--card)" }}>{cartCount}</span>
              )}
            </div>
            <span style={{ fontSize: 10.5, fontWeight: on ? 800 : 600, color: on ? "var(--primary)" : "var(--ink-3)" }}>{t.label}</span>
          </button>
        );
      })}
    </div>
  );
}

window.UI = { Money, Stars, Badge, Thumb, ExchangeTicker, OrderProgress, ProductCard, ProductRow, SectionHeader, BottomNav };
