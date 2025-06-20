package com.scholix.app;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import com.scholix.app.api.Platform;
import com.scholix.app.api.PlatformStorage;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Context context = this;

        getWindow().setSoftInputMode(0x10 | 2);
        ConstraintLayout rootLayout   = findViewById(R.id.rootLayout);
        View           inputContainer = findViewById(R.id.inputContainer);
        View           header         = findViewById(R.id.headerLogoContainer);
        rootLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    boolean keyboardVisible = false;
                    @Override public void onGlobalLayout() {
                        Rect r = new Rect();
                        rootLayout.getWindowVisibleDisplayFrame(r);
                        int screenH = rootLayout.getRootView().getHeight();
                        int keypadH = screenH - r.bottom;
                        boolean isNowVisible = keypadH > screenH * 0.15;

                        if (isNowVisible != keyboardVisible) {
                            // when keyboard shows: move inputContainer up under the header
                            int targetY = isNowVisible
                                    ? header.getBottom() - inputContainer.getTop()
                                    : 0;
                            inputContainer.animate()
                                    .translationY(targetY)
                                    .setDuration(300)
                                    .start();
                            keyboardVisible = isNowVisible;
                        }
                    }
                });

        // Schedule grade fetch every 15 min
        PeriodicWorkRequest request = new PeriodicWorkRequest
                .Builder(GradeSyncWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "grade_sync", ExistingPeriodicWorkPolicy.REPLACE, request);

        // Bind UI elements
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.login_button);

        // Set login button listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String username = usernameEditText.getText().toString().trim();
                final String password = passwordEditText.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Enter both username and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                // Perform network login on a background thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            List<Platform> result = PlatformStorage.addPlatform(context,username, password);

                            if (!result.isEmpty()){
                                System.out.println(PlatformStorage.loadPlatforms(context));
                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else{
                                runOnUiThread(() ->
                                        Toast.makeText(LoginActivity.this, "Login failed: please check your username and password", Toast.LENGTH_SHORT).show()
                                );

                            }

                        } catch (Exception ex) {
                            ex.printStackTrace();

                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "Login error: " + ex.getMessage(), Toast.LENGTH_SHORT).show()
                            );

                        }
                    }
                }).start();
            }
        });
    }

}