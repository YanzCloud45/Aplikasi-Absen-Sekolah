package com.yanzhost.absensekolah;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Splash Screen Activity
 * Menampilkan logo dan nama aplikasi selama 2.5 detik
 * sebelum berpindah ke MainActivity (WebView)
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500; // 2.5 detik

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Sembunyikan action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Animasi fade-in untuk logo
        ImageView logo = findViewById(R.id.splash_logo);
        TextView appName = findViewById(R.id.splash_app_name);
        TextView subtitle = findViewById(R.id.splash_subtitle);
        ProgressBar progressBar = findViewById(R.id.splash_progress);

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000);
        fadeIn.setFillAfter(true);

        AlphaAnimation fadeInDelay = new AlphaAnimation(0.0f, 1.0f);
        fadeInDelay.setDuration(800);
        fadeInDelay.setStartOffset(500);
        fadeInDelay.setFillAfter(true);

        AlphaAnimation fadeInProgress = new AlphaAnimation(0.0f, 1.0f);
        fadeInProgress.setDuration(600);
        fadeInProgress.setStartOffset(800);
        fadeInProgress.setFillAfter(true);

        logo.startAnimation(fadeIn);
        appName.startAnimation(fadeInDelay);
        subtitle.startAnimation(fadeInDelay);
        progressBar.startAnimation(fadeInProgress);

        // Pindah ke MainActivity setelah delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
}
