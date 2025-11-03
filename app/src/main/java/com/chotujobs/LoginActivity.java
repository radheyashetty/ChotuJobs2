package com.chotujobs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    
    private static final String TAG = "LoginActivity";
    private FirebaseAuth auth;
    private FirestoreService firestoreService;
    private SharedPreferences prefs;
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        auth = FirebaseAuth.getInstance();
        firestoreService = FirestoreService.getInstance();
        prefs = getSharedPreferences("chotujobs_prefs", MODE_PRIVATE);
        
        // Check if already logged in
        if (auth.getCurrentUser() != null && prefs.getString("user_role", null) != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        
        initializeViews();
    }
    
    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.btnLogin);
        
        loginButton.setOnClickListener(v -> handleLogin());
    }
    
    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }
        
        loginButton.setEnabled(false);
        
        // Sign in with Firebase Auth
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Login successful");
                        String uid = auth.getCurrentUser().getUid();
                        fetchUserProfileAndNavigate(uid);
                    } else {
                        // If login fails, try creating user
                        Log.w(TAG, "Login failed, attempting to create user", task.getException());
                        createUserAndLogin(email, password);
                    }
                });
    }
    
    private void createUserAndLogin(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User creation successful");
                        String uid = auth.getCurrentUser().getUid();
                        String role = "labour"; // Default role
                        
                        // Create user profile in Firestore
                        User user = new User(email.split("@")[0], email, role);
                        firestoreService.createUserProfile(user, uid, success -> {
                            if (success) {
                                // Save role to SharedPreferences
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("user_role", role);
                                editor.putString("user_id", uid);
                                editor.apply();
                                
                                navigateToMain();
                            } else {
                                Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                                loginButton.setEnabled(true);
                            }
                        });
                    } else {
                        Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        loginButton.setEnabled(true);
                    }
                });
    }
    
    private void fetchUserProfileAndNavigate(String uid) {
        firestoreService.getUserProfile(uid, user -> {
            if (user != null) {
                // Save role to SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user_role", user.getRole());
                editor.putString("user_id", uid);
                editor.apply();
                
                navigateToMain();
            } else {
                Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                loginButton.setEnabled(true);
            }
        });
    }
    
    private void navigateToMain() {
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
