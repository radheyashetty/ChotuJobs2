package com.chotujobs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chotujobs.databinding.ActivityPhoneAuthBinding;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneAuthActivity extends AppCompatActivity {

    private static final String TAG = "PhoneAuthActivity";
    private FirebaseAuth auth;
    private FirestoreService firestoreService;
    private SharedPreferences prefs;
    private ActivityPhoneAuthBinding binding;
    private String verificationId;
    private boolean isSignUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firestoreService = FirestoreService.getInstance();
        prefs = getSharedPreferences("chotujobs_prefs", MODE_PRIVATE);

        initializeViews();
    }

    private void initializeViews() {
        binding.sendOtpButton.setOnClickListener(v -> sendOtp());
        binding.verifyOtpButton.setOnClickListener(v -> verifyOtp());
    }

    private void sendOtp() {
        String phoneNumber = binding.phoneNumberEditText.getText().toString().trim();
        if (phoneNumber.isEmpty() || phoneNumber.length() < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.sendOtpButton.setEnabled(false);

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
                        Toast.makeText(PhoneAuthActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        binding.sendOtpButton.setEnabled(true);
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        PhoneAuthActivity.this.verificationId = verificationId;
                        binding.otpEditText.setVisibility(View.VISIBLE);
                        binding.verifyOtpButton.setVisibility(View.VISIBLE);
                        binding.sendOtpButton.setVisibility(View.GONE);
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
            User user = new User(auth.getCurrentUser().getPhoneNumber(), auth.getCurrentUser().getPhoneNumber(), role, true);
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
        });
        dialog.show(getSupportFragmentManager(), "role_selection_dialog");
    }


    private void navigateToMain() {
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
