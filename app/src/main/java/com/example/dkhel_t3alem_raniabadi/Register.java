package com.example.dkhel_t3alem_raniabadi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class Register extends AppCompatActivity {

    private SupabaseConfig supabaseConfig;

    private TextInputEditText emField;
    private TextInputEditText telField;
    private TextInputEditText fullNameField;
    private TextInputEditText passField;

    private AppCompatButton btnSignUp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        supabaseConfig = new SupabaseConfig(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        btnSignUp = findViewById(R.id.btnSignUp);
        fullNameField = findViewById(R.id.fullNameInput);
        telField = findViewById(R.id.phoneInput);
        emField = findViewById(R.id.emailInput);
        passField = findViewById(R.id.passwordInput);

        btnSignUp.setOnClickListener(v -> {
            try {

                if (fullNameField.getText().toString().isEmpty() || emField.getText().toString().isEmpty() || telField.getText().toString().isEmpty() || passField.getText().toString().isEmpty()) {
                    Toast.makeText(Register.this, "Veuillez remplir tous les champs", Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(Register.this, "dd: " + fullNameField.getText().toString() + "dd: " + telField.getText().toString() + "dd: " + emField.getText().toString() + "dd: " + passField.getText().toString(), Toast.LENGTH_LONG).show();


                supabaseConfig.signUp(fullNameField.getText().toString(),telField.getText().toString(),emField.getText().toString(),passField.getText().toString(),new SupabaseConfig.AuthCallback(){
                    @Override
                    public void onSuccess(JSONObject response) {
                        startActivity(new Intent(Register.this, MainActivity.class));
                    }
                    @Override
                    public void onError(String errorMessage,byte[] dataErrors) {
                        Toast.makeText(Register.this, "Erreur: " + errorMessage + "dataErrors: " + dataErrors, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Toast.makeText(Register.this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

    }
}