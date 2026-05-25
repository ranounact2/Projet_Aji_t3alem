package com.example.dkhel_t3alem_raniabadi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.airbnb.lottie.LottieAnimationView;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChoisirLangues extends AppCompatActivity {

    private static final String TAG = "SupabaseDebug";
    private LinearLayout languageContainer;
    private ProgressBar progressBar;
    private LottieAnimationView lottieLoading, lottieWelcome;
    private SupabaseConfig supabaseConfig;
    private static final int LOCATION_PERMISSION_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choisir_langues);

        supabaseConfig = new SupabaseConfig(this);
        languageContainer = findViewById(R.id.dynamic_language_container);
        progressBar = findViewById(R.id.progressBar);
        lottieLoading = findViewById(R.id.lottieLoading);
        lottieWelcome = findViewById(R.id.lottieWelcome);

        loadLanguages();
    }

    private void loadLanguages() {
        // ✅ Afficher Lottie au lieu du ProgressBar
        lottieLoading.setVisibility(View.VISIBLE);
        lottieLoading.setAnimation(R.raw.loading);
        lottieLoading.playAnimation();

        supabaseConfig.getLanguages(new SupabaseConfig.DataCallback() {
            @Override
            public void onSuccess(JSONArray languages) {
                runOnUiThread(() -> {
                    lottieLoading.cancelAnimation();
                    lottieLoading.setVisibility(View.GONE);
                    displayLanguages(languages);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    lottieLoading.cancelAnimation();
                    lottieLoading.setVisibility(View.GONE);
                    Toast.makeText(ChoisirLangues.this,
                            "Erreur Réseau : " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String getFlagByCode(String code) {
        if (code == null || code.isEmpty()) return "🌐";
        switch (code.toLowerCase()) {
            case "fr": return "🇫🇷";
            case "en": return "🇬🇧";
            case "ar": return "🇸🇦";
            case "ary": return "🇲🇦";
            default: return "🌐";
        }
    }

    private void displayLanguages(JSONArray languages) {
        // ✅ Animation de bienvenue
        showWelcomeAnimation();

        try {
            for (int i = 0; i < languages.length(); i++) {
                JSONObject lang = languages.getJSONObject(i);
                String id = lang.optString("id", "");
                String name = lang.optString("nom_langue", "Langue " + id);
                String codeLangue = lang.optString("code_langue", "");
                String flag = getFlagByCode(codeLangue);

                addLanguageCard(id, name, flag, codeLangue.toUpperCase());
            }

            ajouterBoutonMap();

        } catch (Exception e) {
            Log.e(TAG, "Erreur affichage : " + e.getMessage());
        }
    }

    /**
     * ✅ Animation de bienvenue
     */
    private void showWelcomeAnimation() {
        lottieWelcome.setVisibility(View.VISIBLE);
        lottieWelcome.setAnimation(R.raw.welcome);
        lottieWelcome.playAnimation();

        lottieWelcome.addAnimatorListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                lottieWelcome.setVisibility(View.GONE);
            }
            @Override public void onAnimationStart(android.animation.Animator a) {}
            @Override public void onAnimationCancel(android.animation.Animator a) {}
            @Override public void onAnimationRepeat(android.animation.Animator a) {}
        });
    }

    private void addLanguageCard(String id, String name, String flag, String subText) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 160
        );
        cardParams.setMargins(0, 0, 0, 24);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16);
        cardView.setCardElevation(4);
        cardView.setUseCompatPadding(true);
        cardView.setClickable(true);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);
        innerLayout.setGravity(Gravity.CENTER_VERTICAL);
        innerLayout.setPadding(20, 20, 20, 20);
        innerLayout.setBackgroundColor(Color.WHITE);

        // Drapeau
        TextView tvFlag = new TextView(this);
        tvFlag.setText(flag);
        tvFlag.setTextSize(30);
        LinearLayout.LayoutParams flagParams = new LinearLayout.LayoutParams(80, 80);
        tvFlag.setLayoutParams(flagParams);
        tvFlag.setGravity(Gravity.CENTER);

        // Texte
        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        textLayout.setPadding(20, 0, 0, 0);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(18);
        tvName.setTextColor(Color.parseColor("#333333"));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvSub = new TextView(this);
        tvSub.setText(subText);
        tvSub.setTextSize(13);
        tvSub.setTextColor(Color.parseColor("#999999"));

        // Flèche
        TextView tvArrow = new TextView(this);
        tvArrow.setText("→");
        tvArrow.setTextSize(22);
        tvArrow.setTextColor(Color.parseColor("#58CC02"));

        textLayout.addView(tvName);
        textLayout.addView(tvSub);
        innerLayout.addView(tvFlag);
        innerLayout.addView(textLayout);
        innerLayout.addView(tvArrow);
        cardView.addView(innerLayout);

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(ChoisirLangues.this, QuestionsList.class);
            intent.putExtra("language_id", id);
            intent.putExtra("language_name", name);
            startActivity(intent);
        });

        languageContainer.addView(cardView);
    }

    private void ajouterBoutonMap() {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 160
        );
        cardParams.setMargins(0, 0, 0, 24);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16);
        cardView.setCardElevation(4);
        cardView.setUseCompatPadding(true);
        cardView.setClickable(true);
        cardView.setCardBackgroundColor(Color.parseColor("#58CC02"));

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);
        innerLayout.setGravity(Gravity.CENTER);
        innerLayout.setPadding(20, 20, 20, 20);

        TextView tvIcon = new TextView(this);
        tvIcon.setText("🗺️");
        tvIcon.setTextSize(30);
        tvIcon.setPadding(0, 0, 20, 0);

        TextView tvText = new TextView(this);
        tvText.setText("Voir les apprenants près de moi");
        tvText.setTextSize(16);
        tvText.setTextColor(Color.WHITE);
        tvText.setTypeface(null, android.graphics.Typeface.BOLD);

        innerLayout.addView(tvIcon);
        innerLayout.addView(tvText);
        cardView.addView(innerLayout);

        cardView.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_CODE);
            } else {
                startActivity(new Intent(ChoisirLangues.this, NearbyUsersActivity.class));
            }
        });

        languageContainer.addView(cardView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ Localisation activée !", Toast.LENGTH_SHORT).show();
                startService(new Intent(this, LocationService.class));
                startActivity(new Intent(ChoisirLangues.this, NearbyUsersActivity.class));
            } else {
                Toast.makeText(this, "📍 Permission refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
}