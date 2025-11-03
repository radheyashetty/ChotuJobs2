package com.chotujobs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.chotujobs.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("chotujobs_prefs", MODE_PRIVATE);

        // Check if already logged in
        if (auth.getCurrentUser() != null && prefs.getString("user_role", null) != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding.btnEmailLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, EmailLoginActivity.class));
        });

        binding.btnPhoneLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, PhoneAuthActivity.class));
        });
    }
}
