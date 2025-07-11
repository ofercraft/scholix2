package com.scholix.app.api;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.scholix.app.Grade;
import com.scholix.app.UnsafeOkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BarIlanPlatform implements Platform {

    public String name, studentId, password, token;
    public ArrayList<JSONObject> courses = new ArrayList<>();
    public final OkHttpClient client = new OkHttpClient();
    public boolean loggedIn = false;

    public BarIlanPlatform() {}


    public BarIlanPlatform(String studentId, String password) throws IOException, JSONException {
        this.studentId = studentId;
        this.password = password;

        // Login
        JSONObject loginJson = new JSONObject(client.newCall(
                new Request.Builder()
                        .url("https://biumath.michlol4.co.il/api/Login/Login")
                        .post(RequestBody.create(
                                new JSONObject()
                                        .put("captchaToken", JSONObject.NULL)
                                        .put("loginType", "student")
                                        .put("password", password)
                                        .put("zht", studentId)
                                        .put("deviceDataJson", "{\"isMobile\":true,\"os\":\"Android\",\"browser\":\"Chrome\",\"cookies\":true}")
                                        .toString(),
                                MediaType.get("application/json; charset=utf-8")
                        ))
                        .build()
        ).execute().body().string());
        if (!loginJson.optBoolean("success", false)) {
            loggedIn = false;
            return;
        }
        token = loginJson.getString("token");

        // User Info
        JSONObject infoJson = new JSONObject(client.newCall(
                new Request.Builder()
                        .url("https://biumath.michlol4.co.il/api/Account/UserInfo")
                        .post(RequestBody.create("{}", MediaType.get("application/json; charset=utf-8")))
                        .header("Authorization", "Bearer " + token)
                        .build()
        ).execute().body().string());
        JSONObject user = infoJson.getJSONObject("userInfo");
        name = user.getString("smp") + " " + user.getString("smm");

        // Courses
        JSONObject coursesJson = new JSONObject(client.newCall(
                new Request.Builder()
                        .url("https://biumath.michlol4.co.il/api/StudentCourses/Data")
                        .post(RequestBody.create(
                                new JSONObject().put("urlParameters", new JSONObject()).toString(),
                                MediaType.get("application/json; charset=utf-8")
                        ))
                        .header("Authorization", "Bearer " + token)
                        .build()
        ).execute().body().string());
        JSONArray data = coursesJson.getJSONObject("courses").getJSONArray("clientData");
        for (int i = 0; i < data.length(); i++) {
            JSONObject src = data.getJSONObject(i);
            List<String> parts = Arrays.asList(src.getString("all_pms_shm").trim().split("\\s+"));
            Collections.reverse(parts);
            courses.add(new JSONObject()
                    .put("name", src.getString("krs_shm"))
                    .put("year", src.getString("krs_snl"))
                    .put("teacher", String.join(" ", parts))
            );
        }
        loggedIn = true;
    }







    public ArrayList<JSONObject> getCourses(){
        return courses;
    }
    public JSONArray getGrades() throws JSONException, IOException {
        return getGrades("all");
    }
    public JSONObject getSchedule(int dayIndex){
        return new JSONObject();
    }
    public JSONObject getOriginalSchedule(int dayIndex){
        return getSchedule(dayIndex);
    }
    public JSONArray getGrades(String course) throws IOException, JSONException {
        JSONObject loginData = new JSONObject();
        loginData.put("urlParameters", new ArrayList<>());

        Request request = new Request.Builder()
                .url("https://biumath.michlol4.co.il/api/Grades/Data")
                .post(RequestBody.create(loginData.toString(), MediaType.get("application/json; charset=utf-8")))
                .header("Authorization", "Bearer " + token)

                .build();

        Response response = client.newCall(request).execute();
        String pageData = response.header("pagedata");

        String responseBody = response.body() != null ? response.body().string() : "";
        JSONObject jsonResponse = new JSONObject(responseBody);
        JSONArray courses = jsonResponse.getJSONObject("collapsedCourses").getJSONArray("clientData");
        JSONArray gradesRaw = new JSONArray();
        JSONArray grades = new JSONArray();

        for(int i=0; i<courses.length(); i++){
            JSONObject currentCourse = new JSONObject(courses.get(i).toString());
            if(course.equals("all") || currentCourse.getString("krs_shm").equals(course)){
                gradesRaw=currentCourse.getJSONArray("__body");
                for (int m=0; m<gradesRaw.length(); m++){
                    JSONObject gradeRaw = new JSONObject(gradesRaw.get(m).toString());
                    JSONObject grade = new JSONObject();
                    grade.put("name", gradeRaw.getString("krs_shm"));
                    grade.put("subject", currentCourse.getString("krs_shm"));
                    grade.put("date", gradeRaw.getString("krs_snl"));
                    grade.put("grade", gradeRaw.getString("bhnzin"));
                    JSONArray data = gradeRaw.getJSONArray("__body");
                    JSONArray submissions = new JSONArray();
                    for (int j=0; j<data.length(); j++){
                        JSONObject moedRaw = new JSONObject(data.get(j).toString());
                        JSONObject moed = new JSONObject();

                        moed.put("type", moedRaw.getString("zin_sug"));
                        moed.put("grade", moedRaw.getString("moed_1_zin"));
                        moed.put("date", moedRaw.getString("krs_snl"));
                        JSONArray buttons = moedRaw.getJSONArray("__buttons");
                        JSONObject downloadData = new JSONObject();
                        for (int k=0; k<buttons.length(); k++){
                            JSONObject button = buttons.getJSONObject(k);
                            String description = button.getString("description");
                            if (description.equals("בחינה סרוקה")){
                                JSONObject routeData = button.getJSONObject("routeData");
                                downloadData.put("scanLocation", routeData.getString("scan_location"));
                                downloadData.put("scanFileName", routeData.getString("scanfilename"));
                                downloadData.put("hash", routeData.getString("__hash"));
                                downloadData.put("scanPt", routeData.getString("scan_pt"));
                                downloadData.put("rowKey", routeData.getString("rowkey"));
                                downloadData.put("pageData", pageData);
                            }
                        }
                        moed.put("downloadData", downloadData);
                        submissions.put(moed);
                    }
                    grade.put("submissions", submissions);
                    grades.put(grade);
                    //example grade: {"name":"מבחן מחצית (%)","subject":"שכבה ט הוד השרון יום ב 16:00-19:15","date":"תשפה","grade":"100","submissions":[{"type":"מועד 1","grade":"100","date":"תשפה","downloadData":{"scanLocation":"\\\\rashim\\files\\mimsakim\\upload\\335476065\\00125831041\\BW.PDF","scanFileName":"BW.PDF"}},{"type":"מועד 2","grade":"","date":"תשפה","downloadData":{}}]}            grades.put(grade);
                }
            }
        }


        return grades;
    }
    public void download(JSONObject downloadData) throws IOException, JSONException {
        JSONObject loginData = new JSONObject();
        loginData.put("scan_location", downloadData.getString("scanLocation"));
        loginData.put("scan_answers_location",
                downloadData.getString("scanLocation")
                        .replaceAll("\\\\+" + Pattern.quote(downloadData.getString("scanFileName")) + "$", "")
                        .replaceAll("(\\\\)\\d+(\\\\)(\\d+)$", "$1Answers$2$3"));
        loginData.put("scan_questions_location",
                downloadData.getString("scanLocation")
                        .replaceAll("\\\\+" + Pattern.quote(downloadData.getString("scanFileName")) + "$", "")
                        .replaceAll("(\\\\)\\d+(\\\\)(\\d+)$", "$1Questions$2$3"));
        loginData.put("scanfilename", downloadData.getString("scanFileName"));
        loginData.put("__hash", downloadData.getString("hash"));
        loginData.put("scan_pt", downloadData.getString("scanPt"));
        loginData.put("rowkey", downloadData.getString("rowKey"));
        loginData.put("agudamember", "no");
        loginData.put("num", "10");
        loginData.put("pay_for_scan", "false");
        loginData.put("scan_bhn_moed", "1");
        loginData.put("scan_bhn_sid", "4");
        loginData.put("scan_bhn_sms", "א");
        loginData.put("scan_krs_nmrtr", "12583");

        Request request = new Request.Builder()
                .url("https://biumath.michlol4.co.il/api/Grades/CheckExamPayment")
                .post(RequestBody.create(loginData.toString(), MediaType.get("application/json; charset=utf-8")))
                .header("Authorization", "Bearer " + token)
                 .header("__hash", downloadData.getString("hash"))
                .header("pagedata", downloadData.getString("pageData"))

                .build();

        try{
            Thread.sleep(1000);
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.out.println("Failed to download: " + response.code());
                    return;
                }

                // File name and path
                String fileName = downloadData.getString("scanFileName");
                File outFile = new File("downloads/" + fileName); // save in /downloads folder (create if needed)

                // Ensure directory exists
                outFile.getParentFile().mkdirs();

                // Write response to file
                try (InputStream in = response.body().byteStream();
                     FileOutputStream out = new FileOutputStream(outFile)) {

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }

                    System.out.println("✅ File downloaded: " + outFile.getAbsolutePath());
                }
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() { return name; }
    public String getStudentId() { return studentId; }

    @Override
    public String toString() {
        return "BarIlanPlatform{" +
                "name='" + name + '\'' +
                ", studentId='" + studentId + '\'' +
                ", password='" + (password) + '\'' +  // mask password
                ", token='" + token + '\'' +
                ", courses=" + courses +
                ", loggedIn=" + loggedIn +
                '}';
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
    @Override
    public JSONObject toLoginJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("username", this.studentId); // or username field
        obj.put("password", this.password);  // you'll need to store this if not already
        return obj;
    }
    public boolean refreshCookies() throws IOException, JSONException {
        JSONObject loginData = new JSONObject();
        loginData.put("captchaToken", null);
        loginData.put("loginType", "student");
        loginData.put("password", this.password);
        loginData.put("zht", this.studentId);
        loginData.put("deviceDataJson", "{\"isMobile\":true,\"os\":\"Android\",\"browser\":\"Chrome\",\"cookies\":true}");
        Request request = new Request.Builder()
                .url("https://biumath.michlol4.co.il/api/Login/Login")
                .post(RequestBody.create(loginData.toString(), MediaType.get("application/json; charset=utf-8")))
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";
        JSONObject jsonResponse = new JSONObject(responseBody);
        System.out.println(jsonResponse);
        if (jsonResponse.isNull("success") || !jsonResponse.getBoolean("success")) {
            System.out.println("Login error: " + jsonResponse.optString("errorDescription", "Unknown error"));
            loggedIn=false;
            return false;
        }
        this.token = jsonResponse.getString("token");
        this.studentId=this.studentId;
        loggedIn=true;
        return true;
    }
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("class",       getClass().getName());
        root.put("name",        name);
        root.put("studentId",   studentId);
        root.put("password",    password);

        // Serialize courses array
        JSONArray arr = new JSONArray();
        for (JSONObject course : courses) {
            arr.put(course);
        }
        root.put("courses", arr);

        root.put("token",     token);
        root.put("loggedIn",  loggedIn);
        return root;
    }

    public static Platform fromJson(JSONObject obj) throws JSONException, IOException {
        // 1) Extract credentials & re-login
        System.out.println(obj);
        String studentId = obj.getString("studentId");
        String password  = obj.getString("password");
        BarIlanPlatform p = new BarIlanPlatform();

        // 2) Restore simple fields
        p.name     = obj.optString("name",      p.name);
        p.token    = obj.optString("token",     p.token);
        p.loggedIn = obj.optBoolean("loggedIn", p.loggedIn);
        p.studentId = obj.optString("studentId", p.studentId);
        p.password = obj.optString("password", p.password);

        // 3) Restore courses list
        p.courses.clear();
        JSONArray arr = obj.optJSONArray("courses");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                p.courses.add(arr.getJSONObject(i));
            }
        }
        return p;
    }

    public JSONArray getScheduleIndexes(){
        return new JSONArray();
    }

}
