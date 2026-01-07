package com.example.dudi_project.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dudi_project.R;
import com.example.dudi_project.data.AppDatabase;
import com.example.dudi_project.data.Run;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class DashboardFragment extends Fragment {

    public interface DashboardListener {
        void onStartRun();
        void onViewRunAnalysis(int runId);
    }

    private DashboardListener listener;
    private TextView tvLastDistance, tvLastPace;
    private MaterialCardView cardStats;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof DashboardListener) {
            listener = (DashboardListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement DashboardListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvLastDistance = view.findViewById(R.id.tv_dash_last_distance);
        tvLastPace = view.findViewById(R.id.tv_dash_last_pace);
        cardStats = view.findViewById(R.id.card_stats);

        TextView tvWelcome = view.findViewById(R.id.tv_welcome);
        if (tvWelcome != null) {
            tvWelcome.setText("LACE UP.");
        }

        Button btnStartRun = view.findViewById(R.id.btn_start_run);
        if (btnStartRun != null) {
            btnStartRun.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStartRun();
                }
            });
        }

        loadLastRun();

        return view;
    }

    private void loadLastRun() {
        AppDatabase.getInstance(requireContext()).runDao().getAllRuns().observe(getViewLifecycleOwner(), runs -> {
            if (runs != null && !runs.isEmpty()) {
                Run lastRun = runs.get(0);
                
                float distanceKm = lastRun.getDistance() / 1000f;
                tvLastDistance.setText(String.format(Locale.getDefault(), "%.2f KM", distanceKm));

                int paceMin = (int) (lastRun.getAveragePace() / 60);
                int paceSec = (int) (lastRun.getAveragePace() % 60);
                tvLastPace.setText(String.format(Locale.getDefault(), "PACE: %d:%02d /km", paceMin, paceSec));

                // הוספת לחיצה על הכרטיס למעבר לניתוח הריצה
                cardStats.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onViewRunAnalysis(lastRun.getId());
                    }
                });
            } else {
                tvLastDistance.setText("0.00 KM");
                tvLastPace.setText("NO SESSIONS YET");
                cardStats.setOnClickListener(null);
            }
        });
    }
}