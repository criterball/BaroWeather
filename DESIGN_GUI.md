# BaroWeather GUI and layout design

## Design intent

Pressure must be visible before anything else, but the rest of the app should
still feel like a complete, calm weather app. The interface is information-dense
without being noisy: one strong colour accent, large numerals, thin dividers,
rounded surfaces, and a persistent red warning band when pressure needs
attention.

Colour is never the only signal: the warning band also uses an icon, a title,
and plain-language text. The history chart can add hatch patterns for low,
normal and high bands so colour-blind users can read it without relying on hue.

## Palette

### Dark theme (default)

| Role | Hex | Use |
| --- | --- | --- |
| Background | `#071426` | Window background |
| Surface | `#0E2138` | Cards, bars, dialogs |
| Surface raised | `#132C48` | Hero card, selected rows |
| Divider | `#24415F` | Hairlines |
| Primary text | `#F5F8FF` | Names, values |
| Secondary text | `#A8BDD4` | Labels, units, timestamps |
| Accent | `#45D6E6` | Active tab, gauge fill, links |
| Warm accent | `#FFB454` | UV, sunset, moderate watch |
| Alert red | `#D93025` | Warning banner background |
| Alert red deep | `#7F1410` | Banner gradient end |
| OK green | `#2E7D5B` | Stable pressure chip |

### Light theme

| Role | Hex |
| --- | --- |
| Background | `#F3F7FB` |
| Surface | `#FFFFFF` |
| Surface raised | `#EAF2F8` |
| Divider | `#D6E2EC` |
| Primary text | `#0B2033` |
| Secondary text | `#546B80` |
| Accent | `#0277BD` |
| Alert red | `#C5221F` |

## Screen structure

### 1. Warning banner (pinned at top)

Height: 72 dp minimum, wraps text if needed.

- Leading icon: pressure gauge with down/up arrow.
- Title: `LOW PRESSURE — TAKE CARE` / `VERY HIGH PRESSURE` /
  `LOW PRESSURE INCOMING`.
- Subtitle: current value, change over 3 h/12 h, and time window.
- Trailing `WHY?` button opens a short explanation and the medical disclaimer.
- Background: red gradient. White text. Never hidden behind a collapse except
  when the user explicitly acknowledges for 1 hour.

When pressure is stable the band collapses to a 28 dp cyan/green strip:
`Pressure steady · 1013 hPa · falling slowly`.

### 2. Hero current card

- Location name and last-updated time.
- Large temperature (56 sp), weather icon drawn on canvas, condition text.
- Feels-like, today's high/low, and day/night indicator.
- Small chips: precipitation chance, UV index, wind.

### 3. Pressure card (expanded, the app's focus)

- Big sea-level pressure value (34 sp) with unit toggle hPa / inHg / mmHg.
- Trend arrow and 3 h / 12 h delta chips.
- Semicircular gauge from 980 to 1045 hPa with coloured zones:
  low `<1005`, normal `1005–1030`, high `>1030`. Needle shows current value.
- 24 h pressure sparkline with threshold lines drawn in. It has density-aware
  8 dp top/bottom clearance and a 98 dp card height so high/low dots and the
  line stroke are not clipped in landscape.
- Six-row compact table: current, 3 h ago, 12 h ago, 24 h min, 24 h max,
  forecast min/max in next 12 h.
- Status line: `Here now`, `Incoming in about 6 h`, or `Stable`.

### 4. Hourly strip

Horizontally scrollable 24-hour cards, 96 dp wide:

- time, icon, temperature, precipitation %, pressure value.
- Selected hour expands inline with wind/gust/cloud details.
- Thin pressure line drawn behind the row to show the trend visually.

### 5. Seven-day forecast

Rows with day, icon, condition, high/low, precipitation chance, and a small
temperature range bar. Tap expands pressure range and sunrise/sunset.

### 6. Details grid

Two columns on phone, four on tablet:

- wind speed + direction + gusts
- humidity + dew point
- UV index + description
- visibility
- sunrise / sunset / daylight length
- precipitation and cloud cover
- surface pressure vs sea-level pressure
- coordinates and elevation

### 7. Long-range pressure histogram

Tap the pressure gauge or the large pressure value to open a dedicated screen.

- Up to 92 past days of Open-Meteo hourly sea-level-pressure reanalysis plus
  up to 16 forecast days, so the full available history/prediction is kept.
- Hourly bars for detailed inspection, or daily average bars with daily
  min/max in the tooltip.
- `7 DAYS`, `30 DAYS` and `ALL` filters centre the selected window on "now".
- Y-axis labels use the selected pressure unit; x-axis labels show date/time.
- Red bars are below the low threshold, amber bars above the high threshold,
  cyan/normal otherwise. Dashed threshold guides are labelled `LOW`/`HIGH`.
- Touch/drag a bar to inspect its time, value and past/forecast state.
- Summary shows minimum, maximum, average, total change, forecast low/high,
  and bar count. A short disclaimer separates weather model output from
  medical advice.
- Portrait: chart above stats. Landscape: scrollable control/stat panel on the
  left, wide labelled chart on the right; the plot starts 42 dp from the top so
  the unit label and highest bars are never obscured.

### 8. Pressure story card

A compact card below the pressure table gives one plain sentence with the
current state, 24 h range and next-12-hour forecast. It is generated from the
same assessment that drives the banner and notification, so the wording and
thresholds cannot drift apart. The card's `ADD SYMPTOMS` button opens the log.

### 9. Symptom log

A private, on-device list of symptom masks and optional notes. Entries are
associated with the pressure observation at save time and shown as warm dots on
the long-range histogram; the same annotations are included in CSV exports.
Nothing is uploaded or shared automatically.

### 10. Saved places, widget and export

- Saved places appear as a horizontal chip strip under the toolbar. Search and
  device-location results can be saved; each chip switches the whole dashboard
  and refreshes pressure. Settings can save, switch and remove places.
- The home-screen widget uses the last cached pressure reading, place name and
  warning-colour strip. Because it reads the cache, it remains useful offline
  and never starts a network request from the widget itself. Tapping it opens
  `MainActivity`.
- The history screen has `SHARE PNG` and `SHARE CSV` buttons. Files are written
  to the app cache and exposed through a small read-only platform
  `ContentProvider`, then opened in the system share sheet.

### 11. Footer/settings

- Units: temperature C/F, wind km/h/mph/m/s, pressure hPa/inHg/mmHg.
- Pressure alert settings: low threshold, high threshold, 3 h and 12 h trend
  sensitivity, enable banner, optional notification.
- Saved places: save current place, switch place or remove a saved place.
- Reading/visibility: text size follows system scale or is overridden at
  100–200%; dark, light, high-contrast dark and high-contrast light palettes.
- Colour-blind-safe chart patterns and reduce motion (no needle animation).
- Refresh behaviour, about/disclaimer; no account, ads or analytics.

## Custom views (drawn, no image assets)

| View | Drawing |
| --- | --- |
| `ConditionIconView` | Canvas sun/cloud/rain/snow/storm/fog glyphs, theme-aware |
| `PressureGaugeView` | Arc, coloured zones, needle, threshold ticks |
| `SparklineView` | 24 h line, min/max dots, horizontal threshold guides |
| `RangeBarView` | Daily min/max temperature bar |
| `PressureHistogramView` | Long-range bars, labelled axes, threshold guides, optional hatch patterns, symptom dots, tap tooltip, PNG export |
| `AlertBannerView` | Red gradient, icon, text, acknowledge button |
| `RemoteViews` widget | Cached pressure value, place, warning-colour strip, tap-to-open |

Custom drawing keeps the APK small, sharp at every density, and consistent
across Android versions.

## Phone portrait

Single `ScrollView`, sections stacked in this order:

1. warning banner (sticky top)
2. saved places chip strip (hidden until a place is saved)
3. hero card
4. pressure card
5. pressure story and symptom button
6. hourly strip
7. seven-day list
8. details grid
9. settings entry

Minimum touch target 48 dp. Body text 16 sp, labels 13 sp.

## Phone landscape / large phone

`layout-land` uses two independently scrollable columns with 42/58 weight:

- left: toolbar, saved places strip, hero card, pressure card, pressure story
- right: hourly strip, seven-day list, details grid

The warning banner always spans full width at the top, so it remains the first
thing seen when the device is turned sideways.

## Tablet portrait (sw600dp)

Left pane 38%: saved places strip, hero card, pressure gauge, status and
pressure story.
Right pane 62%: warning banner, hourly strip, daily rows, details grid.

## Tablet landscape (sw600dp-land)

`layout-sw600dp-land` uses two independently scrollable columns with 36/64
weight:

- left: toolbar, saved places strip, hero card, large pressure gauge, 24 h
  sparkline, six-row table, pressure story
- right: hourly strip, seven-day list, four-column details grid

The full-width warning banner stays pinned above both columns. The histogram
screen has its own landscape layout with controls/statistics on the left and a
wide labelled chart on the right.

## Accessibility

- Every icon-only control has `contentDescription`.
- Red banner text includes the reason, never only colour.
- TalkBack reads the pressure value, direction and alert level as one string.
- Font size follows system scale, or an in-app override of 100–200%; layouts
  scroll so fixed-sp labels do not clip at large sizes.
- Minimum contrast 4.5:1 for body text in all four themes; the two high-contrast
  palettes use pure black/white with strong accent, alert and OK colours.
- Screen orientation can follow the device or be locked to portrait/landscape
  for users who prefer a stable layout.
- Chart patterns use diagonal, horizontal and vertical hatching as a second
  signal alongside colour and labels.
- Reduce motion removes the 250 ms needle animation for users who prefer it.
- `WHY?` dialog includes: "Informational weather guidance, not medical advice."

## Motion

- 150 ms fade for banner appearance.
- Gauge needle animates over 250 ms; the reduce-motion setting snaps it instead.
- No parallax, no long transitions; refresh is a thin top progress line.
- The 24 h sparkline keeps density-aware 8 dp clearance and the landscape chart
  plot starts 42 dp from the top, avoiding top-edge clipping after the v0.4
  spacing adjustment.

## Empty/error states

- No location permission: explanatory card with `USE MY LOCATION` and
  `ENTER CITY` buttons.
- No network: last cached forecast with timestamp and `OFFLINE DATA` chip.
- API error: retry button, cached data kept if available.
- Geocoding no match: `No matching place` with suggestions to check spelling.
