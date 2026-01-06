package com.example.dudi_project.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dudi_project.R;
import com.example.dudi_project.data.AppDatabase;
import com.example.dudi_project.data.Run;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class RunAnalysisFragment extends Fragment {

    private MapView map;
    private TextView tvDistance, tvTime, tvPace;
    private LineChart speedChart;
    private ImageButton btnBack;
    private int runId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            runId = getArguments().getInt("RUN_ID", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_run_analysis, container, false);

        map = view.findViewById(R.id.analysis_map);
        tvDistance = view.findViewById(R.id.tv_analysis_distance);
        tvTime = view.findViewById(R.id.tv_analysis_time);
        tvPace = view.findViewById(R.id.tv_analysis_pace);
        speedChart = view.findViewById(R.id.speed_chart);
        btnBack = view.findViewById(R.id.btn_analysis_back);

        initMap();
        setupChart();

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        if (runId != -1) {
            loadRunData();
        }

        return view;
    }

    private void initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
    }

    private void setupChart() {
        speedChart.getDescription().setEnabled(false);
        speedChart.setDrawGridBackground(false);
        speedChart.getLegend().setEnabled(false);
        speedChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        speedChart.getXAxis().setDrawGridLines(false);
        speedChart.getXAxis().setTextColor(Color.WHITE);
        speedChart.getAxisLeft().setTextColor(Color.WHITE);
        speedChart.getAxisRight().setEnabled(false);
    }

    private void loadRunData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Run run = AppDatabase.getInstance(requireContext()).runDao().getRunById(runId);
            if (run != null) {
                requireActivity().runOnUiThread(() -> displayRun(run));
            }
        });
    }

    private void displayRun(Run run) {
        float distanceKm = run.getDistance() / 1000f;
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));

        long millis = run.getDurationMillis();
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) (millis / (1000 * 60));
        tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        int paceMin = (int) (run.getAveragePace() / 60);
        int paceSec = (int) (run.getAveragePace() % 60);
        tvPace.setText(String.format(Locale.getDefault(), "%d:%02d", paceMin, paceSec));

        // Draw Map Polyline
        String routeStr = run.getRoutePoints();
        if (routeStr != null && !routeStr.isEmpty()) {
            Polyline polyline = new Polyline();
            polyline.getOutlinePaint().setColor(Color.parseColor("#FF6D00"));
            polyline.getOutlinePaint().setStrokeWidth(12f);

            List<GeoPoint> points = new ArrayList<>();
            String[] coordPairs = routeStr.split(";");
            for (String pair : coordPairs) {
                if (pair.contains(",")) {
                    String[] latLng = pair.split(",");
                    points.add(new GeoPoint(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1])));
                }
            }
            polyline.setPoints(points);
            map.getOverlays().add(polyline);

            if (!points.isEmpty()) {
                map.getController().setZoom(17.0);
                map.getController().setCenter(points.get(points.size()/2));
            }
        }
        map.invalidate();

        // Draw Speed Chart
        String speedStr = run.getSpeedPoints();
        if (speedStr != null && !speedStr.isEmpty()) {
            List<Entry> entries = new ArrayList<>();
            String[] speeds = speedStr.split(",");
            for (int i = 0; i < speeds.length; i++) {
                if (!speeds[i].isEmpty()) {
                    entries.add(new Entry(i, Float.parseFloat(speeds[i])));
                }
            }

            LineDataSet dataSet = new LineDataSet(entries, "Speed");
            dataSet.setColor(Color.parseColor("#FF6D00"));
            dataSet.setLineWidth(3f);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.parseColor("#FF6D00"));
            dataSet.setFillAlpha(50);

            speedChart.setData(new LineData(dataSet));
            speedChart.invalidate();
        }
    }

    @Override public void onResume() { super.onResume(); map.onResume(); }
    @Override public void onPause() { super.onPause(); map.onPause(); }
}