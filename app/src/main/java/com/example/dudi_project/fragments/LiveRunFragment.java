package com.example.dudi_project.fragments;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.dudi_project.R;
import com.example.dudi_project.data.AppDatabase;
import com.example.dudi_project.data.Run;
import com.example.dudi_project.services.RunTrackingService;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class LiveRunFragment extends Fragment {

    public interface LiveRunListener {
        void onRunFinished();
    }

    private LiveRunListener listener;
    private MapView map;
    private TextView tvTime, tvDistance, tvPace;
    private Polyline runPath;
    private Marker userMarker;

    private RunTrackingService runService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RunTrackingService.LocalBinder binder = (RunTrackingService.LocalBinder) service;
            runService = binder.getService();
            isBound = true;
            observeServiceData();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false))) {
                    startTrackingService();
                } else {
                    Toast.makeText(getContext(), "GPS permission is required", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        if (context instanceof LiveRunListener) {
            listener = (LiveRunListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_run, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        map = view.findViewById(R.id.map_view);
        tvTime = view.findViewById(R.id.tv_run_time);
        tvDistance = view.findViewById(R.id.tv_run_distance);
        tvPace = view.findViewById(R.id.tv_run_pace);

        initMap();

        view.findViewById(R.id.btn_stop_run).setOnClickListener(v -> stopRunAndSave());

        checkPermissions();
    }

    private void initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.getController().setZoom(18.0);

        runPath = new Polyline();
        runPath.getOutlinePaint().setColor(Color.parseColor("#00B0FF"));
        runPath.getOutlinePaint().setStrokeWidth(12f);
        map.getOverlays().add(runPath);

        userMarker = new Marker(map);
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        userMarker.setIcon(ContextCompat.getDrawable(requireContext(), org.osmdroid.library.R.drawable.person));
        map.getOverlays().add(userMarker);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            startTrackingService();
        }
    }

    private void startTrackingService() {
        Intent intent = new Intent(requireContext(), RunTrackingService.class);
        intent.setAction(RunTrackingService.ACTION_START_OR_RESUME_SERVICE);
        requireContext().startService(intent);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void observeServiceData() {
        RunTrackingService.pathPoints.observe(getViewLifecycleOwner(), points -> {
            if (points != null && !points.isEmpty()) {
                runPath.setPoints(points);
                GeoPoint lastPoint = points.get(points.size() - 1);
                userMarker.setPosition(lastPoint);
                map.getController().animateTo(lastPoint);
                map.invalidate();
            }
        });

        RunTrackingService.distance.observe(getViewLifecycleOwner(), dist -> {
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", dist / 1000f));
            updatePace();
        });

        RunTrackingService.timeInMillis.observe(getViewLifecycleOwner(), millis -> {
            int seconds = (int) (millis / 1000) % 60;
            int minutes = (int) (millis / (1000 * 60));
            tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            updatePace();
        });
    }

    private void updatePace() {
        Float dist = RunTrackingService.distance.getValue();
        Long time = RunTrackingService.timeInMillis.getValue();
        if (dist != null && time != null && dist > 0) {
            long totalSeconds = (long) ((time / 1000f) / (dist / 1000f));
            tvPace.setText(String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60));
        }
    }

    private void stopRunAndSave() {
        if (runService != null) {
            float finalDist = RunTrackingService.distance.getValue();
            long finalTime = RunTrackingService.timeInMillis.getValue();
            List<GeoPoint> points = RunTrackingService.pathPoints.getValue();
            int totalSteps = RunTrackingService.stepsCount.getValue() != null ? RunTrackingService.stepsCount.getValue() : 0;
            String spm = RunTrackingService.spmPoints.getValue().toString();

            StringBuilder routeSb = new StringBuilder();
            if (points != null) {
                for (GeoPoint p : points) {
                    routeSb.append(p.getLatitude()).append(",").append(p.getLongitude()).append(";");
                }
            }

            float avgPace = 0;
            if (finalDist > 0) avgPace = (finalTime / 1000f) / (finalDist / 1000f);

            Run runToSave = new Run(System.currentTimeMillis(), finalDist, finalTime, avgPace, routeSb.toString(), "", totalSteps, spm);

            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase.getInstance(requireContext()).runDao().insert(runToSave);
                requireActivity().runOnUiThread(() -> {
                    Intent intent = new Intent(requireContext(), RunTrackingService.class);
                    intent.setAction(RunTrackingService.ACTION_STOP_SERVICE);
                    requireContext().startService(intent);
                    listener.onRunFinished();
                });
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBound) {
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override public void onResume() { super.onResume(); map.onResume(); }
    @Override public void onPause() { super.onPause(); map.onPause(); }
}