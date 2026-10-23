# Feasibility

Based on the evidence in `docs/RESEARCH.md` (checked 2026-10-24).

| Capability | Feasible? | What the app does |
|---|---|---|
| Fetch live coupons from the legacy Buyhatke API | **Not currently.** Every documented URL returns HTTP 404. | Calls the API over HTTPS, parses tolerantly, and reports the 404 honestly as "Coupons aren't available right now … (error 404)" with Try again. It never falls back to sample data. Profile → "Coupon feed status" shows the current status. |
| Automatically test coupons on Jabong | **No.** The domain redirects to Myntra. | Not offered. The `pos=1` mapping is kept and unit-tested. |
| Automatically test coupons on Myntra | **Not within the assignment's rules.** It needs the shopper's real login, and the checkout is protected by an Akamai bot manager. | **User-assisted mode:** Myntra opens in the in-app browser, the bag page is detected (`/checkout/cart`), and the sheet lists live-API coupons to copy. Nothing is typed or clicked on Myntra's page. |
| Automated coupon testing on a real checkout DOM | **Yes, on a site we control.** | A bundled **Modara sample store** (HTML/JS, served from `https://appassets.androidplatform.net`) with deterministic coupon rules. The full find → test → rank → re-apply → verify pipeline runs against its real DOM in a real WebView. The app presents it as a store called **Modara**, and Profile → About states that it is a sample store that sells nothing. |
| In-app floating assistant | **Yes.** | A Compose overlay inside the activity window. No `SYSTEM_ALERT_WINDOW` permission. |
| Login / registration | **Yes, as a demo.** | Offline validation only. No backend; passwords are never sent, logged, or stored. |

## Why the sample store is not a shortcut

The coordinator, ranking engine, checkout adapter, and WebView bridge are the same code that a real retailer adapter would use. Supporting a new automated retailer means:

1. adding a `RetailerConfig` (host allowlist, `CheckoutDetector`, `AutomationSupport.AUTOMATED`);
2. writing a `CheckoutAdapter` that maps that site's DOM to `readCart` / `applyCoupon` / `removeCoupon`.

This only makes sense for a retailer whose terms allow it and whose checkout can be used without bypassing authentication or anti-bot controls.

## Risks and how they are handled

| Risk | Handling |
|---|---|
| A coupon is reported as working when it does not work | Only a coupon whose effect was **observed** in the cart (accepted, savings > 0, total = subtotal − savings) can win. Re-applying the winner is verified again; a mismatch is reported as "not confirmed". |
| The bag changes during testing | Each test checks a cart fingerprint first and aborts with "Your bag changed". |
| A coupon stays applied after a failure or cancel | The `cartModified` invariant plus a `NonCancellable` restore puts back the shopper's original state (their own code, or none). |
| The store hangs | Bounded timeouts: apply 12 s, read 6 s, cleanup 6 s. A timed-out coupon is marked FAILED and testing continues. |
| A purchase is triggered by accident | Adapters only touch the coupon input, apply, and remove controls. Modara's "Place order" button places nothing: it only reveals a note that orders can't be placed in the sample store, and the adapter never touches it. |
