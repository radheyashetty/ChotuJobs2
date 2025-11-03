package com.chotujobs;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.chotujobs.models.Job;
import com.chotujobs.services.FirestoreService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateJobActivity extends AppCompatActivity {
    
    private EditText titleEditText;
    private Spinner categorySpinner;
    private EditText startDateEditText;
    private Button pickLocationButton;
    private Button saveButton;
    private ProgressBar progressBar;
    
    private FirestoreService firestoreService;
    private String contractorId;
    private double latitude = 0;
    private double longitude = 0;
    private String selectedImagePath = null;
    private Calendar calendar;
    
    private static final int PICK_LOCATION_REQUEST = 102;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_job);
        
        firestoreService = FirestoreService.getInstance();
        contractorId = getSharedPreferences("chotujobs_prefs", 0).getString("user_id", "");
        calendar = Calendar.getInstance();
        
        initializeViews();
        setupCategorySpinner();
        setupDatePicker();
        setupListeners();
    }
    
    private void initializeViews() {
        titleEditText = findViewById(R.id.titleEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        startDateEditText = findViewById(R.id.startDateEditText);
        pickLocationButton = findViewById(R.id.pickLocationButton);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar == null) {
            // ProgressBar doesn't exist in layout, create it dynamically or skip
        }
    }
    
    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>();
        categories.add("Construction");
        categories.add("Electricity");
        categories.add("Plumbing");
        categories.add("Painting");
        categories.add("Carpentry");
        categories.add("Other");
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }
    
    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener dateListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        };
        
        startDateEditText.setOnClickListener(v -> new DatePickerDialog(
                this, dateListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show());
    }
    
    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        startDateEditText.setText(sdf.format(calendar.getTime()));
    }
    
    private void setupListeners() {
        pickLocationButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivityForResult(intent, PICK_LOCATION_REQUEST);
        });
        
        saveButton.setOnClickListener(v -> saveJob());
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_LOCATION_REQUEST) {
                latitude = data.getDoubleExtra("latitude", 0);
                longitude = data.getDoubleExtra("longitude", 0);
                Toast.makeText(this, "Location selected", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void saveJob() {
        String title = titleEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String startDate = startDateEditText.getText().toString().trim();
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter job title", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startDate.isEmpty()) {
            Toast.makeText(this, "Please select start date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (latitude == 0 || longitude == 0) {
            Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        saveButton.setEnabled(false);
        
        // Create job in Firestore (without Firebase Storage - using local path placeholder)
        createJobInFirestore(title, category, startDate);
    }
    
    private void createJobInFirestore(String title, String category, String startDate) {
        Job job = new Job();
        job.setContractorId(contractorId);
        job.setTitle(title);
        job.setCategory(category);
        job.setStartDate(startDate);
        job.setLatitude(latitude);
        job.setLongitude(longitude);
        job.setImagePath(selectedImagePath != null ? selectedImagePath : ""); // Local path or empty
        job.setStatus("active");
        
        firestoreService.createJob(job, jobId -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            saveButton.setEnabled(true);
            
            if (jobId != null) {
                Toast.makeText(this, "Job created successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Error creating job", Toast.LENGTH_SHORT).show();
            }
        });
    }
}