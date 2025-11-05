package com.chotujobs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.chotujobs.databinding.ActivityLoginBinding;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    private enum UiState {
        INITIAL,
        EMAIL_LOGIN,
        PHONE_LOGIN,
        OTP_VERIFICATION,
        ROLE_SELECTION
    }

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
        updateUi(UiState.INITIAL);
    }

    private void initializeViews() {
        binding.btnEmailLogin.setOnClickListener(v -> updateUi(UiState.EMAIL_LOGIN));
        binding.btnPhoneLogin.setOnClickListener(v -> updateUi(UiState.PHONE_LOGIN));
        binding.btnAction.setOnClickListener(v -> handleAction());
        binding.btnCancel.setOnClickListener(v -> updateUi(UiState.INITIAL));
    }

    private void updateUi(UiState state) {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnAction.setEnabled(true);
        switch (state) {
            case INITIAL:
                binding.viewFlipper.setDisplayedChild(0);
                binding.btnAction.setVisibility(View.GONE);
                binding.btnCancel.setVisibility(View.GONE);
                break;
            case EMAIL_LOGIN:
                binding.viewFlipper.setDisplayedChild(1);
                binding.btnAction.setText("Login / Sign Up");
                binding.btnAction.setVisibility(View.VISIBLE);
                binding.btnCancel.setVisibility(View.VISIBLE);
                break;
            case PHONE_LOGIN:
                binding.viewFlipper.setDisplayedChild(2);
                binding.otpInputLayout.setVisibility(View.GONE);
                binding.phoneNumberInputLayout.setVisibility(View.VISIBLE);
                binding.btnAction.setText("Send OTP");
                binding.btnAction.setVisibility(View.VISIBLE);
                binding.btnCancel.setVisibility(View.VISIBLE);
                break;
            case OTP_VERIFICATION:
                binding.phoneNumberInputLayout.setVisibility(View.GONE);
                binding.otpInputLayout.setVisibility(View.VISIBLE);
                binding.btnAction.setText("Verify OTP");
                break;
            case ROLE_SELECTION:
                binding.viewFlipper.setVisibility(View.GONE);
                binding.roleSpinner.setVisibility(View.VISIBLE);
                binding.btnAction.setText("Create Account");
                break;
        }
    }

    private void handleAction() {
        UiState currentState = getCurrentUiState();
        switch (currentState) {
            case EMAIL_LOGIN:
                handleEmailLogin();
                break;
            case PHONE_LOGIN:
                sendOtp();
                break;
            case OTP_VERIFICATION:
                verifyOtp();
                break;
        }
    }

    private UiState getCurrentUiState() {
        switch (binding.viewFlipper.getDisplayedChild()) {
            case 1:
                return UiState.EMAIL_LOGIN;
            case 2:
                if (binding.otpInputLayout.getVisibility() == View.VISIBLE) {
                    return UiState.OTP_VERIFICATION;
                } else {
                    return UiState.PHONE_LOGIN;
                }
            default:
                return UiState.INITIAL;
        }
    }

    private void handleEmailLogin() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            fetchUserProfileAndNavigate(user.getUid());
                        } else {
                            hideProgress();
                            Toast.makeText(this, "Failed to get user information", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        hideProgress();
                        promptForRole(email, password);
                    }
                });
    }

    private void sendOtp() {
        String phoneNumber = binding.phoneNumberEditText.getText().toString().trim();
        if (phoneNumber.isEmpty() || phoneNumber.length() < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress();

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
                        Toast.makeText(LoginActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        updateUi(UiState.PHONE_LOGIN);
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        LoginActivity.this.verificationId = verificationId;
                        updateUi(UiState.OTP_VERIFICATION);
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
        showProgress();
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            fetchUserProfileAndNavigate(user.getUid());
                        } else {
                            hideProgress();
                            Toast.makeText(this, "Failed to get user information", Toast.LENGTH_SHORT).show();
                            updateUi(UiState.PHONE_LOGIN);
                        }
                    } else {
                        hideProgress();
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        updateUi(UiState.PHONE_LOGIN);
                    }
                });
    }

    private void fetchUserProfileAndNavigate(String uid) {
        firestoreService.getUserProfile(uid, user -> {
            if (user != null && user.getRole() != null && !user.getRole().isEmpty()) {
                prefs.edit()
                        .putString("user_role", user.getRole())
                        .putString("user_id", uid)
                        .apply();
                navigateToMain();
            } else {
                promptForRole(null, null);
            }
        });
    }

    private void promptForRole(String email, String password) {
        updateUi(UiState.ROLE_SELECTION);
        binding.btnAction.setOnClickListener(v -> {
            if (binding.roleSpinner.getSelectedItem() == null) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }
            String role = binding.roleSpinner.getSelectedItem().toString().toLowerCase();
            showProgress();
            
            if (email != null && password != null && !email.isEmpty() && !password.isEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {
                                    String name = email.contains("@") ? email.split("@")[0] : email;
                                    User userObj = new User(name, email, role);
                                    createUserProfile(userObj, user.getUid(), role);
                                } else {
                                    showError("Failed to get user ID");
                                    updateUi(UiState.EMAIL_LOGIN);
                                }
                            } else {
                                showError("Authentication failed");
                                updateUi(UiState.EMAIL_LOGIN);
                            }
                        });
            } else {
                FirebaseUser currentUser = auth.getCurrentUser();
                if (currentUser != null) {
                    String phoneNumber = currentUser.getPhoneNumber();
                    if (phoneNumber != null) {
                        User user = new User(phoneNumber, phoneNumber, role, true);
                        createUserProfile(user, currentUser.getUid(), role);
                    } else {
                        showError("Phone number not available");
                    }
                } else {
                    showError("User not authenticated");
                }
            }
        });
    }
    
    private void createUserProfile(User user, String uid, String role) {
        firestoreService.createUserProfile(user, uid, success -> {
            hideProgress();
            if (success) {
                prefs.edit()
                        .putString("user_role", role)
                        .putString("user_id", uid)
                        .apply();
                navigateToMain();
            } else {
                showError("Failed to create profile");
            }
        });
    }

    private void showError(String message) {
        hideProgress();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void hideProgress() {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnAction.setEnabled(true);
    }
    
    private void showProgress() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnAction.setEnabled(false);
    }

    private void navigateToMain() {
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
