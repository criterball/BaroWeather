# Weather data source notes

## Open-Meteo Forecast API

Endpoint: `https://api.open-meteo.com/v1/forecast`

No API key is required for non-commercial use. Requested variables:

- `current`: `temperature_2m`, `relative_humidity_2m`,
  `apparent_temperature`, `is_day`, `precipitation`, `weather_code`,
  `cloud_cover`, `pressure_msl`, `surface_pressure`, `wind_speed_10m`,
  `wind_direction_10m`, `wind_gusts_10m`
- `hourly`: `temperature_2m`, `relative_humidity_2m`,
  `apparent_temperature`, `precipitation_probability`, `precipitation`,
  `weather_code`, `cloud_cover`, `pressure_msl`, `surface_pressure`,
  `wind_speed_10m`, `wind_direction_10m`, `wind_gusts_10m`, `uv_index`,
  `visibility`
- `daily`: `weather_code`, `temperature_2m_max`, `temperature_2m_min`,
  `apparent_temperature_max`, `apparent_temperature_min`,
  `precipitation_probability_max`, `precipitation_sum`, `sunrise`, `sunset`,
  `daylight_duration`, `uv_index_max`, `wind_speed_10m_max`,
  `wind_gusts_10m_max`, `wind_direction_10m_dominant`
- `timezone=auto`, `forecast_days=7`, `past_days=1`

Pressure is returned in hPa. `pressure_msl` is sea-level reduced and is the
comparison signal. `surface_pressure` is displayed but not used for warnings.

## MET Norway Locationforecast 2.0 fallback

Endpoint: `https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=...&lon=...`

Used only when the foreground Open-Meteo dashboard fetch fails. It is keyless
and sends an identifying User-Agent (`BaroWeather/0.4.4` plus the repository
URL). The app reads `properties.timeseries[].data.instant.details.air_pressure_at_sea_level`,
already in hPa, into the same `pressureMsl` signal used by the warning rules.
`surface_pressure` remains display-only and is not filled by the fallback; the
Details row shows `—` for it. MET Norway is hourly in the Nordic region and can
be 6-hourly elsewhere; those coarser samples are linearly interpolated onto an
hourly grid so 3 h/12 h and 24 h rules keep their window semantics.

Requests are cached by four-decimal coordinates and reuse cached data until both
the provider `Expires` time and the app minimum interval (30 min in the Nordic
region, 3 h elsewhere) have passed. `If-Modified-Since`/`Last-Modified` handling
keeps conditional refresh cheap. Background notification checks stay on
Open-Meteo only while the app is inactive.

Limitations: Locationforecast has no past samples, no 92-day archive and no
weather-API-native place search. The past 3 h/12 h fall/rise trend therefore
stays neutral on fallback data, while current low/high and incoming forecast
low/high still work. The long-range chart and geocoding remain Open-Meteo-only. When
MET Norway data is served from cache, the dashboard marks it `OFFLINE DATA`.
Attribution shown in Details: `Data from MET Norway`. Data is used under NLOD
2.0 / CC BY 4.0; the app does not imply endorsement.

## Geocoding

Endpoint: `https://geocoding-api.open-meteo.com/v1/search?name=...&count=8&language=en&format=json`

Used only when the user types a place name. Results are cached in memory.

## Weather codes

WMO codes 0–99 are mapped in Rust and mirrored in Java. The authoritative map
is in `core/src/weather.rs`.

## Privacy

Only the chosen coordinates are sent to Open-Meteo, and to MET Norway only when
the primary fetch fails. No account, advertising ID, device ID or analytics
event is sent. The last response and the MET Norway fallback cache are stored
app-private.
