package com.example.dudi_project.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LiveRunFragment extends Fragment {

    public interface LiveRunListener {
        void onRunFinished();
    }

    private LiveRunListener listener;
    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    
    private TextView tvTime, tvDistance, tvPace;
    private MaterialButton btnStop;

    private boolean isRunning = false;
    private long startTime = 0L;
    private float totalDistance = 0f;
    private Location lastLocation;
    private Polyline runPath;
    
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    startLocationUpdates();
                } else {
                    Toast.makeText(getContext(), "Location permission required for tracking", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        if (context instanceof LiveRunListener) {
            listener = (LiveRunListener) context;
        } else {
            throw new RuntimeException("Activity must implement LiveRunListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_run, container, false);

        map = view.findViewById(R.id.map_view);
        tvTime = view.findViewById(R.id.tv_run_time);
        tvDistance = view.findViewById(R.id.tv_run_distance);
        tvPace = view.findViewById(R.id.tv_run_pace);
        btnStop = view.findViewById(R.id.btn_stop_run);

        initMap();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnStop.setOnClickListener(v -> stopRun());

        checkPermissions();
        startRun();

        return view;
    }

    private void initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.getController().setZoom(18.0);
        
        runPath = new Polyline();
        runPath.getOutlinePaint().setColor(Color.parseColor("#FF6D00")); // Neon Orange
        runPath.getOutlinePaint().setStrokeWidth(12f);
        map.getOverlays().add(runPath);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            startLocationUpdates();
        }
    }

    private void startRun() {
        isRunning = true;
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopRun() {
        isRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        listener.onRunFinished();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(3000)
                .setFastestInterval(1500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateRunStats(location);
                }
            }
        };

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateRunStats(Location location) {
        GeoPoint newPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        map.getController().animateTo(newPoint);
        runPath.addPoint(newPoint);
        map.invalidate();

        if (lastLocation != null) {
            float distanceStep = lastLocation.distanceTo(location); // במטרים
            totalDistance += distanceStep;
            
            float distanceKm = totalDistance / 1000f;
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));
            
            // חישוב קצב (Pace)
            if (location.getSpeed() > 0.5) {
                float paceSecondsPerKm = 1000f / location.getSpeed();
                int paceMin = (int) (paceSecondsPerKm / 60);
                int paceSec = (int) (paceSecondsPerKm % 60);
                tvPace.setText(String.format(Locale.getDefault(), "%d:%02d", paceMin, paceSec));
            }
        }
        lastLocation = location;
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}