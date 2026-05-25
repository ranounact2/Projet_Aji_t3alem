package com.example.dkhel_t3alem_raniabadi;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gms.location.*;
import org.json.JSONObject;

public class LocationService extends Service {

    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private SupabaseConfig supabaseConfig;
    private String userId;
    private static final String TAG = "LocationService";

    @Override
    public void onCreate() {
        super.onCreate();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        supabaseConfig = new SupabaseConfig(this);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", "");

        Log.d(TAG, "✅ Service créé - UserID: " + userId);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null && !userId.isEmpty()) {
                    mettreAJourPosition(location.getLatitude(), location.getLongitude());
                }
            }
        };

        demarrerLocalisation();
    }

    private void demarrerLocalisation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(60000);
        locationRequest.setFastestInterval(30000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        try {
            locationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
            Log.d(TAG, "📍 Localisation démarrée");
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Permission manquante");
        }
    }

    private void mettreAJourPosition(double latitude, double longitude) {
        supabaseConfig.updateUserLocation(userId, latitude, longitude,
                new SupabaseConfig.AuthCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.d(TAG, "📍 Position envoyée: " + latitude + ", " + longitude);
                    }

                    @Override
                    public void onError(String errorMessage, byte[] dataErrors) {
                        Log.e(TAG, "❌ Erreur: " + errorMessage);
                    }
                });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationClient != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
        Log.d(TAG, "⏹️ Service arrêté");
    }
}