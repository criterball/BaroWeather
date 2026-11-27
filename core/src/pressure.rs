//! Barometric-pressure assessment rules.
//!
//! The Android app and any future desktop client should call `assess` so the
//! warning behaviour stays identical everywhere.

/// Severity/state of the pressure warning banner.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum AlertLevel {
    Stable = 0,
    IncomingLow = 1,
    LowNow = 2,
    IncomingHigh = 3,
    HighNow = 4,
}

impl AlertLevel {
    /// Numeric id shared with the Java fallback and the JNI bridge.
    pub fn id(self) -> i32 {
        self as i32
    }

    /// Convert from the shared numeric id, defaulting to `Stable`.
    pub fn from_id(id: i32) -> Self {
        match id {
            1 => AlertLevel::IncomingLow,
            2 => AlertLevel::LowNow,
            3 => AlertLevel::IncomingHigh,
            4 => AlertLevel::HighNow,
            _ => AlertLevel::Stable,
        }
    }

    /// True when the red warning banner must be shown.
    pub fn is_warning(self) -> bool {
        self != AlertLevel::Stable
    }
}

/// User-configurable thresholds. Defaults are deliberately conservative and
/// are described as informational, not medical, limits.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct PressureConfig {
    pub low_hpa: f64,
    pub high_hpa: f64,
    /// Minimum fall over 3 hours that counts as "incoming low".
    pub drop3_hpa: f64,
    /// Minimum fall/rise over 12 hours that counts as an incoming change.
    pub change12_hpa: f64,
}

impl Default for PressureConfig {
    fn default() -> Self {
        PressureConfig {
            low_hpa: 1005.0,
            high_hpa: 1030.0,
            drop3_hpa: 2.0,
            change12_hpa: 4.0,
        }
    }
}

/// Result of one pressure assessment.
#[derive(Clone, Debug, PartialEq)]
pub struct Assessment {
    pub level: AlertLevel,
    pub title: &'static str,
    pub message: String,
    pub current_hpa: f64,
    pub delta3_hpa: f64,
    pub delta12_hpa: f64,
}

/// Assess current and forecast pressure.
///
/// `current`, `h3`, `h12` are sea-level pressure in hPa now, 3 h ago and 12 h
/// ago. `forecast_min_12` / `forecast_max_12` cover the next 12 hours.
/// `previous` enables 2 hPa hysteresis so the banner does not flicker when a
/// value sits on a threshold.
pub fn assess(
    current: f64,
    h3: f64,
    h12: f64,
    forecast_min_12: f64,
    forecast_max_12: f64,
    config: PressureConfig,
    previous: Option<AlertLevel>,
) -> Assessment {
    let delta3 = current - h3;
    let delta12 = current - h12;

    let prev_low = matches!(
        previous,
        Some(AlertLevel::LowNow) | Some(AlertLevel::IncomingLow)
    );
    let prev_high = matches!(
        previous,
        Some(AlertLevel::HighNow) | Some(AlertLevel::IncomingHigh)
    );

    let low_persist = prev_low
        && current < config.low_hpa + 2.0
        && (delta3 < 1.0 || forecast_min_12 < config.low_hpa + 2.0);
    let high_persist = prev_high
        && current > config.high_hpa - 2.0
        && (delta12 > -1.0 || forecast_max_12 > config.high_hpa - 2.0);

    let low_now = current < config.low_hpa || low_persist;
    let high_now = current > config.high_hpa || high_persist;

    let falling = delta3 <= -config.drop3_hpa || delta12 <= -config.change12_hpa;
    let rising = delta3 >= config.drop3_hpa || delta12 >= config.change12_hpa;
    let forecast_low = forecast_min_12 < config.low_hpa;
    let forecast_high = forecast_max_12 > config.high_hpa;

    let incoming_low = !low_now
        && (falling || forecast_low)
        && (delta3 < 0.0 || forecast_low);
    let incoming_high = !high_now
        && (rising || forecast_high)
        && (delta3 > 0.0 || forecast_high);

    let (level, title, message) = if low_now {
        (
            AlertLevel::LowNow,
            "LOW PRESSURE — TAKE CARE",
            format!(
                "Sea-level pressure is {:.0} hPa, below the {:.0} hPa warning level.",
                current, config.low_hpa
            ),
        )
    } else if high_now {
        (
            AlertLevel::HighNow,
            "VERY HIGH PRESSURE",
            format!(
                "Sea-level pressure is {:.0} hPa, above the {:.0} hPa warning level.",
                current, config.high_hpa
            ),
        )
    } else if incoming_low {
        let mut reasons = Vec::new();
        if falling {
            reasons.push(format!(
                "fallen {:.1} hPa in 3 h",
                delta3.abs()
            ));
        }
        if forecast_low {
            reasons.push(format!(
                "may reach {:.0} hPa in the next 12 h",
                forecast_min_12
            ));
        }
        (
            AlertLevel::IncomingLow,
            "LOW PRESSURE INCOMING",
            format!(
                "Pressure has {}. Take care if you are sensitive to falls.",
                reasons.join(" and ")
            ),
        )
    } else if incoming_high {
        let mut reasons = Vec::new();
        if rising {
            reasons.push(format!("risen {:.1} hPa in 3 h", delta3));
        }
        if forecast_high {
            reasons.push(format!(
                "may reach {:.0} hPa in the next 12 h",
                forecast_max_12
            ));
        }
        (
            AlertLevel::IncomingHigh,
            "HIGH PRESSURE INCOMING",
            format!(
                "Pressure has {}. Take care if you are sensitive to rises.",
                reasons.join(" and ")
            ),
        )
    } else {
        (
            AlertLevel::Stable,
            "Pressure steady",
            format!(
                "Sea-level pressure is {:.0} hPa and no low/high threshold is expected soon.",
                current
            ),
        )
    };

    Assessment {
        level,
        title,
        message,
        current_hpa: current,
        delta3_hpa: delta3,
        delta12_hpa: delta12,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn cfg() -> PressureConfig {
        PressureConfig::default()
    }

    #[test]
    fn low_now_when_current_below_threshold() {
        let a = assess(1004.0, 1006.0, 1008.0, 1003.0, 1010.0, cfg(), None);
        assert_eq!(a.level, AlertLevel::LowNow);
        assert!(a.level.is_warning());
    }

    #[test]
    fn incoming_low_from_three_hour_fall() {
        let a = assess(1012.0, 1015.0, 1016.0, 1010.0, 1014.0, cfg(), None);
        assert_eq!(a.level, AlertLevel::IncomingLow);
        assert!(a.message.contains("fallen 3.0 hPa"));
    }

    #[test]
    fn incoming_low_from_forecast_breach() {
        let a = assess(1015.0, 1015.5, 1016.0, 1002.0, 1016.0, cfg(), None);
        assert_eq!(a.level, AlertLevel::IncomingLow);
        assert!(a.message.contains("next 12 h"));
    }

    #[test]
    fn high_now_when_current_above_threshold() {
        let a = assess(1031.0, 1029.0, 1027.0, 1028.0, 1032.0, cfg(), None);
        assert_eq!(a.level, AlertLevel::HighNow);
    }

    #[test]
    fn incoming_high_from_rise() {
        let a = assess(1015.0, 1012.0, 1010.0, 1014.0, 1018.0, cfg(), None);
        assert_eq!(a.level, AlertLevel::IncomingHigh);
    }

    #[test]
    fn stable_when_flat_and_inside_range() {
        let a = assess(1013.0, 1013.0, 1013.0, 1011.0, 1015.0, cfg(), None);
        assert_eq!(a.level, AlertLevel::Stable);
        assert!(!a.level.is_warning());
    }

    #[test]
    fn hysteresis_keeps_low_until_two_hpa_recovery() {
        let held = assess(
            1005.5,
            1006.0,
            1007.0,
            1006.0,
            1008.0,
            cfg(),
            Some(AlertLevel::LowNow),
        );
        assert_eq!(held.level, AlertLevel::LowNow);

        let cleared = assess(
            1007.5,
            1007.0,
            1007.0,
            1007.0,
            1009.0,
            cfg(),
            Some(AlertLevel::LowNow),
        );
        assert_eq!(cleared.level, AlertLevel::Stable);
    }

    #[test]
    fn ids_round_trip() {
        for level in [
            AlertLevel::Stable,
            AlertLevel::IncomingLow,
            AlertLevel::LowNow,
            AlertLevel::IncomingHigh,
            AlertLevel::HighNow,
        ] {
            assert_eq!(AlertLevel::from_id(level.id()), level);
        }
    }
}
