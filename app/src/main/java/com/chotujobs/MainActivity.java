package com.chotujobs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.chotujobs.fragments.AgentFragment;
import com.chotujobs.fragments.ContractorFragment;
import com.chotujobs.fragments.LabourFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private String userRole;
    private SharedPreferences prefs;
    private FirebaseAuth auth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        auth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("chotujobs_prefs", MODE_PRIVATE);
        userRole = prefs.getString("user_role", "");
        
        if (userRole.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        bottomNav = findViewById(R.id.bottomNav);
        
        // Set up bottom navigation based on role
        if (userRole.equals("labour")) {
            setupLabourBottomNav();
        } else if (userRole.equals("agent")) {
            setupAgentBottomNav();
        } else if (userRole.equals("contractor")) {
            setupContractorBottomNav();
        }
        
        // Load default fragment
        loadDefaultFragment();
    }
    
    private void setupLabourBottomNav() {
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.bottom_nav_labour);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_jobs) {
                loadFragment(new LabourFragment());
                return true;
            }
            return false;
        });
    }
    
    private void setupAgentBottomNav() {
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.bottom_nav_agent);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_jobs) {
                loadFragment(new AgentFragment());
                return true;
            }
            return false;
        });
    }
    
    private void setupContractorBottomNav() {
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.bottom_nav_contractor);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_my_jobs) {
                loadFragment(new ContractorFragment());
                return true;
            }
            return false;
        });
    }
    
    private void loadDefaultFragment() {
        if (userRole.equals("labour")) {
            loadFragment(new LabourFragment());
        } else if (userRole.equals("agent")) {
            loadFragment(new AgentFragment());
        } else if (userRole.equals("contractor")) {
            loadFragment(new ContractorFragment());
        }
    }
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            handleLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void handleLogout() {
        auth.signOut();
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

