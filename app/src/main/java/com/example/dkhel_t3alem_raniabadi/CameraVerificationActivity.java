package com.example.dkhel_t3alem_raniabadi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.dkhel_t3alem_raniabadi.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraVerificationActivity extends AppCompatActivity {

    private PreviewView previewView;
    private View overlayCadreVisage;
    private TextView txtStatut;
    private TextView txtAlertes;
    private View indicateurCouleur;
    private Button btnContinuer;

    private FaceDetector faceDetector;
    private ExecutorService cameraExecutor;
    private Handler mainHandler;

    // Variables de suivi
    private boolean visageValide = false;
    private int compteurFraude = 0;
    private static final int SEUIL_FRAUDE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_verification);

        previewView = findViewById(R.id.previewView);
        overlayCadreVisage = findViewById(R.id.overlayCadreVisage);
        txtStatut = findViewById(R.id.txtStatut);
        txtAlertes = findViewById(R.id.txtAlertes);
        indicateurCouleur = findViewById(R.id.indicateurCouleur);
        btnContinuer = findViewById(R.id.btnContinuer);

        cameraExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Configurer ML Kit
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build();
        faceDetector = FaceDetection.getClient(options);

        btnContinuer.setOnClickListener(v -> {
            if (visageValide) {
                // ✅ Lancer le service de localisation en arrière-plan
                startService(new Intent(this, LocationService.class));

                // Passer à ChoisirLangues
                Intent intent = new Intent(this, ChoisirLangues.class);
                startActivity(intent);
                finish();
            }
        });

        // Démarrer la caméra
        demarrerCamera();

        // Vérification périodique (mise à jour UI)
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (visageValide) {
                    btnContinuer.setVisibility(View.VISIBLE);
                    btnContinuer.setBackgroundColor(Color.parseColor("#58CC02"));
                } else {
                    btnContinuer.setVisibility(View.GONE);
                }
                mainHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void demarrerCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    try {
                        @SuppressWarnings("ConstantConditions")
                        android.media.Image mediaImage = imageProxy.getImage();

                        // ✅ Vérifier que l'image n'est pas null
                        if (mediaImage != null) {
                            InputImage image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.getImageInfo().getRotationDegrees()
                            );

                            faceDetector.process(image)
                                    .addOnSuccessListener(faces -> {
                                        analyserVisages(faces);
                                    })
                                    .addOnFailureListener(e -> {
                                        // Gérer l'erreur silencieusement
                                    })
                                    .addOnCompleteListener(task -> {
                                        // ✅ Fermer l'image AVANT de fermer le proxy
                                        mediaImage.close();
                                        imageProxy.close();
                                    });
                        } else {
                            // Si l'image est null, fermer juste le proxy
                            imageProxy.close();
                        }
                    } catch (Exception e) {
                        // En cas d'erreur, fermer le proxy
                        imageProxy.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (Exception e) {
                Toast.makeText(this, "Erreur caméra", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyserVisages(java.util.List<Face> faces) {
        mainHandler.post(() -> {
            if (faces.size() == 0) {
                // PAS DE VISAGE
                compteurFraude++;
                mettreAJourUI("❌ Visage non détecté", "Placez-vous devant la caméra", Color.RED);
                visageValide = false;

            } else if (faces.size() > 1) {
                // PLUSIEURS VISAGES
                compteurFraude += 2;
                mettreAJourUI("⚠️ Plusieurs visages", "Une seule personne autorisée", Color.RED);
                visageValide = false;

            } else {
                // UN SEUL VISAGE
                Face face = faces.get(0);
                float angleY = face.getHeadEulerAngleY(); // Gauche/Droite
                float angleZ = face.getHeadEulerAngleZ(); // Haut/Bas

                boolean regardeEcran = Math.abs(angleY) < 15 && Math.abs(angleZ) < 15;

                if (regardeEcran) {
                    compteurFraude = Math.max(0, compteurFraude - 1);
                    mettreAJourUI("✅ Visage vérifié", "Vous regardez l'écran", Color.parseColor("#58CC02"));
                    visageValide = true;
                } else {
                    compteurFraude++;

                    // Déterminer où il regarde
                    String direction = "";
                    if (angleY < -15) direction = "à droite";
                    else if (angleY > 15) direction = "à gauche";
                    else if (angleZ < -15) direction = "en bas";
                    else direction = "ailleurs";

                    mettreAJourUI("👀 Regard détecté : " + direction,
                            "Regardez l'écran svp",
                            Color.parseColor("#FFA500"));
                    visageValide = false;
                }
            }

            // Trop de fraudes détectées
            if (compteurFraude > SEUIL_FRAUDE) {
                txtAlertes.setText("⚠️ ALERTE : Veuillez regarder l'écran !");
                txtAlertes.setTextColor(Color.RED);
                compteurFraude = SEUIL_FRAUDE; // Plafonner
            }
        });
    }

    private void mettreAJourUI(String statut, String alerte, int couleur) {
        txtStatut.setText(statut);
        txtStatut.setTextColor(couleur);
        txtAlertes.setText(alerte);
        indicateurCouleur.setBackgroundColor(couleur);
        overlayCadreVisage.setBackgroundResource(
                couleur == Color.parseColor("#58CC02") ?
                        R.drawable.cadre_visage_vert : R.drawable.cadre_visage_rouge
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        faceDetector.close();
    }
}