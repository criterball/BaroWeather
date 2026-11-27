/* Thin JNI bridge. All logic lives in the Rust static library. */
#include <jni.h>
#include <stdlib.h>

extern char *bw_assess_pressure(double current, double h3, double h12,
                                double forecast_min_12, double forecast_max_12,
                                double low_hpa, double high_hpa,
                                double drop3_hpa, double change12_hpa,
                                int previous_id);
extern char *bw_format_pressure(double hpa, int unit_id);
extern char *bw_weather_condition(int code);
extern char *bw_version(void);
extern void bw_free_string(char *ptr);

static jstring owned_to_jstring(JNIEnv *env, char *value) {
    jstring result;
    if (value == NULL) {
        return NULL;
    }
    result = (*env)->NewStringUTF(env, value);
    bw_free_string(value);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_baroweather_app_NativeCore_pressureAssessment(
        JNIEnv *env, jclass clazz,
        jdouble current, jdouble h3, jdouble h12,
        jdouble forecastMin12, jdouble forecastMax12,
        jdouble lowHpa, jdouble highHpa,
        jdouble drop3Hpa, jdouble change12Hpa,
        jint previousId) {
    (void)clazz;
    return owned_to_jstring(env, bw_assess_pressure(
            current, h3, h12, forecastMin12, forecastMax12,
            lowHpa, highHpa, drop3Hpa, change12Hpa, previousId));
}

JNIEXPORT jstring JNICALL
Java_com_baroweather_app_NativeCore_formatPressure(
        JNIEnv *env, jclass clazz, jdouble hpa, jint unitId) {
    (void)clazz;
    return owned_to_jstring(env, bw_format_pressure(hpa, unitId));
}

JNIEXPORT jstring JNICALL
Java_com_baroweather_app_NativeCore_weatherCondition(
        JNIEnv *env, jclass clazz, jint code) {
    (void)clazz;
    return owned_to_jstring(env, bw_weather_condition(code));
}

JNIEXPORT jstring JNICALL
Java_com_baroweather_app_NativeCore_version(JNIEnv *env, jclass clazz) {
    (void)clazz;
    return owned_to_jstring(env, bw_version());
}
