package com.chotujobs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.chotujobs.databinding.ActivityEditProfileBinding;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirestoreService firestoreService;
    private String userId;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreService = FirestoreService.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = currentUser.getUid();

        loadUserProfile();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        binding.profileImageView.setImageURI(selectedImageUri);
                    }
                });

        binding.btnSave.setOnClickListener(v -> saveUserProfile());

        binding.btnChangeProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
    }

    private void uploadImage(Uri imageUri, OnImageUploadListener listener) {
        if (imageUri == null) {
            listener.onSuccess(null);
            return;
        }
        
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("profile_images/" + userId + ".jpg");
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> listener.onSuccess(uri.toString()))
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                            listener.onSuccess(null);
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    listener.onSuccess(null);
                });
    }

    private void loadUserProfile() {
        firestoreService.getUserProfile(userId, user -> {
            if (user != null) {
                // Load name
                String name = user.getName();
                if (name != null && !name.trim().isEmpty()) {
                    binding.nameEditText.setText(name);
                } else {
                    binding.nameEditText.setText("");
                }
                
                // Load skills
                if (user.getSkills() != null && !user.getSkills().isEmpty()) {
                    binding.skillsEditText.setText(String.join(", ", user.getSkills()));
                } else {
                    binding.skillsEditText.setText("");
                }
                
                // Load address
                String address = user.getAddress();
                if (address != null && !address.trim().isEmpty()) {
                    binding.addressEditText.setText(address);
                } else {
                    binding.addressEditText.setText("");
                }
                
                // Load experience
                binding.experienceEditText.setText(String.valueOf(user.getYearsOfExperience()));

                // Load profile image
                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                    Glide.with(this)
                            .load(user.getProfileImageUrl())
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .circleCrop()
                            .into(binding.profileImageView);
                } else {
                    binding.profileImageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        });
    }

    private void saveUserProfile() {
        String name = binding.nameEditText.getText().toString().trim();
        String skillsStr = binding.skillsEditText.getText().toString().trim();
        String address = binding.addressEditText.getText().toString().trim();
        String experienceStr = binding.experienceEditText.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("skills", Arrays.asList(skillsStr.split("\\s*,\\s*")));
        updates.put("address", address);
        if (!experienceStr.isEmpty()) {
            try {
                updates.put("yearsOfExperience", Integer.parseInt(experienceStr));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number for years of experience", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (selectedImageUri != null) {
            binding.btnSave.setEnabled(false);
            binding.btnSave.setText("Uploading...");
            uploadImage(selectedImageUri, imageUrl -> {
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText("Save");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    updates.put("profileImageUrl", imageUrl);
                }
                updateUser(updates);
            });
        } else {
            updateUser(updates);
        }
    }

    private void updateUser(Map<String, Object> updates) {
        firestoreService.updateUserProfile(userId, updates, success -> {
            if (success) {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    interface OnImageUploadListener {
        void onSuccess(String imageUrl);
    }
}
