package com.example.dudi_project.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.dudi_project.R;
import com.example.dudi_project.data.AppDatabase;
import com.example.dudi_project.data.Run;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    public interface DashboardListener {
        void onStartRun();
        void onViewRunAnalysis(int runId);
        void onOpenProfile();
    }

    private DashboardListener listener;
    private TextView tvWelcome, tvLastDistance, tvLastPace;
    private TextView tvRec1k, tvRecord5k, tvRecordLongest, tvRecordCount;
    private MaterialCardView cardStats, cardProfile;
    private ImageView ivProfilePic;

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

        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvLastDistance = view.findViewById(R.id.tv_dash_last_distance);
        tvLastPace = view.findViewById(R.id.tv_dash_last_pace);
        cardStats = view.findViewById(R.id.card_stats);
        cardProfile = view.findViewById(R.id.card_small_profile);
        ivProfilePic = view.findViewById(R.id.iv_small_profile_pic);

        tvRec1k = view.findViewById(R.id.tv_record_1k);
        tvRecord5k = view.findViewById(R.id.tv_record_5k);
        tvRecordLongest = view.findViewById(R.id.tv_record_longest);
        tvRecordCount = view.findViewById(R.id.tv_record_count);

        setupWelcomeMessage();

        cardProfile.setOnClickListener(v -> {
            if (listener != null) listener.onOpenProfile();
        });

        Button btnStartRun = view.findViewById(R.id.btn_start_run);
        if (btnStartRun != null) {
            btnStartRun.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStartRun();
                }
            });
        }

        loadDashboardData();

        return view;
    }

    private void setupWelcomeMessage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null) {
                String firstName = user.getDisplayName().split(" ")[0];
                tvWelcome.setText("שלום, " + firstName);
            }
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivProfilePic);
            }
        }
    }

    private void loadDashboardData() {
        AppDatabase.getInstance(requireContext()).runDao().getAllRuns().observe(getViewLifecycleOwner(), runs -> {
            if (runs != null && !runs.isEmpty()) {
                displayLastRun(runs.get(0));
                calculatePersonalRecords(runs);
            } else {
                tvLastDistance.setText("0.00 KM");
                tvLastPace.setText("NO MISSIONS YET");
                tvRecordCount.setText("0");
            }
        });
    }

    private void displayLastRun(Run lastRun) {
        float distanceKm = lastRun.getDistance() / 1000f;
        tvLastDistance.setText(String.format(Locale.getDefault(), "%.2f KM", distanceKm));

        int paceMin = (int) (lastRun.getAveragePace() / 60);
        int paceSec = (int) (lastRun.getAveragePace() % 60);
        tvLastPace.setText(String.format(Locale.getDefault(), "PACE: %d:%02d /km", paceMin, paceSec));

        cardStats.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewRunAnalysis(lastRun.getId());
            }
        });
    }

    private void calculatePersonalRecords(List<Run> runs) {
        float maxDistance = 0;
        float best1kPace = Float.MAX_VALUE;
        float best5kPace = Float.MAX_VALUE;

        for (Run run : runs) {
            float distKm = run.getDistance() / 1000f;
            float pace = run.getAveragePace();
            if (distKm > maxDistance) maxDistance = distKm;
            if (distKm >= 1.0f && pace < best1kPace) best1kPace = pace;
            if (distKm >= 5.0f && pace < best5kPace) best5kPace = pace;
        }

        tvRecordLongest.setText(String.format(Locale.getDefault(), "%.1f km", maxDistance));
        tvRecordCount.setText(String.valueOf(runs.size()));

        if (best1kPace != Float.MAX_VALUE) {
            tvRec1k.setText(String.format(Locale.getDefault(), "%d:%02d", (int)best1kPace/60, (int)best1kPace%60));
        }
        if (best5kPace != Float.MAX_VALUE) {
            tvRecord5k.setText(String.format(Locale.getDefault(), "%d:%02d", (int)best5kPace/60, (int)best5kPace%60));
        }
    }
}