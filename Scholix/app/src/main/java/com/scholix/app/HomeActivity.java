package com.scholix.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.TextView;
import android.view.View;
import android.widget.Button;
import androidx.core.view.GravityCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.ImageButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scholix.app.api.Platform;
import com.scholix.app.api.PlatformStorage;


public class HomeActivity extends BaseActivity {

    private static final String TAG = "HomeActivity";
    private RecyclerView scheduleRecyclerView;
    private ScheduleAdapter adapter;
    private ArrayList<JSONObject> scheduleItems = new ArrayList<>();
    private OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();

    // Mapping for subject -> colorClass (using cleaned keys)
    private static final Map<String,String> SUBJECT_COLORS = new HashMap<>();
    static {
        SUBJECT_COLORS.put("מתמטיקה האצה", "lightgreen-cell");
        SUBJECT_COLORS.put("מדעים",       "lightyellow-cell");
        SUBJECT_COLORS.put("של`ח",       "lightgreen-cell");
        SUBJECT_COLORS.put("חינוך",       "pink-cell");
        SUBJECT_COLORS.put("ערבית",       "lightblue-cell");
        SUBJECT_COLORS.put("היסטוריה",    "lightred-cell");
        SUBJECT_COLORS.put("עברית",       "lightpurple-cell");
        SUBJECT_COLORS.put("חינוך גופני", "lightorange-cell");
        SUBJECT_COLORS.put("נחשון",       "lightyellow-cell");
        SUBJECT_COLORS.put("אנגלית",      "lime-cell");
        SUBJECT_COLORS.put("ספרות",       "blue-cell");
        SUBJECT_COLORS.put("תנך",         "lightgrey-cell");
        SUBJECT_COLORS.put("תנ`ך",       "lightgrey-cell");
        SUBJECT_COLORS.put("cancel",      "cancel-cell");
    }

    // currentDay: 0 = Sunday, 6 = Saturday; -1 means "today"
    // currentDay: 0 = Sunday, 6 = Saturday; -1 means "today"
    private int currentDay = -1;
    private TextView dayLabel;


    private RecyclerView gradesRecyclerView;
    private GradeAdapter gradeAdapter;
    private List<Grade> gradeList;
    private SharedPreferences prefs;
    // currentDay: 0 = Sunday, 6 = Saturday; -1 means "today"


    // Drawer and menu button
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    @Override
    protected int getLayoutResourceId() {
        // Provide the layout specific to MainActivity
        return R.layout.activity_home;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // Ensure your XML now includes a BottomNavigationView with id "bottom_navigation"



        scheduleRecyclerView = findViewById(R.id.schedule_recycler_view);
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        scheduleItems = new ArrayList<JSONObject>();
        adapter = new ScheduleAdapter(this, scheduleItems);
        scheduleRecyclerView.setAdapter(adapter);

        new Thread(() -> {
            // Perform network call here safely
            try {
                System.out.println(PlatformStorage.getCourses(this));




            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();



        // Bottom Navigation Bar Setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNavigationView);

        int todayIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 6) % 7;
        if (todayIdx > 5) todayIdx = 0;
        fetchScheduleUpdated(todayIdx);



        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);



    }


    private void fetchScheduleUpdated(int dayIdx){
        Platform platform = PlatformStorage.getAccount(this, 0);

        scheduleItems.clear();

        new Thread(() -> {
            // Perform network call here safely
            try {

                JSONArray scheduledDays = platform.getScheduleIndexes();
                Set<Integer> allowed = new HashSet<>();
                for (int i = 0; i < scheduledDays.length(); i++) {
                    allowed.add(scheduledDays.getInt(i));
                }
                if (!allowed.contains(dayIdx)) {
                    scheduleItems.add(new JSONObject().put("subject", "יום מנוחה").put("num","").put("teacher", "אין לימודים היום \uD83C\uDF89\n").put("changes", "").put("exams", "").put("colorClass", "lightyellow-cell"));
                }

                JSONObject schedule = platform.getSchedule(dayIdx);
                for (Iterator<String> it = schedule.keys(); it.hasNext(); ) {
                    String index = it.next(); // "1", "2", etc.
                    JSONObject hour = schedule.getJSONObject(index);
                    scheduleItems.add(hour);
                    System.out.println(hour);
                }
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();

                });                // You can update UI using runOnUiThread() if needed
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void fetchScheduleOriginal(int dayIdx){
        Platform platform = PlatformStorage.getAccount(this, 0);
        scheduleItems.clear();

        new Thread(() -> {
            // Perform network call here safely
            try {
                JSONObject schedule = platform.getOriginalSchedule(dayIdx);
                for (Iterator<String> it = schedule.keys(); it.hasNext(); ) {
                    String index = it.next(); // "1", "2", etc.
                    JSONObject hour = schedule.getJSONObject(index);
                    scheduleItems.add(hour);
                    System.out.println(hour);
                }
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();

                });                // You can update UI using runOnUiThread() if needed
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}