package com.scholix.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.scholix.app.api.PlatformStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AccountActivity extends BaseActivity {
    private LinearLayout itemContainer;
    private TextView label, value, todayTitle;
    private OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();

    private MaterialButton btnLogout;
    private ImageView platformArrow;
    private SharedPreferences prefs;

    private BottomNavigationView bottomNavigation;
    protected int getLayoutResourceId() { return R.layout.activity_account; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId()); // Make sure this matches your XML file name



        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNav);


        findViewById(R.id.platforms_container).setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, PlatformsActivity.class);
            startActivity(intent);
        });

        MaterialButton logoutButton = findViewById(R.id.btn_logout);
        logoutButton.setOnClickListener(v -> {
            CookieManager cookieManager = CookieManager.getInstance();
            PlatformStorage.clearPlatforms(this);
            cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    cookieManager.flush(); // Ensure it's saved

                    getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply();
                    Intent intent = new Intent(AccountActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
                    startActivity(intent);
                }
            });
        });


    }



}