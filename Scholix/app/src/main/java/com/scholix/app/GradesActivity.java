package com.scholix.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scholix.app.api.Platform;
import com.scholix.app.api.PlatformStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.IntStream;

public class GradesActivity extends BaseActivity {

    private RecyclerView gradesRecyclerView;
    private GradeAdapter gradeAdapter;
    private ArrayList gradeList;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grades);

        Log.d("NAV_DEBUG", "GradesActivity started");

        gradesRecyclerView = findViewById(R.id.grades_recycler_view);
        gradesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<JSONObject> gradeList = new ArrayList<>();
        gradeAdapter = new GradeAdapter(gradeList);
        gradesRecyclerView.setAdapter(gradeAdapter);
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        gradesRecyclerView.addItemDecoration(divider);

        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        // Load accounts list
        String json = prefs.getString("accounts_list", null);
        List<Platform> savedAccounts = PlatformStorage.loadPlatforms(this);

        // Read global cookies and studentId for now (for Webtop accounts, until individual cookies saved)
        String savedCookies = prefs.getString("cookies", "");
        String studentId = prefs.getString("student_id", "");

        // Loop through all accounts
        for (Platform platform : savedAccounts) {


            new Thread(() -> {
                try {
                    JSONArray grades = platform.getGrades();

                    runOnUiThread(() -> {
                        TextView averageGrade = findViewById(R.id.average_grade);
                        int sum = 0;
                        int count = 0;
                        for (int i = 0; i < grades.length(); i++) {
                            JSONObject grade = grades.optJSONObject(i);
                            try {
                                if (!grade.getString("grade").equals("null")) {
                                    try {
                                        sum += grade.getInt("grade");
                                        count++;
                                    } catch (Exception e){}
                                    gradeList.add(grade);  // now this is a checked call
                                }
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        averageGrade.setText(String.valueOf(sum/count));

                        gradeAdapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }).start();

        }

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNavigationView);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_grades;
    }

    // Shows the account popup menu
    private void showAccountPopup(View anchor) {

        PopupMenu popupMenu = new PopupMenu(GradesActivity.this, anchor, 0, 0, R.style.CustomPopupMenu);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
        popupMenu.show();

        // Force show icons
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Menu actions
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_username:
                    Toast.makeText(GradesActivity.this, "Username clicked", Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.menu_settings:
                    startActivity(new Intent(GradesActivity.this, PlatformsActivity.class));
                    Toast.makeText(GradesActivity.this, "Settings clicked", Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.menu_logout:
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.apply();
                    Toast.makeText(GradesActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(GradesActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
    }
}
