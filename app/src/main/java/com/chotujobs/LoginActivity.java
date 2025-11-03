package com.chotujobs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chotujobs.databinding.ActivityLoginBinding;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth auth;
    private FirestoreService firestoreService;
    private SharedPreferences prefs;
    private ActivityLoginBinding binding;
    private String verificationId;
    private boolean isEmailLogin = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firestoreService = FirestoreService.getInstance();
        prefs = getSharedPreferences("chotujobs_prefs", MODE_PRIVATE);

        if (auth.getCurrentUser() != null && prefs.getString("user_role", null) != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initializeViews();
    }

    private void initializeViews() {
        binding.btnEmailLogin.setOnClickListener(v -> showEmailFields());
        binding.btnPhoneLogin.setOnClickListener(v -> showPhoneFields());
        binding.btnAction.setOnClickListener(v -> handleAction());
        binding.btnCancel.setOnClickListener(v -> resetToInitialState());
    }

    private void resetToInitialState(){
        isEmailLogin = true;
        binding.emailFields.setVisibility(View.GONE);
        binding.phoneFields.setVisibility(View.GONE);
        binding.otpInputLayout.setVisibility(View.GONE);
        binding.phoneNumberInputLayout.setVisibility(View.VISIBLE);
        binding.btnAction.setVisibility(View.GONE);
        binding.btnCancel.setVisibility(View.GONE);
        binding.btnEmailLogin.setVisibility(View.VISIBLE);
        binding.btnPhoneLogin.setVisibility(View.VISIBLE);
    }

    private void showEmailFields() {
        isEmailLogin = true;
        binding.emailFields.setVisibility(View.VISIBLE);
        binding.phoneFields.setVisibility(View.GONE);
        binding.btnAction.setVisibility(View.VISIBLE);
        binding.btnCancel.setVisibility(View.VISIBLE);
        binding.btnAction.setText("Login / Sign Up");
        binding.btnEmailLogin.setVisibility(View.GONE);
        binding.btnPhoneLogin.setVisibility(View.GONE);
    }

    private void showPhoneFields() {
        isEmailLogin = false;
        binding.emailFields.setVisibility(View.GONE);
        binding.phoneFields.setVisibility(View.VISIBLE);
        binding.btnAction.setVisibility(View.VISIBLE);
        binding.btnCancel.setVisibility(View.VISIBLE);
        binding.btnAction.setText("Send OTP");
        binding.btnEmailLogin.setVisibility(View.GONE);
        binding.btnPhoneLogin.setVisibility(View.GONE);
    }

    private void handleAction() {
        if (isEmailLogin) {
            handleEmailLogin();
        } else {
            if (binding.btnAction.getText().equals("Send OTP")) {
                sendOtp();
            } else {
                verifyOtp();
            }
        }
    }

    private void handleEmailLogin() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnAction.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Login successful");
                        String uid = auth.getCurrentUser().getUid();
                        fetchUserProfileAndNavigate(uid);
                    } else {
                        Log.w(TAG, "Login failed, attempting to create user", task.getException());
                        promptForRole(null);
                    }
                });
    }

    private void sendOtp() {
        String phoneNumber = binding.phoneNumberEditText.getText().toString().trim();
        if (phoneNumber.isEmpty() || phoneNumber.length() < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnAction.setEnabled(false);

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+91" + phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(com.google.firebase.FirebaseException e) {
                        Log.w(TAG, "onVerificationFailed", e);
                        Toast.makeText(LoginActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        binding.btnAction.setEnabled(true);
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        LoginActivity.this.verificationId = verificationId;
                        binding.phoneNumberInputLayout.setVisibility(View.GONE);
                        binding.otpInputLayout.setVisibility(View.VISIBLE);
                        binding.btnAction.setText("Verify OTP");
                        binding.btnAction.setEnabled(true);
                    }
                });
    }

    private void verifyOtp() {
        String otp = binding.otpEditText.getText().toString().trim();
        if (otp.isEmpty() || otp.length() < 6) {
            Toast.makeText(this, "Please enter a valid OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Login successful");
                        String uid = auth.getCurrentUser().getUid();
                        fetchUserProfileAndNavigate(uid);
                    } else {
                        Log.w(TAG, "Login failed", task.getException());
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserProfileAndNavigate(String uid) {
        firestoreService.getUserProfile(uid, user -> {
            if (user != null && user.getRole() != null && !user.getRole().isEmpty()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user_role", user.getRole());
                editor.putString("user_id", uid);
                editor.apply();
                navigateToMain();
            } else {
                promptForRole(uid);
            }
        });
    }

    private void promptForRole(String uid) {
        binding.roleSpinner.setVisibility(View.VISIBLE);
        binding.btnAction.setText("Create Account");
        binding.btnAction.setEnabled(true);
        binding.btnAction.setOnClickListener(v -> {
            String role = binding.roleSpinner.getSelectedItem().toString().toLowerCase();
            if (isEmailLogin) {
                String email = binding.emailEditText.getText().toString().trim();
                String password = binding.passwordEditText.getText().toString().trim();
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                String newUid = auth.getCurrentUser().getUid();
                                User user = new User(email.split("@")[0], email, role);
                                createUserProfile(user, newUid, role);
                            } else {
                                Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                User user = new User(auth.getCurrentUser().getPhoneNumber(), auth.getCurrentUser().getPhoneNumber(), role, true);
                createUserProfile(user, uid, role);
            }
        });
    }
    
    private void createUserProfile(User user, String uid, String role){
        firestoreService.createUserProfile(user, uid, success -> {
            if (success) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user_role", role);
                editor.putString("user_id", uid);
                editor.apply();
                navigateToMain();
            } else {
                Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMain() {
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
