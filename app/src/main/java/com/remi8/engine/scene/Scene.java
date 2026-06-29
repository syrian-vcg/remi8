package com.remi8.engine.scene;

import android.graphics.Color;
import com.remi8.engine.core.GameEngine;
import com.remi8.engine.core.GameObject;
import com.remi8.engine.renderer.Renderer2D;
import java.util.*;

/**
 * مشهد اللعبة - REMI8
 * يحتوي على مجموعة كائنات اللعبة وإعدادات المشهد
 */
public class Scene {

    private String name;
    private int backgroundColor = Color.parseColor("#1a1a2e");
    private final List<GameObject> objects = new ArrayList<>();
    private GameEngine engine;

    // إعدادات المشهد
    private float cameraX = 0, cameraY = 0;
    private float cameraZoom = 1.0f;
    private boolean physicsEnabled = true;
    private float gravityX = 0, gravityY = 980;

    // البيانات المخصصة
    private final Map<String, Object> sceneData = new HashMap<>();

    public Scene(String name) {
        this.name = name;
    }

    /**
     * استدعاء عند تفعيل المشهد
     */
    public void onActivate(GameEngine engine) {
        this.engine = engine;
        if (physicsEnabled && engine != null) {
            engine.getPhysicsEngine().setGravity(gravityY);
        }
    }

    /**
     * استدعاء عند إلغاء تفعيل المشهد
     */
    public void onDeactivate() {
        engine = null;
    }

    /**
     * تحديث المشهد
     */
    public void update(float dt) {
        // تحديث الكائنات بترتيب الطبقة
        List<GameObject> sortedObjects = new ArrayList<>(objects);
        sortedObjects.sort(Comparator.comparingInt(obj -> obj.getLayer() * 10000 + obj.getZOrder()));
        for (GameObject obj : sortedObjects) {
            if (obj.isActive()) {
                obj.update(dt);
            }
        }
    }

    /**
     * رندرة المشهد
     */
    public void render(Renderer2D renderer) {
        // تطبيق إعدادات الكاميرا
        renderer.setCamera(cameraX, cameraY);
        renderer.setCameraZoom(cameraZoom);

        // رسم الخلفية
        renderer.clearScreen(backgroundColor);

        // رندرة الكائنات مرتبة بالطبقة
        List<GameObject> sortedObjects = new ArrayList<>(objects);
        sortedObjects.sort(Comparator.comparingInt(obj -> obj.getLayer() * 10000 + obj.getZOrder()));

        for (GameObject obj : sortedObjects) {
            if (obj.isActive() && obj.isVisible()) {
                renderer.drawGameObject(obj);
            }
        }
    }

    /**
     * إضافة كائن للمشهد
     */
    public void addObject(GameObject obj) {
        objects.add(obj);
    }

    /**
     * إزالة كائن من المشهد
     */
    public void removeObject(String id) {
        objects.removeIf(obj -> obj.getId().equals(id));
    }

    public void removeObject(GameObject obj) {
        objects.remove(obj);
    }

    /**
     * البحث عن كائن بالاسم
     */
    public GameObject findObject(String name) {
        for (GameObject obj : objects) {
            if (obj.getName().equals(name)) return obj;
        }
        return null;
    }

    /**
     * البحث عن كائن بالمعرف
     */
    public GameObject findObjectById(String id) {
        for (GameObject obj : objects) {
            if (obj.getId().equals(id)) return obj;
        }
        return null;
    }

    /**
     * البحث عن كائنات بالوسم
     */
    public List<GameObject> findObjectsByTag(String tag) {
        List<GameObject> result = new ArrayList<>();
        for (GameObject obj : objects) {
            if (obj.hasTag(tag)) result.add(obj);
        }
        return result;
    }

    /**
     * تحويل بيانات JSON إلى مشهد
     */
    public static Scene fromJson(Map<?, ?> data) {
        Object nameObj = data.get("name");
        String name = (nameObj instanceof String) ? (String) nameObj : "مشهد";
        Scene scene = new Scene(name);

        Object bgColor = data.get("backgroundColor");
        if (bgColor instanceof String) {
            try { scene.backgroundColor = Color.parseColor((String) bgColor); } catch (Exception ignored) {}
        }

        // تحميل الكائنات
        Object objs = data.get("objects");
        if (objs instanceof List) {
            for (Object objData : (List<?>) objs) {
                if (objData instanceof Map) {
                    scene.addObject(gameObjectFromJson((Map<?, ?>) objData));
                }
            }
        }
        return scene;
    }

    private static GameObject gameObjectFromJson(Map<?, ?> data) {
        Object nameObj = data.get("name");
        String name = (nameObj instanceof String) ? (String) nameObj : "كائن";
        GameObject obj = new GameObject(name);
        obj.x = toFloat(data.get("x"), 0);
        obj.y = toFloat(data.get("y"), 0);
        obj.width = toFloat(data.get("width"), 64);
        obj.height = toFloat(data.get("height"), 64);
        obj.rotation = toFloat(data.get("rotation"), 0);
        return obj;
    }

    private static float toFloat(Object v, float def) {
        if (v instanceof Number) return ((Number) v).floatValue();
        return def;
    }

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String n) { name = n; }
    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int c) { backgroundColor = c; }
    public List<GameObject> getObjects() { return objects; }
    public float getCameraX() { return cameraX; }
    public float getCameraY() { return cameraY; }
    public void setCamera(float x, float y) { cameraX = x; cameraY = y; }
    public float getCameraZoom() { return cameraZoom; }
    public void setCameraZoom(float z) { cameraZoom = z; }
    public boolean isPhysicsEnabled() { return physicsEnabled; }
    public void setPhysicsEnabled(boolean e) { physicsEnabled = e; }
    public Map<String, Object> getSceneData() { return sceneData; }
    public void setSceneData(String key, Object value) { sceneData.put(key, value); }
}
