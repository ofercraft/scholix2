package com.scholix.app.api;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PlatformStorage {

    private static final String PREFS_NAME = "platform_prefs";
    public static final String KEY_PLATFORMS = "platforms_logins";
    private static final String TAG = "PlatformStorage";


    /**
     * Serializes and saves the entire list of Platform objects.
     */
    public static void savePlatforms(Context context, List<Platform> platforms) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        for (Platform p : platforms) {
            try {
                array.put(p.toJson());
            } catch (Exception e) {
                Log.e(TAG, "Error serializing platform: " + p.getClass().getSimpleName(), e);
            }
        }
        prefs.edit()
                .putString(KEY_PLATFORMS, array.toString())
                .apply();
    }

    /**
     * Loads and deserializes the full list of Platform objects using manual JSON parsing.
     */
    public static List<Platform> loadPlatforms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PLATFORMS, null);
        List<Platform> platforms = new ArrayList<>();
        if (json == null || json.isEmpty()) return platforms;

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                try {
                    // Each Platform class must implement static fromJson(JSONObject)
                    String className = obj.getString("class");
                    Class<?> cls = Class.forName(className);
                    Platform p = (Platform) cls.getMethod("fromJson", JSONObject.class)
                            .invoke(null, obj);
                    platforms.add(p);
                } catch (Exception e) {
                    Log.e(TAG, "Error deserializing platform JSON at index " + i, e);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Invalid platforms JSON", e);
        }
        return platforms;
    }

    /**
     * Adds a single Platform object to stored list and persists the full list.
     */
    public static void addPlatform(Context context, Platform platform) {
        List<Platform> platforms = loadPlatforms(context);
        platforms.add(platform);
        savePlatforms(context, platforms);
    }
    public static List<Platform> addPlatform(Context context, String username, String password) throws JSONException, IOException {
        List<Platform> platforms = loadPlatforms(context);
        List<Platform> newPlatforms = new ArrayList<>();
        BarIlanPlatform barIlan = new BarIlanPlatform(username, password);
        if(barIlan.loggedIn){
            platforms.add(barIlan);
            newPlatforms.add(barIlan);
        }
        WebtopPlatform webtop = new WebtopPlatform(username, password);
        if(webtop.loggedIn){
            platforms.add(webtop);
            newPlatforms.add(webtop);
        }
        DemoPlatform demo = new DemoPlatform(username, password);
        if(demo.loggedIn){
            platforms.add(demo);
            newPlatforms.add(demo);
        }

        savePlatforms(context, platforms);
        return newPlatforms;
    }

    public static boolean checkPlatform(Context context, String username, String password) {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        tasks.add(() -> new BarIlanPlatform(username, password).loggedIn);
        tasks.add(() -> new WebtopPlatform(username, password).loggedIn);
        tasks.add(() -> new DemoPlatform(username, password).loggedIn);

        try {
            List<Future<Boolean>> results = executor.invokeAll(tasks);

            for (Future<Boolean> result : results) {
                if (result.get()) {
                    executor.shutdownNow(); // stop remaining threads
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
        }
        return false;
    }


    public static void refreshCookies(Context context) {
        List<Platform> platforms = loadPlatforms(context);
        for (Platform p : platforms) {
            boolean success;
            try {
                success = p.refreshCookies();
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing cookies for " + p.getClass().getSimpleName(), e);
                success = false;
            }
            if (success) {
                Log.d(TAG, "Successfully refreshed cookies for " + p.getClass().getSimpleName());
            } else {
                Log.w(TAG, "Failed to refresh cookies for " + p.getClass().getSimpleName());
            }
        }
        savePlatforms(context, platforms);
    }


    public static void clearPlatforms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_PLATFORMS)
                .apply();
        Log.d(TAG, "Cleared all stored platforms");
    }
    public static Platform getAccount(Context context, int index) {
        List<Platform> platforms = loadPlatforms(context);
        if (index < 0 || index >= platforms.size()) {
            Log.w(TAG, "getAccount: index out of bounds: " + index);
            return null;
        }
        return platforms.get(index);
    }

    /**
     * Removes a Platform object at a specific index from the stored list.
     */
    public static void removePlatform(Context context, int index) {
        List<Platform> platforms = loadPlatforms(context);
        if (index < 0 || index >= platforms.size()) {
            Log.w(TAG, "removePlatform: index out of bounds: " + index);
            return;
        }
        platforms.remove(index);
        savePlatforms(context, platforms);
        Log.d(TAG, "Removed platform at index " + index);
    }
    /**
     * Updates an existing Platform object at a specific index and saves the list.
     */
    public static void updatePlatform(Context context, int index, Platform updatedPlatform) {
        List<Platform> platforms = loadPlatforms(context);
        if (index < 0 || index >= platforms.size()) {
            Log.w(TAG, "updatePlatform: index out of bounds: " + index);
            return;
        }
        platforms.set(index, updatedPlatform);
        savePlatforms(context, platforms);
        Log.d(TAG, "Updated platform at index " + index);
    }


    /**
     * returns an array of all the courses
     * [{"name":"Webtop","year":2025,"index":0}, {"name":"שכבה ט הוד השרון יום ב 16:00-19:15","year":"תשפה","teacher":"סמדר הופ","index":1}, {"name":"שכבה ח הוד השרון יום ב 16:00-19:15","year":"תשפד","teacher":"איילה שפירא","index":1}, {"name":"שכבה ו הוד השרון יום ב 17:45-19:15","year":"תשפג","teacher":"יובל כהני","index":1}]
     */
    public static ArrayList<JSONObject> getCourses(Context context) throws JSONException {
        List<Platform> platforms = loadPlatforms(context);
        ArrayList<JSONObject> courses = new ArrayList<JSONObject>();
        for (int i=0; i<platforms.size(); i++){
            Platform platform = platforms.get(i);
            ArrayList<JSONObject> platformCourses = platform.getCourses();
            for (int j=0; j<platformCourses.size(); j++){
                JSONObject course = platformCourses.get(j);
                course.put("index", i);
                courses.add(course);
            }
        }
        return courses;
    }
}
