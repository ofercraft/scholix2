package com.scholix.app.api;

import com.scholix.app.UnsafeOkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;

public interface Platform {
    String name = "";
    String username = "";
    String password = "";
    boolean editing = false;

    boolean loggedIn = false;

    String getName();
    String getUsername();
    String getPassword();
    OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();
    JSONArray getGrades() throws JSONException, IOException;
    JSONArray getGrades(String course) throws JSONException, IOException;

    JSONObject getSchedule(int dayIndex) throws JSONException, IOException;
    JSONObject getOriginalSchedule(int dayIndex) throws JSONException, IOException;
    boolean isLoggedIn();

    boolean refreshCookies() throws IOException, JSONException;
    public JSONObject toJson() throws JSONException;

    public static Platform fromJson(JSONObject obj) throws JSONException, IOException {
        return null;
    }

    JSONArray getScheduleIndexes() throws JSONException, IOException;
    public boolean isEditing();
    public void startEditing();
    public void stopEditing();
    public void setName(String name);
    public void setUsername(String username);
    public void setPassword(String password);
    public ArrayList<JSONObject> getCourses();

}
