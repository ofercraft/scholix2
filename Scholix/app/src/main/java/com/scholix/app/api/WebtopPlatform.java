package com.scholix.app.api;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.util.Log;

import com.scholix.app.Grade;
import com.scholix.app.ScheduleItem;
import com.scholix.app.UnsafeOkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.content.SharedPreferences;

public class WebtopPlatform implements Platform {
    public String name, institution, studentId, classCode, username, password;

    public String cookies;
    public final OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();
    boolean loggedIn = false;
    public boolean editing = false;
    private ArrayList<JSONObject> courses = new ArrayList<>();

    public WebtopPlatform() {

    }

    public WebtopPlatform(String username, String password) throws IOException, JSONException {
        JSONObject loginData = new JSONObject();
        loginData.put("Data", encrypt(username + "0"));
        loginData.put("UserName", username);
        loginData.put("Password", password);
        loginData.put("deviceDataJson", "{\"isMobile\":true,\"os\":\"Android\",\"browser\":\"Chrome\",\"cookies\":true}");

        Request request = new Request.Builder()
                .url("https://webtopserver.smartschool.co.il/server/api/user/LoginByUserNameAndPassword")
                .post(RequestBody.create(loginData.toString(), MediaType.get("application/json; charset=utf-8")))
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";
        JSONObject jsonResponse = new JSONObject(responseBody);

        if (jsonResponse.isNull("data")) {
            System.out.println("Login error: " + jsonResponse.optString("errorDescription", "Unknown error"));
            return;
        }

        JSONObject data = jsonResponse.getJSONObject("data");
        this.studentId = data.getString("userId");
        this.classCode = data.getString("classCode") + "|" + data.get("classNumber");
        this.institution = data.getString("institutionCode");
        this.name = data.getString("firstName") + " " + data.getString("lastName");
        java.util.List<String> cookieList = (java.util.List<String>) response.headers("Set-Cookie");
        this.cookies = String.join("; ", cookieList);
        this.username = username;
        this.password = password;
        loggedIn = true;
        this.courses.add(new JSONObject()
                .put("name", "Webtop")
                .put("semester", getCurrentSemester())
                .put("year", java.time.Year.now().getValue())
        );
    }

    @Override
    public String getName() { return name; }
    @Override
    public String getUsername() { return username; }
    @Override
    public String getPassword() { return password; }

    public String getStudentId() { return studentId; }
    public String getInstitution() { return institution; }

    @Override
    public String toString() {
        return "Platform{" + "name='" + name + '\'' + ", institution='" + institution + "'}";
    }

    private String encrypt(String data) {
        String key = "01234567890000000150778345678901";
        try {
            byte[] salt = new byte[16], iv = new byte[16];
            new SecureRandom().nextBytes(salt);
            new SecureRandom().nextBytes(iv);

            PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), salt, 100, 256);
            SecretKeySpec secretKey = new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec).getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[salt.length + iv.length + encrypted.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getCurrentSemester() {
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH); // 0 = January, 11 = December

        if (month >= Calendar.SEPTEMBER || month <= Calendar.JANUARY) {
            return "a"; // Semester a
        } else {
            return "b"; // Semester b
        }
    }


    public JSONArray getGrades() {
        int year = java.time.LocalDate.now().getYear();
        return getGrades(year, getCurrentSemester());
    }
    public JSONArray getGrades(String course, String semester) {
        int year = java.time.LocalDate.now().getYear();
        return getGrades(year, semester);
    }
    public JSONArray getGrades(String course) {
        int year = java.time.LocalDate.now().getYear();
        return getGrades(year, getCurrentSemester());
    }
    public JSONArray getGrades(int year, String semester) {
        JSONArray grades = new JSONArray();
        try {
            int periodId;
            if ("a".equals(semester)) {
                periodId = 1103;
            } else if ("b".equals(semester)) {
                periodId = 1102;
            } else if ("ab".equals(semester)) {
                periodId = 0;
            } else {
                throw new IllegalArgumentException("Invalid period");
            }

            JSONObject requestJson = new JSONObject();
            requestJson.put("studyYear", year > 0 ? year : java.time.LocalDate.now().getYear());
            requestJson.put("moduleID", 1);
            requestJson.put("periodID", periodId);
            requestJson.put("studentID", studentId);

            Request request = new Request.Builder()
                    .url("https://webtopserver.smartschool.co.il/server/api/PupilCard/GetPupilGrades")
                    .addHeader("Cookie", cookies)
                    .post(RequestBody.create(requestJson.toString(), MediaType.get("application/json; charset=utf-8")))
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) return grades;
            String responseBody = response.body() != null ? response.body().string() : "";
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray data = jsonResponse.getJSONArray("data");

            for (int i = 0; i < data.length(); i++) {
                JSONObject g = data.getJSONObject(i);
                JSONObject grade = new JSONObject();
                String subject = g.optString("subject", "Unknown Subject");
                grade.put("subject", subject);
                String name = g.optString("title", "Untitled");
                grade.put("name", name);
                String value = g.optString("grade", "N/A");
                grade.put("grade", value);
                String date = java.time.LocalDate
                        .parse(g.optString("date", "1999-12-31T00:00:00"), java.time.format.DateTimeFormatter.ISO_DATE_TIME)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                JSONArray submissions = new JSONArray();
                JSONObject submission = new JSONObject();
                submission.put("type", g.optString("type", "N/A"));
                submission.put("grade", g.optString("grade", "N/A"));
                submission.put("date", date);
                grade.put("submission", submission);
                grade.put("date", date);
                if (!value.equals("N/A") && !value.equals("null")) {
                    grades.put(grade);
                }
            }
        } catch (Exception e) {
            Log.e("GradeFetcher", "Failed to fetch grades", e);
        }
        return grades;
    }

    public JSONObject getSchedule(int dayIndex) throws JSONException, IOException{
        JSONObject schedule = new JSONObject();
        if (dayIndex < 0)
            return schedule;
        try {
            JSONObject payload = new JSONObject();
            payload.put("institutionCode", institution);
            payload.put("selectedValue",   classCode);
            payload.put("typeView",        1);

            RequestBody body = RequestBody.create(
                    payload.toString(), MediaType.get("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url("https://webtopserver.smartschool.co.il/server/api/shotef/ShotefSchedualeData")
                    .addHeader("Cookie", cookies)
                    .post(body)
                    .build();


            Response response = client.newCall(request).execute();
            String respBody   = response.body() != null ? response.body().string() : "";
            JSONArray days = new JSONObject(respBody).getJSONArray("data");
            if (dayIndex>=days.length())
                return schedule;
            JSONObject day  = days.getJSONObject(dayIndex);
            JSONArray  hoursRaw   = day.getJSONArray("hoursData");
            JSONObject hoursOriginal = new JSONObject();
            for (int i = 0; i < hoursRaw.length(); i++) {
                JSONObject hour = hoursRaw.getJSONObject(i);
                if (hour.has("scheduale") && hour.getJSONArray("scheduale").length() > 0) {
                    processScheduleOriginal(hour, hoursOriginal);   // original helper
                }
            }
            System.out.println(hoursOriginal);
            JSONObject hours = new JSONObject();

            for (int i = 0; i < hoursRaw.length(); i++) {
                JSONObject hour = hoursRaw.getJSONObject(i);
                if (hour.has("scheduale") && hour.getJSONArray("scheduale").length() > 0) {
                    processScheduleUpdated(hour, hours, hoursOriginal);
                }
            }
            return hours;


        } catch (Exception e) {
            Log.e("GradeFetcher", "Failed to fetch grades", e);
        }
        return new JSONObject();
        //example: {"1":{"num":1,"subject":"מתמטיקה האצה","teacher":"שרון יערי","colorClass":"lightgreen-cell","changes":"","exams":""},"2":{"num":2,"subject":"היסטוריה","teacher":"אביגיל קרפל","colorClass":"lightred-cell","changes":"","exams":""},"3":{"num":3,"subject":"אנגלית","teacher":"טלי איסקוב ענבר","colorClass":"lime-cell","changes":"","exams":""},"4":{"num":4,"subject":"אנגלית","teacher":"טלי איסקוב ענבר","colorClass":"lime-cell","changes":"","exams":""},"5":{"num":5,"subject":"חינוך גופני","teacher":"מיכה פולק","colorClass":"lightorange-cell","changes":"","exams":""},"6":{"num":6,"subject":"עברית","teacher":"חגית ליבוביץ","colorClass":"lightpurple-cell","changes":"","exams":""},"7":{"num":7,"subject":"של``ח","teacher":"נחשון שמר","colorClass":"custom-pink-cell","changes":"","exams":""}}
    }
    public JSONObject getOriginalSchedule(int dayIndex) throws JSONException, IOException{
        JSONObject schedule = new JSONObject();
        if (dayIndex < 0)
            return schedule;
        try {
            JSONObject payload = new JSONObject();
            payload.put("institutionCode", institution);
            payload.put("selectedValue",   classCode);
            payload.put("typeView",        1);

            RequestBody body = RequestBody.create(
                    payload.toString(), MediaType.get("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url("https://webtopserver.smartschool.co.il/server/api/shotef/ShotefSchedualeData")
                    .addHeader("Cookie", cookies)
                    .post(body)
                    .build();


            Response response = client.newCall(request).execute();
            String respBody   = response.body() != null ? response.body().string() : "";
            JSONArray days = new JSONObject(respBody).getJSONArray("data");
            if (dayIndex>=days.length())
                return schedule;
            JSONObject day  = days.getJSONObject(dayIndex);
            JSONArray  hoursRaw   = day.getJSONArray("hoursData");
            JSONObject hoursOriginal = new JSONObject();
            for (int i = 0; i < hoursRaw.length(); i++) {
                JSONObject hour = hoursRaw.getJSONObject(i);
                if (hour.has("scheduale") && hour.getJSONArray("scheduale").length() > 0) {
                    processScheduleOriginal(hour, hoursOriginal);   // original helper
                }
            }
            return hoursOriginal;


        } catch (Exception e) {
            Log.e("GradeFetcher", "Failed to fetch grades", e);
        }
        return new JSONObject();
    }

    public JSONArray getScheduleIndexes() throws JSONException, IOException{
        JSONObject schedule = new JSONObject();
        try {
            JSONObject payload = new JSONObject();
            payload.put("institutionCode", institution);
            payload.put("selectedValue",   classCode);
            payload.put("typeView",        1);

            RequestBody body = RequestBody.create(
                    payload.toString(), MediaType.get("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url("https://webtopserver.smartschool.co.il/server/api/shotef/ShotefSchedualeData")
                    .addHeader("Cookie", cookies)
                    .post(body)
                    .build();


            Response response = client.newCall(request).execute();
            String respBody   = response.body() != null ? response.body().string() : "";
            JSONArray days = new JSONObject(respBody).getJSONArray("data");
            JSONArray indexes = new JSONArray();
            for(int i=0; i<days.length(); i++){
                JSONObject day = days.getJSONObject(i);
                if (day.getJSONArray("hoursData").length()!=0){
                    indexes.put(day.getInt("dayIndex")-1);
                }
            }
            return indexes;


        } catch (Exception e) {
            Log.e("GradeFetcher", "Failed to fetch grades", e);
        }
        return new JSONArray();
    }









    @Override
    public boolean isLoggedIn() {
        return loggedIn;
    }

    private void processScheduleOriginal(JSONObject hourRaw, JSONObject hours) throws Exception {
        JSONArray scheduleArray = hourRaw.getJSONArray("scheduale");
        JSONObject scheduleItem = scheduleArray.getJSONObject(0);

        String subject = cleanSubject(scheduleItem.optString("subject", "לא זמין"));
        String teacher = scheduleItem.optString("teacherPrivateName", "לא זמין") + " " + scheduleItem.optString("teacherLastName", "לא זמין");
        int hourNum = scheduleItem.optInt("hour", -1);
        String colorClass = findColorClass(subject);

        JSONObject hour = new JSONObject();
        hour.put("num", hourNum);
        hour.put("subject", subject);
        hour.put("teacher", teacher);
        hour.put("colorClass", colorClass);
        hour.put("changes", "");
        hour.put("exams", "");
        hours.put(String.valueOf(hourNum), hour);
    }
    private void processScheduleUpdated(JSONObject hourRaw, JSONObject hours, JSONObject hoursOriginal) throws Exception {
        JSONArray scheduleArray = hourRaw.getJSONArray("scheduale");
        JSONObject scheduleItem = scheduleArray.getJSONObject(0);

        String subject = cleanSubject(scheduleItem.optString("subject", "לא זמין"));
        String teacher = scheduleItem.optString("teacherPrivateName", "לא זמין") + " " + scheduleItem.optString("teacherLastName", "לא זמין");
        int hourNum = scheduleItem.optInt("hour", -1);
        String colorClass = findColorClass(subject);

        JSONObject hour = new JSONObject();
        hour.put("num", hourNum);
        hour.put("subject", subject);
        hour.put("teacher", teacher);
        hour.put("colorClass", colorClass);
        hour.put("changes", "");
        hour.put("exams", "");




        if (hourRaw.has("exams")) {
            JSONArray examsArray = hourRaw.getJSONArray("exams");
            for (int j = 0; j < examsArray.length(); j++) {
                JSONObject examObj = examsArray.getJSONObject(j);
                subject = examObj.optString("title", "מבחן");
                teacher = examObj.optString("supervisors", "לא זמין");

                colorClass = "exam-cell";
                hour.put("exams", examObj);
            }
        }





        JSONArray changesArray = scheduleItem.getJSONArray("changes");
        boolean cancel=false;
        for (int j = 0; j < changesArray.length(); j++) {
            JSONObject itemObj = changesArray.getJSONObject(j);
            if (itemObj.optString("definition", "לא זמין").equals("ביטול שיעור") &&
                    (itemObj.optInt("original_hour", -1) == -1 || itemObj.optInt("original_hour", -1) == hourNum)) {
                cancel = true;
            }
            if (itemObj.optInt("original_hour", -1) != -1) {
                JSONObject originalHour = hoursOriginal.getJSONObject(itemObj.optString("original_hour", "0"));

                cancel=true;

            }

            if (itemObj.optString("definition", "לא זמין").equals("הזזת שיעור")) {
                String fillTeacher = itemObj.optString("privateName", "לא זמין") + " " + itemObj.optString("lastName", "לא זמין");

                boolean found=false;
                for (int i=0; i<hoursOriginal.length(); i++) {
                    JSONObject existing = hoursOriginal.getJSONObject(String.valueOf(i));
                    if (existing.getString("teacher").equals(fillTeacher)) {
                        hour.put("subject",existing.getString("subject"));
                        hour.put("teacher", existing.getString("teacher"));
                        hour.put("colorClass",existing.getString("colorClass"));
                        found=true;
                        break;
                    }
                }
                if(!found){
                    String changes = hour.getString("changes");
                    changes += "מילוי מקום של " + fillTeacher + "\n";
                }
            }
            if (itemObj.optString("definition", "לא זמין").equals("מילוי מקום")) {
                String fillTeacher = itemObj.optString("privateName", "לא זמין") + " " + itemObj.optString("lastName", "לא זמין");

                boolean found=false;
                for (int i=0; i<hoursOriginal.length(); i++) {
                    JSONObject existing = hoursOriginal.getJSONObject(String.valueOf(i));
                    if (existing.getString("teacher").equals(fillTeacher)) {
                        hour.put("subject", existing.getString("subject"));
                        hour.put("teacher", existing.getString("teacher"));
                        hour.put("colorClass", existing.getString("colorClass"));
                        found=true;
                        break;
                    }
                }
                if(!found){
                    String changes = hour.getString("changes");
                    changes += "מילוי מקום של " + fillTeacher + "\n";
                }
            }

        }
        if (hourRaw.has("events") && hourRaw.getJSONArray("events").length() > 0){
            JSONArray events = hourRaw.optJSONArray("events");
            assert events != null;
            JSONObject event = events.getJSONObject(0);
            String title = event.getString("title");
            String type = event.getString("title");
            String accompaniers = event.getString("accompaniers").replaceAll(",\\s*$", "");


            if (!accompaniers.equals(",") && !accompaniers.equals(" ") && !accompaniers.isEmpty())
                hour.put("teacher", accompaniers);
            hour.put("subject", title);
            hour.put("changes", "");
        }
        if (!cancel){
            hours.put(String.valueOf(hourNum), hour);
        }


    }
    private String cleanSubject(String subject) {
        if (subject == null) return "לא זמין";
        return subject.replace("\"", "").trim();
    }

    private String findColorClass(String subject) {
        if (SUBJECT_COLORS.containsKey(subject)) {
            return SUBJECT_COLORS.get(subject);
        }
        for (String key : SUBJECT_COLORS.keySet()) {
            if (subject.contains(key)) {
                return SUBJECT_COLORS.get(key);
            }
        }

        // Generate a random pastel color class name
        String[] colorPool = {"red", "green", "blue", "orange", "yellow", "purple", "teal", "lime", "pink"};
        String randomColor = "custom-" + colorPool[new Random().nextInt(colorPool.length)] + "-cell";

        // Save to both map and preferences
        SUBJECT_COLORS.put(subject, randomColor);

        return randomColor;
    }
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


    public boolean refreshCookies() throws IOException, JSONException {
        JSONObject loginData = new JSONObject();
        loginData.put("Data", encrypt(this.username + "0"));
        loginData.put("UserName", this.username);
        loginData.put("Password", this.password);
        loginData.put("deviceDataJson", "{\"isMobile\":true,\"os\":\"Android\",\"browser\":\"Chrome\",\"cookies\":true}");

        Request request = new Request.Builder()
                .url("https://webtopserver.smartschool.co.il/server/api/user/LoginByUserNameAndPassword")
                .post(RequestBody.create(loginData.toString(), MediaType.get("application/json; charset=utf-8")))
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";
        JSONObject jsonResponse = new JSONObject(responseBody);

        if (jsonResponse.isNull("data")) {
            System.out.println("Login error: " + jsonResponse.optString("errorDescription", "Unknown error"));
            loggedIn = false;
            return false;
        }

        JSONObject data = jsonResponse.getJSONObject("data");
        this.studentId = data.getString("userId");
        this.classCode = data.getString("classCode") + "|" + data.get("classNumber");
        this.institution = data.getString("institutionCode");
//        this.name = data.getString("firstName") + " " + data.getString("lastName");
        java.util.List<String> cookieList = (java.util.List<String>) response.headers("Set-Cookie");
        this.cookies = String.join("; ", cookieList);
        loggedIn = true;
        return true;
    }


    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("class", getClass().getName());
        root.put("name",        name);
        root.put("institution", institution);
        root.put("studentId",   studentId);
        root.put("classCode",   classCode);
        root.put("username",    username);
        root.put("password",    password);
        root.put("cookies",     cookies);
        root.put("loggedIn",    loggedIn);
        return root;
    }

    /**
     * Deserialize a WebtopPlatform from the same JSON produced by toJson().
     */
    public static WebtopPlatform fromJson(JSONObject obj) throws JSONException, IOException {
        // 1) pull credentials & re-login via your existing ctor
        String username = obj.getString("username");
        String password = obj.getString("password");
        WebtopPlatform p = new WebtopPlatform();

        // 2) restore all other fields from JSON
        p.username    = obj.optString("username",   p.username);
        p.password    = obj.optString("password",   p.password);
        p.name        = obj.optString("name",        p.name);
        p.institution = obj.optString("institution", p.institution);
        p.studentId   = obj.optString("studentId",   p.studentId);
        p.classCode   = obj.optString("classCode",   p.classCode);
        p.cookies     = obj.optString("cookies",     p.cookies);
        p.loggedIn    = obj.optBoolean("loggedIn",   p.loggedIn);
        p.courses.add(new JSONObject()
                .put("name", "Webtop")
                .put("year", java.time.Year.now().getValue())
        );
        return p;
    }
    public boolean isEditing(){
        return editing;
    };
    public void startEditing(){
        editing = true;
    };
    public void stopEditing(){
        editing = false;
    };
    public void setName(String name) {
        this.name = name;
    }

    public void setUsername(String username){
        this.username = username;
    };
    public void setPassword(String password){
        this.password = password;
    };

    public ArrayList<JSONObject> getCourses(){
        return courses;
    }
}
