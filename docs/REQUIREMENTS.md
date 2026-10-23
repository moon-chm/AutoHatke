# Requirements

Status key:

| Status | Meaning |
|---|---|
| ✅ Implemented and covered by a test | Has an automated test |
| ☑️ Implemented, checked manually | Checked manually on the test device |
| ⚠️ Partial or limited | See the note |

## Functional

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| F1 | Login screen with email and password validation | ✅ | `AuthValidatorTest`, `AuthViewModelTest`, `AuthFlowUiTest` |
| F2 | Registration screen (name, email, password, confirm, strength) | ✅ | `AuthFlowUiTest.registrationValidatesAndSucceeds` |
| F3 | Deterministic guest access ("Continue as guest") | ✅ | `AuthViewModelTest.guest access needs no input` |
| F4 | Passwords never transmitted, logged, or stored | ✅ | No network or persistence in the auth code path. The password is cleared from state after submit (`valid login … discards the password`). |
| F5 | Home screen: store choice, link paste, how it works, savings; coupon feed status and About in Profile | ☑️ | `docs/screenshots/03_home.png`, `10_profile.png`, `11_about.png` |
| F6 | In-app shopping browser (WebView) with back, forward, reload, and close | ☑️ | Screenshots 03, 04, 08 |
| F7 | Checkout detection per retailer | ✅ | `CheckoutDetectorTest`; `DemoStoreEndToEndTest.cartPageExposesCouponEntryAndDefaultBag` |
| F8 | Floating assistant bubble (no overlay permission) showing progress | ☑️ | Screenshot 05 |
| F9 | Fetch coupons from the live legacy API (`pos=1`, `pos=2`) | ⚠️ | Implemented and tested against MockWebServer (`LegacyBuyhatkeCouponProviderTest`). The real endpoint returns **404** (see RESEARCH §2). |
| F10 | Bundled coupon provider for Modara, never mixed with live data | ✅ | `repository never mixes sources`; each store has exactly one source (Modara: bundled list; Myntra: live API) |
| F11 | Baseline cart, test each coupon, clean up between tests | ✅ | `cart is back at baseline before every test` |
| F12 | Deterministic ranking by **verified** savings | ✅ | `CouponRankingEngineTest` (7 tests) |
| F13 | Re-apply the winner and verify the final total | ✅ | Unit tests plus the on-device test `happyPathSelectsDemo250ReappliesAndVerifiesTotal` |
| F14 | Happy path: STYLE250 (₹250) beats FLAT100 (₹100) | ✅ | Final total ₹3,048 (`happyPathSelectsStyle250ReappliesAndVerifiesTotal`, emulator) |
| F15 | Progress UI ("X of Y", current code, best so far, Stop) | ✅/☑️ | `progress counts only finished tests…`; screenshot 05 |
| F16 | Results UI grouped by outcome, with honest failure reasons | ☑️ | Screenshot 06 |
| F17 | Cancel restores the bag | ✅ | `cancelling mid-test removes the half-tested coupon`; on-device `cancellingMidSessionRestoresTheBag` |
| F18 | Myntra user-assisted mode (copy codes, no automation) | ☑️ | Screenshot 08 |

## Non-functional

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| N1 | Never click Place Order / Pay / Buy | ✅ | The adapter script only touches `#coupon-*`. `#place-order` is disabled. |
| N2 | TLS errors are never bypassed | ✅ (code review) | `onReceivedSslError` always calls `handler.cancel()`. No custom TrustManager. |
| N3 | No cleartext traffic | ✅ | `network_security_config.xml` |
| N4 | Light and dark themes | ☑️ | |
| N5 | Survives rotation and dark-mode switch without reloading the WebView | ☑️ | `configChanges` in the manifest |
| N6 | Renderer crash does not crash the app | ⚠️ | Handled in code (`onRenderProcessGone` → error overlay + Retry). Not crash-tested on device. |
| N7 | Accessibility: 48 dp touch targets, content descriptions, radio semantics | ☑️ | Not audited with Accessibility Scanner |
| N8 | Lint has zero errors | ✅ | `lintDebug`: 0 errors, 17 warnings |
| N9 | Cold start under 1 s (release-like build) | ☑️ | 260–742 ms (`docs/PERFORMANCE.md`) |
