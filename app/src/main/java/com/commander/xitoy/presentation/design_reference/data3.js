// appB.jsx — single full-screen "Discovery" prototype with push navigation + detail pages.

const { HomeB } = window.HOMES;
const { CatalogScreen, CartScreen, OrdersScreen, AccountScreen } = window.SCREENS;
const { ProductDetail, OrderTracking } = window.DETAIL;
const { BottomNav } = window.UI;

function DalliAppFull() {
  const [tab, setTab] = React.useState("home");
  const [catalogCat, setCatalogCat] = React.useState("all");
  const [cart, setCart] = React.useState({});
  const [detail, setDetail] = React.useState(null); // {type:'product',p} | {type:'track',o}
  const mainRef = React.useRef(null);

  const cartCount = Object.values(cart).reduce((s, l) => s + l.qty, 0);
  const addToCart = (p, qty) => setCart(c => {
    const q = qty || p.moq, prev = c[p.id]?.qty || 0;
    return { ...c, [p.id]: { p, qty: prev + q } };
  });
  const setQty = (id, q) => setCart(c => { if (q <= 0) { const n = { ...c }; delete n[id]; return n; } return { ...c, [id]: { ...c[id], qty: q } }; });
  const remove = (id) => setCart(c => { const n = { ...c }; delete n[id]; return n; });

  const scrollTop = () => { if (mainRef.current) mainRef.current.scrollTop = 0; };
  const nav = (t) => { setDetail(null); setTab(t); scrollTop(); };
  const goCat = (id) => { setCatalogCat(id); setDetail(null); setTab("catalog"); scrollTop(); };
  const openProduct = (p) => setDetail({ type: "product", p });
  const openTrack = (o) => setDetail({ type: "track", o });

  const homeProps = { onOpen: openProduct, onAdd: (p) => addToCart(p), onNav: nav, onCat: goCat, onTrack: openTrack };

  let screen;
  if (tab === "home") screen = <HomeB {...homeProps} />;
  else if (tab === "catalog") screen = <CatalogScreen initialCat={catalogCat} onOpen={openProduct} onAdd={(p) => addToCart(p)} />;
  else if (tab === "cart") screen = <CartScreen cart={cart} setQty={setQty} remove={remove} onOpen={openProduct} onNav={nav} />;
  else if (tab === "orders") screen = <OrdersScreen onTrack={openTrack} />;
  else screen = <AccountScreen />;

  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column", position: "relative", background: "var(--bg)", overflow: "hidden" }}>
      <div ref={mainRef} className="app-main" style={{ flex: 1, overflowY: "auto", overflowX: "hidden" }}>{screen}</div>
      <BottomNav active={tab} onNav={nav} cartCount={cartCount} />
      {detail?.type === "product" &&
        <ProductDetail p={detail.p} onBack={() => setDetail(null)} onAdd={addToCart}
          onNav={(t) => { setDetail(null); nav(t); }} onOpen={openProduct} cartCount={cartCount} />}
      {detail?.type === "track" &&
        <OrderTracking o={detail.o} onBack={() => setDetail(null)} />}
    </div>
  );
}

/* ---------- centered, auto-scaling stage ---------- */
function Stage() {
  const W = 412, H = 892, M = 24;
  const [scale, setScale] = React.useState(1);
  React.useEffect(() => {
    const fit = () => setScale(Math.min(1, (window.innerHeight - M * 2) / H, (window.innerWidth - M * 2) / W));
    fit();
    window.addEventListener("resize", fit);
    return () => window.removeEventListener("resize", fit);
  }, []);
  return (
    <div style={{ minHeight: "100vh", width: "100%", background: "var(--page)", display: "flex",
      alignItems: "center", justifyContent: "center", overflow: "hidden" }}>
      <div style={{ width: W * scale, height: H * scale }}>
        <div style={{ width: W, height: H, transform: `scale(${scale})`, transformOrigin: "top left" }}>
          <AndroidDevice>
            <DalliAppFull />
          </AndroidDevice>
        </div>
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<Stage />);
