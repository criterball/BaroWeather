# Barometric pressure alert rules

## Signal used

Open-Meteo `pressure_msl` (sea-level pressure) in hPa is the primary signal.
Surface pressure is shown as secondary detail because it varies strongly with
altitude and is not comparable between places.

Forecast pressure comes from the same API's hourly `pressure_msl` series.

## Defaults

| Rule | Default | Reason |
| --- | --- | --- |
| Low now | current `< 1005 hPa` | Clearly below typical ~1013 hPa |
| Very high now | current `> 1030 hPa` | Clearly above typical high-pressure range |
| Falling watch | `<= -2 hPa` over 3 h or `<= -4 hPa` over 12 h | Meaningful short-term fall |
| Rising watch | `>= +2 hPa` over 3 h or `>= +4 hPa` over 12 h | Meaningful short-term rise |
| Incoming low | forecast min in next 12 h `< low threshold` | Warn before arrival |
| Incoming high | forecast max in next 12 h `> high threshold` | Warn before arrival |
| Clear hysteresis | current between `low+2` and `high-2`, no breach in next 6 h | Stop flicker near a threshold |

All values are configurable in Settings.

## Levels and banner text

| Level | Trigger | Banner |
| --- | --- | --- |
| `LOW_NOW` | current below low threshold | Red: `LOW PRESSURE — TAKE CARE` |
| `INCOMING_LOW` | falling watch or forecast breach | Red: `LOW PRESSURE INCOMING` |
| `HIGH_NOW` | current above high threshold | Red: `VERY HIGH PRESSURE` |
| `INCOMING_HIGH` | rising watch or forecast breach | Red: `HIGH PRESSURE INCOMING` |
| `STABLE` | none of the above | Cyan/green strip: `Pressure steady` |

The banner always states the measured/forecast value and the time window. A
single alert is chosen by priority: current conditions outrank incoming ones;
low outranks high when both are present.

## Mitigation text shown in the `WHY?` dialog

- Low pressure or a rapid fall is associated with discomfort for some people
  who are sensitive to weather changes.
- Consider hydration, avoiding sudden posture changes, reducing exertion, and
  following any plan given by a clinician.
- Very high pressure can also be uncomfortable for some people.
- This feature is informational and is **not medical advice**, diagnosis, or a
  substitute for professional care.

## Quiet notifications and widget

- The `Quiet incoming-pressure alerts` setting is on by default but can be
  switched off. On Android 13+ the app requests `POST_NOTIFICATIONS` when needed.
- A `JobScheduler` job checks the saved place every six hours with a 30-minute
  flex window and is restored after reboot. It uses the same assessment rules as
  the banner, including any custom thresholds.
- Alerts use a low-importance, silent channel with no vibration. The same alert
  level is not repeated within 12 hours, and the banner's `Ack` suppresses
  notifications for one hour.
- The home-screen widget reads the last cached forecast only. It shows the
  sea-level pressure, place and a green/red warning strip, and tapping it opens
  the app. It never performs its own network request.

## Implementation split

- `core/src/pressure.rs` — authoritative Rust rules and unit conversions.
- `android/jni/bw_jni.c` — thin JNI bridge to Rust static library.
- `android/src/.../PressureEngine.java` — Java fallback with identical rules.

The app calls native first and falls back silently if the library cannot load.

## Verification cases for the Rust tests

1. 1004 hPa current -> `LOW_NOW`.
2. 1012 hPa now, 1008 hPa in 3 h -> `INCOMING_LOW`.
3. 1031 hPa current -> `HIGH_NOW`.
4. 1015 hPa now, 1020 hPa in 3 h -> `INCOMING_HIGH`.
5. 1013 hPa flat -> `STABLE`.
6. Hysteresis: 1005.5 hPa after a low alert remains low until 1007 hPa.
7. Unit conversion: 1013.25 hPa -> 29.921 inHg and 759.999 mmHg.
