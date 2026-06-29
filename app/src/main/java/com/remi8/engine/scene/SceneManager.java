package com.remi8.engine.scene;

import android.util.Log;

import com.google.gson.Gson;
import com.remi8.engine.core.GameEngine;
import com.remi8.engine.core.GameObject;
import com.remi8.engine.renderer.Renderer2D;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * مدير المشاهد - REMI8
 * يدير تحميل وتبديل وحفظ مشاهد اللعبة
 */
public class SceneManager {

    private static final String TAG = "REMI8_Scene";

    private final GameEngine engine;
    private Scene activeScene;
    private final Map<String, Scene> loadedScenes = new HashMap<>();

    // معلومات المشروع
    private String projectPath;
    private String projectName;

    public SceneManager(GameEngine engine) {
        this.engine = engine;
    }

    /**
     * تحميل مشروع من مسار ملف
     */
    public void loadProjectFromPath(String path) {
        this.projectPath = path;
        try {
            Gson gson = new Gson();
            FileReader reader = new FileReader(path + "/project.remi8");
            Map<?, ?> project = gson.fromJson(reader, Map.class);
            projectName = (String) project.get("name");

            // تحميل المشهد الأول
            String firstScene = (String) project.get("startScene");
            if (firstScene != null) {
                loadScene(path + "/scenes/" + firstScene + ".scene");
            }
        } catch (Exception e) {
            Log.e(TAG, "فشل تحميل المشروع: " + e.getMessage());
            // إنشاء مشهد فارغ افتراضي
            setActiveScene(createEmptyScene("مشهد_1"));
        }
    }

    /**
     * تحميل مشهد من ملف
     */
    public void loadScene(String scenePath) {
        try {
            Gson gson = new Gson();
            FileReader reader = new FileReader(scenePath);
            Map<?, ?> sceneData = gson.fromJson(reader, Map.class);
            Scene scene = Scene.fromJson(sceneData);
            loadedScenes.put(scene.getName(), scene);
            setActiveScene(scene);
            Log.i(TAG, "تم تحميل المشهد: " + scene.getName());
        } catch (Exception e) {
            Log.e(TAG, "فشل تحميل المشهد: " + e.getMessage());
        }
    }

    /**
     * تبديل المشهد
     */
    public void switchScene(String name) {
        if (loadedScenes.containsKey(name)) {
            setActiveScene(loadedScenes.get(name));
        } else {
            Log.w(TAG, "المشهد غير موجود: " + name);
        }
    }

    /**
     * إنشاء مشهد جديد فارغ
     */
    public Scene createEmptyScene(String name) {
        Scene scene = new Scene(name);
        loadedScenes.put(name, scene);
        return scene;
    }

    /**
     * تعيين المشهد النشط
     */
    public void setActiveScene(Scene scene) {
        if (activeScene != null) {
            activeScene.onDeactivate();
        }
        activeScene = scene;
        if (activeScene != null) {
            activeScene.onActivate(engine);
        }
    }

    /**
     * تحديث المشهد النشط
     */
    public void update(float dt) {
        if (activeScene != null) {
            activeScene.update(dt);
        }
    }

    /**
     * رندرة المشهد النشط
     */
    public void render(Renderer2D renderer) {
        if (activeScene != null) {
            activeScene.render(renderer);
        }
    }

    /**
     * إضافة كائن للمشهد النشط
     */
    public void addObject(GameObject obj) {
        if (activeScene != null) {
            activeScene.addObject(obj);
        }
    }

    /**
     * إزالة كائن من المشهد النشط
     */
    public void removeObject(String id) {
        if (activeScene != null) {
            activeScene.removeObject(id);
        }
    }

    /**
     * البحث عن كائن بالاسم
     */
    public GameObject findObject(String name) {
        if (activeScene != null) {
            return activeScene.findObject(name);
        }
        return null;
    }

    /**
     * البحث عن كائنات بالوسم
     */
    public List<GameObject> findObjectsByTag(String tag) {
        if (activeScene != null) {
            return activeScene.findObjectsByTag(tag);
        }
        return new ArrayList<>();
    }

    /**
     * إعادة تحميل المشهد الحالي من جديد
     */
    public void reloadCurrentScene() {
        if (activeScene != null) {
            String name = activeScene.getName();
            if (loadedScenes.containsKey(name)) {
                setActiveScene(loadedScenes.get(name));
            }
        }
    }

    public Scene getActiveScene() { return activeScene; }
    /** اسم بديل لـ getActiveScene للتوافق مع واجهة remiscript */
    public Scene getCurrentScene() { return activeScene; }
    public Map<String, Scene> getLoadedScenes() { return loadedScenes; }
    public String getProjectPath() { return projectPath; }
    public String getProjectName() { return projectName; }
}
