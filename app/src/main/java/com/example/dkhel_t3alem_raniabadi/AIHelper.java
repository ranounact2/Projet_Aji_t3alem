package com.example.dkhel_t3alem_raniabadi;

import android.content.Context;
import android.util.Log;
import com.android.volley.*;
import com.android.volley.toolbox.*;
import org.json.*;
import java.io.*;
import java.util.*;

public class AIHelper {

    private static final String TAG = "AIHelper";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_API_KEY = "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";

    private List<JSONObject> baseConnaissances;
    private Random random = new Random();
    private Context context;
    private RequestQueue queue;

    public AIHelper(Context context) {
        this.context = context;
        this.queue = Volley.newRequestQueue(context);
        this.baseConnaissances = new ArrayList<>();
        chargerBaseConnaissances();
    }

    /**
     * Étape 1 : Charger la base de connaissances depuis res/raw/
     */
    private void chargerBaseConnaissances() {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.indices_questions);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                baseConnaissances.add(array.getJSONObject(i));
            }
            Log.d(TAG, "✅ Base chargée: " + baseConnaissances.size() + " documents");
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur chargement: " + e.getMessage());
        }
    }

    /**
     * Étape 2 : Rechercher les documents pertinents (RETRIEVAL)
     */
    private String recupererContexte(String question) {
        StringBuilder contexte = new StringBuilder();
        String questionClean = nettoyer(question);

        for (JSONObject doc : baseConnaissances) {
            try {
                String qText = nettoyer(doc.optString("question", ""));
                String reponse = doc.optString("reponse", "");
                JSONArray indices = doc.optJSONArray("indices");

                // Vérifier la pertinence
                if (qText.contains(questionClean) || questionClean.contains(qText)) {
                    contexte.append("📚 Question similaire: ").append(doc.optString("question")).append("\n");
                    contexte.append("✅ Réponse: ").append(reponse).append("\n");
                    if (indices != null && indices.length() > 0) {
                        contexte.append("💡 Indices: ");
                        for (int i = 0; i < indices.length(); i++) {
                            contexte.append(indices.getString(i)).append(" | ");
                        }
                        contexte.append("\n");
                    }
                    contexte.append("\n");
                }
            } catch (Exception e) {}
        }

        if (contexte.length() == 0) {
            contexte.append("Aucun document trouvé. Réponds de manière générale.");
        }

        Log.d(TAG, "📄 Contexte trouvé: " + contexte.length() + " caractères");
        return contexte.toString();
    }

    /**
     * Étape 3 : Envoyer à Groq avec le contexte (GENERATION)
     */
    public void demanderAide(String question, String langue, AIResponseCallback callback) {
        // 1. Récupérer le contexte
        String contexte = recupererContexte(question);

        // 2. Créer le prompt avec le contexte
        String prompt = "Tu es un professeur de langues expert.\n\n" +
                "📚 DOCUMENTS DE RÉFÉRENCE (utilise ces informations) :\n" +
                contexte + "\n\n" +
                "❓ QUESTION de l'étudiant : " + question + "\n" +
                "🌍 Langue concernée : " + langue + "\n\n" +
                "CONSIGNES :\n" +
                "- Explique de manière simple et pédagogique\n" +
                "- Si c'est une question de quiz, ne donne PAS la réponse directement\n" +
                "- Donne des indices et des astuces pour trouver\n" +
                "- Utilise des emojis pour rendre l'explication fun 🎓✨\n" +
                "- Réponds en français";

        // 3. Appeler Groq
        appelerGroq(prompt, callback);
    }

    /**
     * Appel API Groq
     */
    private void appelerGroq(String prompt, AIResponseCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("model", "llama-3.3-70b-versatile");

            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", prompt);
            messages.put(msg);
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 1024);
        } catch (Exception e) {
            callback.onError("Erreur création requête");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, GROQ_URL, body,
                response -> {
                    try {
                        String reponse = response.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        callback.onSuccess(reponse);
                    } catch (Exception e) {
                        callback.onError("Erreur parsing réponse");
                    }
                },
                error -> {
                    String msg = error.getMessage();
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        msg = new String(error.networkResponse.data);
                    }
                    callback.onError(msg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + GROQ_API_KEY);
                return headers;
            }
        };

        queue.add(request);
    }

    private String nettoyer(String text) {
        if (text == null) return "";
        return text.toLowerCase().trim()
                .replace("?", "").replace("؟", "")
                .replace("\"", "").replace("'", "");
    }

    public interface AIResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}