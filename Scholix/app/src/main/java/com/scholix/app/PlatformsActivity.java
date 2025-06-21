package com.scholix.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scholix.app.api.Platform;
import com.scholix.app.api.PlatformStorage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class PlatformsActivity extends BaseActivity {

    private RecyclerView accountsRecyclerView;
    private FloatingActionButton addAccountFab;
    private AccountAdapter accountAdapter;
    private SharedPreferences prefs;
    private List<Platform> accountList;
    private Gson gson = new Gson();
    private ImageView backArrow;

    private static final String ACCOUNTS_KEY = "accounts_list";
    private static final String[] sources = {"Classroom", "Bar Ilan", "Webtop"};
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_platforms);
        context=this;
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        accountsRecyclerView = findViewById(R.id.accounts_recycler_view);

        TopSnappingLinearLayoutManager layoutManager = new TopSnappingLinearLayoutManager(this);
        accountsRecyclerView.setLayoutManager(layoutManager);
        accountsRecyclerView.setClipToPadding(false);
        accountsRecyclerView.setPadding(0, 0, 0, 500);
        accountList= PlatformStorage.loadPlatforms(context);
        accountAdapter = new AccountAdapter(context, accountList);
        accountsRecyclerView.setAdapter(accountAdapter);

        addAccountFab = findViewById(R.id.add_account_fab);
        addAccountFab.setOnClickListener(v -> showAddAccountDialog());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNavigationView);

        accountList=PlatformStorage.loadPlatforms(context);


        findViewById(R.id.back_arrow_container).setOnClickListener(v -> {
            finish(); // or go back to AccountActivity explicitly if needed
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = vh.getAdapterPosition();
                int to   = target.getAdapterPosition();

                // 1) swap inside the adapter
                accountAdapter.swapItems(from, to);

                // 2) persist the new order from the adapter’s list
                PlatformStorage.savePlatforms(context, accountAdapter.getPlatforms());

                return true;
            }


            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // no swipe
            }
        });
        itemTouchHelper.attachToRecyclerView(accountsRecyclerView);
        new Thread(() -> {
            // your background code here
            try {
                System.out.println(PlatformStorage.getCourses(this));
                System.out.println(PlatformStorage.loadPlatforms(this));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }

    private Platform getMain() {
        return accountList.get(0);
    }


    private void showAddAccountDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_account, null);
        EditText nameInput = dialogView.findViewById(R.id.dialog_name);
        EditText usernameInput = dialogView.findViewById(R.id.dialog_username);
        EditText passwordInput = dialogView.findViewById(R.id.dialog_password);


        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Account")
                .setView(dialogView)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.rounded_menu));

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                String username = usernameInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        List<Platform> newPlatforms = PlatformStorage.addPlatform(this, username, password);

                        runOnUiThread(() -> {
                            accountList=PlatformStorage.loadPlatforms(context);
                            accountAdapter = new AccountAdapter(context, accountList);
                            accountsRecyclerView.setAdapter(accountAdapter);
                            dialog.dismiss();
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() ->
                                Toast.makeText(context, "Failed to add account", Toast.LENGTH_SHORT).show()
                        );
                    }
                });

            });
        });

        dialog.show();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_platforms;
    }
}
