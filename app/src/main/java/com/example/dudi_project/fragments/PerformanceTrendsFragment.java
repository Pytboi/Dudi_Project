package com.example.dudi_project.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dudi_project.R;
import com.example.dudi_project.data.AppDatabase;
import com.example.dudi_project.data.Run;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerformanceTrendsFragment extends Fragment {

    private BarChart distanceChart;
    private LineChart paceChart;
    private TextView tvNoData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_performance_trends, container, false);

        distanceChart = view.findViewById(R.id.distance_bar_chart);
        paceChart = view.findViewById(R.id.pace_line_chart);
        tvNoData = view.findViewById(R.id.tv_no_data_trends);

        setupCharts();
        loadData();

        return view;
    }

    private void setupCharts() {
        // Distance Chart Setup
        distanceChart.getDescription().setEnabled(false);
        distanceChart.getLegend().setEnabled(false);
        distanceChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        distanceChart.getXAxis().setTextColor(Color.WHITE);
        distanceChart.getXAxis().setDrawGridLines(false);
        distanceChart.getAxisLeft().setTextColor(Color.WHITE);
        distanceChart.getAxisRight().setEnabled(false);

        // Pace Chart Setup
        paceChart.getDescription().setEnabled(false);
        paceChart.getLegend().setEnabled(false);
        paceChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        paceChart.getXAxis().setTextColor(Color.WHITE);
        paceChart.getXAxis().setDrawGridLines(false);
        paceChart.getAxisLeft().setTextColor(Color.WHITE);
        paceChart.getAxisRight().setEnabled(false);
    }

    private void loadData() {
        AppDatabase.getInstance(requireContext()).runDao().getAllRuns().observe(getViewLifecycleOwner(), runs -> {
            if (runs == null || runs.size() < 2) {
                tvNoData.setVisibility(View.VISIBLE);
                distanceChart.setVisibility(View.GONE);
                paceChart.setVisibility(View.GONE);
            } else {
                tvNoData.setVisibility(View.GONE);
                distanceChart.setVisibility(View.VISIBLE);
                paceChart.setVisibility(View.VISIBLE);
                
                // הופכים את הרשימה כדי להציג מהישן לחדש בגרף
                List<Run> chronologicalRuns = new ArrayList<>(runs);
                Collections.reverse(chronologicalRuns);
                
                displayDistanceTrends(chronologicalRuns);
                displayPaceTrends(chronologicalRuns);
            }
        });
    }

    private void displayDistanceTrends(List<Run> runs) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < runs.size(); i++) {
            entries.add(new BarEntry(i, runs.get(i).getDistance() / 1000f));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Distance (km)");
        dataSet.setColor(Color.parseColor("#FF6D00")); // Neon Orange
        dataSet.setValueTextColor(Color.WHITE);
        
        BarData data = new BarData(dataSet);
        distanceChart.setData(data);
        distanceChart.invalidate();
    }

    private void displayPaceTrends(List<Run> runs) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < runs.size(); i++) {
            // קצב בדקות לקילומטר
            float paceInMinutes = runs.get(i).getAveragePace() / 60f;
            entries.add(new Entry(i, paceInMinutes));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Pace (min/km)");
        dataSet.setColor(Color.parseColor("#00B0FF")); // Light Blue
        dataSet.setCircleColor(Color.WHITE);
        dataSet.setLineWidth(3f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        LineData data = new LineData(dataSet);
        paceChart.setData(data);
        paceChart.invalidate();
    }
}