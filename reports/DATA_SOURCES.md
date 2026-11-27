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

## Geocoding

Endpoint: `https://geocoding-api.open-meteo.com/v1/search?name=...&count=8&language=en&format=json`

Used only when the user types a place name. Results are cached in memory.

## Weather codes

WMO codes 0–99 are mapped in Rust and mirrored in Java. The authoritative map
is in `core/src/weather.rs`.

## Privacy

Only the chosen coordinates are sent to Open-Meteo. No account, advertising ID,
device ID or analytics event is sent. Last response is cached app-private.
