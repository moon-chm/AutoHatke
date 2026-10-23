# Testing

## Commands

Run from the project root on Windows; use `./gradlew` on macOS or Linux.

```bat
gradlew.bat testDebugUnitTest                 :: JVM unit tests
gradlew.bat lintDebug                         :: Android lint (abortOnError = true)
gradlew.bat assembleDebug assembleDebugAndroidTest
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w -r io.github.autocoupons.demo.test/androidx.test.runner.AndroidJUnitRunner
```

`gradlew.bat connectedDebugAndroidTest` works too. The `am instrument` form was used here so the run could be repeated without reinstalling.

## Results (2026-10-24, after the UI redesign)

| Suite | Result | Notes |
|---|---|---|
| Unit tests (`testDebugUnitTest --rerun-tasks`) | **69 passed, 0 failed, 0 skipped**; 11 classes | |
| Lint (`lintDebug`) | **0 errors**, 17 warnings | All warnings are dependency/SDK notices (`AndroidGradlePluginVersion`, `GradleDependency`, `NewerVersionAvailable`, `OldTargetApi`, `ObsoleteSdkInt` for `mipmap-anydpi-v26`; that folder is kept because moving it broke AAPT resource linking) |
| Instrumented, Pixel 7 emulator (AVD `ac_pixel`, Android 16, x86_64) | **OK (14 tests)**, 67.7 s | The first run after the redesign failed 2 `AuthFlowUiTest` cases that still waited for the old "Choose a store" heading; they now wait for the `home_screen` tag. The accessibility test first failed on the bottom-sheet drag handle (32 dp wide); the handle is now 48 dp. All 14 pass on the final build |

The pre-redesign build also passed the 11 instrumented tests it had at the time on a physical device (CPH2717, Android 16). The redesigned build has not been run on that device.

## Unit test inventory

| Class | Tests | Covers |
|---|---|---|
| `CouponApplicationCoordinatorTest` | 15 | Happy path (STYLE250); monotonic progress; best-so-far; baseline before every test; no-savings outcome; the shopper's own code removed and restored; fetch error; empty list; duplicate codes; timeout → FAILED and continue; inconsistent totals → UNVERIFIED; cart edited → abort; cleanup refused → abort; re-apply mismatch → not confirmed; cancel removes the half-tested coupon |
| `CouponRankingEngineTest` | 7 | ₹250 beats ₹100; non-verified never wins; no winner; deterministic tie-break; currency mismatch excluded; zero, negative, or impossible savings excluded; savings inconsistent with the total excluded |
| `MoneyTest` | 3 | Indian grouping; minor-unit arithmetic; cross-currency arithmetic rejected |
| `CouponStateMachineTest` | 4 | Allowed and forbidden transitions; no jump to verified; cancel marks only unfinished |
| `AdvertisedDiscountEstimatorTest` | 6 | Flat, flat capped at subtotal, percent, percent with cap, minimum spend, unknown |
| `CouponResponseParserTest` | 10 | Bare array, wrapper key, keyed object, missing fields, invalid codes skipped, duplicates, unknown type, empty, **real 404 HTML**, malformed |
| `LegacyBuyhatkeCouponProviderTest` | 8 | Path + `pos`; parse success labelled LIVE; 404 → `Http(404)`; 200 HTML → Malformed; timeout; dropped connection; unknown retailer makes no request; repository never mixes sources |
| `AuthValidatorTest` | 5 | Email, login password, registration password + strength, confirmation, name |
| `AuthViewModelTest` | 6 | Invalid login; valid login discards the password; guest access; visibility toggle; mismatched registration; successful registration clears passwords |
| `NavigationPolicyTest` | 3 | Allowlist + subdomains; look-alike hosts; non-https and userinfo blocked |
| `CheckoutDetectorTest` | 2 | Demo bag vs product page; Myntra cart path |

`testing/Fakes.kt` provides `FakeCheckout`, which implements Modara's coupon rules plus fault hooks: hanging, apply-then-hang, inconsistent totals, a cart edit before apply, a discount override on re-apply, and a refused removal.

## Instrumented tests

These use a real WebView, the real Modara store assets, the real `DemoStoreCheckoutAdapter`, and the real coordinator.

| Test | Asserts |
|---|---|
| `cartPageExposesCouponEntryAndDefaultBag` | Coupon field detected; subtotal ₹3,298 |
| `happyPathSelectsStyle250ReappliesAndVerifiesTotal` | Winner STYLE250, `WinnerVerified`, page total ₹3,048 with STYLE250 applied |
| `biggerBagMakesMega500TheWinner` | With a ₹1,899 bag added (subtotal ₹5,197): MEGA500 wins, total ₹4,697 |
| `repeatedSessionsAreStableAndDeterministic` | Three sessions in a row give an identical outcome |
| `cancellingMidSessionRestoresTheBag` | Cancel → no coupon applied, total back to baseline |
| `navigatingAwayFailsTheSessionInsteadOfHanging` | Navigation mid-test → `CheckoutUnavailable` error, no hang |
| `AuthFlowUiTest` (5) | Empty login errors; invalid email; valid login → Home; registration validates and succeeds; Register → Login link |
| `AccessibilityChecksTest.nativeScreensPassAccessibilityChecks` | Google's Accessibility Test Framework (via `ui-test-junit4-accessibility`) finds no issues on Login, Create account, Home, Savings, Profile, and an info sheet: touch-target size, text contrast, missing labels, duplicate descriptions, and more |
| `AccessibilityChecksTest.browserAndCouponResultsPassAccessibilityChecks` | The same checks on the browser (bag page with the bubble) and on the coupon results sheet |
| `RendererCrashTest.rendererCrashMidSessionShowsErrorAndRecovers` | Loads `chrome://crash` while a coupon session is running: the app survives, shows "The page stopped responding", and **Try again** creates a fresh WebView that completes a full verified session |

## Modara expected outcomes (default bag: Cloudstep Running Shoes ₹2,499 + Essential Cotton Tee ₹799 = ₹3,298)

| Code | Rule | Expected result |
|---|---|---|
| FLAT100 | ₹100 off, min ₹999 | Works, saves ₹100 |
| SAVE10 | 10 %, max ₹200 | Works, saves ₹200 |
| **STYLE250** | ₹250 off, min ₹2,500 | **Works, saves ₹250, winner** → ₹3,048 |
| MEGA500 | ₹500 off, min ₹5,000 | Didn't apply (minimum not met) |
| SHOES15 | 15 % on footwear, max ₹150 | Works, saves ₹150 |
| FESTIVE50 | Expired 31 Mar 2026 | Didn't apply (expired) |
| WELCOME75 | Not known to the store | Didn't apply ("not valid") |

With two shoes and two tees (₹6,596), MEGA500 wins at ₹6,096; that run is the one in the screenshots.

## Manual checks on the emulator (redesigned UI)

- Login and Register render with the gradient hero; **Continue as guest** opens Home (screenshots 01–03).
- Home → Modara → bag → the bubble shows the one-time tip "Tap to apply the best coupon"; the bottom-bar **Coupons** button is hidden while the bubble is visible (04–05).
- Progress shows "X of Y", the current code, best so far, and **Stop** inside the progress card, so it stays on screen (06).
- Results show "You saved ₹…", the old and new totals, ticket cards with **BEST** on the winner, and *Didn't apply* (07). The bag page shows the winning code applied (08).
- Savings lists the session; Profile and its three info sheets open (09–11). About states the project is not affiliated with Buyhatke.
- Myntra loads live in the browser; the sheet shows "Coupons aren't available right now … (error 404)" with **Try again** (12). The location PIN Myntra showed on the page is blurred in that screenshot.
- **Dark mode** (`adb shell cmd uimode night yes`): Login, Home, Profile, progress, and results all render correctly. Two issues were found and fixed: the dark store shelf blended into the dark background (it now has a faint outline), and a seam showed under the status bar on the gradient tabs (also in light mode; the scrim now only appears once the list scrolls).
- **Small screen and large text** (`wm size 720x1280`, `wm density 320`, `font_scale 1.3`, i.e. a 360 dp-wide phone at 130 % text): Login, Home, bag, progress, and results all fit and scroll. Two issues were found and fixed: hero text ran under the ticket art (the text now reserves the art's width instead of using a fixed width), and the AUTO-APPLY badge wrapped onto two lines over the store logo (badges and tile names now stay on one line).
- **Live API re-checked** with `curl`: `pos=1` and `pos=2` still return HTTP 404.

**Not done:**

- A spoken TalkBack walkthrough. TalkBack is installed on the emulator, but its text-to-speech engine was not initialised, so nothing could be heard or logged. The automated accessibility checks above cover labels, touch targets, and contrast, but not reading order or how announcements sound.
- Tests on a second physical device or on a tablet.
