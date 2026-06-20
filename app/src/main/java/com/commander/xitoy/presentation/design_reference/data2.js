// icons.jsx — lightweight stroke icons. All inherit currentColor.
// Exported as window.Icon, a single component keyed by `name`.

function Icon({ name, size = 22, stroke = 2, color = "currentColor", fill = "none", style }) {
  const common = {
    width: size, height: size, viewBox: "0 0 24 24", fill,
    stroke: color, strokeWidth: stroke, strokeLinecap: "round", strokeLinejoin: "round",
    style,
  };
  const P = {
    home:   <path d="M3 10.5 12 3l9 7.5M5 9.5V20a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V9.5" />,
    grid:   <g><rect x="3.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.5"/></g>,
    cart:   <g><path d="M3 4h2l2.3 11.4a1 1 0 0 0 1 .8h8.7a1 1 0 0 0 1-.8L21 7H6"/><circle cx="9.5" cy="20" r="1.4"/><circle cx="17.5" cy="20" r="1.4"/></g>,
    box:    <g><path d="M21 8 12 3 3 8v8l9 5 9-5V8Z"/><path d="m3 8 9 5 9-5M12 13v8"/></g>,
    user:   <g><circle cx="12" cy="8" r="3.6"/><path d="M5 20c1-3.5 4-5 7-5s6 1.5 7 5"/></g>,
    search: <g><circle cx="11" cy="11" r="6.5"/><path d="m20 20-3.6-3.6"/></g>,
    bell:   <path d="M6 9a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6ZM10 20a2 2 0 0 0 4 0" />,
    pin:    <g><path d="M12 21s7-5.8 7-11a7 7 0 1 0-14 0c0 5.2 7 11 7 11Z"/><circle cx="12" cy="10" r="2.4"/></g>,
    chevR:  <path d="m9 5 7 7-7 7" />,
    chevL:  <path d="m15 5-7 7 7 7" />,
    chevD:  <path d="m5 9 7 7 7-7" />,
    truck:  <g><rect x="1.5" y="6" width="13" height="10" rx="1.5"/><path d="M14.5 9h4l3 3.5V16h-7z"/><circle cx="6" cy="18.5" r="1.8"/><circle cx="17.5" cy="18.5" r="1.8"/></g>,
    calc:   <g><rect x="5" y="2.5" width="14" height="19" rx="2.5"/><path d="M8 6.5h8M8 11h2M11.5 11h1M14 11h2M8 14.5h2M11.5 14.5h1M14 14.5h2M8 18h2M11.5 18h1M14 18h2"/></g>,
    rate:   <g><path d="M3 17l5-5 4 3 8-8"/><path d="M16 7h4v4"/></g>,
    spark:  <path d="M12 2.5 14 9l6.5 2-6.5 2-2 6.5-2-6.5L3.5 11 10 9z" />,
    heart:  <path d="M12 20S4 15 4 9.2A4.2 4.2 0 0 1 12 7a4.2 4.2 0 0 1 8 2.2C20 15 12 20 12 20Z" />,
    star:   <path d="m12 3 2.6 5.4 5.9.8-4.3 4.1 1.1 5.8L12 16.6 6.7 19.2l1.1-5.8L3.5 9.3l5.9-.8L12 3Z" />,
    plus:   <path d="M12 5v14M5 12h14" />,
    minus:  <path d="M5 12h14" />,
    filter: <path d="M3 5h18M6 12h12M10 19h4" />,
    sort:   <path d="M7 4v16m0 0-3-3m3 3 3-3M17 20V4m0 0-3 3m3-3 3 3" />,
    check:  <path d="m5 12.5 4.5 4.5L19 6.5" />,
    clock:  <g><circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/></g>,
    flame:  <path d="M12 3s4 3 4 7a4 4 0 0 1-8 0c0-1 .4-2 1-2.5C9 9 12 8 12 3Zm0 18a5 5 0 0 0 5-5c0-2-1-3-1-3s-1 2-2.5 2C12 15 12 12 12 12s-4 2-4 6a4 4 0 0 0 4 3Z"/>,
    // category glyphs
    shirt:  <path d="M8 3 4 6l2 2 1-1v13h10V7l1 1 2-2-4-3-2 2H10z" />,
    chip:   <g><rect x="6.5" y="6.5" width="11" height="11" rx="1.5"/><path d="M9.5 3v3M14.5 3v3M9.5 18v3M14.5 18v3M3 9.5h3M3 14.5h3M18 9.5h3M18 14.5h3"/></g>,
    drop:   <path d="M12 3s6 6.5 6 11a6 6 0 0 1-12 0C6 9.5 12 3 12 3Z" />,
    block:  <g><rect x="3.5" y="9" width="7" height="7" rx="1"/><rect x="13.5" y="9" width="7" height="7" rx="1"/><path d="M5 9V7.5h4V9M15 9V7.5h4V9"/></g>,
    bag:    <g><path d="M5 8h14l-1 12H6L5 8Z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/></g>,
    qr:     <g><rect x="3.5" y="3.5" width="6" height="6" rx="1"/><rect x="14.5" y="3.5" width="6" height="6" rx="1"/><rect x="3.5" y="14.5" width="6" height="6" rx="1"/><path d="M14.5 14.5h2v2M20.5 14.5v6M14.5 20.5h2"/></g>,
    globe:  <g><circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17M12 3.5c2.5 2.4 2.5 14.6 0 17M12 3.5c-2.5 2.4-2.5 14.6 0 17"/></g>,
    arrowR: <path d="M5 12h14m0 0-5-5m5 5-5 5" />,
    tag:    <g><path d="M3.5 11.5 11 4h7v7l-7.5 7.5a2 2 0 0 1-2.8 0L3.5 14.3a2 2 0 0 1 0-2.8Z"/><circle cx="14.5" cy="7.5" r="1.2"/></g>,
    close:  <path d="M6 6l12 12M18 6 6 18" />,
    gift:   <g><rect x="3.5" y="8.5" width="17" height="4" rx="1"/><path d="M5 12.5V20h14v-7.5M12 8.5V20M12 8.5S10 3.5 7.5 5 9.5 8.5 12 8.5ZM12 8.5s2-5 4.5-3.5S14.5 8.5 12 8.5Z"/></g>,
    refresh:<path d="M20 11a8 8 0 0 0-14-4.5L4 8m0 0V4m0 4h4M4 13a8 8 0 0 0 14 4.5l2-1.5m0 0v4m0-4h-4" />,
  };
  return <svg {...common}>{P[name] || P.box}</svg>;
}

window.Icon = Icon;
