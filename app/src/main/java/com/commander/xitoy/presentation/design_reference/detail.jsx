// detail.jsx — full-screen pushable pages: ProductDetail + OrderTracking.
// Reads window.DALLI, window.UI, window.Icon.

const { fmtSom: fD, PRODUCTS: PD, STAGES: SD, catById: cD, margin: mD, profit: pD } = window.DALLI;
const { Money: MD2, Stars: StD, Badge: BD2, Thumb: ThD, ProductCard: CardD, OrderProgress: PgD } = window.UI;

/* circular floating button */
function RoundBtn({ icon, onClick, badge, light }) {
  return (
    <button onClick={onClick} style={{ position: "relative", width: 40, height: 40, borderRadius: 12, cursor: "pointer",
      background: light ? "rgba(255,255,255,0.92)" : "var(--card)", border: "1px solid var(--line)", display: "grid", placeItems: "center",
      boxShadow: "0 2px 8px rgba(15,23,41,0.08)" }}>
      <Icon name={icon} size={20} color="var(--ink)" />
      {badge > 0 && <span style={{ position: "absolute", top: -5, right: -5, minWidth: 17, height: 17, padding: "0 4px", borderRadius: 9,
        background: "var(--accent)", color: "#fff", fontSize: 10, fontWeight: 800, display: "grid", placeItems: "center", border: "2px solid var(--card)" }}>{badge}</span>}
    </button>
  );
}

function InfoChip({ icon, label, value, tone }) {
  return (
    <div style={{ flex: 1, background: "var(--card)", border: "1px solid var(--line)", borderRadius: 13, padding: "11px 10px", textAlign: "center" }}>
      <Icon name={icon} size={17} color={tone || "var(--primary)"} style={{ margin: "0 auto" }} />
      <div style={{ fontSize: 13, fontWeight: 800, color: "var(--ink)", marginTop: 5 }}>{value}</div>
      <div style={{ fontSize: 10.5, color: "var(--ink-3)", fontWeight: 600, marginTop: 1 }}>{label}</div>
    </div>
  );
}

/* ============ PRODUCT DETAIL PAGE ============ */
function ProductDetail({ p, onBack, onAdd, onNav, onOpen, cartCount }) {
  const [qty, setQty] = React.useState(p.moq);
  const [sell, setSell] = React.useState(p.sell);
  const [added, setAdded] = React.useState(false);
  const [activeImg, setActiveImg] = React.useState(0);
  React.useEffect(() => { setQty(p.moq); setSell(p.sell); setAdded(false); setActiveImg(0); }, [p]);

  const c = cD(p.cat);
  const unitProfit = sell - p.cost, m = sell > 0 ? Math.round((unitProfit / sell) * 100) : 0;
  const totalCost = p.cost * qty, totalProfit = unitProfit * qty;
  const similar = PD.filter(x => x.cat === p.cat && x.id !== p.id).concat(PD.filter(x => x.cat !== p.cat)).slice(0, 6);
  const specs = [["Brend", "OEM / No-name"], ["Material", "Premium"], ["Karobkada", `${p.moq} dona`], ["Og'irlik", "0.4 kg / dona"], ["Sertifikat", "CE, RoHS"]];

  const doAdd = () => { onAdd(p, qty); setAdded(true); setTimeout(() => setAdded(false), 1600); };

  return (
    <div style={{ position: "absolute", inset: 0, zIndex: 30, background: "var(--bg)", display: "flex", flexDirection: "column",
      animation: "slideIn .26s cubic-bezier(.2,.8,.2,1)" }}>
      <div className="app-main" style={{ flex: 1, overflowY: "auto", overflowX: "hidden" }}>
        {/* hero */}
        <div style={{ position: "relative", padding: "12px 16px 0" }}>
          <div style={{ position: "absolute", top: 12, left: 16, right: 16, display: "flex", justifyContent: "space-between", zIndex: 2 }}>
            <RoundBtn icon="chevL" onClick={onBack} />
            <div style={{ display: "flex", gap: 8 }}>
              <RoundBtn icon="heart" />
              <RoundBtn icon="cart" onClick={() => onNav("cart")} badge={cartCount} />
            </div>
          </div>
          <ThD cat={p.cat} h={290} radius={20} label="mahsulot rasmi" />
          {p.hot && <div style={{ position: "absolute", top: 60, left: 28 }}><BD2 icon="flame" bg="var(--accent)" color="#fff">HIT mahsulot</BD2></div>}
        </div>
        {/* thumbnails */}
        <div style={{ display: "flex", gap: 8, padding: "12px 16px 0" }}>
          {[0,1,2,3].map(i => (
            <button key={i} onClick={() => setActiveImg(i)} style={{ width: 58, height: 58, borderRadius: 12, overflow: "hidden", cursor: "pointer",
              border: activeImg === i ? "2px solid var(--primary)" : "1px solid var(--line)", padding: 0, background: c.tint, position: "relative" }}>
              <div style={{ position: "absolute", inset: 0, opacity: 0.5, backgroundImage: `repeating-linear-gradient(45deg, ${c.ink}14 0 1px, transparent 1px 8px)` }} />
              <Icon name={c.glyph} size={22} color={c.ink} stroke={1.6} style={{ position: "absolute", top: "50%", left: "50%", transform: "translate(-50%,-50%)", opacity: 0.55 }} />
            </button>
          ))}
        </div>

        <div style={{ padding: "16px 16px 24px", display: "flex", flexDirection: "column", gap: 16 }}>
          <div>
            <div style={{ fontSize: 12.5, fontWeight: 700, color: c.ink, marginBottom: 5 }}>{c.label}</div>
            <h1 style={{ margin: "0 0 8px", fontSize: 22, fontWeight: 800, letterSpacing: "-0.02em", lineHeight: 1.2 }}>{p.name}</h1>
            <StD rating={p.rating} sold={p.sold} />
          </div>

          {/* price block */}
          <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", background: "var(--card)",
            border: "1px solid var(--line)", borderRadius: 16, padding: 15 }}>
            <div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", fontWeight: 600 }}>Ulgurji tan narx · {p.moq}+ dona</div>
              <div style={{ display: "flex", alignItems: "baseline", gap: 9, marginTop: 3 }}>
                <MD2 value={p.cost} size={24} />
                <span style={{ fontFamily: "var(--mono)", fontSize: 13, color: "var(--ink-3)", textDecoration: "line-through", whiteSpace: "nowrap" }}>{fD(p.sell)}</span>
              </div>
            </div>
            <BD2 bg="var(--success-soft)" color="var(--success)" icon="rate">{mD(p)}% foyda</BD2>
          </div>

          {/* info chips */}
          <div style={{ display: "flex", gap: 9 }}>
            <InfoChip icon="box" label="Min. partiya" value={`${p.moq} dona`} />
            <InfoChip icon="truck" label="Yetkazish" value="8-12 kun" tone="var(--accent)" />
            <InfoChip icon="check" label="Omborda" value="Mavjud" tone="var(--success)" />
          </div>

          {/* supplier */}
          <div style={{ background: "var(--card)", border: "1px solid var(--line)", borderRadius: 16, padding: 14, display: "flex", alignItems: "center", gap: 12 }}>
            <div style={{ width: 42, height: 42, borderRadius: 12, background: "var(--ink)", color: "#fff", display: "grid", placeItems: "center", flexShrink: 0 }}>
              <Icon name="globe" size={21} color="#fff" />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 800 }}>Guangzhou Trade Co.</div>
              <div style={{ fontSize: 11.5, color: "var(--ink-3)", fontWeight: 600 }}>Tasdiqlangan yetkazib beruvchi · 6 yil</div>
            </div>
            <BD2 bg="var(--primary-soft)" color="var(--primary-ink)" icon="star">4.9</BD2>
          </div>

          {/* markup calc */}
          <div style={{ background: "var(--ink)", borderRadius: 16, padding: 16, color: "#fff" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 7, marginBottom: 13 }}>
              <Icon name="calc" size={18} color="#5ee29a" /><span style={{ fontWeight: 800, fontSize: 15 }}>Foyda kalkulyatori</span>
            </div>
            <label style={{ fontSize: 12.5, color: "rgba(255,255,255,0.7)", fontWeight: 600 }}>Sotuv narxingiz (dona)</label>
            <div style={{ display: "flex", alignItems: "center", gap: 10, background: "rgba(255,255,255,0.1)", borderRadius: 11, padding: "0 13px", marginTop: 7 }}>
              <input type="number" value={sell} onChange={e => setSell(Math.max(0, +e.target.value || 0))}
                style={{ flex: 1, border: "none", outline: "none", background: "none", fontFamily: "var(--mono)", fontWeight: 800, fontSize: 19, color: "#fff", padding: "12px 0", width: "100%" }} />
              <span style={{ fontSize: 13, color: "rgba(255,255,255,0.6)", fontWeight: 600 }}>so'm</span>
            </div>
            <input type="range" min={p.cost} max={Math.round(p.cost * 3)} step={1000} value={Math.min(sell, p.cost * 3)}
              onChange={e => setSell(+e.target.value)} style={{ width: "100%", marginTop: 14 }} />
            <div style={{ display: "flex", justifyContent: "space-between", marginTop: 14, gap: 10 }}>
              {[["Dona foyda", "+" + fD(unitProfit), "#5ee29a"], ["Margin", m + "%", "#7eb0ff"], [`Jami (${qty})`, "+" + fD(totalProfit), "#5ee29a"]].map(([k, v, col]) => (
                <div key={k} style={{ flex: 1 }}>
                  <div style={{ fontSize: 10.5, color: "rgba(255,255,255,0.55)", fontWeight: 600 }}>{k}</div>
                  <div style={{ fontFamily: "var(--mono)", fontWeight: 800, fontSize: 15, color: col, marginTop: 2 }}>{v}</div>
                </div>
              ))}
            </div>
          </div>

          {/* description */}
          <div>
            <h3 style={{ margin: "0 0 7px", fontSize: 16, fontWeight: 800 }}>Tavsifi</h3>
            <p style={{ margin: 0, fontSize: 13.5, color: "var(--ink-2)", lineHeight: 1.6 }}>
              Yuqori sifatli {c.label.toLowerCase()} mahsuloti, to'g'ridan-to'g'ri Xitoy fabrikasidan ulgurji narxda yetkaziladi.
              Resellerlar uchun ideal: barqaror talab, yuqori margin va ishonchli sifat nazorati. Har bir partiya bojxona hujjatlari bilan rasmiylashtiriladi.
            </p>
          </div>

          {/* specs */}
          <div style={{ background: "var(--card)", border: "1px solid var(--line)", borderRadius: 16, overflow: "hidden" }}>
            {specs.map(([k, v], i) => (
              <div key={k} style={{ display: "flex", justifyContent: "space-between", padding: "12px 15px", borderTop: i ? "1px solid var(--line)" : "none" }}>
                <span style={{ fontSize: 13, color: "var(--ink-3)", fontWeight: 600 }}>{k}</span>
                <span style={{ fontSize: 13, color: "var(--ink)", fontWeight: 700 }}>{v}</span>
              </div>
            ))}
          </div>

          {/* similar */}
          <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
            <h3 style={{ margin: 0, fontSize: 16, fontWeight: 800 }}>O'xshash tovarlar</h3>
            <div className="hscroll" style={{ display: "flex", gap: 11, overflowX: "auto", margin: "0 -16px", padding: "0 16px" }}>
              {similar.map(sp => <div key={sp.id} style={{ width: 150, flexShrink: 0 }}><CardD p={sp} onOpen={() => onOpen(sp)} onAdd={() => onAdd(sp, sp.moq)} thumbH={120} /></div>)}
            </div>
          </div>
        </div>
      </div>

      {/* sticky add bar */}
      <div style={{ background: "var(--card)", borderTop: "1px solid var(--line)", padding: "12px 16px 14px", display: "flex", alignItems: "center", gap: 12 }}>
        <div style={{ display: "flex", alignItems: "center", border: "1px solid var(--line)", borderRadius: 12, overflow: "hidden" }}>
          <button onClick={() => setQty(Math.max(p.moq, qty - p.moq))} style={qBtn}><Icon name="minus" size={16} color="var(--ink-2)" stroke={2.6} /></button>
          <span style={{ minWidth: 42, textAlign: "center", fontFamily: "var(--mono)", fontWeight: 700, fontSize: 15 }}>{qty}</span>
          <button onClick={() => setQty(qty + p.moq)} style={qBtn}><Icon name="plus" size={16} color="var(--ink-2)" stroke={2.6} /></button>
        </div>
        <button onClick={doAdd} style={{ flex: 1, height: 52, borderRadius: 14, border: "none", cursor: "pointer", fontFamily: "var(--ui)",
          background: added ? "var(--success)" : "var(--primary)", color: "#fff", fontWeight: 800, fontSize: 15,
          display: "inline-flex", alignItems: "center", justifyContent: "center", gap: 8, transition: "background .2s",
          boxShadow: `0 8px 20px -6px ${added ? "var(--success)" : "var(--primary)"}` }}>
          {added ? <><Icon name="check" size={19} color="#fff" stroke={3} /> Qo'shildi</> : <><Icon name="cart" size={18} color="#fff" /> Savatga · {fD(totalCost)} so'm</>}
        </button>
      </div>
    </div>
  );
}
const qBtn = { width: 34, height: 34, background: "var(--bg)", border: "none", display: "grid", placeItems: "center", cursor: "pointer" };

/* ============ ORDER TRACKING PAGE ============ */
function OrderTracking({ o, onBack }) {
  const done = o.stage === SD.length - 1;
  // synthesize line items + timeline timestamps
  const items = PD.slice(0, o.items).map((p, i) => ({ p, qty: [12, 18, 15, 20, 15][i] || 10 }));
  const times = ["09-iyun 14:20", "09-iyun 18:05", "10-iyun 09:40", "11-iyun 07:15", "—", "—"];
  return (
    <div style={{ position: "absolute", inset: 0, zIndex: 30, background: "var(--bg)", display: "flex", flexDirection: "column",
      animation: "slideIn .26s cubic-bezier(.2,.8,.2,1)" }}>
      {/* header */}
      <div style={{ display: "flex", alignItems: "center", gap: 12, padding: "14px 16px", background: "var(--card)", borderBottom: "1px solid var(--line)" }}>
        <RoundBtn icon="chevL" onClick={onBack} />
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 15, fontWeight: 800, fontFamily: "var(--mono)" }}>{o.id}</div>
          <div style={{ fontSize: 11.5, color: "var(--ink-3)", fontWeight: 600 }}>{o.items} pozitsiya · {o.qty} dona</div>
        </div>
        <RoundBtn icon="refresh" />
      </div>

      <div className="app-main" style={{ flex: 1, overflowY: "auto", overflowX: "hidden" }}>
        <div style={{ padding: "16px 16px 24px", display: "flex", flexDirection: "column", gap: 14 }}>
        {/* status hero */}
        <div style={{ background: "var(--ink)", borderRadius: 20, padding: 18, color: "#fff", position: "relative", overflow: "hidden" }}>
          <div style={{ position: "absolute", right: -30, top: -30, width: 130, height: 130, borderRadius: "50%", background: done ? "rgba(94,226,154,0.12)" : "rgba(27,102,255,0.18)" }} />
          <span style={{ fontSize: 11.5, fontWeight: 700, color: "rgba(255,255,255,0.6)", letterSpacing: "0.03em" }}>HOZIRGI HOLAT</span>
          <div style={{ fontSize: 23, fontWeight: 800, letterSpacing: "-0.01em", margin: "5px 0 16px" }}>{SD[o.stage].full}</div>
          {/* route */}
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <Icon name="globe" size={17} color="#5ee29a" />
            <span style={{ fontFamily: "var(--mono)", fontSize: 12.5, fontWeight: 700, color: "rgba(255,255,255,0.85)" }}>Guangzhou</span>
            <div style={{ flex: 1, height: 2, background: "repeating-linear-gradient(90deg,#5ee29a 0 6px,transparent 6px 11px)" }} />
            <Icon name="truck" size={17} color="#5ee29a" />
            <div style={{ flex: 1, height: 2, background: "repeating-linear-gradient(90deg,rgba(255,255,255,0.3) 0 6px,transparent 6px 11px)" }} />
            <Icon name="pin" size={17} color="rgba(255,255,255,0.5)" />
            <span style={{ fontFamily: "var(--mono)", fontSize: 12.5, fontWeight: 700, color: "rgba(255,255,255,0.5)" }}>Toshkent</span>
          </div>
          <div style={{ display: "flex", gap: 20, marginTop: 16 }}>
            <div><div style={{ fontSize: 10.5, color: "rgba(255,255,255,0.55)", fontWeight: 600, whiteSpace: "nowrap" }}>Yetkazish muddati</div>
              <div style={{ fontWeight: 800, fontSize: 15, color: "#5ee29a", marginTop: 3 }}>{o.eta}</div></div>
            <div><div style={{ fontSize: 10.5, color: "rgba(255,255,255,0.55)", fontWeight: 600, whiteSpace: "nowrap" }}>Trek raqami</div>
              <div style={{ fontFamily: "var(--mono)", fontWeight: 700, fontSize: 14, marginTop: 3 }}>YT{o.id.replace("DS-", "")}CN</div></div>
          </div>
        </div>

        {/* map placeholder */}
        <div style={{ position: "relative", height: 130, borderRadius: 16, overflow: "hidden", background: "oklch(0.93 0.02 230)" }}>
          <div style={{ position: "absolute", inset: 0, opacity: 0.5, backgroundImage: "repeating-linear-gradient(45deg, oklch(0.55 0.08 230 / 0.18) 0 1px, transparent 1px 11px)" }} />
          <svg width="100%" height="100%" viewBox="0 0 340 130" preserveAspectRatio="none" style={{ position: "absolute", inset: 0 }}>
            <path d="M40 95 Q160 10 300 45" fill="none" stroke="var(--primary)" strokeWidth="2.5" strokeDasharray="6 6" />
            <circle cx="40" cy="95" r="6" fill="var(--ink)" /><circle cx="300" cy="45" r="6" fill="var(--primary)" />
            <circle cx="180" cy="44" r="9" fill="var(--primary)" opacity="0.25" /><circle cx="180" cy="44" r="4.5" fill="var(--primary)" />
          </svg>
          <span style={{ position: "absolute", left: 10, bottom: 8, fontFamily: "var(--mono)", fontSize: 9.5, color: "var(--ink-3)", letterSpacing: "0.04em", textTransform: "uppercase" }}>jonli xarita</span>
        </div>

        {/* timeline */}
        <div style={{ background: "var(--card)", border: "1px solid var(--line)", borderRadius: 18, padding: "18px 16px" }}>
          <h3 style={{ margin: "0 0 16px", fontSize: 16, fontWeight: 800 }}>Yetkazish bosqichlari</h3>
          {SD.map((s, i) => {
            const sdone = i < o.stage, cur = i === o.stage;
            return (
              <div key={s.id} style={{ display: "flex", gap: 13, minHeight: i === SD.length - 1 ? 0 : 46 }}>
                <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                  <div style={{ width: 22, height: 22, borderRadius: "50%", flexShrink: 0, background: sdone ? "var(--success)" : cur ? "var(--primary)" : "#fff",
                    border: `2px solid ${sdone || cur ? "transparent" : "var(--line-2)"}`, display: "grid", placeItems: "center" }}>
                    {sdone && <Icon name="check" size={12} color="#fff" stroke={3} />}
                    {cur && <div style={{ width: 8, height: 8, borderRadius: "50%", background: "#fff" }} />}
                  </div>
                  {i < SD.length - 1 && <div style={{ width: 2, flex: 1, background: i < o.stage ? "var(--success)" : "var(--line-2)" }} />}
                </div>
                <div style={{ paddingBottom: 18, flex: 1, display: "flex", justifyContent: "space-between", gap: 10 }}>
                  <div style={{ fontSize: 14, fontWeight: cur ? 800 : 600, lineHeight: 1.3, color: cur ? "var(--ink)" : sdone ? "var(--ink-2)" : "var(--ink-3)" }}>{s.full}</div>
                  <span style={{ fontSize: 11.5, color: cur ? "var(--primary)" : "var(--ink-3)", fontWeight: cur ? 700 : 600, fontFamily: "var(--mono)", whiteSpace: "nowrap", flexShrink: 0 }}>{times[i]}</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* contents */}
        <div style={{ background: "var(--card)", border: "1px solid var(--line)", borderRadius: 18, padding: "16px 16px 8px" }}>
          <h3 style={{ margin: "0 0 12px", fontSize: 16, fontWeight: 800 }}>Yuk tarkibi</h3>
          {items.map(({ p, qty }) => (
            <div key={p.id} style={{ display: "flex", alignItems: "center", gap: 12, paddingBottom: 12 }}>
              <div style={{ width: 50, flexShrink: 0 }}><ThD cat={p.cat} h={50} radius={10} label="" /></div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13, fontWeight: 700, lineHeight: 1.3 }}>{p.name}</div>
                <div style={{ fontSize: 11.5, color: "var(--ink-3)", fontWeight: 600 }}>{qty} dona × {fD(p.cost)} so'm</div>
              </div>
              <MD2 value={p.cost * qty} size={14} suffix={false} />
            </div>
          ))}
          <div style={{ display: "flex", justifyContent: "space-between", padding: "12px 0 10px", borderTop: "1px dashed var(--line-2)" }}>
            <span style={{ fontSize: 14, fontWeight: 800 }}>Jami summa</span><MD2 value={o.total} size={17} />
          </div>
        </div>

        <button style={{ height: 50, borderRadius: 14, border: "1px solid var(--line)", background: "var(--card)", color: "var(--ink)",
          fontWeight: 800, fontSize: 14.5, cursor: "pointer", fontFamily: "var(--ui)", display: "inline-flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
          <Icon name="user" size={18} color="var(--ink)" /> Yetkazib beruvchiga yozish
        </button>
        </div>
      </div>
    </div>
  );
}

window.DETAIL = { ProductDetail, OrderTracking };
