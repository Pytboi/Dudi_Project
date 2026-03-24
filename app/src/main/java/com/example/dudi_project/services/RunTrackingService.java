package com.example.dudi_project.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import com.example.dudi_project.MainActivity;
import com.example.dudi_project.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RunTrackingService extends Service implements SensorEventListener, TextToSpeech.OnInitListener {

    public static final String ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE";
    public static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
    private static final String NOTIFICATION_CHANNEL_ID = "run_tracking_channel";
    private static final int NOTIFICATION_ID = 1;

    private final IBinder binder = new LocalBinder();

    // Livedata for UI
    public static MutableLiveData<Boolean> isTracking = new MutableLiveData<>(false);
    public static MutableLiveData<List<GeoPoint>> pathPoints = new MutableLiveData<>(new ArrayList<>());
    public static MutableLiveData<Float> distance = new MutableLiveData<>(0f);
    public static MutableLiveData<Long> timeInMillis = new MutableLiveData<>(0L);
    public static MutableLiveData<Integer> stepsCount = new MutableLiveData<>(0);
    public static MutableLiveData<StringBuilder> spmPoints = new MutableLiveData<>(new StringBuilder());

    private FusedLocationProviderClient fusedLocationClient;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private TextToSpeech tts;
    private int initialSteps = -1;
    private long startTime = 0L;
    private float lastAnnouncedDistance = 0f;

    // 1. BroadcastReceiver לסוללה נמוכה
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
                String msg = "Battery is low! Finish your mission soon to save data.";
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                speak(msg);
            }
        }
    };

    public class LocalBinder extends Binder {
        public RunTrackingService getService() {
            return RunTrackingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        
        // אתחול TTS
        tts = new TextToSpeech(this, this);

        // רישום BroadcastReceiver לסוללה
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_LOW);
        registerReceiver(batteryReceiver, filter);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
        }
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_START_OR_RESUME_SERVICE:
                    startForegroundService();
                    break;
                case ACTION_STOP_SERVICE:
                    stopTracking();
                    break;
            }
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        isTracking.postValue(true);
        startTime = System.currentTimeMillis();
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Run Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }

        startForeground(NOTIFICATION_ID, getNotification("Starting mission..."));
        startLocationUpdates();
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
        speak("Mission started. Good luck!");
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(1500)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException ignored) {}
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (Boolean.TRUE.equals(isTracking.getValue())) {
                for (Location location : locationResult.getLocations()) {
                    updateTrackingData(location);
                }
            }
        }
    };

    private void updateTrackingData(Location location) {
        List<GeoPoint> points = pathPoints.getValue();
        if (points == null) points = new ArrayList<>();
        GeoPoint newPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        points.add(newPoint);
        pathPoints.postValue(points);

        if (points.size() > 1) {
            GeoPoint lastPoint = points.get(points.size() - 2);
            float[] results = new float[1];
            Location.distanceBetween(lastPoint.getLatitude(), lastPoint.getLongitude(),
                    newPoint.getLatitude(), newPoint.getLongitude(), results);
            
            float currentDist = distance.getValue() != null ? distance.getValue() : 0f;
            float newDist = currentDist + results[0];
            distance.postValue(newDist);
            
            long currentTime = System.currentTimeMillis() - startTime;
            timeInMillis.postValue(currentTime);
            
            updateNotification(newDist, currentTime);

            // 2. לוגיקה של TextToSpeech - הכרזה כל קילומטר
            float distKm = newDist / 1000f;
            if (distKm >= lastAnnouncedDistance + 1.0f) {
                lastAnnouncedDistance = (float) Math.floor(distKm);
                String speech = String.format(Locale.US, "Distance %.0f kilometers completed.", lastAnnouncedDistance);
                speak(speech);
            }
        }
    }

    private void updateNotification(float dist, long time) {
        float distKm = dist / 1000f;
        String pace = "0:00";
        if (distKm > 0) {
            long totalSeconds = (long) ((time / 1000f) / distKm);
            pace = String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60);
        }
        String content = String.format(Locale.getDefault(), "Distance: %.2f km | Pace: %s /km", distKm, pace);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, getNotification(content));
    }

    private Notification getNotification(String content) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("MISSION IN PROGRESS")
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void stopTracking() {
        isTracking.postValue(false);
        fusedLocationClient.removeLocationUpdates(locationCallback);
        sensorManager.unregisterListener(this);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {}
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (initialSteps == -1) initialSteps = (int) event.values[0];
            stepsCount.postValue((int) event.values[0] - initialSteps);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        stopTracking();
        super.onDestroy();
    }
}