package com.chotujobs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.chotujobs.databinding.ActivityEditProfileBinding;
import com.chotujobs.models.User;
import com.chotujobs.services.FirestoreService;
import com.google.firebase.auth.FirebaseAuth;
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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

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
            Log.e("EditProfileActivity", "Image URI is null");
            listener.onSuccess(null);
            return;
        }
        
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("profile_images/" + userId + ".jpg");
        Log.d("EditProfileActivity", "Uploading image to: profile_images/" + userId + ".jpg");
        
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("EditProfileActivity", "Image upload successful, getting download URL");
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                Log.d("EditProfileActivity", "Download URL obtained: " + imageUrl);
                                listener.onSuccess(imageUrl);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("EditProfileActivity", "Failed to get download URL", e);
                                Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                listener.onSuccess(null);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("EditProfileActivity", "Failed to upload image", e);
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    listener.onSuccess(null);
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d("EditProfileActivity", "Upload progress: " + progress + "%");
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
                    Log.d("EditProfileActivity", "Image URL saved: " + imageUrl);
                    updates.put("profileImageUrl", imageUrl);
                    Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Log.w("EditProfileActivity", "Image upload failed, saving profile without image");
                    Toast.makeText(this, "Image upload failed, saving profile without image", Toast.LENGTH_LONG).show();
                }
                updateUser(updates);
            });
        } else {
            updateUser(updates);
        }
    }

    private void updateUser(Map<String, Object> updates) {
        Log.d("EditProfileActivity", "Updating user profile with: " + updates.keySet());
        firestoreService.updateUserProfile(userId, updates, success -> {
            if (success) {
                Log.d("EditProfileActivity", "Profile updated successfully");
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                // Set result to refresh profile fragment
                setResult(RESULT_OK);
                finish();
            } else {
                Log.e("EditProfileActivity", "Failed to update profile");
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    interface OnImageUploadListener {
        void onSuccess(String imageUrl);
    }
}
