//! BaroWeather cross-platform core.
//!
//! Pure Rust logic is the single source of truth for pressure warnings, unit
//! conversion and weather-code text. The C ABI at the bottom is consumed by the
//! small Android JNI bridge in `android/jni/bw_jni.c`.

pub mod pressure;
pub mod units;
pub mod weather;

use pressure::{assess, AlertLevel, Assessment, PressureConfig};
use std::ffi::CString;
use std::os::raw::{c_char, c_double, c_int};
use units::{format_pressure, PressureUnit};

fn c_string(value: String) -> *mut c_char {
    CString::new(value)
        .unwrap_or_else(|_| CString::new("invalid string").expect("literal is valid"))
        .into_raw()
}

/// Release a string returned by any `bw_*` function.
///
/// # Safety
/// `ptr` must be a non-null pointer previously returned by this library and
/// must not have been freed already.
#[no_mangle]
pub unsafe extern "C" fn bw_free_string(ptr: *mut c_char) {
    if !ptr.is_null() {
        drop(CString::from_raw(ptr));
    }
}

fn json_escape(value: &str) -> String {
    value
        .replace('\\', "\\\\")
        .replace('"', "\\\"")
        .replace('\n', "\\n")
}

fn assessment_json(a: &Assessment) -> String {
    format!(
        "{{\"level\":{},\"warning\":{},\"title\":\"{}\",\"message\":\"{}\",\"current_hpa\":{:.3},\"delta3_hpa\":{:.3},\"delta12_hpa\":{:.3}}}",
        a.level.id(),
        if a.level.is_warning() { 1 } else { 0 },
        json_escape(a.title),
        json_escape(&a.message),
        a.current_hpa,
        a.delta3_hpa,
        a.delta12_hpa
    )
}

/// Assess pressure and return a JSON string.
///
/// `previous_id` uses `AlertLevel::id()`; pass 0 on first run.
///
/// # Safety
/// The returned pointer must be released with `bw_free_string`.
#[no_mangle]
pub extern "C" fn bw_assess_pressure(
    current: c_double,
    h3: c_double,
    h12: c_double,
    forecast_min_12: c_double,
    forecast_max_12: c_double,
    low_hpa: c_double,
    high_hpa: c_double,
    drop3_hpa: c_double,
    change12_hpa: c_double,
    previous_id: c_int,
) -> *mut c_char {
    let config = PressureConfig {
        low_hpa,
        high_hpa,
        drop3_hpa,
        change12_hpa,
    };
    let previous = if previous_id == 0 {
        None
    } else {
        Some(AlertLevel::from_id(previous_id))
    };
    let result = assess(
        current,
        h3,
        h12,
        forecast_min_12,
        forecast_max_12,
        config,
        previous,
    );
    c_string(assessment_json(&result))
}

/// Format a pressure value using unit id 0=hPa, 1=inHg, 2=mmHg.
///
/// # Safety
/// The returned pointer must be released with `bw_free_string`.
#[no_mangle]
pub extern "C" fn bw_format_pressure(hpa: c_double, unit_id: c_int) -> *mut c_char {
    c_string(format_pressure(hpa, PressureUnit::from_id(unit_id)))
}

/// Return the WMO description for a weather code.
///
/// # Safety
/// The returned pointer must be released with `bw_free_string`.
#[no_mangle]
pub extern "C" fn bw_weather_condition(code: c_int) -> *mut c_char {
    c_string(weather::description(code).to_string())
}

/// Return the core version string.
///
/// # Safety
/// The returned pointer must be released with `bw_free_string`.
#[no_mangle]
pub extern "C" fn bw_version() -> *mut c_char {
    c_string("baroweather-core 0.1.0".to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn c_abi_json_has_expected_fields() {
        let ptr = bw_assess_pressure(
            1004.0, 1006.0, 1008.0, 1003.0, 1010.0, 1005.0, 1030.0, 2.0, 4.0, 0,
        );
        unsafe {
            let text = std::ffi::CStr::from_ptr(ptr).to_string_lossy().into_owned();
            assert!(text.contains("\"level\":2"));
            assert!(text.contains("LOW PRESSURE"));
            bw_free_string(ptr);
        }
    }

    #[test]
    fn c_abi_format_and_condition() {
        let p = bw_format_pressure(1013.25, 1);
        let w = bw_weather_condition(63);
        unsafe {
            assert!(std::ffi::CStr::from_ptr(p)
                .to_string_lossy()
                .contains("inHg"));
            assert_eq!(
                std::ffi::CStr::from_ptr(w).to_string_lossy(),
                "Moderate rain"
            );
            bw_free_string(p);
            bw_free_string(w);
        }
    }
}
