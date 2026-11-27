package com.baroweather.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** City search backed by Open-Meteo geocoding. */
public final class SearchActivity extends BaseActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private EditText input;
    private TextView status;
    private ListView results;
    private final List<WeatherRepository.Place> places = new ArrayList<WeatherRepository.Place>();
    private PlaceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        input = findViewById(R.id.search_input);
        status = findViewById(R.id.search_status);
        results = findViewById(R.id.results);
        adapter = new PlaceAdapter();
        results.setAdapter(adapter);
        status.setText("Type a town or city, then search.");

        findViewById(R.id.search_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runSearch();
            }
        });
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                runSearch();
                return true;
            }
        });
        results.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WeatherRepository.Place place = places.get(position);
                Intent result = new Intent();
                result.putExtra("name", place.name
                        + (place.detail.length() > 0 ? ", " + place.detail : ""));
                result.putExtra("lat", place.latitude);
                result.putExtra("lon", place.longitude);
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void runSearch() {
        final String query = input.getText().toString().trim();
        if (query.length() < 2) {
            Toast.makeText(this, "Enter at least two characters.", Toast.LENGTH_SHORT).show();
            return;
        }
        status.setText(R.string.loading);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<WeatherRepository.Place> found = WeatherRepository.search(query);
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            places.clear();
                            places.addAll(found);
                            adapter.notifyDataSetChanged();
                            status.setText(places.isEmpty()
                                    ? getString(R.string.no_match)
                                    : places.size() + " result(s)");
                        }
                    });
                } catch (final Exception error) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            places.clear();
                            adapter.notifyDataSetChanged();
                            status.setText(error.getMessage() == null
                                    ? "Search failed." : error.getMessage());
                        }
                    });
                }
            }
        });
    }

    private final class PlaceAdapter extends ArrayAdapter<WeatherRepository.Place> {
        PlaceAdapter() {
            super(SearchActivity.this, 0, places);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.search_result_item, parent, false);
            }
            WeatherRepository.Place place = getItem(position);
            ((TextView) view.findViewById(R.id.result_name)).setText(place.name);
            ((TextView) view.findViewById(R.id.result_detail)).setText(place.detail);
            return view;
        }
    }
}
