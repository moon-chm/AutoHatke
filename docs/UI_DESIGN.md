# UI design

## Principles

1. **Feels like a shopping app, not a test harness.** The look follows the Buyhatke app's patterns (gradient hero, search pill, dark store shelf with tile badges, floating pill navigation) without copying its artwork. Developer wording ("demo", "fixture", "live API") is kept out of the UI; the honest disclosure lives in **Profile → About AutoCoupons**.
2. **Honest numbers.** A coupon is shown under *Coupons that work on this bag* only when the store's own total dropped by that amount. Failures say why: not accepted, minimum not met, expired, couldn't check, or coupons unavailable (with the HTTP code).
3. **One primary action per screen**, where the thumb is: the coupon bubble on the bag page, Done on the results.
4. **The shopper is in control.** **Stop** sits inside the progress card, so it is always on screen during a session, and the bag is restored on cancel.

## Colour tokens

Defined in `core/designsystem/Color.kt` (`BrandColors`) and mapped in `Theme.kt`. Sources are in `docs/RESEARCH.md` §4.

| Token | Light | Source | Use |
|---|---|---|---|
| Indigo (primary) | `#4F46E5` | Logo SVG fill | Buttons, links, bubble |
| IndigoDeep / Darkest | `#3932BF` / `#302B9E` | Logo shades | Pressed, on-container text |
| Highlight | `#FFE372` | Logo accent | Dot in the brand mark, done badge, BEST badge |
| IndigoTint / Soft / Light | `#EFEFFF` / `#D0CFFF` / `#918FFF` | Site inline styles | Containers; primary colour in dark theme |
| Background | `#F1F5F9` | Site | App background |
| Surface | `#FFFFFF` | Site | Cards, sheet |
| TextPrimary / Secondary / Tertiary | `#020618` / `#45556C` / `#62748E` | Site | Text hierarchy |
| Border | `#CAD5E2` | Site | Outlines |
| Success / container | `#008357` / `#E8F8F0` | Site | Verified savings |
| Warning / container | `#FF6D1F` / `#FFF8E7` | Site | Warnings, the copy-codes tip |
| WarningText | `#A2440B` | Derived: darkened Warning, 5.9:1 on its container | Banner text |
| SuccessText | `#00754D` | Derived: the site green measured 4.36:1 on its container, below AA; this reaches 5.2:1 | Verified-savings text and pills |
| Error / container | `#C62828` / `#FDECEC` | Derived (not on the site) | Errors, Sign out |
| Violet / Pink | `#7C3AED` / `#EC4899` | Added for the redesign | Gradient ends (hero, buttons, bubble, progress) |
| Coin / CoinDeep | `#FFC83D` / `#F59E0B` | Added for the redesign | ₹ coins in illustrations |
| Shelf / ShelfHigh | `#1C1E2B` / `#2A2D3E` | Added for the redesign | Dark "Shop & save" card and its tiles |
| Mint / Emerald | `#34D399` / `#10B981` | Added for the redesign | Savings gradient, AUTO-APPLY badge |

Gradients (`core/designsystem/Brushes.kt`, `AcBrushes`): **Hero** IndigoDeep → Indigo → Violet; **Primary** (buttons) Indigo → Violet; **Bubble** and **Progress** Indigo → Violet → Pink; **Success** SuccessText → Emerald.

A dark theme exists (deep navy background, IndigoLight as primary), but the redesign was only checked visually in light mode.

## Type, shape, spacing

- Typeface: **Plus Jakarta Sans** (variable font bundled at `res/font/plus_jakarta_sans.ttf`, SIL Open Font License 1.1), with weights set through `FontVariation`. `AcTypography` maps it onto the Material 3 scale with heavy headlines (e.g. headlineLarge 30/36 ExtraBold). Coupon codes use a monospace bold style with 1 sp tracking (`CouponCodeStyle`).
- Shapes: 8 / 12 / 16 / 20 / 28 dp (`AcShapes`); buttons and the navigation bar are full pills; working-coupon cards use a notched `TicketShape`.
- Spacing: 4 / 8 / 12 / 16 / 24 / 32 dp (`Spacing`), 20 dp screen margins.
- Sizes: buttons 54 dp tall; touch targets at least 48 dp; assistant bubble 62 dp.
- Illustrations are drawn in code (`Illustrations.kt`): floating coupon tickets with ₹ coins and sparkles, confetti (kept to the outer 20 % on each side so it never sits under text), and a shopping bag. There are no image assets.
- Status-bar icons switch between light and dark per screen (`SystemBarIcons`); a status-bar scrim stops scrolled content running under the clock.

## Screens

| # | Screen | Screenshot | Notes |
|---|---|---|---|
| 1 | Login | `screenshots/01_login.png` | Gradient hero with ticket art; sheet with email, password (visibility toggle), Sign in, **Continue as guest**, Create account |
| 2 | Create account | `screenshots/02_register.png` | Live validation after the first submit, strength meter, confirm match |
| 3 | Home | `screenshots/03_home.png` | Greeting, "Shop smart. Pay less." hero, search pill (store name or pasted link, paste button), dark *Shop & save* card (Modara AUTO-APPLY, Myntra CODES, More soon), savings strip, How it works carousel, floating pill navigation (Home / Savings / Profile) |
| 4 | Store (Modara) | `screenshots/04_store.png` | Browser top bar: close, store logo, name, lock with "Secure checkout" or the host. Bottom bar: back, forward, reload, **Coupons** |
| 5 | Bag | `screenshots/05_bag.png` | The bubble appears on the bag page with a one-time tip; the bottom-bar Coupons button hides while the bubble is visible |
| 6 | Trying coupons | `screenshots/06_progress.png` | "Trying coupons X of Y", gradient bar, "Applying CODE…", last result, best so far, Stop; a status row per code; progress ring on the bubble |
| 7 | Results | `screenshots/07_results.png` | Green "You saved ₹…" card with old and new totals and the applied code, ticket cards (BEST on the winner), *Didn't apply*, *Couldn't check*, Done / Check again |
| 8 | Winner applied | `screenshots/08_bag_applied.png` | The store's own UI shows the code applied |
| 9 | Savings | `screenshots/09_savings.png` | Total saved and a row per coupon check (store, time, coupons tried, code, amount); empty state with a bag illustration |
| 10 | Profile | `screenshots/10_profile.png` | Coupon feed status, How it works, Privacy & security, About, Sign out |
| 11 | About sheet | `screenshots/11_about.png` | States the project is independent and not affiliated with Buyhatke, and that Modara is a sample store |
| 12 | Myntra (codes to copy) | `screenshots/12_myntra_assisted.png` | Live API down: "Coupons aren't available right now … (error 404)", Try again. When codes load, each has a COPY pill and a tip explains pasting them |

## Floating assistant

- A gradient bubble on the right edge, draggable vertically. It starts at 18 % of the height so it does not cover the bag total, springs in, and pulses while idle.
- The first time, a tip reads "Tap to apply the best coupon".
- While testing it shows a progress ring with "done/total"; when done, a yellow check badge. Tapping it opens the coupon sheet.
- It is not shown on product pages; there, the bottom-bar **Coupons** button opens a "Head to your bag" prompt instead.

## Accessibility

- Text fields have labels and error text announced via `supportingText`.
- The bottom navigation uses `selectableGroup` + `Role.Tab`.
- Icon buttons have content descriptions.
- The assistant bubble is a semantic button whose description includes progress ("AutoCoupons: trying coupons, 3 of 7. Open progress").
- The "Applying CODE…" line is a polite live region.
- Contrast was checked with the WCAG formula for the brand text/container pairs (all ≥ 4.5:1 after the SuccessText fix). There has been no full audit with Accessibility Scanner or TalkBack.
