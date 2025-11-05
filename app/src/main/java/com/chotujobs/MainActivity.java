package com.chotujobs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.chotujobs.databinding.ActivityMainBinding;
import com.chotujobs.fragments.ChatsFragment;
import com.chotujobs.fragments.ContractorFragment;
import com.chotujobs.fragments.JobsListFragment;
import com.chotujobs.fragments.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String userRole;
    private SharedPreferences prefs;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("chotujobs_prefs", MODE_PRIVATE);
        userRole = prefs.getString("user_role", "");

        if (auth.getCurrentUser() == null || userRole.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupBottomNav();
        if (savedInstanceState == null) {
            loadDefaultFragment();
        }
    }

    private void setupBottomNav() {
        binding.bottomNav.getMenu().clear();
        int menuResId = R.menu.bottom_nav_labour;
        if ("agent".equals(userRole)) {
            menuResId = R.menu.bottom_nav_agent;
        } else if ("contractor".equals(userRole)) {
            menuResId = R.menu.bottom_nav_contractor;
        }
        binding.bottomNav.inflateMenu(menuResId);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_jobs) {
                selectedFragment = JobsListFragment.newInstance(userRole);
            } else if (itemId == R.id.nav_my_jobs) {
                selectedFragment = new ContractorFragment();
            } else if (itemId == R.id.nav_messages) {
                selectedFragment = new ChatsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadDefaultFragment() {
        Fragment defaultFragment;
        if ("contractor".equals(userRole)) {
            defaultFragment = new ContractorFragment();
        } else {
            defaultFragment = JobsListFragment.newInstance(userRole);
        }
        loadFragment(defaultFragment);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
