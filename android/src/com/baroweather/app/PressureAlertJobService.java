package com.baroweather.app;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Background pressure check used by the quiet incoming-pressure notification. */
public final class PressureAlertJobService extends JobService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public boolean onStartJob(final JobParameters params) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SettingsStore store = new SettingsStore(PressureAlertJobService.this);
                    WeatherData weather = WeatherRepository.fetch(store.getLat(), store.getLon());
                    weather.placeName = store.getPlaceName();
                    store.saveCache(weather.rawJson, weather.fetchedAt);
                    PressureNotifier.evaluateAndMaybeNotify(PressureAlertJobService.this, weather);
                } catch (Exception ignored) {
                    // A failed background check simply waits for the next scheduled run.
                } finally {
                    jobFinished(params, false);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
