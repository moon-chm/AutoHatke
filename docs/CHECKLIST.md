# Implementation checklist

Status as of 2026-10-24.

| Mark | Meaning |
|---|---|
| [x] | Done, with evidence in `docs/TESTING.md` |
| [~] | Partial (note says why) |
| [ ] | Not done |

## Phase 0–1: Workspace, research, feasibility
- [x] Inspected the empty workspace and the local Android SDK and JDK
- [x] Probed the legacy API (both `pos` values, http and https): 404 recorded with date and commands
- [x] Probed Jabong (301 → Myntra) and Myntra (cart 200, login + Akamai)
- [x] Extracted brand colours from the official logo, favicon, and site styles
- [x] `docs/RESEARCH.md`, `docs/FEASIBILITY.md`

## Phase 2–3: Requirements, architecture, design
- [x] `docs/REQUIREMENTS.md`, `docs/ARCHITECTURE.md`, `docs/UI_DESIGN.md`
- [x] Design tokens (colour, type, shape, spacing), light and dark themes (dark checked on the emulator)
- [x] Automated accessibility checks (ATF) on all native screens, browser, and results
- [x] Small screen + 130 % text check
- [x] Renderer-crash recovery test (`chrome://crash`)
- [ ] Spoken TalkBack walkthrough
- [ ] Redesigned build on a physical phone
- [x] Contrast check of brand text/container pairs (SuccessText added)

## Phase 4: Project setup
- [x] Gradle wrapper 9.4.1, AGP 9.2.1, Kotlin 2.2.10, version catalog
- [x] Manifest hardening (backup off, network security config, metrics opt-out)
- [x] `profiling` build type for release-like measurements

## Phase 5: Authentication
- [x] Login screen with validation, password toggle, demo-mode banner
- [x] Registration screen with live validation and a strength meter
- [x] "Continue as guest" (deterministic)
- [x] Passwords never sent, logged, or stored; cleared from state after completion

## Phase 6: Domain
- [x] `Money` (minor units, Indian formatting)
- [x] Coupon model, sources, per-coupon state machine
- [x] Deterministic ranking engine (verified savings only)
- [x] Coordinator: fetch → dedupe → baseline → test/cleanup → rank → re-apply → verify
- [x] Cancel and failure restore (`cartModified` + `NonCancellable`)

## Phase 7: Data
- [x] Legacy API provider (HTTPS, timeouts, 1 MB cap, cancellation)
- [x] Tolerant parser that rejects HTML
- [x] Demo provider, kept separate from live data
- [~] Live coupons: implemented, but the real endpoint returns 404, so no live coupon has ever been parsed from production

## Phase 8: Browser, checkout, assistant
- [x] Secure WebView, navigation policy, origin-restricted message bridge
- [x] Modara sample store (products, bag, deterministic coupons; Place order only shows a "sample store" note)
- [x] Checkout detection (Modara, Myntra)
- [x] Floating assistant bubble with progress ring
- [x] Coupon sheet: progress, results grouped by outcome, assisted copy list
- [x] Myntra user-assisted mode
- [~] Renderer-crash recovery: implemented, not crash-tested on device

## Phase 9: Testing
- [x] 69 JVM unit tests passing
- [x] 11 instrumented tests passing on device
- [~] `AuthFlowUiTest` changed at 01:14 to assert on the bottom-bar CTA and has not been re-run on device since
- [x] Lint: 0 errors

## Phase 10: Performance and security
- [x] Cold start, PSS, frame pacing, wakelocks measured (`docs/PERFORMANCE.md`)
- [ ] Per-app battery measurement (batterystats was not attributable)
- [ ] Macrobenchmark / Baseline Profile
- [x] `docs/SECURITY.md`

## Phase 11: Delivery
- [x] `README.md` with libraries, setup, and demo login
- [x] Debug APK built: `app/build/outputs/apk/debug/app-debug.apk`
- [x] Screenshots in `docs/screenshots/` (12, redesigned UI, emulator)
- [ ] Registration screenshot (device became unavailable)
- [ ] No commit or push (not requested)
