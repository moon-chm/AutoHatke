# Architecture

Single module (`:app`), single Activity, Jetpack Compose UI, MVVM with unidirectional data flow. Dependencies are wired by hand in `AppContainer`; there is no DI framework, since the graph is small.

```
io.github.autocoupons
├── AutoCouponsApplication / AppContainer   manual DI (OkHttp, repository, coordinator, stores)
├── MainActivity / AppNavHost               routes: auth/login, auth/register, home, browser/{retailerId}
├── core
│   ├── designsystem                        tokens (Color/Type/Theme), buttons, text field, badges, icons
│   └── web                                 SecureWebView, NavigationPolicy, WebPageBridge
├── domain/coupon                           pure Kotlin: Money, Coupon, state machine, ranking, coordinator
├── data
│   ├── coupon                              Legacy API provider, Demo provider, parser, repository
│   └── checkout                            RetailerRegistry, detectors, DemoStoreCheckoutAdapter
└── feature
    ├── auth                                Login / Register screens + ViewModels, DemoSessionStore
    ├── home                                Home screen, live-API check, last session
    ├── browser                             Browser screen, FloatingAssistant, CouponSheet, BrowserViewModel
    └── coupons                             shared coupon cards and user-facing messages
```

`domain` depends on nothing Android-specific. That is why the coordinator and ranking engine are covered by fast JVM unit tests using `FakeCheckout`.

## Key interfaces

```kotlin
interface CouponProvider   { suspend fun fetch(retailerId: String): CouponFetchResult }
interface CouponRepository { suspend fun coupons(retailerId: String, source: CouponSource): CouponFetchResult }
interface CheckoutAdapter  { suspend fun readCart(): CartSnapshot
                             suspend fun applyCoupon(code: String): ApplyObservation
                             suspend fun removeCoupon(): CartSnapshot }
fun interface CheckoutDetector { fun detect(url: String): CheckoutDetection }
interface CouponRankingEngine  { fun rank(baseline: CartSnapshot, results: List<CouponTestResult>): RankingDecision }
```

## Coupon session pipeline

`CouponApplicationCoordinator.run(retailerId, source, adapter): Flow<CouponSessionState>`

```
FETCHING ──► fetch coupons (one source only) ──► dedupe codes case-insensitively
   │
READING_CART ──► readCart() ──► if the shopper already has a code: remember it as originalCode, remove it
   │             (baseline = cart with no coupon)
TESTING ──► for each coupon:
   │          ├ readCart(); fingerprint ≠ baseline → abort "CartChanged"
   │          ├ applyCoupon(code)  (12 s timeout)  → Accepted / Rejected / TimedOut
   │          ├ Accepted + savings consistent → APPLIED_AND_VERIFIED, else UNVERIFIED
   │          └ removeCoupon(); total ≠ baseline → abort "CleanupFailed"
RANKING ──► DefaultCouponRankingEngine (verified only; savings desc, total asc, source order, code)
   │
APPLYING_BEST ──► apply winner again ──► VERIFYING: observed saving == ranked saving?
   │                                      yes → WinnerVerified   no → WinnerNotConfirmed
DONE / NoVerifiedSavings (originalCode restored if there was one)

finally { if (cartModified) withContext(NonCancellable) { restoreBestEffort() } }
```

### Per-coupon state machine

`CouponTestStatus`: DISCOVERED → TESTING → {APPLIED_AND_VERIFIED, REJECTED, FAILED, UNVERIFIED}, and DISCOVERED/TESTING → CANCELLED. Other transitions throw `IllegalCouponTransition`; this is covered by `CouponStateMachineTest`.

## WebView layer

- `SecureWebView.create()` configures one WebView:
  - JavaScript and DOM storage on;
  - file and content access off;
  - Safe Browsing on;
  - mixed content never allowed;
  - no multiple windows, no geolocation, downloads blocked.
- Its `WebViewClient`:
  - serves `/assets/` through `WebViewAssetLoader`;
  - checks every main-frame navigation with `NavigationPolicy` (https + host allowlist). Anything else becomes an "Open in browser" snackbar;
  - cancels SSL errors;
  - reports renderer death to the ViewModel.
- `WebPageBridge`:
  - Uses AndroidX `addWebMessageListener`, restricted to the Modara sample store origin.
  - Kotlin injects a script via `evaluateJavascript`; the page replies through `AutoCouponsBridge.postMessage({id, …})`.
  - Pending calls fail immediately on navigation or detach (`CheckoutUnavailableException`), so a session never hangs on a page that has gone.
- `DemoStoreCheckoutAdapter` maps the Modara sample store DOM contract to `CheckoutAdapter`:
  - Inputs: `#coupon-input`, `#coupon-apply`, `#coupon-remove`.
  - Results: `#coupon-message[data-state][data-seq]`, and the summary totals via `[data-minor]`.
  - It waits for results with a `MutationObserver` instead of sleeping.

## State and lifecycle

- ViewModels expose `StateFlow<…UiState>` (immutable data classes) and are created with `viewModelFactory` + `CreationExtras.appContainer`.
- `BrowserViewModel` owns the session `Job`.
  - Cancel does `cancelAndJoin()` with a `restoring` flag, so the UI shows "Restoring your bag…" until the cleanup finishes.
  - The last URL is kept in the `SavedStateHandle`.
- The activity declares `configChanges` for orientation, size, and uiMode, so the WebView (and a running session) survive rotation and theme switches. The WebView is destroyed in a `DisposableEffect` when the browser route leaves composition.
- The demo session and last-session summary are in memory only (`DemoSessionStore`, `LastSessionStore`). Nothing is written to disk.
