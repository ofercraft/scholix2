package com.scholix.app;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.scholix.app.api.PlatformStorage;

public class MainActivity extends AppCompatActivity {

    private LoginManager loginManager = new LoginManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String savedUsername = prefs.getString("username", null);
        String savedPassword = prefs.getString("password", null);

        if (!PlatformStorage.loadPlatforms(this).isEmpty()) {

            if (isTaskRoot()) {
                // ✅ Load and start logo spin animation
                ImageView appLogo = findViewById(R.id.appLogo);
                Animation rotate = AnimationUtils.loadAnimation(this, R.anim.spin);
                appLogo.startAnimation(rotate);

                new Thread(() -> {
                    try {
                        PlatformStorage.refreshCookies(this);

                        runOnUiThread(() -> {
                            appLogo.clearAnimation(); // ✅ stop animation
                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        });

                    } catch (Exception e) {
                        runOnUiThread(appLogo::clearAnimation); // stop if error
                        e.printStackTrace();
                    }
                }).start();
            }

        } else {
            Log.d("MainActivity", "No platforms found, redirecting to Login");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // don't come back to splash screen
        }
    }
}
