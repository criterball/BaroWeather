//! Unit conversion helpers shared by all clients.

/// Pressure unit selected in Settings.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum PressureUnit {
    Hpa = 0,
    InHg = 1,
    MmHg = 2,
}

impl PressureUnit {
    pub fn from_id(id: i32) -> Self {
        match id {
            1 => PressureUnit::InHg,
            2 => PressureUnit::MmHg,
            _ => PressureUnit::Hpa,
        }
    }

    pub fn suffix(self) -> &'static str {
        match self {
            PressureUnit::Hpa => "hPa",
            PressureUnit::InHg => "inHg",
            PressureUnit::MmHg => "mmHg",
        }
    }
}

pub fn hpa_to_inhg(hpa: f64) -> f64 {
    hpa * 0.029529983071445
}

pub fn hpa_to_mmhg(hpa: f64) -> f64 {
    hpa * 0.750061682704
}

pub fn c_to_f(celsius: f64) -> f64 {
    celsius * 9.0 / 5.0 + 32.0
}

pub fn kmh_to_mph(kmh: f64) -> f64 {
    kmh * 0.621371192237
}

pub fn kmh_to_ms(kmh: f64) -> f64 {
    kmh / 3.6
}

/// Format pressure with the correct precision for the chosen unit.
pub fn format_pressure(hpa: f64, unit: PressureUnit) -> String {
    match unit {
        PressureUnit::Hpa => format!("{:.0} hPa", hpa),
        PressureUnit::InHg => format!("{:.2} inHg", hpa_to_inhg(hpa)),
        PressureUnit::MmHg => format!("{:.0} mmHg", hpa_to_mmhg(hpa)),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pressure_conversions() {
        assert!((hpa_to_inhg(1013.25) - 29.9213).abs() < 0.001);
        assert!((hpa_to_mmhg(1013.25) - 760.0).abs() < 0.01);
    }

    #[test]
    fn formatting_includes_units() {
        assert_eq!(format_pressure(1013.0, PressureUnit::Hpa), "1013 hPa");
        assert!(format_pressure(1013.0, PressureUnit::InHg).ends_with("inHg"));
        assert!(format_pressure(1013.0, PressureUnit::MmHg).ends_with("mmHg"));
    }

    #[test]
    fn temperature_and_wind() {
        assert!((c_to_f(0.0) - 32.0).abs() < f64::EPSILON);
        assert!((c_to_f(100.0) - 212.0).abs() < f64::EPSILON);
        assert!((kmh_to_ms(36.0) - 10.0).abs() < f64::EPSILON);
    }
}
