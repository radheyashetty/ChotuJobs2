package com.chotujobs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chotujobs.databinding.ActivityEmailLoginBinding;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;

public class EmailLoginActivity extends AppCompatActivity {

    private static final String TAG = "EmailLoginActivity";
    private FirebaseAuth auth;
    private FirestoreService firestoreService;
    private SharedPreferences prefs;
    private ActivityEmailLoginBinding binding;
    private boolean isSignUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmailLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firestoreService = FirestoreService.getInstance();
        prefs = getSharedPreferences("chotujobs_prefs", MODE_PRIVATE);

        initializeViews();
    }

    private void initializeViews() {
        binding.btnLogin.setOnClickListener(v -> handleLogin());
        binding.btnSignUp.setOnClickListener(v -> toggleSignUp());
    }

    private void toggleSignUp() {
        isSignUp = !isSignUp;
        if (isSignUp) {
            binding.roleSpinner.setVisibility(View.VISIBLE);
            binding.btnLogin.setText("Create Account");
            binding.btnSignUp.setText("Cancel");
        } else {
            binding.roleSpinner.setVisibility(View.GONE);
            binding.btnLogin.setText("Login");
            binding.btnSignUp.setText("Sign Up");
        }
    }

    private void handleLogin() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnLogin.setEnabled(false);

        if (isSignUp) {
            createUserAndLogin(email, password);
        } else {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Login successful");
                            String uid = auth.getCurrentUser().getUid();
                            fetchUserProfileAndNavigate(uid);
                        } else {
                            Log.w(TAG, "Login failed", task.getException());
                            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            binding.btnLogin.setEnabled(true);
                        }
                    });
        }
    }

    private void createUserAndLogin(String email, String password) {
        String role = binding.roleSpinner.getSelectedItem().toString().toLowerCase();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User creation successful");
                        String uid = auth.getCurrentUser().getUid();

                        User user = new User(email.split("@")[0], email, role);
                        firestoreService.createUserProfile(user, uid, success -> {
                            if (success) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("user_role", role);
                                editor.putString("user_id", uid);
                                editor.apply();

                                navigateToMain();
                            } else {
                                Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                                binding.btnLogin.setEnabled(true);
                            }
                        });
                    } else {
                        Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        binding.btnLogin.setEnabled(true);
                    }
                });
    }

    private void fetchUserProfileAndNavigate(String uid) {
        firestoreService.getUserProfile(uid, user -> {
            if (user != null) {
                if(user.getRole() == null || user.getRole().isEmpty()){
                    promptForRole(uid);
                }
                else{
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user_role", user.getRole());
                    editor.putString("user_id", uid);
                    editor.apply();

                    navigateToMain();
                }
            } else {
                promptForRole(uid);
            }
        });
    }

    private void promptForRole(String uid) {
        RoleSelectionDialogFragment dialog = new RoleSelectionDialogFragment();
        dialog.setOnRoleSelectedListener(role -> {
            User user = new User(auth.getCurrentUser().getEmail().split("@")[0], auth.getCurrentUser().getEmail(), role);
            firestoreService.createUserProfile(user, uid, success -> {
                if (success) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user_role", role);
                    editor.putString("user_id", uid);
                    editor.apply();

                    navigateToMain();
                } else {
                    Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                    binding.btnLogin.setEnabled(true);
                }
            });
        });
        dialog.show(getSupportFragmentManager(), "role_selection_dialog");
    }

    private void navigateToMain() {
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
