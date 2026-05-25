package com.example.dkhel_t3alem_raniabadi;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class
SupabaseConfig {
    private static final String URL = "http://10.28.249.213:8000";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzc4OTUyMDA3LCJleHAiOjE5MzY2MzIwMDd9.Bs6OGUsG5I23DvRFonizcRZoq0yJdAZCao8sl-7JOq0";

    private RequestQueue queue;

    public SupabaseConfig(Context context) {
        queue = Volley.newRequestQueue(context);
    }

    public void signIn(String email, String password, AuthCallback callback) {
        String url = URL + "/auth/v1/token?grant_type=password";
        JSONObject data = new JSONObject();
        try { data.put("email", email); data.put("password", password); } catch (Exception e) {}
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, data, callback::onSuccess,
                error -> callback.onError(
                        error.getMessage(),
                        error.networkResponse != null ? error.networkResponse.data : null
                )
        ) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>(); h.put("apikey", API_KEY); h.put("Content-Type", "application/json"); return h;
            }
        };
        queue.add(req);
    }

    public void signUp(String fullname, String tel, String email, String password, AuthCallback callback) {
        String url = URL + "/auth/v1/signup";
        JSONObject data = new JSONObject();
        try {
            data.put("display_name", fullname);
            data.put("email", email);
            data.put("password", password);
        } catch (Exception e) {}
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, data, callback::onSuccess,
                error -> callback.onError(error.getMessage(), error.networkResponse != null ? error.networkResponse.data : null)) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>(); h.put("apikey", API_KEY); h.put("Content-Type", "application/json"); return h;
            }
        };
        queue.add(req);
    }

    public void getLanguages(DataCallback callback) {
        String url = URL + "/rest/v1/langues?select=*";
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> callback.onSuccess(response),
                error -> callback.onError(error.getMessage())) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>(); h.put("apikey", API_KEY); h.put("Authorization", "Bearer " + API_KEY); return h;
            }
        };
        queue.add(req);
    }

    public void getQuestionsByLanguage(String languageId, DataCallback callback) {
        String url = URL + "/rest/v1/questions?langue_id=eq." + languageId + "&select=*";

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                callback::onSuccess,
                error -> {
                    String detail = error.getMessage();
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        detail = new String(error.networkResponse.data);
                    }
                    callback.onError(detail);
                }) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>(); h.put("apikey", API_KEY); h.put("Authorization", "Bearer " + API_KEY); return h;
            }
        };
        queue.add(req);
    }

    // Méthode pour les utilisateurs proches
    public void getNearbyUsers(double latMin, double latMax, double lonMin, double lonMax,
                               DataCallback callback) {
        String url = URL + "/rest/v1/user_locations?select=user_id,latitude,longitude" +
                "&latitude=gt." + latMin +
                "&latitude=lt." + latMax +
                "&longitude=gt." + lonMin +
                "&longitude=lt." + lonMax;

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                callback::onSuccess,
                error -> callback.onError(error.getMessage())) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("apikey", API_KEY);
                h.put("Authorization", "Bearer " + API_KEY);
                return h;
            }
        };
        queue.add(req);
    }

    // Méthode pour mettre à jour la position
    public void updateUserLocation(String userId, double latitude, double longitude,
                                   AuthCallback callback) {
        String url = URL + "/rest/v1/user_locations";

        JSONObject data = new JSONObject();
        try {
            data.put("user_id", userId);
            data.put("latitude", latitude);
            data.put("longitude", longitude);
            data.put("last_update", new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date()));
        } catch (Exception e) {
            callback.onError(e.getMessage(), null);
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, data,
                callback::onSuccess,
                error -> callback.onError(error.getMessage(),
                        error.networkResponse != null ? error.networkResponse.data : null)) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("apikey", API_KEY);
                h.put("Authorization", "Bearer " + API_KEY);
                h.put("Content-Type", "application/json");
                h.put("Prefer", "resolution=merge-duplicates");
                return h;
            }
        };
        queue.add(req);
    }


    public interface AuthCallback { void onSuccess(JSONObject response); void onError(String error, byte[] data); }
    public interface DataCallback { void onSuccess(JSONArray response); void onError(String error); }


}