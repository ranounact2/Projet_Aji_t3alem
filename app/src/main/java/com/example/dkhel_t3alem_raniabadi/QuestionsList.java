package com.example.dkhel_t3alem_raniabadi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.*;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.navigation.NavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuestionsList extends AppCompatActivity {

    // ==================== VARIABLES QUESTIONS ====================
    private TextView tvQuestionNumber, tvQuestionText, tvLevelIndicator, tvVoiceResult;
    private LinearLayout optionsContainer;
    private Button btnValidate, btnRefaireNiveau;
    private FrameLayout microphoneContainer;
    private TextView tvMicrophoneIcon;
    private EditText etWrittenAnswer;
    private View progressBar;

    private List<JSONObject> easyQuestions = new ArrayList<>();
    private List<JSONObject> mediumQuestions = new ArrayList<>();
    private List<JSONObject> hardQuestions = new ArrayList<>();

    private int currentLevel = 0;
    private int currentQuestionIndex = 0;
    private int totalScore = 0;
    private int levelScore = 0;
    private int selectedOptionIndex = -1;
    private String currentCorrectAnswer = "";
    private String userSpokenText = "";
    private String currentQuestionType = "qcm";
    private String selectedLanguage = "";
    private String languageId = "";


    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private static final int REQUEST_RECORD_AUDIO = 100;
    private boolean isListening = false;

    private SupabaseConfig supabaseConfig;

    private static final double POURCENTAGE_PASSING = 0.70;

    // ==================== VARIABLES SCORES PAR NIVEAU ====================
    private int scoreNiveauFacile = 0;
    private int scoreNiveauMoyen = 0;
    private int scoreNiveauDifficile = 0;
    private int totalQuestionsFacile = 0;
    private int totalQuestionsMoyen = 0;
    private int totalQuestionsDifficile = 0;

    // ==================== VARIABLES CAMÉRA ====================
    private PreviewView cameraPreviewView;
    private FaceDetector faceDetector;
    private ExecutorService cameraExecutor;
    private Handler cameraHandler;
    private AlertDialog blockDialog;

    private boolean quizBloque = false;
    private boolean visagePresent = false;
    private boolean regardeEcran = true;
    private int compteurFraude = 0;
    private static final int SEUIL_BLOCAGE = 15;



    private AIHelper aiHelper;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private TextView tvAiQuestion, tvAiResponse;
    private ProgressBar aiProgressBar;
    private Button btnAskAI;

    // ==================== MODE ALÉATOIRE ====================
    private static final boolean MODE_ALEATOIRE = true;




    private LottieAnimationView lottieAnimation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions_list);
        // ✅ AJOUTER CETTE LIGNE (ligne ~121)
        initLottie();
        supabaseConfig = new SupabaseConfig(this);

        aiHelper = new AIHelper(this);
        initAssistantSidebar();

        // Initialisation des vues existantes
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        optionsContainer = findViewById(R.id.optionsContainer);
        btnValidate = findViewById(R.id.btnValidate);
        progressBar = findViewById(R.id.progressBar);

        // Initialisation caméra
        cameraPreviewView = findViewById(R.id.cameraPreviewView);
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraHandler = new Handler(Looper.getMainLooper());

        createLevelIndicator();
        createVoiceComponents();
        createWritingComponents();
        ajouterBoutonRefaire();

        btnValidate.setOnClickListener(v -> {
            if (quizBloque) {
                Toast.makeText(this, "⚠️ Regardez l'écran pour continuer !", Toast.LENGTH_LONG).show();
                return;
            }
            validateAnswer();
        });


        languageId = getIntent().getStringExtra("language_id");
        selectedLanguage = getIntent().getStringExtra("language_name");

        initSpeechRecognizer();
        initFaceDetector();
        demarrerCamera();

        if (languageId != null && !languageId.isEmpty()) {
            loadQuestionsFromDB();
        } else {
            String questionsJson = getIntent().getStringExtra("questions");
            if (questionsJson != null) {
                try {
                    processQuestions(new JSONArray(questionsJson));
                } catch (Exception e) {
                    Toast.makeText(this, "Erreur format questions", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "ID de langue manquant", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }




    // ==================== AI ASSISTANT SIDEBAR ====================

    private void initAssistantSidebar() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        // Ouvrir la sidebar avec le bouton 🤖
        TextView btnOpenAssistant = findViewById(R.id.btnOpenAssistant);
        btnOpenAssistant.setOnClickListener(v -> {
            // Mettre la question actuelle dans la sidebar
            String questionActuelle = tvQuestionText.getText().toString();
            if (tvAiQuestion != null) {
                tvAiQuestion.setText("📝 " + questionActuelle);
            }
            drawerLayout.openDrawer(GravityCompat.END);
        });

        // Initialiser les vues de la sidebar
        View headerView = navView.getHeaderView(0);
        tvAiQuestion = headerView.findViewById(R.id.tvAiQuestion);
        tvAiResponse = headerView.findViewById(R.id.tvAiResponse);
        aiProgressBar = headerView.findViewById(R.id.aiProgressBar);
        btnAskAI = headerView.findViewById(R.id.btnAskAI);

        if (btnAskAI != null) {
            btnAskAI.setOnClickListener(v -> demanderAideAI());
        }
    }

    private void demanderAideAI() {
        String questionActuelle = tvQuestionText.getText().toString();

        if (questionActuelle.isEmpty()) {
            Toast.makeText(this, "❌ Pas de question", Toast.LENGTH_SHORT).show();
            return;
        }

        // Afficher le loader
        if (aiProgressBar != null) aiProgressBar.setVisibility(View.VISIBLE);
        if (btnAskAI != null) btnAskAI.setEnabled(false);
        if (tvAiResponse != null) tvAiResponse.setText("⏳ Réflexion...");

        aiHelper.demanderAide(questionActuelle, selectedLanguage, new AIHelper.AIResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    if (aiProgressBar != null) aiProgressBar.setVisibility(View.GONE);
                    if (btnAskAI != null) btnAskAI.setEnabled(true);
                    if (tvAiResponse != null) {
                        tvAiResponse.setText("🤖 " + response);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (aiProgressBar != null) aiProgressBar.setVisibility(View.GONE);
                    if (btnAskAI != null) btnAskAI.setEnabled(true);
                    if (tvAiResponse != null) {
                        tvAiResponse.setText("❌ Erreur: " + error);
                    }
                });
            }
        });
    }

    // ==================== BOUTON REFAIRE NIVEAU ====================

    private void ajouterBoutonRefaire() {
        btnRefaireNiveau = new Button(this);
        btnRefaireNiveau.setText("🔄 Refaire ce niveau");
        btnRefaireNiveau.setTextColor(Color.WHITE);
        btnRefaireNiveau.setTextSize(16);
        btnRefaireNiveau.setBackgroundColor(Color.parseColor("#FF9800"));
        btnRefaireNiveau.setVisibility(View.GONE);
        btnRefaireNiveau.setPadding(20, 15, 20, 15);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 16, 0, 16);
        btnRefaireNiveau.setLayoutParams(params);

        btnRefaireNiveau.setOnClickListener(v -> refaireNiveau());

        LinearLayout mainLayout = findViewById(R.id.main);
        int indexBtn = mainLayout.indexOfChild(btnValidate);
        mainLayout.addView(btnRefaireNiveau, indexBtn + 1);
    }

    private void refaireNiveau() {
        currentQuestionIndex = 0;
        levelScore = 0;
        selectedOptionIndex = -1;
        userSpokenText = "";
        quizBloque = false;
        compteurFraude = 0;

        if (MODE_ALEATOIRE) {
            List<JSONObject> currentQuestions = getCurrentLevelQuestions();
            if (!currentQuestions.isEmpty()) {
                switch (currentLevel) {
                    case 0: easyQuestions = shuffleList(currentQuestions); break;
                    case 1: mediumQuestions = shuffleList(currentQuestions); break;
                    case 2: hardQuestions = shuffleList(currentQuestions); break;
                }
            }
        }

        btnRefaireNiveau.setVisibility(View.GONE);
        btnValidate.setVisibility(View.VISIBLE);

        updateLevelUI();
        updateProgressBar();
        displayCurrentQuestion();

        Toast.makeText(this, "🔄 Niveau redémarré !", Toast.LENGTH_SHORT).show();
    }

    // ==================== CAMÉRA ====================

    private void initFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();
        faceDetector = FaceDetection.getClient(options);
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
                        android.media.Image mediaImage = imageProxy.getImage();
                        if (mediaImage != null) {
                            InputImage image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.getImageInfo().getRotationDegrees()
                            );

                            faceDetector.process(image)
                                    .addOnSuccessListener(faces -> analyserVisages(faces))
                                    .addOnCompleteListener(task -> {
                                        mediaImage.close();
                                        imageProxy.close();
                                    });
                        } else {
                            imageProxy.close();
                        }
                    } catch (Exception e) {
                        imageProxy.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

            } catch (Exception e) {
                Log.e("Camera", "Erreur caméra: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyserVisages(List<Face> faces) {
        cameraHandler.post(() -> {
            if (faces.size() == 0) {
                visagePresent = false;
                regardeEcran = false;
                compteurFraude += 3;
            } else if (faces.size() > 1) {
                visagePresent = true;
                regardeEcran = false;
                compteurFraude += 5;
            } else {
                Face face = faces.get(0);
                visagePresent = true;

                float angleY = face.getHeadEulerAngleY();
                float angleZ = face.getHeadEulerAngleZ();

                if (Math.abs(angleY) < 20 && Math.abs(angleZ) < 20) {
                    regardeEcran = true;
                    compteurFraude = Math.max(0, compteurFraude - 2);
                } else {
                    regardeEcran = false;
                    compteurFraude++;
                }
            }

            if (compteurFraude >= SEUIL_BLOCAGE && !quizBloque) {
                bloquerQuiz();
            } else if (compteurFraude < SEUIL_BLOCAGE && quizBloque) {
                debloquerQuiz();
            }

            compteurFraude = Math.min(compteurFraude, SEUIL_BLOCAGE + 5);
        });
    }

    private void bloquerQuiz() {
        quizBloque = true;
        runOnUiThread(() -> {
            if (blockDialog == null || !blockDialog.isShowing()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(false);

                LinearLayout dialogLayout = new LinearLayout(this);
                dialogLayout.setOrientation(LinearLayout.VERTICAL);
                dialogLayout.setPadding(50, 40, 50, 40);
                dialogLayout.setGravity(Gravity.CENTER);
                dialogLayout.setBackgroundColor(Color.parseColor("#E6000000"));

                TextView iconText = new TextView(this);
                iconText.setText("⚠️");
                iconText.setTextSize(80);
                iconText.setGravity(Gravity.CENTER);
                iconText.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

                TextView titleText = new TextView(this);
                titleText.setText("Quiz Bloqué !");
                titleText.setTextSize(28);
                titleText.setTextColor(Color.WHITE);
                titleText.setTypeface(null, android.graphics.Typeface.BOLD);
                titleText.setGravity(Gravity.CENTER);
                titleText.setPadding(0, 20, 0, 15);

                TextView messageText = new TextView(this);
                if (!visagePresent) {
                    messageText.setText("❌ Aucun visage détecté");
                } else {
                    messageText.setText("👀 Vous ne regardez pas l'écran");
                }
                messageText.setTextSize(18);
                messageText.setTextColor(Color.parseColor("#FFA500"));
                messageText.setGravity(Gravity.CENTER);
                messageText.setPadding(0, 0, 0, 30);

                TextView instructionText = new TextView(this);
                instructionText.setText("Regardez l'écran pour continuer");
                instructionText.setTextSize(16);
                instructionText.setTextColor(Color.WHITE);
                instructionText.setGravity(Gravity.CENTER);
                instructionText.setPadding(0, 0, 0, 30);

                Button btnReprendre = new Button(this);
                btnReprendre.setText("JE REGARDE L'ÉCRAN");
                btnReprendre.setTextColor(Color.WHITE);
                btnReprendre.setTextSize(18);
                btnReprendre.setTypeface(null, android.graphics.Typeface.BOLD);
                btnReprendre.setBackgroundColor(Color.parseColor("#58CC02"));
                btnReprendre.setPadding(30, 15, 30, 15);
                btnReprendre.setOnClickListener(v -> {
                    if (regardeEcran && visagePresent) {
                        debloquerQuiz();
                        if (blockDialog != null) {
                            blockDialog.dismiss();
                        }
                    } else {
                        Toast.makeText(QuestionsList.this,
                                "⚠️ Regardez l'écran d'abord !",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                dialogLayout.addView(iconText);
                dialogLayout.addView(titleText);
                dialogLayout.addView(messageText);
                dialogLayout.addView(instructionText);
                dialogLayout.addView(btnReprendre);

                builder.setView(dialogLayout);
                blockDialog = builder.create();

                if (blockDialog.getWindow() != null) {
                    blockDialog.getWindow().setBackgroundDrawable(
                            new ColorDrawable(Color.TRANSPARENT));
                    blockDialog.getWindow().setLayout(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT);
                }

                blockDialog.show();
            }
        });
    }

    private void debloquerQuiz() {
        quizBloque = false;
        compteurFraude = 0;
        runOnUiThread(() -> {
            if (blockDialog != null && blockDialog.isShowing()) {
                blockDialog.dismiss();
                Toast.makeText(QuestionsList.this,
                        "✅ Quiz débloqué !", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== RECONNAISSANCE VOCALE ====================

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // ✅ Langue dynamique selon la langue choisie
        String locale = getLanguageLocale();
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        Log.d("SpeechRecognizer", "🌐 Langue configurée: " + locale);

        speechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvVoiceResult.setText("🎤 Écoute en cours...");
                tvVoiceResult.setTextColor(Color.parseColor("#58CC02"));
                animateMicrophone(true);
            }

            @Override
            public void onBeginningOfSpeech() {
                tvVoiceResult.setText("🔴 Parlez maintenant...");
                tvVoiceResult.setTextColor(Color.parseColor("#FF5722"));
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                tvVoiceResult.setText("⏳ Analyse de votre prononciation...");
                tvVoiceResult.setTextColor(Color.parseColor("#FF9800"));
                animateMicrophone(false);
                isListening = false;
            }

            @Override
            public void onError(int error) {
                tvVoiceResult.setText("❌ Erreur - Réessayez");
                tvVoiceResult.setTextColor(Color.RED);
                animateMicrophone(false);
                isListening = false;
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    userSpokenText = matches.get(0);
                    tvVoiceResult.setText("🗣️ Vous avez dit : \"" + userSpokenText + "\"");
                    tvVoiceResult.setTextColor(Color.parseColor("#333333"));
                }
                isListening = false;
                animateMicrophone(false);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void createVoiceComponents() {
        microphoneContainer = new FrameLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(200, 200);
        containerParams.gravity = Gravity.CENTER;
        containerParams.setMargins(0, 40, 0, 16);
        microphoneContainer.setLayoutParams(containerParams);
        microphoneContainer.setVisibility(View.GONE);

        tvMicrophoneIcon = new TextView(this);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        tvMicrophoneIcon.setLayoutParams(iconParams);
        tvMicrophoneIcon.setText("🎤");
        tvMicrophoneIcon.setTextSize(60);
        tvMicrophoneIcon.setGravity(Gravity.CENTER);

        GradientDrawable circleDrawable = new GradientDrawable();
        circleDrawable.setShape(GradientDrawable.OVAL);
        circleDrawable.setColor(Color.parseColor("#58CC02"));
        tvMicrophoneIcon.setBackground(circleDrawable);

        tvMicrophoneIcon.setOnClickListener(v -> {
            if (!isListening) {
                startVoiceRecognition();
            }
        });

        microphoneContainer.addView(tvMicrophoneIcon);

        tvVoiceResult = new TextView(this);
        tvVoiceResult.setTextSize(14);
        tvVoiceResult.setGravity(Gravity.CENTER);
        tvVoiceResult.setPadding(0, 16, 0, 8);
        tvVoiceResult.setVisibility(View.GONE);

        LinearLayout mainLayout = findViewById(R.id.main);
        mainLayout.addView(microphoneContainer, mainLayout.indexOfChild(optionsContainer));
        mainLayout.addView(tvVoiceResult, mainLayout.indexOfChild(optionsContainer) + 1);
    }

    private void createWritingComponents() {
        etWrittenAnswer = new EditText(this);
        etWrittenAnswer.setHint("✍️ Écrivez votre réponse ici...");
        etWrittenAnswer.setTextSize(18);
        etWrittenAnswer.setTextColor(Color.parseColor("#333333"));
        etWrittenAnswer.setHintTextColor(Color.parseColor("#BBBBBB"));
        etWrittenAnswer.setPadding(24, 20, 24, 20);
        etWrittenAnswer.setBackgroundResource(R.drawable.button_green_rounded);
        etWrittenAnswer.setVisibility(View.GONE);
        etWrittenAnswer.setGravity(Gravity.TOP);

        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 150);
        editParams.setMargins(0, 16, 0, 16);
        etWrittenAnswer.setLayoutParams(editParams);

        LinearLayout mainLayout = findViewById(R.id.main);
        mainLayout.addView(etWrittenAnswer, mainLayout.indexOfChild(optionsContainer));
    }

    private void animateMicrophone(boolean start) {
        if (start) {
            ScaleAnimation scaleAnimation = new ScaleAnimation(
                    1.0f, 1.1f, 1.0f, 1.1f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setDuration(800);
            scaleAnimation.setRepeatMode(Animation.REVERSE);
            scaleAnimation.setRepeatCount(Animation.INFINITE);
            tvMicrophoneIcon.startAnimation(scaleAnimation);
        } else {
            tvMicrophoneIcon.clearAnimation();
        }
    }

    // ✅ Méthode pour obtenir la langue de reconnaissance
    private String getLanguageLocale() {
        if (selectedLanguage == null || selectedLanguage.isEmpty()) {
            return Locale.getDefault().toString();
        }

        String lang = selectedLanguage.toLowerCase().trim();

        if (lang.contains("fran")) return "fr-FR";
        if (lang.contains("ang") || lang.contains("eng")) return "en-US";
        if (lang.contains("ara")) return "ar-SA";
        if (lang.contains("dari") || lang.contains("maroc")) return "ar-MA";

        return Locale.getDefault().toString();
    }

    private void startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        isListening = true;
        userSpokenText = "";

        // ✅ Mettre à jour la langue avant chaque écoute
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLanguageLocale());

        speechRecognizer.startListening(speechRecognizerIntent);
    }

    // ==================== CHARGEMENT QUESTIONS ====================

    private void loadQuestionsFromDB() {
        supabaseConfig.getQuestionsByLanguage(languageId, new SupabaseConfig.DataCallback() {
            @Override
            public void onSuccess(JSONArray response) {
                runOnUiThread(() -> {
                    if (response.length() == 0) {
                        Toast.makeText(QuestionsList.this,
                                "Aucune question disponible", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        processQuestions(response);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(QuestionsList.this,
                            "Erreur : " + error, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void processQuestions(JSONArray allQuestions) {
        try {
            easyQuestions.clear();
            mediumQuestions.clear();
            hardQuestions.clear();

            for (int i = 0; i < allQuestions.length(); i++) {
                JSONObject q = allQuestions.getJSONObject(i);
                String niveauId = q.optString("niveau_id", "1");
                String level;
                switch (niveauId) {
                    case "1": level = "easy"; break;
                    case "2": level = "medium"; break;
                    case "3": level = "hard"; break;
                    default: level = "easy"; break;
                }

                JSONObject qf = new JSONObject();
                qf.put("question", q.optString("titre", ""));
                qf.put("answer", q.optString("reponse", ""));
                qf.put("points", q.optString("nbr_points", "10"));
                qf.put("level", level);
                qf.put("type", q.optString("type_question", "qcm"));

                String optionsStr = q.optString("options", "[]");
                JSONArray optionsArray;
                try {
                    optionsArray = new JSONArray(optionsStr);
                } catch (Exception e) {
                    optionsArray = q.optJSONArray("options");
                    if (optionsArray == null) optionsArray = new JSONArray();
                }

                JSONArray shuffledOptions = shuffleJsonArray(optionsArray);
                qf.put("options", shuffledOptions);

                switch (level) {
                    case "easy": easyQuestions.add(qf); break;
                    case "medium": mediumQuestions.add(qf); break;
                    case "hard": hardQuestions.add(qf); break;
                    default: easyQuestions.add(qf); break;
                }
            }

            if (MODE_ALEATOIRE) {
                easyQuestions = shuffleList(easyQuestions);
                mediumQuestions = shuffleList(mediumQuestions);
                hardQuestions = shuffleList(hardQuestions);
            }

            totalQuestionsFacile = easyQuestions.size();
            totalQuestionsMoyen = mediumQuestions.size();
            totalQuestionsDifficile = hardQuestions.size();

            if (easyQuestions.isEmpty() && mediumQuestions.isEmpty() && hardQuestions.isEmpty()) {
                Toast.makeText(this, "Aucune question trouvée", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            startLevel(0);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private List<JSONObject> shuffleList(List<JSONObject> list) {
        List<JSONObject> shuffled = new ArrayList<>(list);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    private JSONArray shuffleJsonArray(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                list.add(array.getString(i));
            } catch (Exception e) {
                Log.e("Shuffle", "Erreur mélange options");
            }
        }
        Collections.shuffle(list);
        return new JSONArray(list);
    }

    // ==================== AFFICHAGE ====================

    private void createLevelIndicator() {
        tvLevelIndicator = new TextView(this);
        tvLevelIndicator.setTextSize(16);
        tvLevelIndicator.setTextColor(Color.parseColor("#58CC02"));
        tvLevelIndicator.setGravity(Gravity.CENTER);
        tvLevelIndicator.setPadding(0, 0, 0, 16);
        LinearLayout mainLayout = findViewById(R.id.main);
        int index = mainLayout.indexOfChild(tvQuestionNumber);
        if (index != -1) mainLayout.addView(tvLevelIndicator, index + 1);
    }

    private void startLevel(int level) {
        currentLevel = level;
        currentQuestionIndex = 0;
        levelScore = 0;
        selectedOptionIndex = -1;
        userSpokenText = "";

        if (btnRefaireNiveau != null) {
            btnRefaireNiveau.setVisibility(View.GONE);
            btnValidate.setVisibility(View.VISIBLE);
        }

        updateLevelUI();
        updateProgressBar();
        displayCurrentQuestion();
    }

    private void displayCurrentQuestion() {
        List<JSONObject> currentQuestions = getCurrentLevelQuestions();
        if (currentQuestions.isEmpty()) { advanceLevel(); return; }
        if (currentQuestionIndex >= currentQuestions.size()) { finishLevel(); return; }

        try {
            JSONObject q = currentQuestions.get(currentQuestionIndex);
            tvQuestionNumber.setText("Question " + (currentQuestionIndex + 1) +
                    "/" + currentQuestions.size());
            tvQuestionText.setText(q.getString("question"));
            currentCorrectAnswer = q.getString("answer").trim();
            currentQuestionType = q.optString("type", "qcm");
            selectedOptionIndex = -1;
            userSpokenText = "";

            optionsContainer.removeAllViews();
            optionsContainer.setVisibility(View.GONE);
            microphoneContainer.setVisibility(View.GONE);
            tvVoiceResult.setVisibility(View.GONE);
            etWrittenAnswer.setVisibility(View.GONE);
            etWrittenAnswer.setText("");

            switch (currentQuestionType) {
                case "ecriture":
                    showWritingQuestion();
                    break;
                case "prononciation":
                    showPronunciationQuestion();
                    break;
                default:
                    showQCMQuestion(q);
                    break;
            }

            btnValidate.setEnabled(true);
            updateProgressBar();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur affichage", Toast.LENGTH_SHORT).show();
        }
    }

    private void showQCMQuestion(JSONObject q) {
        optionsContainer.setVisibility(View.VISIBLE);
        try {
            JSONArray optionsArray = q.getJSONArray("options");
            for (int i = 0; i < optionsArray.length(); i++) {
                optionsContainer.addView(createOptionCard(optionsArray.getString(i), i));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur options", Toast.LENGTH_SHORT).show();
        }
    }

    private void showWritingQuestion() {
        etWrittenAnswer.setVisibility(View.VISIBLE);
        etWrittenAnswer.requestFocus();
    }

    private void showPronunciationQuestion() {
        microphoneContainer.setVisibility(View.VISIBLE);
        tvVoiceResult.setVisibility(View.VISIBLE);
        tvVoiceResult.setText("👆 Appuyez sur le micro et prononcez");
        tvVoiceResult.setTextColor(Color.parseColor("#999999"));
    }

    // ==================== VALIDATION ====================

    private void validateAnswer() {
        if (quizBloque) {
            Toast.makeText(this, "⚠️ Quiz bloqué ! Regardez l'écran", Toast.LENGTH_LONG).show();
            return;
        }

        switch (currentQuestionType) {
            case "ecriture": validateWrittenAnswer(); break;
            case "prononciation": validatePronunciationAnswer(); break;
            default: validateQCMAnswer(); break;
        }
    }

    private void validateQCMAnswer() {
        if (selectedOptionIndex == -1) {
            Toast.makeText(this, "⚠️ Choisissez une réponse", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            List<JSONObject> currentQuestions = getCurrentLevelQuestions();
            JSONObject q = currentQuestions.get(currentQuestionIndex);
            String selectedAnswer = q.getJSONArray("options").getString(selectedOptionIndex);
            checkAnswer(selectedAnswer);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur validation", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateWrittenAnswer() {
        String userAnswer = etWrittenAnswer.getText().toString().trim();
        if (userAnswer.isEmpty()) {
            Toast.makeText(this, "⚠️ Écrivez votre réponse", Toast.LENGTH_SHORT).show();
            return;
        }
        checkAnswer(userAnswer);
    }

    private void validatePronunciationAnswer() {
        if (userSpokenText.isEmpty()) {
            Toast.makeText(this, "⚠️ Appuyez sur le micro et prononcez d'abord", Toast.LENGTH_SHORT).show();
            return;
        }
        checkAnswer(userSpokenText);
    }

    // ✅ REMPLACER TOUTE LA MÉTHODE
    private void checkAnswer(String userAnswer) {
        boolean isCorrect = cleanString(userAnswer).equalsIgnoreCase(cleanString(currentCorrectAnswer));

        if (isCorrect) {
            totalScore += 10;
            levelScore++;
        }

        int animRes = isCorrect ? R.raw.success : R.raw.error;

        showLottie(animRes, () -> {
            runOnUiThread(() -> {
                Toast.makeText(QuestionsList.this,
                        isCorrect ? "✅ Correct ! +10 pts" : "❌ Réponse: " + currentCorrectAnswer,
                        Toast.LENGTH_SHORT).show();
            });
            currentQuestionIndex++;
            selectedOptionIndex = -1;
            userSpokenText = "";
            new Handler().postDelayed(() -> displayCurrentQuestion(), 500);
        });
    }

    private String cleanString(String input) {
        if (input == null) return "";
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    // ==================== NAVIGATION & SCORES PAR NIVEAU ====================

    // ✅ REMPLACER votre finishLevel() actuelle par celle-ci
    private void finishLevel() {
        String levelName = getLevelName();
        int totalQuestions = getCurrentLevelQuestions().size();

        int minimumRequis = (int) Math.ceil(totalQuestions * POURCENTAGE_PASSING);

        switch (currentLevel) {
            case 0: scoreNiveauFacile = levelScore; totalQuestionsFacile = totalQuestions; break;
            case 1: scoreNiveauMoyen = levelScore; totalQuestionsMoyen = totalQuestions; break;
            case 2: scoreNiveauDifficile = levelScore; totalQuestionsDifficile = totalQuestions; break;
        }

        if (levelScore >= minimumRequis) {
            afficherDialogSucces(levelName, levelScore, totalQuestions);
        } else {
            afficherDialogEchec(levelName, levelScore, totalQuestions);
        }
    }

    // ✅ AJOUTER cette méthode (juste après finishLevel)
    // ✅ REMPLACER votre afficherDialogSucces actuelle par celle-ci
    private void afficherDialogSucces(String niveau, int score, int total) {
        // Lottie celebration plein écran d'abord
        showLottie(R.raw.celebration, () -> {
            // Puis afficher le dialogue
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Translucent_NoTitleBar);
                builder.setCancelable(false);

                // Layout principal avec fond dégradé
                LinearLayout mainLayout = new LinearLayout(this);
                mainLayout.setOrientation(LinearLayout.VERTICAL);
                mainLayout.setGravity(Gravity.CENTER);
                mainLayout.setPadding(30, 50, 30, 50);

                // Fond dégradé vert
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.RECTANGLE);
                bg.setColors(new int[]{Color.parseColor("#E8F5E9"), Color.WHITE});
                bg.setCornerRadius(30);
                mainLayout.setBackground(bg);

                String emoji;
                switch (currentLevel) {
                    case 0: emoji = "🟢"; break;
                    case 1: emoji = "🟠"; break;
                    case 2: emoji = "🔴"; break;
                    default: emoji = "⭐";
                }

                // Icône succès
                TextView ic = new TextView(this);
                ic.setText("🏆");
                ic.setTextSize(70);
                ic.setGravity(Gravity.CENTER);

                // Titre
                TextView ti = new TextView(this);
                ti.setText(emoji + " Niveau " + niveau + " Réussi !");
                ti.setTextSize(24);
                ti.setTextColor(Color.parseColor("#2E7D32"));
                ti.setTypeface(null, android.graphics.Typeface.BOLD);
                ti.setGravity(Gravity.CENTER);
                ti.setPadding(0, 16, 0, 8);

                // Message
                TextView ms = new TextView(this);
                ms.setText("🎉 Félicitations ! Vous avez réussi ce niveau !");
                ms.setTextSize(14);
                ms.setTextColor(Color.parseColor("#666666"));
                ms.setGravity(Gravity.CENTER);
                ms.setPadding(20, 0, 20, 16);

                // Cercle score
                LinearLayout scoreCircle = new LinearLayout(this);
                scoreCircle.setGravity(Gravity.CENTER);
                scoreCircle.setPadding(0, 0, 0, 16);

                TextView scoreText = new TextView(this);
                scoreText.setText(score + "/" + total);
                scoreText.setTextSize(36);
                scoreText.setTextColor(Color.parseColor("#58CC02"));
                scoreText.setTypeface(null, android.graphics.Typeface.BOLD);
                scoreText.setGravity(Gravity.CENTER);
                scoreText.setPadding(30, 20, 30, 20);

                GradientDrawable circleBg = new GradientDrawable();
                circleBg.setShape(GradientDrawable.OVAL);
                circleBg.setColor(Color.WHITE);
                circleBg.setStroke(3, Color.parseColor("#58CC02"));
                scoreText.setBackground(circleBg);

                scoreCircle.addView(scoreText);

                // Points
                TextView pts = new TextView(this);
                pts.setText("+" + (score * 10) + " points gagnés");
                pts.setTextSize(16);
                pts.setTextColor(Color.parseColor("#58CC02"));
                pts.setGravity(Gravity.CENTER);
                pts.setPadding(0, 0, 0, 16);

                // Barre de progression
                LinearLayout progressBar = new LinearLayout(this);
                progressBar.setOrientation(LinearLayout.HORIZONTAL);
                progressBar.setGravity(Gravity.CENTER);
                progressBar.setPadding(0, 0, 0, 24);

                LinearLayout progressBg = new LinearLayout(this);
                progressBg.setLayoutParams(new LinearLayout.LayoutParams(250, 20));
                progressBg.setBackgroundColor(Color.parseColor("#E0E0E0"));
                progressBg.setPadding(2, 2, 2, 2);

                GradientDrawable progressBgDrawable = new GradientDrawable();
                progressBgDrawable.setShape(GradientDrawable.RECTANGLE);
                progressBgDrawable.setCornerRadius(10);
                progressBgDrawable.setColor(Color.parseColor("#E0E0E0"));
                progressBg.setBackground(progressBgDrawable);

                // Barre remplie
                View progressFill = new View(this);
                int fillWidth = (int) (246 * ((float) score / total));
                progressFill.setLayoutParams(new LinearLayout.LayoutParams(fillWidth, 16));

                GradientDrawable fillDrawable = new GradientDrawable();
                fillDrawable.setShape(GradientDrawable.RECTANGLE);
                fillDrawable.setCornerRadius(8);

                int totalQuestions = getCurrentLevelQuestions().size();
                int minimumRequis = (int) Math.ceil(totalQuestions * POURCENTAGE_PASSING);

                if (score >= minimumRequis) {
                    fillDrawable.setColors(new int[]{Color.parseColor("#58CC02"), Color.parseColor("#76FF03")});
                } else {
                    fillDrawable.setColors(new int[]{Color.parseColor("#FF9800"), Color.parseColor("#FFC107")});
                }
                fillDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                progressFill.setBackground(fillDrawable);

                progressBg.addView(progressFill);
                progressBar.addView(progressBg);

                mainLayout.addView(ic);
                mainLayout.addView(ti);
                mainLayout.addView(ms);
                mainLayout.addView(scoreCircle);
                mainLayout.addView(pts);
                mainLayout.addView(progressBar);

                AlertDialog dialog;
                if (currentLevel < 2) {
                    String next = currentLevel == 0 ? "Moyen" : "Difficile";
                    String nxtEmoji = currentLevel == 0 ? "🟠" : "🔴";

                    Button btnC = new Button(this);
                    btnC.setText("▶ Continuer vers le niveau " + next + " " + nxtEmoji);
                    btnC.setTextColor(Color.WHITE);
                    btnC.setTextSize(16);
                    btnC.setTypeface(null, android.graphics.Typeface.BOLD);
                    GradientDrawable btnBg = new GradientDrawable();
                    btnBg.setShape(GradientDrawable.RECTANGLE);
                    btnBg.setCornerRadius(25);
                    btnBg.setColors(new int[]{Color.parseColor("#58CC02"), Color.parseColor("#76FF03")});
                    btnC.setBackground(btnBg);
                    btnC.setPadding(20, 15, 20, 15);
                    LinearLayout.LayoutParams btnCParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    btnCParams.setMargins(10, 0, 10, 12);
                    btnC.setLayoutParams(btnCParams);

                    Button btnR = new Button(this);
                    btnR.setText("🔄 Refaire ce niveau");
                    btnR.setTextColor(Color.WHITE);
                    btnR.setTextSize(14);
                    GradientDrawable btnRg = new GradientDrawable();
                    btnRg.setShape(GradientDrawable.RECTANGLE);
                    btnRg.setCornerRadius(25);
                    btnRg.setColors(new int[]{Color.parseColor("#FF9800"), Color.parseColor("#FFC107")});
                    btnR.setBackground(btnRg);
                    btnR.setPadding(15, 12, 15, 12);
                    LinearLayout.LayoutParams btnRParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    btnRParams.setMargins(10, 0, 10, 0);
                    btnR.setLayoutParams(btnRParams);

                    mainLayout.addView(btnC);
                    mainLayout.addView(btnR);

                    builder.setView(mainLayout);
                    dialog = builder.create();
                    btnC.setOnClickListener(v -> { dialog.dismiss(); advanceLevel(); });
                    btnR.setOnClickListener(v -> { dialog.dismiss(); refaireNiveau(); });
                } else {
                    TextView f = new TextView(this);
                    f.setText("🏆 Tous les niveaux sont terminés !");
                    f.setTextSize(16);
                    f.setTextColor(Color.parseColor("#FF9800"));
                    f.setTypeface(null, android.graphics.Typeface.BOLD);
                    f.setGravity(Gravity.CENTER);
                    f.setPadding(0, 8, 0, 16);

                    Button btnS = new Button(this);
                    btnS.setText("🏆 Voir le score final");
                    btnS.setTextColor(Color.WHITE);
                    btnS.setTextSize(16);
                    btnS.setTypeface(null, android.graphics.Typeface.BOLD);
                    GradientDrawable btnBg = new GradientDrawable();
                    btnBg.setShape(GradientDrawable.RECTANGLE);
                    btnBg.setCornerRadius(25);
                    btnBg.setColors(new int[]{Color.parseColor("#58CC02"), Color.parseColor("#76FF03")});
                    btnS.setBackground(btnBg);
                    btnS.setPadding(20, 15, 20, 15);
                    LinearLayout.LayoutParams btnSParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    btnSParams.setMargins(10, 0, 10, 0);
                    btnS.setLayoutParams(btnSParams);

                    mainLayout.addView(f);
                    mainLayout.addView(btnS);

                    builder.setView(mainLayout);
                    dialog = builder.create();
                    btnS.setOnClickListener(v -> { dialog.dismiss(); showLottie(R.raw.champion, () -> goToScorePage(true)); });
                }

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                    dialog.getWindow().setWindowAnimations(android.R.style.Animation_Translucent);
                }
                dialog.show();
            });
        });
    }

    // ✅ MÉTHODE DU DIALOG D'ÉCHEC (UTILISE LE LAYOUT XML)
    // ✅ REMPLACER votre afficherDialogEchec actuelle par celle-ci
    private void afficherDialogEchec(String niveau, int score, int total) {
        showLottie(R.raw.error, () -> {
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Translucent_NoTitleBar);
                builder.setCancelable(false);

                LinearLayout mainLayout = new LinearLayout(this);
                mainLayout.setOrientation(LinearLayout.VERTICAL);
                mainLayout.setGravity(Gravity.CENTER);
                mainLayout.setPadding(30, 50, 30, 50);

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.RECTANGLE);
                bg.setColors(new int[]{Color.parseColor("#FFEBEE"), Color.WHITE});
                bg.setCornerRadius(30);
                mainLayout.setBackground(bg);

                String emoji;
                switch (currentLevel) {
                    case 0: emoji = "🟢"; break;
                    case 1: emoji = "🟠"; break;
                    case 2: emoji = "🔴"; break;
                    default: emoji = "⭐";
                }

                // Icône échec
                TextView ic = new TextView(this);
                ic.setText("😢");
                ic.setTextSize(70);
                ic.setGravity(Gravity.CENTER);

                // Titre
                TextView ti = new TextView(this);
                ti.setText(emoji + " Niveau " + niveau + " Échoué !");
                ti.setTextSize(24);
                ti.setTextColor(Color.parseColor("#C62828"));
                ti.setTypeface(null, android.graphics.Typeface.BOLD);
                ti.setGravity(Gravity.CENTER);
                ti.setPadding(0, 16, 0, 8);

                // Message
                TextView ms = new TextView(this);
                ms.setText("Vous n'avez pas atteint le score minimum");
                ms.setTextSize(14);
                ms.setTextColor(Color.parseColor("#666666"));
                ms.setGravity(Gravity.CENTER);
                ms.setPadding(20, 0, 20, 16);

                // Cercle score
                LinearLayout scoreCircle = new LinearLayout(this);
                scoreCircle.setGravity(Gravity.CENTER);
                scoreCircle.setPadding(0, 0, 0, 8);

                TextView scoreText = new TextView(this);
                scoreText.setText(score + "/" + total);
                scoreText.setTextSize(36);
                scoreText.setTextColor(Color.parseColor("#C62828"));
                scoreText.setTypeface(null, android.graphics.Typeface.BOLD);
                scoreText.setGravity(Gravity.CENTER);
                scoreText.setPadding(30, 20, 30, 20);

                GradientDrawable circleBg = new GradientDrawable();
                circleBg.setShape(GradientDrawable.OVAL);
                circleBg.setColor(Color.WHITE);
                circleBg.setStroke(3, Color.parseColor("#C62828"));
                scoreText.setBackground(circleBg);

                scoreCircle.addView(scoreText);


                int totalQuestions = getCurrentLevelQuestions().size();
                int minimumRequis = (int) Math.ceil(totalQuestions * POURCENTAGE_PASSING);
                // Score minimum
                TextView minText = new TextView(this);
                minText.setText("Minimum requis : " + minimumRequis + "/" + total);
                minText.setTextSize(14);
                minText.setTextColor(Color.parseColor("#999999"));
                minText.setGravity(Gravity.CENTER);
                minText.setPadding(0, 0, 0, 20);

                // Barre de progression
                LinearLayout progressBar = new LinearLayout(this);
                progressBar.setOrientation(LinearLayout.HORIZONTAL);
                progressBar.setGravity(Gravity.CENTER);
                progressBar.setPadding(0, 0, 0, 24);

                LinearLayout progressBg = new LinearLayout(this);
                progressBg.setLayoutParams(new LinearLayout.LayoutParams(250, 20));
                progressBg.setPadding(2, 2, 2, 2);

                GradientDrawable progressBgDrawable = new GradientDrawable();
                progressBgDrawable.setShape(GradientDrawable.RECTANGLE);
                progressBgDrawable.setCornerRadius(10);
                progressBgDrawable.setColor(Color.parseColor("#E0E0E0"));
                progressBg.setBackground(progressBgDrawable);

                // Barre remplie (rouge)
                View progressFill = new View(this);
                int fillWidth = (int) (246 * ((float) score / total));
                progressFill.setLayoutParams(new LinearLayout.LayoutParams(fillWidth, 16));

                GradientDrawable fillDrawable = new GradientDrawable();
                fillDrawable.setShape(GradientDrawable.RECTANGLE);
                fillDrawable.setCornerRadius(8);
                fillDrawable.setColors(new int[]{Color.parseColor("#FF5252"), Color.parseColor("#FF8A80")});
                fillDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                progressFill.setBackground(fillDrawable);

                // Marqueur minimum
                TextView markerText = new TextView(this);
                markerText.setText("|");
                markerText.setTextSize(20);
                markerText.setTextColor(Color.parseColor("#58CC02"));
                markerText.setTypeface(null, android.graphics.Typeface.BOLD);
                markerText.setPadding(5, 0, 5, 0);

                progressBg.addView(progressFill);
                progressBar.addView(progressBg);
                progressBar.addView(markerText);

                mainLayout.addView(ic);
                mainLayout.addView(ti);
                mainLayout.addView(ms);
                mainLayout.addView(scoreCircle);
                mainLayout.addView(minText);
                mainLayout.addView(progressBar);

                // Boutons
                Button btnR = new Button(this);
                btnR.setText("🔄 Réessayer ce niveau");
                btnR.setTextColor(Color.WHITE);
                btnR.setTextSize(16);
                btnR.setTypeface(null, android.graphics.Typeface.BOLD);
                GradientDrawable btnBg = new GradientDrawable();
                btnBg.setShape(GradientDrawable.RECTANGLE);
                btnBg.setCornerRadius(25);
                btnBg.setColors(new int[]{Color.parseColor("#FF9800"), Color.parseColor("#FFC107")});
                btnR.setBackground(btnBg);
                btnR.setPadding(20, 15, 20, 15);
                LinearLayout.LayoutParams btnRParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                btnRParams.setMargins(10, 0, 10, 12);
                btnR.setLayoutParams(btnRParams);

                Button btnQ = new Button(this);
                btnQ.setText("🏠 Changer de langue");
                btnQ.setTextColor(Color.parseColor("#666666"));
                btnQ.setTextSize(14);
                GradientDrawable btnQbg = new GradientDrawable();
                btnQbg.setShape(GradientDrawable.RECTANGLE);
                btnQbg.setCornerRadius(25);
                btnQbg.setColor(Color.parseColor("#F5F5F5"));
                btnQbg.setStroke(1, Color.parseColor("#DDDDDD"));
                btnQ.setBackground(btnQbg);
                btnQ.setPadding(15, 12, 15, 12);
                LinearLayout.LayoutParams btnQParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                btnQParams.setMargins(10, 0, 10, 0);
                btnQ.setLayoutParams(btnQParams);

                mainLayout.addView(btnR);
                mainLayout.addView(btnQ);

                builder.setView(mainLayout);
                AlertDialog dialog = builder.create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                    dialog.getWindow().setWindowAnimations(android.R.style.Animation_Translucent);
                }

                btnR.setOnClickListener(v -> { dialog.dismiss(); refaireNiveau(); });
                btnQ.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(QuestionsList.this, ChoisirLangues.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });

                dialog.show();
            });
        });
    }

    private void afficherScoreNiveau(String niveau, int score, int total, boolean reussi) {
        String emoji = reussi ? "✅" : "❌";
        String message = emoji + " " + niveau + " : " + score + "/" + total;

        if (reussi) {
            message += " (+" + (score * 10) + " pts)";
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // ✅ REMPLACER votre advanceLevel() actuelle
    private void advanceLevel() {
        btnRefaireNiveau.setVisibility(View.GONE);
        btnValidate.setVisibility(View.VISIBLE);
        currentLevel++;
        if (currentLevel <= 2) {
            startLevel(currentLevel);
        } else {
            goToScorePage(true);
        }
    }

    private List<JSONObject> getCurrentLevelQuestions() {
        switch (currentLevel) {
            case 0: return easyQuestions;
            case 1: return mediumQuestions;
            case 2: return hardQuestions;
            default: return new ArrayList<>();
        }
    }

    private void updateLevelUI() {
        String emoji;
        switch (currentLevel) {
            case 0: emoji = "🟢"; break;
            case 1: emoji = "🟠"; break;
            case 2: emoji = "🔴"; break;
            default: emoji = "⭐";
        }
        tvLevelIndicator.setText(emoji + " Niveau " + getLevelName());
    }

    private void updateProgressBar() {
        if (progressBar != null) {
            List<JSONObject> currentQuestions = getCurrentLevelQuestions();
            if (!currentQuestions.isEmpty()) {
                float progress = (float) currentQuestionIndex / currentQuestions.size();
                progressBar.post(() -> {
                    progressBar.getLayoutParams().width =
                            (int) (progressBar.getRootView().getWidth() * progress);
                    progressBar.requestLayout();
                });
            }
        }
    }

    private String getLevelName() {
        switch (currentLevel) {
            case 0: return "Facile";
            case 1: return "Moyen";
            case 2: return "Difficile";
            default: return "";
        }
    }

    private CardView createOptionCard(String text, int index) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);
        card.setRadius(12);
        card.setCardElevation(4);
        card.setUseCompatPadding(true);
        card.setClickable(true);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        TextView letter = new TextView(this);
        letter.setText(String.valueOf((char) ('A' + index)));
        letter.setTextSize(16);
        letter.setTextColor(Color.WHITE);
        letter.setBackgroundResource(R.drawable.circle_green);
        letter.setGravity(Gravity.CENTER);
        letter.setWidth(60);
        letter.setHeight(60);

        TextView optionText = new TextView(this);
        optionText.setText(text);
        optionText.setTextSize(16);
        optionText.setTextColor(Color.parseColor("#333333"));
        optionText.setPadding(16, 0, 0, 0);

        layout.addView(letter);
        layout.addView(optionText);
        card.addView(layout);

        card.setOnClickListener(v -> {
            selectedOptionIndex = index;
            for (int i = 0; i < optionsContainer.getChildCount(); i++) {
                optionsContainer.getChildAt(i).setBackgroundColor(Color.WHITE);
            }
            card.setBackgroundColor(Color.parseColor("#E8F5E9"));
        });

        return card;
    }

    private void goToScorePage(boolean completed) {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (faceDetector != null) faceDetector.close();
        cameraExecutor.shutdown();

        Intent intent = new Intent(this, ScoreFinal.class);
        intent.putExtra("score", totalScore);
        intent.putExtra("total", totalQuestionsFacile + totalQuestionsMoyen + totalQuestionsDifficile);
        intent.putExtra("language", selectedLanguage);
        intent.putExtra("completed", completed);

        intent.putExtra("scoreFacile", scoreNiveauFacile);
        intent.putExtra("totalFacile", totalQuestionsFacile);
        intent.putExtra("scoreMoyen", scoreNiveauMoyen);
        intent.putExtra("totalMoyen", totalQuestionsMoyen);
        intent.putExtra("scoreDifficile", scoreNiveauDifficile);
        intent.putExtra("totalDifficile", totalQuestionsDifficile);

        startActivity(intent);
        finish();
    }

    // ✅ AJOUTER CES 2 MÉTHODES
    private void initLottie() {
        lottieAnimation = new LottieAnimationView(this);
        lottieAnimation.setVisibility(View.GONE);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        View rootView = findViewById(android.R.id.content);
        if (rootView instanceof FrameLayout) {
            ((FrameLayout) rootView).addView(lottieAnimation, params);
        }
    }

    private void showLottie(int rawRes, Runnable onEnd) {
        lottieAnimation.setVisibility(View.VISIBLE);
        lottieAnimation.setAnimation(rawRes);
        lottieAnimation.setRepeatCount(0);
        lottieAnimation.setSpeed(1.5f);
        lottieAnimation.playAnimation();

        lottieAnimation.addAnimatorListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                lottieAnimation.setVisibility(View.GONE);
                lottieAnimation.removeAllAnimatorListeners();
                if (onEnd != null) onEnd.run();
            }
            @Override public void onAnimationStart(android.animation.Animator a) {}
            @Override public void onAnimationCancel(android.animation.Animator a) {}
            @Override public void onAnimationRepeat(android.animation.Animator a) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (faceDetector != null) faceDetector.close();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                Toast.makeText(this, "Permission micro refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
}