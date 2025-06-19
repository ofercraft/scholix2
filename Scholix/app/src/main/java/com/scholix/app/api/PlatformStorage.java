package com.scholix.app.api;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
        System.out.println(array.toString());
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
            System.out.println(array);
            System.out.println(array);
            System.out.println(array);
            System.out.println(array);
            System.out.println(array);
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
}
