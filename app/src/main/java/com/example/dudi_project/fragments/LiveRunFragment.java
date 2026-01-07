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
import android.view.WindowManager;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.Locale;
import java.util.concurrent.Executors;

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
    private Marker userMarker;
    private StringBuilder speedHistory = new StringBuilder(); 
    
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                if (tvTime != null) {
                    tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                }
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false))) {
                    startLocationUpdates();
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
        
        // נעילת מסך דולק בזמן הריצה
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        map = view.findViewById(R.id.map_view);
        tvTime = view.findViewById(R.id.tv_run_time);
        tvDistance = view.findViewById(R.id.tv_run_distance);
        tvPace = view.findViewById(R.id.tv_run_pace);
        btnStop = view.findViewById(R.id.btn_stop_run);

        initMap();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnStop.setOnClickListener(v -> stopRunAndSave());

        checkPermissionsAndStart();
    }

    private void initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.getController().setZoom(18.0);
        
        runPath = new Polyline();
        runPath.getOutlinePaint().setColor(Color.parseColor("#00B0FF")); // Light Blue
        runPath.getOutlinePaint().setStrokeWidth(12f);
        map.getOverlays().add(runPath);

        userMarker = new Marker(map);
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        userMarker.setIcon(ContextCompat.getDrawable(requireContext(), org.osmdroid.library.R.drawable.person));
        map.getOverlays().add(userMarker);
    }

    private void checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            startLocationUpdates();
        }
        startTimer();
    }

    private void startTimer() {
        if (!isRunning) {
            isRunning = true;
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }

    private void stopRunAndSave() {
        // שחרור נעילת מסך בסיום הריצה
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        isRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        long durationMillis = System.currentTimeMillis() - startTime;
        float distanceKm = totalDistance / 1000f;
        float avgPace = 0;
        if (distanceKm > 0) {
            avgPace = (durationMillis / 1000f) / distanceKm;
        }

        StringBuilder routeSb = new StringBuilder();
        for (GeoPoint p : runPath.getActualPoints()) {
            routeSb.append(p.getLatitude()).append(",").append(p.getLongitude()).append(";");
        }

        final Run runToSave = new Run(
                System.currentTimeMillis(),
                totalDistance,
                durationMillis,
                avgPace,
                routeSb.toString(),
                speedHistory.toString()
        );

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(requireContext()).runDao().insert(runToSave);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Run Saved Successfully!", Toast.LENGTH_SHORT).show();
                    listener.onRunFinished();
                }
            });
        });
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(1500)
                .build();

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
        if (!isRunning) return;

        GeoPoint newPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        map.getController().animateTo(newPoint);
        
        runPath.addPoint(newPoint);
        
        if (userMarker != null) {
            userMarker.setPosition(newPoint);
        }
        
        map.invalidate();

        speedHistory.append(location.getSpeed()).append(",");

        if (lastLocation != null) {
            float distanceStep = lastLocation.distanceTo(location);
            totalDistance += distanceStep;
            
            float distanceKm = totalDistance / 1000f;
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));
            
            float speed = location.getSpeed();
            if (speed > 0.5) {
                float paceSecondsPerKm = 1000f / speed;
                int paceMin = (int) (paceSecondsPerKm / 60);
                int paceSec = (int) (paceSecondsPerKm % 60);
                tvPace.setText(String.format(Locale.getDefault(), "%d:%02d", paceMin, paceSec));
            }
        }
        lastLocation = location;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // שחרור נעילת מסך אם יוצאים מהפרגמנט באמצע
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
