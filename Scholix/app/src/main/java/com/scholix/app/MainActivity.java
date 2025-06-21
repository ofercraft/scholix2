package com.scholix.app;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.JsonObject;
import com.scholix.app.api.BarIlanPlatform;
import com.scholix.app.api.PlatformStorage;
import com.scholix.app.api.WebtopPlatform;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scholix.app.api.Webtop;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private LoginManager loginManager = new LoginManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_main);
//        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//        this.startActivity(intent);








//
//        // Schedule grade fetch every 15 min
//        PeriodicWorkRequest request = new PeriodicWorkRequest
//                .Builder(GradeSyncWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
//                .build();
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//                "grade_sync", ExistingPeriodicWorkPolicy.REPLACE, request);









        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String savedUsername = prefs.getString("username", null);
        String savedPassword = prefs.getString("password", null);
        if (!PlatformStorage.loadPlatforms(this).isEmpty()) {

            // Clear cookies ONLY when first launching the app (not when returning to MainActivity)
            if (isTaskRoot()) {
                new Thread(() -> {
                    // Perform network call here safely
                    try {
                        PlatformStorage.refreshCookies(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        }
        else {
            System.out.println(PlatformStorage.loadPlatforms(this));
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }
    }

}