package com.example.dkhel_t3alem_raniabadi;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NearbyUsersActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private SupabaseConfig supabaseConfig;

    private LinearLayout usersListContainer;
    private TextView tvTitle, tvUserCount;
    private ProgressBar progressBar;

    private LatLng myLocation;
    private String languageFilter = "";
    private String userId = "";

    private static final int LOCATION_PERMISSION_CODE = 102;
    private static final double RAYON_RECHERCHE_KM = 10.0;
    private static final String TAG = "NearbyUsers";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_users);

        supabaseConfig = new SupabaseConfig(this);

        languageFilter = getIntent().getStringExtra("language_name");
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", "");

        tvTitle = findViewById(R.id.tvTitle);
        tvUserCount = findViewById(R.id.tvUserCount);
        usersListContainer = findViewById(R.id.usersListContainer);
        progressBar = findViewById(R.id.progressBar);

        if (languageFilter != null && !languageFilter.isEmpty()) {
            tvTitle.setText("🗺️ " + languageFilter + " près de vous");
        }

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.d(TAG, "📍 Position: " + location.getLatitude() +
                            ", " + location.getLongitude() +
                            " | Précision: " + location.getAccuracy() + "m");
                    updateMyPositionOnMap();
                    loadNearbyUsers();
                }
            }
        };

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            Toast.makeText(this, "🔄 Rafraîchissement...", Toast.LENGTH_SHORT).show();
            if (checkLocationPermission()) {
                getLastLocationImmediate();
            }
            loadNearbyUsers();
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
            getLastLocationImmediate();
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    private void getLastLocationImmediate() {
        if (!checkLocationPermission()) return;

        locationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "📍 Dernière position: " + location.getLatitude() +
                                ", " + location.getLongitude());
                        updateMyPositionOnMap();
                        loadNearbyUsers();
                    } else {
                        Log.d(TAG, "⏳ Aucune position en cache");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Erreur: " + e.getMessage());
                });
    }

    private void startLocationUpdates() {
        if (!checkLocationPermission()) return;

        mMap.setMyLocationEnabled(true);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_CODE);
    }

    private void updateMyPositionOnMap() {
        if (mMap != null && myLocation != null) {
            mMap.clear();

            mMap.addMarker(new MarkerOptions()
                    .position(myLocation)
                    .title("📍 Vous êtes ici")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            mMap.addCircle(new CircleOptions()
                    .center(myLocation)
                    .radius(RAYON_RECHERCHE_KM * 1000)
                    .strokeColor(Color.parseColor("#58CC02"))
                    .strokeWidth(2)
                    .fillColor(Color.argb(30, 88, 204, 2)));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14));
        }
    }

    private void loadNearbyUsers() {
        if (myLocation == null) return;

        progressBar.setVisibility(View.VISIBLE);

        double latMin = myLocation.latitude - (RAYON_RECHERCHE_KM / 111.0);
        double latMax = myLocation.latitude + (RAYON_RECHERCHE_KM / 111.0);
        double lonMin = myLocation.longitude - (RAYON_RECHERCHE_KM /
                (111.0 * Math.cos(Math.toRadians(myLocation.latitude))));
        double lonMax = myLocation.longitude + (RAYON_RECHERCHE_KM /
                (111.0 * Math.cos(Math.toRadians(myLocation.latitude))));

        supabaseConfig.getNearbyUsers(latMin, latMax, lonMin, lonMax,
                new SupabaseConfig.DataCallback() {
                    @Override
                    public void onSuccess(JSONArray response) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            afficherUtilisateurs(response);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(NearbyUsersActivity.this,
                                    "Erreur chargement", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void afficherUtilisateurs(JSONArray users) {
        usersListContainer.removeAllViews();
        List<NearbyUser> nearbyUsers = new ArrayList<>();

        for (int i = 0; i < users.length(); i++) {
            try {
                JSONObject user = users.getJSONObject(i);
                String uId = user.optString("user_id", "");
                double lat = user.optDouble("latitude", 0);
                double lon = user.optDouble("longitude", 0);

                // ✅ Ne pas s'afficher soi-même
                if (uId.equals(userId)) continue;

                // ✅ Utiliser le user_id comme nom (ou faire une 2ème requête)
                String name = "Apprenant " + uId.substring(0, 8);
                int score = 0;

                float[] results = new float[1];
                Location.distanceBetween(
                        myLocation.latitude, myLocation.longitude,
                        lat, lon, results);
                float distance = results[0];

                NearbyUser nearbyUser = new NearbyUser(uId, name, lat, lon, score, distance);
                nearbyUsers.add(nearbyUser);

                if (mMap != null) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(lat, lon))
                            .title(name)
                            .snippet(nearbyUser.getFormattedDistance())
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_AZURE)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Collections.sort(nearbyUsers, (u1, u2) ->
                Float.compare(u1.getDistance(), u2.getDistance()));

        tvUserCount.setText(nearbyUsers.size() + " apprenant(s) trouvé(s)");

        if (nearbyUsers.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("😔 Aucun apprenant dans " + (int)RAYON_RECHERCHE_KM + " km");
            tvEmpty.setTextSize(16);
            tvEmpty.setTextColor(Color.parseColor("#999999"));
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(0, 40, 0, 40);
            usersListContainer.addView(tvEmpty);
        } else {
            for (NearbyUser user : nearbyUsers) {
                usersListContainer.addView(createUserCard(user));
            }
        }
    }

    private CardView createUserCard(NearbyUser user) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);
        card.setRadius(16);
        card.setCardElevation(4);
        card.setUseCompatPadding(true);
        card.setBackgroundColor(Color.WHITE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(20, 16, 20, 16);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvAvatar = new TextView(this);
        tvAvatar.setText("👤");
        tvAvatar.setTextSize(40);
        tvAvatar.setGravity(Gravity.CENTER);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setPadding(16, 0, 0, 0);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvName = new TextView(this);
        tvName.setText(user.getUsername());
        tvName.setTextSize(16);
        tvName.setTextColor(Color.parseColor("#333333"));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvScore = new TextView(this);
        tvScore.setText("⭐ Score: " + user.getScore());
        tvScore.setTextSize(14);
        tvScore.setTextColor(Color.parseColor("#666666"));

        infoLayout.addView(tvName);
        infoLayout.addView(tvScore);

        TextView tvDistance = new TextView(this);
        tvDistance.setText("📍 " + user.getFormattedDistance());
        tvDistance.setTextSize(14);
        tvDistance.setTextColor(Color.parseColor("#58CC02"));
        tvDistance.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDistance.setGravity(Gravity.END);

        layout.addView(tvAvatar);
        layout.addView(infoLayout);
        layout.addView(tvDistance);
        card.addView(layout);

        return card;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    onMapReady(mMap);
                }
            } else {
                Toast.makeText(this, "Permission localisation refusée", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
    }
}