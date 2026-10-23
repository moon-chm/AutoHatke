# Security and privacy

## Data handled

| Data | Where it goes | Kept? |
|---|---|---|
| Email / name typed at login or registration | ViewModel state, then the in-memory `DemoSessionStore` (display name only) | Memory only; gone when the process dies |
| Password | ViewModel state while the field is filled. **Cleared from state** when the simulated sign-in or registration completes (about 0.5 s after submit). | Never stored, logged, or sent |
| Coupon codes and session results | Memory (`LastSessionStore`) | Memory only |
| Web content and cookies (WebView) | Android WebView's own profile storage | Managed by WebView |

The app has no backend, no analytics, and no crash reporting. `android:allowBackup="false"` and `data_extraction_rules.xml` exclude app data from cloud backup and device transfer. `WebView.MetricsOptOut` is set.

## Network

- The only permission is `INTERNET`.
- `network_security_config.xml` sets `cleartextTrafficPermitted="false"` for every domain. The legacy API is called over **HTTPS**.
- There is **no custom TrustManager, HostnameVerifier, or certificate pinning override.** Platform TLS validation applies everywhere.
- In WebView, `onReceivedSslError` always calls `handler.cancel()`. TLS errors are never bypassed.
- The only outbound requests made by app code are to `coupons.buyhatke.com` (coupon fetch and the Home "Check live API" button). Pages the user browses are loaded by WebView.

## WebView hardening (`core/web/SecureWebView.kt`)

| Control | Setting |
|---|---|
| File and content access | `allowFileAccess = false`, `allowContentAccess = false`. The Modara sample store is served by `WebViewAssetLoader` over `https://appassets.androidplatform.net`. |
| Mixed content | `MIXED_CONTENT_NEVER_ALLOW` |
| Safe Browsing | Enabled |
| Popups / multiple windows | Disabled |
| Geolocation | Disabled |
| Downloads | Blocked |
| Navigation | Main-frame navigation only to `https` URLs on the retailer's host allowlist (exact host or a true subdomain; look-alikes such as `myntra.com.evil.io` are rejected). URLs with user-info are blocked. Other links go to an "Open in browser" prompt. |
| JS ↔ native | **No `@JavascriptInterface`.** AndroidX `addWebMessageListener` is restricted to the Modara sample store origin. The bridge additionally checks the message's origin and `isMainFrame`, and checks the WebView's current origin before every call. Messages are parsed as JSON and matched by a random request id. |
| Renderer crash | `onRenderProcessGone` returns `true`; the WebView is discarded and an error with Retry is shown. |

## Checkout safety rules

- The adapter script touches only the coupon input, apply, and remove controls. It **never** clicks Place Order, Pay, or Buy. Modara's Place-order button places nothing (it only shows a "sample store" note), and the script does not reference it.
- On Myntra (user-assisted), the app injects **no** scripts and performs no clicks. The shopper copies a code and applies it themselves.
- No CAPTCHA, authentication, anti-bot, or payment control is bypassed or automated.
- After a session the bag is left either with the verified best coupon or exactly as the shopper had it (their own code restored).

## Honesty controls

- Coupon results carry a source (`LIVE_API` or `DEMO_FIXTURE`), shown as a badge. The repository routes each request to exactly one provider; this is tested.
- API failures (404, timeout, network, malformed or HTML body) are shown as such. There is no fallback to demo data.
- The app and docs state that this is an independent demo, not an official Buyhatke product. No Buyhatke logo artwork is bundled.

## Known limitations

- The WebView's own cookie and cache storage is not cleared on sign-out. Sign-out ends the demo session only.
- No root or tamper detection; not needed for a demo with no secrets.
- The Modara sample store origin is trusted by design. Anyone who can modify the APK's assets could change its behaviour, which is outside the threat model.
