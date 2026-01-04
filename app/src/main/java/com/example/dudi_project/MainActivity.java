package com.example.dudi_project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.dudi_project.fragments.*;

public class MainActivity extends AppCompatActivity implements
        OnboardingFragment.OnboardingListener,
        DashboardFragment.DashboardListener,
        LiveRunFragment.LiveRunListener,
        RunHistoryFragment.HistoryListener {

    private BottomNavigationView bottomNav;
    private SharedPreferences prefs;
    private int currentNavId = -1; // שמירת הטאב הנוכחי לצורך זיהוי כיוון הגלילה

    private static final String PREFS_NAME = "CoachPrefs";
    private static final String KEY_ONBOARDED = "isOnboarded";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentNavId) return true;

            // קביעת כיוון האנימציה לפי מיקום הטאב
            boolean slideRight = getNavPosition(id) > getNavPosition(currentNavId);
            
            if (id == R.id.nav_dashboard) {
                loadFragment(new DashboardFragment(), slideRight);
            } else if (id == R.id.nav_history) {
                loadFragment(new RunHistoryFragment(), slideRight);
            } else if (id == R.id.nav_trends) {
                loadFragment(new PerformanceTrendsFragment(), slideRight);
            }
            
            currentNavId = id;
            return true;
        });

        boolean isOnboarded = prefs.getBoolean(KEY_ONBOARDED, false);

        if (!isOnboarded) {
            loadFragment(new OnboardingFragment(), true);
            bottomNav.setVisibility(View.GONE);
        } else {
            currentNavId = R.id.nav_dashboard;
            loadFragment(new DashboardFragment(), true);
            bottomNav.setVisibility(View.VISIBLE);
        }
    }

    // פונקציה לקביעת סדר הטאבים (0 עד 2)
    private int getNavPosition(int id) {
        if (id == R.id.nav_dashboard) return 0;
        if (id == R.id.nav_history) return 1;
        if (id == R.id.nav_trends) return 2;
        return 0;
    }

    private void loadFragment(Fragment fragment, boolean slideRight) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        // הגדרת האנימציות לפני ה-replace
        if (slideRight) {
            transaction.setCustomAnimations(
                R.anim.slide_in_right,  // כניסה של החדש מימין
                R.anim.slide_out_left,  // יציאה של הישן לשמאל
                R.anim.slide_in_left,   // (עבור כפתור חזור)
                R.anim.slide_out_right  // (עבור כפתור חזור)
            );
        } else {
            transaction.setCustomAnimations(
                R.anim.slide_in_left,   // כניסה משמאל
                R.anim.slide_out_right,  // יציאה לימין
                R.anim.slide_in_right,
                R.anim.slide_out_left
            );
        }

        transaction.replace(R.id.fragment_container, fragment)
                .commit();

        updateNavigationVisibility(fragment);
    }

    private void updateNavigationVisibility(Fragment fragment) {
        if (fragment instanceof OnboardingFragment || fragment instanceof LiveRunFragment) {
            bottomNav.setVisibility(View.GONE);
        } else {
            bottomNav.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onOnboardingComplete() {
        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply();
        currentNavId = R.id.nav_dashboard;
        loadFragment(new DashboardFragment(), true);
        bottomNav.setVisibility(View.VISIBLE);
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
    }

    @Override
    public void onStartRun() {
        loadFragment(new LiveRunFragment(), true);
    }

    @Override
    public void onRunFinished() {
        currentNavId = R.id.nav_dashboard;
        loadFragment(new DashboardFragment(), false); // גלילה חזרה שמאלה
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
    }

    @Override
    public void onViewRunAnalysis(int runId) {
        RunAnalysisFragment fragment = new RunAnalysisFragment();
        Bundle args = new Bundle();
        args.putInt("RUN_ID", runId);
        fragment.setArguments(args);
        loadFragment(fragment, true);
    }
}