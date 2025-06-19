package com.scholix.app.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import static java.time.format.DateTimeFormatter.ofPattern;

public class DemoPlatform implements Platform {

    public boolean loggedIn = false;
    public String name;
    public DemoPlatform(String username, String password) throws IOException, JSONException {
        if(Objects.equals(username, "demo") && Objects.equals(password, "demo")){
            loggedIn = true;
            name="demo";
        }
    }

    public JSONArray getGrades() throws JSONException, IOException {
        return getGrades("all");
    }
    private JSONObject createHour(String num, String subject, String teacher) throws JSONException {
        String colorClass = findColorClass(subject);

        return new JSONObject()
                .put("num", num)
                .put("subject", subject)
                .put("teacher", teacher)
                .put("colorClass", colorClass)
                .put("changes", "")
                .put("exams", "");
    }

    public JSONObject getSchedule(int dayIndex) throws JSONException {
        JSONObject[] week = new JSONObject[7];

        // Day 0
        week[0] = new JSONObject();
        week[0].put("1", createHour("1", "מתמטיקה", "רון נתניהו"));
        week[0].put("2", createHour("2", "עברית", "יעל כהן"));
        week[0].put("3", createHour("3", "אנגלית", "מירב שלו"));
        week[0].put("4", createHour("4", "היסטוריה", "אורי מוח"));
        week[0].put("5", createHour("5", "תנ\"ך", "רועי ברק"));

        // Day 1
        week[1] = new JSONObject();
        week[1].put("1", createHour("1", "עברית", "יעל כהן"));
        week[1].put("2", createHour("2", "אנגלית", "מירב שלו"));
        week[1].put("3", createHour("3", "חינוך", "שרה דוד"));
        week[1].put("4", createHour("4", "ערבית", "אורי חן"));

        // Day 2
        week[2] = new JSONObject();
        week[2].put("1", createHour("1", "מדעים", "רון נתניהו"));
        week[2].put("2", createHour("2", "מתמטיקה", "רועי ברק"));
        week[2].put("3", createHour("3", "עברית", "יעל כהן"));
        week[2].put("4", createHour("4", "תנ\"ך", "יוסי כהן"));
        week[2].put("5", createHour("5", "של\"ח", "אורי חן"));

        // Day 3
        week[3] = new JSONObject();
        week[3].put("1", createHour("1", "ספרות", "מירב שלו"));
        week[3].put("2", createHour("2", "עברית", "יעל כהן"));
        week[3].put("3", createHour("3", "אנגלית", "רועי ברק"));
        week[3].put("4", createHour("4", "מדעים", "אורי מוח"));
        week[3].put("5", createHour("5", "של\"ח", "אורי חן"));
        week[3].put("6", createHour("6", "חינוך", "שרה דוד"));

        // Day 4
        week[4] = new JSONObject();
        week[4].put("1", createHour("1", "היסטוריה", "רון נתניהו"));
        week[4].put("2", createHour("2", "עברית", "יעל כהן"));
        week[4].put("3", createHour("3", "אנגלית", "מירב שלו"));
        week[4].put("4", createHour("4", "תנ\"ך", "יוסי כהן"));

        // Day 5
        week[5] = new JSONObject();
        week[5].put("1", createHour("1", "של\"ח", "רועי ברק"));
        week[5].put("2", createHour("2", "ספרות", "שרה דוד"));
        week[5].put("3", createHour("3", "עברית", "יעל כהן"));
        week[5].put("4", createHour("4", "אנגלית", "מירב שלו"));
        week[5].put("5", createHour("5", "חינוך", "אורי מוח"));
        week[5].put("6", createHour("6", "מדעים", "רון נתניהו"));

        // Day 6
        week[6] = new JSONObject();
        week[6].put("1", createHour("1", "עברית", "יעל כהן"));
        week[6].put("2", createHour("2", "חינוך", "שרה דוד"));
        week[6].put("3", createHour("3", "של\"ח", "יוסי כהן"));
        week[6].put("4", createHour("4", "היסטוריה", "רון נתניהו"));

        return week[dayIndex];
    }
    public JSONObject getOriginalSchedule(int dayIndex) throws JSONException {
        return getSchedule(dayIndex);
    }


    private JSONObject createGrade(String subject, String name, int gradeValue, String type) throws JSONException {
        String date = LocalDate.of(LocalDate.now().getYear(), 1, 1).format(ofPattern("dd/MM/yyyy"));

        JSONObject submission = new JSONObject()
                .put("type", type)
                .put("grade", gradeValue)
                .put("date", date);

        JSONArray submissions = new JSONArray().put(submission);

        return new JSONObject()
                .put("subject", subject)
                .put("name", name)
                .put("date", date)
                .put("grade", gradeValue)
                .put("submissions", submissions);
    }

    public JSONArray getGrades(String course) throws IOException, JSONException {

        JSONArray grades = new JSONArray();
        grades.put(createGrade("אנגלית", "מבחן באנגלית", 98, "מועד א"));
        grades.put(createGrade("עברית", "חיבור", 87, "מועד ב"));
        grades.put(createGrade("מתמטיקה", "מבחן סוף", 100, "מועד א"));
        grades.put(createGrade("תנ\"ך", "מבחן תנ\"ך", 90, "מועד א"));
        grades.put(createGrade("ספרות", "בגרות פנימית", 85, "מועד ב"));
        grades.put(createGrade("היסטוריה", "מבחן יחידה 2", 78, "מועד א"));
        grades.put(createGrade("ערבית", "מבחן הבנה", 92, "מועד א"));
        grades.put(createGrade("מדעים", "מעבדה", 95, "מועד ב"));
        grades.put(createGrade("חינוך", "השתתפות", 100, "שנתי"));
        grades.put(createGrade("של\"ח", "מבחן מסכם", 89, "מועד א"));
                    //example grade: {"name":"מבחן מחצית (%)","subject":"שכבה ט הוד השרון יום ב 16:00-19:15","date":"תשפה","grade":"100","submissions":[{"type":"מועד 1","grade":"100","date":"תשפה","downloadData":{"scanLocation":"\\\\rashim\\files\\mimsakim\\upload\\335476065\\00125831041\\BW.PDF","scanFileName":"BW.PDF"}},{"type":"מועד 2","grade":"","date":"תשפה","downloadData":{}}]}            grades.put(grade);

        return grades;
    }

    @Override
    public String getName() { return name; }

    @Override
    public String toString() {
        return "Platform{" + "name='" + name + '\'' + "'}";
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

    @Override
    public boolean isLoggedIn() {
        return loggedIn;
    }
    @Override
    public JSONObject toLoginJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("username", "demo"); // or username field
        obj.put("password", "demo");  // you'll need to store this if not already
        return obj;
    }

    public boolean refreshCookies(){
        loggedIn = true;
        return true;
    }
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("class", getClass().getName());
        obj.put("loggedIn", this.loggedIn);
        obj.put("name",     this.name);
        return obj;
    }


    /**
     * Recreate an instance from the JSON produced by toJson().
     */
    public static DemoPlatform fromJson(JSONObject obj) throws JSONException, IOException {
        DemoPlatform inst = new DemoPlatform("demo", "demo");

        inst.loggedIn = obj.optBoolean("loggedIn", false);
        inst.name     = obj.optString("name", null);
        return inst;
    }
    public JSONArray getScheduleIndexes(){
        return new JSONArray();
    }
}
