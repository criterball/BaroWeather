package com.baroweather.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small private list of frequently used places. */
public final class PlaceStore {
    public static final int MAX_PLACES = 12;

    private static final String NAME = "baroweather_places";
    private static final String KEY = "places";

    private final SharedPreferences prefs;

    public PlaceStore(Context context) {
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static final class Place {
        public final String name;
        public final double lat;
        public final double lon;
        public final boolean favorite;

        public Place(String name, double lat, double lon) {
            this(name, lat, lon, true);
        }

        public Place(String name, double lat, double lon, boolean favorite) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.favorite = favorite;
        }

        String key() {
            return String.format(Locale.US, "%s|%.5f|%.5f", name, lat, lon);
        }
    }

    public List<Place> getPlaces() {
        List<Place> results = new ArrayList<Place>();
        String raw = prefs.getString(KEY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                results.add(new Place(item.optString("name", ""),
                        item.optDouble("lat", 0.0), item.optDouble("lon", 0.0),
                        item.optBoolean("favorite", true)));
            }
        } catch (Exception ignored) {
            // Treat a corrupt list as empty rather than crashing the dashboard.
        }
        return results;
    }

    public boolean add(String name, double lat, double lon) {
        if (name == null || name.trim().length() == 0) {
            return false;
        }
        Place candidate = new Place(name.trim(), lat, lon, true);
        List<Place> places = getPlaces();
        List<Place> kept = new ArrayList<Place>();
        for (Place place : places) {
            if (!place.key().equals(candidate.key())) {
                kept.add(place);
            }
        }
        kept.add(0, candidate);
        if (kept.size() > MAX_PLACES) {
            kept = new ArrayList<Place>(kept.subList(0, MAX_PLACES));
        }
        save(kept);
        return true;
    }

    public void remove(int index) {
        List<Place> places = getPlaces();
        if (index < 0 || index >= places.size()) {
            return;
        }
        places.remove(index);
        save(places);
    }

    public void setFavorite(int index, boolean favorite) {
        List<Place> places = getPlaces();
        if (index < 0 || index >= places.size()) {
            return;
        }
        Place old = places.get(index);
        places.set(index, new Place(old.name, old.lat, old.lon, favorite));
        save(places);
    }

    public int indexOf(Place place) {
        if (place == null) {
            return -1;
        }
        List<Place> places = getPlaces();
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).key().equals(place.key())) {
                return i;
            }
        }
        return -1;
    }

    public void save(List<Place> places) {
        JSONArray array = new JSONArray();
        for (Place place : places) {
            JSONObject item = new JSONObject();
            try {
                item.put("name", place.name);
                item.put("lat", place.lat);
                item.put("lon", place.lon);
                item.put("favorite", place.favorite);
                array.put(item);
            } catch (Exception ignored) {
                // Skip a place that cannot be serialised.
            }
        }
        prefs.edit().putString(KEY, array.toString()).apply();
    }
}
