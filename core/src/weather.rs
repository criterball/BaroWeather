//! WMO weather-code mapping and icon families.

/// Broad icon family used by `ConditionIconView`.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum WeatherFamily {
    Clear,
    Cloudy,
    Fog,
    Drizzle,
    Rain,
    Snow,
    Storm,
}

/// Human-readable description for a WMO code.
pub fn description(code: i32) -> &'static str {
    match code {
        0 => "Clear sky",
        1 => "Mainly clear",
        2 => "Partly cloudy",
        3 => "Overcast",
        45 => "Fog",
        48 => "Depositing rime fog",
        51 => "Light drizzle",
        53 => "Moderate drizzle",
        55 => "Dense drizzle",
        56 => "Light freezing drizzle",
        57 => "Dense freezing drizzle",
        61 => "Slight rain",
        63 => "Moderate rain",
        65 => "Heavy rain",
        66 => "Light freezing rain",
        67 => "Heavy freezing rain",
        71 => "Slight snowfall",
        73 => "Moderate snowfall",
        75 => "Heavy snowfall",
        77 => "Snow grains",
        80 => "Slight rain showers",
        81 => "Moderate rain showers",
        82 => "Violent rain showers",
        85 => "Slight snow showers",
        86 => "Heavy snow showers",
        95 => "Thunderstorm",
        96 => "Thunderstorm with slight hail",
        97 => "Heavy thunderstorm",
        99 => "Thunderstorm with heavy hail",
        _ => "Unknown conditions",
    }
}

/// Icon family for a WMO code.
pub fn family(code: i32) -> WeatherFamily {
    match code {
        0 | 1 => WeatherFamily::Clear,
        2 | 3 => WeatherFamily::Cloudy,
        45 | 48 => WeatherFamily::Fog,
        51 | 53 | 55 | 56 | 57 => WeatherFamily::Drizzle,
        61 | 63 | 65 | 66 | 67 | 80 | 81 | 82 => WeatherFamily::Rain,
        71 | 73 | 75 | 77 | 85 | 86 => WeatherFamily::Snow,
        95 | 96 | 97 | 99 => WeatherFamily::Storm,
        _ => WeatherFamily::Cloudy,
    }
}

/// Short label suitable for the hourly strip.
pub fn short_label(code: i32) -> &'static str {
    match family(code) {
        WeatherFamily::Clear => "Clear",
        WeatherFamily::Cloudy => "Cloud",
        WeatherFamily::Fog => "Fog",
        WeatherFamily::Drizzle => "Drizzle",
        WeatherFamily::Rain => "Rain",
        WeatherFamily::Snow => "Snow",
        WeatherFamily::Storm => "Storm",
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn known_codes() {
        assert_eq!(description(0), "Clear sky");
        assert_eq!(description(63), "Moderate rain");
        assert_eq!(description(95), "Thunderstorm");
        assert_eq!(description(123), "Unknown conditions");
    }

    #[test]
    fn families() {
        assert_eq!(family(2), WeatherFamily::Cloudy);
        assert_eq!(family(48), WeatherFamily::Fog);
        assert_eq!(family(65), WeatherFamily::Rain);
        assert_eq!(family(75), WeatherFamily::Snow);
        assert_eq!(family(99), WeatherFamily::Storm);
    }
}
