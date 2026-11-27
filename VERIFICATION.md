# BaroWeather verification record

Date: 2026-11-27

## Host checks

| Check | Result |
| --- | --- |
| `cargo test --offline` (core) | 15 passed, 0 failed |
| Rust static libraries | arm64, armv7, x86_64 built with NDK 27.2.12479018 |
| JNI `.so` files | 3 ABIs linked with NDK clang, no errors |
| `aapt2 compile` / `link` | Passed, min SDK 21, target SDK 35 |
| `javac` / `d8` | Passed, single `classes.dex` |
| APK contents | `classes.dex` plus `lib/{arm64-v8a,armeabi-v7a,x86_64}/libbaroweather_core.so` |
| `apksigner verify` | v1 true, v2 true, v3 true |
| Build/version | v0.4.0, versionCode 3, output `dist\android\baroweather-0.4.0.apk` |
| APK size | 4,889,589 bytes (v0.3 was 4,864,648 bytes) |

## Device checks (Lenovo TB-X606F / `HPV2VPC2`, Android 10, 1200×1920)

| Check | Result |
| --- | --- |
| `adb install -r` | Success |
| Launch via `monkey` | `MainActivity` in the foreground; no FATAL EXCEPTION or ANR |
| Live Open-Meteo data | Place, temperature, condition, hourly and daily rows populated |
| Pressure banner | Stable strip shown with live sea-level pressure |
| Tablet two-pane layout | Left pane `place_name` / `pressure_value`, right pane `hourly_container` / `daily_container` |
| Saved places | `SAVE CURRENT PLACE` added the current place; dashboard chip strip appeared after returning |
| Pressure story | Scrolling the left pane revealed `story_text` and `button_symptom_log` |
| Symptom log | Opened from the chart; note field and save button present |
| History chart | `histogram`, `chart_legend`, `button_export_png` and `button_export_csv` present |
| PNG export | `SHARE PNG` opened `android/com.android.internal.app.ChooserActivity`, proving cache + URI provider + share sheet work |
| Quiet alerts | `dumpsys jobscheduler` shows job `7301` for `com.baroweather.app/.PressureAlertJobService` with a roughly 6 h window |
| Widget | `dumpsys package` lists `com.baroweather.app/.BaroWeatherWidget` with its provider metadata |
| Widget/export provider | `dumpsys package` also lists `com.baroweather.app/.ExportFileProvider` |
| Logcat | No FATAL EXCEPTION, ANR or AndroidRuntime crash during the full flow |

## Earlier v0.2/v0.3 checks retained

| Check | Result |
| --- | --- |
| Long-range data | 92 past + 16 forecast days requested; test location returned 61 past + 15 forecast days (1,836 hourly points) |
| Histogram | 77 daily or 1,836 hourly bars, 7/30/all filters, tooltip and summary stats verified |
| Landscape layout | Phone and tablet landscape two-pane layouts; chart bounds `[362,36][1920,1128]` |
| High contrast light / 150% text | Tested and restored; no clipping or crash |
| Orientation setting | `Landscape only` and `Follow device` both applied correctly |
| Backups | `2026-11-27_v0.2_pre-horizontal`, `2026-11-27_v0.3_horizontal`, `2026-11-27_v0.3_pre-bonus` under `D:\opencode_home_win\backups\BaroWeather\` |

## v0.4 top-edge spacing check

| Check | Result |
| --- | --- |
| 24 h sparkline | Density-aware 8 dp clearance replaced the fixed 8 px padding; `layout_height` raised from 90 dp to 98 dp (device bounds `[39,984][651,1128]`) |
| Long-range chart | Plot top moved from 34 dp to 42 dp and unit label from 20 dp to 24 dp, giving the landscape chart extra room above the bars |
| Visual rationale | Fixes the reported 5–8 px obscured top edge without touching pressure data or thresholds |

## Publication checks

| Check | Result |
| --- | --- |
| Final backup | `D:\opencode_home_win\backups\BaroWeather\2026-11-27_v0.4_bonus\` with source ZIP, signed APK and SHA-256 record |
| Local signing keystore | `android\debug.jks` excluded by `.gitignore`; not pushed |
| Public repository | `https://github.com/criterball/BaroWeather` |
| Push | Source, docs and signed v0.4.0 APK pushed to `main` |

## Notes

- The app default place remains London, United Kingdom. The test device had
  Modbury North saved as a chip during the saved-places flow.
- No thresholds, theme, font scale or orientation preference was left changed.
- Quiet notifications remain enabled by default; the banner Ack suppression and
  12 h dedupe are documented in `PRESSURE_ALERTS.md`.
- All weather/pressure guidance is informational, not medical advice.
