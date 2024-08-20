package com.raymond.redditdownloader;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class IntroActivity extends AppCompatActivity {

    private ImageView introImage;
    private TextView featureTitle;
    private TextView featureDescription;
    private Button btnNext;
    private LinearProgressIndicator progressIndicator;
    private int currentIndex = 0;
    private final int[] images = {
            R.drawable.intro_image1,
            R.drawable.intro_image2,
            R.drawable.intro_image3
    };
    private final String[] titles = {
            "Welcome to Reddit Downloader",
            "Easy Download",
            "Organize Your Content"
    };
    private final String[] descriptions = {
            "Discover a new way to save and enjoy your favorite Reddit content offline.",
            "Simply paste a Reddit URL and download images, videos, and GIFs with a single tap.",
            "Keep your downloaded content organized and easily accessible within the app."
    };
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FIRST_RUN_KEY = "isFirstRun";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        introImage = findViewById(R.id.intro_image);
        featureTitle = findViewById(R.id.featureTitle);
        featureDescription = findViewById(R.id.featureDescription);
        btnNext = findViewById(R.id.btnNext);
        progressIndicator = findViewById(R.id.progressIndicator);
        updateContent();
        btnNext.setOnClickListener(v -> {
            currentIndex++;
            if (currentIndex < images.length) {
                updateContent();
            } else {
                setFirstRunComplete();
                startActivity(new Intent(IntroActivity.this, MainActivity.class));
                finish();
            }
        });
        startButtonAnimation();
    }

    private void updateContent() {
        fadeOutViews();
        new Handler().postDelayed(() -> {
            introImage.setImageResource(images[currentIndex]);
            featureTitle.setText(titles[currentIndex]);
            featureDescription.setText(descriptions[currentIndex]);
            progressIndicator.setProgress((currentIndex + 1) * 100 / images.length);
            if (currentIndex == images.length - 1) {
                btnNext.setText("Get Started");
            } else {
                btnNext.setText("Next");
            }
            fadeInViews();
        }, 300);
    }

    private void fadeOutViews() {
        introImage.animate().alpha(0f).setDuration(300).start();
        featureTitle.animate().alpha(0f).setDuration(300).start();
        featureDescription.animate().alpha(0f).setDuration(300).start();
    }

    private void fadeInViews() {
        introImage.animate().alpha(1f).setDuration(300).start();
        featureTitle.animate().alpha(1f).setDuration(300).start();
        featureDescription.animate().alpha(1f).setDuration(300).start();
    }

    private void startButtonAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(btnNext, View.SCALE_X, 0.9f, 1.1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(btnNext, View.SCALE_Y, 0.9f, 1.1f);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.REVERSE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatMode(ObjectAnimator.REVERSE);

        scaleX.setDuration(1000);
        scaleY.setDuration(1000);

        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleX.start();
        scaleY.start();
    }

    private void setFirstRunComplete() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(FIRST_RUN_KEY, false);
        editor.apply();
    }
}
