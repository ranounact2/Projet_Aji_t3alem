package com.example.dkhel_t3alem_raniabadi;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class CameraBlockOverlay {

    private Dialog blockDialog;
    private Context context;

    public CameraBlockOverlay(Context context) {
        this.context = context;
    }

    /**
     * Affiche l'overlay de blocage
     */
    public void bloquerQuiz(String raison) {
        if (blockDialog != null && blockDialog.isShowing()) {
            return; // Déjà bloqué
        }

        blockDialog = new Dialog(context);
        blockDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        blockDialog.setContentView(R.layout.dialog_camera_block);
        blockDialog.setCancelable(false); // ⚠️ Impossible de fermer

        // Fond transparent
        Window window = blockDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.CENTER);
        }

        // Texte d'alerte
        TextView tvRaison = blockDialog.findViewById(R.id.tvRaison);
        tvRaison.setText(raison);

        // Bouton "Je regarde l'écran"
        Button btnReprendre = blockDialog.findViewById(R.id.btnReprendre);
        btnReprendre.setBackgroundColor(Color.parseColor("#58CC02"));
        btnReprendre.setOnClickListener(v -> {
            // L'overlay sera retiré quand la caméra détecte à nouveau le visage
        });

        blockDialog.show();
    }

    /**
     * Enlève l'overlay de blocage
     */
    public void debloquerQuiz() {
        if (blockDialog != null && blockDialog.isShowing()) {
            blockDialog.dismiss();
            blockDialog = null;
        }
    }

    /**
     * Vérifie si le quiz est bloqué
     */
    public boolean estBloque() {
        return blockDialog != null && blockDialog.isShowing();
    }
}