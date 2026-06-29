package com.remi8.editor;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.remi8.engine.core.GameEngine;
import com.remi8.engine.core.GameObject;

import java.io.*;
import java.util.*;

/**
 * مدير مشاريع REMI8
 * يحفظ ويحمّل المشاريع بتنسيق JSON
 */
public class ProjectManager {

    private static final String TAG = "REMI8_Project";
    private static final String PROJECTS_DIR = "REMI8Projects";
    private final Context context;
    private final Gson gson;

    public ProjectManager(Context context) {
        this.context = context;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * حفظ المشروع كاملاً
     */
    public boolean saveProject(GameEngine engine, String name, List<GameObject> objects) {
        try {
            File dir = getProjectDir(name);
            if (!dir.exists()) dir.mkdirs();

            // بيانات المشروع الرئيسية
            Map<String, Object> project = new LinkedHashMap<>();
            project.put("name", name);
            project.put("version", engine.getGameVersion());
            project.put("engine", "REMI8");
            project.put("engineVersion", "1.0.0");
            project.put("screenWidth", engine.getScreenWidth());
            project.put("screenHeight", engine.getScreenHeight());
            project.put("startScene", "مشهد_1");
            project.put("createdAt", System.currentTimeMillis());

            writeJson(new File(dir, "project.remi8"), project);

            // حفظ المشهد
            File scenesDir = new File(dir, "scenes");
            scenesDir.mkdirs();
            saveScene(new File(scenesDir, "مشهد_1.scene"), objects);

            // حفظ إعدادات المشروع
            Map<String, Object> settings = new LinkedHashMap<>();
            settings.put("gravity", 980);
            settings.put("backgroundColor", "#1a1a2e");
            settings.put("physics", true);
            writeJson(new File(dir, "settings.json"), settings);

            Log.i(TAG, "تم حفظ المشروع: " + name);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "فشل حفظ المشروع: " + e.getMessage());
            return false;
        }
    }

    private void saveScene(File file, List<GameObject> objects) throws IOException {
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("name", "مشهد_1");
        scene.put("backgroundColor", "#1a1a2e");

        List<Map<String, Object>> objList = new ArrayList<>();
        for (GameObject obj : objects) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", obj.getId());
            o.put("name", obj.getName());
            o.put("x", obj.x);
            o.put("y", obj.y);
            o.put("width", obj.width);
            o.put("height", obj.height);
            o.put("rotation", obj.rotation);
            o.put("scaleX", obj.scaleX);
            o.put("scaleY", obj.scaleY);
            o.put("active", obj.isActive());
            o.put("visible", obj.isVisible());
            o.put("layer", obj.getLayer());
            o.put("tags", obj.getTags());
            o.put("scripts", obj.getAttachedScripts());
            o.put("properties", obj.getProperties());
            objList.add(o);
        }
        scene.put("objects", objList);
        writeJson(file, scene);
    }

    /**
     * الحصول على مسار مجلد المشاريع
     */
    public File getProjectsRoot() {
        File extDir = context.getExternalFilesDir(null);
        if (extDir == null) extDir = context.getFilesDir();
        return new File(extDir, PROJECTS_DIR);
    }

    public File getProjectDir(String name) {
        return new File(getProjectsRoot(), name);
    }

    /**
     * قائمة المشاريع المتاحة
     */
    public List<String> listProjects() {
        List<String> names = new ArrayList<>();
        File root = getProjectsRoot();
        if (root.exists()) {
            File[] dirs = root.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File d : dirs) names.add(d.getName());
            }
        }
        return names;
    }

    private void writeJson(File file, Object data) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(gson.toJson(data));
        }
    }
}
