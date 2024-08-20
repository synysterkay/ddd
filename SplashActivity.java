package com.raymond.redditdownloader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FIRST_RUN_KEY = "isFirstRun";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFirstRun()) {
                    // Start IntroActivity for first-time users
                    Intent introIntent = new Intent(SplashActivity.this, IntroActivity.class);
                    startActivity(introIntent);
                } else {
                    // Start MainActivity for returning users
                    Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                }
                finish();
            }
        }, SPLASH_DURATION);
    }

    private boolean isFirstRun() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean(FIRST_RUN_KEY, true);
        if (isFirstRun) {
            // Set the flag to false so that it's not the first run next time
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(FIRST_RUN_KEY, false);
            editor.apply();
        }
        return isFirstRun;
    }
}
