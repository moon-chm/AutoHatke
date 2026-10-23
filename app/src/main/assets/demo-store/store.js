/*
 * Modara — a fictional fashion store bundled with AutoCoupons and served locally through
 * WebViewAssetLoader. Coupon rules live here (the "store server"), so the Android app must
 * observe outcomes through the page exactly as it would on a real site. Nothing is ever sold:
 * there is no ordering or payment code at all.
 */
(function () {
  'use strict';

  var STORAGE_KEY = 'ac-demo-store-v1';
  var SERVER_LATENCY_MS = 450;

  var ART = {
    sneakers: '<path d="M20 78C20 60 40 52 58 50L78 40C86 36 96 38 100 46L108 62C120 64 138 68 142 80V84H20Z" fill="#4f46e5"/><path d="M20 84H142V88C142 91 139 94 134 94H26C22 94 20 91 20 88Z" fill="#fff" stroke="#c7d2fe" stroke-width="2"/><path d="M70 50l10 8M78 45l10 8M86 42l9 8" stroke="#fff" stroke-width="3" stroke-linecap="round"/><path d="M48 74C70 72 96 66 118 70" stroke="#ffe372" stroke-width="5" fill="none" stroke-linecap="round"/>',
    tee: '<path d="M56 26L40 32 22 50 36 62 46 54V98H114V54L124 62 138 50 120 32 104 26C100 34 92 38 80 38S60 34 56 26Z" fill="#10b981"/><path d="M60 27C64 33 72 36 80 36S96 33 100 27" stroke="#047857" stroke-width="3" fill="none"/><rect x="58" y="58" width="44" height="6" rx="3" fill="#a7f3d0"/>',
    bag: '<path d="M58 52C58 30 102 30 102 52" stroke="#7a4a22" stroke-width="7" fill="none" stroke-linecap="round"/><rect x="24" y="50" width="112" height="50" rx="20" fill="#c2773a"/><path d="M36 63H124" stroke="#8a5428" stroke-width="3" stroke-dasharray="6 4"/><rect x="56" y="72" width="48" height="20" rx="6" fill="#a8622c"/>',
    earbuds: '<rect x="50" y="60" width="60" height="42" rx="20" fill="#e2e8f0"/><path d="M50 78H110" stroke="#cbd5e1" stroke-width="2"/><circle cx="68" cy="34" r="11" fill="#fff" stroke="#94a3b8" stroke-width="2"/><rect x="64" y="38" width="8" height="18" rx="4" fill="#fff" stroke="#94a3b8" stroke-width="2"/><circle cx="92" cy="34" r="11" fill="#fff" stroke="#94a3b8" stroke-width="2"/><rect x="88" y="38" width="8" height="18" rx="4" fill="#fff" stroke="#94a3b8" stroke-width="2"/><circle cx="80" cy="90" r="3" fill="#22c55e"/>',
    watch: '<rect x="68" y="12" width="24" height="96" rx="8" fill="#334155"/><circle cx="80" cy="60" r="28" fill="#f8fafc" stroke="#94a3b8" stroke-width="5"/><path d="M80 60V44M80 60L92 66" stroke="#0f172a" stroke-width="3" stroke-linecap="round"/><rect x="108" y="56" width="6" height="8" rx="2" fill="#94a3b8"/>',
    slides: '<ellipse cx="80" cy="86" rx="58" ry="14" fill="#f59e0b"/><path d="M46 84C46 54 114 54 114 84Z" fill="#ec4899"/><path d="M56 72C70 62 90 62 104 72" stroke="#fff" stroke-width="4" fill="none" opacity=".6"/>'
  };

  var PRODUCTS = [
    { sku: 'sneakers', name: 'Cloudstep Running Shoes', category: 'footwear', price: 249900, mrp: 329900, tint: '#eef2ff' },
    { sku: 'tee', name: 'Essential Cotton Tee', category: 'apparel', price: 79900, mrp: 99900, tint: '#ecfdf5' },
    { sku: 'bag', name: 'Canvas Weekender Bag', category: 'accessories', price: 189900, mrp: 249900, tint: '#fff7ed' },
    { sku: 'earbuds', name: 'Pulse Wireless Earbuds', category: 'electronics', price: 199900, mrp: 299900, tint: '#f1f5f9' },
    { sku: 'watch', name: 'Minimal Analog Watch', category: 'accessories', price: 149900, mrp: 199900, tint: '#f5f3ff' },
    { sku: 'slides', name: 'Comfort Slides', category: 'footwear', price: 69900, mrp: 89900, tint: '#fdf2f8' }
  ];

  function art(sku) {
    return '<svg viewBox="0 0 160 120" aria-hidden="true">' + (ART[sku] || '') + '</svg>';
  }

  // Deterministic store rules. Expected outcomes for the default bag are in docs/TESTING.md.
  var COUPONS = {
    FLAT100: { kind: 'flat', amount: 10000, min: 99900 },
    SAVE10: { kind: 'percent', percent: 10, cap: 20000 },
    STYLE250: { kind: 'flat', amount: 25000, min: 250000 },
    MEGA500: { kind: 'flat', amount: 50000, min: 500000 },
    SHOES15: { kind: 'percent', percent: 15, cap: 15000, category: 'footwear' },
    FESTIVE50: { kind: 'flat', amount: 5000, expired: '31 Mar 2026' }
  };

  var DEFAULT_CART = { sneakers: 1, tee: 1 };

  function load() {
    try {
      var raw = sessionStorage.getItem(STORAGE_KEY);
      if (raw) {
        var parsed = JSON.parse(raw);
        if (parsed && parsed.cart) return parsed;
      }
    } catch (e) { /* storage unavailable: fall back to defaults */ }
    return { cart: Object.assign({}, DEFAULT_CART), coupon: null };
  }

  var state = load();

  function save() {
    try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch (e) { /* ignore */ }
  }

  function product(sku) {
    for (var i = 0; i < PRODUCTS.length; i++) if (PRODUCTS[i].sku === sku) return PRODUCTS[i];
    return null;
  }

  function lines() {
    return Object.keys(state.cart).filter(function (sku) { return state.cart[sku] > 0 && product(sku); })
      .map(function (sku) { var p = product(sku); return { product: p, qty: state.cart[sku], total: p.price * state.cart[sku] }; });
  }

  function subtotal() { return lines().reduce(function (sum, l) { return sum + l.total; }, 0); }

  function rupees(minor) {
    var major = Math.floor(minor / 100);
    var paise = minor % 100;
    var s = String(major);
    if (s.length > 3) {
      var last = s.slice(-3), rest = s.slice(0, -3);
      rest = rest.replace(/\B(?=(\d{2})+(?!\d))/g, ',');
      s = rest + ',' + last;
    }
    return '₹' + s + (paise ? '.' + String(paise).padStart(2, '0') : '');
  }

  /** The store's coupon engine. Returns {ok, discount, message}. */
  function evaluate(code) {
    var rule = COUPONS[code];
    var sub = subtotal();
    if (!rule) return { ok: false, message: 'Coupon code ' + code + ' is not valid' };
    if (rule.expired) return { ok: false, message: 'This coupon expired on ' + rule.expired };
    if (rule.min && sub < rule.min) {
      return { ok: false, message: 'Add items worth ' + rupees(rule.min - sub) + ' more to use ' + code + ' (minimum ' + rupees(rule.min) + ')' };
    }
    var base = sub;
    if (rule.category) {
      base = lines().filter(function (l) { return l.product.category === rule.category; })
        .reduce(function (sum, l) { return sum + l.total; }, 0);
      if (base === 0) return { ok: false, message: 'No ' + rule.category + ' items in your bag for ' + code };
    }
    var discount = rule.kind === 'flat' ? rule.amount : Math.floor(base * rule.percent / 100);
    if (rule.cap) discount = Math.min(discount, rule.cap);
    discount = Math.min(discount, sub);
    return { ok: true, discount: discount, message: code + ' applied — you save ' + rupees(discount) };
  }

  function currentDiscount() {
    if (!state.coupon) return 0;
    var result = evaluate(state.coupon);
    return result.ok ? result.discount : 0;
  }

  window.DemoStore = {
    products: PRODUCTS,
    art: art,
    rupees: rupees,
    lines: lines,
    subtotal: subtotal,
    discount: currentDiscount,
    coupon: function () { return state.coupon; },
    count: function () { return lines().reduce(function (n, l) { return n + l.qty; }, 0); },
    qty: function (sku) { return state.cart[sku] || 0; },
    setQty: function (sku, qty) {
      state.cart[sku] = Math.max(0, Math.min(9, qty));
      // Like a real store: re-validate the applied coupon whenever the bag changes.
      var dropped = null;
      if (state.coupon && !evaluate(state.coupon).ok) { dropped = state.coupon; state.coupon = null; }
      save();
      return dropped;
    },
    reset: function () { state = { cart: Object.assign({}, DEFAULT_CART), coupon: null }; save(); },
    /** Simulates a round trip to the store's coupon service. */
    applyCoupon: function (code, callback) {
      var normalized = String(code || '').trim().toUpperCase();
      setTimeout(function () {
        if (!normalized) { callback({ ok: false, message: 'Enter a coupon code' }); return; }
        if (state.coupon) { callback({ ok: false, message: 'Remove ' + state.coupon + ' before applying another coupon' }); return; }
        var result = evaluate(normalized);
        if (result.ok) { state.coupon = normalized; save(); }
        callback(result);
      }, SERVER_LATENCY_MS);
    },
    removeCoupon: function (callback) {
      setTimeout(function () { state.coupon = null; save(); callback(); }, SERVER_LATENCY_MS / 3);
    }
  };
})();
