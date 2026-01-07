package com.example.dudi_project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
    private int currentNavId = -1;

    private static final String PREFS_NAME = "CoachPrefs";
    private static final String KEY_ONBOARDED = "isOnboarded";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // ניהול כפתור החזור
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                
                // אם אנחנו בניתוח ריצה, היסטוריה או טרנדים - חזור לדשבורד
                if (currentFragment instanceof RunAnalysisFragment || 
                    currentFragment instanceof RunHistoryFragment || 
                    currentFragment instanceof PerformanceTrendsFragment) {
                    navigateToDashboard();
                } else if (currentFragment instanceof LiveRunFragment) {
                    // בריצה חיה אולי עדיף לא לחזור בטעות כדי לא להרוס את האימון, או להציג דיאלוג אישור
                    navigateToDashboard();
                } else {
                    // אם אנחנו כבר בדשבורד, סגור את האפליקציה
                    finish();
                }
            }
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentNavId) return true;

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
            navigateToDashboard();
        }
    }

    private void navigateToDashboard() {
        currentNavId = R.id.nav_dashboard;
        loadFragment(new DashboardFragment(), false);
        bottomNav.setVisibility(View.VISIBLE);
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
    }

    private int getNavPosition(int id) {
        if (id == R.id.nav_dashboard) return 0;
        if (id == R.id.nav_history) return 1;
        if (id == R.id.nav_trends) return 2;
        return 0;
    }

    private void loadFragment(Fragment fragment, boolean slideRight) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        if (slideRight) {
            transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            );
        } else {
            transaction.setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
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
        navigateToDashboard();
    }

    @Override
    public void onStartRun() {
        loadFragment(new LiveRunFragment(), true);
    }

    @Override
    public void onRunFinished() {
        navigateToDashboard();
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