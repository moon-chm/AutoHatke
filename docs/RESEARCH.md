# Research

Evidence gathered before and during implementation. Unless noted otherwise, every observation below was made on **2026-10-24 (IST); about 19:07 UTC on 2026-10-23**, from this development machine. Endpoints can change at any time, so re-check before relying on them.

## 1. Assignment

- The assignment page is `https://compare.buyhatke.com/company/android-developer.php`. It asks for an Android app that finds coupons for a retailer, tries them at checkout, and applies the best one.
- It names a legacy coupon endpoint:
  `http://coupons.buyhatke.com/PickCoupon/FreshCoupon/getCoupons.php?pos=N`
  with `pos=1` for Jabong and `pos=2` for Myntra.
- This project is an **independent demonstration**. It is not an official Buyhatke release, and the app says so on the login screen, on Home, and in About & limitations.

## 2. Legacy coupon API

| Request | Result |
|---|---|
| `http://coupons.buyhatke.com/…/getCoupons.php?pos=1` | **HTTP 404**, nginx HTML error page |
| `http://…?pos=2` | **HTTP 404** |
| `https://coupons.buyhatke.com/…?pos=1` | **HTTP 404**. TLS handshake succeeds with a valid certificate. |
| `https://…?pos=2` | **HTTP 404** |

- **Response headers:** `server: cloudflare` in front of the origin. The 404 body identifies `nginx/1.24.0 (Ubuntu)`.
- **Historical schema:** the Wayback Machine CDX endpoint returned "Temporarily Offline", so the historical JSON schema **could not be recovered**.

Consequences for the design:

- The app calls the **HTTPS** URL. TLS works, so no cleartext exception is needed, and `network_security_config.xml` disables cleartext everywhere.
- The parser (`CouponResponseParser`) is deliberately **tolerant**. It accepts:
  - a bare array, an array under a wrapper key, or an object keyed by id;
  - several aliases for each field.

  It rejects HTML bodies, so the real 404 page can never be parsed as an empty coupon list. The real 404 body is a test fixture (`CouponResponseParserTest`, `LegacyBuyhatkeCouponProviderTest`).
- A 404 is shown to the user as **"Coupons aren't available right now"** together with the HTTP code (see `docs/screenshots/12_myntra_assisted.png`). The app never substitutes demo data for a failed live call.

Commands used (Git Bash):

```sh
curl -sS -o /dev/null -w "%{http_code} %{redirect_url}\n" "http://coupons.buyhatke.com/PickCoupon/FreshCoupon/getCoupons.php?pos=1"
curl -sS -D - "https://coupons.buyhatke.com/PickCoupon/FreshCoupon/getCoupons.php?pos=2" | head -40
curl -sS "https://web.archive.org/cdx/search/cdx?url=coupons.buyhatke.com/PickCoupon/*&output=json&limit=20"
```

## 3. Retailers

| Retailer | Observation | Decision |
|---|---|---|
| Jabong (`pos=1`) | `www.jabong.com` answers **301 → myntra.com**. The store no longer exists. | Not offered as a retailer. The provider still maps `jabong → pos=1` so the API contract stays testable. |
| Myntra (`pos=2`) | `https://www.myntra.com/checkout/cart` → **200**, title "SHOPPING BAG". It loads an Akamai bot-manager sensor, and the bag needs a signed-in account. | **User-assisted mode.** The app shows live-API coupons (when the API answers) for the shopper to copy, and never automates Myntra's page. |

Automating Myntra's checkout would mean running scripts against a logged-in session protected by bot detection. That conflicts with the assignment's own rules: no bypassing anti-bot measures or authentication, and no real credentials. Automated coupon testing is therefore demonstrated on the local **Modara sample store** (see `docs/FEASIBILITY.md`).

## 4. Brand colours

Sources, downloaded to the scratchpad, never shipped, and never fetched by the app:

| Source | Finding |
|---|---|
| Current site logo SVG (`buyhatke.com` header) | Fill `#4F46E5` (indigo) with accent `#FFE372` (yellow). Darker logo shades `#3932BF` and `#302B9E`. |
| Favicon / `manifest.json` icons | Sampled with `palette.py` (Pillow, dominant opaque pixels): about `#4840D9`, consistent with the logo indigo. |
| Site inline styles | Surfaces and text use a slate scale: `#F1F5F9`, `#FFFFFF`, `#020618`, `#45556C`, `#62748E`, `#CAD5E2`. Indigo tints: `#EFEFFF`, `#D0CFFF`, `#918FFF`. Status colours: `#008357` and `#E8F8F0` (success), `#FF6D1F` and `#FFF8E7` (warning). |
| Careers-page logo | Cyan `#08B0D8`. This is an **older** brand mark and was not used. |

How each token was mapped is in `docs/UI_DESIGN.md`. **No Buyhatke logo or trademark artwork is bundled.** The launcher icon and the in-app `BrandMark` are an original coupon-tag glyph that only uses the colours.

## 5. Android platform facts relied on

- `WebViewCompat.addWebMessageListener` (androidx.webkit, feature `WEB_MESSAGE_LISTENER`) injects a JS object only into frames whose origin matches an allow-list. That makes it safer than `addJavascriptInterface`, which exposes the object to every origin loaded in the WebView.
- `WebViewAssetLoader` serves bundled assets at `https://appassets.androidplatform.net/assets/…`. The Modara sample store therefore runs on a real HTTPS origin, with no `file://` access.
- `WebViewClient.onRenderProcessGone` must return `true` and the WebView must be discarded. Otherwise the app process is killed with the renderer.
- In-app floating UI needs no `SYSTEM_ALERT_WINDOW` permission. The assistant bubble is a Compose overlay inside the app's own window.

## 6. Test device

| Property | Value |
|---|---|
| Model | CPH2717 (OnePlus/OPPO family) |
| OS | Android 16 (API 36) |
| ABI | arm64-v8a |
| Notes | A personal phone, connected over USB with ADB. The owner was using it during part of the work, so some measurements were spread out (see `docs/PERFORMANCE.md`). |

## 7. Toolchain choice

The first combination that built cleanly on this machine, and was then pinned:

| Component | Version | Why |
|---|---|---|
| Android Gradle Plugin | 9.2.1 | Current stable line. Built-in Kotlin support, so no separate `kotlin-android` plugin. |
| Gradle | 9.4.1 (wrapper) | Required by AGP 9.2. Wrapper copied from another local project with the same AGP line. |
| Kotlin | 2.2.10 (+ Compose compiler plugin) | Matches AGP's built-in Kotlin. |
| JDK | 17 target (Android Studio JBR used to run Gradle) | |
| compileSdk / targetSdk | 36 | Matches the test device. |
| minSdk | 26 | Adaptive icons without a fallback, `java.time`, and broad device coverage. |
| Compose BOM | 2026.02.01 | Material 3 `ModalBottomSheet`, `LocalClipboard`. |

Lint reports newer versions for several dependencies (warnings only). They were left pinned to keep the build reproducible during the assignment.
