# BaroWeather feature backlog

## v0.1 — complete

| # | Feature | Status |
| --- | --- | --- |
| 1 | Rust cross-platform core with pressure rules, unit conversion, WMO codes | Done, 15 host tests passing |
| 2 | JNI bridge and native libraries for arm64, armv7, x86_64 | Done |
| 3 | Java fallback when native library is unavailable | Done |
| 4 | Red top banner for low / very-high / incoming pressure with WHY? and Ack | Done |
| 5 | Expanded pressure card: gauge, trend, 24 h sparkline, 6-row stats | Done |
| 6 | Current conditions hero card with canvas weather icons | Done |
| 7 | 24 h hourly strip with tap-to-expand detail | Done |
| 8 | 7-day forecast with expandable pressure/sun details | Done |
| 9 | Details grid (humidity, dew point, UV, wind, visibility, sun, coordinates) | Done |
| 10 | Settings: thresholds, trend sensitivity, units, theme, banner toggle | Done |
| 11 | City search via Open-Meteo geocoding | Done |
| 12 | Device location button with runtime permission request | Done |
| 13 | Last forecast cached for offline viewing with timestamp | Done |
| 14 | Phone single-column and tablet two-pane layouts | Done |
| 15 | Dependency-free local build with signing | Done, v1/v2/v3 verified |
| 16 | Live-device smoke test on Lenovo TB-X606F | Done |

## v0.2 — complete

| # | Feature | Status |
| --- | --- | --- |
| 23 | Long-range pressure histogram opened from gauge or pressure value | Done, cached and refreshed |
| 24 | Hourly/daily bars, 7/30/all-day windows, threshold colours, touch tooltip | Done |
| 25 | Axis labels, min/max/average/change/forecast summary, portrait + landscape layouts | Done |
| 26 | Reading/visibility settings: 100–200% text scale and four high-contrast-ready themes | Done |

## v0.3 — complete

| # | Feature | Status |
| --- | --- | --- |
| 27 | Dedicated phone and tablet landscape dashboard layouts | Done, full-width warning banner + two panes |
| 28 | Screen orientation setting: follow device / portrait / landscape | Done, applied across all activities |
| 29 | Timestamped source + APK backup before and after the landscape work | Done locally, no GitHub push |

## v0.4 — complete (approved bonus set)

| # | Feature | Status |
| --- | --- | --- |
| 1 | Plain-language pressure story card from the shared assessment | Done, shown in all main layouts |
| 2 | Private symptom log tied to the observed pressure | Done, dashboard + chart entry buttons, chart dots/tooltip/content description |
| 3 | Quiet incoming-pressure notification | Done, opt-out setting, Android 13 permission flow, channel, 6 h job with 30 min flex, Ack suppression, 12 h per-level dedupe |
| 4 | Home-screen pressure widget | Done, cached value, place, warning-colour strip and tap-to-open |
| 5 | Multiple saved places | Done, settings management and dashboard chip strip/tablet left rail |
| 6 | Colour-blind-safe chart patterns | Done, diagonal low / horizontal normal / vertical high + symptom dot |
| 7 | Animated gauge needle with reduce-motion switch | Done, 250 ms animation snaps when reduce motion is on |
| 8 | Pressure history export | Done, PNG of visible chart and CSV of full series + symptom notes via platform share sheet |
| 9 | Landscape sparkline/chart/gauge top-edge clearance | Done, density-aware 16 dp top / 8 dp bottom sparkline padding, 104 dp sparkline height, 20 dp gauge top inset with 160 dp gauge height, 42 dp histogram plot top |

## v0.4.1 — complete

| # | Feature | Status |
| --- | --- | --- |
| 1 | Rain chance (%) and expected mm in hero chip, hourly/daily rows and expanded details | Done, uses Open-Meteo `precipitation_probability`, `precipitation` and `precipitation_sum` already requested |
| 2 | `Rain today` details-grid entry | Done, daily projected total in mm |
| 3 | 110% text-scale option | Done, spinner now offers 100/110/125/150/175/200% plus system |
| 4 | Large temperature stays on one line at 125% and above | Done, 52 sp base with automatic shrink to 28 sp and single-line layout |
| 5 | Three new high-contrast themes | Done, blue, navy and matrix green alongside the four existing themes |
| 6 | Version/APK bump | Done, v0.4.1 / versionCode 4, signed and device-verified |

## v0.4.2 — complete

| # | Feature | Status |
| --- | --- | --- |
| 1 | Dedicated starred-favorites page | Done, toolbar star launches `FavoritesActivity` with name/coordinate rows |
| 2 | Star current place or add from search | Done, existing geocoder result is saved and starred without leaving the page |
| 3 | Open a favorite | Done, returns name/lat/lon to `MainActivity`, switches place and refreshes |
| 4 | Unstar/remove a favorite | Done, removes from starred list and dashboard chip strip |
| 5 | Version/APK bump | Done, v0.4.2 / versionCode 5, signed and device-verified |

## v0.4.3 — complete

| # | Feature | Status |
| --- | --- | --- |
| 1 | Keyless MET Norway pressure fallback behind Open-Meteo | Done, foreground fetch falls back on primary failure with no embedded secret |
| 2 | `air_pressure_at_sea_level` normalised to the app's `pressureMsl` signal | Done, hPa field mapped into current and hourly data; 6-hourly samples interpolated to hourly; `surfacePressure` stays display-only and shows `—` when absent |
| 3 | Provider-aware MET Norway caching | Done, four-decimal coordinate key, `Expires`/`Last-Modified` handling, 30 min Nordic / 3 h global minimum interval |
| 4 | Identifying User-Agent and attribution | Done, app/version/repository UA and `Data from MET Norway` source display in Details |
| 5 | Forecast-only scope | Done, no past samples so 3 h/12 h past trend stays neutral; 92-day history and place search remain Open-Meteo; background jobs remain Open-Meteo-only |
| 6 | Version/APK bump | Done, v0.4.3 / versionCode 6, signed and device-verified |

## Remaining / later

| # | Feature | Notes |
| --- | --- | --- |
| 19 | Radar / government severe-weather alerts | Different providers, heavier data model |
| 20 | Wear OS build | Out of scope for the phone/tablet APK |
| 22 | Automated UI screenshots / instrumented tests | Host unit tests and manual device smoke test done; automation later |
| 30 | Full multi-provider replacement | Keyless MET Norway forecast-pressure fallback delivered in v0.4.3; a replacement matching Open-Meteo's 92-day archive and geocoding remains deferred |
| 31 | Localisation | Deferred to the end of the list: Spanish, French, German and Polish after the other later work |

## Not planned

- Medical diagnosis, advice or treatment suggestions.
- Ads, analytics, account sign-in, billing or Play Services dependencies.
- Uploading symptom notes or pressure history to a third-party service.
