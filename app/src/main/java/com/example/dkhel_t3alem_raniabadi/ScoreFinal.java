package com.example.dkhel_t3alem_raniabadi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.airbnb.lottie.LottieAnimationView;

public class ScoreFinal extends AppCompatActivity {

    private TextView tvTitreScore, tvSousTitre, tvScoreGlobal, tvScoreTexte, tvPoints;
    private TextView tvScoreFacile, tvScoreMoyen, tvScoreDifficile;
    private View barFacile, barMoyen, barDifficile;
    private AppCompatButton btnRefaire, btnChangerLangue;
    private LottieAnimationView lottieChampion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_score_final);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.score_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        chargerScores();
    }

    private void initViews() {
        lottieChampion = findViewById(R.id.lottieChampion);
        tvTitreScore = findViewById(R.id.tvTitreScore);
        tvSousTitre = findViewById(R.id.tvSousTitre);
        tvScoreGlobal = findViewById(R.id.tvScoreGlobal);
        tvScoreTexte = findViewById(R.id.tvScoreTexte);
        tvPoints = findViewById(R.id.tvPoints);
        tvScoreFacile = findViewById(R.id.tvScoreFacile);
        tvScoreMoyen = findViewById(R.id.tvScoreMoyen);
        tvScoreDifficile = findViewById(R.id.tvScoreDifficile);
        barFacile = findViewById(R.id.barFacile);
        barMoyen = findViewById(R.id.barMoyen);
        barDifficile = findViewById(R.id.barDifficile);
        btnRefaire = findViewById(R.id.btnRefaire);
        btnChangerLangue = findViewById(R.id.btnChangerLangue);

        btnRefaire.setOnClickListener(v -> {
            // Refaire le quiz
            finish();
        });

        btnChangerLangue.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChoisirLangues.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void chargerScores() {
        Intent intent = getIntent();

        int scoreTotal = intent.getIntExtra("score", 0);
        int totalQuestions = intent.getIntExtra("total", 30);
        boolean completed = intent.getBooleanExtra("completed", false);
        String language = intent.getStringExtra("language");

        int scoreFacile = intent.getIntExtra("scoreFacile", 0);
        int totalFacile = intent.getIntExtra("totalFacile", 10);
        int scoreMoyen = intent.getIntExtra("scoreMoyen", 0);
        int totalMoyen = intent.getIntExtra("totalMoyen", 10);
        int scoreDifficile = intent.getIntExtra("scoreDifficile", 0);
        int totalDifficile = intent.getIntExtra("totalDifficile", 10);

        // Pourcentage global
        int pourcentage = totalQuestions > 0 ? (scoreTotal * 100) / (totalQuestions * 10) : 0;
        tvScoreGlobal.setText(pourcentage + "%");
        tvPoints.setText(scoreTotal + " / " + (totalQuestions * 10) + " points");

        // Titre selon réussite
        if (completed) {
            tvTitreScore.setText("🏆 Félicitations !");
            tvSousTitre.setText("Quiz terminé avec succès - " + language);
            lottieChampion.setAnimation(R.raw.champion);
        } else {
            tvTitreScore.setText("📊 Quiz terminé");
            tvSousTitre.setText("Continuez à vous améliorer - " + language);
            lottieChampion.setAnimation(R.raw.success);
        }

        // Scores par niveau
        tvScoreFacile.setText(scoreFacile + "/" + totalFacile);
        tvScoreMoyen.setText(scoreMoyen + "/" + totalMoyen);
        tvScoreDifficile.setText(scoreDifficile + "/" + totalDifficile);

        // Barres de progression
        updateBar(barFacile, scoreFacile, totalFacile);
        updateBar(barMoyen, scoreMoyen, totalMoyen);
        updateBar(barDifficile, scoreDifficile, totalDifficile);
    }

    private void updateBar(View bar, int score, int total) {
        if (total == 0) return;
        float ratio = (float) score / total;
        int maxWidth = bar.getRootView().getWidth() - 200; // Largeur estimée
        if (maxWidth > 0) {
            bar.getLayoutParams().width = (int) (maxWidth * ratio);
            bar.requestLayout();
        }
    }
}