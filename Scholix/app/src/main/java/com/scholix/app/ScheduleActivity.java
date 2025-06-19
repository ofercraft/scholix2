package com.scholix.app;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;
import com.scholix.app.api.Platform;
import com.scholix.app.api.PlatformStorage;

import org.json.*;

import java.io.IOException;
import java.util.*;
import okhttp3.*;
/**
 * Schedule screen with six RTL day‑tabs (“ראשון” … “שישי”).
 * Tap a tab → fetch and render that day’s schedule.
 */
public class ScheduleActivity extends BaseActivity {

    private static final String TAG = "ScheduleActivity";

    private ScheduleAdapter adapter;
    private TabLayout     dayTabs;
    private TextView      dayLabel;   // optional label under the tabs

    // ────────── DATA ─────────────────────────────────────────
    private final ArrayList<JSONObject> scheduleItems = new ArrayList<>();
    private final OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();
    private boolean isLoadingSchedule = false;

    // subject → colourClass
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

    @Override protected int getLayoutResourceId() { return R.layout.activity_schedule; }

    // ───────────────────────── onCreate ──────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        TextView today_title = findViewById(R.id.today_title);

        today_title.setText(R.string.schedule);

        MaterialButtonToggleGroup scheduleToggleGroup = findViewById(R.id.schedule_mode_toggle);
        dayTabs  = findViewById(R.id.day_tabs);

        if (scheduleToggleGroup != null) {
            scheduleToggleGroup.check(R.id.btn_updated); // set default
            scheduleToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {

                    }, 3000); // delay in milliseconds

                    if (checkedId == R.id.btn_original) {
                        fetchScheduleOriginal(dayTabs.getSelectedTabPosition());
                    }
                    else if (checkedId == R.id.btn_updated) {
                        fetchScheduleUpdated(dayTabs.getSelectedTabPosition());
                    }
                }
            });
        }

        // ────────── UI ───────────────────────────────────────────
        RecyclerView scheduleRecyclerView = findViewById(R.id.schedule_recycler_view);
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScheduleAdapter(this, scheduleItems);
        scheduleRecyclerView.setAdapter(adapter);

        int todayIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 6) % 7;
        if (todayIdx > 5) todayIdx = 0;

        dayTabs.selectTab(dayTabs.getTabAt(todayIdx));
        updateDayLabel(todayIdx);
        Platform platform = PlatformStorage.getAccount(this, 0);
        System.out.println(platform);
        scheduleItems.clear();
        fetchScheduleUpdated(todayIdx);

        TabLayout dayTabs = findViewById(R.id.day_tabs);

        new Thread(() -> {
            try {
                // 1. fetch and parse
                JSONArray scheduledDays = platform.getScheduleIndexes();
                Set<Integer> allowed = new HashSet<>();
                for (int i = 0; i < scheduledDays.length(); i++) {
                    allowed.add(scheduledDays.getInt(i));
                }

                // 2. now switch back to the UI thread to hide/show
                runOnUiThread(() -> {
                    ViewGroup tabStrip = (ViewGroup) dayTabs.getChildAt(0);
                    for (int i = 0; i < tabStrip.getChildCount(); i++) {
                        View tabView = tabStrip.getChildAt(i);
                        tabView.setVisibility( allowed.contains(i)
                                ? View.VISIBLE
                                : View.GONE );
                    }
                });
            } catch (JSONException|IOException e) {
                e.printStackTrace();
            }
        }).start();


        if (dayTabs != null) {
            dayTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    updateDayLabel(tab.getPosition());
                    fetchScheduleUpdated(tab.getPosition());
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
            dayTabs.selectTab(dayTabs.getTabAt(todayIdx));   // will trigger fetchSchedule
        } else {
            fetchScheduleUpdated(todayIdx);
        }

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNav);
    }

    // ───────────────────────── helpers ───────────────────────────────
    private void updateDayLabel(int day) {
        if (dayLabel == null) return;
        switch (day) {
            case 0: dayLabel.setText("ראשון");  break;
            case 1: dayLabel.setText("שני");    break;
            case 2: dayLabel.setText("שלישי");  break;
            case 3: dayLabel.setText("רביעי");  break;
            case 4: dayLabel.setText("חמישי");  break;
            case 5: dayLabel.setText("שישי");   break;
            default: dayLabel.setText("היום");
        }
    }

    private void fetchScheduleUpdated(int dayIdx){
        if (isLoadingSchedule) return;
        isLoadingSchedule = true;
        System.out.println("a");
        Platform platform = PlatformStorage.getAccount(this, 0);
        scheduleItems.clear();

        new Thread(() -> {
            // Perform network call here safely
            try {
                JSONObject schedule = platform.getSchedule(dayIdx);
                for (Iterator<String> it = schedule.keys(); it.hasNext(); ) {
                    String index = it.next(); // "1", "2", etc.
                    JSONObject hour = schedule.getJSONObject(index);
                    scheduleItems.add(hour);
                    System.out.println(hour);
                }
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    isLoadingSchedule = false;

                });                // You can update UI using runOnUiThread() if needed
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void fetchScheduleOriginal(int dayIdx){
        if (isLoadingSchedule) return;
        isLoadingSchedule = true;
        System.out.println("b");
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
                    isLoadingSchedule = false;

                });                // You can update UI using runOnUiThread() if needed
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}