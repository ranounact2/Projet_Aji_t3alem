package com.example.dkhel_t3alem_raniabadi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionsActivity extends AppCompatActivity {

    private static final int ALL_PERMISSIONS_CODE = 100;

    // Liste de toutes les permissions dont on a besoin
    private String[] permissionsRequises = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private Button btnAutoriser;
    private LinearLayout statusLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        btnAutoriser = findViewById(R.id.btnAutoriser);
        statusLayout = findViewById(R.id.statusLayout);

        btnAutoriser.setBackgroundColor(Color.parseColor("#58CC02"));

        // Vérifier si toutes les permissions sont déjà accordées
        if (toutesPermissionsAccordees()) {
            passerAEtapeSuivante();
            return;
        }

        btnAutoriser.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(
                    PermissionsActivity.this,
                    permissionsRequises,
                    ALL_PERMISSIONS_CODE
            );
        });
    }

    private boolean toutesPermissionsAccordees() {
        for (String permission : permissionsRequises) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ALL_PERMISSIONS_CODE) {
            // Mise à jour de l'affichage
            updatePermissionStatus();

            if (toutesPermissionsAccordees()) {
                Toast.makeText(this, "✅ Toutes les permissions accordées !",
                        Toast.LENGTH_SHORT).show();
                passerAEtapeSuivante();
            } else {
                Toast.makeText(this,
                        "⚠️ Certaines permissions sont nécessaires",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updatePermissionStatus() {
        statusLayout.removeAllViews();

        for (String permission : permissionsRequises) {
            boolean accorde = ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            TextView tvIcon = new TextView(this);
            tvIcon.setTextSize(24);

            TextView tvNom = new TextView(this);
            tvNom.setTextSize(16);
            tvNom.setPadding(16, 0, 0, 0);

            if (accorde) {
                tvIcon.setText("✅");
                tvNom.setTextColor(Color.parseColor("#58CC02"));
            } else {
                tvIcon.setText("❌");
                tvNom.setTextColor(Color.RED);
            }

            // Nom de la permission
            switch (permission) {
                case Manifest.permission.CAMERA:
                    tvNom.setText("📸 Caméra");
                    break;
                case Manifest.permission.RECORD_AUDIO:
                    tvNom.setText("🎤 Micro");
                    break;
                case Manifest.permission.ACCESS_FINE_LOCATION:
                    tvNom.setText("📍 Localisation");
                    break;
            }

            row.addView(tvIcon);
            row.addView(tvNom);
            statusLayout.addView(row);
        }
    }

    private void passerAEtapeSuivante() {
        // Lancer la vérification du visage
        Intent intent = new Intent(this, CameraVerificationActivity.class);
        startActivity(intent);
        finish();
    }
}