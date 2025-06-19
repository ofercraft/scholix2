package com.scholix.app.api;

import com.scholix.app.UnsafeOkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;

public interface Platform {
    String name=null;
    boolean loggedIn = false;

    String getName();
    OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();
    JSONArray getGrades() throws JSONException, IOException;
    JSONObject getSchedule(int dayIndex) throws JSONException, IOException;
    JSONObject getOriginalSchedule(int dayIndex) throws JSONException, IOException;
    boolean isLoggedIn();
    JSONObject toLoginJson() throws JSONException;
    boolean refreshCookies() throws IOException, JSONException;
    public JSONObject toJson() throws JSONException;

    public static Platform fromJson(JSONObject obj) throws JSONException, IOException {
        return null;
    }

    JSONArray getScheduleIndexes() throws JSONException, IOException;
}
