package com.baroweather.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/** Dedicated page for starred, frequently used places. */
public final class FavoritesActivity extends BaseActivity {
    private static final int REQUEST_FAVORITE_SEARCH = 404;

    private SettingsStore store;
    private PlaceStore places;
    private LinearLayout list;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        store = new SettingsStore(this);
        places = new PlaceStore(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        list = findViewById(R.id.favorites_list);
        empty = findViewById(R.id.favorites_empty);
        render();

        findViewById(R.id.favorite_add_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(FavoritesActivity.this, SearchActivity.class),
                        REQUEST_FAVORITE_SEARCH);
            }
        });
        findViewById(R.id.favorite_add_current).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = store.getPlaceName();
                if (name == null || name.trim().length() == 0) {
                    name = "Selected place";
                }
                places.add(name, store.getLat(), store.getLon());
                render();
                Toast.makeText(FavoritesActivity.this, R.string.place_saved, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.favorite_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_FAVORITE_SEARCH && resultCode == RESULT_OK && intent != null) {
            String name = intent.getStringExtra("name");
            if (name == null || name.trim().length() == 0) {
                name = "Selected place";
            }
            places.add(name, intent.getDoubleExtra("lat", 0.0),
                    intent.getDoubleExtra("lon", 0.0));
            render();
            Toast.makeText(this, R.string.place_saved, Toast.LENGTH_SHORT).show();
        }
    }

    private void render() {
        list.removeAllViews();
        List<PlaceStore.Place> entries = places.getPlaces();
        int starred = 0;
        for (PlaceStore.Place place : entries) {
            if (place.favorite) {
                starred++;
            }
        }
        empty.setVisibility(starred == 0 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < entries.size(); i++) {
            final PlaceStore.Place place = entries.get(i);
            final int index = i;
            if (!place.favorite) {
                continue;
            }
            View row = LayoutInflater.from(this).inflate(R.layout.favorite_row, list, false);
            ((TextView) row.findViewById(R.id.favorite_name)).setText(place.name);
            ((TextView) row.findViewById(R.id.favorite_coords)).setText(
                    String.format(Locale.US, "%.4f, %.4f", place.lat, place.lon));

            ImageButton open = row.findViewById(R.id.favorite_open);
            open.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent result = new Intent();
                    result.putExtra("name", place.name);
                    result.putExtra("lat", place.lat);
                    result.putExtra("lon", place.lon);
                    setResult(RESULT_OK, result);
                    finish();
                }
            });

            ImageButton remove = row.findViewById(R.id.favorite_remove);
            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    places.remove(index);
                    render();
                    Toast.makeText(FavoritesActivity.this, R.string.place_removed,
                            Toast.LENGTH_SHORT).show();
                }
            });
            list.addView(row);
        }
    }
}
