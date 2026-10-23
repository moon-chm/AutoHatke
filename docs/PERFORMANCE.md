# Performance

Measured on the test device (CPH2717, Android 16, arm64) on 2026-10-24, using `adb` tooling. The device was a personal phone with other apps installed, and its owner used it in between runs. Treat these numbers as indicative single-device samples, not benchmarks.

**These measurements predate the UI redesign** (bundled font, gradients, Canvas illustrations, animations). They have not been repeated on the redesigned build, and the APK sizes below are from before the 176 KB font was added.

## Builds measured

| Build | How | APK size |
|---|---|---|
| `debug` | `assembleDebug` (debuggable, no R8) | 13.2 MB (13,231,104 bytes) |
| `profiling` | `assembleProfiling`: `initWith(release)`, so R8 minify + resource shrinking, not debuggable, signed with the debug key, id suffix `.profiling` | 1.69 MB (1,686,718 bytes) |

Debug builds run Compose without R8 optimisation and with debugging hooks. Use the `profiling` numbers to judge real-world behaviour.

## Results

| Metric | Command | Debug | Profiling |
|---|---|---|---|
| Cold start (`TotalTime`) | `adb shell am force-stop <pkg>` then `adb shell am start -W -n <pkg>/io.github.autocoupons.MainActivity` | 904–1013 ms | 742, 416, 262, 286, 260 ms (the first run includes compiling the APK) |
| Memory at Home (PSS) | `adb shell dumpsys meminfo <pkg>` | ~138 MB | 95.8 MB |
| Memory in browser after a Modara sample store session | same | 244 MB | 166.7 MB |
| Memory after closing the browser | same | — | ~162 MB, `WebViews: 0`. Chromium's native libraries stay mapped in the process once loaded; the WebView instance itself was released. |
| Frame pacing | `adb shell dumpsys gfxinfo <pkg> reset`, run the flow, then `dumpsys gfxinfo <pkg>` | 21.48 % janky; p50 9 ms, p90 65 ms, p95 101 ms, p99 150 ms (included a Myntra page load) | 9.92 % janky; p50 8 ms, p90 26 ms, p95 48 ms (Modara sample store flow only) |
| Coupon session duration (7 coupons, happy path) | Instrumented test timing | 5.1–5.6 s over 3 runs | — |
| Wakelocks | `adb shell dumpsys power` during a session | No app-held wakelocks; only system `*launch*` entries, plus a transient AudioMix while Myntra played media | — |

Session time is dominated by the Modara sample store's deliberate 450 ms simulated latency per apply or remove (7 coupons × (apply + remove) + baseline + re-apply).

## Design choices that affect performance

- **No polling.** The checkout adapter waits for DOM changes with a `MutationObserver` keyed on `data-seq`, not with timed sleeps.
- **One WebView** per browser screen. It is destroyed in `DisposableEffect`, kept alive across configuration changes, and never recreated on recomposition.
- **OkHttp:**
  - one shared client;
  - bodies capped at 1 MB via `peekBody`;
  - calls cancelled when the coroutine is cancelled;
  - `retryOnConnectionFailure(false)`, so a dead API fails fast.
- `LazyColumn` with stable keys (coupon code) for result lists.
- Release builds use R8 and resource shrinking (13.2 MB → 1.69 MB).

## Not measured

- **Per-app battery drain.** `dumpsys batterystats` on this device reported device-wide figures that could not be attributed reliably, so they were discarded.
- **Jetpack Macrobenchmark / Baseline Profiles.** No benchmark module was added. A Baseline Profile would probably reduce first-launch time further.
- **Lower-end hardware.** Not tested on any other device or emulator.
