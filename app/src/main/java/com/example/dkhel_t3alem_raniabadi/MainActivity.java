package com.example.dkhel_t3alem_raniabadi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    private AppCompatButton btnLogin;
    private TextView btnRegister;

    private TextInputEditText emField;
    private TextInputEditText passField;


    private SupabaseConfig supabaseConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);


        supabaseConfig = new SupabaseConfig(this);


        // Affiche d'abord le splash screen
        setContentView(R.layout.activity_splash);

        // Appliquer les insets au layout splash
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Après 2 secondes, changer vers le layout de login
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.activity_main);

                // Appliquer les insets au layout login
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });

                btnLogin = findViewById(R.id.buttonLogin);
                btnRegister = findViewById(R.id.btnRegister);

                emField = findViewById(R.id.email);
                passField = findViewById(R.id.password);


                btnRegister.setOnClickListener(v->{
                    try {
                        startActivity(new Intent(MainActivity.this, Register.class));
                    }catch (Exception e){
                        Toast.makeText(MainActivity.this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                });

                btnLogin.setOnClickListener(v -> {
                    try {

                        Log.d("MainActivity", "Email: " + emField.getText().toString() + ", Password: " + passField.getText().toString());
                        supabaseConfig.signIn(emField.getText().toString(),passField.getText().toString(),new SupabaseConfig.AuthCallback(){


                            @Override
                            public void onSuccess(JSONObject response) {
                                startActivity(new Intent(MainActivity.this, PermissionsActivity.class));
                                Toast.makeText(MainActivity.this, "Bonjour  ", Toast.LENGTH_LONG).show();
                            }
                            @Override
                            public void onError(String errorMessage,byte[] dataErrors) {
                                Toast.makeText(MainActivity.this, "Erreur: " + errorMessage + "dataErrors: " + dataErrors , Toast.LENGTH_LONG).show();
                            }
                        });

                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                });


            }
        }, 1500);
    }
}