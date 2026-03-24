package com.example.dudi_project.fragments;

import android.content.Intent;
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
    private TextView tvDistance, tvTime, tvPace, tvAvgSpm;
    private LineChart speedChart, spmChart;
    private ImageButton btnBack, btnShare;
    private int runId = -1;
    private Run currentRun;

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
        tvAvgSpm = view.findViewById(R.id.tv_avg_spm);
        
        speedChart = view.findViewById(R.id.speed_chart);
        spmChart = view.findViewById(R.id.spm_chart);
        
        btnBack = view.findViewById(R.id.btn_analysis_back);
        btnShare = view.findViewById(R.id.btn_analysis_share);

        initMap();
        setupChart(speedChart, "Speed (m/s)");
        setupChart(spmChart, "Cadence (SPM)");

        btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        btnShare.setOnClickListener(v -> shareRunStats());

        if (runId != -1) loadRunData();

        return view;
    }

    private void initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
    }

    private void setupChart(LineChart chart, String noDataText) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisRight().setEnabled(false);
        chart.setNoDataText(noDataText);
        chart.setNoDataTextColor(Color.GRAY);
    }

    private void loadRunData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            currentRun = AppDatabase.getInstance(requireContext()).runDao().getRunById(runId);
            if (currentRun != null && isAdded()) {
                requireActivity().runOnUiThread(() -> displayRun(currentRun));
            }
        });
    }

    private void shareRunStats() {
        if (currentRun == null) return;
        String shareBody = String.format(Locale.getDefault(),
                "MISSION COMPLETE! 🏃‍♂️🔥\nDistance: %.2f km\nAvg SPM: %d\nTracked by COACHES DONT PLAY...",
                currentRun.getDistance() / 1000f, calculateAverageSpm(currentRun.getSpmPoints()));
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(intent, "Share Mission"));
    }

    private void displayRun(Run run) {
        try {
            float distanceKm = run.getDistance() / 1000f;
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));

            long millis = run.getDurationMillis();
            tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", (millis / 60000), (millis / 1000) % 60));

            int pMin = (int) (run.getAveragePace() / 60);
            int pSec = (int) (run.getAveragePace() % 60);
            tvPace.setText(String.format(Locale.getDefault(), "%d:%02d", pMin, pSec));

            drawMap(run.getRoutePoints());
            drawChart(speedChart, run.getSpeedPoints(), "#FF6D00", "Speed");
            drawChart(spmChart, run.getSpmPoints(), "#00B0FF", "Cadence");
            
            int avgSpm = calculateAverageSpm(run.getSpmPoints());
            tvAvgSpm.setText("Avg: " + (avgSpm > 0 ? avgSpm : "--"));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawMap(String routeStr) {
        if (routeStr == null || routeStr.isEmpty()) return;
        Polyline polyline = new Polyline();
        polyline.getOutlinePaint().setColor(Color.parseColor("#00B0FF"));
        polyline.getOutlinePaint().setStrokeWidth(12f);
        List<GeoPoint> points = new ArrayList<>();
        for (String pair : routeStr.split(";")) {
            if (pair.contains(",")) {
                String[] latLng = pair.split(",");
                points.add(new GeoPoint(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1])));
            }
        }
        polyline.setPoints(points);
        map.getOverlays().add(polyline);
        if (!points.isEmpty()) {
            map.getController().setZoom(17.0);
            map.getController().setCenter(points.get(points.size() / 2));
        }
        map.invalidate();
    }

    private void drawChart(LineChart chart, String dataStr, String colorHex, String label) {
        if (dataStr == null || dataStr.isEmpty()) return;
        List<Entry> entries = new ArrayList<>();
        String[] values = dataStr.split(",");
        for (int i = 0; i < values.length; i++) {
            try {
                if (!values[i].trim().isEmpty()) {
                    entries.add(new Entry(i, Float.parseFloat(values[i])));
                }
            } catch (Exception ignored) {}
        }
        if (entries.isEmpty()) return;
        
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(Color.parseColor(colorHex));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor(colorHex));
        dataSet.setFillAlpha(50);
        
        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }

    private int calculateAverageSpm(String spmStr) {
        if (spmStr == null || spmStr.isEmpty()) return 0;
        String[] values = spmStr.split(",");
        float sum = 0;
        int count = 0;
        for (String val : values) {
            try {
                if (!val.trim().isEmpty()) {
                    sum += Float.parseFloat(val);
                    count++;
                }
            } catch (Exception ignored) {}
        }
        return count > 0 ? (int) (sum / count) : 0;
    }

    @Override public void onResume() { super.onResume(); map.onResume(); }
    @Override public void onPause() { super.onPause(); map.onPause(); }
}